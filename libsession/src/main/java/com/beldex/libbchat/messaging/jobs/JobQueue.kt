package com.beldex.libbchat.messaging.jobs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libsignal.utilities.Log
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.schedule
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

class JobQueue : JobDelegate {
    private var hasResumedPendingJobs = false // Just for debugging
    private val jobTimestampMap = ConcurrentHashMap<Long, AtomicInteger>()
    private val rxDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val txDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = GlobalScope + SupervisorJob()
    private val queue = Channel<Job>(UNLIMITED)
    private val pendingJobIds = mutableSetOf<String>()

    val timer = Timer()

    private fun CoroutineScope.processWithDispatcher(
        channel: Channel<Job>,
        dispatcher: CoroutineDispatcher
    ) = launch(dispatcher) {
        for (job in channel) {
            if (!isActive) break
            job.delegate = this@JobQueue
            job.execute()
        }
    }

    init {
        // Process jobs
        scope.launch {
            val rxQueue = Channel<Job>(capacity = 4096)
            val txQueue = Channel<Job>(capacity = 4096)

            val receiveJob = processWithDispatcher(rxQueue, rxDispatcher)
            val txJob = processWithDispatcher(txQueue, txDispatcher)

            while (isActive) {
                for (job in queue) {
                    when (job) {
                        is NotifyPNServerJob, is AttachmentUploadJob, is MessageSendJob -> {
                            txQueue.send(job)
                        }
                        is MessageReceiveJob, is TrimThreadJob, is BatchMessageReceiveJob,
                        is AttachmentDownloadJob, is GroupAvatarDownloadJob -> {
                            rxQueue.send(job)
                        }
                        else -> {
                            throw IllegalStateException("Unexpected job type.")
                        }
                    }
                }
            }

            // The job has been cancelled
            receiveJob.cancel()
            txJob.cancel()

        }
    }

    companion object {

        @JvmStatic
        val shared: JobQueue by lazy { JobQueue() }
    }

    fun add(job: Job) {
        addWithoutExecuting(job)
        queue.trySend(job) // offer always called on unlimited capacity
    }

    private fun addWithoutExecuting(job: Job) {
        // When adding multiple jobs in rapid succession, timestamps might not be good enough as a unique ID. To
        // deal with this we keep track of the number of jobs with a given timestamp and add that to the end of the
        // timestamp to make it a unique ID. We can't use a random number because we do still want to keep track
        // of the order in which the jobs were added.
        val currentTime = System.currentTimeMillis()
        jobTimestampMap.putIfAbsent(currentTime, AtomicInteger())
        job.id = currentTime.toString() + jobTimestampMap[currentTime]!!.getAndIncrement().toString()
        MessagingModuleConfiguration.shared.storage.persistJob(job)
    }

    fun resumePendingSendMessage(job: Job) {
        val id = job.id ?: run {
            Log.e("Beldex", "tried to resume pending send job with no ID")
            return
        }
        if (!pendingJobIds.add(id)) {
            Log.e("Beldex","tried to re-queue pending/in-progress job")
            return
        }
        queue.trySend(job)
        //-Log.d("Beldex", "resumed pending send message $id")
    }

    fun resumePendingJobs(typeKey: String) {
        val allPendingJobs = MessagingModuleConfiguration.shared.storage.getAllPendingJobs(typeKey)
        val pendingJobs = mutableListOf<Job>()
        for ((id, job) in allPendingJobs) {
            if (job == null) {
                // Job failed to deserialize, remove it from the DB
                handleJobFailedPermanently(id)
            } else {
                pendingJobs.add(job)
            }
        }
        pendingJobs.sortedBy { it.id }.forEach { job ->
            Log.i("Beldex", "Resuming pending job of type: ${job::class.simpleName}.")
            queue.trySend(job) // Offer always called on unlimited capacity
        }
    }

    fun resumePendingJobs() {
        if (hasResumedPendingJobs) {
            Log.d("Beldex", "resumePendingJobs() should only be called once.")
            return
        }
        hasResumedPendingJobs = true
        val allJobTypes = listOf(
            AttachmentUploadJob.KEY,
            AttachmentDownloadJob.KEY,
            MessageReceiveJob.KEY,
            MessageSendJob.KEY,
            NotifyPNServerJob.KEY,
            BatchMessageReceiveJob.KEY,
            GroupAvatarDownloadJob.KEY
        )
        allJobTypes.forEach { type ->
            resumePendingJobs(type)
        }
    }

    override fun handleJobSucceeded(job: Job) {
        val jobId = job.id ?: return
        MessagingModuleConfiguration.shared.storage.markJobAsSucceeded(jobId)
        pendingJobIds.remove(jobId)
    }

    override fun handleJobFailed(job: Job, error: Exception) {
        // Canceled
        val storage = MessagingModuleConfiguration.shared.storage
        if (storage.isJobCanceled(job)) {
            return Log.i("Beldex", "${job::class.simpleName} canceled.")
        }
        // Message send jobs waiting for the attachment to upload
        if (job is MessageSendJob && error is MessageSendJob.AwaitingAttachmentUploadException) {
            Log.i("Beldex", "Message send job waiting for attachment upload to finish.")
            return
        }
        // Batch message receive job, re-queue non-permanently failed jobs
        if (job is BatchMessageReceiveJob) {
            val replacementParameters = job.failures
        }

        // Regular job failure
        job.failureCount += 1
        if (job.failureCount >= job.maxFailureCount) {
            handleJobFailedPermanently(job, error)
        } else {
            storage.persistJob(job)
            val retryInterval = getRetryInterval(job)
            Log.i("Beldex", "${job::class.simpleName} failed; scheduling retry (failure count is ${job.failureCount}).")
            timer.schedule(delay = retryInterval) {
                Log.i("Beldex", "Retrying ${job::class.simpleName}.")
                queue.trySend(job)
            }
        }
    }

    override fun handleJobFailedPermanently(job: Job, error: Exception) {
        val jobId = job.id ?: return
        handleJobFailedPermanently(jobId)
    }

    private fun handleJobFailedPermanently(jobId: String) {
        val storage = MessagingModuleConfiguration.shared.storage
        storage.markJobAsFailedPermanently(jobId)
    }

    private fun getRetryInterval(job: Job): Long {
        // Arbitrary backoff factor...
        // try  1 delay: 0.5s
        // try  2 delay: 1s
        // ...
        // try  5 delay: 16s
        // ...
        // try 11 delay: 512s
        val maxBackoff = (10 * 60).toDouble() // 10 minutes
        return (1000 * 0.25 * min(maxBackoff, (2.0).pow(job.failureCount))).roundToLong()
    }

    private fun Job.isSend() = this is MessageSendJob || this is AttachmentUploadJob

}
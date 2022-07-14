package com.beldex.libbchat.messaging.sending_receiving.pollers

import nl.komponents.kovenant.Promise
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.jobs.MessageReceiveJob
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libsignal.crypto.getRandomElementOrNull
import com.beldex.libsignal.utilities.Log
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.min

class ClosedGroupPollerV2 {
    private val executorService = Executors.newScheduledThreadPool(4)
    private var isPolling = mutableMapOf<String, Boolean>()
    private var futures = mutableMapOf<String, ScheduledFuture<*>>()

    private fun isPolling(groupPublicKey: String): Boolean {
        return isPolling[groupPublicKey] ?: false
    }

    companion object {
        private val minPollInterval = 4 * 1000
        private val maxPollInterval = 4 * 60 * 1000

        @JvmStatic
        val shared = ClosedGroupPollerV2()
    }

    class InsufficientMnodesException() : Exception("No mnodes left to poll.")
    class PollingCanceledException() : Exception("Polling canceled.")

    fun start() {
        val storage = MessagingModuleConfiguration.shared.storage
        val allGroupPublicKeys = storage.getAllClosedGroupPublicKeys()
        allGroupPublicKeys.iterator().forEach { startPolling(it) }
    }

    fun startPolling(groupPublicKey: String) {
        if (isPolling(groupPublicKey)) { return }
        isPolling[groupPublicKey] = true
        setUpPolling(groupPublicKey)
    }

    fun stop() {
        val storage = MessagingModuleConfiguration.shared.storage
        val allGroupPublicKeys = storage.getAllClosedGroupPublicKeys()
        allGroupPublicKeys.iterator().forEach { stopPolling(it) }
    }

    fun stopPolling(groupPublicKey: String) {
        futures[groupPublicKey]?.cancel(false)
        isPolling[groupPublicKey] = false
    }

    private fun setUpPolling(groupPublicKey: String) {
        poll(groupPublicKey).success {
            pollRecursively(groupPublicKey)
        }.fail {
            // The error is logged in poll(_:)
            pollRecursively(groupPublicKey)
        }
    }

    private fun pollRecursively(groupPublicKey: String) {
        if (!isPolling(groupPublicKey)) { return }
        // Get the received date of the last message in the thread. If we don't have any messages yet, pick some
        // reasonable fake time interval to use instead.
        val storage = MessagingModuleConfiguration.shared.storage
        val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
        val threadID = storage.getThreadId(groupID) ?: return
        val lastUpdated = storage.getLastUpdated(threadID)
        val timeSinceLastMessage = if (lastUpdated != -1L) Date().time - lastUpdated else 5 * 60 * 1000
        val minPollInterval = Companion.minPollInterval
        val limit: Long = 12 * 60 * 60 * 1000
        val a = (Companion.maxPollInterval - minPollInterval).toDouble() / limit.toDouble()
        val nextPollInterval = a * min(timeSinceLastMessage, limit) + minPollInterval
        executorService?.schedule({
            poll(groupPublicKey).success {
                pollRecursively(groupPublicKey)
            }.fail {
                // The error is logged in poll(_:)
                pollRecursively(groupPublicKey)
            }
        }, nextPollInterval.toLong(), TimeUnit.MILLISECONDS)
    }

    fun poll(groupPublicKey: String): Promise<Unit, Exception> {
        if (!isPolling(groupPublicKey)) { return Promise.of(Unit) }
        val promise = MnodeAPI.getSwarm(groupPublicKey).bind { swarm ->
            val mnode = swarm.getRandomElementOrNull() ?: throw InsufficientMnodesException() // Should be cryptographically secure
            if (!isPolling(groupPublicKey)) { throw PollingCanceledException() }
            Log.d("Beldex", "invoke MnodeAPI 3")
            MnodeAPI.getRawMessages(mnode, groupPublicKey).map { MnodeAPI.parseRawMessagesResponse(it, mnode, groupPublicKey) }
        }
        promise.success { envelopes ->
            if (!isPolling(groupPublicKey)) { return@success }
            envelopes.iterator().forEach { (envelope, serverHash) ->
                val job = MessageReceiveJob(envelope.toByteArray(), serverHash)
                JobQueue.shared.add(job)
            }
        }
        promise.fail {
            Log.d("Beldex", "Polling failed for closed group due to error: $it.")
        }
        return promise.map { }
    }
}

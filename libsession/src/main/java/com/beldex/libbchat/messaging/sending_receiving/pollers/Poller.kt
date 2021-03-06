package com.beldex.libbchat.messaging.sending_receiving.pollers

import nl.komponents.kovenant.*
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.BatchMessageReceiveJob
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.jobs.MessageReceiveParameters
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.mnode.MnodeModule
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.Mnode
import nl.komponents.kovenant.functional.bind
import java.security.SecureRandom
import java.util.*

private class PromiseCanceledException : Exception("Promise canceled.")

class Poller {
    var userPublicKey = MessagingModuleConfiguration.shared.storage.getUserPublicKey() ?: ""
    private var hasStarted: Boolean = false
    private val usedMnodes: MutableSet<Mnode> = mutableSetOf()
    var isCaughtUp = false

    // region Settings
    companion object {
        private val retryInterval: Long = 1 * 1000
    }
    // endregion

    // region Public API
    fun startIfNeeded() {
        if (hasStarted) { return }
        Log.d("Beldex", "Started polling.")
        hasStarted = true
        setUpPolling()
    }

    fun stopIfNeeded() {
        Log.d("Beldex", "Stopped polling.")
        hasStarted = false
        usedMnodes.clear()
    }
    // endregion

    // region Private API
    private fun setUpPolling() {
        if (!hasStarted) { return; }
        val thread = Thread.currentThread()
        MnodeAPI.getSwarm(userPublicKey).bind {
            usedMnodes.clear()
            val deferred = deferred<Unit, Exception>()
            pollNextMnode(deferred)
            deferred.promise
        }.always {
            Timer().schedule(object : TimerTask() {

                override fun run() {
                    thread.run { setUpPolling() }
                }
            }, retryInterval)
        }
    }

    private fun pollNextMnode(deferred: Deferred<Unit, Exception>) {
        val swarm = MnodeModule.shared.storage.getSwarm(userPublicKey) ?: setOf()
        val unusedMnodes = swarm.subtract(usedMnodes)
        if (unusedMnodes.isNotEmpty()) {
            val index = SecureRandom().nextInt(unusedMnodes.size)
            val nextMnode = unusedMnodes.elementAt(index)
            usedMnodes.add(nextMnode)
            Log.d("Beldex", "Polling $nextMnode.")
            poll(nextMnode, deferred).fail { exception ->
                if (exception is PromiseCanceledException) {
                    Log.d("Beldex", "Polling $nextMnode canceled.")
                } else {
                    Log.d("Beldex", "Polling $nextMnode failed; dropping it and switching to next mnode.")
                    MnodeAPI.dropMnodeFromSwarmIfNeeded(nextMnode, userPublicKey)
                    pollNextMnode(deferred)
                }
            }
        } else {
            isCaughtUp = true
            deferred.resolve()
        }
    }

    private fun poll(mnode: Mnode, deferred: Deferred<Unit, Exception>): Promise<Unit, Exception> {
        if (!hasStarted) { return Promise.ofFail(PromiseCanceledException()) }
        Log.d("Beldex", "invoke MnodeAPI 2")
        return MnodeAPI.getRawMessages(mnode, userPublicKey).bind { rawResponse ->
            isCaughtUp = true
            if (deferred.promise.isDone()) {
                task { Unit } // The long polling connection has been canceled; don't recurse
            } else {
                val messages = MnodeAPI.parseRawMessagesResponse(rawResponse, mnode, userPublicKey)
                val parameters = messages.map { (envelope, serverHash) ->
                    MessageReceiveParameters(envelope.toByteArray(), serverHash = serverHash)
                }
                parameters.chunked(20).forEach { chunk ->
                    val job = BatchMessageReceiveJob(chunk)
                    JobQueue.shared.add(job)
                }

                poll(mnode, deferred)
            }
        }
    }
    // endregion
}

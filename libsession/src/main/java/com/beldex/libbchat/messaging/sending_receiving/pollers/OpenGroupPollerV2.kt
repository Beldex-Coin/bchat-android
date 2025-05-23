package com.beldex.libbchat.messaging.sending_receiving.pollers

import nl.komponents.kovenant.Promise
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.*
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.messaging.open_groups.OpenGroupMessageV2
import com.beldex.libbchat.messaging.sending_receiving.MessageReceiver
import com.beldex.libbchat.messaging.sending_receiving.handleOpenGroupReactions
import com.beldex.libbchat.utilities.Address
import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.successBackground
import nl.komponents.kovenant.functional.map
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.max

class OpenGroupPollerV2(private val server: String, private val executorService: ScheduledExecutorService?) {
    var hasStarted = false
    var isCaughtUp = false
    var secondToLastJob: MessageReceiveJob? = null
    private var future: ScheduledFuture<*>? = null

    companion object {
        private const val pollInterval: Long = 4000L
        const val maxInactivityPeriod = 14 * 24 * 60 * 60 * 1000
    }

    fun startIfNeeded() {
        if (hasStarted) { return }
        hasStarted = true
        future = executorService?.schedule(::poll, 0, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        future?.cancel(false)
        hasStarted = false
    }

    fun poll(isBackgroundPoll: Boolean = false): Promise<Unit, Exception> {
        val storage = MessagingModuleConfiguration.shared.storage
        val rooms = storage.getAllV2OpenGroups().values.filter { it.server == server }.map { it.room }
        rooms.forEach { downloadGroupAvatarIfNeeded(it) }
        return OpenGroupAPIV2.compactPoll(rooms, server).successBackground { responses ->
            responses.forEach { (room, response) ->
                val openGroupID = "$server.$room"
                handleNewMessages(room, openGroupID, response.messages, isBackgroundPoll)
                handleDeletedMessages(room, openGroupID, response.deletions)
                if (secondToLastJob == null && !isCaughtUp) {
                    isCaughtUp = true
                }
            }
        }.always {
            executorService?.schedule(this@OpenGroupPollerV2::poll, pollInterval, TimeUnit.MILLISECONDS)
        }.map { }
    }

    private fun handleNewMessages(room: String, openGroupID: String, messages: List<OpenGroupMessageV2>, isBackgroundPoll: Boolean) {
        val storage = MessagingModuleConfiguration.shared.storage
        val groupID = GroupUtil.getEncodedOpenGroupID(openGroupID.toByteArray())
        // check thread still exists
        val threadId = storage.getThreadId(Address.fromSerialized(groupID)) ?: -1
        val threadExists = threadId >= 0
        if (!hasStarted || !threadExists) { return }
        val envelopes =  mutableListOf<Triple<Long?, SignalServiceProtos.Envelope, Map<String, OpenGroupAPIV2.Reaction>?>>()
        messages.sortedBy { it.serverID!! }.forEach { message ->
            if (!message.base64EncodedData.isNullOrEmpty()) {
                val envelope = SignalServiceProtos.Envelope.newBuilder()
                    .setType(SignalServiceProtos.Envelope.Type.BCHAT_MESSAGE)
                    .setSource(message.sender!!)
                    .setSourceDevice(1)
                    .setContent(message.toProto().toByteString())
                    .setTimestamp(message.sentTimestamp)
                    .build()
                envelopes.add(Triple( message.serverID, envelope, message.reactions))
            } else if (!message.reactions.isNullOrEmpty()) {
                message.serverID?.let {
                    MessageReceiver.handleOpenGroupReactions(threadId, it, message.reactions)
                }
            }
        }

        envelopes.chunked(256).forEach { list ->
            val parameters = list.map { (serverId, message, reactions) ->
                MessageReceiveParameters(message.toByteArray(), openGroupMessageServerID = serverId, reactions = reactions)
            }
            JobQueue.shared.add(BatchMessageReceiveJob(parameters, openGroupID))
        }
        if (envelopes.isNotEmpty()) {
            JobQueue.shared.add(TrimThreadJob(threadId,openGroupID))
        }

        val indicatedMax = messages.mapNotNull { it.serverID }.maxOrNull() ?: 0
        val currentLastMessageServerID = storage.getLastMessageServerID(room, server) ?: 0
        val actualMax = max(indicatedMax, currentLastMessageServerID)
        if (actualMax > 0 && indicatedMax > currentLastMessageServerID){
            storage.setLastMessageServerID(room, server, actualMax)
        }
    }

    private fun handleDeletedMessages(room: String, openGroupID: String, deletions: List<OpenGroupAPIV2.MessageDeletion>) {
        val storage = MessagingModuleConfiguration.shared.storage
        val groupID = GroupUtil.getEncodedOpenGroupID(openGroupID.toByteArray())
        val threadID = storage.getThreadId(Address.fromSerialized(groupID)) ?: return
        val serverIds = deletions.map { deletion ->
            deletion.deletedMessageServerID
        }
        if (serverIds.isNotEmpty()) {
            val deleteJob = OpenGroupDeleteJob(serverIds.toLongArray(), threadID, openGroupID)
            JobQueue.shared.add(deleteJob)
        }
        val currentMax = storage.getLastDeletionServerID(room, server) ?: 0L
        val latestMax = deletions.map { it.id }.maxOrNull() ?: 0L
        if (latestMax > currentMax && latestMax != 0L) {
            storage.setLastDeletionServerID(room, server, latestMax)
        }
    }

    private fun downloadGroupAvatarIfNeeded(room: String) {
        val storage = MessagingModuleConfiguration.shared.storage
        if (storage.getGroupAvatarDownloadJob(server, room) != null) return
        val groupId = GroupUtil.getEncodedOpenGroupID("$server.$room".toByteArray())
        storage.getGroup(groupId)?.let {
            if (System.currentTimeMillis() > it.updatedTimestamp + TimeUnit.DAYS.toMillis(7)) {
                JobQueue.shared.add(GroupAvatarDownloadJob(room, server))
            }
        }
    }
}
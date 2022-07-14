package com.thoughtcrimes.securesms.repository

import com.beldex.libbchat.database.MessageDataProvider
import com.beldex.libbchat.messaging.messages.control.UnsendRequest
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage
import com.beldex.libbchat.messaging.messages.visible.OpenGroupInvitation
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.toHexString
import com.thoughtcrimes.securesms.database.*
import com.thoughtcrimes.securesms.database.model.MessageRecord
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface ConversationRepository {
    fun isBeldexHostedOpenGroup(threadId: Long): Boolean
    fun getRecipientForThreadId(threadId: Long): Recipient
    fun saveDraft(threadId: Long, text: String)
    fun getDraft(threadId: Long): String?
    fun inviteContacts(threadId: Long, contacts: List<Recipient>)
    fun unblock(recipient: Recipient)
    fun deleteLocally(recipient: Recipient, message: MessageRecord)

    suspend fun deleteForEveryone(
        threadId: Long,
        recipient: Recipient,
        message: MessageRecord
    ): ResultOf<Unit>

    fun buildUnsendRequest(recipient: Recipient, message: MessageRecord): UnsendRequest?

    suspend fun deleteMessageWithoutUnsendRequest(
        threadId: Long,
        messages: Set<MessageRecord>
    ): ResultOf<Unit>

    suspend fun banUser(threadId: Long, recipient: Recipient): ResultOf<Unit>

    suspend fun banAndDeleteAll(threadId: Long, recipient: Recipient): ResultOf<Unit>
}

class DefaultConversationRepository @Inject constructor(
    private val textSecurePreferences: TextSecurePreferences,
    private val messageDataProvider: MessageDataProvider,
    private val threadDb: ThreadDatabase,
    private val draftDb: DraftDatabase,
    private val beldexThreadDb: BeldexThreadDatabase,
    private val smsDb: SmsDatabase,
    private val mmsDb: MmsDatabase,
    private val recipientDb: RecipientDatabase,
    private val beldexMessageDb: BeldexMessageDatabase
) : ConversationRepository {

    override fun isBeldexHostedOpenGroup(threadId: Long): Boolean {
        val openGroup = beldexThreadDb.getOpenGroupChat(threadId)
        Log.d("Beldex","social group $openGroup")
        return openGroup?.room == "bchat" || openGroup?.room == "beldex"
                || openGroup?.room == "crypto"  || openGroup?.room == "masternode"
    }

    override fun getRecipientForThreadId(threadId: Long): Recipient {
        return threadDb.getRecipientForThreadId(threadId)!!
    }

    override fun saveDraft(threadId: Long, text: String) {
        if (text.isEmpty()) return
        val drafts = DraftDatabase.Drafts()
        drafts.add(DraftDatabase.Draft(DraftDatabase.Draft.TEXT, text))
        draftDb.insertDrafts(threadId, drafts)
    }

    override fun getDraft(threadId: Long): String? {
        val drafts = draftDb.getDrafts(threadId)
        draftDb.clearDrafts(threadId)
        return drafts.find { it.type == DraftDatabase.Draft.TEXT }?.value
    }

    override fun inviteContacts(threadId: Long, contacts: List<Recipient>) {
        val openGroup = beldexThreadDb.getOpenGroupChat(threadId) ?: return
        for (contact in contacts) {
            val message = VisibleMessage()
            message.sentTimestamp = System.currentTimeMillis()
            val openGroupInvitation = OpenGroupInvitation()
            openGroupInvitation.name = openGroup.name
            openGroupInvitation.url = openGroup.joinURL
            message.openGroupInvitation = openGroupInvitation
            val outgoingTextMessage = OutgoingTextMessage.fromOpenGroupInvitation(
                openGroupInvitation,
                contact,
                message.sentTimestamp
            )
            smsDb.insertMessageOutboxNew(-1, outgoingTextMessage, message.sentTimestamp!!)
            MessageSender.send(message, contact.address)
        }
    }

    override fun unblock(recipient: Recipient) {
        recipientDb.setBlocked(recipient, false)
    }

    override fun deleteLocally(recipient: Recipient, message: MessageRecord) {
        buildUnsendRequest(recipient, message)?.let { unsendRequest ->
            textSecurePreferences.getLocalNumber()?.let {
                MessageSender.send(unsendRequest, Address.fromSerialized(it))
            }
        }
        messageDataProvider.deleteMessage(message.id, !message.isMms)
    }

    override suspend fun deleteForEveryone(
        threadId: Long,
        recipient: Recipient,
        message: MessageRecord
    ): ResultOf<Unit> = suspendCoroutine { continuation ->
        buildUnsendRequest(recipient, message)?.let { unsendRequest ->
            MessageSender.send(unsendRequest, recipient.address)
        }
        val openGroup = beldexThreadDb.getOpenGroupChat(threadId)
        if (openGroup != null) {
            beldexMessageDb.getServerID(message.id, !message.isMms)?.let { messageServerID ->
                OpenGroupAPIV2.deleteMessage(messageServerID, openGroup.room, openGroup.server)
                    .success {
                        messageDataProvider.deleteMessage(message.id, !message.isMms)
                        continuation.resume(ResultOf.Success(Unit))
                    }.fail { error ->
                        continuation.resumeWithException(error)
                    }
            }
        } else {
            messageDataProvider.deleteMessage(message.id, !message.isMms)
            messageDataProvider.getServerHashForMessage(message.id)?.let { serverHash ->
                var publicKey = recipient.address.serialize()
                if (recipient.isClosedGroupRecipient) {
                    publicKey = GroupUtil.doubleDecodeGroupID(publicKey).toHexString()
                }
                MnodeAPI.deleteMessage(publicKey, listOf(serverHash))
                    .success {
                        continuation.resume(ResultOf.Success(Unit))
                    }.fail { error ->
                        continuation.resumeWithException(error)
                    }
            }
        }
    }

    override fun buildUnsendRequest(recipient: Recipient, message: MessageRecord): UnsendRequest? {
        if (recipient.isOpenGroupRecipient) return null
        messageDataProvider.getServerHashForMessage(message.id) ?: return null
        val unsendRequest = UnsendRequest()
        if (message.isOutgoing) {
            unsendRequest.author = textSecurePreferences.getLocalNumber()
        } else {
            unsendRequest.author = message.individualRecipient.address.contactIdentifier()
        }
        unsendRequest.timestamp = message.timestamp

        return unsendRequest
    }

    override suspend fun deleteMessageWithoutUnsendRequest(
        threadId: Long,
        messages: Set<MessageRecord>
    ): ResultOf<Unit> = suspendCoroutine { continuation ->
        val openGroup = beldexThreadDb.getOpenGroupChat(threadId)
        if (openGroup != null) {
            val messageServerIDs = mutableMapOf<Long, MessageRecord>()
            for (message in messages) {
                val messageServerID =
                    beldexMessageDb.getServerID(message.id, !message.isMms) ?: continue
                messageServerIDs[messageServerID] = message
            }
            for ((messageServerID, message) in messageServerIDs) {
                OpenGroupAPIV2.deleteMessage(messageServerID, openGroup.room, openGroup.server)
                    .success {
                        messageDataProvider.deleteMessage(message.id, !message.isMms)
                    }.fail { error ->
                        continuation.resumeWithException(error)
                    }
            }
        } else {
            for (message in messages) {
                if (message.isMms) {
                    mmsDb.deleteMessage(message.id)
                } else {
                    smsDb.deleteMessage(message.id)
                }
            }
        }
        continuation.resume(ResultOf.Success(Unit))
    }

    override suspend fun banUser(threadId: Long, recipient: Recipient): ResultOf<Unit> =
        suspendCoroutine { continuation ->
            val bchatID = recipient.address.toString()
            val openGroup = beldexThreadDb.getOpenGroupChat(threadId)!!
            OpenGroupAPIV2.ban(bchatID, openGroup.room, openGroup.server)
                .success {
                    continuation.resume(ResultOf.Success(Unit))
                }.fail { error ->
                    continuation.resumeWithException(error)
                }
        }

    override suspend fun banAndDeleteAll(threadId: Long, recipient: Recipient): ResultOf<Unit> =
        suspendCoroutine { continuation ->
            val bchatID = recipient.address.toString()
            val openGroup = beldexThreadDb.getOpenGroupChat(threadId)!!
            OpenGroupAPIV2.banAndDeleteAll(bchatID, openGroup.room, openGroup.server)
                .success {
                    continuation.resume(ResultOf.Success(Unit))
                }.fail { error ->
                    continuation.resumeWithException(error)
                }
        }

}
package io.beldex.bchat.repository

import android.content.ContentResolver
import com.beldex.libbchat.database.MessageDataProvider
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.messages.Destination
import com.beldex.libbchat.messaging.messages.control.MessageRequestResponse
import com.beldex.libbchat.messaging.messages.control.UnsendRequest
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage
import com.beldex.libbchat.messaging.messages.visible.OpenGroupInvitation
import com.beldex.libbchat.messaging.messages.visible.Payment
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
import io.beldex.bchat.database.*
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.database.model.ThreadRecord
import kotlinx.coroutines.flow.Flow
import app.cash.copper.Query
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface ConversationRepository {
    fun isBeldexHostedOpenGroup(threadId: Long): Boolean
    fun getRecipientForThreadId(threadId: Long): Recipient?
    fun changes(threadId: Long): Flow<Query>
    fun saveDraft(threadId: Long, text: String)
    fun getDraft(threadId: Long): String?
    fun clearDrafts(threadId: Long)
    fun inviteContacts(threadId: Long, contacts: List<Recipient>)
    //Payment Tah
    fun sentPayment(threadId: Long, amount: String, txnId: String?, recipient: Recipient?)
    fun setBlocked(recipient: Recipient, blocked: Boolean)
    fun deleteLocally(recipient: Recipient, message: MessageRecord)
    /*Hales63*/
    fun setApproved(recipient: Recipient, isApproved: Boolean)

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

    suspend fun deleteThread(threadId: Long): ResultOf<Unit>

    /*Hales63*/
    suspend fun deleteMessageRequest(thread: ThreadRecord): ResultOf<Unit>

    suspend fun clearAllMessageRequests(): ResultOf<Unit>

    suspend fun acceptAllMessageRequests(): ResultOf<Unit>

    suspend fun acceptAllMessageRequest(thread: ThreadRecord): ResultOf<Unit>

    suspend fun request(): ResultOf<Unit>



    suspend fun acceptMessageRequest(threadId: Long, recipient: Recipient): ResultOf<Unit>

    fun declineMessageRequest(threadId: Long)

    fun hasReceived(threadId: Long): Boolean

    fun getLastSentMessageID(threadId: Long): Flow<Long>
}

class DefaultConversationRepository @Inject constructor(
    private val textSecurePreferences: TextSecurePreferences,
    private val messageDataProvider: MessageDataProvider,
    private val threadDb: ThreadDatabase,
    private val draftDb: DraftDatabase,
    private val beldexThreadDb: BeldexThreadDatabase,
    private val smsDb: SmsDatabase,
    private val mmsDb: MmsDatabase,
    private val mmsSmsDb: MmsSmsDatabase,
    private val recipientDb: RecipientDatabase,
    private val beldexMessageDb: BeldexMessageDatabase,
    private val bchatjobdatabase: BchatJobDatabase,
    private val contentResolver: ContentResolver,
) : ConversationRepository {

    override fun isBeldexHostedOpenGroup(threadId: Long): Boolean {
        val openGroup = beldexThreadDb.getOpenGroupChat(threadId)
        Log.d("Beldex","open group $openGroup")
        /*return openGroup?.room == "bchat" || openGroup?.room == "beldex"
                || openGroup?.room == "crypto"  || openGroup?.room == "masternode" || openGroup?.room == "belnet"*/
        return openGroup?.publicKey == OpenGroupAPIV2.defaultServerPublicKey
    }

    override fun getRecipientForThreadId(threadId: Long): Recipient? {
        return threadDb.getRecipientForThreadId(threadId)
    }

    override fun changes(threadId: Long): Flow<Query> =
        contentResolver.observeQuery(DatabaseContentProviders.Conversation.getUriForThread(threadId))

    override fun saveDraft(threadId: Long, text: String) {
        if (text.isEmpty()) return
        val drafts = DraftDatabase.Drafts()
        drafts.add(DraftDatabase.Draft(DraftDatabase.Draft.TEXT, text))
        draftDb.insertDrafts(threadId, drafts)
    }

    override fun getDraft(threadId: Long): String? {
        val drafts = draftDb.getDrafts(threadId)
        return drafts.find { it.type == DraftDatabase.Draft.TEXT }?.value
    }

    override fun clearDrafts(threadId: Long) {
        draftDb.clearDrafts(threadId)
    }

    override fun inviteContacts(threadId: Long, contacts: List<Recipient>) {
        val openGroup = beldexThreadDb.getOpenGroupChat(threadId) ?: return
        for (contact in contacts) {
            val message = VisibleMessage()
            message.sentTimestamp = MnodeAPI.nowWithOffset
            val openGroupInvitation = OpenGroupInvitation()
            openGroupInvitation.name = openGroup.name
            openGroupInvitation.url = openGroup.joinURL
            message.openGroupInvitation = openGroupInvitation
            val outgoingTextMessage = OutgoingTextMessage.fromOpenGroupInvitation(
                openGroupInvitation,
                contact,
                message.sentTimestamp
            )
            smsDb.insertMessageOutboxNew(-1, outgoingTextMessage, message.sentTimestamp!!,true)
            MessageSender.send(message, contact.address)
        }
    }

    override fun sentPayment(threadId: Long, amount: String, txnId: String?, recipient: Recipient?) {
        val message = VisibleMessage()
        message.sentTimestamp = MnodeAPI.nowWithOffset
        val payment = Payment()
        payment.amount = amount
        payment.txnId = txnId
        message.payment = payment
        val outgoingTextMessage = OutgoingTextMessage.fromPayment(
            payment,
            recipient,
            message.sentTimestamp
        )
        smsDb.insertMessageOutboxNew(-1,outgoingTextMessage,message.sentTimestamp!!,true)
        MessageSender.send(message,recipient!!.address)
    }
    override fun setBlocked(recipient: Recipient, blocked: Boolean) {
        recipientDb.setBlocked(recipient, blocked)
    }

    override fun deleteLocally(recipient: Recipient, message: MessageRecord) {
        buildUnsendRequest(recipient, message)?.let { unsendRequest ->
            textSecurePreferences.getLocalNumber()?.let {
                MessageSender.send(unsendRequest, Address.fromSerialized(it))
            }
        }
        messageDataProvider.deleteMessage(message.id, !message.isMms)
    }

    override suspend fun deleteThread(threadId: Long): ResultOf<Unit> {
        bchatjobdatabase.cancelPendingMessageSendJobs(threadId)
        threadDb.deleteConversation(threadId)
        return ResultOf.Success(Unit)
    }

    override fun setApproved(recipient: Recipient, isApproved: Boolean) {
        recipientDb.setApproved(recipient, isApproved)
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
            messageDataProvider.getServerHashForMessage(message.id,message.isMms)?.let { serverHash ->
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
        messageDataProvider.getServerHashForMessage(message.id,message.isMms) ?: return null
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

    override suspend fun deleteMessageRequest(thread: ThreadRecord): ResultOf<Unit> {
        bchatjobdatabase.cancelPendingMessageSendJobs(thread.threadId)
        threadDb.deleteConversation(thread.threadId)
        return ResultOf.Success(Unit)
    }

    override suspend fun acceptAllMessageRequest(thread: ThreadRecord): ResultOf<Unit>{
        bchatjobdatabase.cancelPendingMessageSendJobs(thread.threadId)
        recipientDb.setApproved(thread.recipient, true)
        val message = MessageRequestResponse(true)
        MessageSender.send(message, Destination.from(thread.recipient.address))
            .success {
                threadDb.setHasSent(thread.threadId, true)
                //continuation.resume(ResultOf.Success(Unit))
            }.fail { error ->
                //continuation.resumeWithException(error)
            }
        return ResultOf.Success(Unit)
    }

    override suspend fun clearAllMessageRequests(): ResultOf<Unit> {
        threadDb.readerFor(threadDb.unapprovedConversationList).use { reader ->
            while (reader.next != null) {
                deleteMessageRequest(reader.current)
            }
        }
        return ResultOf.Success(Unit)
    }

    override suspend fun acceptAllMessageRequests(): ResultOf<Unit> {
        threadDb.readerFor(threadDb.unapprovedConversationList).use { reader ->
            while (reader.next != null) {
                acceptAllMessageRequest(reader.current)
            }
        }
        return request()
    }

    override suspend fun request(): ResultOf<Unit> = suspendCoroutine { continuation ->
        continuation.resume(ResultOf.Success(Unit))
    }

    override suspend fun acceptMessageRequest(threadId: Long, recipient: Recipient): ResultOf<Unit> = suspendCoroutine { continuation ->
        recipientDb.setApproved(recipient, true)
        val storage = MessagingModuleConfiguration.shared.storage
        val message = MessageRequestResponse(true)
        MessageSender.send(message=message, address=recipient.address)
        // add a control message for our user
        storage.insertMessageRequestResponseFromYou(threadId)
        threadDb.setHasSent(threadId, true)
    }

    override fun declineMessageRequest(threadId: Long) {
        bchatjobdatabase.cancelPendingMessageSendJobs(threadId)
        threadDb.deleteConversation(threadId)
    }

    override fun hasReceived(threadId: Long): Boolean {
        val cursor = mmsSmsDb.getConversation(threadId, true)
        mmsSmsDb.readerFor(cursor).use { reader ->
            while (reader.next != null) {
                if (!reader.current.isOutgoing) {
                    return true
                }
            }
        }
        return false
    }

    override fun getLastSentMessageID(threadId: Long): Flow<Long> =
        flow {
            emit(mmsSmsDb.getLastMessageID(threadId, false, false))
        }.flowOn(Dispatchers.IO)

}
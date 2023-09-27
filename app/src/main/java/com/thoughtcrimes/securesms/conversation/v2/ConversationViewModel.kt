package com.thoughtcrimes.securesms.conversation.v2

import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.messages.signal.OutgoingMediaMessage
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupRecord
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.Pair
import com.thoughtcrimes.securesms.database.BchatContactDatabase
import com.thoughtcrimes.securesms.database.BeldexAPIDatabase
import com.thoughtcrimes.securesms.database.BeldexMessageDatabase
import com.thoughtcrimes.securesms.database.BeldexThreadDatabase
import com.thoughtcrimes.securesms.database.GroupDatabase
import com.thoughtcrimes.securesms.database.MmsDatabase
import com.thoughtcrimes.securesms.database.MmsSmsDatabase
import com.thoughtcrimes.securesms.database.RecipientDatabase
import com.thoughtcrimes.securesms.database.SmsDatabase
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.repository.ConversationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ConversationViewModel (
    private val repository: ConversationRepository,
    private val beldexThreadDb: BeldexThreadDatabase,
    private val bchatContactDb: BchatContactDatabase,
    private val threadDb: ThreadDatabase,
    private val recipientDatabase: RecipientDatabase,
    private val groupDb: GroupDatabase,
    private val beldexApiDb: BeldexAPIDatabase,
    private val mmsDb: MmsDatabase,
    private val smsDb: SmsDatabase,
    private val mmsSmsDatabase: MmsSmsDatabase,
    private val beldexMessageDb: BeldexMessageDatabase,
    val threadId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState

    private val _recipient = MutableStateFlow<Recipient?>(null)
    val recipient: StateFlow<Recipient?> = _recipient

    private val _backToHome = MutableStateFlow(false)
    val backToHome: StateFlow<Boolean> = _backToHome

    private lateinit var conversationsCursor: Cursor

    var senderBeldexAddress: String? = null

    /*Hales63*/
//    val recipient: Recipient?
//        get() = repository.getRecipientForThreadId(threadId)

    init {
        _uiState.update {
            it.copy(isBeldexHostedOpenGroup = repository.isBeldexHostedOpenGroup(threadId))
        }
        viewModelScope.launch {
            _recipient.value = repository.getRecipientForThreadId(threadId)
            if (recipient.value == null) {
                _backToHome.value = true
            }
            recipient.value?.let {
                if (it.isOpenGroupRecipient) {
                    val openGroup = beldexThreadDb.getOpenGroupChat(threadId)
                    if (openGroup == null) {
                        _backToHome.value = true
                    }
                }
                if (!it.isGroupRecipient && it.hasApprovedMe()) {
                    senderBeldexAddress = getBeldexAddress(it.address)
                }
                fetchConversations()
            }
        }
    }

    fun getConversationsCursor(): Cursor {
        return if (::conversationsCursor.isInitialized) {
            conversationsCursor
        } else {
            fetchConversations()
            conversationsCursor
        }
    }

    private fun fetchConversations() {
        conversationsCursor = mmsSmsDatabase.getConversation(threadId, !isIncomingMessageRequestThread())
    }

    fun isIncomingMessageRequestThread(): Boolean {
        return recipient.value?.let { recipient ->
            !recipient.isGroupRecipient &&
                    !recipient.isApproved &&
                    !recipient.isLocalNumber &&
                    getLastSeenAndHasSent().second() &&
                    getMessageCount() > 0
        } ?: false
    }

    fun acceptMessageRequest() = viewModelScope.launch {
        val recipient = recipient.value ?: return@launch Log.w("Beldex", "Recipient was null for accept message request action")
        repository.acceptMessageRequest(threadId, recipient)
            .onSuccess {
                _uiState.update {
                    it.copy(isMessageRequestAccepted = true)
                }
            }
            .onFailure {
                showMessage("Couldn't accept message request due to error: $it")
            }
    }

    fun declineMessageRequest() {
        repository.declineMessageRequest(threadId)
    }

    fun saveDraft(text: String) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.saveDraft(threadId, text)
        }
    }

    fun getDraft(): String? {
        val draft: String? = repository.getDraft(threadId)
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearDrafts(threadId)
        }
        return draft
    }

    fun inviteContacts(contacts: List<Recipient>) {
        repository.inviteContacts(threadId, contacts)
    }

    //Payment Tag
    fun sentPayment(amount: String, txnId: String?, recipient: Recipient?){
        repository.sentPayment(threadId,amount,txnId,recipient)
    }

    fun block() {
        val recipient = recipient.value ?: return Log.w("Beldex", "Recipient was null for block action")
        if (recipient.isContactRecipient) {
            repository.setBlocked(recipient, true)
        }
    }

    fun unblock() {
        val recipient = recipient.value ?: return Log.w("Beldex", "Recipient was null for unblock action")
        if (recipient.isContactRecipient) {
            repository.setBlocked(recipient,false)
        }
    }

    fun deleteThread() = viewModelScope.launch {
        repository.deleteThread(threadId)
    }

    fun deleteLocally(message: MessageRecord) {
        val recipient = recipient.value ?: return Log.w("Beldex", "Recipient was null for delete locally action")
        repository.deleteLocally(recipient, message)
    }

    //New Line v32
    fun setRecipientApproved() {
        val recipient = recipient.value ?: return Log.w("Beldex", "Recipient was null for set approved action")
        repository.setApproved(recipient, true)
    }

    fun deleteForEveryone(message: MessageRecord) = viewModelScope.launch {
        val recipient = recipient.value ?: return@launch
        repository.deleteForEveryone(threadId, recipient, message)
            .onFailure {
                showMessage("Couldn't delete message due to error: $it")
            }
    }

    fun deleteMessagesWithoutUnsendRequest(messages: Set<MessageRecord>) = viewModelScope.launch {
        repository.deleteMessageWithoutUnsendRequest(threadId, messages)
            .onFailure {
                showMessage("Couldn't delete message due to error: $it")
            }
    }

    fun banUser(recipient: Recipient) = viewModelScope.launch {
        repository.banUser(threadId, recipient)
            .onSuccess {
                showMessage("Successfully banned user")
            }
            .onFailure {
                showMessage("Couldn't ban user due to error: $it")
            }
    }

    fun banAndDeleteAll(recipient: Recipient) = viewModelScope.launch {
        repository.banAndDeleteAll(threadId, recipient)
            .onSuccess {
                showMessage("Successfully banned user and deleted all their messages")
            }
            .onFailure {
                showMessage("Couldn't execute request due to error: $it")
            }
    }

    private fun showMessage(message: String) {
        _uiState.update { currentUiState ->
            val messages = currentUiState.uiMessages + UiMessage(
                id = UUID.randomUUID().mostSignificantBits,
                message = message
            )
            currentUiState.copy(uiMessages = messages)
        }
    }
    
    fun messageShown(messageId: Long) {
        _uiState.update { currentUiState ->
            val messages = currentUiState.uiMessages.filterNot { it.id == messageId }
            currentUiState.copy(uiMessages = messages)
        }
    }
    /*Hales63*/
    fun hasReceived(): Boolean {
        return repository.hasReceived(threadId)
    }

    fun getBeldexAddress(address: Address): String {
        val contact = bchatContactDb.getContactWithBchatID(address.toString())
        return contact?.displayBeldexAddress(Contact.ContactContext.REGULAR) ?: address.toString()
    }

    fun markAllRead(): Boolean {
        return recipient.value?.let {
            threadDb.markAllAsRead(
                threadId,
                it.isOpenGroupRecipient
            )
            true
        } ?: false
    }

    fun setExpireMessages(recipient: Recipient, expirationTime: Int) {
        recipientDatabase.setExpireMessages(recipient, expirationTime)
    }

    fun getGroup(recipient: Recipient): GroupRecord? = groupDb.getGroup(recipient.address.toGroupString()).orNull()

    fun getOpenGroupChat() = beldexThreadDb.getOpenGroupChat(threadId)

    fun getUserCount(openGroup: OpenGroupV2) = beldexApiDb.getUserCount(
        openGroup.room,
        openGroup.server
    )

    fun getContactWithBChatId() = bchatContactDb.getContactWithBchatID(recipient.value?.address.toString())

    fun getLastSeenAndHasSent(): Pair<Long, Boolean> = threadDb.getLastSeenAndHasSent(threadId)

    fun getMessageCount() = threadDb.getMessageCount(threadId)

    fun insertMessageOutBox(outgoingTextMessage: OutgoingMediaMessage): Long {
        return mmsDb.insertMessageOutbox(outgoingTextMessage, threadId, false, null, runThreadUpdate = true)
    }

    fun insertMessageOutBoxSMS(outgoingTextMessage: OutgoingTextMessage, sentTimeStamp: Long?): Long {
        return smsDb.insertMessageOutbox(
            threadId,
            outgoingTextMessage,
            false,
            sentTimeStamp!!,
            null,
            true
        )
    }

    fun getMessagePositionInConversation(timestamp: Long, author: Address): Int {
        return mmsSmsDatabase.getMessagePositionInConversation(
            threadId,
            timestamp,
            author
        )
    }

//    fun getConversations(isIncomingRequestThread: Boolean): Cursor = mmsSmsDatabase.getConversation(threadId, isIncomingRequestThread)

    fun getUnreadCount() = mmsSmsDatabase.getUnreadCount(threadId)

    fun getMessageServerHash(id: Long): String? = beldexMessageDb.getMessageServerHash(id)

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(threadId: Long): Factory
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @AssistedInject constructor(
        @Assisted private val threadId: Long,
        private val repository: ConversationRepository,
        private val beldexThreadDb: BeldexThreadDatabase,
        private val bchatContactDb: BchatContactDatabase,
        private val threadDb: ThreadDatabase,
        private val recipientDatabase: RecipientDatabase,
        private val groupDb: GroupDatabase,
        private val beldexApiDb: BeldexAPIDatabase,
        private val mmsDb: MmsDatabase,
        private val smsDb: SmsDatabase,
        private val mmsSmsDatabase: MmsSmsDatabase,
        private val beldexMessageDb: BeldexMessageDatabase,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationViewModel(repository,beldexThreadDb, bchatContactDb, threadDb, recipientDatabase, groupDb, beldexApiDb, mmsDb, smsDb, mmsSmsDatabase, beldexMessageDb, threadId) as T
        }
    }
}

data class UiMessage(val id: Long, val message: String)
/*Hales63*/
data class ConversationUiState(
    val isBeldexHostedOpenGroup: Boolean = false,
    val uiMessages: List<UiMessage> = emptyList(),
    val isMessageRequestAccepted: Boolean? = null
)

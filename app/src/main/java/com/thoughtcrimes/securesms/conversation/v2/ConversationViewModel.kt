package com.thoughtcrimes.securesms.conversation.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.thoughtcrimes.securesms.database.model.MessageRecord
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.thoughtcrimes.securesms.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import java.util.UUID

class ConversationViewModel(
    val threadId: Long,
    private val repository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState
    /*Hales63*/
    val recipient: Recipient?
        get() = repository.getRecipientForThreadId(threadId)

    init {
        _uiState.update {
            it.copy(isBeldexHostedOpenGroup = repository.isBeldexHostedOpenGroup(threadId))
        }
    }
    fun acceptMessageRequest() = viewModelScope.launch {
        val recipient = recipient ?: return@launch Log.w("Beldex", "Recipient was null for accept message request action")
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
        val recipient = recipient ?: return Log.w("Beldex", "Recipient was null for block action")
        if (recipient.isContactRecipient) {
            repository.setBlocked(recipient, true)
        }
    }

    fun unblock() {
        val recipient = recipient ?: return Log.w("Beldex", "Recipient was null for unblock action")
        if (recipient.isContactRecipient) {
            repository.setBlocked(recipient,false)
        }
    }

    fun deleteThread() = viewModelScope.launch {
        repository.deleteThread(threadId)
    }

    fun deleteLocally(message: MessageRecord) {
        val recipient = recipient ?: return Log.w("Beldex", "Recipient was null for delete locally action")
        repository.deleteLocally(recipient, message)
    }

    //New Line v32
    fun setRecipientApproved() {
        val recipient = recipient ?: return Log.w("Beldex", "Recipient was null for set approved action")
        repository.setApproved(recipient, true)
    }

    fun deleteForEveryone(message: MessageRecord) = viewModelScope.launch {
        val recipient = recipient ?: return@launch
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


    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(threadId: Long): Factory
    }

    @Suppress("UNCHECKED_CAST")
    class Factory @AssistedInject constructor(
        @Assisted private val threadId: Long,
        private val repository: ConversationRepository
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationViewModel(threadId, repository) as T
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

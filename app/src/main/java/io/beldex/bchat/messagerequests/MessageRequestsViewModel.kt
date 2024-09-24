package io.beldex.bchat.messagerequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.repository.ConversationRepository
import javax.inject.Inject

@HiltViewModel
class MessageRequestsViewModel @Inject constructor(
    private val repository: ConversationRepository
) : ViewModel() {

    fun blockMessageRequest(thread: ThreadRecord) = viewModelScope.launch {
        val recipient = thread.recipient
        if (recipient.isContactRecipient) {
            repository.setBlocked(recipient, true)
            deleteMessageRequest(thread)
        }
    }

    fun deleteMessageRequest(thread: ThreadRecord) = viewModelScope.launch {
        repository.deleteMessageRequest(thread)
    }

    fun clearAllMessageRequests() = viewModelScope.launch {
        repository.clearAllMessageRequests()
    }
    fun acceptAllMessageRequests() = viewModelScope.launch {
        repository.acceptAllMessageRequests()
    }

}

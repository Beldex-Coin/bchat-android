package com.thoughtcrimes.securesms.messagerequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtcrimes.securesms.database.ThreadDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.my_account.ui.MessageRequestEvents
import com.thoughtcrimes.securesms.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MessageRequestsViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    private val repository: ConversationRepository
) : ViewModel() {

    data class UIState(
        val messageRequests: List<ThreadRecord> = emptyList()
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: MessageRequestEvents) {
        when (event) {
            is MessageRequestEvents.BlockRequest -> {
                blockMessageRequest(event.request)
            }
            is MessageRequestEvents.DeleteRequest -> {
                deleteMessageRequest(event.request)
            }
            is MessageRequestEvents.RequestSelected -> {

            }
            MessageRequestEvents.AcceptAllRequest -> {
                acceptAllMessageRequests()
            }
            MessageRequestEvents.ClearAllRequest -> {
                clearAllMessageRequests()
            }
        }
    }

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

    fun refreshRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            threadDb.unapprovedConversationList.use { openCursor ->
                val reader = threadDb.readerFor(openCursor)
                val threads = mutableListOf<ThreadRecord>()
                while (true) {
                    threads += reader.next ?: break
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            messageRequests = threads
                        )
                    }
                }
            }
        }
    }

}

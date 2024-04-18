package com.thoughtcrimes.securesms.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import com.thoughtcrimes.securesms.database.DatabaseContentProviders
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    private val repository: ConversationRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val executor = viewModelScope + SupervisorJob()

    private val _conversations = MutableLiveData<List<ThreadRecord>>()
    val conversations: LiveData<List<ThreadRecord>> = _conversations

    private val _isButtonsExpanded = MutableStateFlow(false)
    val isButtonExpanded = _isButtonsExpanded.asStateFlow()

    private val listUpdateChannel = Channel<Unit>(capacity = Channel.CONFLATED)

    fun tryUpdateChannel() = listUpdateChannel.trySend(Unit)

    fun getObservable(context: Context): LiveData<List<ThreadRecord>> {
        executor.launch(Dispatchers.IO) {
            context.contentResolver
                .observeQuery(DatabaseContentProviders.ConversationList.CONTENT_URI)
                .onEach { listUpdateChannel.trySend(Unit) }
                .collect()
        }
        executor.launch(Dispatchers.IO) {
            for (update in listUpdateChannel) {
                threadDb.approvedConversationList.use { openCursor ->
                    val reader = threadDb.readerFor(openCursor)
                    val threads = mutableListOf<ThreadRecord>()
                    while (true) {
                        threads += reader.next ?: break
                    }
                    withContext(Dispatchers.Main) {
                        _conversations.value = threads
                    }
                }
            }
        }
        return conversations
    }

    fun setButtonExpandedStatus(isExpanded: Boolean) {
        _isButtonsExpanded.update { isExpanded }
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

}
package io.beldex.bchat.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import io.beldex.bchat.database.DatabaseContentProviders
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.beldex.bchat.webrtc.CallManager
import io.beldex.bchat.webrtc.data.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import io.beldex.bchat.R
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val threadDb: ThreadDatabase,
    private val repository: ConversationRepository,
    savedStateHandle: SavedStateHandle,
    private val callManager: CallManager,
): ViewModel() {

    val callBanner: StateFlow<String?> = callManager.currentConnectionStateFlow.map {
        // a call is in progress if it isn't idle nor disconnected
        if(it !is State.Idle && it !is State.Disconnected){
            // call is started, we need to differentiate between in progress vs incoming
            if(it is State.Connected) context.getString(R.string.call_in_progress)
            else context.getString(R.string.unknown_sender)
        } else null // null when the call isn't in progress / incoming
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), initialValue = null)



    private val executor = viewModelScope + SupervisorJob()

    private val _conversations = MutableLiveData<List<ThreadRecord>>()
    val conversations: LiveData<List<ThreadRecord>> = _conversations

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
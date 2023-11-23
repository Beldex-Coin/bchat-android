package com.thoughtcrimes.securesms.home

import android.content.Context
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import com.thoughtcrimes.securesms.database.DatabaseContentProviders
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    savedStateHandle: SavedStateHandle
): ViewModel() {

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

}
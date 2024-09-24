package io.beldex.bchat.contacts.blocked

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.DatabaseContentProviders
import io.beldex.bchat.database.Storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BlockedContactsViewModel @Inject constructor(private val storage: Storage): ViewModel() {

    private val executor = viewModelScope + SupervisorJob()

    private val listUpdateChannel = Channel<Unit>(capacity = Channel.CONFLATED)

    private val _contacts = MutableLiveData(BlockedContactsViewState(emptyList()))

    fun subscribe(context: Context): LiveData<BlockedContactsViewState> {
        executor.launch(IO) {
            context.contentResolver
                .observeQuery(io.beldex.bchat.database.DatabaseContentProviders.Recipient.CONTENT_URI)
                .onStart {
                    listUpdateChannel.trySend(Unit)
                }
                .onEach {
                    listUpdateChannel.trySend(Unit)
                }
                .collect()
        }
        executor.launch(IO) {
            for (update in listUpdateChannel) {
                val blockedContactState = BlockedContactsViewState(storage.blockedContacts().sortedBy { it.name })
                withContext(Main) {
                    _contacts.value = blockedContactState
                }
            }
        }
        return _contacts
    }

    fun unblock(toUnblock: List<Recipient>) {
        storage.unblock(toUnblock)
    }

    fun unblockSingleUser(toUnblock:Recipient){
        storage.unblockSingleUser(toUnblock)
    }

    data class BlockedContactsViewState(
        val blockedContacts: List<Recipient>
    )

}
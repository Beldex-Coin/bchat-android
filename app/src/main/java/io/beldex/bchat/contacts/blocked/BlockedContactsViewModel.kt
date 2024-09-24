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
import io.beldex.bchat.my_account.ui.BlockedContactEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BlockedContactsViewModel @Inject constructor(private val storage: Storage): ViewModel() {

    data class UiState(
        val multiSelectedActivated: Boolean = false,
        val selectedList: List<Recipient> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val executor = viewModelScope + SupervisorJob()

    private val listUpdateChannel = Channel<Unit>(capacity = Channel.CONFLATED)

    private val _contacts = MutableLiveData(BlockedContactsViewState(emptyList()))

    fun subscribe(context: Context): LiveData<BlockedContactsViewState> {
        executor.launch(IO) {
            context.contentResolver
                .observeQuery(DatabaseContentProviders.Recipient.CONTENT_URI)
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

    fun onEvent(event: BlockedContactEvents) {
        when (event) {
            is BlockedContactEvents.AddContactToUnBlockList -> {
                _uiState.update {
                    it.copy(
                        selectedList = if (event.add)
                            uiState.value.selectedList + event.contact
                        else
                        uiState.value.selectedList.filter { contact -> contact != event.contact }
                    )
                }
            }
            BlockedContactEvents.MultiSelectClicked -> {
                _uiState.update {
                    it.copy(
                        multiSelectedActivated = !uiState.value.multiSelectedActivated
                    )
                }
            }
            BlockedContactEvents.UnblockMultipleContact -> {
                unblock(uiState.value.selectedList)
            }
            is BlockedContactEvents.UnblockSingleContact -> {
                unblockSingleUser(event.contact)
            }
        }
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
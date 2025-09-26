package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.repository.ConversationRepository
import io.beldex.bchat.util.SharedPreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ContactSharingState(
    val searchQuery: String = "",
    val contacts: List<ThreadRecord> = emptyList(),
    val filteredContacts: List<ThreadRecord> = emptyList(),
    val selectedContacts: List<ThreadRecord> = emptyList()
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    private val repository: ConversationRepository,
    private val preferenceUtil: SharedPreferenceUtil,
): ViewModel() {

    private val _state = MutableStateFlow(ContactSharingState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val publicKey = preferenceUtil.getPublicKey()
            val allContacts = arrayListOf<ThreadRecord>()
            threadDb.approvedConversationList.use { openCursor ->
                val reader = threadDb.readerFor(openCursor)
                val threads = mutableListOf<ThreadRecord>()
                while (true) {
                    threads += reader.next ?: break
                }
                allContacts.addAll(threads)

            }
            threadDb.archivedConversationList.use { openCursor ->
                val reader=threadDb.readerFor(openCursor)
                val threads=mutableListOf<ThreadRecord>()
                while (true) {
                    threads+=reader.next ?: break
                }
                allContacts.addAll(threads)
            }
            withContext(Dispatchers.Main) {
                val contacts = allContacts.filter { record ->
                    record.recipient.isContactRecipient
                }.filter { record ->
                    record.recipient.address.toString() != publicKey
                }
                _state.update {
                    it.copy(
                        contacts = contacts,
                        filteredContacts = contacts
                    )
                }
            }
        }
        state.distinctUntilChangedBy { it.searchQuery }
            .debounce(500)
            .onEach { state ->
                if (state.searchQuery.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            filteredContacts = it.contacts.filter { contact ->  contact.recipient.name?.contains(state.searchQuery, true) ?: false }
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            filteredContacts = it.contacts
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

    }

    fun postQuery(charSequence: CharSequence?) {
        charSequence ?: return
        _state.update {
            it.copy(
                searchQuery = charSequence.toString()
            )
        }
    }

    fun onContactSelected(contact: ThreadRecord, isSelected: Boolean) {
        _state.update { currentState ->
            val currentList = currentState.selectedContacts.toMutableList()

            if (isSelected) {
                if (!currentList.contains(contact)) {
                    currentList.add(contact)
                }
            } else {
                currentList.remove(contact)
            }

            currentState.copy(
                selectedContacts = currentList
            )
        }
    }
}
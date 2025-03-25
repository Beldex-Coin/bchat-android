package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.repository.ConversationRepository
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
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    private val repository: ConversationRepository,
): ViewModel() {

    private val _state = MutableStateFlow(ContactSharingState())
    val state = _state.asStateFlow()

    val selectedContacts = arrayListOf<ThreadRecord>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            threadDb.approvedConversationList.use { openCursor ->
                val reader = threadDb.readerFor(openCursor)
                val threads = mutableListOf<ThreadRecord>()
                while (true) {
                    threads += reader.next ?: break
                }
                withContext(Dispatchers.Main) {
                    val contacts = threads.filter { records -> records.recipient.isContactRecipient }
                    _state.update {
                        it.copy(
                            contacts = contacts,
                            filteredContacts = contacts
                        )
                    }
                }
            }
        }
        state.distinctUntilChangedBy { it.searchQuery }
            .debounce(500)
            .onEach { state ->
                if (state.searchQuery.isNotEmpty() && state.searchQuery.length > 2) {
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
        if (isSelected) {
            selectedContacts.add(contact)
        } else {
            selectedContacts.remove(contact)
        }
    }
}
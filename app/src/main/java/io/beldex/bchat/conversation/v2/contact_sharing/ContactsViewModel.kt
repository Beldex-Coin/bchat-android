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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ContactSharingState(
    val searchQuery: String = "",
    val contacts: List<ThreadRecord> = emptyList(),
    val filteredContacts: List<ThreadRecord> = emptyList(),
    val selectedContacts: List<ThreadRecord> = emptyList(),
    // True until the contact list has been read, so the empty state isn't shown while loading.
    val isLoading: Boolean = true
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
                    record.recipient.address.toString() != publicKey && record.recipient.hasApprovedMe()
                }
                _state.update {
                    it.copy(
                        contacts = contacts,
                        filteredContacts = filter(contacts, it.searchQuery),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun postQuery(charSequence: CharSequence?) {
        charSequence ?: return
        val query = charSequence.toString()
        // Filter right away: the list is in memory, and a delayed filter left the previous
        // "no such contact" result on screen while the text was being edited or cleared.
        _state.update {
            it.copy(searchQuery = query, filteredContacts = filter(it.contacts, query))
        }
    }

    private fun filter(contacts: List<ThreadRecord>, query: String): List<ThreadRecord> =
        if (query.isEmpty()) contacts
        else contacts.filter { contact -> contact.recipient.name?.contains(query, true) ?: false }

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
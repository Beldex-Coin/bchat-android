package com.thoughtcrimes.securesms.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreateSecretGroupViewModel@Inject constructor(
    private val threadDb: ThreadDatabase,
    private val textSecurePreferences: TextSecurePreferences,
    private val databaseComponent: DatabaseComponent
) : ViewModel() {

    private val _selectedRecipients = MutableStateFlow(listOf<String>())
    val selectedRecipients: StateFlow<List<String>> = _selectedRecipients.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recipients = MutableStateFlow(listOf<Recipient>())
    val recipients = searchQuery
        .combine(_recipients) { query, recipients ->
            if (query.isBlank()) {
                recipients
            } else {
                recipients.filter {
                    it.address.serialize().contains(query, ignoreCase = true) || it.name?.contains(query, ignoreCase = true) == true
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _recipients.value
        )


//    fun subscribe(context: Context): LiveData<SelectedContact> {
//        return selectedContacts
//    }

    init {
        viewModelScope.launch {
            threadDb.approvedConversationList.use { openCursor ->
                val reader = threadDb.readerFor(openCursor)
                val recipients = mutableListOf<Recipient>()
                while (true) {
                    recipients += reader.next?.recipient ?: break
                }
                withContext(Dispatchers.Main) {
                    _recipients.value = recipients
                        .filter { recipient -> !recipient.isGroupRecipient && recipient.hasApprovedMe() && recipient.address.serialize() != textSecurePreferences.getLocalNumber() }
                    kotlinx.coroutines.delay(3000)
                    readUserDisplayName()
                }
            }
        }
    }

    fun onEvent(event: SecretGroupEvents) {
        when (event) {
            is SecretGroupEvents.RecipientSelectionChanged -> {
                _selectedRecipients.value = if (event.isSelected) {
                    selectedRecipients.value + event.recipient.address.toString()
                } else {
                    selectedRecipients.value - event.recipient.address.toString()
                }
            }
            is SecretGroupEvents.SearchQueryChanged -> {
                _searchQuery.value = event.query
            }
        }
    }

    private fun readUserDisplayName() {
        recipients.value.forEach {
            val contact = databaseComponent.bchatContactDatabase().getContactWithBchatID(it.address.toString())
        }
    }


//    fun filter(query: String): List<Recipient> {
//        return _recipients.value?.filter {
//            it.address.serialize().contains(query, ignoreCase = true) || it.name?.contains(query, ignoreCase = true) == true
//        } ?: emptyList()
//    }
}



data class SelectedContact(
    val blockedContacts: List<String>)


package io.beldex.bchat.conversation_v2

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
class NewChatScreenViewModel@Inject constructor(
    @ApplicationContext context : Context,
    private val threadDb: ThreadDatabase,
    private val textSecurePreferences: TextSecurePreferences,
    private val databaseComponent: DatabaseComponent
) : ViewModel() {

    private val _selectedRecipients = MutableStateFlow(listOf<String>())
    val selectedRecipients: StateFlow<List<String>> = _selectedRecipients.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(value: String) {
        _searchQuery.value = value
    }

    private val _recipients = MutableStateFlow(listOf<Recipient>())
    val recipients = searchQuery
        .combine(_recipients) { query, recipients ->
            if (query.isBlank()) {
                recipients
            } else {
                recipients.filter {
                    it.address.serialize().contains(query, ignoreCase = true) || it.name?.contains(
                        query,
                        ignoreCase = true
                    ) == true
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _recipients.value
        )

    init {
        viewModelScope.launch {
            threadDb.conversationList.use { openCursor ->
                val reader = threadDb.readerFor(openCursor)
                val recipients = mutableListOf<Recipient>()
                while (true) {
                    recipients += reader.next?.recipient ?: break
                }
                withContext(Dispatchers.Main) {
                    _recipients.value = recipients
                        .filter { recipient -> (!recipient.isGroupRecipient && recipient.hasApprovedMe() && recipient.isApproved && recipient.address.serialize() != textSecurePreferences.getLocalNumber()) || (recipient.address.isClosedGroup && DatabaseComponent.get(context)
                            .groupDatabase().isActive(recipient.address.toGroupString())) }
                    kotlinx.coroutines.delay(3000)
                    readUserDisplayName()
                }
            }
        }
    }

    fun onEvent(query: String) {
        _searchQuery.value = query
    }

    private fun readUserDisplayName() {
        recipients.value.forEach {
            val contact = databaseComponent.bchatContactDatabase()
                .getContactWithBchatID(it.address.toString())
        }
    }
}

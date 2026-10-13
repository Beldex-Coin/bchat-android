package io.beldex.bchat.conversation_v2

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.groupSizeLimit
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.R
import io.beldex.bchat.util.isValidGroupName
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val databaseComponent: DatabaseComponent,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /** One-shot UI events: a message to toast, or the created group to open. */
    sealed interface Event {
        data class Message(@StringRes val resId: Int? = null, val text: String? = null) : Event
        data class GroupCreated(val threadId: Long, val address: Address) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCreateEnabled = MutableStateFlow(true)
    val isCreateEnabled: StateFlow<Boolean> = _isCreateEnabled.asStateFlow()

    /** Validates the form, creates the closed group and reports the outcome through [events]. */
    fun createGroup(rawName: String) {
        if (!_isCreateEnabled.value) return
        _isCreateEnabled.value = false
        viewModelScope.launch {
            try {
                if (!CheckOnline.isOnline(appContext)) {
                    _events.emit(Event.Message(resId = R.string.please_check_your_internet_connection))
                } else {
                    startCreation(rawName.trim())
                }
            } finally {
                // Same 2s debounce the button had, so a double tap can't create two groups.
                delay(2000)
                _isCreateEnabled.value = true
            }
        }
    }

    private fun startCreation(name: String) {
        val selected = _selectedRecipients.value
        val error = when {
            name.isEmpty() -> R.string.activity_create_closed_group_group_name_missing_error
            name.length >= 26 -> R.string.activity_create_closed_group_group_name_too_long_error
            selected.isEmpty() -> R.string.activity_create_closed_group_not_enough_group_members_error
            selected.count() >= groupSizeLimit -> R.string.activity_create_closed_group_too_many_group_members_error
            !isValidGroupName(name) -> R.string.nickname_special_char_not_allowed
            else -> null
        }
        if (error != null) {
            _events.tryEmit(Event.Message(resId = error))
            return
        }
        _isLoading.value = true
        val userPublicKey = textSecurePreferences.getLocalNumber()!!
        MessageSender.createClosedGroup(Device.ANDROID, name, selected + setOf(userPublicKey))
            .successUi { groupID ->
                val address = Address.fromSerialized(groupID)
                val threadId = databaseComponent.threadDatabase().getOrCreateThreadIdFor(
                    Recipient.from(appContext, address, false)
                )
                _isLoading.value = false
                _events.tryEmit(Event.GroupCreated(threadId, address))
            }.failUi {
                _isLoading.value = false
                _events.tryEmit(Event.Message(text = it.message))
            }
    }

    private val _selectedRecipients = MutableStateFlow(listOf<String>())
    val selectedRecipients: StateFlow<List<String>> = _selectedRecipients.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(value: String){
        _searchQuery.value = value
    }

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

    init {
        viewModelScope.launch(Dispatchers.IO) {

            val recipients = mutableListOf<Recipient>()

            threadDb.approvedConversationList.use { cursor ->
                val reader = threadDb.readerFor(cursor)
                while (true) {
                    recipients += reader.next?.recipient ?: break
                }
            }

            threadDb.archivedConversationList.use { cursor ->
                val reader = threadDb.readerFor(cursor)
                while (true) {
                    recipients += reader.next?.recipient ?: break
                }
            }

            val uniqueRecipients = recipients.distinctBy { it.address.serialize() }

            withContext(Dispatchers.Main) {
                _recipients.value = uniqueRecipients.filter { recipient ->
                    !recipient.isGroupRecipient &&
                            recipient.hasApprovedMe() &&
                            recipient.address.serialize() != textSecurePreferences.getLocalNumber() &&
                            !recipient.isBlocked
                }

                delay(3000)
                readUserDisplayName()
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


package io.beldex.bchat.conversation_v2

import android.content.Context
import androidx.annotation.StringRes
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libsignal.utilities.PublicKeyValidation
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
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
    @ApplicationContext private val context : Context,
    private val threadDb: ThreadDatabase,
    private val textSecurePreferences: TextSecurePreferences,
    private val databaseComponent: DatabaseComponent
) : ViewModel() {

    /** One-shot events for the hosting screen. */
    sealed interface Event {
        data class Message(@StringRes val resId: Int) : Event
        /** A chat with [address] should be opened; [threadId] is -1 when no thread exists yet. */
        data class OpenChat(val address: Address, val bnsName: String, val threadId: Long) : Event
    }

    /** What the contact row needs to draw a secret-group avatar. */
    data class GroupAvatarInfo(val hasTwoMembers: Boolean, val additionalPublicKey: String, val additionalDisplayName: String)

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _isBnsLoading = MutableStateFlow(false)
    val isBnsLoading: StateFlow<Boolean> = _isBnsLoading.asStateFlow()

    private val _chatIdError = MutableStateFlow(false)
    val chatIdError: StateFlow<Boolean> = _chatIdError.asStateFlow()

    fun clearChatIdError() { _chatIdError.value = false }

    /** Starts a chat from a typed BChat ID or BNS name (the New Chat popup). */
    fun startChat(input: String) {
        if (!CheckOnline.isOnline(context)) {
            _events.tryEmit(Event.Message(R.string.please_check_your_internet_connection))
            return
        }
        if (input.isEmpty()) {
            _events.tryEmit(Event.Message(R.string.bchat_id_warning_message))
            return
        }
        if (PublicKeyValidation.isValid(input)) {
            openChat(input, input)
        } else {
            // Not a key: treat as a BNS name.
            _isBnsLoading.value = true
            MnodeAPI.getBchatID(input).successUi { hexEncodedPublicKey ->
                _isBnsLoading.value = false
                openChat(hexEncodedPublicKey, input)
            }.failUi {
                // Shown inline under the field only; a toast on top of it was a duplicate (N21.3).
                _chatIdError.value = true
                _isBnsLoading.value = false
            }
        }
    }

    /** Opens a chat for an already-resolved key (typed, resolved from BNS, or scanned from a QR code). */
    fun openChat(hexEncodedPublicKey: String, bnsName: String) {
        val address = Address.fromSerialized(hexEncodedPublicKey)
        val recipient = Recipient.from(context, address, false)
        val threadId = threadDb.getThreadIdIfExistsFor(recipient)
        _events.tryEmit(Event.OpenChat(address, bnsName, threadId))
    }

    fun groupAvatarInfo(recipient: Recipient): GroupAvatarInfo {
        val members = databaseComponent.groupDatabase()
            .getGroupMemberAddresses(recipient.address.toGroupString(), true)
            .sorted()
            .take(2)
        val additionalPk = members.getOrNull(1)?.serialize() ?: ""
        val contact = databaseComponent.bchatContactDatabase().getContactWithBchatID(additionalPk)
        val additionalDisplay = contact?.displayName(Contact.ContactContext.REGULAR, context) ?: additionalPk
        return GroupAvatarInfo(members.size >= 2, additionalPk, additionalDisplay)
    }

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
            val recipients = mutableListOf<Recipient>()
            threadDb.conversationList.use { openCursor ->
                val reader = threadDb.readerFor(openCursor)
                while (true) {
                    recipients += reader.next?.recipient ?: break
                }
            }

            threadDb.archivedConversationList.use { archivedCursor ->
                val reader = threadDb.readerFor(archivedCursor)
                while (true) {
                    recipients += reader.next?.recipient ?: break
                }
            }

            withContext(Dispatchers.Main) {
                _recipients.value = recipients
                    .distinctBy { it.address.serialize() } // avoid duplicates
                    .filter { recipient ->
                        (!recipient.isGroupRecipient && recipient.hasApprovedMe() && recipient.isApproved &&
                                recipient.address.serialize() != textSecurePreferences.getLocalNumber()) ||
                                (recipient.address.isClosedGroup && DatabaseComponent.get(context)
                                    .groupDatabase().isActive(recipient.address.toGroupString()))
                    }
                kotlinx.coroutines.delay(3000)
                readUserDisplayName()
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

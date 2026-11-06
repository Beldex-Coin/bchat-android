package io.beldex.bchat.conversation.v2.forward

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.R
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One chat the user can forward to. */
data class ForwardTarget(
    val threadId: Long,
    val recipient: Recipient,
    val name: String,
    val kind: Kind
) {
    enum class Kind { NOTE_TO_SELF, SECRET_GROUP, SOCIAL_GROUP, CONTACT }
}

data class ForwardState(
    val loading: Boolean = true,
    val query: String = "",
    val targets: List<ForwardTarget> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val payloads: List<ForwardPayload> = emptyList(),
    val error: ForwardError? = null,
    val sending: Boolean = false
) {
    private fun visible(kind: ForwardTarget.Kind) = targets.filter {
        it.kind == kind && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
    }
    val noteToSelf get() = visible(ForwardTarget.Kind.NOTE_TO_SELF)
    val secretGroups get() = visible(ForwardTarget.Kind.SECRET_GROUP)
    val socialGroups get() = visible(ForwardTarget.Kind.SOCIAL_GROUP)
    val contacts get() = visible(ForwardTarget.Kind.CONTACT)
    val canForward get() = selected.isNotEmpty() && payloads.isNotEmpty() && !sending
}

enum class ForwardError { NOT_DOWNLOADED, NOT_FORWARDABLE }

class ForwardMessageViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ForwardState())
    val state = _state.asStateFlow()

    init {
        val ids: LongArray = savedState[ForwardMessageActivity.EXTRA_MESSAGE_IDS] ?: LongArray(0)
        val isMms: BooleanArray = savedState[ForwardMessageActivity.EXTRA_MESSAGE_IS_MMS] ?: BooleanArray(0)
        val given: List<ForwardPayload> = savedState.get<ArrayList<ForwardPayload>>(ForwardMessageActivity.EXTRA_PAYLOADS).orEmpty()
        viewModelScope.launch(Dispatchers.IO) {
            val (payloads, error) = resolvePayloads(ids, isMms, given)
            val targets = loadTargets()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = false, payloads = payloads, targets = targets, error = error) }
            }
        }
    }

    private fun resolvePayloads(ids: LongArray, isMms: BooleanArray, given: List<ForwardPayload>): Pair<List<ForwardPayload>, ForwardError?> {
        val db = DatabaseComponent.get(getApplication())
        val payloads = given.filterNot { it.isEmpty }.toMutableList()
        var error: ForwardError? = null
        ids.forEachIndexed { index, id ->
            val record = try {
                if (isMms.getOrElse(index) { false }) db.mmsDatabase().getMessageRecord(id)
                else db.smsDatabase().getMessageRecord(id)
            } catch (e: Exception) {
                Log.w("ForwardMessage", "Message $id not found", e); null
            } ?: return@forEachIndexed
            when (val result = ForwardPayload.from(record)) {
                is ForwardPayload.Companion.Result.Ok -> payloads += result.payload
                ForwardPayload.Companion.Result.NotDownloaded -> error = ForwardError.NOT_DOWNLOADED
                ForwardPayload.Companion.Result.NotForwardable -> if (error == null) error = ForwardError.NOT_FORWARDABLE
            }
        }
        return payloads to error
    }

    /** Same sources as the contact-share picker: approved + archived chats, minus self / inactive groups. */
    private fun loadTargets(): List<ForwardTarget> {
        val context = getApplication<Application>()
        val db = DatabaseComponent.get(context)
        val threadDb = db.threadDatabase()
        val self = TextSecurePreferences.getLocalNumber(context)
        val records = mutableListOf<ThreadRecord>()
        threadDb.approvedConversationList.use { cursor ->
            val reader = threadDb.readerFor(cursor)
            while (true) records += reader.next ?: break
        }
        threadDb.archivedConversationList.use { cursor ->
            val reader = threadDb.readerFor(cursor)
            while (true) records += reader.next ?: break
        }
        val chats = records.distinctBy { it.threadId }.mapNotNull { record ->
            val recipient = record.recipient
            val kind = when {
                recipient.isClosedGroupRecipient -> {
                    if (!db.groupDatabase().isActive(recipient.address.toGroupString())) return@mapNotNull null
                    ForwardTarget.Kind.SECRET_GROUP
                }
                recipient.isOpenGroupRecipient -> ForwardTarget.Kind.SOCIAL_GROUP
                recipient.isContactRecipient -> {
                    if (recipient.address.serialize() == self || !recipient.hasApprovedMe()) return@mapNotNull null
                    ForwardTarget.Kind.CONTACT
                }
                else -> return@mapNotNull null
            }
            val name = recipient.name?.takeIf { it.isNotBlank() } ?: recipient.address.serialize()
            ForwardTarget(record.threadId, recipient, name, kind)
        }.sortedBy { it.name.lowercase() }

        // Note to Self is always offered (first), even before its chat has ever been opened.
        val noteToSelf = self?.let { publicKey ->
            val selfRecipient = Recipient.from(context, Address.fromSerialized(publicKey), false)
            val existing = threadDb.getThreadIdIfExistsFor(selfRecipient)
            ForwardTarget(
                threadId = if (existing > 0) existing else NOTE_TO_SELF_KEY,
                recipient = selfRecipient,
                name = context.getString(R.string.note_to_self),
                kind = ForwardTarget.Kind.NOTE_TO_SELF
            )
        }
        return listOfNotNull(noteToSelf) + chats
    }

    companion object {
        /** Selection key for Note to Self while it has no thread yet (real thread ids are > 0). */
        const val NOTE_TO_SELF_KEY = -1L
    }

    fun onQueryChanged(query: String) = _state.update { it.copy(query = query) }

    fun toggle(target: ForwardTarget) = _state.update {
        it.copy(selected = if (target.threadId in it.selected) it.selected - target.threadId else it.selected + target.threadId)
    }

    /** Sends to every selected chat; [onDone] gets the chats that were sent to (on the main thread). */
    fun forward(onDone: (List<ForwardTarget>) -> Unit) {
        val current = _state.value
        if (!current.canForward) return
        _state.update { it.copy(sending = true) }
        val chosen = current.targets.filter { it.threadId in current.selected }
        viewModelScope.launch(Dispatchers.IO) {
            val threads = MessageForwarder.forward(getApplication(), chosen.map { it.recipient }, current.payloads)
            // Use the real thread ids (Note to Self may have just been created).
            val sent = chosen.mapNotNull { target ->
                threads[target.recipient.address.serialize()]?.let { target.copy(threadId = it) }
            }
            withContext(Dispatchers.Main) {
                _state.update { it.copy(sending = false) }
                onDone(sent)
            }
        }
    }
}

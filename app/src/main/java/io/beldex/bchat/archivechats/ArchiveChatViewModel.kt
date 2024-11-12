package io.beldex.bchat.archivechats

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libsignal.utilities.ThreadUtils
import com.beldex.libsignal.utilities.toHexString
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.R
import io.beldex.bchat.database.ThreadDatabase
import kotlinx.coroutines.launch
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.my_account.ui.ArchiveChatsEvents
import io.beldex.bchat.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ArchiveChatViewModel @Inject constructor(
    private val threadDb : ThreadDatabase, private val repository : ConversationRepository
) : ViewModel() {

    data class UIState(
        val archiveChats : List<ThreadRecord> = emptyList()
    )

    private val _uiState=MutableStateFlow(UIState())
    val uiState=_uiState.asStateFlow()

    private val _archiveChatsCount=MutableLiveData<Int>()
    val archiveChatsCount : LiveData<Int> get()=_archiveChatsCount

    init {
        _archiveChatsCount.value=threadDb.archivedConversationList.count
    }

    fun onEvent(event : ArchiveChatsEvents) {
        when (event) {
            is ArchiveChatsEvents.UnArchiveChats -> {
                unArchiveChat(event.thread)
            }

            is ArchiveChatsEvents.BlockConversation -> {
                blockContact(event.thread)
            }

            is ArchiveChatsEvents.UnBlockConversation -> {
                unBlockContact(event.thread)
            }

            is ArchiveChatsEvents.MuteNotification -> {
                muteConversation(event.thread, event.index, event.context)
            }

            is ArchiveChatsEvents.NotificationSettings -> {
                notificationSettings(event.thread, event.index, event.context)
            }

            is ArchiveChatsEvents.MarkAsRead -> {
                markAsRead(event.thread)
            }

            is ArchiveChatsEvents.DeleteConversation -> {
                deleteConversation(event.thread, event.context)
            }
        }
    }

    private fun unArchiveChat(thread : ThreadRecord)=viewModelScope.launch {
        val threadID=thread.threadId
        viewModelScope.launch(Dispatchers.IO) {
            threadDb.setThreadUnArchived(threadID)
            updateArchiveChatCount(threadDb.archivedConversationList.count)
        }
    }

    private fun blockContact(thread : ThreadRecord)=viewModelScope.launch {
        val recipient=thread.recipient
        if (recipient.isContactRecipient) {
            repository.setBlocked(recipient, true)
        }

    }

    private fun unBlockContact(thread : ThreadRecord)=viewModelScope.launch {
        val recipient=thread.recipient
        if (recipient.isContactRecipient) {
            repository.setBlocked(recipient, false)
        }
    }

    private fun muteConversation(thread : ThreadRecord, index : Int, context : Context) {
        val muteUntil=when (index) {
            1 -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
            2 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
            3 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
            4 -> Long.MAX_VALUE
            else -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
        }
        viewModelScope.launch(Dispatchers.IO) {
            thread.let {
                DatabaseComponent.get(context).recipientDatabase().setMuted(it.recipient, muteUntil)
            }
        }
    }

    private fun notificationSettings(thread : ThreadRecord, index : Int, context : Context) {
        viewModelScope.launch(Dispatchers.IO) {
            thread.let {
                DatabaseComponent.get(context).recipientDatabase()
                    .setNotifyType(it.recipient, index.toString().toInt())
            }
        }

    }

    private fun markAsRead(thread : ThreadRecord)=viewModelScope.launch {
        viewModelScope.launch(Dispatchers.IO) {
            thread.let {
                ThreadUtils.queue {
                    threadDb.markAllAsRead(thread.threadId, thread.recipient.isOpenGroupRecipient)
                }
            }
        }

    }

    fun refreshContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            threadDb.archivedConversationList.use { openCursor ->
                val reader=threadDb.readerFor(openCursor)
                val threads=mutableListOf<ThreadRecord>()
                while (true) {
                    threads+=reader.next ?: break
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            archiveChats=threads
                        )
                    }
                }
            }
        }
    }

    private fun deleteConversation(threadRecord : ThreadRecord, context : Context) {
        threadRecord.let {
            val threadID=it.threadId
            val recipient=it.recipient
            viewModelScope.launch(Dispatchers.Main) {
                // Cancel any outstanding jobs
                DatabaseComponent.get(context).bchatJobDatabase()
                    .cancelPendingMessageSendJobs(threadID)
                // Send a leave group message if this is an active closed group
                if (recipient.address.isClosedGroup && DatabaseComponent.get(context)
                        .groupDatabase().isActive(recipient.address.toGroupString())
                ) {
                    var isClosedGroup : Boolean
                    var groupPublicKey : String?
                    try {
                        groupPublicKey=GroupUtil.doubleDecodeGroupID(recipient.address.toString())
                            .toHexString()
                        isClosedGroup=DatabaseComponent.get(context).beldexAPIDatabase()
                            .isClosedGroup(groupPublicKey)
                    } catch (e : IOException) {
                        groupPublicKey=null
                        isClosedGroup=false
                    }
                    if (isClosedGroup) {
                        MessageSender.explicitLeave(groupPublicKey!!, false)
                    }
                }
                // Delete the conversation
                val v2OpenGroup=
                    DatabaseComponent.get(context).beldexThreadDatabase().getOpenGroupChat(threadID)
                if (v2OpenGroup != null) {
                    OpenGroupManager.delete(
                        v2OpenGroup.server, v2OpenGroup.room, context
                    )
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        threadDb.deleteConversation(threadID)
                    }
                }
                // Update the badge count
                ApplicationContext.getInstance(context).messageNotifier.updateNotification(
                    context
                )
                // Notify the user
                val toastMessage=
                    if (recipient.isGroupRecipient) R.string.MessageRecord_left_group else R.string.activity_home_conversation_deleted_message
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun updateArchiveChatCount(count : Int) {
        _archiveChatsCount.postValue(count)
    }
}

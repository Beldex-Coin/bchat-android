package io.beldex.bchat.conversation.v2

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.WorkerThread
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.RequestManager
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.messages.ControlMessageView
import io.beldex.bchat.conversation.v2.messages.VisibleMessageView
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.preferences.PrivacySettingsActivity
import io.beldex.bchat.conversation.v2.messages.VisibleMessageViewDelegate
import io.beldex.bchat.conversation.v2.search.SearchViewModel
import io.beldex.bchat.database.CursorRecyclerViewAdapter
import io.beldex.bchat.database.MmsSmsColumns
import io.beldex.bchat.database.MmsSmsDatabase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ConversationAdapter(
    context: Context,
    cursor: Cursor?,
    private val searchViewModel : SearchViewModel?,
    private val onItemPress: (MessageRecord, Int, VisibleMessageView, MotionEvent) -> Unit,
    private val onItemSwipeToReply: (MessageRecord, Int) -> Unit,
    private val onItemLongPress: (MessageRecord, Int, View) -> Unit,
    private val glide: RequestManager,
    private val onDeselect: (MessageRecord, Int) -> Unit,
    private val onAttachmentNeedsDownload: (Long, Long) -> Unit, lifecycleCoroutineScope: LifecycleCoroutineScope,
    var isAdmin: Boolean = false,
    private val threadRecipientProvider: Recipient?,
    private val messageDB: MmsSmsDatabase,
) : CursorRecyclerViewAdapter<ViewHolder>(context, cursor) {
    private val contactDB by lazy { DatabaseComponent.get(context).bchatContactDatabase() }
    var selectedItems = mutableSetOf<MessageRecord>()
    private var searchQuery: String? = null
    var visibleMessageViewDelegate: VisibleMessageViewDelegate? = null

    private val updateQueue = Channel<String>(1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val contactCache = ConcurrentHashMap<String, Contact>(100)
    private val contactLoadedCache = ConcurrentHashMap<String, Boolean>(100)
    @WorkerThread
    private fun getSenderInfo(sender: String): Contact? {
        return contactDB.getContactWithBchatID(sender)
    }

    var lastSentMessageId: Long? = null
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    init {
        lifecycleCoroutineScope.launch(IO) {
            while (isActive) {
                val item = updateQueue.receive()
                val contact = getSenderInfo(item) ?: continue
                contactCache[item] = contact
                contactLoadedCache[item] = true
            }
        }
        setHasStableIds(true)
    }

    class VisibleMessageViewHolder(val view: VisibleMessageView) : ViewHolder(view)
    class ControlMessageViewHolder(val view: ControlMessageView) : ViewHolder(view)

    override fun getItemViewType(cursor: Cursor): Int {
        val message = getMessage(cursor)!!
        return when {
            message.isControlMessage -> VIEW_TYPE_CONTROL
            else -> VIEW_TYPE_VISIBLE
        }
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_VISIBLE -> VisibleMessageViewHolder(VisibleMessageView(context))
            VIEW_TYPE_CONTROL -> ControlMessageViewHolder(ControlMessageView(context))
            else -> throw IllegalStateException("Unexpected view type: $viewType.")
        }
    }

    override fun getItemId(cursor: Cursor): Long {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MmsSmsColumns.ID))
        val transport = cursor.getString(cursor.getColumnIndexOrThrow(MmsSmsDatabase.TRANSPORT))
        return if (transport == MmsSmsDatabase.SMS_TRANSPORT) id else -id
    }

    override fun onBindItemViewHolder(viewHolder: ViewHolder, cursor: Cursor, position: Int) {
        val message = getMessage(cursor)!!
        val messageBefore = getMessageBefore(position, cursor)
        val messageAfter = getMessageAfter(position, cursor)
        when (viewHolder) {
            is VisibleMessageViewHolder -> {
                val visibleMessageView = viewHolder.view
                val isSelected = selectedItems.contains(message)
                visibleMessageView.isMessageSelected = isSelected
                visibleMessageView.indexInAdapter = position
                val senderId = message.individualRecipient.address.serialize()
                updateQueue.trySend(senderId)
                if (contactCache[senderId] == null && !contactLoadedCache.getOrDefault(senderId, false)) {
                    getSenderInfo(senderId)?.let { contact ->
                        contactCache[senderId] = contact
                    }
                }
                val contact = contactCache[senderId]
                visibleMessageView.bind(message, threadRecipient = threadRecipientProvider!!, messageBefore, messageAfter, glide, searchQuery, contact, senderId, onAttachmentNeedsDownload,{ selectedItems.isNotEmpty() }, visibleMessageViewDelegate, position, searchViewModel, lastSentMessageId)
                if (!message.isDeleted) {
                    visibleMessageView.onPress = { event -> onItemPress(message, position, visibleMessageView, event) }
                    visibleMessageView.onSwipeToReply = { onItemSwipeToReply(message, position) }
                    visibleMessageView.onLongPress = {
                        ViewUtil.hideKeyboard(context,visibleMessageView)
                        onItemLongPress(message, position, visibleMessageView)
                    }
                } else {
                    visibleMessageView.onPress = null
                    visibleMessageView.onSwipeToReply = null
                    // you can long press on "marked as deleted" messages
                    visibleMessageView.onLongPress =
                        { onItemLongPress(message, position, visibleMessageView) }
                }
            }
            is ControlMessageViewHolder -> {
                viewHolder.view.bind(
                    message = message,
                    previous = messageBefore,
                    longPress = { onItemLongPress(message, position, viewHolder.view) }
                )
                if (message.isCallLog && message.isFirstMissedCall) {
                    viewHolder.view.setOnClickListener {
                        val factory = LayoutInflater.from(context)
                        val callMissedDialogView: View = factory.inflate(R.layout.call_missed_dialog_box, null)
                        val callMissedDialog = AlertDialog.Builder(context).create()
                        callMissedDialog.window?.setBackgroundDrawableResource(R.color.transparent)
                        callMissedDialog.setView(callMissedDialogView)
                        val color = ResourcesCompat.getColor(context.resources, R.color.text_old_green, context.theme)
                        val description = callMissedDialogView.findViewById<TextView>(R.id.messageTextView)
                        description.text = context.getString(R.string.call_missed_description,
                            if(message.recipient.name != null) "\"${message.recipient.name}\"" else "\"${message.recipient.address}\"")

                        val recipientName = SpannableStringBuilder(description.text)
                        val startIndex =message.recipient.name?.let { it1 ->
                            description.text.indexOf(
                                it1
                            )
                        }
                        var endIndex = 0
                        if(startIndex != -1){
                            if (startIndex != null) {
                                endIndex = startIndex + message.recipient.name!!.length
                            }
                        }
                        if (startIndex != null) {
                            recipientName.setSpan(ForegroundColorSpan(color), startIndex -1, endIndex +1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        if (startIndex != null) {
                            recipientName.setSpan(StyleSpan(Typeface.BOLD), startIndex -1, endIndex +1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        description.text = recipientName
                        val okButton =  callMissedDialogView.findViewById<Button>(R.id.missedCallOkButton)
                        okButton.setOnClickListener {
                            val intent = Intent(context, PrivacySettingsActivity::class.java)
                            context.startActivity(intent)
                            callMissedDialog.dismiss()
                        }
                        callMissedDialog.show()
                    }
                } else {
                    viewHolder.view.setOnClickListener(null)
                }
            }
        }
    }

    fun toggleSelection(message: MessageRecord, position: Int) {
        if (selectedItems.contains(message)) selectedItems.remove(message) else selectedItems.add(message)
        notifyItemChanged(position)
    }

    override fun onItemViewRecycled(viewHolder: ViewHolder?) {
        when (viewHolder) {
            is VisibleMessageViewHolder -> viewHolder.view.recycle()
            is ControlMessageViewHolder -> viewHolder.view.recycle()
        }
        super.onItemViewRecycled(viewHolder)
    }

    private fun getMessage(cursor: Cursor): MessageRecord? {
        return messageDB.readerFor(cursor).current
    }

    private fun getMessageBefore(position: Int, cursor: Cursor): MessageRecord? {
        // The message that's visually before the current one is actually after the current
        // one for the cursor because the layout is reversed
        if (!cursor.moveToPosition(position + 1)) { return null }
        return messageDB.readerFor(cursor).current
    }

    private fun getMessageAfter(position: Int, cursor: Cursor): MessageRecord? {
        // The message that's visually after the current one is actually before the current
        // one for the cursor because the layout is reversed
        if (!cursor.moveToPosition(position - 1)) { return null }
        return messageDB.readerFor(cursor).current
    }

    override fun changeCursor(cursor: Cursor?) {
        super.changeCursor(cursor)
        val toRemove = mutableSetOf<MessageRecord>()
        val toDeselect = mutableSetOf<Pair<Int, MessageRecord>>()
        for (selected in selectedItems) {
            val position = getItemPositionForTimestamp(selected.timestamp)
            if (position == null || position == -1) {
                toRemove += selected
            } else {
                val item = cursor?.let {
                    it.moveToPosition(position)
                    getMessage(it)
                }
                if (item == null || item.isDeleted) {
                    toDeselect += position to selected
                }
            }
        }
        selectedItems -= toRemove
        toDeselect.iterator().forEach { (pos, record) ->
            onDeselect(record, pos)
        }
    }

    fun findLastSeenItemPosition(lastSeenTimestamp: Long): Int? {
        val cursor = this.cursor
        if (lastSeenTimestamp <= 0L || cursor == null || !isActiveCursor) return null
        for (i in 0 until itemCount) {
            cursor.moveToPosition(i)
            val message = messageDB.readerFor(cursor).current
            if (message.isOutgoing || message.dateReceived <= lastSeenTimestamp) { return i }
        }
        return null
    }

    fun getItemPositionForTimestamp(timestamp: Long): Int? {
        val cursor = this.cursor
        if (timestamp <= 0L || cursor == null || !isActiveCursor) return null
        for (i in 0 until itemCount) {
            cursor.moveToPosition(i)
            val message = messageDB.readerFor(cursor).current
            if (message.dateSent == timestamp) { return i }
        }
        return null
    }

    fun onSearchQueryUpdated(query: String?) {
        this.searchQuery = query
        val cursor = this.cursor
        if(!query.isNullOrEmpty() && cursor !=null && isActiveCursor) {
            for (i in 0 until itemCount) {
                cursor.moveToPosition(i)
                val message = messageDB.readerFor(cursor).current
                if (message.body.lowercase(Locale.US).contains(searchQuery.toString().lowercase(Locale.US))) {
                    notifyItemChanged(i)
                } else {
                    notifyDataSetChanged()
                }
            }
        }else{
            notifyDataSetChanged()
        }
    }
    companion object {
        private const val VIEW_TYPE_VISIBLE = 1
        private const val VIEW_TYPE_CONTROL = 2
    }
}
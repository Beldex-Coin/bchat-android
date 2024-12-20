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
import android.util.Log
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.WorkerThread
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.getOrDefault
import androidx.core.util.set
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.beldex.libbchat.messaging.contacts.Contact
import io.beldex.bchat.conversation.v2.messages.ControlMessageView
import io.beldex.bchat.conversation.v2.messages.VisibleMessageContentViewDelegate
import io.beldex.bchat.conversation.v2.messages.VisibleMessageView
import io.beldex.bchat.database.CursorRecyclerViewAdapter
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.mms.GlideRequests
import io.beldex.bchat.preferences.PrivacySettingsActivity
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.messages.VisibleMessageViewDelegate
import io.beldex.bchat.databinding.ViewVisibleMessageBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class ConversationAdapter(
    context: Context,
    cursor: Cursor,
    private val onItemPress: (MessageRecord, Int, VisibleMessageView, MotionEvent) -> Unit,
    private val onItemSwipeToReply: (MessageRecord, Int) -> Unit,
    private val onItemLongPress: (MessageRecord, Int, View) -> Unit,
    private val glide: GlideRequests,
    private val onDeselect: (MessageRecord, Int) -> Unit,
    private val onAttachmentNeedsDownload: (Long, Long) -> Unit, lifecycleCoroutineScope: LifecycleCoroutineScope,
    var isAdmin: Boolean = false

) : CursorRecyclerViewAdapter<ViewHolder>(context, cursor) {
    private val messageDB by lazy { DatabaseComponent.get(context).mmsSmsDatabase() }
    private val contactDB by lazy { DatabaseComponent.get(context).bchatContactDatabase() }
    var selectedItems = mutableSetOf<MessageRecord>()
    private var searchQuery: String? = null
    var visibleMessageViewDelegate: VisibleMessageViewDelegate? = null

    private val updateQueue = Channel<String>(1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val contactCache = SparseArray<Contact>(100)
    private val contactLoadedCache = SparseBooleanArray(100)
    init {
        lifecycleCoroutineScope.launch(IO) {
            while (isActive) {
                val item = updateQueue.receive()
                val contact = getSenderInfo(item) ?: continue
                    contactCache[item.hashCode()] = contact
                contactLoadedCache[item.hashCode()] = true
            }
        }
    }

    @WorkerThread
    private fun getSenderInfo(sender: String): Contact? {
        return contactDB.getContactWithBchatID(sender)
    }

    sealed class ViewType(val rawValue: Int) {
        object Visible : ViewType(0)
        object Control : ViewType(1)

        companion object {

            val allValues: Map<Int, ViewType> get() = mapOf(
                Visible.rawValue to Visible,
                Control.rawValue to Control
            )
        }
    }

    class VisibleMessageViewHolder(val view: View) : ViewHolder(view)
    class ControlMessageViewHolder(val view: ControlMessageView) : ViewHolder(view)

    override fun getItemViewType(cursor: Cursor): Int {
        val message = getMessage(cursor)!!
        if (message.isControlMessage) { return ViewType.Control.rawValue }
        return ViewType.Visible.rawValue
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        @Suppress("NAME_SHADOWING")
        val viewType = ViewType.allValues[viewType]
        return when (viewType) {
            ViewType.Visible -> VisibleMessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_visible_message, parent, false))
            ViewType.Control -> ControlMessageViewHolder(ControlMessageView(context))
            else -> throw IllegalStateException("Unexpected view type: $viewType.")
        }
    }

    override fun onBindItemViewHolder(viewHolder: ViewHolder, cursor: Cursor) {
        val message = getMessage(cursor)!!
        val position = viewHolder.adapterPosition
        val messageBefore = getMessageBefore(position, cursor)
        when (viewHolder) {
            is VisibleMessageViewHolder -> {
                val visibleMessageView = ViewVisibleMessageBinding.bind(viewHolder.view).visibleMessageView
                val isSelected = selectedItems.contains(message)
                visibleMessageView.snIsSelected = isSelected
                visibleMessageView.indexInAdapter = position
                val senderId = message.individualRecipient.address.serialize()
                val senderIdHash = senderId.hashCode()
                updateQueue.trySend(senderId)
                if (contactCache[senderIdHash] == null && !contactLoadedCache.getOrDefault(senderIdHash, false)) {
                    getSenderInfo(senderId)?.let { contact ->
                            contactCache[senderIdHash] = contact
                    }
                }
                val contact = contactCache[senderIdHash]

                visibleMessageView.bind(message, messageBefore, getMessageAfter(position, cursor), glide, searchQuery, contact, senderId,onAttachmentNeedsDownload,visibleMessageViewDelegate)
                if (!message.isDeleted) {
                    visibleMessageView.onPress = { event -> onItemPress(message, viewHolder.adapterPosition, visibleMessageView, event) }
                    visibleMessageView.onSwipeToReply = { onItemSwipeToReply(message, viewHolder.adapterPosition) }
                    visibleMessageView.onLongPress = { onItemLongPress(message, viewHolder.adapterPosition, visibleMessageView) }

                } else {

                    visibleMessageView.onPress = null
                    visibleMessageView.onSwipeToReply = null
                    // you can long press on "marked as deleted" messages
                    visibleMessageView.onLongPress =
                        { onItemLongPress(message, viewHolder.adapterPosition, visibleMessageView) }
                }
            }
            is ControlMessageViewHolder -> {
                viewHolder.view.bind(
                    message = message,
                    previous = messageBefore,
                    longPress = { onItemLongPress(message, viewHolder.adapterPosition, viewHolder.view) }
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
            is VisibleMessageViewHolder -> viewHolder.view.findViewById<VisibleMessageView>(R.id.visibleMessageView).recycle()
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
                val item = getMessage(getCursorAtPositionOrThrow(position))
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
                }
            }
        }else{
            notifyDataSetChanged()
        }
    }
}
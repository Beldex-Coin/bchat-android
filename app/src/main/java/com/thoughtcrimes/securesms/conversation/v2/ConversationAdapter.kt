package com.thoughtcrimes.securesms.conversation.v2

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.view.MotionEvent
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.thoughtcrimes.securesms.conversation.v2.messages.VisibleMessageContentViewDelegate
import com.thoughtcrimes.securesms.conversation.v2.messages.VisibleMessageView
import com.thoughtcrimes.securesms.conversation.v2.messages.ControlMessageView
import com.thoughtcrimes.securesms.database.CursorRecyclerViewAdapter
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.preferences.PrivacySettingsActivity
import io.beldex.bchat.R

class ConversationAdapter(context: Context, cursor: Cursor, private val onItemPress: (MessageRecord, Int, VisibleMessageView, MotionEvent) -> Unit,
                          private val onItemSwipeToReply: (MessageRecord, Int) -> Unit, private val onItemLongPress: (MessageRecord, Int) -> Unit,
                          private val glide: GlideRequests, private val onDeselect: (MessageRecord, Int) -> Unit)
    : CursorRecyclerViewAdapter<ViewHolder>(context, cursor) {
    private val messageDB = DatabaseComponent.get(context).mmsSmsDatabase()
    var selectedItems = mutableSetOf<MessageRecord>()
    private var searchQuery: String? = null
    var visibleMessageContentViewDelegate: VisibleMessageContentViewDelegate? = null

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

    class VisibleMessageViewHolder(val view: VisibleMessageView) : ViewHolder(view)
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
            ViewType.Visible -> VisibleMessageViewHolder(VisibleMessageView(context))
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
                val view = viewHolder.view
                val isSelected = selectedItems.contains(message)
                view.snIsSelected = isSelected
                view.indexInAdapter = position

                view.bind(message, messageBefore, getMessageAfter(position, cursor), glide, searchQuery)
                if (!message.isDeleted) {
                    view.onPress = { event -> onItemPress(message, viewHolder.adapterPosition, view, event) }
                    view.onSwipeToReply = { onItemSwipeToReply(message, viewHolder.adapterPosition) }
                    view.onLongPress = { onItemLongPress(message, viewHolder.adapterPosition) }

                } else {

                    view.onPress = null
                    view.onSwipeToReply = null
                    view.onLongPress = null
                }
                view.contentViewDelegate = visibleMessageContentViewDelegate
            }
            is ControlMessageViewHolder -> {
                viewHolder.view.bind(message, messageBefore)
                if (message.isCallLog && message.isFirstMissedCall) {
                    viewHolder.view.setOnClickListener {
                        AlertDialog.Builder(context,R.style.BChatAlertDialog_Call_Missed)
                            .setTitle(R.string.CallNotificationBuilder_first_call_title)
                            .setMessage(R.string.CallNotificationBuilder_first_call_message)
                            .setPositiveButton(R.string.activity_settings_title) { _, _ ->
                                val intent = Intent(context, PrivacySettingsActivity::class.java)
                                context.startActivity(intent)
                            }
                            .setNeutralButton(R.string.cancel) { d, _ ->
                                d.dismiss()
                            }
                            .show()
                    }
                } else {
                    viewHolder.view.setOnClickListener(null)
                }
            }
        }
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

    fun toggleSelection(message: MessageRecord, position: Int) {
        if (selectedItems.contains(message)) selectedItems.remove(message) else selectedItems.add(message)
        notifyItemChanged(position)
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
        notifyDataSetChanged()
    }
}
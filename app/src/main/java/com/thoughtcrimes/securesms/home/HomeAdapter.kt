package com.thoughtcrimes.securesms.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.util.DateUtils
import io.beldex.bchat.databinding.ViewMessageRequestBannerBinding
import java.util.Locale

class HomeAdapter(private val context: Context, private val listener: ConversationClickListener,
    private val threadDB: ThreadDatabase
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ListUpdateCallback {

    companion object {
        private const val HEADER = 0
        private const val ITEM = 1
    }

    var header: View? = null

    private var hasMessageRequests: Boolean = false
    private var _data: List<ThreadRecord> = emptyList()
    var data: List<ThreadRecord>
        get() = _data.toList()
        set(newData) {
            val previousData = _data.toList()
            val diff = HomeDiffUtil(previousData, newData, context, hasMessageRequests)
            val diffResult = DiffUtil.calculateDiff(diff)
            _data = newData
            diffResult.dispatchUpdatesTo(this as ListUpdateCallback)
        }

    fun hasHeaderView(): Boolean = header != null

    private val headerCount: Int
        get() = if (header == null) 0 else 1

    override fun onInserted(position: Int, count: Int) {
        notifyItemRangeInserted(position + headerCount, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        notifyItemRangeRemoved(position + headerCount, count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition + headerCount, toPosition + headerCount)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        notifyItemRangeChanged(position + headerCount, count, payload)
    }

    override fun getItemId(position: Int): Long  {
        if (hasMessageRequests && position == 0) return NO_ID
        val offsetPosition = if (hasHeaderView()) position-1 else position
        return _data[offsetPosition].threadId
    }

    lateinit var glide: GlideRequests
    var typingThreadIDs = setOf<Long>()
        set(value) {
            if (field == value) { return }

            field = value
            // TODO: replace this with a diffed update or a partial change set with payloads
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            HEADER -> {
                val binding = ViewMessageRequestBannerBinding.inflate(LayoutInflater.from(context))
                HeaderFooterViewHolder(binding)
            }
            ITEM -> {
                val view = ConversationView(context)
                view.setOnClickListener { view.thread?.let { listener.onConversationClick(it) } }
                view.setOnLongClickListener {
                    view.thread?.let { listener.onLongConversationClick(it) }
                    true
                }
                ViewHolder(view)
            }
            else -> throw Exception("viewType $viewType isn't valid")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0 && hasMessageRequests) {
            with((holder as HeaderFooterViewHolder).view) {
                val item = data[position]
                unreadCountTextView.text = item.messageRequestCount.toString()
                timestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(
                    context,
                    Locale.getDefault(),
                    threadDB.latestUnapprovedConversationTimestamp
                )
                root.setOnClickListener { listener.showMessageRequests() }
                expandMessageRequest.setOnClickListener { listener.showMessageRequests() }
                root.setOnLongClickListener { listener.hideMessageRequests(); true }
                root.layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
        } else {
            val offset = if (hasHeaderView()) position - 1 else position
            val thread = data[position]
            val isTyping = typingThreadIDs.contains(thread.threadId)
            (holder as ViewHolder).view.bind(thread, isTyping, glide)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ViewHolder) {
            holder.view.recycle()
        } else {
            super.onViewRecycled(holder)
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0 && hasMessageRequests) HEADER
        else ITEM

    override fun getItemCount(): Int = data.size

    class ViewHolder(val view: ConversationView) : RecyclerView.ViewHolder(view)

    class HeaderFooterViewHolder(val view: ViewMessageRequestBannerBinding) : RecyclerView.ViewHolder(view.root)

    fun setHasMessageRequestCount(hasRequests: Boolean) {
        hasMessageRequests = hasRequests
    }

}
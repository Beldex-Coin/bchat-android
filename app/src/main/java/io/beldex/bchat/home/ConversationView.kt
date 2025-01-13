package io.beldex.bchat.home

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities.highlightMentions
import io.beldex.bchat.database.RecipientDatabase.NOTIFY_TYPE_ALL
import io.beldex.bchat.database.RecipientDatabase.NOTIFY_TYPE_NONE
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.mms.GlideRequests
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewConversationBinding
import java.util.Locale

class ConversationView : LinearLayout {
    private lateinit var binding: ViewConversationBinding
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    var thread: ThreadRecord? = null
    var isReportIssueID: Boolean = false
    private val reportIssueBChatID = BuildConfig.REPORT_ISSUE_ID

    // region Lifecycle
    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        binding = ViewConversationBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }
    // endregion

    // region Updating
    fun bind(thread: ThreadRecord, isTyping: Boolean, glide: GlideRequests) {
        this.thread = thread
        val recipient = thread.recipient
        val isMuted = recipient.isMuted || recipient.notifyType != NOTIFY_TYPE_ALL
        if (thread.isPinned) {
            binding.contentView.apply {
                background = ContextCompat.getDrawable(context,R.drawable.unread_message_chat_background)
                val params = layoutParams as FrameLayout.LayoutParams
                params.setMargins(0, 16, 16, 16)
                layoutParams = params
                if(isMuted && thread.unreadCount == 0 && !thread.isRead){
                    binding.muteIcon.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        rightMargin=50
                    }
                }

            }
            binding.pinnedViewContainer.isVisible = thread.isPinned
        } else {
            binding.pinnedViewContainer.isVisible = thread.isPinned
            binding.contentView.apply {
                background = null
                val params = layoutParams as FrameLayout.LayoutParams
                params.setMargins(0, 0, 0, 0)
                layoutParams = params
            }
        }
        if(isMuted && thread.unreadCount == 0 && !thread.isRead && !thread.isPinned){
            binding.muteIcon.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightMargin=70
            }
        }
        binding.profilePictureView.root.glide = glide
        val unreadCount = thread.unreadCount
        if (thread.recipient.isBlocked) {
//            binding.accentView.setBackgroundResource(R.color.destructive)
//            binding.accentView.visibility = View.VISIBLE
        } else {
//            binding.accentView.setBackgroundResource(R.color.accent)
            // Using thread.isRead we can determine if the last message was our own, and display it as 'read' even though previous messages may not be
            // This would also not trigger the disappearing message timer which may or may not be desirable
//            binding.accentView.visibility = if (unreadCount > 0 && !thread.isRead) View.VISIBLE else View.INVISIBLE
        }
        val formattedUnreadCount = if (thread.isRead) {
            null
        } else {
            if (unreadCount < 100) unreadCount.toString() else "99+"
        }
        binding.unreadCountTextView.text = formattedUnreadCount
        val textSize = if (unreadCount < 100) 12.0f else 10.0f
        binding.unreadCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
        binding.unreadCountTextView.setTypeface(Typeface.DEFAULT, if (unreadCount < 100) Typeface.BOLD else Typeface.NORMAL)
        binding.unreadCountIndicator.isVisible = (unreadCount != 0 && !thread.isRead)
        val senderDisplayName = getUserDisplayName(thread.recipient)
                ?: thread.recipient.address.toString()
        val recipientName : String=senderDisplayName.substring(0, 1).uppercase(Locale.ROOT) + senderDisplayName.substring(1).lowercase(Locale.ROOT)

        binding.conversationViewDisplayNameTextView.text = recipientName
        binding.timestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(context, Locale.getDefault(), thread.date)
        if(unreadCount !=0 && !thread.isRead) {
            binding.timestampTextView.setTextColor(context.getColor(R.color.text_green))
        } else{
            binding.timestampTextView.setTextColor(context.getColor(R.color.received_quoted_text_color))
        }
        binding.muteIcon.isVisible = isMuted
        val drawableRes = if (recipient.isMuted || recipient.notifyType == NOTIFY_TYPE_NONE) {
            R.drawable.ic_mute_home
        } else {
            R.drawable.ic_mention_home
        }
        binding.muteIcon.setImageResource(drawableRes)
        val rawSnippet = thread.getDisplayBody(context)
        val snippet = highlightMentions(rawSnippet,thread.threadId, context)

        //SteveJosephh21-17 - if
        /*val mmsSmsDatabase = get(context).mmsSmsDatabase()
        var reader: MmsSmsDatabase.Reader? = null
        try {
            reader = mmsSmsDatabase.readerFor(mmsSmsDatabase.getConversationSnippet(thread.threadId))
            var record: MessageRecord? = null
            if (reader != null) {
                record = reader.next
                while (record != null && record.isDeleted) {
                    record = reader.next
                }
                Log.d("ThreadDatabase- 1", "" + record)
                if(record==null){
                    binding.snippetTextView.text = "This message has been deleted"
                }else{
                    binding.snippetTextView.text = snippet
                }
            }
        } finally {
            if (reader != null)
            reader.close()
        }*/
        //Important - else
        binding.snippetTextView.text = snippet

        //binding.snippetTextView.typeface = if (unreadCount > 0 && !thread.isRead) Typeface.DEFAULT else Typeface.DEFAULT
        binding.snippetTextView.visibility = if (isTyping) View.GONE else View.VISIBLE
        if (isTyping) {
            binding.typingIndicatorView.root.startAnimation()
        } else {
            binding.typingIndicatorView.root.stopAnimation()
        }
        binding.typingIndicatorView.root.visibility = if (isTyping) View.VISIBLE else View.GONE
//        binding.statusIndicatorImageView.visibility = View.VISIBLE
//        when {
//            !thread.isOutgoing -> binding.statusIndicatorImageView.visibility = View.GONE
//            thread.isPending -> binding.statusIndicatorImageView.setImageResource(R.drawable.ic_circle_dot_dot_dot)
//            thread.isRead -> binding.statusIndicatorImageView.setImageResource(R.drawable.ic_filled_circle_check)
//            thread.isSent -> binding.statusIndicatorImageView.setImageResource(R.drawable.ic_circle_check)
//            thread.isFailed -> {
//                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_error)?.mutate()
//                drawable?.setTint(ContextCompat.getColor(context, R.color.destructive))
//                binding.statusIndicatorImageView.setImageDrawable(drawable)
//            }
//
//            else -> binding.statusIndicatorImageView.setImageResource(R.drawable.ic_circle_check)
//
//        }
        binding.profilePictureView.root.update(thread.recipient)
    }

    fun recycle() {
        binding.profilePictureView.root.recycle()
    }

    private fun getUserDisplayName(recipient: Recipient): String? {
        isReportIssueID = recipient.address.toString() == reportIssueBChatID
        return when {
            recipient.isLocalNumber -> {
                context.getString(R.string.note_to_self)
            }
            isReportIssueID -> {
                context.getString(R.string.report_issue)
            }
            else -> {
                recipient.name // Internally uses the Contact API
            }
        }
    }

    private fun getActualDPsFromPixels(context: Context, pixels: Int): Float {
        val resources = context.resources
        return pixels / (resources.displayMetrics.densityDpi / 160f)
    }
    // endregion
}

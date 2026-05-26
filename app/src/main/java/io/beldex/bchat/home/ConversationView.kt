package io.beldex.bchat.home

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.RequestManager
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter
import io.beldex.bchat.conversation.v2.contact_sharing.flattenData
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities.highlightMentionsSpannableString
import io.beldex.bchat.database.RecipientDatabase.NOTIFY_TYPE_ALL
import io.beldex.bchat.database.RecipientDatabase.NOTIFY_TYPE_NONE
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.databinding.ViewConversationBinding
import io.beldex.bchat.textformatter.TextFormatter
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.isSharedContact
import io.beldex.bchat.util.shortNameAndAddress
import java.util.Locale

class ConversationView : LinearLayout {
    private lateinit var binding: ViewConversationBinding
    var thread: ThreadRecord? = null
    var isReportIssueID: Boolean = false
    private val reportIssueBChatID = BuildConfig.REPORT_ISSUE_ID

    val attachmentRegex = Regex("""📷\s*Attachment:""")

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
    fun bind(thread: ThreadRecord, isTyping: Boolean, glide: RequestManager) {
        this.thread = thread
        val recipient = thread.recipient
        val isMuted = recipient.isMuted || recipient.notifyType != NOTIFY_TYPE_ALL
        val unreadCount = thread.unreadCount
        if (thread.isPinned) {
            binding.contentView.apply {
                background = ContextCompat.getDrawable(context,R.drawable.unread_message_chat_background)
                val params = layoutParams as FrameLayout.LayoutParams
                params.setMargins(0, 16, 16, 16)
                layoutParams = params

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
        val margin = when {
            isMuted && unreadCount == 0 && !thread.isRead && thread.isPinned -> 50
            isMuted && unreadCount == 0 && !thread.isRead && !thread.isPinned -> 70
            else -> 16
        }

        binding.muteIcon.updateLayoutParams<ConstraintLayout.LayoutParams> {
            rightMargin = margin
        }
        binding.profilePictureView.root.glide = glide

        val formattedUnreadCount = if (unreadCount < 100) unreadCount.toString() else "99+"
        binding.unreadCountTextView.text = formattedUnreadCount
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
        if (thread.isSharedContact || isSharedContact(thread.body)) {
            binding.contactView.visibility = VISIBLE
            binding.snippetTextViewLayout.visibility = GONE

            UpdateMessageData.fromJSON(thread.body)?.let {
                val data = it.kind as UpdateMessageData.Kind.SharedContact
                val addresses = flattenData(data.address)
                val names = flattenData(data.name).ifEmpty { addresses }
                val displayName = when(names.size) {
                    0 -> "No Name"
                    1 -> names.first().capitalizeFirstLetter()
                    2 -> "${shortNameAndAddress(names[0],addresses[0])} and ${names.size - 1} other"
                    else -> "${shortNameAndAddress(names.first(), addresses.first())} and ${names.size - 1} others"
                }
                binding.contactName.text = displayName
            }
        } else {
            binding.contactView.visibility = GONE
            binding.snippetTextViewLayout.visibility = VISIBLE

            val rawSnippet = thread.getDisplayBody(context)
            val snippet = if (thread.isGroupUpdateMessage) {
                rawSnippet
            } else {
                // Format basic styles (bold, italic, lists, etc.)
                val formatted = TextFormatter.formatForSentMessage(context, rawSnippet)

                // Apply mentions
                val mentionFormatted = highlightMentionsSpannableString(
                    formatted,
                    thread.threadId,
                    context
                )

                // (optional) apply block quote again for attachments
                val isAttachment = attachmentRegex
                    .containsMatchIn(mentionFormatted.toString())

                if (isAttachment) {
                    TextFormatter.applyInlineBlockQuote(
                        context,
                        mentionFormatted,
                        attachmentRegex
                    )
                }

                mentionFormatted
            }

            binding.snippetTextView.text = snippet

            binding.snippetTextView.visibility = if (isTyping) GONE else VISIBLE
            if (isTyping) {
                binding.typingIndicatorView.root.startAnimation()
            } else {
                binding.typingIndicatorView.root.stopAnimation()
            }
            binding.typingIndicatorView.root.visibility = if (isTyping) VISIBLE else GONE
        }
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
    // endregion
}

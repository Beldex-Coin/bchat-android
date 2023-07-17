package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.text.getSpans
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.AttachmentDownloadJob
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentTransferProgress
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.ThemeUtil
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.database.model.SmsMessageRecord
import com.thoughtcrimes.securesms.mms.GlideRequests
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.thoughtcrimes.securesms.conversation.v2.ModalUrlBottomSheet
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionUtilities
import com.thoughtcrimes.securesms.conversation.v2.utilities.ModalURLSpan
import com.thoughtcrimes.securesms.conversation.v2.utilities.TextUtilities.getIntersectedModalSpans
import com.thoughtcrimes.securesms.database.model.MmsMessageRecord
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.mms.PartAuthority
import com.thoughtcrimes.securesms.util.*
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewVisibleMessageContentBinding
import java.util.*
import kotlin.math.roundToInt

class VisibleMessageContentView : LinearLayout {
    private lateinit var binding: ViewVisibleMessageContentBinding
    var onContentClick: MutableList<((event: MotionEvent) -> Unit)> = mutableListOf()
    var onContentDoubleTap: (() -> Unit)? = null
    var delegate: VisibleMessageContentViewDelegate? = null
    var indexInAdapter: Int = -1

    // region Lifecycle
    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize()
    }

    private fun initialize() {
        binding = ViewVisibleMessageContentBinding.inflate(LayoutInflater.from(context), this, true)
    }
    // endregion

    // region Updating
    fun bind(
        message: MessageRecord,
        isStartOfMessageCluster: Boolean,
        isEndOfMessageCluster: Boolean,
        glide: GlideRequests,
        thread: Recipient,
        searchQuery: String?,
        contactIsTrusted: Boolean
    ) {
        // Background
        val background = getBackground(message.isOutgoing, isStartOfMessageCluster, isEndOfMessageCluster)
        val colorID = if (message.isOutgoing) {
            if (message.isFailed) {
                R.attr.message_sent_background_transparent_color
            } else {
                R.attr.message_sent_background_color
            }
        } else {
            if(message.isPayment){
                R.attr.payment_message_received_background_color
            }else {
                R.attr.message_received_background_color
            }
        }
        val color = ThemeUtil.getThemedColor(context, colorID)
        val filter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color,
            BlendModeCompat.SRC_IN
        )
        background.colorFilter = filter
        binding.contentParent.background = background

        val onlyBodyMessage = message is SmsMessageRecord
        val mediaThumbnailMessage =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.thumbnailSlide != null

        // reset visibilities / containers
        onContentClick.clear()
        binding.albumThumbnailView.clearViews()
        onContentDoubleTap = null

        if (message.isDeleted) {
            binding.deletedMessageView.root.isVisible = true
            binding.deletedMessageView.root.bind(
                message,
                VisibleMessageContentView.getTextColor(context, message)
            )
            return
        } else {
            binding.deletedMessageView.root.isVisible = false
        }

        // clear the
        binding.bodyTextView.text = ""

        binding.quoteView.root.isVisible = message is MmsMessageRecord && message.quote != null

        binding.linkPreviewView.isVisible =
            message is MmsMessageRecord && message.linkPreviews.isNotEmpty()
        binding.linkPreviewView.bodyTextView = binding.bodyTextView


        binding.untrustedView.root.isVisible =
            !contactIsTrusted && message is MmsMessageRecord && message.quote == null
        binding.voiceMessageView.root.isVisible =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.audioSlide != null
        binding.documentView.root.isVisible =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.documentSlide != null
        binding.albumThumbnailView.isVisible = mediaThumbnailMessage
        binding.openGroupInvitationView.root.isVisible = message.isOpenGroupInvitation
        //Payment Tag
        binding.paymentCardView.isVisible = message.isPayment

        var hideBody = false

        if (message is MmsMessageRecord && message.quote != null) {
            binding.quoteView.root.isVisible = true
            val quote = message.quote!!
            val quoteText = if (quote.isOriginalMissing) {
                context.getString(R.string.QuoteView_original_missing)
            } else {
                quote.text
            }
            binding.quoteView.root.bind(
                quote.author.toString(), quoteText, quote.attachment, thread,
                message.isOutgoing, message.isOpenGroupInvitation, message.isPayment,
                message.isOutgoing, message.threadId, quote.isOriginalMissing, glide
            )
            onContentClick.add { event ->
                val r = Rect()
                binding.quoteView.root.getGlobalVisibleRect(r)
                if (r.contains(event.rawX.roundToInt(), event.rawY.roundToInt())) {
                    delegate?.scrollToMessageIfPossible(quote.id)
                }
            }
            val hasMedia = message.slideDeck.asAttachments().isNotEmpty()
        }

        if (message is MmsMessageRecord) {
            message.slideDeck.asAttachments().forEach { attach ->
                val dbAttachment = attach as? DatabaseAttachment ?: return@forEach
                val attachmentId = dbAttachment.attachmentId.rowId
                if (attach.transferState == AttachmentTransferProgress.TRANSFER_PROGRESS_PENDING
                    && MessagingModuleConfiguration.shared.storage.getAttachmentUploadJob(attachmentId) == null) {
                    // start download
                    JobQueue.shared.add(AttachmentDownloadJob(attachmentId, dbAttachment.mmsId))
                }
            }
            message.linkPreviews.forEach { preview ->
                val previewThumbnail = preview.getThumbnail().orNull() as? DatabaseAttachment ?: return@forEach
                val attachmentId = previewThumbnail.attachmentId.rowId
                if (previewThumbnail.transferState == AttachmentTransferProgress.TRANSFER_PROGRESS_PENDING
                    && MessagingModuleConfiguration.shared.storage.getAttachmentUploadJob(attachmentId) == null) {
                    JobQueue.shared.add(AttachmentDownloadJob(attachmentId, previewThumbnail.mmsId))
                }
            }
        }

        when {
            message is MmsMessageRecord && message.linkPreviews.isNotEmpty() -> {
                binding.linkPreviewView.bind(
                    message,
                    glide,
                    isStartOfMessageCluster,
                    isEndOfMessageCluster
                )
                onContentClick.add { event -> binding.linkPreviewView.calculateHit(event) }
                // Body text view is inside the link preview for layout convenience
            }
            message is MmsMessageRecord && message.slideDeck.audioSlide != null -> {
                hideBody = true
                // Audio attachment
                if (contactIsTrusted || message.isOutgoing) {
                    binding.voiceMessageView.root.indexInAdapter = indexInAdapter
                    binding.voiceMessageView.root.delegate = context as? HomeActivity
                    binding.voiceMessageView.root.bind(
                        message,
                        isStartOfMessageCluster,
                        isEndOfMessageCluster
                    )
                    // We have to use onContentClick (rather than a click listener directly on the voice
                    // message view) so as to not interfere with all the other gestures.
                    onContentClick.add { binding.voiceMessageView.root.togglePlayback() }
                    onContentDoubleTap = { binding.voiceMessageView.root.handleDoubleTap() }
                } else {
                    // TODO: move this out to its own area
                    binding.untrustedView.root.bind(
                        UntrustedAttachmentView.AttachmentType.AUDIO,
                        VisibleMessageContentView.getTextColor(context, message)
                    )
                    onContentClick.add { binding.untrustedView.root.showTrustDialog(message.individualRecipient) }
                }
            }
            message is MmsMessageRecord && message.slideDeck.documentSlide != null -> {
                hideBody = true
                // Document attachment
                if (contactIsTrusted || message.isOutgoing) {
                    binding.documentView.root.bind(
                        message,
                        VisibleMessageContentView.getTextColor(context, message)
                    )
                    //New Line
                    binding.documentView.root.setOnClickListener {
                        if (message.slideDeck.documentSlide!!.uri != null) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.setDataAndType(
                                PartAuthority.getAttachmentPublicUri(message.slideDeck.documentSlide!!.uri),
                                message.slideDeck.documentSlide!!.contentType
                            )
                            try {
                                context.startActivity(intent)
                            } catch (anfe: ActivityNotFoundException) {
                                Log.w(
                                    StickyHeaderGridLayoutManager.TAG,
                                    "No activity existed to view the media."
                                )
                                Toast.makeText(
                                    context,
                                    R.string.ConversationItem_unable_to_open_media,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Please wait until file downloaded",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    binding.untrustedView.root.bind(
                        UntrustedAttachmentView.AttachmentType.DOCUMENT,
                        VisibleMessageContentView.getTextColor(context, message)
                    )
                    onContentClick.add { binding.untrustedView.root.showTrustDialog(message.individualRecipient) }
                }
            }
            message is MmsMessageRecord && message.slideDeck.asAttachments().isNotEmpty() -> {
                /*
             *    Images / Video attachment
             */
                if (contactIsTrusted || message.isOutgoing) {
                    // isStart and isEnd of cluster needed for calculating the mask for full bubble image groups
                    // bind after add view because views are inflated and calculated during bind
                    binding.albumThumbnailView.bind(
                        glideRequests = glide,
                        message = message,
                        isStart = isStartOfMessageCluster,
                        isEnd = isEndOfMessageCluster
                    )
                    val layoutParams = binding.albumThumbnailView.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.horizontalBias = if (message.isOutgoing) 1f else 0f
                    binding.albumThumbnailView.layoutParams = layoutParams
                    onContentClick.add { event ->
                        binding.albumThumbnailView.calculateHitObject(event, message, thread)
                    }
                } else {
                    hideBody = true
                    binding.albumThumbnailView.clearViews()
                    binding.untrustedView.root.bind(
                        UntrustedAttachmentView.AttachmentType.MEDIA,
                        VisibleMessageContentView.getTextColor(context, message)
                    )
                    onContentClick.add { binding.untrustedView.root.showTrustDialog(message.individualRecipient) }
                }
            }
            message.isOpenGroupInvitation -> {
                hideBody = true
                binding.openGroupInvitationView.root.bind(
                    message,
                    VisibleMessageContentView.getTextColor(context, message)
                )
                onContentClick.add { binding.openGroupInvitationView.root.joinOpenGroup() }
            }
            message.isPayment -> { //Payment Tag
                hideBody = true
                binding.paymentCardView.bind(
                    message,
                    VisibleMessageContentView.getTextColor(context, message)
                )
                //onContentClick.add { binding.openGroupInvitationView.joinOpenGroup() }
            }
        }

        binding.bodyTextView.isVisible = message.body.isNotEmpty() && !hideBody

        val fontSize = TextSecurePreferences.getChatFontSize(context)
        binding.bodyTextView.textSize = fontSize!!.toFloat()

        if (message.body.isNotEmpty() && !hideBody) {
            val color = getTextColor(context, message)
            binding.bodyTextView.setTextColor(color)
            binding.bodyTextView.setLinkTextColor(color)
            val body = getBodySpans(context, message, searchQuery)

            binding.bodyTextView.text = body
            //New Line
            if (binding.bodyTextView.length() > 705) {
                addReadMore(binding.bodyTextView.text.toString(), binding.bodyTextView, message)
            }
            //makeTextViewResizable(binding.bodyTextView, 3, "View More", true);
            onContentClick.add { e: MotionEvent ->
                binding.bodyTextView.getIntersectedModalSpans(e).iterator().forEach { span ->
                    span.onClick(binding.bodyTextView)
                }
            }
        }
        val layoutParams = binding.contentParent.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.horizontalBias = if (message.isOutgoing) 1f else 0f
        binding.contentParent.layoutParams = layoutParams
    }

    private fun ViewVisibleMessageContentBinding.barrierViewsGone(): Boolean =
        listOf<View>(
            albumThumbnailView,
            linkPreviewView,
            voiceMessageView.root,
            quoteView.root
        ).none { it.isVisible }

    private fun getBackground(
        isOutgoing: Boolean,
        isStartOfMessageCluster: Boolean,
        isEndOfMessageCluster: Boolean
    ): Drawable {
        val isSingleMessage = (isStartOfMessageCluster && isEndOfMessageCluster)
        @DrawableRes val backgroundID = when {
            isSingleMessage -> {
                if (isOutgoing) R.drawable.message_bubble_background_sent_alone else R.drawable.message_bubble_background_sent_alone
            }
            isStartOfMessageCluster -> {
                if (isOutgoing) R.drawable.message_bubble_background_sent_alone else R.drawable.message_bubble_background_sent_alone
            }
            isEndOfMessageCluster -> {
                if (isOutgoing) R.drawable.message_bubble_background_sent_alone else R.drawable.message_bubble_background_sent_alone
            }
            else -> {
                if (isOutgoing) R.drawable.message_bubble_background_sent_alone else R.drawable.message_bubble_background_sent_alone
            }
        }
        return ResourcesCompat.getDrawable(resources, backgroundID, context.theme)!!
    }

    //Original Code
    /* private fun getBackground(isOutgoing: Boolean, isStartOfMessageCluster: Boolean, isEndOfMessageCluster: Boolean): Drawable {
         val isSingleMessage = (isStartOfMessageCluster && isEndOfMessageCluster)
         @DrawableRes val backgroundID = when {
             isSingleMessage -> {
                 if (isOutgoing) R.drawable.message_bubble_background_sent_alone else R.drawable.message_bubble_background_received_alone
             }
             isStartOfMessageCluster -> {
                 if (isOutgoing) R.drawable.message_bubble_background_sent_start else R.drawable.message_bubble_background_received_start
             }
             isEndOfMessageCluster -> {
                 if (isOutgoing) R.drawable.message_bubble_background_sent_end else R.drawable.message_bubble_background_received_end
             }
             else -> {
                 if (isOutgoing) R.drawable.message_bubble_background_sent_middle else R.drawable.message_bubble_background_received_middle
             }
         }
         return ResourcesCompat.getDrawable(resources, backgroundID, context.theme)!!
     }*/

    fun recycle() {
        arrayOf(
            binding.deletedMessageView.root,
            binding.untrustedView.root,
            binding.voiceMessageView.root,
            binding.openGroupInvitationView.root,
            binding.paymentCardView, //Payment Tag
            binding.documentView.root,
            binding.quoteView.root,
            binding.linkPreviewView,
            binding.albumThumbnailView,
            binding.bodyTextView
        ).forEach { view:View -> view.isVisible = false }
    }

    fun playVoiceMessage() {
        binding.voiceMessageView.root.togglePlayback()
    }
    // endregion

    // region Convenience
    companion object {

        fun getBodySpans(
            context: Context,
            message: MessageRecord,
            searchQuery: String?
        ): Spannable {
            var body = message.body.toSpannable()

            body = MentionUtilities.highlightMentions(
                body,
                message.isOutgoing,
                message.threadId,
                context
            )
            body = SearchUtil.getHighlightedSpan(Locale.getDefault(),
                { BackgroundColorSpan(Color.WHITE) }, body, searchQuery
            )
            body = SearchUtil.getHighlightedSpan(Locale.getDefault(),
                { ForegroundColorSpan(Color.BLACK) }, body, searchQuery
            )

            Linkify.addLinks(body, Linkify.WEB_URLS)

            // replace URLSpans with ModalURLSpans
            body.getSpans<URLSpan>(0, body.length).toList().forEach { urlSpan ->
                val updatedUrl = urlSpan.url.let { it.toHttpUrlOrNull().toString() }
                val replacementSpan = ModalURLSpan(updatedUrl) { url ->
                    val activity = context as AppCompatActivity
                    ModalUrlBottomSheet(url).show(
                        activity.supportFragmentManager,
                        "Open URL Dialog"
                    )
                }
                val start = body.getSpanStart(urlSpan)
                val end = body.getSpanEnd(urlSpan)
                val flags = body.getSpanFlags(urlSpan)
                body.removeSpan(urlSpan)
                body.setSpan(replacementSpan, start, end, flags)
            }
            return body
        }

        @ColorInt
        fun getTextColor(context: Context, message: MessageRecord): Int {
            val isDayUiMode = UiModeUtilities.isDayUiMode(context)
            val colorID = if (message.isOutgoing) {
                if (isDayUiMode) {
                    if (message.isFailed) {
                        R.color.black
                    } else {
                        R.color.white
                    }
                } else R.color.white
            } else {
                if (isDayUiMode) R.color.black else R.color.white
            }
            return context.resources.getColorWithID(colorID, context.theme)
        }
    }
    // endregion

    //New Line
    private fun addReadMore(text: String, textView: TextView, message: MessageRecord) {
        val ss = SpannableString(text.substring(0, 705) + "... Read more")
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                addReadLess(text, textView, message)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
                val isDayUiMode = UiModeUtilities.isDayUiMode(context)
                ds.color = if (message.isOutgoing) {
                        if (isDayUiMode) {
                            if (message.isFailed) {
                                ContextCompat.getColor(context, R.color.black)
                            } else {
                                ContextCompat.getColor(context, R.color.black)
                            }
                        } else ContextCompat.getColor(context, R.color.chat_id_card_background)
                    } else {
                        if (isDayUiMode) ContextCompat.getColor(
                            context,
                            R.color.send_message_background
                        ) else ContextCompat.getColor(context, R.color.send_message_background)
                    }
               /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ds.color = if (message.isOutgoing) {
                        if (isDayUiMode) {
                            if (message.isFailed) {
                                ContextCompat.getColor(context, R.color.black)
                            } else {
                                ContextCompat.getColor(context, R.color.black)
                            }
                        } else ContextCompat.getColor(context, R.color.chat_id_card_background)
                    } else {
                        if (isDayUiMode) ContextCompat.getColor(
                            context,
                            R.color.send_message_background
                        ) else ContextCompat.getColor(context, R.color.send_message_background)
                    }
                } else {
                    ds.color = if (message.isOutgoing) {
                        if (isDayUiMode) {
                            if (message.isFailed) {
                                ContextCompat.getColor(context, R.color.black)
                            } else {
                                ContextCompat.getColor(context, R.color.black)
                            }
                        } else ContextCompat.getColor(context, R.color.chat_id_card_background)
                    } else {
                        if (isDayUiMode) ContextCompat.getColor(
                            context,
                            R.color.send_message_background
                        ) else ContextCompat.getColor(context, R.color.send_message_background)
                    }
                }*/
            }
        }
        ss.setSpan(clickableSpan, ss.length - 10, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = ss
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun addReadLess(text: String, textView: TextView, message: MessageRecord) {
        val ss = SpannableString("$text Read less")
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                addReadMore(text, textView, message)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
                val isDayUiMode = UiModeUtilities.isDayUiMode(context)
                ds.color = if (message.isOutgoing) {
                    if (isDayUiMode){
                        if(message.isFailed) {
                            ContextCompat.getColor(context,R.color.black)
                        }else{
                            ContextCompat.getColor(context,R.color.black)
                        }
                    }else ContextCompat.getColor(context,R.color.chat_id_card_background)
                } else {
                    if (isDayUiMode) ContextCompat.getColor(context,R.color.send_message_background) else ContextCompat.getColor(context,R.color.send_message_background)
                }
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ds.color = if (message.isOutgoing) {
                        if (isDayUiMode){
                            if(message.isFailed) {
                                ContextCompat.getColor(context,R.color.black)
                            }else{
                                ContextCompat.getColor(context,R.color.black)
                            }
                        }else ContextCompat.getColor(context,R.color.chat_id_card_background)
                    } else {
                        if (isDayUiMode) ContextCompat.getColor(context,R.color.send_message_background) else ContextCompat.getColor(context,R.color.send_message_background)
                    }
                } else {
                    ds.color = if (message.isOutgoing) {
                        if (isDayUiMode){
                            if(message.isFailed) {
                                ContextCompat.getColor(context,R.color.black)
                            }else{
                                ContextCompat.getColor(context,R.color.black)
                            }
                        }else ContextCompat.getColor(context,R.color.chat_id_card_background)
                    } else {
                        if (isDayUiMode) ContextCompat.getColor(context,R.color.send_message_background) else ContextCompat.getColor(context,R.color.send_message_background)
                    }
                }*/
            }
        }
        ss.setSpan(clickableSpan, ss.length - 10, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = ss
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}

interface VisibleMessageContentViewDelegate {

    fun scrollToMessageIfPossible(timestamp: Long)
}
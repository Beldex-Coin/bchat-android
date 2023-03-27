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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.text.getSpans
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
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
        maxWidth: Int,
        thread: Recipient,
        searchQuery: String?,
        contactIsTrusted: Boolean
    ) {
        //New Line
        /* val sender = message.individualRecipient
         val senderBchatID = sender.address.serialize()
         //val threadID = message.threadId
         //val thread = threadDb.getRecipientForThreadId(threadID) ?: return
         val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(senderBchatID)
         val isGroupThread = thread.isGroupRecipient
         // Show profile picture and sender name if this is a group thread AND
         // the message is incoming
         if (isGroupThread && !message.isOutgoing) {
             binding.senderNameTextView.isVisible = isStartOfMessageCluster
             val context = if (thread.isOpenGroupRecipient) Contact.ContactContext.OPEN_GROUP else Contact.ContactContext.REGULAR
             binding.senderNameTextView.text = contact?.displayName(context) ?: senderBchatID
         } else {
             binding.senderNameTextView.visibility = View.GONE
         }*/
        // Background
        val background =
            getBackground(message.isOutgoing, isStartOfMessageCluster, isEndOfMessageCluster)
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
        setBackground(background)

        val onlyBodyMessage = message is SmsMessageRecord
        val mediaThumbnailMessage =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.thumbnailSlide != null

        // reset visibilities / containers
        onContentClick.clear()
        binding.albumThumbnailView.clearViews()
        onContentDoubleTap = null

        if (message.isDeleted) {
            binding.deletedMessageView.isVisible = true
            binding.deletedMessageView.bind(
                message,
                VisibleMessageContentView.getTextColor(context, message)
            )
            return
        } else {
            binding.deletedMessageView.isVisible = false
        }

        binding.quoteView.isVisible = message is MmsMessageRecord && message.quote != null

        binding.linkPreviewView.isVisible =
            message is MmsMessageRecord && message.linkPreviews.isNotEmpty()
        binding.linkPreviewView.bodyTextView = binding.bodyTextView

        val linkPreviewLayout = binding.linkPreviewView.layoutParams
        linkPreviewLayout.width =
            if (mediaThumbnailMessage) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
        binding.linkPreviewView.layoutParams = linkPreviewLayout

        binding.untrustedView.isVisible =
            !contactIsTrusted && message is MmsMessageRecord && message.quote == null
        binding.voiceMessageView.isVisible =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.audioSlide != null
        binding.documentView.isVisible =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.documentSlide != null
        binding.albumThumbnailView.isVisible = mediaThumbnailMessage
        Log.d("DataMessage 1->",message.isOpenGroupInvitation.toString())
        binding.openGroupInvitationView.isVisible = message.isOpenGroupInvitation
        Log.d("DataMessage 2->",message.isPayment.toString())
        //Payment Tag
        binding.paymentCardView.isVisible = message.isPayment

        var hideBody = false

        if (message is MmsMessageRecord && message.quote != null) {
            binding.quoteView.isVisible = true
            val quote = message.quote!!
            // The max content width is the max message bubble size - 2 times the horizontal padding - 2
            // times the horizontal margin. This unfortunately has to be calculated manually
            // here to get the layout right.
            val maxContentWidth =
                (maxWidth - 2 * resources.getDimension(R.dimen.medium_spacing) - 2 * toPx(
                    16,
                    resources
                )).roundToInt()
            val quoteText = if (quote.isOriginalMissing) {
                context.getString(R.string.QuoteView_original_missing)
            } else {
                quote.text
            }
            binding.quoteView.bind(
                quote.author.toString(), quoteText, quote.attachment, thread,
                message.isOutgoing, message.isOpenGroupInvitation, message.threadId,
                quote.isOriginalMissing, glide
            )
            onContentClick.add { event ->
                val r = Rect()
                binding.quoteView.getGlobalVisibleRect(r)
                if (r.contains(event.rawX.roundToInt(), event.rawY.roundToInt())) {
                    delegate?.scrollToMessageIfPossible(quote.id)
                }
            }
        }

        if (message is MmsMessageRecord && message.linkPreviews.isNotEmpty()) {
            binding.linkPreviewView.bind(
                message,
                glide,
                isStartOfMessageCluster,
                isEndOfMessageCluster
            )
            onContentClick.add { event -> binding.linkPreviewView.calculateHit(event) }
            // Body text view is inside the link preview for layout convenience
        } else if (message is MmsMessageRecord && message.slideDeck.audioSlide != null) {
            hideBody = true
            // Audio attachment
            if (contactIsTrusted || message.isOutgoing) {
                binding.voiceMessageView.indexInAdapter = indexInAdapter
                binding.voiceMessageView.delegate = context as? HomeActivity
                binding.voiceMessageView.bind(
                    message,
                    isStartOfMessageCluster,
                    isEndOfMessageCluster
                )
                // We have to use onContentClick (rather than a click listener directly on the voice
                // message view) so as to not interfere with all the other gestures.
                onContentClick.add { binding.voiceMessageView.togglePlayback() }
                onContentDoubleTap = { binding.voiceMessageView.handleDoubleTap() }
            } else {
                // TODO: move this out to its own area
                binding.untrustedView.bind(
                    UntrustedAttachmentView.AttachmentType.AUDIO,
                    VisibleMessageContentView.getTextColor(context, message)
                )
                onContentClick.add { binding.untrustedView.showTrustDialog(message.individualRecipient) }
            }
        } else if (message is MmsMessageRecord && message.slideDeck.documentSlide != null) {
            hideBody = true
            // Document attachment
            if (contactIsTrusted || message.isOutgoing) {
                binding.documentView.bind(
                    message,
                    VisibleMessageContentView.getTextColor(context, message)
                )
                //New Line
                binding.documentView.setOnClickListener {
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
                binding.untrustedView.bind(
                    UntrustedAttachmentView.AttachmentType.DOCUMENT,
                    VisibleMessageContentView.getTextColor(context, message)
                )
                onContentClick.add { binding.untrustedView.showTrustDialog(message.individualRecipient) }
            }
        } else if (message is MmsMessageRecord && message.slideDeck.asAttachments().isNotEmpty()) {
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
                onContentClick.add { event ->
                    binding.albumThumbnailView.calculateHitObject(event, message, thread)
                }
            } else {
                hideBody = true
                binding.albumThumbnailView.clearViews()
                binding.untrustedView.bind(
                    UntrustedAttachmentView.AttachmentType.MEDIA,
                    VisibleMessageContentView.getTextColor(context, message)
                )
                onContentClick.add { binding.untrustedView.showTrustDialog(message.individualRecipient) }
            }
        } else if (message.isOpenGroupInvitation) {
            hideBody = true
            binding.openGroupInvitationView.bind(
                message,
                VisibleMessageContentView.getTextColor(context, message)
            )
            onContentClick.add { binding.openGroupInvitationView.joinOpenGroup() }
        } else if (message.isPayment) { //Payment Tag
            hideBody = true
            binding.paymentCardView.bind(
                message,
                VisibleMessageContentView.getTextColor(context, message)
            )
            //onContentClick.add { binding.openGroupInvitationView.joinOpenGroup() }
        }

        binding.bodyTextView.isVisible = message.body.isNotEmpty() && !hideBody

        // set it to use constraints if not only a text message, otherwise wrap content to whatever width it wants
        val params = binding.bodyTextView.layoutParams
        params.width =
            if (onlyBodyMessage || binding.barrierViewsGone()) ViewGroup.LayoutParams.WRAP_CONTENT else 0
        binding.bodyTextView.layoutParams = params
        binding.bodyTextView.maxWidth = maxWidth

        val bodyWidth = with(binding.bodyTextView) {
            StaticLayout.getDesiredWidth(text, paint).roundToInt()
        }

        val quote = (message as? MmsMessageRecord)?.quote
        val quoteLayoutParams = binding.quoteView.layoutParams
        quoteLayoutParams.width =
            if (mediaThumbnailMessage || quote == null) 0
            else binding.quoteView.calculateWidth(quote, bodyWidth, maxWidth, thread)

        binding.quoteView.layoutParams = quoteLayoutParams
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
    }

    private fun ViewVisibleMessageContentBinding.barrierViewsGone(): Boolean =
        listOf<View>(
            albumThumbnailView,
            linkPreviewView,
            voiceMessageView,
            quoteView
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
            binding.deletedMessageView,
            binding.untrustedView,
            binding.voiceMessageView,
            binding.openGroupInvitationView,
            binding.paymentCardView, //Payment Tag
            binding.documentView,
            binding.quoteView,
            binding.linkPreviewView,
            binding.albumThumbnailView,
            binding.bodyTextView
        ).forEach { view -> view.isVisible = false }
    }

    fun playVoiceMessage() {
        binding.voiceMessageView.togglePlayback()
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
                //val updatedUrl = urlSpan.url.let { HttpUrl.parse(it).toString() }4
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
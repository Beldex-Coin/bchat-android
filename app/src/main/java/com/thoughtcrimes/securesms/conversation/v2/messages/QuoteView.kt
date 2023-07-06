package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.text.StaticLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewQuoteBinding
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.database.model.Quote
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.mms.SlideDeck
import com.thoughtcrimes.securesms.util.MediaUtil
import com.thoughtcrimes.securesms.util.toPx
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionUtilities
import com.thoughtcrimes.securesms.conversation.v2.utilities.TextUtilities
import com.thoughtcrimes.securesms.database.BchatContactDatabase
import com.thoughtcrimes.securesms.util.UiModeUtilities
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min


// There's quite some calculation going on here. It's a bit complex so don't make changes
// if you don't need to. If you do then test:
// • Quoted text in both private chats and group chats
// • Quoted images and videos in both private chats and group chats
// • Quoted voice messages and documents in both private chats and group chats
// • All of the above in both dark mode and light mode
@AndroidEntryPoint
class QuoteView : LinearLayout {

    @Inject lateinit var contactDb: BchatContactDatabase

    private lateinit var binding: ViewQuoteBinding
    private lateinit var mode: Mode
    private val vPadding by lazy { toPx(6, resources) }
    var delegate: QuoteViewDelegate? = null

    enum class Mode { Regular, Draft }

    // region Lifecycle
    constructor(context: Context) : this(context, Mode.Regular)
    constructor(context: Context, attrs: AttributeSet) : this(context,  Mode.Regular, attrs)

    constructor(context: Context, mode: Mode, attrs: AttributeSet? = null) : super(context, attrs) {
        this.mode = mode
        binding = ViewQuoteBinding.inflate(LayoutInflater.from(context), this, true)
        // Add padding here (not on binding.mainQuoteViewContainer) to get a bit of a top inset while avoiding
        // the clipping issue described in getIntrinsicHeight(maxContentWidth:).
        setPadding(0, toPx(6, resources), 0, 0)
        when (mode) {
            Mode.Draft -> binding.quoteViewCancelButton.setOnClickListener { delegate?.cancelQuoteDraft(1)}
            Mode.Regular -> {
                binding.quoteViewCancelButton.isVisible = false
                binding.mainQuoteViewContainer.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.transparent, context.theme))
            }
        }
    }
    // endregion

    // region General
    fun getIntrinsicContentHeight(maxContentWidth: Int): Int {
        // If we're showing an attachment thumbnail, just constrain to the height of that
        if (binding.quoteViewAttachmentPreviewContainer.isVisible) { return toPx(40, resources) }
        var result = 0
        val authorTextViewIntrinsicHeight: Int
        if (binding.quoteViewAuthorTextView.isVisible) {
            val author = binding.quoteViewAuthorTextView.text
            authorTextViewIntrinsicHeight = TextUtilities.getIntrinsicHeight(author, binding.quoteViewAuthorTextView.paint, maxContentWidth)
            result += authorTextViewIntrinsicHeight
        }
        val body = binding.quoteViewBodyTextView.text
        val bodyTextViewIntrinsicHeight = TextUtilities.getIntrinsicHeight(body, binding.quoteViewBodyTextView.paint, maxContentWidth)
        val staticLayout = TextUtilities.getIntrinsicLayout(body, binding.quoteViewBodyTextView.paint, maxContentWidth)
        result += bodyTextViewIntrinsicHeight
        if (!binding.quoteViewAuthorTextView.isVisible) {
            // We want to at least be as high as the cancel button 36DP, and no higher than 3 lines of text.
            // Height from intrinsic layout is the height of the text before truncation so we shorten
            // proportionally to our max lines setting.
            return max(toPx(32, resources) ,min((result / staticLayout.lineCount) * 3, result))
        } else {
            // Because we're showing the author text view, we should have a height of at least 32 DP
            // anyway, so there's no need to constrain to that. We constrain to a max height of 56 DP
            // because that's approximately the height of the author text view + 2 lines of the body
            // text view.
            return min(result, toPx(56, resources))
        }
    }

    fun getIntrinsicHeight(maxContentWidth: Int): Int {
        // The way all this works is that we just calculate the total height the quote view should be
        // and then center everything inside vertically. This effectively means we're applying padding.
        // Applying padding the regular way results in a clipping issue though due to a bug in
        // RelativeLayout.
        return getIntrinsicContentHeight(maxContentWidth)  + (2 * vPadding )
    }
    // endregion

    // region Updating
    fun bind(
        authorPublicKey: String, body: String?, attachments: SlideDeck?, thread: Recipient,
        isOutgoingMessage: Boolean, isOpenGroupInvitation: Boolean, isPayment: Boolean,
        outgoing: Boolean, threadID: Long, isOriginalMissing: Boolean, glide: GlideRequests
    ) {
        // Reduce the max body text view line count to 2 if this is a group thread because
        // we'll be showing the author text view and we don't want the overall quote view height
        // to get too big.
        binding.quoteViewBodyTextView.maxLines = if (thread.isGroupRecipient) 2 else 3
        // Author
        if (thread.isGroupRecipient) {
            val author = contactDb.getContactWithBchatID(authorPublicKey)
            val authorDisplayName = author?.displayName(Contact.contextForRecipient(thread)) ?: authorPublicKey
            binding.quoteViewAuthorTextView.text = authorDisplayName
            binding.quoteViewAuthorTextView.setTextColor(getTextColor(isOutgoingMessage))
        }
        binding.quoteViewAuthorTextView.isVisible = thread.isGroupRecipient
        // Body
        binding.quoteViewBodyTextView.text = when {
            isOpenGroupInvitation -> {
                resources.getString(R.string.open_group_invitation_view__open_group_invitation)
            }
            isPayment -> {
                //Payment Tag
                var amount = ""
                var direction = ""
                try {
                    val mainObject: JSONObject = JSONObject(body)
                    val uniObject = mainObject.getJSONObject("kind")
                    amount = uniObject.getString("amount")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                direction = if (outgoing) {
                    context.getString(R.string.payment_sent)
                } else {
                    context.getString(R.string.payment_received)
                }
                resources.getString(R.string.reply_payment_card_message,direction,amount)
            }
            else -> {
                var bodyText=""
                if(body!=null && body.isNotEmpty()){
                    var type = ""
                    try {
                        val mainObject: JSONObject = JSONObject(body)
                        val uniObject = mainObject.getJSONObject("kind")
                        type = uniObject.getString("@type")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    when (type) {
                        "OpenGroupInvitation" -> {
                            bodyText = resources.getString(R.string.open_group_invitation_view__open_group_invitation)
                        }
                        "Payment" -> {
                            //Payment Tag
                            var amount = ""
                            var direction = ""
                            try {
                                val mainObject: JSONObject = JSONObject(body)
                                val uniObject = mainObject.getJSONObject("kind")
                                amount = uniObject.getString("amount")
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                            direction = if (outgoing) {
                                context.getString(R.string.payment_sent)
                            } else {
                                context.getString(R.string.payment_received)
                            }
                            bodyText = resources.getString(R.string.reply_payment_card_message,direction,amount)
                        }
                        else -> {
                            bodyText = MentionUtilities.highlightMentions((body ?: "").toSpannable(), threadID, context)
                        }
                    }
                }else{
                   bodyText = MentionUtilities.highlightMentions((body ?: "").toSpannable(), threadID, context)
                }
                bodyText
            }
        }
        binding.quoteViewBodyTextView.setTextColor(getTextColor(isOutgoingMessage))
        // Accent line / attachment preview
        val hasAttachments = (attachments != null && attachments.asAttachments().isNotEmpty()) && !isOriginalMissing
        binding.quoteViewAccentLine.isVisible = !hasAttachments
        binding.quoteViewAttachmentPreviewContainer.isVisible = hasAttachments
        if (!hasAttachments) {
            binding.quoteViewAccentLine.setBackgroundColor(getLineColor(isOutgoingMessage))
        } else if (attachments != null) {
            binding.quoteViewAttachmentPreviewImageView.imageTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.white, context.theme))
            val backgroundColorID = if (UiModeUtilities.isDayUiMode(context)) R.color.black else R.color.accent
            val backgroundColor = ResourcesCompat.getColor(resources, backgroundColorID, context.theme)
            binding.quoteViewAttachmentPreviewContainer.backgroundTintList = ColorStateList.valueOf(backgroundColor)
            binding.quoteViewAttachmentPreviewImageView.isVisible = false
            binding.quoteViewAttachmentThumbnailImageView.isVisible = false
            when {
                attachments.audioSlide != null -> {
                    binding.quoteViewAttachmentPreviewImageView.setImageResource(R.drawable.ic_microphone)
                    binding.quoteViewAttachmentPreviewImageView.isVisible = true
                    binding.quoteViewBodyTextView.text = resources.getString(R.string.Slide_audio)
                }
                attachments.documentSlide != null -> {
                    binding.quoteViewAttachmentPreviewImageView.setImageResource(R.drawable.ic_document_large_light)
                    binding.quoteViewAttachmentPreviewImageView.isVisible = true
                    binding.quoteViewBodyTextView.text = resources.getString(R.string.document)
                }
                attachments.thumbnailSlide != null -> {
                    val slide = attachments.thumbnailSlide!!
                    // This internally fetches the thumbnail
                    binding.quoteViewAttachmentThumbnailImageView.radius = toPx(4, resources)
                    binding.quoteViewAttachmentThumbnailImageView.setImageResource(glide, slide, false, false)
                    binding.quoteViewAttachmentThumbnailImageView.isVisible = true
                    binding.quoteViewBodyTextView.text = if (MediaUtil.isVideo(slide.asAttachment())) resources.getString(R.string.Slide_video) else resources.getString(R.string.Slide_image)
                }
            }
        }
    }
    // endregion

    // region Convenience
    @ColorInt private fun getLineColor(isOutgoingMessage: Boolean): Int {
        val isLightMode = UiModeUtilities.isDayUiMode(context)
        return when {
            mode == Mode.Regular && isLightMode || mode == Mode.Draft && isLightMode -> {
                if(isOutgoingMessage) {
                    ResourcesCompat.getColor(resources, R.color.white, context.theme)
                }else{
                    ResourcesCompat.getColor(resources, R.color.accent, context.theme)
                }
            }
            mode == Mode.Regular && !isLightMode -> {
                if (isOutgoingMessage) {
                    ResourcesCompat.getColor(resources, R.color.white, context.theme)
                } else {
                    ResourcesCompat.getColor(resources, R.color.accent, context.theme)
                }
            }
            else -> { // Draft & dark mode
                ResourcesCompat.getColor(resources, R.color.accent, context.theme)
            }
        }
    }

    @ColorInt private fun getTextColor(isOutgoingMessage: Boolean): Int {
        if (mode == Mode.Draft) { return ResourcesCompat.getColor(resources, R.color.text, context.theme) }
        val isLightMode = UiModeUtilities.isDayUiMode(context)
        return if ((isOutgoingMessage && !isLightMode)) {
            ResourcesCompat.getColor(resources, R.color.white, context.theme)
        } else if((!isOutgoingMessage && isLightMode)){
            ResourcesCompat.getColor(resources, R.color.black, context.theme)
        }else {
            ResourcesCompat.getColor(resources, R.color.white, context.theme)
        }
    }

    fun calculateWidth(quote: Quote, bodyWidth: Int, maxContentWidth: Int, thread: Recipient): Int {
        binding.quoteViewAuthorTextView.isVisible = thread.isGroupRecipient
        var paddingWidth = resources.getDimensionPixelSize(R.dimen.medium_spacing) * 5 // initial horizontal padding
        with (binding) {
            if (quoteViewAttachmentPreviewContainer.isVisible) {
                paddingWidth += toPx(40, resources)
            }
            if (quoteViewAccentLine.isVisible) {
                paddingWidth += resources.getDimensionPixelSize(R.dimen.accent_line_thickness)
            }
        }
        val quoteBodyWidth = StaticLayout.getDesiredWidth(binding.quoteViewBodyTextView.text, binding.quoteViewBodyTextView.paint).toInt() + paddingWidth

        val quoteAuthorWidth = if (thread.isGroupRecipient) {
            val authorPublicKey = quote.author.serialize()
            val author = contactDb.getContactWithBchatID(authorPublicKey)
            val authorDisplayName = author?.displayName(Contact.contextForRecipient(thread)) ?: authorPublicKey
            StaticLayout.getDesiredWidth(authorDisplayName, binding.quoteViewBodyTextView.paint).toInt() + paddingWidth
        } else 0

        val quoteWidth = max(quoteBodyWidth, quoteAuthorWidth)
        val usedWidth = max(quoteWidth, bodyWidth)
        return min(maxContentWidth, usedWidth)
    }
    // endregion
}

interface QuoteViewDelegate {

    fun cancelQuoteDraft(i:Int)
}
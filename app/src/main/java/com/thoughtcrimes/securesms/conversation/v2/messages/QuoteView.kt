package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.google.android.material.card.MaterialCardView
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionUtilities
import com.thoughtcrimes.securesms.database.BchatContactDatabase
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.mms.SlideDeck
import com.thoughtcrimes.securesms.util.MediaUtil
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.util.toPx
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewQuoteBinding
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject


// There's quite some calculation going on here. It's a bit complex so don't make changes
// if you don't need to. If you do then test:
// • Quoted text in both private chats and group chats
// • Quoted images and videos in both private chats and group chats
// • Quoted voice messages and documents in both private chats and group chats
// • All of the above in both dark mode and light mode
@AndroidEntryPoint
class QuoteView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : MaterialCardView(context, attrs) {

    @Inject lateinit var contactDb: BchatContactDatabase

    private val binding: ViewQuoteBinding by lazy { ViewQuoteBinding.bind(this) }
    private val mode: Mode
    private val vPadding by lazy { toPx(6, resources) }
    var delegate: QuoteViewDelegate? = null

    enum class Mode { Regular, Draft }

    // region Lifecycle
    init {
        mode = attrs?.let { attrSet ->
            context.obtainStyledAttributes(attrSet, R.styleable.QuoteView).use { typedArray ->
                val modeIndex = typedArray.getInt(R.styleable.QuoteView_quote_mode,  0)
                Mode.values()[modeIndex]
            }
        } ?: Mode.Regular
    }

    // region Lifecycle
    override fun onFinishInflate() {
        super.onFinishInflate()
        when (mode) {
            Mode.Draft -> binding.quoteViewCancelButton.setOnClickListener { delegate?.cancelQuoteDraft(1)}
            Mode.Regular -> {
                binding.quoteViewCancelButton.isVisible = false
//                binding.mainQuoteViewContainer.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.transparent, context.theme))
            }
        }
    }
    // endregion

    // region Updating
    fun bind(
        authorPublicKey: String, body: String?, attachments: SlideDeck?, thread: Recipient,
        isOutgoingMessage: Boolean, isOpenGroupInvitation: Boolean, isPayment: Boolean,
        outgoing: Boolean, threadID: Long, isOriginalMissing: Boolean, glide: GlideRequests
    ) {
        // Author
        val author = contactDb.getContactWithBchatID(authorPublicKey)
        val localNumber = TextSecurePreferences.getLocalNumber(context)
        val quoteIsLocalUser = localNumber != null && localNumber != null && authorPublicKey == localNumber
        val authorDisplayName =
            if (quoteIsLocalUser) context.getString(R.string.QuoteView_you)
            else author?.displayName(Contact.contextForRecipient(thread)) ?: "${authorPublicKey.take(4)}...${authorPublicKey.takeLast(4)}"
        binding.quoteViewAuthorTextView.text = authorDisplayName
        binding.quoteViewAuthorTextView.setTextColor(if(quoteIsLocalUser){
            ResourcesCompat.getColor(resources, R.color.button_green, context.theme)
        }else {
           getTextColor(isOutgoingMessage)
        })
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
                if(!body.isNullOrEmpty()){
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
        //binding.quoteViewBodyTextView.setTextColor(getTextColor(isOutgoingMessage))
        // Accent line / attachment preview
        val hasAttachments = (attachments != null && attachments.asAttachments().isNotEmpty()) && !isOriginalMissing
//        binding.quoteViewAccentLine.isVisible = !hasAttachments
        binding.quoteViewAttachmentPreviewContainer.isVisible = hasAttachments
        if (!hasAttachments) {
//            binding.quoteViewAccentLine.setBackgroundColor(getLineColor(isOutgoingMessage))
            binding.quoteViewAuthorTextView.isVisible = mode != Mode.Regular
            binding.contentTypeIcon.isVisible = false
        } else if (attachments != null) {
            binding.mainQuoteViewContainer.post {
                val w = binding.mainQuoteViewContainer.width
                binding.container.minimumWidth = (w * 0.9).toInt()
            }
            binding.quoteViewAttachmentPreviewImageView.imageTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.white, context.theme))
            val backgroundColorID = R.color.quote_view_background//if (UiModeUtilities.isDayUiMode(context)) R.color.black else R.color.accent
            val backgroundColor = ResourcesCompat.getColor(resources, backgroundColorID, context.theme)
            binding.quoteViewAttachmentPreviewContainer.backgroundTintList = ColorStateList.valueOf(backgroundColor)
            binding.quoteViewAttachmentPreviewImageView.isVisible = false
            binding.quoteViewAttachmentThumbnailImageView.root.isVisible = false
            when {
                attachments.audioSlide != null -> {
                    binding.quoteViewAttachmentPreviewImageView.setImageResource(R.drawable.ic_microphone)
                    binding.quoteViewAttachmentPreviewImageView.isVisible = true
                    binding.quoteViewBodyTextView.text = resources.getString(R.string.Slide_audio)
                    binding.quoteViewBodyTextView.setTextColor(getTextColor(isOutgoingMessage))
                    binding.contentTypeIcon.visibility = View.GONE
                }
                attachments.documentSlide != null -> {
                    binding.quoteViewAttachmentPreviewImageView.setImageResource(R.drawable.ic_document_large_light)
                    binding.quoteViewAttachmentPreviewImageView.isVisible = true
                    binding.quoteViewBodyTextView.text = resources.getString(R.string.document)
                    binding.quoteViewBodyTextView.setTextColor(getTextColor(isOutgoingMessage))
                    binding.contentTypeIcon.visibility = View.GONE
                }
                attachments.thumbnailSlide != null -> {
                    val slide = attachments.thumbnailSlide!!
                    // This internally fetches the thumbnail
                    binding.quoteViewAttachmentThumbnailImageView.root.setImageResource(glide, slide, false, null)
                    binding.quoteViewAttachmentThumbnailImageView.root.radius = toPx(10, resources)
                    binding.quoteViewAttachmentThumbnailImageView.root.isVisible = true
                    binding.quoteViewBodyTextView.text = if (MediaUtil.isVideo(slide.asAttachment())) resources.getString(R.string.Slide_video) else resources.getString(R.string.Slide_image)
                    binding.contentTypeIcon.setImageResource(
                        if (MediaUtil.isVideo(slide.asAttachment())) R.drawable.ic_video_attachment else R.drawable.ic_image_attachment
                    )
                    binding.contentTypeIcon.visibility = View.VISIBLE
                }
            }
        }
        if (mode == Mode.Regular) {
            binding.mainQuoteViewContainer.setBackgroundColor(resources.getColor(getContainerColor(isOutgoingMessage), null))
            binding.quoteViewAuthorTextView.setTextColor(resources.getColor(getAuthorTextColor(isOutgoingMessage), null))
            binding.mainQuoteViewContainer.radius = if (hasAttachments) 24f else 100f
            if (hasAttachments) {
                val quotedTextColor = quotedTextColor(isOutgoingMessage)
                binding.quoteViewBodyTextView.setTextColor(resources.getColor(quotedTextColor, null))
                binding.contentTypeIcon.setColorFilter(resources.getColor(quotedTextColor,null))
            }
        }
    }
    // endregion

    private fun getContainerColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.dialled_call_detail_background
            else -> R.color.quote_view_background
        }
    }

    private fun getAuthorTextColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.white
            else -> R.color.quote_author_text_color
        }
    }

    private fun quotedTextColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.sent_quoted_text_color
            else -> R.color.received_quoted_text_color
        }
    }
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
    // endregion
}

interface QuoteViewDelegate {

    fun cancelQuoteDraft(i:Int)
}
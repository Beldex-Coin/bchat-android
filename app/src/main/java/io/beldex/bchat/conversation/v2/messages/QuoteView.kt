package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.RequestManager
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.contact_sharing.flattenData
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities
import io.beldex.bchat.database.BchatContactDatabase
import io.beldex.bchat.databinding.ViewQuoteBinding
import io.beldex.bchat.mms.SlideDeck
import io.beldex.bchat.util.MediaUtil
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.getScreenWidth
import io.beldex.bchat.util.toPx
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
            Mode.Draft -> {
                binding.quoteViewCancelButton.setOnClickListener {
                    delegate?.cancelQuoteDraft(1)
                }
            }
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
        outgoing: Boolean, threadID: Long, isOriginalMissing: Boolean, glide: RequestManager, textWidth: Int = 0
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

        /*------code section to handle sent contact inside quote view-------*/
        try {
            if (body != null && body.trim().startsWith("{")) {
                val mainObject = JSONObject(body)
                val uniObject = mainObject.optJSONObject("kind")
                val type = uniObject?.optString("@type")

                if (type == "SharedContact") {
                    binding.contactView.visibility = View.VISIBLE
                    binding.contactName.setTextColor(getTextColor(isOutgoingMessage))

                    if (mode == Mode.Regular) {
                        binding.quoteViewAttachmentPreviewContainer.visibility = View.GONE
                        binding.quoteContentType.visibility = View.GONE
                        binding.container.orientation = LinearLayout.VERTICAL
                        binding.mainQuoteViewContainer.setBackgroundColor(
                            resources.getColor(getContainerColor(isOutgoingMessage), null)
                        )
                    } else {
                        binding.quoteGroup.visibility = View.GONE
                        binding.quoteViewAttachmentPreviewImageView.visibility = View.GONE
                    }

                    body.let { message ->
                        UpdateMessageData.fromJSON(message)?.let {
                            val data = it.kind as UpdateMessageData.Kind.SharedContact
                            val names = flattenData(data.name).ifEmpty { flattenData(data.address) }
                            val displayName = when {
                                names.size > 2 -> "${names.first()} and ${names.size - 1} others"
                                names.size == 2 -> "${names[0]} and ${names[1]}"
                                names.size == 1 -> names.first()
                                else -> "No Name"
                            }
                            binding.contactName.text = displayName

                            // Dynamic width calculation
                            if (mode == Mode.Regular) {
                                val paint = Paint().apply {
                                    textSize = TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_SP,
                                        14f,
                                        context.resources.displayMetrics
                                    )
                                }
                                val nameWidth = paint.measureText(data.name)
                                val params = binding.contactView.layoutParams
                                val maxWidth = max(max(350, nameWidth.toInt()), textWidth)
                                val maxPossibleWidth =
                                    min((getScreenWidth() * 0.6).toInt(), maxWidth)
                                params.width = maxPossibleWidth
                                binding.contactView.layoutParams = params
                            }
                        }
                    }
                    return
                }
            }

            // If not JSON or type isn’t SharedContact → fallback
            binding.contactView.visibility = View.GONE
            if (mode == Mode.Regular) {
                binding.quoteContentType.visibility = View.VISIBLE
                binding.container.orientation = LinearLayout.HORIZONTAL
            } else {
                binding.quoteGroup.visibility = View.VISIBLE
            }

        } catch (e: JSONException) {
            e.printStackTrace()
            binding.contactView.visibility = View.GONE
            if (mode == Mode.Regular) {
                binding.quoteContentType.visibility = View.VISIBLE
                binding.container.orientation = LinearLayout.HORIZONTAL
            } else {
                binding.quoteGroup.visibility = View.VISIBLE
            }
        }

        /*--------section ends here----------*/

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
                    if(body.contains("@type")) {
                        try {
                            val mainObject: JSONObject = JSONObject(body)
                            val uniObject = mainObject.getJSONObject("kind")
                            type = uniObject.getString("@type")
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        when (type) {
                            "OpenGroupInvitation" -> {
                                bodyText =
                                    resources.getString(R.string.open_group_invitation_view__open_group_invitation)
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
                                bodyText = resources.getString(
                                    R.string.reply_payment_card_message,
                                    direction,
                                    amount
                                )
                            }

                            else -> {
                                bodyText = MentionUtilities.highlightMentions(
                                    (body ?: "").toSpannable(),
                                    threadID,
                                    context
                                )
                            }
                        }
                    }else{
                        bodyText = MentionUtilities.highlightMentions(
                            (body ?: "").toSpannable(),
                            threadID,
                            context
                        )
                    }
                }else{
                   bodyText = MentionUtilities.highlightMentions((body ?: "").toSpannable(), threadID, context)
                }
                bodyText = when {
                    bodyText.length > 35 -> "${bodyText.subSequence(0, 35)}... "
                    else -> bodyText
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
            //binding.quoteViewAuthorTextView.isVisible = mode != Mode.Regular
            binding.contentTypeIcon.isVisible = false
        } else if (attachments != null) {
            binding.mainQuoteViewContainer.post {
                val w = binding.mainQuoteViewContainer.width
                binding.container.minimumWidth = (w * 0.9).toInt()
            }
            binding.quoteViewAttachmentPreviewImageView.imageTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, if (outgoing) {
                R.color.outgoing_reply_message_icon
            } else {
                R.color.incoming_reply_message_icon
            }, context.theme))
            if(mode == Mode.Regular) {
                val backgroundColorID = if (outgoing) {
                    R.color.button_green
                } else {
                    R.color.user_view_background
                }
                val backgroundColor =
                    ResourcesCompat.getColor(resources, backgroundColorID, context.theme)
                binding.quoteViewAttachmentPreviewContainer.backgroundTintList =
                    ColorStateList.valueOf(backgroundColor)
                binding.quoteViewAttachmentPreviewImageView.isVisible = false
                binding.quoteViewAttachmentThumbnailImageView.root.isVisible = false
            }else{
                val backgroundColorID = R.color.quote_view_background
                val backgroundColor =
                    ResourcesCompat.getColor(resources, backgroundColorID, context.theme)
                binding.quoteViewAttachmentPreviewContainer.backgroundTintList =
                    ColorStateList.valueOf(backgroundColor)
                binding.quoteViewAttachmentPreviewImageView.setColorFilter(resources.getColor(R.color.incoming_reply_message_icon,null))
                binding.contentTypeIcon.setColorFilter(resources.getColor(R.color.incoming_reply_message_icon,null))
            }
            when {
                attachments.audioSlide != null -> {
                    binding.quoteViewAttachmentPreviewImageView.setImageResource(R.drawable.ic_microphone)
                    binding.quoteViewAttachmentPreviewImageView.isVisible = true
                    binding.quoteViewBodyTextView.text = resources.getString(R.string.Slide_audio)
                    binding.contentTypeIcon.setImageResource(
                        R.drawable.ic_microphone
                    )
                    binding.contentTypeIcon.visibility = View.VISIBLE
                }
                attachments.documentSlide != null -> {
                    binding.quoteViewAttachmentPreviewImageView.setImageResource(R.drawable.ic_document)
                    binding.quoteViewAttachmentPreviewImageView.isVisible = true
                    binding.quoteViewBodyTextView.text = resources.getString(R.string.document)
                    binding.contentTypeIcon.setImageResource(
                        R.drawable.ic_document
                    )
                    binding.contentTypeIcon.visibility = View.VISIBLE
                }
                attachments.thumbnailSlide != null -> {
                    val slide = attachments.thumbnailSlide!!
                    // This internally fetches the thumbnail
                    /*binding.quoteViewAttachmentThumbnailImageView.root.setImageResource(glide, slide, false, null)
                    binding.quoteViewAttachmentThumbnailImageView.root.radius = toPx(10, resources)
                    binding.quoteViewAttachmentThumbnailImageView.root.isVisible = true*/
                    binding.quoteViewAttachmentPreviewImageView.setImageResource(if (MediaUtil.isVideo(slide.asAttachment())) R.drawable.ic_video_attachment else R.drawable.ic_image_attachment)
                    binding.quoteViewAttachmentPreviewImageView.isVisible = true
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
            val quotedTextColor = quotedTextColor(isOutgoingMessage)
            binding.quoteViewBodyTextView.setTextColor(resources.getColor(quotedTextColor, null))
            if (hasAttachments) {
                binding.contentTypeIcon.setColorFilter(resources.getColor(quotedTextColor,null))
            }
        }
    }
    // endregion

    private fun getContainerColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.outgoing_call_background
            else -> R.color.quote_view_background
        }
    }

    private fun getAuthorTextColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.white
            else -> R.color.text_green
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
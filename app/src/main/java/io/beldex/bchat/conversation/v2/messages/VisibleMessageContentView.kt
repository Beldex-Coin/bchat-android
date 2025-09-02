package io.beldex.bchat.conversation.v2.messages

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.text.getSpans
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentTransferProgress
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.ThemeUtil
import com.beldex.libbchat.utilities.modifyLayoutParams
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.codewaves.stickyheadergrid.StickyHeaderGridLayoutManager
import com.google.android.material.card.MaterialCardView
import io.beldex.bchat.conversation.v2.ModalUrlBottomSheet
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities
import io.beldex.bchat.conversation.v2.utilities.ModalURLSpan
import io.beldex.bchat.conversation.v2.utilities.TextUtilities.getIntersectedModalSpans
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.database.model.MmsMessageRecord
import io.beldex.bchat.mms.PartAuthority
import com.bumptech.glide.RequestManager
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.SearchUtil
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.getColorWithID
import io.beldex.bchat.util.isSharedContact
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.TextColor
import io.beldex.bchat.compose_utils.noRippleCallback
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.contact_sharing.ContactModel
import io.beldex.bchat.conversation.v2.contact_sharing.SharedContactView
import io.beldex.bchat.databinding.ViewVisibleMessageContentBinding
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale
import kotlin.math.roundToInt

class VisibleMessageContentView : MaterialCardView {
    private val binding: ViewVisibleMessageContentBinding by lazy { ViewVisibleMessageContentBinding.bind(this) }
    var onContentDoubleTap: (() -> Unit)? = null
    var delegate: VisibleMessageViewDelegate? = null
    var indexInAdapter: Int = -1
    private var data: UpdateMessageData.Kind.OpenGroupInvitation? = null
    private var documentViewLastClickTime: Long = 0
    var onLongPress: (() -> Unit)? = null
    var chatWithContact: ((ContactModel) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    // endregion

    // region Updating
    fun bind(
        message : MessageRecord,
        isStartOfMessageCluster : Boolean,
        isEndOfMessageCluster : Boolean,
        glide : RequestManager,
        thread : Recipient,
        searchQuery : String?,
        contactIsTrusted : Boolean,
        onAttachmentNeedsDownload : (Long, Long) -> Unit,
        isSocialGroupRecipient : Boolean,
        delegate : VisibleMessageViewDelegate,
        visibleMessageView : VisibleMessageView,
        position : Int,
        messageSelected: () -> Boolean
    ) {
        // Background
        val background = getBackground(message.isOutgoing, isStartOfMessageCluster, isEndOfMessageCluster)
        val colorID = if (message.isOutgoing) {
            R.attr.message_sent_background_color
        } else {
            R.attr.message_received_background_color
        }
        val color = ThemeUtil.getThemedColor(context, colorID)
        val filter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color,
            BlendModeCompat.SRC_IN
        )
        binding.tailSendView.colorFilter = filter
        binding.tailReceiveView.colorFilter = filter
        background.colorFilter = filter
        setBackground(background)

        val mediaThumbnailMessage =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.thumbnailSlide != null

        // reset visibilities / containers
        onContentClick.clear()
        binding.albumThumbnailView.root.clearViews()
        onContentDoubleTap = null

        if (message.isDeleted) {
            binding.deletedMessageView.root.isVisible = true
            binding.deletedMessageView.root.bind(message, getTextColor(context, message))
            binding.bodyTextView.isVisible = false
            binding.bodyTextViewLayout.isVisible = false
            binding.quoteBodyTextView.isVisible = false
            binding.quoteBodyTextViewLayout.isVisible = false
            binding.quoteView.root.isVisible = false
            binding.linkPreviewView.root.isVisible = false
            binding.untrustedView.root.isVisible = false
            binding.voiceMessageView.root.isVisible = false
            binding.documentView.root.isVisible = false
            binding.albumThumbnailView.root.isVisible = false
            binding.openGroupInvitationView.root.isVisible = false
            binding.contactView.isVisible = false
            return
        } else {
            binding.deletedMessageView.root.isVisible = false
        }

        // clear the
        binding.bodyTextView.text = null
        binding.quoteBodyTextView.text = null

        binding.quoteView.root.isVisible = message is MmsMessageRecord && message.quote != null

        binding.linkPreviewView.root.isVisible =
            message is MmsMessageRecord && message.linkPreviews.isNotEmpty()
        binding.linkPreviewView.root.bodyTextView = binding.bodyTextView

        binding.untrustedView.root.isVisible =
            !contactIsTrusted && message is MmsMessageRecord && message.quote == null && message.linkPreviews.isEmpty()
        binding.voiceMessageView.root.isVisible =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.audioSlide != null
        binding.documentView.root.isVisible =
            contactIsTrusted && message is MmsMessageRecord && message.slideDeck.documentSlide != null
        binding.albumThumbnailView.root.isVisible = mediaThumbnailMessage
        binding.openGroupInvitationView.root.isVisible = message.isOpenGroupInvitation
        //Payment Tag
        binding.paymentCardView.isVisible = message.isPayment
        //shared contact
        binding.contactView.isVisible = message.isSharedContact

        var hideBody = false
        var showQuoteBody = false

        if (message is MmsMessageRecord && message.quote != null) {
            if(contactIsTrusted || message.isOutgoing) {
                if(message.slideDeck.asAttachments().isNotEmpty()) {
                    binding.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    binding.quoteContainer.layoutParams.width = binding.albumContainer.width
                } else if(message.slideDeck.documentSlide != null) {
                    binding.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    binding.quoteContainer.layoutParams.width = binding.documentView.root.width
                } else if(message.slideDeck.audioSlide != null) {
                    binding.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    binding.quoteContainer.layoutParams.width = binding.quoteView.root.width
                } else {
                    binding.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    binding.quoteContainer.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            } else {
                binding.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                if(message.slideDeck.audioSlide != null || message.slideDeck.documentSlide != null || message.slideDeck.asAttachments().isNotEmpty()) {
                    binding.quoteContainer.layoutParams.width = binding.untrustedView.root.width
                } else {
                    binding.quoteContainer.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            hideBody = true
            showQuoteBody = true

            /*dynamic width calculation of quote message to set the quote content width dynamically*/
            var textWidth = 0f
            val text: Spannable?
            val paint = Paint()
            paint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14f,
                context.resources.displayMetrics
            )
            text = getBodySpans(context, message, searchQuery)
            textWidth = paint.measureText(text.toString())
            /*End*/

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
                message.isOutgoing, message.threadId, quote.isOriginalMissing, glide, textWidth.toInt()
            )
            onContentClick.add { event ->
                val r = Rect()
                binding.quoteView.root.getGlobalVisibleRect(r)
                if (r.contains(event.rawX.roundToInt(), event.rawY.roundToInt())) {
                    delegate?.scrollToMessageIfPossible(quote.id)
                }
            }
        }else {
            binding.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            binding.quoteContainer.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        if (message is MmsMessageRecord) {
            message.slideDeck.asAttachments().forEach { attach ->
                val dbAttachment = attach as? DatabaseAttachment ?: return@forEach
                val attachmentId = dbAttachment.attachmentId.rowId
                if (attach.transferState == AttachmentTransferProgress.TRANSFER_PROGRESS_PENDING
                    && MessagingModuleConfiguration.shared.storage.getAttachmentUploadJob(attachmentId) == null) {
                    onAttachmentNeedsDownload(attachmentId, dbAttachment.mmsId)
                }
            }
            message.linkPreviews.forEach { preview ->
                val previewThumbnail = preview.getThumbnail().orNull() as? DatabaseAttachment ?: return@forEach
                val attachmentId = previewThumbnail.attachmentId.rowId
                if (previewThumbnail.transferState == AttachmentTransferProgress.TRANSFER_PROGRESS_PENDING
                    && MessagingModuleConfiguration.shared.storage.getAttachmentUploadJob(attachmentId) == null) {
                    onAttachmentNeedsDownload(attachmentId, previewThumbnail.mmsId)
                }
            }
        }

        when {
            //Link Preview
            message is MmsMessageRecord && message.linkPreviews.isNotEmpty() -> {
                showQuoteBody=false
                binding.linkPreviewView.root.bind(
                    message,
                    glide,
                    isStartOfMessageCluster,
                    isEndOfMessageCluster
                )
                onContentClick.add { event ->
                    binding.linkPreviewView.root.calculateHit(event)
                }
                // Body text view is inside the link preview for layout convenience
            }
            //Audio
            message is MmsMessageRecord && message.slideDeck.audioSlide != null -> {
                hideBody=true
                showQuoteBody=false

                // Audio attachment
                if (contactIsTrusted || message.isOutgoing) {
                    binding.voiceMessageView.root.indexInAdapter=indexInAdapter
                    binding.voiceMessageView.root.delegate=context as? ConversationFragmentV2
                    binding.voiceMessageView.root.bind(
                        message,
                        isStartOfMessageCluster,
                        isEndOfMessageCluster,
                        delegate
                    )
                    // We have to use onContentClick (rather than a click listener directly on the voice
                    // message view) so as to not interfere with all the other gestures.
                    onContentClick.add {
                        if (message.quote == null) {
                            binding.voiceMessageView.root.togglePlayback()
                        }
                    }
                    onContentDoubleTap={ binding.voiceMessageView.root.handleDoubleTap() }
                } else {
                    binding.untrustedView.root.visibility=View.VISIBLE
                    binding.untrustedView.root.bind(
                        message,
                        message.quote,
                        UntrustedAttachmentView.AttachmentType.AUDIO,
                        getTextColor(context, message)
                    )
                    onContentClick.add {
                        if (message.quote == null) {
                            binding.untrustedView.root.showTrustDialog(message.individualRecipient)
                        }
                    }
                }
            }
            //Document
            message is MmsMessageRecord && message.slideDeck.documentSlide != null -> {
                hideBody=true
                showQuoteBody=false
                // Document attachment
                if (contactIsTrusted || message.isOutgoing) {
                    binding.documentView.root.bind(
                        message,
                        getTextColor(context, message)
                    )
                    //New Line
                    binding.documentView.root.setOnClickListener {
                        if (SystemClock.elapsedRealtime() - documentViewLastClickTime >= 500) {
                            documentViewLastClickTime=SystemClock.elapsedRealtime()
                            val documentSlide=message.slideDeck.documentSlide
                            if (documentSlide != null && documentSlide.uri != null) {
                                val intent=Intent(Intent.ACTION_VIEW).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    setDataAndType(
                                        PartAuthority.getAttachmentPublicUri(documentSlide.uri),
                                        documentSlide.contentType
                                    )
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (anfe : ActivityNotFoundException) {
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
                    }
                } else {
                    binding.untrustedView.root.visibility=View.VISIBLE
                    binding.untrustedView.root.bind(
                        message,
                        message.quote,
                        UntrustedAttachmentView.AttachmentType.DOCUMENT,
                        getTextColor(context, message)
                    )
                    onContentClick.add {
                        if (message.quote == null) {
                            binding.untrustedView.root.showTrustDialog(message.individualRecipient)
                        }
                    }
                }
            }
            //Image / Video
            message is MmsMessageRecord && message.slideDeck.asAttachments().isNotEmpty() -> {
                /*
             *    Images / Video attachment
             */
                if (contactIsTrusted || message.isOutgoing) {
                    hideBody=true
                    showQuoteBody=true
                    // isStart and isEnd of cluster needed for calculating the mask for full bubble image groups
                    // bind after add view because views are inflated and calculated during bind
                    binding.albumMessageTime.isVisible=message.body.isEmpty()
                    binding.albumMessageTime.text=
                        DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
                    binding.albumMessageTime.setTextColor(
                        getTimeTextColor(
                            context,
                            message.isOutgoing
                        )
                    )

                    binding.albumThumbnailView.root.bind(
                        glideRequests=glide,
                        message=message,
                        isStart=isStartOfMessageCluster,
                        isEnd=isEndOfMessageCluster
                    )
                    binding.albumContainer.modifyLayoutParams<ConstraintLayout.LayoutParams> {
                        horizontalBias=if (message.isOutgoing) 1f else 0f
                        topMargin=if (message.quote != null) 10 else 0
                    }
                    onContentClick.add { event ->
                        binding.albumThumbnailView.root.calculateHitObject(
                            event,
                            message,
                            thread,
                            onAttachmentNeedsDownload
                        )
                    }
                } else {
                    hideBody=true
                    showQuoteBody=false
                    binding.albumThumbnailView.root.clearViews()
                    binding.untrustedView.root.visibility=View.VISIBLE
                    binding.untrustedView.root.bind(
                        message,
                        message.quote,
                        UntrustedAttachmentView.AttachmentType.MEDIA, getTextColor(context, message)
                    )
                    onContentClick.add {
                        if (message.quote == null) {
                            binding.untrustedView.root.showTrustDialog(message.individualRecipient)
                        }
                    }
                }
            }

            message.isOpenGroupInvitation -> {
                hideBody=true
                showQuoteBody=false
                val umd=UpdateMessageData.fromJSON(message.body)!!
                val data=umd.kind as UpdateMessageData.Kind.OpenGroupInvitation
                this.data=data
                binding.openGroupInvitationView.root.bind(
                    message, getTextColor(context, message)
                )
                onContentClick.add { binding.openGroupInvitationView.root.joinOpenGroup() }
            }

            message.isPayment -> { //Payment Tag
                val umd=UpdateMessageData.fromJSON(message.body)!!
                hideBody=true
                showQuoteBody=false
                binding.paymentCardView.bind(
                    message, getTextColor(context, message)
                )
            }

            message.isSharedContact -> {
                hideBody=true
                showQuoteBody=false
                setContactView(message, messageSelected)
            }
        }



        binding.bodyTextView.isVisible = message.body.isNotEmpty() && !hideBody
        binding.bodyTextViewLayout.isVisible = message.body.isNotEmpty() && !hideBody
        binding.shortMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
        binding.shortMessageTime.setTextColor(getTimeTextColor(context, message.isOutgoing))
        binding.quoteBodyTextView.isVisible = message.body.isNotEmpty() && showQuoteBody
        binding.quoteBodyTextViewLayout.isVisible = message.body.isNotEmpty() && showQuoteBody
        binding.quoteShortMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
        binding.quoteShortMessageTime.setTextColor(getTimeTextColor(context, message.isOutgoing))


        if(binding.albumThumbnailView.root.isVisible){
            val params: ConstraintLayout.LayoutParams = binding.bodyTextViewLayout.layoutParams as ConstraintLayout.LayoutParams
            params.width = binding.albumContainer.width
            params.topMargin = 4
            val params1: RelativeLayout.LayoutParams = binding.bodyTextView.layoutParams as RelativeLayout.LayoutParams
            params1.width = RelativeLayout.LayoutParams.MATCH_PARENT
        } else if(binding.linkPreviewView.root.isVisible){
            val params = binding.bodyTextViewLayout.layoutParams
            params.width = binding.albumContainer.width
            val params1: RelativeLayout.LayoutParams = binding.bodyTextView.layoutParams as RelativeLayout.LayoutParams
            params1.width = RelativeLayout.LayoutParams.MATCH_PARENT
        }else{
            val params: ConstraintLayout.LayoutParams = binding.bodyTextViewLayout.layoutParams as ConstraintLayout.LayoutParams
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            params.topMargin = -4
            val params1: RelativeLayout.LayoutParams = binding.bodyTextView.layoutParams as RelativeLayout.LayoutParams
            params1.width = RelativeLayout.LayoutParams.WRAP_CONTENT
        }

        // set it to use constraints if not only a text message, otherwise wrap content to whatever width it wants
        val fontSize = TextSecurePreferences.getChatFontSize(context)
        binding.bodyTextView.textSize = fontSize!!.toFloat()
        binding.quoteBodyTextView.textSize = fontSize!!.toFloat()

        if (message.body.isNotEmpty() && !hideBody) {
            val color = getTextColor(context, message)
            binding.bodyTextView.setTextColor(color)
            binding.bodyTextView.setLinkTextColor(color)
            val body = getBodySpans(context, message, searchQuery)
            binding.bodyTextView.text = body
            //New Line
            if (binding.bodyTextView.text.trim().length > 705) {
                addReadMore(binding.bodyTextView.text.trim().toString(), binding.bodyTextView, message, delegate, visibleMessageView, position)
            }
            onContentClick.add { e: MotionEvent ->
                binding.bodyTextView.getIntersectedModalSpans(e).iterator().forEach { span ->
                    span.onClick(binding.bodyTextView)
                }
            }
        }
        if (message.body.isNotEmpty() && showQuoteBody) {
            if (isSharedContact(message.body)) {
                binding.contactView.visibility = View.VISIBLE
                setContactView(message, messageSelected, true)
            } else {
                setBodyForQuotedMessage(message, searchQuery,delegate, visibleMessageView, position)
            }
        }
    }

    private fun setBodyForQuotedMessage(message: MessageRecord, searchQuery: String?,delegate : VisibleMessageViewDelegate,visibleMessageView : VisibleMessageView, position : Int) {
        binding.quoteBodyTextViewLayout.isVisible = true
        binding.quoteBodyTextView.isVisible = true
            val color = getTextColor(context, message)
            binding.quoteBodyTextView.setTextColor(color)
            binding.quoteBodyTextView.setLinkTextColor(color)
            val body = getBodySpans(context, message, searchQuery)
            //New Line
            binding.quoteBodyTextView.text = body
            if (binding.quoteBodyTextView.text.trim().length > 705) {
                addReadMore(
                    binding.quoteBodyTextView.text.trim().toString(),
                    binding.quoteBodyTextView,
                    message,
                    delegate,
                    visibleMessageView,
                    position
                )
            }
            onContentClick.add { e: MotionEvent ->
                binding.quoteBodyTextView.getIntersectedModalSpans(e).iterator().forEach { span ->
                    span.onClick(binding.quoteBodyTextView)
                }
            }
    }

    private fun setContactView(message: MessageRecord, messageSelected: () -> Boolean, isQuoted: Boolean = false) {
        val umd = UpdateMessageData.fromJSON(message.body)!!
        val data = umd.kind as UpdateMessageData.Kind.SharedContact
        binding.contactView.setContent {
            BChatTheme {
                val contact = ContactModel(
                    address = Address.fromSerialized(data.address),
                    name = data.name
                )
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp.dp
                val cardBackgroundColor by remember(message) {
                    val backgroundColor = when {
                        message.isOutgoing && !isQuoted -> R.color.outgoing_call_background
                        message.isOutgoing && isQuoted -> R.color.send_message_background
                        !message.isOutgoing && !isQuoted -> R.color.received_call_card_background
                        !message.isOutgoing && isQuoted -> R.color.received_message_background
                        else -> R.color.outgoing_call_background
                    }
                    mutableIntStateOf(
                        backgroundColor
                    )
                }
                SharedContactView(
                    contacts = listOf(
                        contact
                    ),
                    backgroundColor = colorResource(cardBackgroundColor),
                    timeStamp = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp),
                    isQuoted = isQuoted,
                    timeStampColor = colorResource(
                        if (message.isOutgoing) {
                            R.color.sent_message_time_color
                        } else {
                            R.color.received_message_time_color
                        }
                    ),
                    titleColor = colorResource(
                        if (message.isOutgoing) {
                            R.color.white
                        } else {
                            R.color.received_message_text_color
                        }
                    ),
                    subtitleColor = if (message.isOutgoing) {
                        TextColor
                    } else {
                        colorResource(R.color.received_message_text_color)
                    },
                    modifier = Modifier
                        .width(screenWidth * 0.7f)
                        .padding(
                            bottom = if (isQuoted) 0.dp else 8.dp,
                            start = 8.dp,
                            end = 8.dp
                        ),
                    columnModifier = Modifier
                        .padding(bottom = 8.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    onLongPress?.let { it1 -> it1() }
                                },
                                onTap = {
                                    if (messageSelected()) {
                                        onLongPress?.let { it1 -> it1() }
                                    } else {
                                        chatWithContact?.let { it1 -> it1(contact) }
                                    }
                                },
                            )
                        }
                )
            }
        }

    }
    private fun detectTapGestures(onLongPress: () -> Unit?, onTap: () -> Unit?, on: Any) {

    }

    val onContentClick: MutableList<((event: MotionEvent) -> Unit)> = mutableListOf()

    fun onContentClick(event: MotionEvent) {
        onContentClick.forEach { clickHandler -> clickHandler.invoke(event) }
    }

    private fun ViewVisibleMessageContentBinding.barrierViewsGone(): Boolean =
        listOf<View>(
            albumThumbnailView.root,
            linkPreviewView.root,
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
                if (isOutgoing) {
                    binding.tailSendView.visibility = VISIBLE
                    binding.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_end
                } else {
                    binding.tailSendView.visibility = GONE
                    binding.tailReceiveView.visibility = VISIBLE
                    R.drawable.message_bubble_background_received_end
                }
            }
            isStartOfMessageCluster -> {
                if (isOutgoing) {
                    binding.tailSendView.visibility = GONE
                    binding.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_alone
                } else {
                    binding.tailSendView.visibility = GONE
                    binding.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_alone
                }
            }
            isEndOfMessageCluster -> {
                if (isOutgoing) {
                    binding.tailSendView.visibility = VISIBLE
                    binding.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_end
                } else {
                    binding.tailSendView.visibility = GONE
                    binding.tailReceiveView.visibility = VISIBLE
                    R.drawable.message_bubble_background_received_end
                }
            }
            else -> {
                if (isOutgoing) {
                    binding.tailSendView.visibility = GONE
                    binding.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_alone
                } else {
                    binding.tailSendView.visibility = GONE
                    binding.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_alone
                }
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
            binding.linkPreviewView.root,
            binding.albumThumbnailView.root,
            binding.albumMessageTime,
            binding.bodyTextView,
            binding.bodyTextViewLayout,
            binding.contactView
        ).forEach { view:View -> view.isVisible = false }
    }

    fun playVoiceMessage() {
        binding.voiceMessageView.root.togglePlayback()
    }

    fun stopVoiceMessage(){
        binding.voiceMessageView.root.stoppedVoiceMessage()
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
            var linkLastClickTime: Long = 0

            body = MentionUtilities.highlightMentions(
                body,
                message.isOutgoing,
                message.threadId,
                context
            )
            body = SearchUtil.getHighlightedSpan(Locale.getDefault(),
                { BackgroundColorSpan(if(message.isOutgoing) context.getColor(R.color.black) else context.getColor(R.color.incoming_message_search_query)) }, body, searchQuery
            )
            body = SearchUtil.getHighlightedSpan(Locale.getDefault(),
                { ForegroundColorSpan(if(message.isOutgoing) context.getColor(R.color.white) else context.getColor(R.color.received_message_text_color)) }, body, searchQuery
            )

            Linkify.addLinks(body, Linkify.WEB_URLS)

            // replace URLSpans with ModalURLSpans
            body.getSpans<URLSpan>(0, body.length).toList().forEach { urlSpan ->
                val updatedUrl = urlSpan.url.let { it.toHttpUrlOrNull().toString() }
                val replacementSpan = ModalURLSpan(updatedUrl) { url ->
                    ActivityDispatcher.get(context)?.showBottomSheetDialog(ModalUrlBottomSheet(url),"Open URL Dialog")
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
        fun getTextColor(context: Context, message: MessageRecord): Int  = context.resources.getColorWithID(
            if (message.isOutgoing) R.color.white else R.color.received_message_text_color,context.theme
        )

        @ColorInt
        fun getTimeTextColor(context: Context, isOutGoing: Boolean): Int {
            val colorID = if (isOutGoing) {
                R.color.sent_message_time_color
            } else {
                R.color.received_message_time_color
            }
            return context.resources.getColorWithID(colorID, context.theme)
        }
    }
    // endregion

    //New Line
    private fun addReadMore(
        text : String,
        textView : TextView,
        message : MessageRecord,
        delegate : VisibleMessageViewDelegate,
        visibleMessageView : VisibleMessageView,
        position : Int
    ) {
        val ss = SpannableString(text.substring(0, 705) + "... Read more")
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                addReadLess(text, textView, message, delegate, visibleMessageView,position)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
                val isDayUiMode = UiModeUtilities.isDayUiMode(context)
                ds.color = if (message.isOutgoing) {
                    if (isDayUiMode) {
                        ContextCompat.getColor(context, R.color.black)
                    } else ContextCompat.getColor(context, R.color.chat_id_card_background)
                } else {
                    ContextCompat.getColor(context, R.color.send_message_background)
                }
            }
        }
        ss.setSpan(clickableSpan, ss.length - 10, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = ss
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.setOnLongClickListener {
            delegate.onItemLongPress(message, visibleMessageView, position)
            true
        }
    }

    private fun addReadLess(text: String, textView: TextView, message: MessageRecord, delegate : VisibleMessageViewDelegate, visibleMessageView: VisibleMessageView, position:Int) {
        val ss = SpannableString("$text Read less")
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                addReadMore(text, textView, message, delegate, visibleMessageView, position)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
                val isDayUiMode = UiModeUtilities.isDayUiMode(context)
                ds.color = if (message.isOutgoing) {
                    if (isDayUiMode){
                        ContextCompat.getColor(context,R.color.black)
                    }else ContextCompat.getColor(context,R.color.chat_id_card_background)
                } else {
                    ContextCompat.getColor(context,R.color.send_message_background)
                }
            }
        }
        ss.setSpan(clickableSpan, ss.length - 10, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = ss
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.setOnLongClickListener {
            delegate.onItemLongPress(message, visibleMessageView, position)
            true
        }
    }

    interface VisibleMessageContentViewDelegate {
        fun scrollToMessageIfPossible(timestamp: Long)
    }
}
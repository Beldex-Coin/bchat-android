package io.beldex.bchat.conversation.v2.messages

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.ColorFilter
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
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
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.contact_sharing.ContactModel
import io.beldex.bchat.conversation.v2.contact_sharing.SharedContactView
import io.beldex.bchat.conversation.v2.search.SearchViewModel
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

    // Keep a static cache for ColorFilters to avoid reallocation every bind
    object ColorFilterCache {
        private val cache = mutableMapOf<Int, ColorFilter?>()
        fun get(color: Int): ColorFilter? = cache.getOrPut(color) {
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        }
    }

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
        messageSelected : () -> Boolean,
        searchViewModel : SearchViewModel?
    ) {
        fun View.setVisibleIfChanged(visible: Boolean) {
            if (this.isVisible != visible) this.isVisible = visible
        }
        fun <T> MutableMap<String, T>.getOrCompute(key: String, compute: () -> T): T {
            return this[key] ?: compute().also { this[key] = it }
        }
        val ctx = context
        val bindingRef = binding

        val isOutgoing = message.isOutgoing
        val mms = message as? MmsMessageRecord
        val attachments = mms?.slideDeck?.asAttachments().orEmpty()
        val hasAttachments = attachments.isNotEmpty()
        val hasLinkPreviews = mms?.linkPreviews?.isNotEmpty() == true
        val hasQuote = mms?.quote != null
        val isDeleted = message.isDeleted

        if (isDeleted) {
            bindingRef.deletedMessageView.root.setVisibleIfChanged(true)
            bindingRef.deletedMessageView.root.bind(message, getTextColor(ctx, message))
            bindingRef.bodyTextView.setVisibleIfChanged(false)
            bindingRef.bodyTextViewLayout.setVisibleIfChanged(false)
            bindingRef.quoteBodyTextView.setVisibleIfChanged(false)
            bindingRef.quoteBodyTextViewLayout.setVisibleIfChanged(false)
            bindingRef.quoteView.root.setVisibleIfChanged(false)
            bindingRef.linkPreviewView.root.setVisibleIfChanged(false)
            bindingRef.untrustedView.root.setVisibleIfChanged(false)
            bindingRef.voiceMessageView.root.setVisibleIfChanged(false)
            bindingRef.documentView.root.setVisibleIfChanged(false)
            bindingRef.albumThumbnailView.root.setVisibleIfChanged(false)
            bindingRef.openGroupInvitationView.root.setVisibleIfChanged(false)
            bindingRef.sharedContactView.setVisibleIfChanged(false)
            return
        } else {
            bindingRef.deletedMessageView.root.setVisibleIfChanged(false)
        }

        val background = getBackground(isOutgoing, isStartOfMessageCluster, isEndOfMessageCluster, bindingRef, ctx)
        val colorID = if (isOutgoing) R.attr.message_sent_background_color else R.attr.message_received_background_color
        val color = ThemeUtil.getThemedColor(ctx, colorID)

        val filter = ColorFilterCache.get(color)
        bindingRef.tailSendView.colorFilter = filter
        bindingRef.tailReceiveView.colorFilter = filter
        background.colorFilter = filter
        setBackground(background)

        // reset visibilities / containers
        onContentClick.clear()
        bindingRef.albumThumbnailView.root.clearViews()
        onContentDoubleTap = null

        // clear the
        if (bindingRef.bodyTextView.text?.isNotEmpty() == true) bindingRef.bodyTextView.text = null
        if (bindingRef.quoteBodyTextView.text?.isNotEmpty() == true) bindingRef.quoteBodyTextView.text = null

        bindingRef.quoteView.root.setVisibleIfChanged(hasQuote && mms != null)
        bindingRef.linkPreviewView.root.setVisibleIfChanged(hasLinkPreviews)
        bindingRef.linkPreviewView.root.bodyTextView = bindingRef.bodyTextView
        bindingRef.untrustedView.root.setVisibleIfChanged(!contactIsTrusted && mms != null && mms.quote == null && !hasLinkPreviews)
        bindingRef.voiceMessageView.root.setVisibleIfChanged(contactIsTrusted && mms?.slideDeck?.audioSlide != null)
        bindingRef.documentView.root.setVisibleIfChanged(contactIsTrusted && mms?.slideDeck?.documentSlide != null)
        bindingRef.albumThumbnailView.root.setVisibleIfChanged(contactIsTrusted && mms != null && mms.slideDeck.thumbnailSlide != null)
        bindingRef.openGroupInvitationView.root.setVisibleIfChanged(message.isOpenGroupInvitation)
        bindingRef.paymentCardView.setVisibleIfChanged(message.isPayment)
        bindingRef.sharedContactView.setVisibleIfChanged(message.isSharedContact)

        val bodySpans = message.body.takeIf { it.isNotEmpty() }?.let {
            getBodySpans(ctx, message, searchQuery, isOutgoing)
        }

        var hideBody = false
        var showQuoteBody = false

        if (hasQuote && mms != null) {
            if(contactIsTrusted || isOutgoing) {
                val params = bindingRef.quoteContainer.layoutParams

                when {
                    mms.slideDeck.audioSlide != null -> {
                        bindingRef.voiceMessageView.root.post {
                            params.width = bindingRef.voiceMessageView.root.measuredWidth
                            bindingRef.quoteContainer.layoutParams = params
                        }
                    }
                    mms.slideDeck.documentSlide != null -> {
                        bindingRef.documentView.root.post {
                            params.width = bindingRef.documentView.root.measuredWidth
                            bindingRef.quoteContainer.layoutParams = params
                        }
                    }
                    hasAttachments -> {
                        bindingRef.albumContainer.post {
                            params.width = bindingRef.albumContainer.measuredWidth
                            bindingRef.quoteContainer.layoutParams = params
                        }
                    }
                    isSharedContact(message.body) -> {
                        bindingRef.sharedContactView.post {
                            params.width = bindingRef.sharedContactView.measuredWidth
                            bindingRef.quoteContainer.layoutParams = params
                        }
                    }
                    else -> {
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        bindingRef.quoteContainer.layoutParams = params
                    }
                }
                bindingRef.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                bindingRef.quoteContainer.layoutParams = params
                bindingRef.quoteContainer.requestLayout()
            } else {
                if (mms.slideDeck.audioSlide != null || mms.slideDeck.documentSlide != null || hasAttachments) {
                    bindingRef.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    val params = bindingRef.quoteContainer.layoutParams
                    bindingRef.untrustedView.root.post {
                        params.width = bindingRef.untrustedView.root.measuredWidth
                        bindingRef.quoteContainer.layoutParams = params
                    }
                    bindingRef.quoteContainer.layoutParams = params
                    bindingRef.quoteContainer.requestLayout()
                } else {
                    bindingRef.quoteContainer.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            hideBody = true
            showQuoteBody = true

            /*dynamic width calculation of quote message to set the quote content width dynamically*/
            var textWidth = 0f
            val paint = Paint()
            paint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14f,
                ctx.resources.displayMetrics
            )
            textWidth = paint.measureText(bodySpans.toString())

            bindingRef.quoteView.root.isVisible = true
            val quote = message.quote!!
            val quoteText = if (quote.isOriginalMissing) {
                ctx.getString(R.string.QuoteView_original_missing)
            } else {
                quote.text
            }
            bindingRef.quoteView.root.bind(
                quote.author.toString(), quoteText, quote.attachment, thread,
                isOutgoing, message.isOpenGroupInvitation, message.isPayment,
                isOutgoing, message.threadId, quote.isOriginalMissing, glide, textWidth.toInt()
            )
            onContentClick.add { event ->
                val r = Rect()
                bindingRef.quoteView.root.getGlobalVisibleRect(r)
                if (r.contains(event.rawX.roundToInt(), event.rawY.roundToInt())) {
                    delegate.scrollToMessageIfPossible(quote.id)
                }
            }
        } else {
            bindingRef.quoteView.root.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            bindingRef.quoteContainer.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        if (mms != null) {
            mms.slideDeck.asAttachments().forEach { attach ->
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
            mms !=null && hasLinkPreviews -> {
                showQuoteBody=false
                bindingRef.linkPreviewView.root.bind(
                    message,
                    glide,
                    isStartOfMessageCluster,
                    isEndOfMessageCluster
                )
                onContentClick.add { event ->
                    bindingRef.linkPreviewView.root.calculateHit(event)
                }
                // Body text view is inside the link preview for layout convenience
            }
            //Audio
            mms != null && mms.slideDeck.audioSlide != null -> {
                hideBody=true
                showQuoteBody=false

                // Audio attachment
                if (contactIsTrusted || isOutgoing) {
                    bindingRef.voiceMessageView.root.indexInAdapter=indexInAdapter
                    bindingRef.voiceMessageView.root.delegate=ctx as? ConversationFragmentV2
                    bindingRef.voiceMessageView.root.bind(
                        message,
                        isStartOfMessageCluster,
                        isEndOfMessageCluster,
                        delegate
                    )
                    // We have to use onContentClick (rather than a click listener directly on the voice
                    // message view) so as to not interfere with all the other gestures.
                    onContentClick.add {
                        if (message.quote == null) {
                            bindingRef.voiceMessageView.root.togglePlayback()
                        }
                    }
                    onContentDoubleTap={ bindingRef.voiceMessageView.root.handleDoubleTap() }
                } else {
                    bindingRef.untrustedView.root.visibility=VISIBLE
                    bindingRef.untrustedView.root.bind(
                        message,
                        message.quote,
                        UntrustedAttachmentView.AttachmentType.AUDIO,
                        getTextColor(ctx, message)
                    )
                    onContentClick.add {
                        if (message.quote == null) {
                            bindingRef.untrustedView.root.showTrustDialog(message.individualRecipient)
                        }
                    }
                }
            }
            //Document
            mms != null && mms.slideDeck.documentSlide != null -> {
                hideBody=true
                showQuoteBody=false
                // Document attachment
                if (contactIsTrusted || isOutgoing) {
                    bindingRef.documentView.root.bind(
                        message,
                        getTextColor(ctx, message)
                    )
                    //New Line
                    bindingRef.documentView.root.setOnClickListener {
                        if (SystemClock.elapsedRealtime() - documentViewLastClickTime >= 500) {
                            documentViewLastClickTime=SystemClock.elapsedRealtime()
                            val documentSlide=mms.slideDeck.documentSlide
                            if (documentSlide != null && documentSlide.uri != null) {
                                val intent=Intent(Intent.ACTION_VIEW).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    setDataAndType(
                                        PartAuthority.getAttachmentPublicUri(documentSlide.uri),
                                        documentSlide.contentType
                                    )
                                }
                                try {
                                    ctx.startActivity(intent)
                                } catch (anfe : ActivityNotFoundException) {
                                    Log.w(
                                        StickyHeaderGridLayoutManager.TAG,
                                        "No activity existed to view the media."
                                    )
                                    Toast.makeText(
                                        ctx,
                                        R.string.ConversationItem_unable_to_open_media,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    ctx,
                                    "Please wait until file downloaded",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    bindingRef.untrustedView.root.visibility=VISIBLE
                    bindingRef.untrustedView.root.bind(
                        message,
                        message.quote,
                        UntrustedAttachmentView.AttachmentType.DOCUMENT,
                        getTextColor(ctx, message)
                    )
                    onContentClick.add {
                        if (message.quote == null) {
                            bindingRef.untrustedView.root.showTrustDialog(message.individualRecipient)
                        }
                    }
                }
            }
            //Image / Video
            mms != null && hasAttachments -> {
                /*
             *    Images / Video attachment
             */
                if (contactIsTrusted || isOutgoing) {
                    hideBody=true
                    showQuoteBody=true
                    // isStart and isEnd of cluster needed for calculating the mask for full bubble image groups
                    // bind after add view because views are inflated and calculated during bind
                    bindingRef.albumMessageTime.isVisible=message.body.isEmpty()
                    bindingRef.albumMessageTime.text=
                        DateUtils.getTimeStamp(ctx, Locale.getDefault(), message.timestamp)
                    bindingRef.albumMessageTime.setTextColor(
                        getTimeTextColor(
                            ctx,
                            isOutgoing
                        )
                    )

                    bindingRef.albumThumbnailView.root.bind(
                        glideRequests=glide,
                        message=message,
                        isStart=isStartOfMessageCluster,
                        isEnd=isEndOfMessageCluster
                    )
                    bindingRef.albumContainer.modifyLayoutParams<ConstraintLayout.LayoutParams> {
                        horizontalBias=if (isOutgoing) 1f else 0f
                        topMargin=if (message.quote != null) 10 else 0
                    }
                    onContentClick.add { event ->
                        bindingRef.albumThumbnailView.root.calculateHitObject(
                            event,
                            message,
                            thread,
                            onAttachmentNeedsDownload
                        )
                    }
                } else {
                    hideBody=true
                    showQuoteBody=false
                    bindingRef.albumThumbnailView.root.clearViews()
                    bindingRef.untrustedView.root.visibility=VISIBLE
                    bindingRef.untrustedView.root.bind(
                        message,
                        message.quote,
                        UntrustedAttachmentView.AttachmentType.MEDIA, getTextColor(ctx, message)
                    )
                    onContentClick.add {
                        if (message.quote == null) {
                            bindingRef.untrustedView.root.showTrustDialog(message.individualRecipient)
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
                bindingRef.openGroupInvitationView.root.bind(
                    message, getTextColor(ctx, message)
                )
            }

            message.isPayment -> { //Payment Tag
                hideBody=true
                showQuoteBody=false
                bindingRef.paymentCardView.bind(
                    message
                )
            }

            message.isSharedContact -> {
                hideBody=true
                showQuoteBody=false
                setContactView(message, messageSelected,searchQuery, searchViewModel, hasQuote, mms, bindingRef, ctx, isOutgoing)
            }
        }

        bindingRef.bodyTextView.isVisible = message.body.isNotEmpty() && !hideBody
        bindingRef.bodyTextViewLayout.isVisible = message.body.isNotEmpty() && !hideBody
        bindingRef.shortMessageTime.text = DateUtils.getTimeStamp(ctx, Locale.getDefault(), message.timestamp)
        bindingRef.shortMessageTime.setTextColor(getTimeTextColor(ctx, isOutgoing))
        bindingRef.quoteBodyTextView.isVisible = message.body.isNotEmpty() && showQuoteBody
        bindingRef.quoteBodyTextViewLayout.isVisible = message.body.isNotEmpty() && showQuoteBody
        bindingRef.quoteShortMessageTime.text = DateUtils.getTimeStamp(ctx, Locale.getDefault(), message.timestamp)
        bindingRef.quoteShortMessageTime.setTextColor(getTimeTextColor(ctx, isOutgoing))

        if(bindingRef.albumThumbnailView.root.isVisible){
            val params: ConstraintLayout.LayoutParams = bindingRef.bodyTextViewLayout.layoutParams as ConstraintLayout.LayoutParams
            params.width = bindingRef.albumContainer.width
            params.topMargin = 4
            val params1: RelativeLayout.LayoutParams = bindingRef.bodyTextView.layoutParams as RelativeLayout.LayoutParams
            params1.width = RelativeLayout.LayoutParams.MATCH_PARENT
        } else if(bindingRef.linkPreviewView.root.isVisible){
            val params = bindingRef.bodyTextViewLayout.layoutParams
            params.width = bindingRef.albumContainer.width
            val params1: RelativeLayout.LayoutParams = bindingRef.bodyTextView.layoutParams as RelativeLayout.LayoutParams
            params1.width = RelativeLayout.LayoutParams.MATCH_PARENT
        }else{
            val params: ConstraintLayout.LayoutParams = bindingRef.bodyTextViewLayout.layoutParams as ConstraintLayout.LayoutParams
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            params.topMargin = -4
            val params1: RelativeLayout.LayoutParams = bindingRef.bodyTextView.layoutParams as RelativeLayout.LayoutParams
            params1.width = RelativeLayout.LayoutParams.WRAP_CONTENT
        }

        // set it to use constraints if not only a text message, otherwise wrap content to whatever width it wants
        val fontSize = TextSecurePreferences.getChatFontSize(ctx)
        bindingRef.bodyTextView.textSize = fontSize!!.toFloat()
        bindingRef.quoteBodyTextView.textSize = fontSize.toFloat()

        if (message.body.isNotEmpty() && !hideBody) {
            val textColor = getTextColor(ctx, message)
            bindingRef.bodyTextView.setTextColor(textColor)
            bindingRef.bodyTextView.setLinkTextColor(textColor)
            bindingRef.bodyTextView.text = bodySpans
            //New Line
            if (bindingRef.bodyTextView.text.trim().length > 705) {
                addReadMore(bindingRef.bodyTextView.text.trim().toString(), bindingRef.bodyTextView, message, delegate, visibleMessageView, position, ctx, isOutgoing)
            }
            onContentClick.add { e: MotionEvent ->
                bindingRef.bodyTextView.getIntersectedModalSpans(e).iterator().forEach { span ->
                    span.onClick(bindingRef.bodyTextView)
                }
            }
        }
        if (message.body.isNotEmpty() && showQuoteBody) {
            if (isSharedContact(message.body)) {
                bindingRef.sharedContactView.visibility = VISIBLE
                setContactView(
                    message,
                    messageSelected,
                    searchQuery,
                    searchViewModel,
                    hasQuote,
                    mms,
                    bindingRef,
                    ctx,
                    isOutgoing
                )
            } else {
                setBodyForQuotedMessage(message,delegate, visibleMessageView, position, bindingRef, ctx, isOutgoing, bodySpans)
            }
        }
    }

    private fun setBodyForQuotedMessage(
        message: MessageRecord,
        delegate: VisibleMessageViewDelegate,
        visibleMessageView: VisibleMessageView,
        position: Int,
        bindingRef: ViewVisibleMessageContentBinding,
        context: Context,
        isOutgoing: Boolean,
        bodySpans: Spannable?
    ) {
        bindingRef.quoteBodyTextViewLayout.isVisible = true
        bindingRef.quoteBodyTextView.isVisible = true
        bindingRef.quoteShortMessageTime.isVisible = true
        val color = getTextColor(context, message)
        bindingRef.quoteBodyTextView.setTextColor(color)
        bindingRef.quoteBodyTextView.setLinkTextColor(color)
        //New Line
        bindingRef.quoteBodyTextView.text = bodySpans
        if (bindingRef.quoteBodyTextView.text.trim().length > 705) {
            addReadMore(
                bindingRef.quoteBodyTextView.text.trim().toString(),
                bindingRef.quoteBodyTextView,
                message,
                delegate,
                visibleMessageView,
                position,
                context,
                isOutgoing
            )
        }
        onContentClick.add { e: MotionEvent ->
            bindingRef.quoteBodyTextView.getIntersectedModalSpans(e).iterator().forEach { span ->
                span.onClick(bindingRef.quoteBodyTextView)
            }
        }
    }

    private fun setContactView(
        message: MessageRecord,
        messageSelected: () -> Boolean,
        searchQuery: String?,
        viewModel: SearchViewModel?,
        hasQuote: Boolean,
        mms: MmsMessageRecord?,
        bindingRef: ViewVisibleMessageContentBinding,
        context: Context,
        isOutgoing: Boolean
    ) {
        val isQuoteView : Boolean= hasQuote && mms != null

        if (isQuoteView) {
            bindingRef.quoteView.root.layoutParams.width=ViewGroup.LayoutParams.MATCH_PARENT
            bindingRef.quoteContainer.layoutParams.width=bindingRef.sharedContactView.width
        }
        bindingRef.quoteShortMessageTime.isVisible = false
        bindingRef.sharedContactView.setContent {
            key(message.id) {
                BChatTheme {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    var hadResult by remember {
                        mutableStateOf(false)
                    }
                    viewModel?.hasSearchResults?.observe(lifecycleOwner){ value ->
                        hadResult = value
                    }

                    val umd=UpdateMessageData.fromJSON(message.body)!!
                    val data=umd.kind as UpdateMessageData.Kind.SharedContact
                    val contact=ContactModel(
                        address=Address.fromSerialized(data.address),
                        name=data.name
                    )
                    val cardBackgroundColor by remember(message) {
                        val backgroundColor=when {
                            isOutgoing && !isQuoteView -> R.color.outgoing_call_background
                            isOutgoing && isQuoteView -> R.color.outgoing_call_background
                            !isOutgoing && !isQuoteView -> R.color.received_call_card_background
                            !isOutgoing && isQuoteView -> R.color.quote_view_background
                            else -> R.color.outgoing_call_background
                        }
                        mutableIntStateOf(
                            backgroundColor
                        )
                    }

                    SharedContactView(
                        contacts=listOf(
                            contact
                        ),
                        backgroundColor=colorResource(cardBackgroundColor),
                        timeStamp=DateUtils.getTimeStamp(
                            context,
                            Locale.getDefault(),
                            message.timestamp
                        ),
                        isQuoted=isQuoteView,
                        isOutgoing=isOutgoing,
                        titleColor=colorResource(
                            if (isOutgoing) {
                                R.color.white
                            } else {
                                R.color.received_message_text_color
                            }
                        ),
                        subtitleColor=if (isOutgoing) {
                            TextColor
                        } else {
                            colorResource(R.color.received_message_text_color)
                        },
                        columnModifier=Modifier
                            .padding(bottom=4.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress={
                                        onLongPress?.let { it1 -> it1() }
                                    },
                                    onTap={
                                        if (messageSelected()) {
                                            onLongPress?.let { it1 -> it1() }
                                        } else {
                                            chatWithContact?.let { it1 -> it1(contact) }

                                        }
                                    },
                                )
                            },
                        searchQuery=searchQuery ?: "",
                        hasResult = hadResult
                    )
                }
            }
        }
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
        isEndOfMessageCluster: Boolean,
        bindingRef: ViewVisibleMessageContentBinding,
        context: Context
    ): Drawable {
        val isSingleMessage = (isStartOfMessageCluster && isEndOfMessageCluster)
        @DrawableRes val backgroundID = when {
            isSingleMessage -> {
                if (isOutgoing) {
                    bindingRef.tailSendView.visibility = VISIBLE
                    bindingRef.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_end
                } else {
                    bindingRef.tailSendView.visibility = GONE
                    bindingRef.tailReceiveView.visibility = VISIBLE
                    R.drawable.message_bubble_background_received_end
                }
            }
            isStartOfMessageCluster -> {
                if (isOutgoing) {
                    bindingRef.tailSendView.visibility = GONE
                    bindingRef.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_alone
                } else {
                    bindingRef.tailSendView.visibility = GONE
                    bindingRef.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_alone
                }
            }
            isEndOfMessageCluster -> {
                if (isOutgoing) {
                    bindingRef.tailSendView.visibility = VISIBLE
                    bindingRef.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_end
                } else {
                    bindingRef.tailSendView.visibility = GONE
                    bindingRef.tailReceiveView.visibility = VISIBLE
                    R.drawable.message_bubble_background_received_end
                }
            }
            else -> {
                if (isOutgoing) {
                    bindingRef.tailSendView.visibility = GONE
                    bindingRef.tailReceiveView.visibility = GONE
                    R.drawable.message_bubble_background_sent_alone
                } else {
                    bindingRef.tailSendView.visibility = GONE
                    bindingRef.tailReceiveView.visibility = GONE
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
         return ResourcesCompat.getDrawable(resources, backgroundID, ctx.theme)!!
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
            binding.sharedContactView
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
            searchQuery: String?,
            isOutgoing: Boolean
        ): Spannable {
            var body = message.body.toSpannable()

            body = MentionUtilities.highlightMentions(
                body,
                isOutgoing,
                message.threadId,
                context
            )
            body = SearchUtil.getHighlightedSpan(Locale.getDefault(),
                { BackgroundColorSpan(if(isOutgoing) context.getColor(R.color.black) else context.getColor(R.color.incoming_message_search_query)) }, body, searchQuery
            )
            body = SearchUtil.getHighlightedSpan(Locale.getDefault(),
                { ForegroundColorSpan(if(isOutgoing) context.getColor(R.color.white) else context.getColor(R.color.received_message_text_color)) }, body, searchQuery
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
        text: String,
        textView: TextView,
        message: MessageRecord,
        delegate: VisibleMessageViewDelegate,
        visibleMessageView: VisibleMessageView,
        position: Int,
        context: Context,
        isOutgoing: Boolean
    ) {
        val ss = SpannableString(text.substring(0, 705) + "... Read more")
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                addReadLess(text, textView, message, delegate, visibleMessageView,position, context, isOutgoing)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
                val isDayUiMode = UiModeUtilities.isDayUiMode(context)
                ds.color = if (isOutgoing) {
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

    private fun addReadLess(
        text: String,
        textView: TextView,
        message: MessageRecord,
        delegate: VisibleMessageViewDelegate,
        visibleMessageView: VisibleMessageView,
        position: Int,
        context: Context,
        isOutgoing: Boolean
    ) {
        val ss = SpannableString("$text Read less")
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                addReadMore(text, textView, message, delegate, visibleMessageView, position, context, isOutgoing)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
                val isDayUiMode = UiModeUtilities.isDayUiMode(context)
                ds.color = if (isOutgoing) {
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
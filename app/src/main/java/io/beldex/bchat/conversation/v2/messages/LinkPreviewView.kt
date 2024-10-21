package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import io.beldex.bchat.components.CornerMask
import io.beldex.bchat.conversation.v2.ModalUrlBottomSheet
import io.beldex.bchat.database.model.MmsMessageRecord
import io.beldex.bchat.mms.GlideRequests
import io.beldex.bchat.mms.ImageSlide
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.toPx
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewLinkPreviewBinding

class LinkPreviewView : LinearLayout {
    private val binding: ViewLinkPreviewBinding by lazy { ViewLinkPreviewBinding.bind(this) }
    private val cornerMask by lazy {
        CornerMask(
            this
        )
    }
    private var url: String? = null
    lateinit var bodyTextView: TextView

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    // endregion

    // region Updating
    fun bind(
        message: MmsMessageRecord,
        glide: GlideRequests,
        isStartOfMessageCluster: Boolean,
        isEndOfMessageCluster: Boolean
    ) {
        val linkPreview = message.linkPreviews.first()
        url = linkPreview.url
        // Thumbnail
        if (linkPreview.getThumbnail().isPresent) {
            // This internally fetches the thumbnail
            binding.thumbnailImageView.root.setImageResource(glide, ImageSlide(context, linkPreview.getThumbnail().get()), isPreview = false, message)
            binding.thumbnailImageView.root.loadIndicator.isVisible = false
        }
        // Title
        binding.titleTextView.text = linkPreview.title
        val textColorID = if (message.isOutgoing && UiModeUtilities.isDayUiMode(context)) {
            R.color.white
        } else {
            if (UiModeUtilities.isDayUiMode(context)) R.color.black else R.color.white
        }
        binding.titleTextView.setTextColor(ResourcesCompat.getColor(resources, textColorID, context.theme))
        // Body
        binding.titleTextView.setTextColor(ResourcesCompat.getColor(resources, textColorID, context.theme))
        val cardBackgroundColorID = if (message.isOutgoing) {
            R.color.outgoing_call_background
        } else {
            R.color.transaction_history_background
        }
        binding.linkPreviewCard.setCardBackgroundColor(ResourcesCompat.getColor(resources, cardBackgroundColorID, context.theme))

//        // Corner radii
//        val cornerRadii = MessageBubbleUtilities.calculateRadii(context, isStartOfMessageCluster, isEndOfMessageCluster, message.isOutgoing)
//        cornerMask.setTopLeftRadius(cornerRadii[0])
//        cornerMask.setTopRightRadius(cornerRadii[1])
//        cornerMask.setBottomRightRadius(cornerRadii[2])
//        cornerMask.setBottomLeftRadius(cornerRadii[3])
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        cornerMask.mask(canvas)
    }
    // endregion

    // region Interaction
    fun calculateHit(event: MotionEvent) {
        val rawXInt = event.rawX.toInt()
        val rawYInt = event.rawY.toInt()
        val hitRect = Rect(rawXInt, rawYInt, rawXInt, rawYInt)
        val previewRect = Rect()
        binding.mainLinkPreviewParent.getGlobalVisibleRect(previewRect)
        if (previewRect.contains(hitRect)) {
            openURL()
            return
        }
    }

    private fun openURL() {
        val url = this.url ?: return
        ActivityDispatcher.get(context)?.showBottomSheetDialog(ModalUrlBottomSheet(url),"Open URL Dialog")
    }
    // endregion
}
package io.beldex.bchat.conversation.v2.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentTransferProgress
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.MediaPreviewActivity
import io.beldex.bchat.components.CornerMask
import io.beldex.bchat.conversation.v2.utilities.ThumbnailView
import io.beldex.bchat.database.model.MmsMessageRecord
import com.bumptech.glide.RequestManager
import io.beldex.bchat.mms.Slide
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.messages.VisibleMessageContentView
import io.beldex.bchat.databinding.AlbumThumbnailViewBinding
import io.beldex.bchat.util.DateUtils
import java.util.Locale

class AlbumThumbnailView : RelativeLayout {

    companion object {
        const val MAX_ALBUM_DISPLAY_SIZE = 3
    }

    private val binding: AlbumThumbnailViewBinding by lazy { AlbumThumbnailViewBinding.bind(this) }

    // region Lifecycle
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val cornerMask by lazy {
        CornerMask(
            this
        )
    }
    private var slides: List<Slide> = listOf()
    private var slideSize: Int = 0


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        cornerMask.mask(canvas)
    }
    // endregion

    // region Interaction

    fun calculateHitObject(event: MotionEvent, mms: MmsMessageRecord, threadRecipient: Recipient,  onAttachmentNeedsDownload: (Long, Long) -> Unit) {
        val rawXInt = event.rawX.toInt()
        val rawYInt = event.rawY.toInt()
        val eventRect = Rect(rawXInt, rawYInt, rawXInt, rawYInt)
        val testRect = Rect()
        // test each album child
        binding.albumCellContainer.findViewById<ViewGroup>(R.id.album_thumbnail_root)?.children?.forEachIndexed forEach@{ index, child ->
            child.getGlobalVisibleRect(testRect)
            if (testRect.contains(eventRect)) {
                // hit intersects with this particular child
                val slide = slides.getOrNull(index) ?: return@forEach
                // only open to downloaded images
                if (slide.transferState == AttachmentTransferProgress.TRANSFER_PROGRESS_FAILED) {
                    // Restart download here (on IO thread)
                    (slide.asAttachment() as? DatabaseAttachment)?.let { attachment ->
                        onAttachmentNeedsDownload(attachment.attachmentId.rowId, mms.getId())
                    }
                }
                if (slide.isInProgress) return@forEach

                ActivityDispatcher.get(context)?.dispatchIntent { context ->
                    MediaPreviewActivity.getPreviewIntent(context, slide, mms, threadRecipient)
                }
            }
        }
    }

    fun clearViews() {
        binding.albumCellContainer.removeAllViews()
        slideSize = -1
    }

    fun bind(glideRequests: RequestManager, message: MmsMessageRecord,
             isStart: Boolean, isEnd: Boolean) {
        slides = message.slideDeck.thumbnailSlides
        if (slides.isEmpty()) {
            // this should never be encountered because it's checked by parent
            return
        }
        //calculateRadius(isStart, isEnd, message.isOutgoing)

        // recreate cell views if different size to what we have already (for recycling)
        if (slides.size != this.slideSize) {
            binding.albumCellContainer.removeAllViews()
            LayoutInflater.from(context).inflate(layoutRes(slides.size), binding.albumCellContainer)
            val overflowed = slides.size > MAX_ALBUM_DISPLAY_SIZE
            binding.albumCellContainer.findViewById<TextView>(R.id.album_cell_overflow_text)?.let { overflowText ->
                // overflowText will be null if !overflowed
                overflowText.isVisible = overflowed // more than max album size
                overflowText.text = context.getString(R.string.AlbumThumbnailView_plus, slides.size - MAX_ALBUM_DISPLAY_SIZE)
            }
            this.slideSize = slides.size
        }
        // iterate binding
        slides.take(MAX_ALBUM_DISPLAY_SIZE).forEachIndexed { position, slide ->
            val thumbnailView = getThumbnailView(position)
            thumbnailView.setImageResource(glideRequests, slide, isPreview = false, mms = message)
        }
    }

    // endregion


    fun layoutRes(slideCount: Int) = when (slideCount) {
        1 -> R.layout.album_thumbnail_1 // single
        2 -> R.layout.album_thumbnail_2// two sidebyside
        else -> R.layout.album_thumbnail_3 // three stacked with additional text
    }

    fun getThumbnailView(position: Int): ThumbnailView = when (position) {
        0 -> binding.albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_1)
        1 -> binding.albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_2)
        2 -> binding.albumCellContainer.findViewById<ViewGroup>(R.id.albumCellContainer).findViewById(R.id.album_cell_3)
        else -> throw Exception("Can't get thumbnail view for non-existent thumbnail at position: $position")
    }

    fun calculateRadius(isStart: Boolean, isEnd: Boolean, outgoing: Boolean) {
        val roundedDimen = context.resources.getDimension(R.dimen.message_corner_radius).toInt()
        val collapsedDimen = context.resources.getDimension(R.dimen.message_corner_collapse_radius).toInt()
        val (startTop, endTop, startBottom, endBottom) = when {
            // single message, consistent dimen
            isStart && isEnd -> intArrayOf(roundedDimen, roundedDimen, roundedDimen, roundedDimen)
            // start of message cluster, collapsed BL
            isStart -> intArrayOf(roundedDimen, roundedDimen, collapsedDimen, roundedDimen)
            // end of message cluster, collapsed TL
            isEnd -> intArrayOf(collapsedDimen, roundedDimen, roundedDimen, roundedDimen)
            // else in the middle, no rounding left side
            else -> intArrayOf(collapsedDimen, roundedDimen, collapsedDimen, roundedDimen)
        }
        // TL, TR, BR, BL (CW direction)
        cornerMask.setRadii(
                if (!outgoing) startTop else endTop, // TL
                if (!outgoing) endTop else startTop, // TR
                if (!outgoing) endBottom else startBottom, // BR
                if (!outgoing) startBottom else endBottom // BL
        )
    }

}
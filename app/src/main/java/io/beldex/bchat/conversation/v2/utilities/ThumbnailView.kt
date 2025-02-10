package io.beldex.bchat.conversation.v2.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ThumbnailViewBinding
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentTransferProgress
import com.beldex.libsignal.utilities.ListenableFuture
import com.beldex.libsignal.utilities.SettableFuture
import io.beldex.bchat.components.GlideBitmapListeningTarget
import io.beldex.bchat.components.GlideDrawableListeningTarget
import io.beldex.bchat.database.model.MmsMessageRecord
import io.beldex.bchat.mms.DecryptableStreamUriLoader.DecryptableUri
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import io.beldex.bchat.mms.Slide
import com.beldex.libbchat.utilities.Util.equals
import kotlin.Boolean
import kotlin.Int
import kotlin.getValue
import kotlin.lazy
import kotlin.let

open class ThumbnailView: FrameLayout {
    companion object {
        private const val WIDTH = 0
        private const val HEIGHT = 1
    }

    private val binding: ThumbnailViewBinding by lazy { ThumbnailViewBinding.bind(this) }

    // region Lifecycle
    constructor(context: Context) : super(context) { initialize(null) }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize(attrs) }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize(attrs) }


    val loadIndicator: View by lazy { binding.thumbnailLoadIndicator }

    private val dimensDelegate = ThumbnailDimensDelegate()

    private var slide: Slide? = null
    var radius: Int = 0

    private fun initialize(attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ThumbnailView, 0, 0)

            dimensDelegate.setBounds(typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_minWidth, 0),
                    typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_minHeight, 0),
                    typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_maxWidth, 0),
                    typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_maxHeight, 0))

            radius = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_thumbnail_radius, 0)

            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val adjustedDimens = dimensDelegate.resourceSize()
        if (adjustedDimens[WIDTH] == 0 && adjustedDimens[HEIGHT] == 0) {
            return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }

        val finalWidth: Int = adjustedDimens[WIDTH] + paddingLeft + paddingRight
        val finalHeight: Int = adjustedDimens[HEIGHT] + paddingTop + paddingBottom

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
        )
    }

    private fun getDefaultWidth() = maxOf(layoutParams?.width ?: 0, 0)
    private fun getDefaultHeight() = maxOf(layoutParams?.height ?: 0, 0)
    // endregion

    // region Interaction
    fun setImageResource(glide: RequestManager, slide: Slide, isPreview: Boolean, mms: MmsMessageRecord?): ListenableFuture<Boolean> {
        return setImageResource(glide, slide, isPreview, 0, 0, mms)
    }

    fun setImageResource(glide: RequestManager, slide: Slide,
                         isPreview: Boolean, naturalWidth: Int,
                         naturalHeight: Int, mms: MmsMessageRecord?
    ): ListenableFuture<Boolean> {

        val currentSlide = this.slide

        binding.playOverlay.isVisible = (slide.thumbnailUri != null && slide.hasPlayOverlay() &&
                (slide.transferState == AttachmentTransferProgress.TRANSFER_PROGRESS_DONE || isPreview))

        if (equals(currentSlide, slide)) {
            // don't re-load slide
            return SettableFuture(false)
        }


        if (currentSlide != null && currentSlide.fastPreflightId != null && currentSlide.fastPreflightId == slide.fastPreflightId) {
            // not reloading slide for fast preflight
            this.slide = slide
        }

        this.slide = slide

        binding.thumbnailLoadIndicator.isVisible = slide.isInProgress
        binding.thumbnailDownloadIcon.isVisible = slide.transferState == AttachmentTransferProgress.TRANSFER_PROGRESS_FAILED

        dimensDelegate.setDimens(naturalWidth, naturalHeight)
        invalidate()

        val result = SettableFuture<Boolean>()

        when {
            slide.thumbnailUri != null -> {
                buildThumbnailGlideRequest(glide, slide).into(
                        GlideDrawableListeningTarget(
                                binding.thumbnailImage,
                                result
                        )
                )
            }
            slide.hasPlaceholder() -> {
                buildPlaceholderGlideRequest(glide, slide).into(
                        GlideBitmapListeningTarget(
                                binding.thumbnailImage,
                                result
                        )
                )
            }
            else -> {
                glide.clear(binding.thumbnailImage)
                result.set(false)
            }
        }
        return result
    }

    fun buildThumbnailGlideRequest(glide: RequestManager, slide: Slide): RequestBuilder<Drawable> {

        val dimens = dimensDelegate.resourceSize()

        val request = glide.load(DecryptableUri(slide.thumbnailUri!!))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .let { request ->
                    if (dimens[WIDTH] == 0 || dimens[HEIGHT] == 0) {
                        request.override(getDefaultWidth(), getDefaultHeight())
                    } else {
                        request.override(dimens[WIDTH], dimens[HEIGHT])
                    }
                }
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()

        return if (slide.isInProgress) request else request.apply(RequestOptions.errorOf(R.drawable.ic_missing_thumbnail_picture))
    }

    fun buildPlaceholderGlideRequest(glide: RequestManager, slide: Slide): RequestBuilder<Bitmap> {

        val dimens = dimensDelegate.resourceSize()

        return glide.asBitmap()
                .load(slide.getPlaceholderRes(context.theme))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .let { request ->
                    if (dimens[WIDTH] == 0 || dimens[HEIGHT] == 0) {
                        request.override(getDefaultWidth(), getDefaultHeight())
                    } else {
                        request.override(dimens[WIDTH], dimens[HEIGHT])
                    }
                }
                .fitCenter()
    }

    open fun clear(glideRequests: RequestManager) {
        glideRequests.clear(binding.thumbnailImage)
        slide = null
    }

    fun setImageResource(glideRequests: RequestManager, uri: Uri): ListenableFuture<Boolean> {
        val future = SettableFuture<Boolean>()

        var request: RequestBuilder<Drawable> = glideRequests.load(DecryptableUri(uri))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())

        request = if (radius > 0) {
            request.transforms(CenterCrop(), RoundedCorners(radius))
        } else {
            request.transforms(CenterCrop())
        }

        request.into(
                GlideDrawableListeningTarget(
                        binding.thumbnailImage,
                        future
                )
        )

        return future
    }

    // endregion

}
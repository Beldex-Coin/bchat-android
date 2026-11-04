package io.beldex.bchat.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.beldex.bchat.compose_utils.AVATAR_NOTCH_FRACTION

class NotchedAvatarImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val cornerPath = Path()
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val width = w.toFloat()
        val cut = minOf(w, h) * AVATAR_NOTCH_FRACTION
        cornerPath.reset()
        // The triangle that gets erased from the top-right corner (extended past the bounds so
        // anti-aliasing leaves no sliver on the edges).
        cornerPath.moveTo(width - cut, -1f)
        cornerPath.lineTo(width + 1f, -1f)
        cornerPath.lineTo(width + 1f, cut)
        cornerPath.close()
    }

    override fun draw(canvas: Canvas) {
        if (width <= 0 || height <= 0) {
            super.draw(canvas)
            return
        }
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.draw(canvas)
        canvas.drawPath(cornerPath, cornerPaint)
        canvas.restoreToCount(layer)
    }
}

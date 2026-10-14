package io.beldex.bchat.compose_utils

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Background with the Revamp_2026 cut corners (top-right and bottom-left), drawn at the view's real
 * size so wrap-content chips never stretch the way a fixed vector would.
 */
class NotchedDrawable(
    private val notchPx: Float,
    private val strokePx: Float,
    fillColor: Int,
    strokeColor: Int
) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = strokePx
    }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val half = strokePx / 2
        val l = half
        val t = half
        val r = bounds.width() - half
        val b = bounds.height() - half
        val n = minOf(notchPx, (r - l) / 2, (b - t) / 2)
        path.reset()
        path.moveTo(l, t)
        path.lineTo(r - n, t)
        path.lineTo(r, t + n)
        path.lineTo(r, b)
        path.lineTo(l + n, b)
        path.lineTo(l, b - n)
        path.close()
        canvas.drawPath(path, fill)
        if (strokePx > 0f) canvas.drawPath(path, line)
    }

    override fun setAlpha(alpha: Int) {
        fill.alpha = alpha
        line.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

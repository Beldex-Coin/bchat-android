package io.beldex.bchat.compose_utils

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class InnerTileDrawable(
    private val notchPx: Float,
    private val barWidthPx: Float,
    fillColor: Int,
    barColor: Int
) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
    private val bar = Paint().apply { color = barColor }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val n = minOf(notchPx, w / 2, h / 2)
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(w - n, 0f)
        path.lineTo(w, n)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.drawPath(path, fill)
        if (barWidthPx > 0f) canvas.drawRect(0f, 0f, barWidthPx, h, bar)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        fill.alpha = alpha
        bar.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

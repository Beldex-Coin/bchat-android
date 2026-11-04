package io.beldex.bchat.conversation.v2.input_bar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min


class WaveFormView(context: Context?, attr: AttributeSet?) : View(context, attr) {

    private val density = resources.displayMetrics.density
    private val barWidth = 2f * density
    private val barGap = 2f * density
    private val minBarHeight = 2f * density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEBEBEB.toInt() }
    private val amplitudes = ArrayList<Float>()
    private val rect = RectF()

    fun addAmplitude(amp: Float) {
        // perceptual-ish scaling so normal speech fills most of the height
        val level = min(1f, max(0f, amp) / 12000f)
        amplitudes.add(level)
        val maxBars = if (width > 0) (width / (barWidth + barGap)).toInt() + 1 else 200
        while (amplitudes.size > maxBars) amplitudes.removeAt(0)
        invalidate()
    }

    fun clear() {
        amplitudes.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return
        val h = height.toFloat()
        val centre = h / 2f
        val radius = barWidth / 2f
        var x = width.toFloat() - barWidth
        for (i in amplitudes.indices.reversed()) {
            if (x < 0) break
            val barHeight = max(minBarHeight, amplitudes[i] * h)
            rect.set(x, centre - barHeight / 2f, x + barWidth, centre + barHeight / 2f)
            canvas.drawRoundRect(rect, radius, radius, paint)
            x -= barWidth + barGap
        }
    }
}

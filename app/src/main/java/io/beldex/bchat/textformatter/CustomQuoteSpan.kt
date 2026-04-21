package io.beldex.bchat.textformatter

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class CustomQuoteSpan(
    private val stripeColor: Int = 0xFFCCCCCC.toInt(), // light gray
    private val stripeWidth: Float = 6f,               // thin vertical stripe
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        // Width for the vertical bar + some padding
        return paint.measureText("│ ").toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val oldColor = paint.color
        val strokeWidth = paint.strokeWidth

        paint.color = stripeColor
        paint.strokeWidth = stripeWidth

        // Draw vertical bar
        canvas.drawLine(x + 10, top.toFloat(), x + 10, bottom.toFloat(), paint)

        paint.color = oldColor
        paint.strokeWidth = strokeWidth
    }
}
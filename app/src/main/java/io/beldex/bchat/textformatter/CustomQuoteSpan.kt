package io.beldex.bchat.textformatter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan
import androidx.core.content.ContextCompat
import io.beldex.bchat.R

class CustomQuoteSpan(
    context: Context
) : android.text.style.LineBackgroundSpan {

    private val stripeColor =
        ContextCompat.getColor(context, R.color.quote_gray)
    private val stripeWidth: Float = 6f

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {

        val oldColor = paint.color
        val oldStroke = paint.strokeWidth
        val oldStyle = paint.style

        paint.color = stripeColor
        paint.strokeWidth = stripeWidth
        paint.style = Paint.Style.FILL

        // vertical quote line
        canvas.drawLine(
            left.toFloat(),
            top.toFloat(),
            left.toFloat(),
            bottom.toFloat(),
            paint
        )

        paint.color = oldColor
        paint.strokeWidth = oldStroke
        paint.style = oldStyle
    }
}

class QuoteIndentSpan(private val margin: Int) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return margin
    }

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout
    ) {
        // no-op
    }
}
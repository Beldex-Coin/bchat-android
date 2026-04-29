package io.beldex.bchat.textformatter

import android.graphics.Typeface
import android.text.style.TypefaceSpan

class MonospaceSpan : TypefaceSpan("monospace") {
    override fun updateDrawState(ds: android.text.TextPaint) {
        apply(ds)
    }

    override fun updateMeasureState(paint: android.text.TextPaint) {
        apply(paint)
    }

    private fun apply(paint: android.text.TextPaint) {
        val old = paint.typeface
        val oldStyle = old?.style ?: 0

        val tf = Typeface.MONOSPACE
        val fake = oldStyle and tf.style.inv()

        paint.isFakeBoldText = false
        paint.textSkewX = 0f

        if (fake and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }
        if (fake and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }

        paint.typeface = tf
    }
}
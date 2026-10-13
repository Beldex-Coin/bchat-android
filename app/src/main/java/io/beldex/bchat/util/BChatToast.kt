package io.beldex.bchat.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import io.beldex.bchat.R

/**
 * Revamp_2026 toast (Figma "Frame 2792"): dark #1A1A1A notched box with a green outline, a green
 * check icon and a Roboto Mono message. Drop-in replacement for [Toast.makeText].
 */
object BChatToast {

    @JvmStatic
    fun makeText(context: Context?, @StringRes resId: Int, duration: Int): Toast =
        makeText(context, context!!.getText(resId), duration)

    @JvmStatic
    fun makeText(context: Context?, text: CharSequence?, duration: Int): Toast {
        requireNotNull(context) { "BChatToast needs a context" }
        val toast = Toast(context.applicationContext)
        toast.duration = duration
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(context, 96))
        toast.view = buildView(context, text ?: "")
        return toast
    }

    private fun buildView(context: Context, text: CharSequence): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(context, 42)
            setPadding(dp(context, 22), dp(context, 10), dp(context, 22), dp(context, 10))
            background = NotchedToastDrawable(dp(context, 10).toFloat(), dp(context, 1).toFloat())
        }
        row.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_toast_check)
        }, LinearLayout.LayoutParams(dp(context, 16), dp(context, 16)))
        row.addView(TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFFEBEBEB.toInt())
            typeface = ResourcesCompat.getFont(context, R.font.roboto_mono)
            maxLines = 4
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(context, 14) })
        return row
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    /** Top-right and bottom-left corners cut, like every notched surface in the revamp. */
    private class NotchedToastDrawable(private val notch: Float, private val stroke: Float) : Drawable() {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A1A1A.toInt() }
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF078720.toInt()
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        private val path = Path()

        override fun draw(canvas: Canvas) {
            val l = stroke / 2; val t = stroke / 2
            val r = bounds.width() - stroke / 2; val b = bounds.height() - stroke / 2
            path.reset()
            path.moveTo(l, t)
            path.lineTo(r - notch, t)
            path.lineTo(r, t + notch)
            path.lineTo(r, b)
            path.lineTo(l + notch, b)
            path.lineTo(l, b - notch)
            path.close()
            canvas.drawPath(path, fill)
            canvas.drawPath(path, line)
        }

        override fun setAlpha(alpha: Int) { fill.alpha = alpha; line.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}

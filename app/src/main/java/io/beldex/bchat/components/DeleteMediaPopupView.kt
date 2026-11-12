package io.beldex.bchat.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.InnerTileDrawable
import io.beldex.bchat.compose_utils.NotchedDrawable
import io.beldex.bchat.util.UiModeUtilities

class DeleteMediaPopupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    override fun onFinishInflate() {
        super.onFinishInflate()
        val day = UiModeUtilities.isDayUiMode(context)
        val density = resources.displayMetrics.density
        val stroke = 1f * density
        val notch = 12f * density

        background = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = 260f * density
            colors = if (day) intArrayOf(Color.argb(0, 255, 255, 255), Color.rgb(240, 240, 240))
            else intArrayOf(Color.argb(0, 26, 26, 26), Color.rgb(11, 11, 11))
            setStroke(stroke.toInt().coerceAtLeast(1), if (day) Color.rgb(208, 208, 208) else Color.rgb(68, 68, 68))
        }

        findViewById<View>(R.id.deleteMediaIconTile)?.background = InnerTileDrawable(
            notch, 0f, ContextCompat.getColor(context, R.color.reply_draft_card_background), Color.TRANSPARENT
        )

        findViewById<View>(R.id.cancelMediaDialogBoxButton)?.let {
            flat(it)
            it.background = NotchedDrawable(
                notch, stroke,
                if (day) Color.WHITE else Color.rgb(17, 17, 17),
                if (day) Color.rgb(208, 208, 208) else Color.rgb(68, 68, 68)
            )
        }
        findViewById<View>(R.id.deleteMediaDialogBoxButton)?.let {
            flat(it)
            it.background = NotchedDrawable(notch, stroke, Color.argb(26, 255, 62, 62), Color.rgb(255, 62, 62))
        }
    }

    /** Buttons pick up a theme tint and shadow that would cover the Figma backgrounds. */
    private fun flat(button: View) {
        ViewCompat.setBackgroundTintList(button, null)
        (button as? com.google.android.material.button.MaterialButton)?.backgroundTintList = null
        button.stateListAnimator = null
        button.elevation = 0f
    }
}

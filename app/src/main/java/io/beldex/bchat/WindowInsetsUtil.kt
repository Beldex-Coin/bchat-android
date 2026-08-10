package io.beldex.bchat

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.appcompat.widget.Toolbar
import kotlin.math.max

object WindowInsetsUtil {

    private val SYSTEM_BARS_AND_CUTOUT =
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

    fun applyTopInset(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(SYSTEM_BARS_AND_CUTOUT).top
            v.updatePadding(top = top)
            insets
        }
    }

    fun applySafeDrawingInsets(view: View) {
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(SYSTEM_BARS_AND_CUTOUT)
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun applySafeDrawingAndImeInsets(view: View) {
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(SYSTEM_BARS_AND_CUTOUT)
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.setPadding(
                left + bars.left,
                top + bars.top,
                right + bars.right,
                bottom + max(bars.bottom, imeBottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun applyBottomInset(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            ).bottom
            v.updatePadding(bottom = bars)
            insets
        }
    }

    fun applyTopAndImeInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(SYSTEM_BARS_AND_CUTOUT).top
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(top = top, bottom = imeBottom)
            insets
        }
    }

    fun applyToolbarInsets(toolbar: Toolbar) {

        val left = toolbar.paddingLeft
        val top = toolbar.paddingTop
        val right = toolbar.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->

            val bars = insets.getInsets(SYSTEM_BARS_AND_CUTOUT)

            view.setPadding(
                left + bars.left,
                top + bars.top,
                right + bars.right,
                view.paddingBottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(toolbar)
    }
}
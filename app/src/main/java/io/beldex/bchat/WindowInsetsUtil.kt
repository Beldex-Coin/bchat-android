package io.beldex.bchat

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.appcompat.widget.Toolbar

object WindowInsetsUtil {

    fun applyTopInset(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.updatePadding(top = top)
            insets
        }
    }

    fun applyBottomInset(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = bars)
            insets
        }
    }

    fun applyTopAndImeInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
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

            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

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
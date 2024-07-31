package io.beldex.bchat.keyboard.layouts

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.CallSuper
import io.beldex.bchat.keyboard.utils.ComponentUtils

abstract class ResizableRelativeLayout(
    context: Context, attr: AttributeSet
) :
    RelativeLayout(context, attr) {

    val Int.toPx: Int
        get() = ComponentUtils.dpToPx(context, this)

    val Int.toDp: Int
        get() = ComponentUtils.pxToDp(context, this)

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resetContent()
    }

    @CallSuper
    fun resetContent() {
        removeAllViews()
        // Adding a delay gives the parent activity time to handle its configuration change
        // prior to us handling ours. Otherwise we run into several issues, including the
        // screen size property of our parent window not updating prior to us accessing it.
        Handler().postDelayed({ configureSelf() }, 1) // delayMillis 50 to 1
    }

    protected abstract fun configureSelf()
}
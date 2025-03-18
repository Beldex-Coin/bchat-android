package com.beldex.libbchat.utilities

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

val RecyclerView.isScrolledToBottom: Boolean
    get() {
        return max(0, computeVerticalScrollOffset()) + computeVerticalScrollExtent() >= computeVerticalScrollRange()
    }

inline fun <reified LP: ViewGroup.LayoutParams> View.modifyLayoutParams(function: LP.() -> Unit) {
    layoutParams = (layoutParams as LP).apply { function() }
}
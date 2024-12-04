package io.beldex.bchat.components.menu

import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * Represents an action to be rendered
 */
data class ActionItem(
    @AttrRes val iconRes: Int,
    val title: CharSequence,
    val action: Runnable,
    @ColorInt val color: Int? = null,
)
package io.beldex.bchat.components.menu

import androidx.annotation.AttrRes
/**
 * Represents an action to be rendered
 */
data class ActionItem(
    @AttrRes val iconRes: Int,
    val title: CharSequence,
    val action: Runnable
)
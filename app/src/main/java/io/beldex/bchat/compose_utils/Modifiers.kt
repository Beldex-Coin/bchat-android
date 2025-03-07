package io.beldex.bchat.compose_utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.noRippleCallback(onClick: () -> Unit) = composed {
    clickable(
        indication = null,
        interactionSource = remember {
            MutableInteractionSource()
        }
    ) {
        onClick()
    }
}
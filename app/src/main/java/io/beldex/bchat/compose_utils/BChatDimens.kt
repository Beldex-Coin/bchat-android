package io.beldex.bchat.compose_utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared sizes for the Revamp_2026 look, so shapes and buttons don't carry loose literals. */
object BChatDimens {
    // Corner cuts of notchedCornerShape(): top-right and bottom-left.
    val NotchSmall: Dp = 8.dp
    val NotchMedium: Dp = 12.dp
    val NotchLarge: Dp = 14.5.dp
    val NotchCard: Dp = 20.dp

    // Minimum button heights. Used with heightIn(min = …) so large font sizes can grow the button.
    val DialogButtonMinHeight: Dp = 52.dp
    val PrimaryButtonMinHeight: Dp = 54.dp
}

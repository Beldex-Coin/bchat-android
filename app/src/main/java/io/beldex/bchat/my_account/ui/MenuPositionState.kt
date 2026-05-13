package io.beldex.bchat.my_account.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

data class MenuPositionState(
    val offset: Offset = Offset.Zero,
    val itemHeight: Int = 0
)

@Composable
fun rememberMenuPosition(
    menuPositionState: MenuPositionState,
    menuWidthEstimate: Dp = 200.dp
): IntOffset {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val menuWidthPx = with(density) { menuWidthEstimate.toPx() }

    val itemTop = menuPositionState.offset.y
    val yOffset = itemTop.toInt()
    val xOffset = ((screenWidthPx - menuWidthPx) / 2).toInt()

    return IntOffset(
        x = xOffset,
        y = yOffset
    )
}
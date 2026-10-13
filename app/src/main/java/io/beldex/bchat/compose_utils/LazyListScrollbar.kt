package io.beldex.bchat.compose_utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/** Thin scrollbar (Figma "Rectangle 6364": 3dp wide, #444444) drawn over the right edge of a lazy list. */
fun Modifier.lazyListScrollbar(
    state: LazyListState,
    color: Color = Color(0xFF444444),
    widthPx: Float = 9f
): Modifier = drawWithContent {
    drawContent()
    val info = state.layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo
    if (total == 0 || visible.isEmpty() || visible.size >= total && !state.canScrollForward && !state.canScrollBackward) return@drawWithContent
    val first = visible.first()
    val itemSize = (first.size + 1).toFloat()
    val progress = (first.index + (-first.offset / itemSize)) / total
    val thumbHeight = (size.height * visible.size / total).coerceAtLeast(24f)
    val top = (size.height * progress).coerceIn(0f, size.height - thumbHeight)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - widthPx, top),
        size = Size(widthPx, thumbHeight),
        cornerRadius = CornerRadius(widthPx / 2)
    )
}

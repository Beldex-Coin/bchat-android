package io.beldex.bchat.compose_utils

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * The cut-corner button/card shape used throughout the Revamp_2026 design: the top-right and
 * bottom-left corners are cut off at 45°, the other two corners stay square. Notch size is a
 * fixed dp value regardless of component size, matching the Figma spec (e.g. the onboarding CTA
 * buttons use a ~14.5dp notch on a 54dp-tall button).
 */
@Composable
fun notchedCornerShape(notch: Dp): Shape {
    val density = LocalDensity.current
    return remember(notch, density) {
        val notchPx = with(density) { notch.toPx() }
        GenericShape { size, _ ->
            val n = notchPx.coerceAtMost(minOf(size.width, size.height) / 2f)
            moveTo(0f, 0f)
            lineTo(size.width - n, 0f)
            lineTo(size.width, n)
            lineTo(size.width, size.height)
            lineTo(n, size.height)
            lineTo(0f, size.height - n)
            close()
        }
    }
}

/**
 * The single corner that gets cut off by [singleCornerCutShape].
 */
enum class NotchCorner {
    TopRight,
    TopLeft,
    BottomRight,
    BottomLeft
}
@Composable
fun singleCornerCutShape(
    notch: Dp,
    corner: NotchCorner = NotchCorner.TopRight
): Shape {
    val density = LocalDensity.current
    return remember(notch, corner, density) {
        val notchPx = with(density) { notch.toPx() }
        GenericShape { size, _ ->
            val n = notchPx.coerceAtMost(minOf(size.width, size.height) / 2f)
            when (corner) {
                NotchCorner.TopRight -> {
                    moveTo(0f, 0f)
                    lineTo(size.width - n, 0f)
                    lineTo(size.width, n)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                }
                NotchCorner.TopLeft -> {
                    moveTo(n, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    lineTo(0f, n)
                }
                NotchCorner.BottomRight -> {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height - n)
                    lineTo(size.width - n, size.height)
                    lineTo(0f, size.height)
                }
                NotchCorner.BottomLeft -> {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(n, size.height)
                    lineTo(0f, size.height - n)
                    lineTo(0f, 0f)
                }
            }
            close()
        }
    }
}

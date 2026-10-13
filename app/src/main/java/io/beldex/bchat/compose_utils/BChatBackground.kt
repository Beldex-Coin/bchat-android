package io.beldex.bchat.compose_utils

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

private val BackgroundBase = Color(0xFF0B0B0B)
private val DotColor = Color(0xFFD9D9D9).copy(alpha = 0.15f)

/**
 * Revamp_2026 screen background (Figma frame fills): #0B0B0B base, an elliptical radial vignette
 * centred at 43.5% of the height, and a faint dot grid (1.5dp dots every 6dp). Dark theme only;
 * light theme keeps the flat [fallback] colour.
 */
fun Modifier.bchatBackground(fallback: Color = BackgroundBase): Modifier = composed {
    val context = LocalContext.current
    val isDark = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
    if (!isDark) {
        Modifier.drawBehind { drawRect(fallback) }
    } else {
        val density = LocalDensity.current
        val dotBrush = remember(density.density) {
            val pitch = (6 * density.density).toInt().coerceAtLeast(2)
            val tile = ImageBitmap(pitch, pitch, ImageBitmapConfig.Argb8888)
            val canvas = Canvas(tile)
            val paint = Paint().apply { color = DotColor; isAntiAlias = true }
            canvas.drawCircle(Offset(pitch / 2f, pitch / 2f), 0.75f * density.density, paint)
            ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
        }
        Modifier.drawBehind {
            drawRect(BackgroundBase)
            val rx = size.width * 0.5f
            val ry = size.height * 0.565f
            val center = Offset(size.width * 0.5f, size.height * 0.435f)
            scale(scaleX = 1f, scaleY = ry / rx, pivot = center) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1A1A).copy(alpha = 0.6f), Color.Black),
                        center = center,
                        radius = rx
                    )
                )
            }
            drawRect(brush = dotBrush)
        }
    }
}

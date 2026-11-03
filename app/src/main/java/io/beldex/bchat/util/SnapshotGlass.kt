package io.beldex.bchat.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

class WindowSnapshot(
    val image: ImageBitmap,
    val screenX: Int,
    val screenY: Int
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Captures the whole activity window. Call this right before showing the popup. */
fun captureWindowSnapshot(view: View, onResult: (WindowSnapshot?) -> Unit) {
    val window = view.context.findActivity()?.window
    val root = window?.decorView
    if (window == null || root == null || root.width <= 0 || root.height <= 0 ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O
    ) {
        onResult(null)
        return
    }
    val loc = IntArray(2)
    root.getLocationOnScreen(loc)
    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    try {
        PixelCopy.request(window, bitmap, { result ->
            onResult(
                if (result == PixelCopy.SUCCESS)
                    WindowSnapshot(bitmap.asImageBitmap(), loc[0], loc[1])
                else null
            )
        }, Handler(Looper.getMainLooper()))
    } catch (e: Exception) {
        onResult(null)
    }
}

/** Transparent space kept around the glass so its drop shadow isn't clipped by the popup window. */
val SnapshotGlassShadowPadding = 28.dp

@Composable
fun SnapshotGlass(
    snapshot: WindowSnapshot?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,                 // square corners (use RoundedCornerShape(..) for round)
    blurRadius: Dp = 24.dp,
    saturation: Float = 1f,
    tint: Color = Color(0xE0101010),               // ~88% near-black, blur only faintly visible
    fallbackTint: Color = Color(0xF2101010),       // no blur available (Android 11 and below)
    borderBrush: Brush = SolidColor(Color(0x1AFFFFFF)),
    shadowElevation: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val popupView = LocalView.current
    val pad = with(LocalDensity.current) { (blurRadius * 2).roundToPx() }
    var origin by remember { mutableStateOf(IntOffset.Zero) }
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && snapshot != null

    // Outer padding = room for the shadow inside the popup window
    Box(modifier = modifier.padding(SnapshotGlassShadowPadding)) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(shape)
                .border(1.dp, borderBrush, shape)
                .onGloballyPositioned { coords ->
                    if (snapshot != null) {
                        val loc = IntArray(2)
                        popupView.getLocationOnScreen(loc)
                        val p = coords.positionInWindow()
                        origin = IntOffset(
                            (loc[0] + p.x).roundToInt() - snapshot.screenX,
                            (loc[1] + p.y).roundToInt() - snapshot.screenY
                        )
                    }
                }
        ) {
            if (canBlur && snapshot != null) {
                // Blurred copy of the screen, drawn larger so blur edges never show
                Box(
                    Modifier
                        .matchParentSize()
                        .layout { measurable, constraints ->
                            val w = constraints.maxWidth
                            val h = constraints.maxHeight
                            val placeable = measurable.measure(
                                Constraints.fixed(w + pad * 2, h + pad * 2)
                            )
                            layout(w, h) { placeable.place(-pad, -pad) }
                        }
                        .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
                        .drawBehind {
                            drawImage(
                                snapshot.image,
                                topLeft = Offset(
                                    (pad - origin.x).toFloat(),
                                    (pad - origin.y).toFloat()
                                ),
                                colorFilter = ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(saturation) }
                                )
                            )
                        }
                )
                Box(Modifier
                    .matchParentSize()
                    .background(tint))
            } else {
                Box(Modifier
                    .matchParentSize()
                    .background(fallbackTint))
            }
            content()
        }
    }
}
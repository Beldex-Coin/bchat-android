package io.beldex.bchat.compose_utils

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline as ComposeOutline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

// Revamp_2026 avatar (Figma home rows, Image 7 7546:1821): a square with the top-right corner cut.
const val AVATAR_NOTCH_FRACTION = 0.215f

object NotchedAvatarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): ComposeOutline {
        val cut = minOf(size.width, size.height) * AVATAR_NOTCH_FRACTION
        return ComposeOutline.Generic(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width - cut, 0f)
                lineTo(size.width, cut)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        )
    }
}

class NotchedAvatarOutlineProvider : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        val w = view.width
        val h = view.height
        if (w <= 0 || h <= 0) return
        val cut = minOf(w, h) * AVATAR_NOTCH_FRACTION
        outline.setConvexPath(
            android.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(w - cut, 0f)
                lineTo(w.toFloat(), cut)
                lineTo(w.toFloat(), h.toFloat())
                lineTo(0f, h.toFloat())
                close()
            }
        )
    }
}

fun View.clipToNotchedAvatar() {
    outlineProvider = NotchedAvatarOutlineProvider()
    clipToOutline = true
}

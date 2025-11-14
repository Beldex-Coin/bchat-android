package io.beldex.bchat.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import androidx.annotation.DimenRes
import android.view.View
import androidx.core.graphics.applyCanvas
import android.util.Size
import kotlin.math.roundToInt

fun View.contains(point: PointF): Boolean {
    return hitRect.contains(point.x.toInt(), point.y.toInt())
}

val View.hitRect: Rect
    get()  {
        val rect = Rect()
        getHitRect(rect)
        return rect
    }

fun View.animateSizeChange(@DimenRes startSizeID: Int, @DimenRes endSizeID: Int, animationDuration: Long = 250) {
    val startSize = resources.getDimension(startSizeID)
    val endSize = resources.getDimension(endSizeID)
    animateSizeChange(startSize, endSize)
}

fun View.animateSizeChange(startSize: Float, endSize: Float, animationDuration: Long = 250) {
    val layoutParams = this.layoutParams
    val animation = ValueAnimator.ofObject(FloatEvaluator(), startSize, endSize)
    animation.duration = animationDuration
    animation.addUpdateListener { animator ->
        val size = animator.animatedValue as Float
        layoutParams.width = size.toInt()
        layoutParams.height = size.toInt()
        this.layoutParams = layoutParams
    }
    animation.start()
}

fun View.fadeIn(duration: Long = 150) {
    alpha = 0.0f
    visibility = View.VISIBLE
    animate().setDuration(duration).alpha(1.0f).setListener(null).start()
}

fun View.fadeOut(duration: Long = 150) {
    animate().setDuration(duration).alpha(0.0f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            visibility = View.GONE
        }
    })
}

fun View.drawToBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888, longestWidth: Int = 2000): Bitmap {
    val size = Size(measuredWidth, measuredHeight).coerceAtMost(longestWidth)
    val scale = size.width / measuredWidth.toFloat()

    return Bitmap.createBitmap(size.width, size.height, config).applyCanvas {
        scale(scale, scale)
        translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(this)
    }
}

fun Size.coerceAtMost(longestWidth: Int): Size =
    (width.toFloat() / height).let { aspect ->
        if (aspect > 1) {
            width.coerceAtMost(longestWidth).let { Size(it, (it / aspect).roundToInt()) }
        } else {
            height.coerceAtMost(longestWidth).let { Size((it * aspect).roundToInt(), it) }
        }
    }
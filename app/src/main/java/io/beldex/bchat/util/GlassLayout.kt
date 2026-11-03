package io.beldex.bchat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import io.beldex.bchat.R
import kotlin.math.min


class GlassLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    var cornerRadius: Float = 28f * density
        set(value) { field = value; invalidateOutline(); invalidate() }

    var blurRadius: Float = 24f * density
        set(value) { field = value; invalidate() }

    @ColorInt var tintColor: Int = Color.argb(150, 17, 17, 17)
        set(value) { field = value; invalidate() }

    @ColorInt var fallbackTintColor: Int = Color.argb(235, 17, 17, 17)
        set(value) { field = value; invalidate() }

    @ColorInt var borderColor: Int = Color.argb(31, 255, 255, 255)
        set(value) { field = value; borderPaint.color = value; buildShaders(); invalidate() }

    /** Redraw every frame so the glass follows scrolling/animations behind it. */
    var liveUpdate: Boolean = true

    /** 1 = normal, >1 = colors behind the glass glow through (the "glassy" part). */
    var saturation: Float = 1.4f
        set(value) { field = value; invalidate() }

    /** Strength of the soft white light on the top edge (0 = off). */
    var sheenAlpha: Float = 0.14f
        set(value) { field = value; buildShaders(); invalidate() }

    /** false = never guess a blur source (use setBlurSource / setSnapshot only). */
    var autoDetectSource: Boolean = true

    private var blurSourceId: Int = NO_ID
    private var explicitSource: View? = null
    private var autoSource: View? = null

    private val renderNode: RenderNode? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderNode("glass") else null

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
        color = borderColor
    }
    private val borderRect = RectF()
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val myLoc = IntArray(2)
    private val srcLoc = IntArray(2)

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (liveUpdate && isShown) invalidate()
        true
    }

    init {
        setWillNotDraw(false)

        // Default shadow only if android:elevation is not set in XML
        context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.elevation)).apply {
            if (!hasValue(0)) elevation = 16f * density
            recycle()
        }
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val r = min(cornerRadius, min(view.width, view.height) / 2f)
                outline.setRoundRect(0, 0, view.width, view.height, r)
            }
        }

        context.obtainStyledAttributes(attrs, R.styleable.GlassLayout).apply {
            cornerRadius = getDimension(R.styleable.GlassLayout_glass_cornerRadius, cornerRadius)
            blurRadius = getDimension(R.styleable.GlassLayout_glass_blurRadius, blurRadius)
            tintColor = getColor(R.styleable.GlassLayout_glass_tintColor, tintColor)
            fallbackTintColor =
                getColor(R.styleable.GlassLayout_glass_fallbackTintColor, fallbackTintColor)
            borderColor = getColor(R.styleable.GlassLayout_glass_borderColor, borderColor)
            liveUpdate = getBoolean(R.styleable.GlassLayout_glass_liveUpdate, liveUpdate)
            blurSourceId = getResourceId(R.styleable.GlassLayout_glass_blurSource, NO_ID)
            saturation = getFloat(R.styleable.GlassLayout_glass_saturation, saturation)
            sheenAlpha = getFloat(R.styleable.GlassLayout_glass_sheen, sheenAlpha)
            autoDetectSource = getBoolean(R.styleable.GlassLayout_glass_autoSource, autoDetectSource)
            recycle()
        }
    }

    /** Optional: set the source from code. Pass null to go back to auto-detect. */
    fun setBlurSource(view: View?) {
        explicitSource = view
        invalidate()
    }

    private var snapshot: Bitmap? = null
    private var snapshotX = 0
    private var snapshotY = 0

    /**
     * Use a screenshot instead of a live view. Needed for overlays that sit ON TOP of the
     * screen they blur (a live source would contain the glass itself) and for PopupWindows.
     * [screenX]/[screenY] = where the bitmap's top-left sits on the screen.
     * Pass null to go back to the live blur source.
     */
    fun setSnapshot(bitmap: Bitmap?, screenX: Int = 0, screenY: Int = 0) {
        snapshot = bitmap
        snapshotX = screenX
        snapshotY = screenY
        invalidate()
    }

    // ---------------- Source resolution ----------------

    private fun resolveSource(): View? {
        explicitSource?.let { if (it.isAttachedToWindow) return it }

        if (blurSourceId != NO_ID) {
            var p = parent
            while (p is View) {
                val found = p.findViewById<View>(blurSourceId)
                if (found != null && found !== this && !isAncestorOrSelf(found)) {
                    explicitSource = found
                    return found
                }
                p = p.parent
            }
        }

        if (!autoDetectSource) return null
        autoSource?.let { if (it.isAttachedToWindow) return it }
        autoSource = findAutoSource()
        return autoSource
    }

    private fun isAncestorOrSelf(v: View): Boolean {
        var p: Any? = this
        while (p is View) {
            if (p === v) return true
            p = p.parent
        }
        return false
    }

    /** Nearest level where a visible sibling exists -> pick the biggest one. */
    private fun findAutoSource(): View? {
        var child: View = this
        var p = child.parent
        while (p is ViewGroup) {
            var best: View? = null
            var bestArea = 0
            for (i in 0 until p.childCount) {
                val s = p.getChildAt(i)
                if (s === child || s.visibility != VISIBLE) continue
                val area = s.width * s.height
                if (area > bestArea) {
                    best = s
                    bestArea = area
                }
            }
            if (best != null) return best
            child = p
            p = p.parent
        }
        return null
    }

    // ---------------- Lifecycle ----------------

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        autoSource = null
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateOutline()
        buildShaders()
    }

    private fun scaleAlpha(color: Int, factor: Float): Int {
        val a = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun buildShaders() {
        if (width <= 0 || height <= 0) return
        // Light catching the top of the glass, fading out
        sheenPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                Color.argb((255 * sheenAlpha).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.argb((255 * sheenAlpha * 0.15f).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        // Edge light: bright top-left, soft bottom-right
        borderPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                scaleAlpha(borderColor, 3.0f),
                scaleAlpha(borderColor, 0.5f),
                scaleAlpha(borderColor, 1.6f)
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    // ---------------- Drawing ----------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val node = renderNode
        val snap = snapshot?.takeIf { !it.isRecycled }
        val source = if (snap == null) resolveSource() else null

        if (node == null || !canvas.isHardwareAccelerated || width <= 0 || height <= 0 ||
            (snap == null && source == null)
        ) {
            canvas.drawColor(fallbackTintColor)
            return
        }

        var recording = false
        try {
            node.setPosition(0, 0, width, height)
            val blur = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
            node.setRenderEffect(
                if (saturation != 1f) {
                    val cm = ColorMatrix().apply { setSaturation(saturation) }
                    RenderEffect.createChainEffect(
                        RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm)),
                        blur
                    )
                } else blur
            )
            val rc = node.beginRecording(width, height)
            recording = true
            if (snap != null) {
                getLocationOnScreen(myLoc)
                rc.translate((snapshotX - myLoc[0]).toFloat(), (snapshotY - myLoc[1]).toFloat())
                rc.drawBitmap(snap, 0f, 0f, null)
            } else if (source != null) {
                getLocationInWindow(myLoc)
                source.getLocationInWindow(srcLoc)
                rc.translate((srcLoc[0] - myLoc[0]).toFloat(), (srcLoc[1] - myLoc[1]).toFloat())
                source.draw(rc)
            }
            node.endRecording()
            recording = false

            canvas.drawRenderNode(node)
            canvas.drawColor(tintColor)
            if (sheenAlpha > 0f) {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sheenPaint)
            }
        } catch (e: Exception) {
            // Never crash the screen because of a visual effect
            if (recording) runCatching { node.endRecording() }
            android.util.Log.e("GlassLayout", "Blur failed, using fallback tint", e)
            canvas.drawColor(fallbackTintColor)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val half = borderPaint.strokeWidth / 2f
        borderRect.set(half, half, width - half, height - half)
        val r = min(cornerRadius, min(width, height) / 2f) - half
        canvas.drawRoundRect(borderRect, r, r, borderPaint)
    }
}
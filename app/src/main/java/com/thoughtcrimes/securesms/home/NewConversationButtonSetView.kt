package com.thoughtcrimes.securesms.home


import android.animation.FloatEvaluator
import android.animation.PointFEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.beldex.bchat.R
import com.thoughtcrimes.securesms.util.*

class NewConversationButtonSetView : RelativeLayout {
    public var expandedButton: Button? = null
    private var previousAction: Int? = null
    public var isExpanded = false
    var delegate: NewConversationButtonSetViewDelegate? = null

    // region Convenience
    //Important
    /*private val bchatButtonExpandedPosition: PointF get() { return PointF(width.toFloat() / 2 - bchatButton.expandedSize / 2 - bchatButton.shadowMargin, 0.0f) }
    private val closedGroupButtonExpandedPosition: PointF get() { return PointF(width.toFloat() - closedGroupButton.expandedSize - 2 * closedGroupButton.shadowMargin, height.toFloat() - bottomMargin - closedGroupButton.expandedSize - 2 * closedGroupButton.shadowMargin) }
    private val openGroupButtonExpandedPosition: PointF get() { return PointF(0.0f, height.toFloat() - bottomMargin - openGroupButton.expandedSize - 2 * openGroupButton.shadowMargin) }
    private val buttonRestPosition: PointF get() { return PointF(width.toFloat() / 2 - mainButton.expandedSize / 2 - mainButton.shadowMargin, height.toFloat() - bottomMargin - mainButton.expandedSize - 2 * mainButton.shadowMargin) }*/

    //New Line
    private val bchatButtonExpandedPosition: PointF get() { return PointF(0.0f, height.toFloat() - bottomMargin - bchatButton.expandedSize - 2 * bchatButton.shadowMargin) }
    private val closedGroupButtonExpandedPosition: PointF get() { return PointF(50.0f, 001.2f) }
    private val openGroupButtonExpandedPosition: PointF get() { return PointF(width.toFloat()  - openGroupButton.expandedSize  - openGroupButton.shadowMargin, -70.0f)}
    private val buttonRestPosition: PointF get() { return PointF(width.toFloat()-mainButton.expandedSize-mainButton.shadowMargin, height.toFloat()-bottomMargin - mainButton.expandedSize-2 *mainButton.shadowMargin) }
    // endregion

    // region Settings
    private val minDragDistance by lazy { toPx(40, resources).toFloat() }
    private val maxDragDistance by lazy { toPx(56, resources).toFloat() }
    private val dragMargin by lazy { toPx(16, resources).toFloat() }
    private val bottomMargin by lazy { resources.getDimension(R.dimen.new_conversation_button_bottom_offset) }
    // endregion

    // region Components
    public val mainButton by lazy { Button(context, true, R.drawable.ic_bchat_plus) }
    private val bchatButton by lazy { Button(context, false, R.drawable.ic_chat) }
    private val closedGroupButton by lazy { Button(context, false, R.drawable.ic_closed_group_chat) }
    private val openGroupButton by lazy { Button(context, false, R.drawable.ic_open_group_chat) }
    // endregion

    // region Button
    class Button : RelativeLayout {
        @DrawableRes private var iconID = 0
        private var isMain = false

        fun getIconID() = iconID

        companion object {
            val animationDuration = 250.toLong()
        }

        val expandedSize by lazy { resources.getDimension(R.dimen.new_conversation_button_expanded_size) }
        val collapsedSize by lazy { resources.getDimension(R.dimen.new_conversation_button_collapsed_size) }
        val shadowMargin by lazy { toPx(6, resources).toFloat() }
        private val expandedImageViewPosition by lazy { PointF(shadowMargin, shadowMargin) }
        private val collapsedImageViewPosition by lazy { PointF(shadowMargin + (expandedSize - collapsedSize) / 2, shadowMargin + (expandedSize - collapsedSize) / 2) }

        private val imageView by lazy {
            val result = NewConversationButtonImageView(context)
            val size = collapsedSize.toInt()
            result.layoutParams = LayoutParams(size, size)
            result.setBackgroundResource(R.drawable.new_conversation_button_background)
            @ColorRes val backgroundColorID = if (isMain) R.color.accent else R.color.new_conversation_button_collapsed_background
            @ColorRes val shadowColorID = if (isMain) {
                R.color.new_conversation_button_shadow
            } else {
                if (UiModeUtilities.isDayUiMode(context)) R.color.transparent_black_30 else R.color.black
            }
            result.mainColor = resources.getColorWithID(backgroundColorID, context.theme)
            result.bchatShadowColor = resources.getColorWithID(shadowColorID, context.theme)
            result.scaleType = ImageView.ScaleType.CENTER
            result.setImageResource(iconID)
            /*result.imageTintList = if (isMain) {
                ColorStateList.valueOf(resources.getColorWithID(android.R.color.white, context.theme))
            } else {
                ColorStateList.valueOf(resources.getColorWithID(R.color.text, context.theme))
            }*/
            if (!isMain) {
                result.imageTintList = ColorStateList.valueOf(resources.getColorWithID(R.color.text, context.theme))
            }
            result
        }

        constructor(context: Context) : super(context) { throw IllegalAccessException("Use Button(context:iconID:) instead.") }
        constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { throw IllegalAccessException("Use Button(context:iconID:) instead.") }
        constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { throw IllegalAccessException("Use Button(context:iconID:) instead.") }
        constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) { throw IllegalAccessException("Use Button(context:iconID:) instead.") }

        constructor(context: Context, isMain: Boolean, @DrawableRes iconID: Int) : super(context) {
            this.iconID = iconID
            this.isMain = isMain
            disableClipping()
            val size = resources.getDimension(R.dimen.new_conversation_button_expanded_size).toInt() + 2 * shadowMargin.toInt()
            val layoutParams = LayoutParams(size, size)
            this.layoutParams = layoutParams
            addView(imageView)
            imageView.x = collapsedImageViewPosition.x
            imageView.y = collapsedImageViewPosition.y
            gravity = Gravity.TOP or Gravity.LEFT // Intentionally not Gravity.START
        }

        fun expand() {
            GlowViewUtilities.animateColorChange(context, imageView, R.color.new_conversation_button_collapsed_background, R.color.accent)
            @ColorRes val startShadowColorID = if (UiModeUtilities.isDayUiMode(context)) R.color.transparent_black_30 else R.color.black
            GlowViewUtilities.animateShadowColorChange(context, imageView, startShadowColorID, R.color.new_conversation_button_shadow)
            imageView.animateSizeChange(R.dimen.new_conversation_button_collapsed_size, R.dimen.new_conversation_button_expanded_size, animationDuration)
            animateImageViewPositionChange(collapsedImageViewPosition, expandedImageViewPosition)
        }

        fun collapse() {
            GlowViewUtilities.animateColorChange(context, imageView, R.color.accent, R.color.new_conversation_button_collapsed_background)
            @ColorRes val endShadowColorID = if (UiModeUtilities.isDayUiMode(context)) R.color.transparent_black_30 else R.color.black
            GlowViewUtilities.animateShadowColorChange(context, imageView, R.color.new_conversation_button_shadow, endShadowColorID)
            imageView.animateSizeChange(R.dimen.new_conversation_button_expanded_size, R.dimen.new_conversation_button_collapsed_size, animationDuration)
            animateImageViewPositionChange(expandedImageViewPosition, collapsedImageViewPosition)
        }

        private fun animateImageViewPositionChange(startPosition: PointF, endPosition: PointF) {
            val animation = ValueAnimator.ofObject(PointFEvaluator(), startPosition, endPosition)
            animation.duration = animationDuration
            animation.addUpdateListener { animator ->
                val point = animator.animatedValue as PointF
                imageView.x = point.x
                imageView.y = point.y
            }
            animation.start()
        }

        fun animatePositionChange(startPosition: PointF, endPosition: PointF) {
            val animation = ValueAnimator.ofObject(PointFEvaluator(), startPosition, endPosition)
            animation.duration = animationDuration
            animation.addUpdateListener { animator ->
                val point = animator.animatedValue as PointF
                x = point.x
                y = point.y
            }
            animation.start()
        }

        fun animateAlphaChange(startAlpha: Float, endAlpha: Float) {
            val animation = ValueAnimator.ofObject(FloatEvaluator(), startAlpha, endAlpha)
            animation.duration = animationDuration
            animation.addUpdateListener { animator ->
                alpha = animator.animatedValue as Float
            }
            animation.start()
        }
    }
    // endregion

    // region Lifecycle
    constructor(context: Context) : super(context) { setUpViewHierarchy() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { setUpViewHierarchy() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { setUpViewHierarchy() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) { setUpViewHierarchy() }

    private fun setUpViewHierarchy() {
        disableClipping()
        isHapticFeedbackEnabled = true
        // Set up bchat button
        addView(bchatButton)
        bchatButton.alpha = 0.0f
        val bchatButtonLayoutParams = bchatButton.layoutParams as LayoutParams
        bchatButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        bchatButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        bchatButtonLayoutParams.bottomMargin = bottomMargin.toInt()
        // Set up Secret group button
        addView(closedGroupButton)
        closedGroupButton.alpha = 0.0f
        val closedGroupButtonLayoutParams = closedGroupButton.layoutParams as LayoutParams
        closedGroupButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        closedGroupButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        closedGroupButtonLayoutParams.bottomMargin = bottomMargin.toInt()
        // Set up social group button
        addView(openGroupButton)
        openGroupButton.alpha = 0.0f
        val openGroupButtonLayoutParams = openGroupButton.layoutParams as LayoutParams
        openGroupButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        openGroupButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        openGroupButtonLayoutParams.bottomMargin = bottomMargin.toInt()
        // Set up main button
        addView(mainButton)
        val mainButtonLayoutParams = mainButton.layoutParams as LayoutParams
        mainButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        mainButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        mainButtonLayoutParams.bottomMargin = bottomMargin.toInt()
    }
    // endregion

    // region Interaction
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touch = PointF(event.x, event.y)
        val allButtons = listOf( mainButton, bchatButton, closedGroupButton, openGroupButton )
        val buttonsExcludingMainButton = listOf( bchatButton, closedGroupButton, openGroupButton )
        if (allButtons.none { it.contains(touch) }) { return false }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isExpanded) {
                    if (mainButton.contains(touch)) { collapse() }
                } else {
                    isExpanded = true
                    expand()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                } else {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                mainButton.x = touch.x - mainButton.expandedSize
                mainButton.y = touch.y - mainButton.expandedSize
                mainButton.alpha = 1 - (PointF(mainButton.x, mainButton.y).distanceTo(buttonRestPosition) / maxDragDistance)
                val buttonToExpand = buttonsExcludingMainButton.firstOrNull { button ->
                    var hasUserDraggedBeyondButton = false
                    if (button == openGroupButton && touch.isAbove(openGroupButton, dragMargin)) { hasUserDraggedBeyondButton = true }
                    if (button == bchatButton && touch.isLeftOf(bchatButton, dragMargin)) { hasUserDraggedBeyondButton = true }
                    if (button == closedGroupButton) { hasUserDraggedBeyondButton = true }
                    button.contains(touch) || hasUserDraggedBeyondButton
                }
                if (buttonToExpand != null) {
                    if (buttonToExpand == expandedButton) { return true }
                    expandedButton?.collapse()
                    buttonToExpand.expand()
                    this.expandedButton = buttonToExpand
                } else {
                    expandedButton?.collapse()
                    this.expandedButton = null
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val mainButtonCenter = PointF(width.toFloat(), height.toFloat() - bottomMargin - mainButton.expandedSize)
                val distanceFromMainButtonCenter = touch.distanceTo(mainButtonCenter)
                fun collapse() {
                    isExpanded = false
                    this.collapse()
                }
                if (distanceFromMainButtonCenter > (minDragDistance + mainButton.collapsedSize / 2)) {
                    if (bchatButton.contains(touch) || touch.isLeftOf(bchatButton, dragMargin)) { delegate?.createNewPrivateChat(); collapse() }
                    else if (closedGroupButton.contains(touch)) { delegate?.createNewClosedGroup(); collapse() }
                    else if (openGroupButton.contains(touch) || touch.isAbove(openGroupButton, dragMargin)) { delegate?.joinOpenGroup(); collapse() }
                    else { collapse() }
                } else {
                    val currentPosition = PointF(mainButton.x, mainButton.y)
                    mainButton.animatePositionChange(currentPosition, buttonRestPosition)
                    val endAlpha = 1.0f
                    mainButton.animateAlphaChange(mainButton.alpha, endAlpha)
                    expandedButton?.collapse()
                    this.expandedButton = null
                }
            }
        }
        previousAction = event.action
        return true
    }

    public fun expand() {
        val buttonsExcludingMainButton = listOf( bchatButton, closedGroupButton, openGroupButton )
        bchatButton.animatePositionChange(buttonRestPosition, bchatButtonExpandedPosition)
        closedGroupButton.animatePositionChange(buttonRestPosition, closedGroupButtonExpandedPosition)
        openGroupButton.animatePositionChange(buttonRestPosition, openGroupButtonExpandedPosition)
        buttonsExcludingMainButton.forEach { it.animateAlphaChange(0.0f, 1.0f) }
        postDelayed({ isExpanded = true }, Button.animationDuration)
    }

    public fun collapse() {
        val allButtons = listOf( mainButton, bchatButton, closedGroupButton, openGroupButton )
        allButtons.forEach {
            val currentPosition = PointF(it.x, it.y)
            it.animatePositionChange(currentPosition, buttonRestPosition)
            val endAlpha = if (it == mainButton) 1.0f else 0.0f
            it.animateAlphaChange(it.alpha, endAlpha)
        }
        postDelayed({ isExpanded = false }, Button.animationDuration)
    }
    // endregion
}

// region Delegate
interface NewConversationButtonSetViewDelegate {

    fun joinOpenGroup()
    fun createNewPrivateChat()
    fun createNewClosedGroup()
}
// endregion

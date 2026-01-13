package io.beldex.bchat.home


import android.animation.FloatEvaluator
import android.animation.PointFEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PointF
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import io.beldex.bchat.R
import io.beldex.bchat.util.*

class NewConversationButtonSetView : RelativeLayout {
    private var expandedButton: Button? = null
    private var previousAction: Int? = null
    private var isExpanded = false
    var delegate: NewConversationButtonSetViewDelegate? = null

    // region Convenience
    //Important
    /*private val bchatButtonExpandedPosition: PointF get() { return PointF(width.toFloat() / 2 - bchatButton.expandedSize / 2 - bchatButton.shadowMargin, 0.0f) }
    private val secretGroupButtonExpandedPosition: PointF get() { return PointF(width.toFloat() - secretGroupButton.expandedSize - 2 * secretGroupButton.shadowMargin, height.toFloat() - bottomMargin - secretGroupButton.expandedSize - 2 * secretGroupButton.shadowMargin) }
    private val socialGroupButtonExpandedPosition: PointF get() { return PointF(0.0f, height.toFloat() - bottomMargin - socialGroupButton.expandedSize - 2 * socialGroupButton.shadowMargin) }
    private val buttonRestPosition: PointF get() { return PointF(width.toFloat() / 2 - mainButton.expandedSize / 2 - mainButton.shadowMargin, height.toFloat() - bottomMargin - mainButton.expandedSize - 2 * mainButton.shadowMargin) }*/

    //New Line
    private val bchatButtonExpandedPosition: PointF get() { return PointF(0.0f, height.toFloat() - bottomMargin - bchatButton.expandedSize - 2 * bchatButton.shadowMargin) }
    private val bchatButtonTitleExpandedPosition:PointF get() {
        val x = bchatButtonExpandedPosition.x + bchatButton.width / 2 - bchatButtonTitle.width / 2
        val y = bchatButtonExpandedPosition.y + bchatButton.height - bchatButtonTitle.height / 2
        return PointF(x, y) }
    private val secretGroupButtonExpandedPosition: PointF get() { return PointF(50.0f, 001.2f) }
    private val secretGroupButtonTitleExpandedPosition:PointF get() {
        val x = secretGroupButtonExpandedPosition.x + secretGroupButton.width / 2 - secretGroupButtonTitle.width / 2
        val y = secretGroupButtonExpandedPosition.y + secretGroupButton.height - secretGroupButtonTitle.height / 2
        return PointF(x, y) }
    private val socialGroupButtonExpandedPosition: PointF get() { return PointF(width.toFloat()  - socialGroupButton.expandedSize  - socialGroupButton.shadowMargin, -70.0f)}
    private val socialGroupButtonTitleExpandedPosition: PointF get()  {
        val x = socialGroupButtonExpandedPosition.x + socialGroupButton.width / 2 - socialGroupButtonTitle.width / 2
        val y = socialGroupButtonExpandedPosition.y + socialGroupButton.height - socialGroupButtonTitle.height / 2
        return PointF(x, y)}
    private val buttonRestPosition: PointF get() { return PointF(width.toFloat()-mainButton.expandedSize-mainButton.shadowMargin, height.toFloat()-bottomMargin - mainButton.expandedSize-2 *mainButton.shadowMargin) }
    private fun tooltipRestPosition(viewWidth: Int): PointF {
        return PointF(width.toFloat() / 2 - viewWidth / 2, height.toFloat() - bottomMargin)
    }
    // endregion

    // region Settings
    private val minDragDistance by lazy { toPx(40, resources).toFloat() }
    private val maxDragDistance by lazy { toPx(56, resources).toFloat() }
    private val dragMargin by lazy { toPx(16, resources).toFloat() }
    private val bottomMargin by lazy { resources.getDimension(R.dimen.new_conversation_button_bottom_offset) }
    // endregion

    // region Components
    private val mainButton by lazy { Button(context, true, R.drawable.ic_bchat_plus) }
    private val bchatButton by lazy { Button(context, false, R.drawable.ic_chat) }
    private val bchatButtonTitle by lazy {
        TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = ResourcesCompat.getFont(this.context, R.font.open_sans_bold)
            setText(R.string.home_screen_new_chat_title)
            isAllCaps = true
        }
    }
    private val secretGroupButton by lazy { Button(context, false, R.drawable.ic_secret_group_chat) }
    private val secretGroupButtonTitle by lazy {
        TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = ResourcesCompat.getFont(this.context, R.font.open_sans_bold)
            setText(R.string.home_screen_secret_groups_title)
            isAllCaps = true
        }
    }
    private val socialGroupButton by lazy { Button(context, false, R.drawable.ic_social_group_chat) }
    private val socialGroupButtonTitle by lazy {
        TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = ResourcesCompat.getFont(this.context, R.font.open_sans_bold)
            setText(R.string.home_screen_social_groups_title)
            isAllCaps = true
        }
    }
    // endregion

    // region Button
    class Button : RelativeLayout {
        @DrawableRes private var iconID = 0
        private var isMain = false

        fun getIconID() = iconID

        companion object {
            const val animationDuration = 250.toLong()
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
        addView(bchatButtonTitle)
        bchatButton.alpha = 0.0f
        bchatButtonTitle.alpha = 0.0f
        val bchatButtonLayoutParams = bchatButton.layoutParams as LayoutParams
        bchatButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        bchatButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        bchatButtonLayoutParams.bottomMargin = bottomMargin.toInt()
        // Set up secret group button
        addView(secretGroupButton)
        addView(secretGroupButtonTitle)
        secretGroupButton.alpha = 0.0f
        secretGroupButtonTitle.alpha = 0.0f
        val secretGroupButtonLayoutParams = secretGroupButton.layoutParams as LayoutParams
        secretGroupButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        secretGroupButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        secretGroupButtonLayoutParams.bottomMargin = bottomMargin.toInt()
        // Set up social group button
        addView(socialGroupButton)
        addView(socialGroupButtonTitle)
        socialGroupButton.alpha = 0.0f
        socialGroupButtonTitle.alpha = 0.0f
        val socialGroupButtonLayoutParams = socialGroupButton.layoutParams as LayoutParams
        socialGroupButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        socialGroupButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        socialGroupButtonLayoutParams.bottomMargin = bottomMargin.toInt()
        // Set up main button
        mainButton.x+=12.0F
        addView(mainButton)
        val mainButtonLayoutParams = mainButton.layoutParams as LayoutParams
        mainButtonLayoutParams.addRule(ALIGN_PARENT_END, TRUE)
        mainButtonLayoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        mainButtonLayoutParams.bottomMargin = bottomMargin.toInt()

        bchatButtonTitle.setOnClickListener {
            if(isExpanded) {
                delegate?.openNewConversationChat(); collapse()
            }
        }
        secretGroupButtonTitle.setOnClickListener {
            if(isExpanded) {
                delegate?.createNewSecretGroup(); collapse()
            }
        }
        socialGroupButtonTitle.setOnClickListener {
            if(isExpanded) {
                delegate?.joinSocialGroup(); collapse()
            }
        }
        mainButton.setOnClickListener {
            if(isExpanded){
                collapse()
            }else {
                isExpanded = true
                expand()
            }
        }
        bchatButton.setOnClickListener {
            if(isExpanded) {
                delegate?.openNewConversationChat(); collapse()
            }
        }
        secretGroupButton.setOnClickListener {
            if(isExpanded) {
                delegate?.createNewSecretGroup(); collapse()
            }
        }
        socialGroupButton.setOnClickListener {
            if(isExpanded) {
                delegate?.joinSocialGroup(); collapse()
            }
        }
    }
    // endregion

    // region Interaction
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touch = PointF(event.x, event.y)
        val allButtons = listOf( mainButton, bchatButton, secretGroupButton, socialGroupButton )
        val buttonsExcludingMainButton = listOf( bchatButton, secretGroupButton, socialGroupButton )
        if (allButtons.none { it.contains(touch) }) { return false }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isExpanded) {
                    if (mainButton.contains(touch)) { collapse() }
                }
                performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
           /* MotionEvent.ACTION_MOVE -> {
                mainButton.x = touch.x - mainButton.expandedSize
                mainButton.y = touch.y - mainButton.expandedSize
                mainButton.alpha = 1 - (PointF(mainButton.x, mainButton.y).distanceTo(buttonRestPosition) / maxDragDistance)
                val buttonToExpand = buttonsExcludingMainButton.firstOrNull { button ->
                    var hasUserDraggedBeyondButton = false
                    if (button == socialGroupButton && touch.isAbove(socialGroupButton, dragMargin)) { hasUserDraggedBeyondButton = true }
                    if (button == bchatButton && touch.isLeftOf(bchatButton, dragMargin)) { hasUserDraggedBeyondButton = true }
                    if (button == secretGroupButton) { hasUserDraggedBeyondButton = true }
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
            }*/
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val mainButtonCenter = PointF(width.toFloat(), height.toFloat() - bottomMargin - mainButton.expandedSize)
                val distanceFromMainButtonCenter = touch.distanceTo(mainButtonCenter)
                fun collapse() {
                    isExpanded = false
                    this.collapse()
                }
                if (distanceFromMainButtonCenter > (minDragDistance + mainButton.collapsedSize / 2)) {
                    if (bchatButton.contains(touch) || touch.isLeftOf(bchatButton, dragMargin)) { }
                    else if (secretGroupButton.contains(touch)) {}
                    else if (socialGroupButton.contains(touch) || touch.isAbove(socialGroupButton, dragMargin)) {}
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

    private fun expand() {
        val buttonsExcludingMainButton = listOf( bchatButton, secretGroupButton, socialGroupButton )
        val allTooltips = listOf(bchatButtonTitle, secretGroupButtonTitle, socialGroupButtonTitle)

        bchatButton.animatePositionChange(buttonRestPosition, bchatButtonExpandedPosition)
        bchatButtonTitle.animatePositionChange(tooltipRestPosition(bchatButtonTitle.width), bchatButtonTitleExpandedPosition)
        secretGroupButton.animatePositionChange(buttonRestPosition, secretGroupButtonExpandedPosition)
        secretGroupButtonTitle.animatePositionChange(tooltipRestPosition(secretGroupButtonTitle.width),secretGroupButtonTitleExpandedPosition)
        socialGroupButton.animatePositionChange(buttonRestPosition, socialGroupButtonExpandedPosition)
        socialGroupButtonTitle.animatePositionChange(tooltipRestPosition(socialGroupButtonTitle.width),socialGroupButtonTitleExpandedPosition)
        buttonsExcludingMainButton.forEach { it.animateAlphaChange(0.0f, 1.0f) }
        allTooltips.forEach { it.animateAlphaChange(0.0f, 1.0f) }
        postDelayed({ isExpanded = true }, Button.animationDuration)
    }

     private fun collapse() {
        val allButtons = listOf( mainButton, bchatButton, secretGroupButton, socialGroupButton )
         val allButtonsTitle = listOf(bchatButtonTitle,secretGroupButtonTitle,socialGroupButtonTitle)
        allButtons.forEach {
            val currentPosition = PointF(it.x, it.y)
            it.animatePositionChange(currentPosition, buttonRestPosition)
            val endAlpha = if (it == mainButton) 1.0f else 0.0f
            it.animateAlphaChange(it.alpha, endAlpha)
        }
         allButtonsTitle.forEach {
             it.animateAlphaChange(1.0f, 0.0f)
             it.animatePositionChange(PointF(it.x, it.y), tooltipRestPosition(it.width))
         }
        postDelayed({ isExpanded = false }, Button.animationDuration)
    }
    // endregion
}
fun View.animatePositionChange(startPosition: PointF, endPosition: PointF) {
    val animation = ValueAnimator.ofObject(PointFEvaluator(), startPosition, endPosition)
    animation.duration = NewConversationButtonSetView.Button.animationDuration
    animation.addUpdateListener { animator ->
        val point = animator.animatedValue as PointF
        x = point.x
        y = point.y
    }
    animation.start()
}

fun View.animateAlphaChange(startAlpha: Float, endAlpha: Float) {
    val animation = ValueAnimator.ofObject(FloatEvaluator(), startAlpha, endAlpha)
    animation.duration = NewConversationButtonSetView.Button.animationDuration
    animation.addUpdateListener { animator ->
        alpha = animator.animatedValue as Float
    }
    animation.start()
}
// region Delegate
interface NewConversationButtonSetViewDelegate {

    fun joinSocialGroup()
    fun openNewConversationChat()
    fun createNewSecretGroup()
}
// endregion

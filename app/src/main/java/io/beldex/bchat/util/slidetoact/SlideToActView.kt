package io.beldex.bchat.util.slidetoact

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.Image
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.TextViewCompat
import io.beldex.bchat.util.slidetoact.SlideToActIconUtil.createIconAnimator
import io.beldex.bchat.util.slidetoact.SlideToActIconUtil.loadIconCompat
import io.beldex.bchat.util.slidetoact.SlideToActIconUtil.startIconAnimation
import io.beldex.bchat.util.slidetoact.SlideToActIconUtil.stopIconAnimation
import io.beldex.bchat.util.slidetoact.SlideToActIconUtil.tintIconCompat
import io.beldex.bchat.R
import org.webrtc.ContextUtils.getApplicationContext

class SlideToActView @JvmOverloads constructor(
    context: Context,
    xmlAttrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.slideToActViewStyle
) : View(context, xmlAttrs, defStyleAttr) {

    companion object {
        const val TAG = "SlideToActView"
    }

    /* -------------------- LAYOUT BOUNDS -------------------- */

    private var mDesiredSliderHeightDp: Float = 72F
    private var mDesiredSliderWidthDp: Float = 280F
    private var mDesiredSliderHeight: Int = 0
    private var mDesiredSliderWidth: Int = 0

    /* -------------------- MEMBERS -------------------- */

    /** Height of the drawing area */
    private var mAreaHeight: Int = 0

    /** Width of the drawing area */
    private var mAreaWidth: Int = 0

    /** Actual Width of the drawing area, used for animations */
    private var mActualAreaWidth: Int = 0

    /** Border Radius, default to mAreaHeight/2, -1 when not initialized */
    private var mBorderRadius: Int = -1

    /** Margin of the cursor from the outer area */
    private var mActualAreaMargin: Int
    private val mOriginAreaMargin: Int

    /** Text message */
    var text: CharSequence = ""
        set(value) {
            field = value
            mTextView.text = value
            mTextPaint.set(mTextView.paint)
            invalidate()
        }

    /** Typeface for the text field */
    var typeFace = Typeface.NORMAL
        set(value) {
            field = value
            mTextView.typeface = Typeface.create("sans-serif-light", value)
            mTextPaint.set(mTextView.paint)
            invalidate()
        }

    /** Text Appearance used to fully customize the font */
    @StyleRes
    var textAppearance: Int = 0
        set(value) {
            field = value
            if (value != 0) {
                TextViewCompat.setTextAppearance(mTextView, value)
                mTextPaint.set(mTextView.paint)
                mTextPaint.color = mTextView.currentTextColor
            }
        }

    /** Outer color used by the slider (primary)*/
    @ColorInt
    var outerColor: Int = 0
        set(value) {
            field = value
            mOuterPaint.color = value
            invalidate()
        }

    /** Inner color used by the slider (secondary, icon and border) */
    @ColorInt
    var innerColor: Int = 0
        set(value) {
            field = value
            mInnerPaint.color = value
            invalidate()
        }


    private var mImage: Drawable

    /** Image color used by the Send icon (secondary, icon) */
    @ColorInt
    var imageColor: Int = 0
        set(value) {
            field = value
            DrawableCompat.setTint(mImage, value)
            invalidate()
        }

    /** Duration of the complete and reset animation (in milliseconds). */
    var animDuration: Long = 300

    /** Duration of vibration after bumping to the end point */
    var bumpVibration: Long = 0L

    @ColorInt
    var textColor: Int = 0
        set(value) {
            field = value
            mTextView.setTextColor(value)
            mTextPaint.color = textColor
            invalidate()
        }

    /** Custom Icon color */
    @ColorInt
    var iconColor: Int = 0
        set(value) {
            field = value
            //DrawableCompat.setTint(mDrawableArrow, value)
            invalidate()
        }

    /** Custom Slider Icon */
    @DrawableRes
    var sliderIcon: Int = R.drawable.slidetoact_ic_arrow
        set(value) {
            field = value
            if (field != 0) {
                ResourcesCompat.getDrawable(context.resources, value, context.theme)?.let {
                    mDrawableArrow = it
                    //DrawableCompat.setTint(it, iconColor)
                }
                invalidate()
            }
        }

    /** Slider cursor position (between 0 and (`mAreaWidth - mAreaHeight)) */
    private var mPosition: Int = 0
        set(value) {
            field = value
            if (mAreaWidth - mAreaHeight == 0) {
                // Avoid 0 division
                mPositionPerc = 0f
                mPositionPercInv = 1f
                return
            }
            mPositionPerc = value.toFloat() / (mAreaWidth - mAreaHeight).toFloat()
            mPositionPercInv = 1 - value.toFloat() / (mAreaWidth - mAreaHeight).toFloat()
            mEffectivePosition = mPosition
        }

    /** Slider cursor effective position. This is used to handle the `reversed` scenario. */
    private var mEffectivePosition: Int = 0
        set(value) {
            field = if (isReversed) (mAreaWidth - mAreaHeight) - value else value
        }

    /** Positioning of text */
    private var mTextYPosition = -1f
    private var mTextXPosition = -1f

    /** Private size for the text message */
    private var mTextSize: Int = 0
        set(value) {
            field = value
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize.toFloat())
            mTextPaint.set(mTextView.paint)
        }

    /** Slider cursor position in percentage (between 0f and 1f) */
    private var mPositionPerc: Float = 0f

    /** 1/mPositionPerc */
    private var mPositionPercInv: Float = 1f

    /* -------------------- ICONS -------------------- */

    private val mIconMargin: Int

    private val mSendIconMargin: Int

    /** Margin for Arrow Icon */
    private var mArrowMargin: Int

    /** Current angle for Arrow Icon */
    private var mArrowAngle: Float = 0f

    /** Margin for Tick Icon */
    private var mTickMargin: Int

    /** Arrow drawable */
    private lateinit var mDrawableArrow: Drawable

    /** Tick drawable, if is an AnimatedVectorDrawable it will be animated */
    private var mDrawableTick: Drawable
    private var mFlagDrawTick: Boolean = false

    @DrawableRes
    var completeIcon: Int = 0
        set(value) {
            field = value
            if (field != 0) {
                mDrawableTick = loadIconCompat(context, value)
                invalidate()
            }
        }

    /* -------------------- PAINT & DRAW -------------------- */
    /** Paint used for outer elements */
    private val mOuterPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Paint used for inner elements */
    private val mInnerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Paint used for text elements */
    private var mTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** TextView used for text elements */
    private var mTextView: TextView

    /** Inner rectangle (used for arrow rotation) */
    private var mInnerRect: RectF

    /** Outer rectangle (used for area drawing) */
    private var mOuterRect: RectF

    /** Grace value, when mPositionPerc > mGraceValue slider will perform the 'complete' operations */
    private val mGraceValue: Float = 0.8F

    /** Last X coordinate for the touch event */
    private var mLastX: Float = 0F

    /** Flag to understand if user is moving the slider cursor */
    private var mFlagMoving: Boolean = false

    /** Private flag to check if the slide gesture have been completed */
    private var mIsCompleted = false

    /** Private flag to check if the touch events should be handled or not */
    private var mIsRespondingToTouchEvents = true

    /** Public flag to lock the slider */
    var isLocked = false

    /** Public flag to reverse the slider by 180 degree */
    var isReversed = false
        set(value) {
            field = value
            // We reassign the position field to trigger the re-computation of the effective position.
            mPosition = mPosition
            invalidate()
        }

    /** Public flag to lock the rotation icon */
    var isRotateIcon = true

    /** Public flag to enable complete animation */
    var isAnimateCompletion = true

    /** Public Slide event listeners */
    var onSlideToActAnimationEventListener: OnSlideToActAnimationEventListener? = null
    var onSlideCompleteListener: OnSlideCompleteListener? = null
    var onSlideResetListener: OnSlideResetListener? = null
    var onSlideUserFailedListener: OnSlideUserFailedListener? = null

    private var bounceAnimator: ValueAnimator = ValueAnimator.ofInt(
        0, 50, 0, 20, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    )

    /** Public flag to enable bounce animation */
    private var mStartBounceAnimation: Boolean = false

    /** Public flag to set bounce animation duration */
    private var mBounceAnimationDuration: Long = 0

    /** Public flag to set bounce animation repeat time, default value infinity */
    private var mBounceAnimationRepeat: Int = 0

    init {
        val actualOuterColor: Int
        val actualInnerColor: Int
        val actualTextColor: Int
        val actualIconColor: Int
        val actualImageColor: Int

        val actualCompleteDrawable: Int

        val actualImageDrawable : Int

        mTextView = TextView(context)
        mTextPaint = mTextView.paint

        val attrs: TypedArray = context.theme.obtainStyledAttributes(
            xmlAttrs,
            R.styleable.SlideToActView,
            defStyleAttr,
            R.style.SlideToActView
        )
        try {
            mDesiredSliderHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mDesiredSliderHeightDp,
                resources.displayMetrics
            ).toInt()
            mDesiredSliderWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mDesiredSliderWidthDp,
                resources.displayMetrics
            ).toInt()

            val defaultOuter = ContextCompat.getColor(
                this.context,
                R.color.slidetoact_defaultAccent
            )
            val defaultWhite = ContextCompat.getColor(
                this.context,
                R.color.slidetoact_white
            )

            with(attrs) {
                mDesiredSliderHeight = getDimensionPixelSize(
                    R.styleable.SlideToActView_slider_height,
                    mDesiredSliderHeight
                )
                mBorderRadius = getDimensionPixelSize(R.styleable.SlideToActView_border_radius, -1)

                actualOuterColor = getColor(R.styleable.SlideToActView_outer_color, defaultOuter)
                actualInnerColor = getColor(R.styleable.SlideToActView_inner_color, defaultWhite)
                actualImageColor = getColor(R.styleable.SlideToActView_image_color,defaultWhite)

                // For text color, check if the `text_color` is set.
                // if not check if the `outer_color` is set.
                // if not, default to white.
                actualTextColor = when {
                    hasValue(R.styleable.SlideToActView_text_color) ->
                        getColor(R.styleable.SlideToActView_text_color, defaultWhite)
                    hasValue(R.styleable.SlideToActView_inner_color) -> actualInnerColor
                    else -> defaultWhite
                }

                text = getString(R.styleable.SlideToActView_text) ?: ""
                typeFace = getInt(R.styleable.SlideToActView_text_style, 1)
                mTextSize = getDimensionPixelSize(
                    R.styleable.SlideToActView_text_size,
                    resources.getDimensionPixelSize(R.dimen.slidetoact_default_text_size)
                )
                textColor = actualTextColor

                // TextAppearance is the last as will have precedence over everything text related.
                textAppearance = getResourceId(R.styleable.SlideToActView_text_appearance, 0)

                isLocked = getBoolean(R.styleable.SlideToActView_slider_locked, false)
                isReversed = getBoolean(R.styleable.SlideToActView_slider_reversed, false)
                isRotateIcon = getBoolean(R.styleable.SlideToActView_rotate_icon, true)
                isAnimateCompletion = getBoolean(
                    R.styleable.SlideToActView_animate_completion,
                    true
                )
                animDuration = getInteger(
                    R.styleable.SlideToActView_animation_duration,
                    300
                ).toLong()
                bumpVibration = getInt(
                    R.styleable.SlideToActView_bump_vibration,
                    0
                ).toLong()

                mOriginAreaMargin = getDimensionPixelSize(
                    R.styleable.SlideToActView_area_margin,
                    resources.getDimensionPixelSize(R.dimen.slidetoact_default_area_margin)
                )
                mActualAreaMargin = mOriginAreaMargin

                sliderIcon = getResourceId(
                    R.styleable.SlideToActView_slider_icon,
                    R.drawable.slide_with_pay_coin
                )

                // For icon color. check if the `slide_icon_color` is set.
                // if not check if the `outer_color` is set.
                // if not, default to defaultOuter.
                actualIconColor = when {
                    hasValue(R.styleable.SlideToActView_slider_icon_color) ->
                        getColor(R.styleable.SlideToActView_slider_icon_color, defaultOuter)
                    hasValue(R.styleable.SlideToActView_outer_color) -> actualOuterColor
                    else -> defaultOuter
                }

                actualCompleteDrawable = getResourceId(
                    R.styleable.SlideToActView_complete_icon,
                    R.drawable.slidetoact_animated_ic_check
                )

                actualImageDrawable = getResourceId(
                    R.styleable.SlideToActView_send_image_icon,
                    R.drawable.send
                )

                mIconMargin = getDimensionPixelSize(
                    R.styleable.SlideToActView_icon_margin,
                    resources.getDimensionPixelSize(R.dimen.slidetoact_default_icon_margin)
                )

                mSendIconMargin = getDimensionPixelSize(
                    R.styleable.SlideToActView_send_icon_margin,
                    resources.getDimensionPixelSize(R.dimen.slidetoact_default_send_icon_margin)
                )

                mArrowMargin = mIconMargin
                mTickMargin = mIconMargin


                mIsCompleted = getBoolean(R.styleable.SlideToActView_state_complete, false)

                mStartBounceAnimation = getBoolean(
                    R.styleable.SlideToActView_bounce_on_start,
                    false
                )
                mBounceAnimationDuration = getInteger(
                    R.styleable.SlideToActView_bounce_duration,
                    2000
                ).toLong()
                mBounceAnimationRepeat = getInteger(
                    R.styleable.SlideToActView_bounce_repeat,
                    ValueAnimator.INFINITE
                )
            }
        } finally {
            attrs.recycle()
        }

        mInnerRect = RectF(
            (mActualAreaMargin + mEffectivePosition).toFloat(),
            mActualAreaMargin.toFloat(),
            (mAreaHeight + mEffectivePosition).toFloat() - mActualAreaMargin.toFloat(),
            mAreaHeight.toFloat() - mActualAreaMargin.toFloat()
        )

        mOuterRect = RectF(
            mActualAreaWidth.toFloat(),
            0f,
            mAreaWidth.toFloat() - mActualAreaWidth.toFloat(),
            mAreaHeight.toFloat()
        )

        mDrawableTick = loadIconCompat(context, actualCompleteDrawable)
        mImage = loadIconCompat(context,actualImageDrawable)

        mTextPaint.textAlign = Paint.Align.CENTER

        outerColor = actualOuterColor
        innerColor = actualInnerColor
        iconColor = actualIconColor
        imageColor = actualImageColor

        // This outline provider force removal of shadow
        outlineProvider = SlideToActOutlineProvider()
        if (mStartBounceAnimation) {
            startBounceAnimation(mBounceAnimationDuration, mBounceAnimationRepeat)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val width: Int

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val height: Int

        width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(mDesiredSliderWidth, widthSize)
            MeasureSpec.UNSPECIFIED -> mDesiredSliderWidth
            else -> mDesiredSliderWidth
        }

        height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(mDesiredSliderHeight, heightSize)
            MeasureSpec.UNSPECIFIED -> mDesiredSliderHeight
            else -> mDesiredSliderHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mAreaWidth = w
        mAreaHeight = h
        mBorderRadius = h / 2

        // Text horizontal/vertical positioning (both centered)
        mTextXPosition = mAreaWidth.toFloat() / 2
        mTextYPosition = (mAreaHeight.toFloat() / 2) -
                (mTextPaint.descent() + mTextPaint.ascent()) / 2

        // Make sure the position is recomputed.
        mPosition = 0

        // Set state to complete if needed
        setCompletedNotAnimated(mIsCompleted)
    }

    var viewWidth = 0
    var viewHeight = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Outer area
        mOuterRect.set(
            mActualAreaWidth.toFloat(),
            0f,
            mAreaWidth.toFloat() - mActualAreaWidth.toFloat(),
            mAreaHeight.toFloat()
        )
        canvas.drawRoundRect(
            mOuterRect,
            mBorderRadius.toFloat(),
            mBorderRadius.toFloat(),
            mOuterPaint
        )

        // Text alpha
        mTextPaint.alpha = (255 * mPositionPercInv).toInt()
        // Checking if the TextView has a Transformation method applied (e.g. AllCaps).
        val textToDraw = mTextView.transformationMethod?.getTransformation(text, mTextView) ?: text
        canvas.drawText(
            textToDraw,
            0,
            textToDraw.length,
            mTextXPosition,
            mTextYPosition,
            mTextPaint
        )
        // Image drawing
        mImage.alpha = (255 * mPositionPercInv).toInt()
        mImage.setBounds(
            (mAreaWidth-mAreaHeight)+20,
            mSendIconMargin,
            mAreaWidth - mSendIconMargin - mActualAreaWidth,
            mAreaHeight - mSendIconMargin
        )

        tintIconCompat(mImage, imageColor)
        mImage.draw(canvas)

        // Inner Cursor
        // ratio is used to compute the proper border radius for the inner rect (see #8).
        val ratio = (mAreaHeight - 2 * mActualAreaMargin).toFloat() / mAreaHeight.toFloat()
        mInnerRect.set(
            (mActualAreaMargin + mEffectivePosition).toFloat(),
            mActualAreaMargin.toFloat(),
            (mAreaHeight + mEffectivePosition).toFloat() - mActualAreaMargin.toFloat(),
            mAreaHeight.toFloat() - mActualAreaMargin.toFloat()
        )
        canvas.drawRoundRect(
            mInnerRect,
            mBorderRadius.toFloat() * ratio,
            mBorderRadius.toFloat() * ratio,
            mInnerPaint
        )

        // Arrow angle
        // We compute the rotation of the arrow and we apply .rotate transformation on the canvas.
        canvas.save()
        if (isReversed) {
            canvas.scale(-1F, 1F, mInnerRect.centerX(), mInnerRect.centerY())
        }
        if (isRotateIcon) {
            mArrowAngle = -180 * mPositionPerc
            canvas.rotate(mArrowAngle, mInnerRect.centerX(), mInnerRect.centerY())
        }
        mDrawableArrow.setBounds(
            mInnerRect.left.toInt() + mArrowMargin,
            mInnerRect.top.toInt() + mArrowMargin,
            mInnerRect.right.toInt() - mArrowMargin,
            mInnerRect.bottom.toInt() - mArrowMargin
        )
        if (mDrawableArrow.bounds.left <= mDrawableArrow.bounds.right &&
            mDrawableArrow.bounds.top <= mDrawableArrow.bounds.bottom
        ) {
            mDrawableArrow.draw(canvas)
        }
        canvas.restore()

        // Tick drawing
        mDrawableTick.setBounds(
            mActualAreaWidth + mTickMargin,
            mTickMargin,
            mAreaWidth - mTickMargin - mActualAreaWidth,
            mAreaHeight - mTickMargin
        )

        tintIconCompat(mDrawableTick, innerColor)
        if (mFlagDrawTick) {
            mDrawableTick.draw(canvas)
        }
    }

    // Intentionally override `performClick` to do not lose accessibility support.
    @Suppress("RedundantOverride")
    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && event.action == MotionEvent.ACTION_DOWN) {
            // Calling performClick on every ACTION_DOWN so OnClickListener is triggered properly.
            performClick()
        }
        stopBounceAnimation()
        if (event != null && isEnabled && mIsRespondingToTouchEvents) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkInsideButton(event.x, event.y)) {
                        mFlagMoving = true
                        mLastX = event.x
                        parent.requestDisallowInterceptTouchEvent(true)
                    } else {
                        // Clicking outside the area -> User failed, notify the listener.
                        onSlideUserFailedListener?.onSlideFailed(this, true)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    if ((mPosition > 0 && isLocked) ||
                        (mPosition > 0 && mPositionPerc < mGraceValue)
                    ) {
                        // Check for grace value
                        val positionAnimator = ValueAnimator.ofInt(mPosition, 0)
                        positionAnimator.duration = animDuration
                        positionAnimator.addUpdateListener {
                            mPosition = it.animatedValue as Int
                            invalidate()
                        }
                        positionAnimator.start()
                    } else if (mPosition > 0 && mPositionPerc >= mGraceValue) {
                        startAnimationComplete()
                    } else if (mFlagMoving && mPosition == 0) {
                        // mFlagMoving == true means user successfully grabbed the slider,
                        // but mPosition == 0 means that the slider is released at the beginning
                        // so either a Tap or the user slided back.
                        onSlideUserFailedListener?.onSlideFailed(this, false)
                    }
                    mFlagMoving = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mFlagMoving) {
                        // True if the cursor was not at the end position before this event
                        val wasIncomplete = mPositionPerc < 1f

                        val diffX = event.x - mLastX
                        mLastX = event.x
                        increasePosition(diffX.toInt())
                        invalidate()

                        // If this event brought the cursor to the end position, we can vibrate
                        if (bumpVibration > 0 && wasIncomplete && mPositionPerc == 1f) {
                            handleVibration()
                        }
                    }
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    /**
     * Private method to check if user has touched the slider cursor
     * @param x The x coordinate of the touch event
     * @param y The y coordinate of the touch event
     * @return A boolean that informs if user has pressed or not
     */
    private fun checkInsideButton(x: Float, y: Float): Boolean {
        return (
                0 < y &&
                        y < mAreaHeight &&
                        mEffectivePosition < x &&
                        x < (mAreaHeight + mEffectivePosition)
                )
    }

    /**
     * Private method for increasing/decreasing the position
     * Ensure that position never exits from its range [0, (mAreaWidth - mAreaHeight)].
     *
     * Please note that the increment is inverted in case of a reversed slider.
     *
     * @param inc Increment to be performed (negative if it's a decrement)
     */
    private fun increasePosition(inc: Int) {
        mPosition = if (isReversed) {
            mPosition - inc
        } else {
            mPosition + inc
        }
        if (mPosition < 0) {
            mPosition = 0
        }
        if (mPosition > (mAreaWidth - mAreaHeight)) {
            mPosition = mAreaWidth - mAreaHeight
        }
    }

    /**
     * Private method that is performed when user completes the slide
     */
    private fun startAnimationComplete() {
        val animSet = AnimatorSet()

        // Animator that moves the cursor
        val finalPositionAnimator = ValueAnimator.ofInt(mPosition, mAreaWidth - mAreaHeight)
        finalPositionAnimator.addUpdateListener {
            mPosition = it.animatedValue as Int
            invalidate()
        }

        // Animator that bounce away the cursors
        val marginAnimator = ValueAnimator.ofInt(
            mActualAreaMargin,
            (mInnerRect.width() / 2).toInt() + mActualAreaMargin
        )
        marginAnimator.addUpdateListener {
            mActualAreaMargin = it.animatedValue as Int
            invalidate()
        }
        marginAnimator.interpolator = AnticipateOvershootInterpolator(2f)

        // Animator that reduces the outer area (to right)
        val areaAnimator = ValueAnimator.ofInt(0, (mAreaWidth - mAreaHeight) / 2)
        areaAnimator.addUpdateListener {
            mActualAreaWidth = it.animatedValue as Int
            invalidateOutline()
            invalidate()
        }

        val tickListener = ValueAnimator.AnimatorUpdateListener {
            // We need to enable the drawing of the AnimatedVectorDrawable before starting it.
            if (!mFlagDrawTick) {
                mFlagDrawTick = true
                mTickMargin = mIconMargin
            }
        }
        val tickAnimator: ValueAnimator = createIconAnimator(this, mDrawableTick, tickListener)

        val animators = mutableListOf<Animator>()
        if (mPosition < mAreaWidth - mAreaHeight) {
            animators.add(finalPositionAnimator)
        }

        if (isAnimateCompletion) {
            animators.add(marginAnimator)
            animators.add(areaAnimator)
            animators.add(tickAnimator)
        }

        animSet.playSequentially(*animators.toTypedArray())

        animSet.duration = animDuration

        animSet.addListener(
            object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    onSlideToActAnimationEventListener?.onSlideCompleteAnimationStarted(
                        this@SlideToActView,
                        mPositionPerc
                    )
                }

                override fun onAnimationCancel(p0: Animator) {
                }

                override fun onAnimationEnd(p0: Animator) {
                    mIsCompleted = true
                    onSlideToActAnimationEventListener?.onSlideCompleteAnimationEnded(
                        this@SlideToActView
                    )
                    try {
                        onSlideCompleteListener?.onSlideComplete(this@SlideToActView)
                    }catch(ex: IllegalStateException){
                        Log.d("Exception: ",ex.message.toString())
                    }
                }

                override fun onAnimationRepeat(p0: Animator) {
                }
            }
        )
        mIsRespondingToTouchEvents = false
        animSet.start()
    }

    /** Private method to update view to base state */
    private fun setBaseState() {
        mPosition = 0
        mActualAreaMargin = mOriginAreaMargin
        mActualAreaWidth = 0
        mArrowMargin = mIconMargin

        mIsCompleted = false
        mIsRespondingToTouchEvents = true
        mFlagDrawTick = false
    }

    /**
     * Method for complete slider immediately without animation
     */
    private fun setCompletedNotAnimated(state: Boolean) {
        if (state) {
            setCompleteState()
        } else {
            setBaseState()
        }
    }

    /** Private method to update view to complete state */
    private fun setCompleteState() {
        mPosition = mAreaWidth - mAreaHeight
        mActualAreaMargin = mAreaHeight / 2
        mActualAreaWidth = mPosition / 2
        mIsCompleted = true

        startIconAnimation(mDrawableTick)

        mFlagDrawTick = true
        mTickMargin = mIconMargin

        invalidateOutline()
    }

    private fun setCompletedAnimated(state: Boolean) {
        if (state) {
            if (!mIsCompleted) {
                startAnimationComplete()
            }
        } else {
            if (mIsCompleted) {
                startAnimationReset()
            }
        }
    }

    /**
     * Method to change slider state
     * @param completed - True for the completed state, False for the base state
     * @param withAnimation - True for full slide animation
     * Note: If you provide an animated source file for 'completeIcon' this animation will be run.
     */
    fun setCompleted(completed: Boolean, withAnimation: Boolean) {
        stopBounceAnimation()
        if (withAnimation) {
            setCompletedAnimated(completed)
        } else {
            setCompletedNotAnimated(completed)
        }
    }

    /**
     * @deprecated Method that completes the slider
     */
    @Deprecated(
        message = "Use setCompleted(completed: true, withAnimation: true) instead.",
        replaceWith = ReplaceWith("setCompleted(completed: true, withAnimation: true)")
    )
    fun completeSlider() {
        stopBounceAnimation()
        if (!mIsCompleted) {
            startAnimationComplete()
        }
    }

    /**
     * @deprecated Method that resets the slider
     */
    @Deprecated(
        message = "Use setCompleted(completed: false, withAnimation: true) instead.",
        replaceWith = ReplaceWith("setCompleted(completed: false, withAnimation: true)")
    )
    fun resetSlider() {
        stopBounceAnimation()
        if (mIsCompleted) {
            startAnimationReset()
        }
    }

    /**
     * Method that returns the 'mIsCompleted' flag
     * @return True if slider is in the Complete state
     */
    fun isCompleted(): Boolean {
        return this.mIsCompleted
    }

    /**
     * Bounce animation on the slider on start
     */
    private fun startBounceAnimation(duration: Long, repeatCount: Int) {
        bounceAnimator.apply {
            addUpdateListener {
                mPosition = it.animatedValue as Int
                invalidate()
            }
            setDuration(duration)
            setRepeatCount(repeatCount)
            repeatMode = ValueAnimator.RESTART
            startDelay = 1000
            start()
        }
    }

    private fun stopBounceAnimation() {
        if (bounceAnimator.isRunning) {
            bounceAnimator.end()
        }
    }

    /**
     * Private method that is performed when you want to reset the cursor
     */
    private fun startAnimationReset() {
        mIsCompleted = false
        val animSet = AnimatorSet()

        // Animator that reduces the tick size
        val tickAnimator = ValueAnimator.ofInt(mTickMargin, mAreaWidth / 2)
        tickAnimator.addUpdateListener {
            mTickMargin = it.animatedValue as Int
            invalidate()
        }

        // Animator that enlarges the outer area
        val areaAnimator = ValueAnimator.ofInt(mActualAreaWidth, 0)
        areaAnimator.addUpdateListener {
            // Now we can hide the tick till the next complete
            mFlagDrawTick = false
            mActualAreaWidth = it.animatedValue as Int
            invalidateOutline()
            invalidate()
        }

        val positionAnimator = ValueAnimator.ofInt(mPosition, 0)
        positionAnimator.addUpdateListener {
            mPosition = it.animatedValue as Int
            invalidate()
        }

        // Animator that re-draw the cursors
        val marginAnimator = ValueAnimator.ofInt(mActualAreaMargin, mOriginAreaMargin)
        marginAnimator.addUpdateListener {
            mActualAreaMargin = it.animatedValue as Int
            invalidate()
        }
        marginAnimator.interpolator = AnticipateOvershootInterpolator(2f)

        // Animator that makes the arrow appear
        val arrowAnimator = ValueAnimator.ofInt(mArrowMargin, mIconMargin)
        arrowAnimator.addUpdateListener {
            mArrowMargin = it.animatedValue as Int
            invalidate()
        }

        marginAnimator.interpolator = OvershootInterpolator(2f)

        if (isAnimateCompletion) {
            animSet.playSequentially(
                tickAnimator,
                areaAnimator,
                positionAnimator,
                marginAnimator,
                arrowAnimator
            )
        } else {
            animSet.playSequentially(positionAnimator)
        }

        animSet.duration = animDuration

        animSet.addListener(
            object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    onSlideToActAnimationEventListener?.onSlideResetAnimationStarted(
                        this@SlideToActView
                    )
                }

                override fun onAnimationCancel(p0: Animator) {
                }

                override fun onAnimationEnd(p0: Animator) {
                    mIsRespondingToTouchEvents = true
                    stopIconAnimation(mDrawableTick)
                    onSlideToActAnimationEventListener?.onSlideResetAnimationEnded(
                        this@SlideToActView
                    )
                    onSlideResetListener?.onSlideReset(this@SlideToActView)
                }

                override fun onAnimationRepeat(p0: Animator) {
                }
            }
        )
        animSet.start()
    }

    /**
     * Private method to handle vibration logic, called when the cursor it moved to the end of
     * it's path.
     */
    @SuppressLint("MissingPermission")
    private fun handleVibration() {
        if (bumpVibration <= 0) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(
                TAG,
                "bumpVibration is set but permissions are unavailable." +
                        "You must have the permission android.permission.VIBRATE in " +
                        "AndroidManifest.xml to use bumpVibration"
            )
            return
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(
            VibrationEffect.createOneShot(bumpVibration, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /**
     * Event handler for the SlideToActView animation events.
     * This event handler can be used to react to animation events from the Slide,
     * the event will be fired whenever an animation start/end.
     */
    interface OnSlideToActAnimationEventListener {

        /**
         * Called when the slide complete animation start. You can perform actions during the
         * complete animations.
         *
         * @param view The SlideToActView who created the event
         * @param threshold The mPosition (in percentage [0f,1f]) where the user has left the cursor
         */
        fun onSlideCompleteAnimationStarted(view: SlideToActView, threshold: Float)

        /**
         * Called when the slide complete animation finish. At this point the slider is stuck in the
         * center of the slider.
         *
         * @param view The SlideToActView who created the event
         */
        fun onSlideCompleteAnimationEnded(view: SlideToActView)

        /**
         * Called when the slide reset animation start. You can perform actions during the reset
         * animations.
         *
         * @param view The SlideToActView who created the event
         */
        fun onSlideResetAnimationStarted(view: SlideToActView)

        /**
         * Called when the slide reset animation finish. At this point the slider will be in the
         * ready on the left of the screen and user can interact with it.
         *
         * @param view The SlideToActView who created the event
         */
        fun onSlideResetAnimationEnded(view: SlideToActView)
    }

    /**
     * Event handler for the slide complete event.
     * Use this handler to react to slide event
     */
    interface OnSlideCompleteListener {
        /**
         * Called when user performed the slide
         * @param view The SlideToActView who created the event
         */
        fun onSlideComplete(view: SlideToActView)
    }

    /**
     * Event handler for the slide react event.
     * Use this handler to inform the user that he can slide again.
     */
    interface OnSlideResetListener {
        /**
         * Called when slides is again available
         * @param view The SlideToActView who created the event
         */
        fun onSlideReset(view: SlideToActView)
    }

    /**
     * Event handler for the user failure with the Widget.
     * You can subscribe to this event to get notified when the user is wrongly
     * interacting with the widget to eventually educate it:
     *
     * - The user clicked outside of the cursor
     * - The user slided but left when the cursor was back to zero
     *
     * You can use this listener to show a Toast or other messages.
     */
    interface OnSlideUserFailedListener {
        /**
         * Called when user failed to interact with the slider slide
         * @param view The SlideToActView who created the event
         * @param isOutside True if user pressed outside the cursor
         */
        fun onSlideFailed(view: SlideToActView, isOutside: Boolean)
    }

    /**
     * Outline provider for the SlideToActView.
     * This outline will suppress the shadow (till the moment when Android will support
     * updatable Outlines).
     */
    private inner class SlideToActOutlineProvider : ViewOutlineProvider() {

        override fun getOutline(view: View?, outline: Outline?) {
            if (view == null || outline == null) return

            outline.setRoundRect(
                mActualAreaWidth,
                0,
                mAreaWidth - mActualAreaWidth,
                mAreaHeight,
                mBorderRadius.toFloat()
            )
        }
    }
}
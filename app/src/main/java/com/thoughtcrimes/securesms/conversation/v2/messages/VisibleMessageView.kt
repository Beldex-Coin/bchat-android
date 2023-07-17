package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.contacts.Contact
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewVisibleMessageBinding
import com.beldex.libbchat.messaging.contacts.Contact.ContactContext
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.ViewUtil
import com.beldex.libsignal.utilities.ThreadUtils
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.database.*
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.home.UserDetailsBottomSheet
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.util.*
import java.util.Date
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@AndroidEntryPoint
class VisibleMessageView : LinearLayout {

    @Inject lateinit var threadDb: ThreadDatabase
    @Inject lateinit var beldexThreadDb: BeldexThreadDatabase
    @Inject lateinit var mmsSmsDb: MmsSmsDatabase
    @Inject lateinit var smsDb: SmsDatabase
    @Inject lateinit var mmsDb: MmsDatabase

    private val binding by lazy { ViewVisibleMessageBinding.bind(this) }
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val swipeToReplyIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_reply_24)!!.mutate()
    private val swipeToReplyIconRect = Rect()
    private var dx = 0.0f
    private var previousTranslationX = 0.0f
    private val gestureHandler = Handler(Looper.getMainLooper())
    private var pressCallback: Runnable? = null
    private var longPressCallback: Runnable? = null
    private var onDownTimestamp = 0L
    private var onDoubleTap: (() -> Unit)? = null
    var indexInAdapter: Int = -1
    var snIsSelected = false
        set(value) {
            field = value
            handleIsSelectedChanged()
        }
    var onPress: ((event: MotionEvent) -> Unit)? = null
    var onSwipeToReply: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var contentViewDelegate: VisibleMessageContentViewDelegate? = null

    companion object {
        const val swipeToReplyThreshold = 64.0f // dp
        const val longPressMovementThreshold = 10.0f // dp
        const val longPressDurationThreshold = 250L // ms
        const val maxDoubleTapInterval = 200L
    }

    // region Lifecycle
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()
        initialize()
    }

    private fun initialize() {
        isHapticFeedbackEnabled = true
        setWillNotDraw(false)
        binding.expirationTimerViewContainer.disableClipping()
        binding.messageContentView.disableClipping()
    }
    // endregion

    // region Updating
    fun bind(message: MessageRecord, previous: MessageRecord?, next: MessageRecord?, glide: GlideRequests, searchQuery: String?, contact: Contact?, senderBChatID: String) {
        val threadID = message.threadId
        val thread = threadDb.getRecipientForThreadId(threadID) ?: return
        val isGroupThread = thread.isGroupRecipient
        val isStartOfMessageCluster = isStartOfMessageCluster(message, previous, isGroupThread)
        val isEndOfMessageCluster = isEndOfMessageCluster(message, next, isGroupThread)
        val fontSize = TextSecurePreferences.getChatFontSize(context)
        binding.senderNameTextView.textSize = fontSize!!.toFloat()
        // Show profile picture and sender name if this is a group thread AND
        // the message is incoming
        binding.moderatorIconImageView.isVisible = false
        binding.profilePictureView.root.visibility = when {
            thread.isGroupRecipient && !message.isOutgoing && isEndOfMessageCluster -> View.VISIBLE
            thread.isGroupRecipient -> View.INVISIBLE
            else -> View.GONE
        }

        val bottomMargin = if (isEndOfMessageCluster) resources.getDimensionPixelSize(R.dimen.small_spacing)
        else ViewUtil.dpToPx(context,2)

        if (binding.profilePictureView.root.visibility == View.GONE) {
            val expirationParams = binding.expirationTimerViewContainer.layoutParams as MarginLayoutParams
            expirationParams.bottomMargin = bottomMargin
            binding.expirationTimerViewContainer.layoutParams = expirationParams
        } else {
            val avatarLayoutParams = binding.profilePictureView.root.layoutParams as MarginLayoutParams
            avatarLayoutParams.bottomMargin = bottomMargin
            binding.profilePictureView.root.layoutParams = avatarLayoutParams
        }
        if (isGroupThread && !message.isOutgoing) {
            if (isEndOfMessageCluster) {
                binding.profilePictureView.root.publicKey = senderBChatID
                binding.profilePictureView.root.glide = glide
                binding.profilePictureView.root.update(message.individualRecipient)
                binding.profilePictureView.root.setOnClickListener {
                    showUserDetails(senderBChatID, threadID)
                }
                if (thread.isOpenGroupRecipient) {
                    val openGroup = beldexThreadDb.getOpenGroupChat(threadID) ?: return
                    val isModerator = OpenGroupAPIV2.isUserModerator(
                        senderBChatID,
                        openGroup.room,
                        openGroup.server
                    )
                    binding.moderatorIconImageView.isVisible = !message.isOutgoing && isModerator
                }
            }
            binding.senderNameTextView.isVisible = isStartOfMessageCluster
            val context = if (thread.isOpenGroupRecipient) ContactContext.OPEN_GROUP else ContactContext.REGULAR
            binding.senderNameTextView.text = contact?.displayName(context) ?: senderBChatID
        } else {
            binding.senderNameTextView.visibility = View.GONE
        }
        // Date break
        binding.dateBreakTextView.textSize = fontSize.toFloat()
        binding.dateBreakTextView.showDateBreak(message, previous)
        // Timestamp
        //binding.messageTimestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(context, Locale.getDefault(), message.timestamp)
        // Message status indicator
        val (iconID, iconColor) = getMessageStatusImage(message)
        if (iconID != null) {
            val drawable = ContextCompat.getDrawable(context, iconID)?.mutate()
            if (iconColor != null) {
                drawable?.setTint(iconColor)
            }
            binding.messageStatusImageView.setImageDrawable(drawable)
        }
        if (message.isOutgoing) {
            val lastMessageID = mmsSmsDb.getLastMessageID(message.threadId)
            binding.messageStatusImageView.isVisible = !message.isSent || message.id == lastMessageID
        } else {
            binding.messageStatusImageView.isVisible = false
        }
        // Expiration timer
        updateExpirationTimer(message)
        // Populate content view
        binding.messageContentView.indexInAdapter = indexInAdapter
        binding.messageContentView.bind(message, isStartOfMessageCluster, isEndOfMessageCluster, glide, thread, searchQuery, message.isOutgoing || isGroupThread || (contact?.isTrusted ?: false))
        binding.messageContentView.delegate = contentViewDelegate
        onDoubleTap = { binding.messageContentView.onContentDoubleTap?.invoke() }
    }

    private fun isStartOfMessageCluster(current: MessageRecord, previous: MessageRecord?, isGroupThread: Boolean): Boolean {
        return if (isGroupThread) {
            previous == null || previous.isUpdate || !DateUtils.isSameHour(current.timestamp, previous.timestamp)
                || current.recipient.address != previous.recipient.address
        } else {
            previous == null || previous.isUpdate || !DateUtils.isSameHour(current.timestamp, previous.timestamp)
                || current.isOutgoing != previous.isOutgoing
        }
    }

    private fun isEndOfMessageCluster(current: MessageRecord, next: MessageRecord?, isGroupThread: Boolean): Boolean {
        return if (isGroupThread) {
            next == null || next.isUpdate || !DateUtils.isSameHour(current.timestamp, next.timestamp)
                || current.recipient.address != next.recipient.address
        } else {
            next == null || next.isUpdate || !DateUtils.isSameHour(current.timestamp, next.timestamp)
                || current.isOutgoing != next.isOutgoing
        }
    }

    private fun getMessageStatusImage(message: MessageRecord): Pair<Int?,Int?> {
        return when {
            !message.isOutgoing -> null to null
            message.isPending -> R.drawable.ic_circle_dot_dot_dot to null
            message.isRead -> R.drawable.ic_filled_circle_check to null
            message.isSent -> R.drawable.ic_circle_check to null
            message.isFailed -> R.drawable.ic_error to resources.getColor(R.color.destructive, context.theme)
            else -> R.drawable.ic_circle_check to null
        }
    }

    private fun updateExpirationTimer(message: MessageRecord) {
        val container = binding.expirationTimerViewContainer
        val content = binding.messageContentView
        val expiration = binding.expirationTimerView
        val spacing = binding.messageContentSpacing
        container.removeAllViewsInLayout()
        container.addView(if (message.isOutgoing) expiration else content)
        container.addView(if (message.isOutgoing) content else expiration)
       /* val expirationTimerViewSize = toPx(12, resources)
        val smallSpacing = resources.getDimension(R.dimen.small_spacing).roundToInt()
        expirationTimerViewLayoutParams.marginStart = if (message.isOutgoing) -(smallSpacing + expirationTimerViewSize) else 10
        expirationTimerViewLayoutParams.marginEnd = if (message.isOutgoing) 10 else -(smallSpacing + expirationTimerViewSize)
        binding.expirationTimerView.layoutParams = expirationTimerViewLayoutParams*/
        container.addView(spacing, if (message.isOutgoing) 0 else 2)
        val containerParams = container.layoutParams as ConstraintLayout.LayoutParams
        containerParams.horizontalBias = if (message.isOutgoing) 1f else 0f
        container.layoutParams = containerParams
        if (message.expiresIn > 0 && !message.isPending) {
            binding.expirationTimerView.setColorFilter(ResourcesCompat.getColor(resources, R.color.text, context.theme))
            binding.expirationTimerView.isVisible = true
            binding.expirationTimerView.setPercentComplete(0.0f)
            if (message.expireStarted > 0) {
                binding.expirationTimerView.setExpirationTime(message.expireStarted, message.expiresIn)
                binding.expirationTimerView.startAnimation()
                if (message.expireStarted + message.expiresIn <= System.currentTimeMillis()) {
                    ApplicationContext.getInstance(context).expiringMessageManager.checkSchedule()
                }
            } else if (!message.isMediaPending) {
                binding.expirationTimerView.setPercentComplete(0.0f)
                binding.expirationTimerView.stopAnimation()
                ThreadUtils.queue {
                    val expirationManager = ApplicationContext.getInstance(context).expiringMessageManager
                    val id = message.getId()
                    val mms = message.isMms
                    if (mms) mmsDb.markExpireStarted(id) else smsDb.markExpireStarted(id)
                    expirationManager.scheduleDeletion(id, mms, message.expiresIn)
                }
            } else {
                binding.expirationTimerView.stopAnimation()
                binding.expirationTimerView.setPercentComplete(0.0f)
            }
        } else {
            binding.expirationTimerView.isVisible = false
        }
        container.requestLayout()
    }

    private fun handleIsSelectedChanged() {
        background = if (snIsSelected) {
            ColorDrawable(context.resources.getColorWithID(R.color.message_selected, context.theme))
        } else {
            null
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (translationX < 0 && !binding.expirationTimerView.isVisible) {
            val spacing = context.resources.getDimensionPixelSize(R.dimen.small_spacing)
            val threshold = swipeToReplyThreshold
            val iconSize = toPx(24, context.resources)
            val bottomVOffset = paddingBottom + binding.messageStatusImageView.height + (binding.messageContentView.height - iconSize) / 2
            swipeToReplyIconRect.left = binding.messageContentView.right - binding.messageContentView.paddingEnd + spacing
            swipeToReplyIconRect.top = height - bottomVOffset - iconSize
            swipeToReplyIconRect.right = binding.messageContentView.right - binding.messageContentView.paddingEnd + iconSize + spacing
            swipeToReplyIconRect.bottom = height - bottomVOffset
            swipeToReplyIcon.bounds = swipeToReplyIconRect
            swipeToReplyIcon.alpha = (255.0f * (min(abs(translationX), threshold) / threshold)).roundToInt()
        } else {
            swipeToReplyIcon.alpha = 0
        }
        swipeToReplyIcon.draw(canvas)
        super.onDraw(canvas)
    }

    fun recycle() {
        binding.profilePictureView.root.recycle()
        binding.messageContentView.recycle()
    }
    // endregion

    // region Interaction
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onPress == null || onSwipeToReply == null || onLongPress == null) { return false }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onDown(event)
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_CANCEL -> onCancel(event)
            MotionEvent.ACTION_UP -> onUp(event)
        }
        return true
    }

    private fun onDown(event: MotionEvent) {
        dx = x - event.rawX
        longPressCallback?.let { gestureHandler.removeCallbacks(it) }
        val newLongPressCallback = Runnable { onLongPress() }
        this.longPressCallback = newLongPressCallback
        gestureHandler.postDelayed(newLongPressCallback, longPressDurationThreshold)
        onDownTimestamp = Date().time
    }

    private fun onMove(event: MotionEvent) {
        val translationX = toDp(event.rawX + dx, context.resources)
        if (abs(translationX) < longPressMovementThreshold || snIsSelected) {
            return
        } else {
            longPressCallback?.let { gestureHandler.removeCallbacks(it) }
        }
        if (translationX > 0) { return } // Only allow swipes to the left
        // The idea here is to asymptotically approach a maximum drag distance
        val damping = 50.0f
        val sign = -1.0f
        val x = (damping * (sqrt(abs(translationX)) / sqrt(damping))) * sign
        this.translationX = x
        binding.dateBreakTextView.translationX = -x // Bit of a hack to keep the date break text view from moving
        postInvalidate() // Ensure onDraw(canvas:) is called
        if (abs(x) > swipeToReplyThreshold && abs(previousTranslationX) < swipeToReplyThreshold) {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        previousTranslationX = x
    }

    private fun onCancel(event: MotionEvent) {
        if (abs(translationX) > swipeToReplyThreshold) {
            onSwipeToReply?.invoke()
        }
        longPressCallback?.let { gestureHandler.removeCallbacks(it) }
        resetPosition()
    }

    private fun onUp(event: MotionEvent) {
        if (abs(translationX) > swipeToReplyThreshold) {
            onSwipeToReply?.invoke()
        } else if ((Date().time - onDownTimestamp) < longPressDurationThreshold) {
            longPressCallback?.let { gestureHandler.removeCallbacks(it) }
            val pressCallback = this.pressCallback
            if (pressCallback != null) {
                // If we're here and pressCallback isn't null, it means that we tapped again within
                // maxDoubleTapInterval ms and we should count this as a double tap
                gestureHandler.removeCallbacks(pressCallback)
                this.pressCallback = null
                onDoubleTap?.invoke()
            } else {
                val newPressCallback = Runnable { onPress(event) }
                this.pressCallback = newPressCallback
                gestureHandler.postDelayed(newPressCallback, VisibleMessageView.maxDoubleTapInterval)
            }
        }
        resetPosition()
    }

    private fun resetPosition() {
        animate()
            .translationX(0.0f)
            .setDuration(150)
            .setUpdateListener {
                postInvalidate() // Ensure onDraw(canvas:) is called
            }
            .start()
        // Bit of a hack to keep the date break text view from moving
        binding.dateBreakTextView.animate()
            .translationX(0.0f)
            .setDuration(150)
            .start()
    }

    private fun onLongPress() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    fun onContentClick(event: MotionEvent) {
        binding.messageContentView.onContentClick.iterator().forEach { clickHandler -> clickHandler.invoke(event) }
    }

    private fun onPress(event: MotionEvent) {
        onPress?.invoke(event)
        pressCallback = null
    }

    private fun showUserDetails(publicKey: String, threadID: Long) {
        val userDetailsBottomSheet = UserDetailsBottomSheet()
        val bundle = bundleOf(
                UserDetailsBottomSheet.ARGUMENT_PUBLIC_KEY to publicKey,
                UserDetailsBottomSheet.ARGUMENT_THREAD_ID to threadID
        )
        userDetailsBottomSheet.arguments = bundle
        val activity = context as AppCompatActivity
        userDetailsBottomSheet.show(activity.supportFragmentManager, userDetailsBottomSheet.tag)
    }

    fun playVoiceMessage() {
        binding.messageContentView.playVoiceMessage()
    }
    // endregion
}

package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.contacts.Contact.ContactContext
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.ViewUtil
import com.beldex.libsignal.utilities.ThreadUtils
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.R
import io.beldex.bchat.database.BeldexThreadDatabase
import io.beldex.bchat.database.MmsDatabase
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.database.SmsDatabase
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.databinding.ViewVisibleMessageBinding
import io.beldex.bchat.home.UserDetailsBottomSheet
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.disableClipping
import io.beldex.bchat.util.getColorWithID
import io.beldex.bchat.util.isSameDayMessage
import io.beldex.bchat.util.toDp
import io.beldex.bchat.util.toPx
import io.beldex.bchat.database.BeldexAPIDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
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
    @Inject lateinit var beldexApiDb: BeldexAPIDatabase

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
    val messageContentView: VisibleMessageContentView by lazy { binding.messageContentView.root }

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
        binding.messageInnerContainer.disableClipping()
        binding.messageContentView.root.disableClipping()
    }
    // endregion

    // region Updating
    fun bind(
        message : MessageRecord,
        previous : MessageRecord?,
        next : MessageRecord?,
        glide : RequestManager,
        searchQuery : String?,
        contact : Contact?,
        senderBChatID : String,
        onAttachmentNeedsDownload : (Long, Long) -> Unit,
        messageSelected: () -> Boolean,
        delegate : VisibleMessageViewDelegate?,
        position : Int
    ) {
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
            thread.isGroupRecipient && !message.isOutgoing -> View.INVISIBLE
            else -> View.GONE
        }

        val bottomMargin = if (isEndOfMessageCluster) resources.getDimensionPixelSize(R.dimen.small_spacing)
        else ViewUtil.dpToPx(context,2)

        if (binding.profilePictureView.root.visibility == View.GONE) {
            val expirationParams = binding.messageInnerContainer.layoutParams as MarginLayoutParams
            expirationParams.bottomMargin = bottomMargin
            binding.messageInnerContainer.layoutParams = expirationParams
        } else {
            val avatarLayoutParams = binding.profilePictureView.root.layoutParams as MarginLayoutParams
            avatarLayoutParams.bottomMargin = bottomMargin
            binding.profilePictureView.root.layoutParams = avatarLayoutParams
        }
        if (isGroupThread && !message.isOutgoing) {
            if (isEndOfMessageCluster) {
                binding.profilePictureView.root.publicKey = senderBChatID
                binding.profilePictureView.root.glide = glide
                binding.profilePictureView.root.update(message.individualRecipient,groupImage = true)
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
        }
        binding.senderNameTextView.isVisible = !message.isOutgoing && (isStartOfMessageCluster && (isGroupThread || snIsSelected))
        val contactContext =
            if (thread.isOpenGroupRecipient) ContactContext.OPEN_GROUP else ContactContext.REGULAR
        binding.senderNameTextView.text = contact?.displayName(contactContext) ?: senderBChatID
        // Date break
        val showDateBreak =  (isStartOfMessageCluster || snIsSelected) && !isSameDayMessage(message, previous)
        if (showDateBreak) {
            binding.dateBreakTextView.text = DateUtils.getCoversationDisplayFormattedTimeSpanString(context, Locale.getDefault(), message.timestamp)
            binding.dateBreakTextView.isVisible = true
            binding.dateBreakTextView.textSize = fontSize.toFloat()
        } else {
            binding.dateBreakTextView.isVisible = false
        }
       /* val (iconID, iconColor) = getMessageStatusImage(message)
        if (iconID != null) {
            val drawable = ContextCompat.getDrawable(context, iconID)?.mutate()
            if (iconColor != null) {
                drawable?.setTint(iconColor)
            }
            binding.messageStatusImageView.setImageDrawable(drawable)
        }
        if (message.isOutgoing) {
            val lastMessageID = mmsSmsDb.getLastMessageID(message.threadId)
            binding.messageStatusImageView.isVisible = (iconID != null && (!message.isSent || message.id == lastMessageID))
        } else {
            binding.messageStatusImageView.isVisible = false
        }*/
        // Expiration timer
        updateExpirationTimer(message)

        val emojiLayoutParams=
            binding.emojiReactionsView.root.layoutParams as ConstraintLayout.LayoutParams
        emojiLayoutParams.horizontalBias=if (message.isOutgoing) 1f else 0f
        binding.emojiReactionsView.root.layoutParams=emojiLayoutParams

        val containerParams = binding.messageInnerContainer.layoutParams as ConstraintLayout.LayoutParams


        if (message.reactions.isNotEmpty()) {
            binding.emojiReactionsView.root.setReactions(message.id, message.reactions, message.isOutgoing, delegate)
            binding.emojiReactionsView.root.isVisible = true
            if(isEndOfMessageCluster) {
                containerParams.bottomMargin = if(isGroupThread) resources.getDimensionPixelSize(R.dimen.react_with_any_emoji_parent_container_bottom_margin_with_tail_group_message) else resources.getDimensionPixelSize(R.dimen.react_with_any_emoji_parent_container_bottom_margin_with_tail)
            }else {
                containerParams.bottomMargin = if(isGroupThread) resources.getDimensionPixelSize(R.dimen.react_with_any_emoji_parent_container_bottom_margin_group_message) else resources.getDimensionPixelSize(R.dimen.react_with_any_emoji_parent_container_bottom_margin)
            }
        } else {
            binding.emojiReactionsView.root.visibility = View.GONE
            containerParams.bottomMargin = 0
        }

        // Populate content view
        binding.messageContentView.root.indexInAdapter = indexInAdapter
        //added for the long press handling on shared contact
        binding.messageContentView.root.onLongPress = {
            onDown(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN,
                    0F,
                    0F,
                    0
                )
            )
        }
        binding.messageContentView.root.bind(message, isStartOfMessageCluster, isEndOfMessageCluster, glide, thread, searchQuery, message.isOutgoing || isGroupThread || (contact?.isTrusted ?: false),
            onAttachmentNeedsDownload, thread.isOpenGroupRecipient,delegate!!, this, position,messageSelected)
        binding.messageContentView.root.delegate = delegate
        binding.messageContentView.root.chatWithContact = { ct ->
            delegate.chatWithContact(ct)
        }
        onDoubleTap = { binding.messageContentView.root.onContentDoubleTap?.invoke() }
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
            message.isRead -> R.drawable.ic_message_seen to null
            message.isSent -> R.drawable.ic_message_sent to null
            message.isFailed -> R.drawable.ic_message_failed to null
            else -> R.drawable.ic_message_sent to null
        }
    }

    private fun updateExpirationTimer(message: MessageRecord) {
        val container = binding.messageInnerContainer
        val content = binding.messageContentView.root
        val expiration = binding.expirationTimerView
        val spacing = binding.messageContentSpacing
        val statusView = binding.messageStatusImageView
        container.removeAllViewsInLayout()
        container.addView(if (message.isOutgoing) expiration else content)
        container.addView(statusView)
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
            binding.messageStatusImageView.isVisible = (iconID != null && (!message.isSent || message.id == lastMessageID))
        } else {
            binding.messageStatusImageView.isVisible = false
        }

        if (message.expiresIn > 0 && !message.isPending) {
            binding.expirationTimerView.setColorFilter(ResourcesCompat.getColor(resources, R.color.text, context.theme))
            binding.expirationTimerView.isInvisible = false
            binding.expirationTimerView.setPercentComplete(0.0f)
            if (message.expireStarted > 0) {
                binding.expirationTimerView.setExpirationTime(message.expireStarted, message.expiresIn)
                binding.expirationTimerView.startAnimation()
                if (message.expireStarted + message.expiresIn <= MnodeAPI.nowWithOffset) {
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
            binding.expirationTimerView.isInvisible = true
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
        val spacing = context.resources.getDimensionPixelSize(R.dimen.small_spacing)
        val iconSize = toPx(24, context.resources)
        val top = height - (binding.messageInnerContainer.height / 2) - binding.profilePictureView.root.marginBottom - (iconSize / 2)
        val bottom = top + iconSize
        swipeToReplyIconRect.left = -(spacing+spacing)
        swipeToReplyIconRect.top = top
        swipeToReplyIconRect.right = 16
        swipeToReplyIconRect.bottom = bottom
        if (translationX > 0 && !binding.expirationTimerView.isVisible) {
            val threshold = swipeToReplyThreshold
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
        binding.messageContentView.root.recycle()
    }
    // endregion

    // region Interaction
    override fun onTouchEvent(event : MotionEvent) : Boolean {
        if (onPress == null && onSwipeToReply == null && onLongPress == null) {
            return false
        }
        if (!TextSecurePreferences.getIsReactionOverlayVisible(context)) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> onDown(event)
                MotionEvent.ACTION_MOVE -> {
                    // only bother with movements if we have swipe to reply
                    onSwipeToReply?.let { onMove(event) }
                }

                MotionEvent.ACTION_CANCEL -> onCancel(event)
                MotionEvent.ACTION_UP -> onUp(event)
            }
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
        if (translationX < 0) { return } // Only allow swipes to the left
        // The idea here is to asymptotically approach a maximum drag distance
        val damping = 50.0f
        val sign = 1.0f
        val x = (damping * (sqrt(abs(translationX)) / sqrt(damping))) * sign
        this.translationX = x
        binding.dateBreakTextView.translationX = -x // Bit of a hack to keep the date break text view from moving
        postInvalidate() // Ensure onDraw(canvas:) is called
        if (abs(x) < swipeToReplyThreshold && abs(previousTranslationX) > swipeToReplyThreshold) {
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
                gestureHandler.postDelayed(newPressCallback, maxDoubleTapInterval)
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
        binding.messageContentView.root.onContentClick.iterator().forEach { clickHandler -> clickHandler.invoke(event) }
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
        ActivityDispatcher.get(context)?.showBottomSheetDialogWithBundle(UserDetailsBottomSheet(),userDetailsBottomSheet.tag,bundle)
    }

    fun playVoiceMessage() {
        binding.messageContentView.root.playVoiceMessage()
    }
    fun stoppedVoiceMessage() {
        binding.messageContentView.root.stopVoiceMessage()
    }
    // endregion
}

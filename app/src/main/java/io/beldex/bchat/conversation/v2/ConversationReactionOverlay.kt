package io.beldex.bchat.conversation.v2

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.squareup.phrase.Phrase
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.AnimatorInflaterCompat
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.LocalisedTimeUtil.toShortTwoPartString
import io.beldex.bchat.R
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getLocalNumber
import com.beldex.libbchat.utilities.ThemeUtil
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.components.emoji.EmojiImageView
import io.beldex.bchat.components.emoji.RecentEmojiPageModel
import io.beldex.bchat.components.menu.ActionItem
import io.beldex.bchat.conversation.v2.menus.ConversationMenuItemHelper
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.database.model.MediaMmsMessageRecord
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.database.model.ReactionRecord
import io.beldex.bchat.dependencies.DatabaseComponent.Companion.get
import io.beldex.bchat.repository.ConversationRepository
import io.beldex.bchat.util.AnimationCompleteListener
import io.beldex.bchat.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class ConversationReactionOverlay : FrameLayout {
    private val emojiViewGlobalRect = Rect()
    private val emojiStripViewBounds = Rect()
    private var segmentSize = 0f
    private val horizontalEmojiBoundary = Boundary()
    private val verticalScrubBoundary = Boundary()
    private val deadzoneTouchPoint = PointF()
    private lateinit var activity: Activity
    lateinit var messageRecord: MessageRecord
    private lateinit var selectedConversationModel: SelectedConversationModel
    private var overlayState = OverlayState.HIDDEN
    private lateinit var recentEmojiPageModel: RecentEmojiPageModel
    private var downIsOurs = false
    private var selected = -1
    private var customEmojiIndex = 0
    private var originalStatusBarColor = 0
    private var originalNavigationBarColor = 0
    private lateinit var dropdownAnchor: View
    private lateinit var conversationItem: LinearLayout
    private lateinit var conversationBubble: View
    private lateinit var backgroundView: View
    private lateinit var foregroundView: ConstraintLayout
    private lateinit var emojiViews: List<EmojiImageView>
    private var contextMenu: ConversationContextMenu? = null
    private var touchDownDeadZoneSize = 0f
    private var distanceFromTouchDownPointToBottomOfScrubberDeadZone = 0f
    private var scrubberWidth = 0
    private var selectedVerticalTranslation = 0
    private var scrubberHorizontalMargin = 0
    private var animationEmojiStartDelayFactor = 0
    private var statusBarHeight = 0
    private var onReactionSelectedListener: OnReactionSelectedListener? = null
    private var onActionSelectedListener: OnActionSelectedListener? = null
    private var onHideListener: OnHideListener? = null
    private val revealAnimatorSet = AnimatorSet()
    private var hideAnimatorSet = AnimatorSet()

    @Inject
    lateinit var mmsSmsDatabase: MmsSmsDatabase
    @Inject lateinit var repository: ConversationRepository
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    override fun onFinishInflate() {
        super.onFinishInflate()
        dropdownAnchor = findViewById(R.id.dropdown_anchor)
        conversationItem = findViewById(R.id.conversation_item)
        conversationBubble = conversationItem.findViewById(R.id.conversation_item_bubble)
        backgroundView = findViewById(R.id.conversation_reaction_scrubber_background)
        foregroundView = findViewById(R.id.conversation_reaction_scrubber_foreground)
        emojiViews = listOf(R.id.reaction_1, R.id.reaction_2, R.id.reaction_3, R.id.reaction_4, R.id.reaction_5, R.id.reaction_6, R.id.reaction_7).map { findViewById(it) }
        customEmojiIndex = emojiViews.size - 1
        distanceFromTouchDownPointToBottomOfScrubberDeadZone = resources.getDimensionPixelSize(R.dimen.conversation_reaction_scrub_deadzone_distance_from_touch_bottom).toFloat()
        touchDownDeadZoneSize = resources.getDimensionPixelSize(R.dimen.conversation_reaction_touch_deadzone_size).toFloat()
        scrubberWidth = resources.getDimensionPixelOffset(R.dimen.reaction_scrubber_width)
        selectedVerticalTranslation = resources.getDimensionPixelOffset(R.dimen.conversation_reaction_scrub_vertical_translation)
        scrubberHorizontalMargin = resources.getDimensionPixelOffset(R.dimen.conversation_reaction_scrub_horizontal_margin)
        animationEmojiStartDelayFactor = resources.getInteger(R.integer.reaction_scrubber_emoji_reveal_duration_start_delay_factor)
        initAnimators()
    }
    fun show(activity: Activity,
             messageRecord: MessageRecord,
             lastSeenDownPoint: PointF,
             selectedConversationModel: SelectedConversationModel) {
        job?.cancel()
        if (overlayState != OverlayState.HIDDEN) {
            return
        }
        this.messageRecord = messageRecord
        this.selectedConversationModel = selectedConversationModel
        overlayState = OverlayState.UNINITAILIZED
        selected = -1
        recentEmojiPageModel = RecentEmojiPageModel(activity)
        setupSelectedEmoji()
        val statusBarBackground = activity.findViewById<View>(android.R.id.statusBarBackground)
        statusBarHeight = statusBarBackground?.height ?: 0
        val conversationItemSnapshot = selectedConversationModel.bitmap
        conversationBubble.layoutParams = LinearLayout.LayoutParams(conversationItemSnapshot.width, conversationItemSnapshot.height)
        conversationBubble.background = BitmapDrawable(resources, conversationItemSnapshot)
        val isMessageOnLeft = selectedConversationModel.isOutgoing xor ViewUtil.isLtr(this)
        conversationItem.scaleX = LONG_PRESS_SCALE_FACTOR
        conversationItem.scaleY = LONG_PRESS_SCALE_FACTOR
        visibility = INVISIBLE
        this.activity = activity
        updateSystemUiOnShow(activity)
        doOnLayout { showAfterLayout(messageRecord, lastSeenDownPoint, isMessageOnLeft) }

        job = scope.launch(Dispatchers.IO) {
            repository.changes(messageRecord.threadId)
                .filter { mmsSmsDatabase.getMessageForTimestamp(messageRecord.timestamp) == null }
                .collect { withContext(Dispatchers.Main) { hide() } }
        }
    }
    private fun showAfterLayout(messageRecord: MessageRecord,
                                lastSeenDownPoint: PointF,
                                isMessageOnLeft: Boolean) {
        val contextMenu = ConversationContextMenu(dropdownAnchor, getMenuActionItems(messageRecord))
        this.contextMenu = contextMenu
        var endX = if (isMessageOnLeft) scrubberHorizontalMargin.toFloat() else selectedConversationModel.bubbleX - conversationItem.width + selectedConversationModel.bubbleWidth
        var endY = selectedConversationModel.bubbleY - statusBarHeight
        conversationItem.x = endX
        conversationItem.y = endY
        val conversationItemSnapshot = selectedConversationModel.bitmap
        val isWideLayout = contextMenu.getMaxWidth() + scrubberWidth < width
        val overlayHeight = height
        val bubbleWidth = selectedConversationModel.bubbleWidth
        var endApparentTop = endY
        var endScale = 1f
        val menuPadding = DimensionUnit.DP.toPixels(12f)
        val reactionBarTopPadding = DimensionUnit.DP.toPixels(32f)
        val reactionBarHeight = backgroundView.height
        var reactionBarBackgroundY: Float
        if (isWideLayout) {
            val everythingFitsVertically = reactionBarHeight + menuPadding + reactionBarTopPadding + conversationItemSnapshot.height < overlayHeight
            if (everythingFitsVertically) {
                val reactionBarFitsAboveItem = conversationItem.y > reactionBarHeight + menuPadding + reactionBarTopPadding
                if (reactionBarFitsAboveItem) {
                    reactionBarBackgroundY = conversationItem.y - menuPadding - reactionBarHeight
                } else {
                    endY = reactionBarHeight + menuPadding + reactionBarTopPadding
                    reactionBarBackgroundY = reactionBarTopPadding
                }
            } else {
                val spaceAvailableForItem = overlayHeight - reactionBarHeight - menuPadding - reactionBarTopPadding
                endScale = spaceAvailableForItem / conversationItem.height
                endX += Util.halfOffsetFromScale(conversationItemSnapshot.width, endScale) * if (isMessageOnLeft) -1 else 1
                endY = reactionBarHeight + menuPadding + reactionBarTopPadding - Util.halfOffsetFromScale(conversationItemSnapshot.height, endScale)
                reactionBarBackgroundY = reactionBarTopPadding
            }
        } else {
            val reactionBarOffset = DimensionUnit.DP.toPixels(48f)
            val spaceForReactionBar = Math.max(reactionBarHeight + reactionBarOffset, 0f)
            val everythingFitsVertically = contextMenu.getMaxHeight() + conversationItemSnapshot.height + menuPadding + spaceForReactionBar < overlayHeight
            if (everythingFitsVertically) {
                val bubbleBottom = selectedConversationModel.bubbleY + conversationItemSnapshot.height
                val menuFitsBelowItem = bubbleBottom + menuPadding + contextMenu.getMaxHeight() <= overlayHeight + statusBarHeight
                if (menuFitsBelowItem) {
                    if (conversationItem.y < 0) {
                        endY = 0f
                    }
                    val contextMenuTop = endY + conversationItemSnapshot.height
                    reactionBarBackgroundY = getReactionBarOffsetForTouch(selectedConversationModel.bubbleY, contextMenuTop, menuPadding, reactionBarOffset, reactionBarHeight, reactionBarTopPadding, endY)
                    if (reactionBarBackgroundY <= reactionBarTopPadding) {
                        endY = backgroundView.height + menuPadding + reactionBarTopPadding
                    }
                } else {
                    endY = overlayHeight - contextMenu.getMaxHeight() - menuPadding - conversationItemSnapshot.height
                    reactionBarBackgroundY = endY - reactionBarHeight - menuPadding
                }
                endApparentTop = endY
            } else if (reactionBarOffset + reactionBarHeight + contextMenu.getMaxHeight() + menuPadding < overlayHeight) {
                val spaceAvailableForItem = overlayHeight.toFloat() - contextMenu.getMaxHeight() - menuPadding - spaceForReactionBar
                endScale = spaceAvailableForItem / conversationItemSnapshot.height
                endX += Util.halfOffsetFromScale(conversationItemSnapshot.width, endScale) * if (isMessageOnLeft) -1 else 1
                endY = spaceForReactionBar - Util.halfOffsetFromScale(conversationItemSnapshot.height, endScale)
                val contextMenuTop = endY + conversationItemSnapshot.height * endScale
                reactionBarBackgroundY = reactionBarTopPadding //getReactionBarOffsetForTouch(selectedConversationModel.getBubbleY(), contextMenuTop + Util.halfOffsetFromScale(conversationItemSnapshot.getHeight(), endScale), menuPadding, reactionBarOffset, reactionBarHeight, reactionBarTopPadding, endY);
                endApparentTop = endY + Util.halfOffsetFromScale(conversationItemSnapshot.height, endScale)
            } else {
                contextMenu.height = contextMenu.getMaxHeight() / 2
                val menuHeight = contextMenu.height
                val fitsVertically = menuHeight + conversationItem.height + menuPadding * 2 + reactionBarHeight + reactionBarTopPadding < overlayHeight
                if (fitsVertically) {
                    val bubbleBottom = selectedConversationModel.bubbleY + conversationItemSnapshot.height
                    val menuFitsBelowItem = bubbleBottom + menuPadding + menuHeight <= overlayHeight + statusBarHeight
                    if (menuFitsBelowItem) {
                        reactionBarBackgroundY = conversationItem.y - menuPadding - reactionBarHeight
                        if (reactionBarBackgroundY < reactionBarTopPadding) {
                            endY = reactionBarTopPadding + reactionBarHeight + menuPadding
                            reactionBarBackgroundY = reactionBarTopPadding
                        }
                    } else {
                        endY = overlayHeight - menuHeight - menuPadding - conversationItemSnapshot.height
                        reactionBarBackgroundY = endY - reactionBarHeight - menuPadding
                    }
                    endApparentTop = endY
                } else {
                    val spaceAvailableForItem = overlayHeight.toFloat() - menuHeight - menuPadding * 2 - reactionBarHeight - reactionBarTopPadding
                    endScale = spaceAvailableForItem / conversationItemSnapshot.height
                    endX += Util.halfOffsetFromScale(conversationItemSnapshot.width, endScale) * if (isMessageOnLeft) -1 else 1
                    endY = reactionBarHeight - Util.halfOffsetFromScale(conversationItemSnapshot.height, endScale) + menuPadding + reactionBarTopPadding
                    reactionBarBackgroundY = reactionBarTopPadding
                    endApparentTop = reactionBarHeight + menuPadding + reactionBarTopPadding
                }
            }
        }
        reactionBarBackgroundY = Math.max(reactionBarBackgroundY, -statusBarHeight.toFloat())
        hideAnimatorSet.end()
        visibility = VISIBLE
        val scrubberX = if (isMessageOnLeft) {
            scrubberHorizontalMargin.toFloat()
        } else {
            (width - scrubberWidth - scrubberHorizontalMargin).toFloat()
        }
        foregroundView.x = scrubberX
        foregroundView.y = reactionBarBackgroundY + reactionBarHeight / 2f - foregroundView.height / 2f
        backgroundView.x = scrubberX
        backgroundView.y = reactionBarBackgroundY
        verticalScrubBoundary.update(reactionBarBackgroundY,
            lastSeenDownPoint.y + distanceFromTouchDownPointToBottomOfScrubberDeadZone)
        updateBoundsOnLayoutChanged()
        revealAnimatorSet.start()
        if (isWideLayout) {
            val scrubberRight = scrubberX + scrubberWidth
            val offsetX = if (isMessageOnLeft) scrubberRight + menuPadding else scrubberX - contextMenu.getMaxWidth() - menuPadding
            contextMenu.show(offsetX.toInt(), Math.min(backgroundView.y, (overlayHeight - contextMenu.getMaxHeight()).toFloat()).toInt())
        } else {
            val contentX = if (isMessageOnLeft) scrubberHorizontalMargin.toFloat() else selectedConversationModel.bubbleX
            val offsetX = if (isMessageOnLeft) contentX else -contextMenu.getMaxWidth() + contentX + bubbleWidth
            val menuTop = endApparentTop + conversationItemSnapshot.height * endScale
            contextMenu.show(offsetX.toInt(), (menuTop + menuPadding).toInt())
        }
        val revealDuration = context.resources.getInteger(R.integer.reaction_scrubber_reveal_duration)
        conversationBubble.animate()
            .scaleX(endScale)
            .scaleY(endScale)
            .setDuration(revealDuration.toLong())
        conversationItem.animate()
            .x(endX)
            .y(endY)
            .setDuration(revealDuration.toLong())
    }
    private fun getReactionBarOffsetForTouch(itemY: Float,
                                             contextMenuTop: Float,
                                             contextMenuPadding: Float,
                                             reactionBarOffset: Float,
                                             reactionBarHeight: Int,
                                             spaceNeededBetweenTopOfScreenAndTopOfReactionBar: Float,
                                             messageTop: Float): Float {
        val adjustedTouchY = itemY - statusBarHeight
        var reactionStartingPoint = Math.min(adjustedTouchY, contextMenuTop)
        val spaceBetweenTopOfMessageAndTopOfContextMenu = Math.abs(messageTop - contextMenuTop)
        if (spaceBetweenTopOfMessageAndTopOfContextMenu < DimensionUnit.DP.toPixels(150f)) {
            val offsetToMakeReactionBarOffsetMatchMenuPadding = reactionBarOffset - contextMenuPadding
            reactionStartingPoint = messageTop + offsetToMakeReactionBarOffsetMatchMenuPadding
        }
        return Math.max(reactionStartingPoint - reactionBarOffset - reactionBarHeight, spaceNeededBetweenTopOfScreenAndTopOfReactionBar)
    }
    private fun updateSystemUiOnShow(activity: Activity) {
        val window = activity.window
        val barColor = ContextCompat.getColor(context, R.color.reactions_screen_dark_shade_color)
        originalStatusBarColor = window.statusBarColor
        WindowUtil.setStatusBarColor(window, barColor)
        originalNavigationBarColor = window.navigationBarColor
        WindowUtil.setNavigationBarColor(window, barColor)
        if (!ThemeUtil.isDarkTheme(context)) {
            WindowUtil.clearLightStatusBar(window)
            WindowUtil.clearLightNavigationBar(window)
        }
    }
    fun hide() {
        hideInternal(onHideListener)
    }
    fun hideForReactWithAny() {
        hideInternal(onHideListener)
    }
    private fun hideInternal(onHideListener: OnHideListener?) {
        job?.cancel()
        overlayState = OverlayState.HIDDEN
        val animatorSet = newHideAnimatorSet()
        hideAnimatorSet = animatorSet
        revealAnimatorSet.end()
        animatorSet.start()
        onHideListener?.startHide()
        selectedConversationModel.focusedView?.let(ViewUtil::focusAndShowKeyboard)
        animatorSet.addListener(object : AnimationCompleteListener() {
            override fun onAnimationEnd(animation: Animator) {
                animatorSet.removeListener(this)
                onHideListener?.onHide()
            }
        })
        contextMenu?.dismiss()
    }
    val isShowing: Boolean
        get() = overlayState != OverlayState.HIDDEN
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateBoundsOnLayoutChanged()
    }
    private fun updateBoundsOnLayoutChanged() {
        backgroundView.getGlobalVisibleRect(emojiStripViewBounds)
        emojiViews[0].getGlobalVisibleRect(emojiViewGlobalRect)
        emojiStripViewBounds.left = getStart(emojiViewGlobalRect)
        emojiViews[emojiViews.size - 1].getGlobalVisibleRect(emojiViewGlobalRect)
        emojiStripViewBounds.right = getEnd(emojiViewGlobalRect)
        segmentSize = emojiStripViewBounds.width() / emojiViews.size.toFloat()
    }
    private fun getStart(rect: Rect): Int = if (ViewUtil.isLtr(this)) rect.left else rect.right
    private fun getEnd(rect: Rect): Int = if (ViewUtil.isLtr(this)) rect.right else rect.left
    fun applyTouchEvent(motionEvent: MotionEvent): Boolean {
        check(isShowing) { "Touch events should only be propagated to this method if we are displaying the scrubber." }
        if (motionEvent.action and MotionEvent.ACTION_POINTER_INDEX_MASK != 0) {
            return true
        }
        if (overlayState == OverlayState.UNINITAILIZED) {
            downIsOurs = false
            deadzoneTouchPoint[motionEvent.x] = motionEvent.y
            overlayState = OverlayState.DEADZONE
        }
        if (overlayState == OverlayState.DEADZONE) {
            val deltaX = Math.abs(deadzoneTouchPoint.x - motionEvent.x)
            val deltaY = Math.abs(deadzoneTouchPoint.y - motionEvent.y)
            if (deltaX > touchDownDeadZoneSize || deltaY > touchDownDeadZoneSize) {
                overlayState = OverlayState.SCRUB
            } else {
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    overlayState = OverlayState.TAP
                    if (downIsOurs) {
                        handleUpEvent()
                        return true
                    }
                }
                return MotionEvent.ACTION_MOVE == motionEvent.action
            }
        }
        return when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                selected = getSelectedIndexViaDownEvent(motionEvent)
                deadzoneTouchPoint[motionEvent.x] = motionEvent.y
                overlayState = OverlayState.DEADZONE
                downIsOurs = true
                true
            }
            MotionEvent.ACTION_MOVE -> {
                selected = getSelectedIndexViaMoveEvent(motionEvent)
                true
            }
            MotionEvent.ACTION_UP -> {
                handleUpEvent()
                downIsOurs
            }
            MotionEvent.ACTION_CANCEL -> {
                hide()
                downIsOurs
            }
            else -> false
        }
    }
    private fun setupSelectedEmoji() {
        val emojis = recentEmojiPageModel.emoji
        emojiViews.forEachIndexed { i, view ->
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            view.translationY = 0f
            val isAtCustomIndex = i == customEmojiIndex
            if (isAtCustomIndex) {
                view.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_add_24))
                view.tag = null
            } else {
                view.setImageEmoji(emojis[i])
            }
        }
    }
    private fun getSelectedIndexViaDownEvent(motionEvent: MotionEvent): Int =
        getSelectedIndexViaMotionEvent(motionEvent, Boundary(emojiStripViewBounds.top.toFloat(), emojiStripViewBounds.bottom.toFloat()))
    private fun getSelectedIndexViaMoveEvent(motionEvent: MotionEvent): Int =
        getSelectedIndexViaMotionEvent(motionEvent, verticalScrubBoundary)
    private fun getSelectedIndexViaMotionEvent(motionEvent: MotionEvent, boundary: Boundary): Int {
        var selected = -1
        if (backgroundView.visibility != VISIBLE) {
            return selected
        }
        for (i in emojiViews.indices) {
            val emojiLeft = segmentSize * i + emojiStripViewBounds.left
            horizontalEmojiBoundary.update(emojiLeft, emojiLeft + segmentSize)
            if (horizontalEmojiBoundary.contains(motionEvent.x) && boundary.contains(motionEvent.y)) {
                selected = i
            }
        }
        if (this.selected != -1 && this.selected != selected) {
            shrinkView(emojiViews[this.selected])
        }
        if (this.selected != selected && selected != -1) {
            growView(emojiViews[selected])
        }
        return selected
    }
    private fun growView(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        view.animate()
            .scaleY(1.5f)
            .scaleX(1.5f)
            .translationY(-selectedVerticalTranslation.toFloat())
            .setDuration(200)
            .setInterpolator(INTERPOLATOR)
            .start()
    }
    private fun shrinkView(view: View) {
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .translationY(0f)
            .setDuration(200)
            .setInterpolator(INTERPOLATOR)
            .start()
    }
    private fun handleUpEvent() {
        val onReactionSelectedListener = onReactionSelectedListener
        if (selected != -1 && onReactionSelectedListener != null && backgroundView.visibility == VISIBLE) {
            if (selected == customEmojiIndex) {
                onReactionSelectedListener.onCustomReactionSelected(messageRecord, emojiViews[selected].tag != null)
            } else {
                onReactionSelectedListener.onReactionSelected(messageRecord, recentEmojiPageModel.emoji[selected])
            }
        } else {
            hide()
        }
    }
    fun setOnReactionSelectedListener(onReactionSelectedListener: OnReactionSelectedListener?) {
        this.onReactionSelectedListener = onReactionSelectedListener
    }
    fun setOnActionSelectedListener(onActionSelectedListener: OnActionSelectedListener?) {
        this.onActionSelectedListener = onActionSelectedListener
    }
    fun setOnHideListener(onHideListener: OnHideListener?) {
        this.onHideListener = onHideListener
    }
    private fun getOldEmoji(messageRecord: MessageRecord): String? =
        messageRecord.reactions
            .filter { it.author == getLocalNumber(context) }
            .firstOrNull()
            ?.let(ReactionRecord::emoji)
    private fun getMenuActionItems(message: MessageRecord): List<ActionItem> {
        val items: MutableList<ActionItem> = ArrayList()
        // Prepare
        val containsControlMessage = message.isUpdate
        val hasText = message.body.isNotEmpty() && !message.isDeleted
        val openGroup = get(context).beldexThreadDatabase().getOpenGroupChat(message.threadId)
        val recipient = get(context).threadDatabase().getRecipientForThreadId(message.threadId)
            ?: return emptyList()
        val userPublicKey = getLocalNumber(context)!!

        // control messages and "marked as deleted" messages can only delete
        val isDeleteOnly = message.isDeleted || message.isControlMessage

        val isSharedContact = message.isSharedContact


        // Select message
        if(!isDeleteOnly) {
            items += ActionItem(
                R.attr.menu_select_icon,
                context.resources.getString(R.string.accessibilityId_select),
                { handleActionItemClicked(Action.SELECT) },
                R.string.accessibilityId_select
            )
        }
        // Reply
        if (!message.isPending && !message.isFailed && !isDeleteOnly) {
            items += ActionItem(R.attr.menu_reply_icon, context.resources.getString(R.string.accessibilityId_reply), { handleActionItemClicked(Action.REPLY) }, R.string.accessibilityId_reply)
        }
        // Copy message text
        if (!containsControlMessage && hasText && !isSharedContact) {
            items += ActionItem(R.attr.menu_copy_icon, context.resources.getString(R.string.copy), { handleActionItemClicked(Action.COPY_MESSAGE) })
        }
        // Copy BChat ID
        if (recipient.isGroupRecipient && !recipient.isOpenGroupRecipient && message.recipient.address.toString() != userPublicKey && !isDeleteOnly) {
            items += ActionItem(R.attr.menu_copy_icon, context.resources.getString(R.string.activity_conversation_menu_copy_bchat_id), { handleActionItemClicked(Action.COPY_BCHAT_ID) })
        }
        // Delete message
        if (ConversationMenuItemHelper.userCanDeleteSelectItems(message, openGroup, userPublicKey)) {
            items += ActionItem(R.attr.menu_trash_icon,  context.resources.getString(R.string.delete), { handleActionItemClicked(Action.DELETE) },
                R.string.delete, message.subtitle, ThemeUtil.getThemedColor(context, R.attr.menu_delete_color))
        }
        // Ban user
        if (ConversationMenuItemHelper.userCanBanSelectUsers( message, openGroup, userPublicKey )) {
            items += ActionItem(R.attr.menu_block_icon, context.resources.getString(R.string.conversation_context__menu_ban_user), { handleActionItemClicked(Action.BAN_USER) })
        }
        // Ban and delete all
        if (ConversationMenuItemHelper.userCanBanSelectUsers( message, openGroup, userPublicKey)) {
            items += ActionItem(R.attr.menu_trash_icon,context.resources.getString(R.string.conversation_context__menu_ban_and_delete_all), { handleActionItemClicked(Action.BAN_AND_DELETE_ALL) })
        }
        // Message detail
        if(!isDeleteOnly && message.isFailed) {
            items+=ActionItem(
                R.attr.menu_info_icon,
                context.resources.getString(R.string.conversation_context__menu_message_details),
                { handleActionItemClicked(Action.VIEW_INFO) })
        }
        // Resend
        if (message.isFailed) {
            items += ActionItem(R.attr.menu_reply_icon, context.resources.getString(R.string.conversation_context__menu_resend_message), { handleActionItemClicked(Action.RESEND) })
        }
        // Save media
        if (message.isMms && (message as MediaMmsMessageRecord).containsMediaSlide()  && !isDeleteOnly) {
            val mmsMessage = message as MediaMmsMessageRecord
            if (mmsMessage.containsMediaSlide() && !mmsMessage.isMediaPending) {
                items += ActionItem(R.attr.menu_save_icon,
                    context.resources.getString(R.string.save),
                    { handleActionItemClicked(Action.DOWNLOAD) },
                    R.string.conversation_context_image__save_attachment
                )
            }
        }
        // deleted messages have  no emoji reactions
        backgroundView.isVisible = !isDeleteOnly
        foregroundView.isVisible = !isDeleteOnly
        return items
    }
    private fun handleActionItemClicked(action: Action) {
        hideInternal(object : OnHideListener {
            override fun startHide() {
                onHideListener?.startHide()
            }
            override fun onHide() {
                onHideListener?.onHide()
                onActionSelectedListener?.onActionSelected(action)
            }
        })
    }
    private fun initAnimators() {
        val revealDuration = context.resources.getInteger(R.integer.reaction_scrubber_reveal_duration)
        val revealOffset = context.resources.getInteger(R.integer.reaction_scrubber_reveal_offset)
        val reveals = emojiViews.mapIndexed { idx: Int, v: EmojiImageView? ->
            AnimatorInflaterCompat.loadAnimator(context, R.animator.reactions_scrubber_reveal).apply {
                setTarget(v)
                startDelay = (idx * animationEmojiStartDelayFactor).toLong()
            }
        } + AnimatorInflaterCompat.loadAnimator(context, android.R.animator.fade_in).apply {
            setTarget(backgroundView)
            setDuration(revealDuration.toLong())
            startDelay = revealOffset.toLong()
        }
        revealAnimatorSet.interpolator = INTERPOLATOR
        revealAnimatorSet.playTogether(reveals)
    }
    private fun newHideAnimatorSet(): AnimatorSet {
        val set = AnimatorSet()
        set.addListener(object : AnimationCompleteListener() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = GONE
            }
        })
        set.interpolator = INTERPOLATOR
        set.playTogether(newHideAnimators())
        return set
    }
    private fun newHideAnimators(): List<Animator> {
        val duration = context.resources.getInteger(R.integer.reaction_scrubber_hide_duration).toLong()
        fun conversationItemAnimator(configure: ObjectAnimator.() -> Unit) = ObjectAnimator().apply {
            target = conversationItem
            setDuration(duration)
            configure()
        }
        return emojiViews.map {
            AnimatorInflaterCompat.loadAnimator(context, R.animator.reactions_scrubber_hide).apply { setTarget(it) }
        } + AnimatorInflaterCompat.loadAnimator(context, android.R.animator.fade_out).apply {
            setTarget(backgroundView)
            setDuration(duration)
        } + conversationItemAnimator {
            setProperty(SCALE_X)
            setFloatValues(1f)
        } + conversationItemAnimator {
            setProperty(SCALE_Y)
            setFloatValues(1f)
        } + conversationItemAnimator {
            setProperty(X)
            setFloatValues(selectedConversationModel.bubbleX)
        } + conversationItemAnimator {
            setProperty(Y)
            setFloatValues(selectedConversationModel.bubbleY - statusBarHeight)
        } + ValueAnimator.ofArgb(activity.window.statusBarColor, originalStatusBarColor).apply {
            setDuration(duration)
            addUpdateListener { animation: ValueAnimator -> WindowUtil.setStatusBarColor(activity.window, animation.animatedValue as Int) }
        } + ValueAnimator.ofArgb(activity.window.statusBarColor, originalNavigationBarColor).apply {
            setDuration(duration)
            addUpdateListener { animation: ValueAnimator -> WindowUtil.setNavigationBarColor(activity.window, animation.animatedValue as Int) }
        }
    }
    interface OnHideListener {
        fun startHide()
        fun onHide()
    }
    interface OnReactionSelectedListener {
        fun onReactionSelected(messageRecord: MessageRecord, emoji: String)
        fun onCustomReactionSelected(messageRecord: MessageRecord, hasAddedCustomEmoji: Boolean)
    }
    interface OnActionSelectedListener {
        fun onActionSelected(action: Action)
    }
    private class Boundary {
        private var min = 0f
        private var max = 0f
        internal constructor()
        internal constructor(min: Float, max: Float) {
            update(min, max)
        }
        fun update(min: Float, max: Float) {
            this.min = min
            this.max = max
        }
        operator fun contains(value: Float): Boolean {
            return if (min < max) {
                min < value && max > value
            } else {
                min > value && max < value
            }
        }
    }
    private enum class OverlayState {
        HIDDEN,
        UNINITAILIZED,
        DEADZONE,
        SCRUB,
        TAP
    }
    enum class Action {
        REPLY,
        RESEND,
        RESYNC,
        DOWNLOAD,
        COPY_MESSAGE,
        COPY_BCHAT_ID,
        VIEW_INFO,
        SELECT,
        DELETE,
        BAN_USER,
        BAN_AND_DELETE_ALL
    }
    companion object {
        const val LONG_PRESS_SCALE_FACTOR = 0.95f
        private val INTERPOLATOR: Interpolator = DecelerateInterpolator()
    }
}

private val MessageRecord.subtitle: ((Context) -> CharSequence?)?
    get() = if (expiresIn <= 0) {
        null
    } else { context ->
        (expiresIn - (MnodeAPI.nowWithOffset - (expireStarted.takeIf { it > 0 } ?: timestamp)))
            .coerceAtLeast(0L)
            .milliseconds
            .toShortTwoPartString()
            .let {
                Phrase.from(context, R.string.disappearingMessagesCountdownBigMobile)
                    .put("time_large", it)
                    .format().toString()
            }
    }
package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getLocalNumber
import com.google.android.flexbox.JustifyContent
import io.beldex.bchat.R
import io.beldex.bchat.components.emoji.EmojiImageView
import io.beldex.bchat.components.emoji.EmojiUtil
import io.beldex.bchat.conversation.v2.ViewUtil
import io.beldex.bchat.database.model.MessageId
import io.beldex.bchat.database.model.ReactionRecord
import io.beldex.bchat.databinding.ViewEmojiReactionsBinding
import java.util.Collections
import java.util.Date

class EmojiReactionsView : ConstraintLayout, OnTouchListener {
    companion object {
        private const val DEFAULT_THRESHOLD = 5
        private const val longPressDurationThreshold: Long = 250
        private const val maxDoubleTapInterval: Long = 200
    }
    private val binding: ViewEmojiReactionsBinding by lazy { ViewEmojiReactionsBinding.bind(this) }
    // Normally 6dp, but we have 1dp left+right margin on the pills themselves
    private val OUTER_MARGIN = ViewUtil.dpToPx(2)
    private var records: MutableList<ReactionRecord>? = null
    private var messageId: Long = 0
    private var delegate: VisibleMessageViewDelegate? = null
    private val gestureHandler = Handler(Looper.getMainLooper())
    private var pressCallback: Runnable? = null
    private var longPressCallback: Runnable? = null
    private var onDownTimestamp: Long = 0
    private var extended = false
    constructor(context: Context) : super(context) { init(null) }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init(attrs) }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init(attrs) }
    private fun init(attrs: AttributeSet?) {
        records = ArrayList()
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.EmojiReactionsView, 0, 0)
            typedArray.recycle()
        }
    }
    fun clear() {
        records!!.clear()
        binding.layoutEmojiContainer.removeAllViews()
    }
    fun setReactions(messageId: Long, records: List<ReactionRecord>, outgoing: Boolean, delegate: VisibleMessageViewDelegate?) {
        this.delegate = delegate
        if (records == this.records) {
            return
        }
        binding.layoutEmojiContainer.justifyContent = if (outgoing) JustifyContent.FLEX_END else JustifyContent.FLEX_START
        this.records!!.clear()
        this.records!!.addAll(records)
        if (this.messageId != messageId) {
            extended = false
        }
        this.messageId = messageId
        val emojiCount=this.records!!.size.toString()
        binding.totalEmojiCount.text=emojiCount
        displayReactions(if (extended) Int.MAX_VALUE else DEFAULT_THRESHOLD)
    }
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v.tag == null) return false
        val reaction = v.tag as Reaction
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN) onClickEmoji(MessageId(reaction.messageId, reaction.isMms), reaction.emoji!!)
        else if (action == MotionEvent.ACTION_CANCEL) removeLongPressCallback()
        return true
    }
    private fun onClickEmoji(messageId : MessageId, emoji : String) {
        if (delegate != null) {
            delegate!!.onReactionLongClicked(messageId, emoji)
        }
    }
    private fun displayReactions(threshold: Int) {
        val userPublicKey = getLocalNumber(context)
        val reactions = buildSortedReactionsList(records!!, userPublicKey, threshold)
        binding.layoutEmojiContainer.removeAllViews()
        val overflowContainer = LinearLayout(context)
        overflowContainer.orientation = LinearLayout.HORIZONTAL
        val innerPadding = ViewUtil.dpToPx(2)
        overflowContainer.setPaddingRelative(innerPadding, innerPadding, innerPadding, innerPadding)
        val pixelSize = ViewUtil.dpToPx(1)
        for (reaction in reactions) {
            if (binding.layoutEmojiContainer.childCount + 1 >= DEFAULT_THRESHOLD && threshold != Int.MAX_VALUE && reactions.size > threshold) {
                if (overflowContainer.parent == null) {
                    binding.layoutEmojiContainer.addView(overflowContainer)
                    val overflowParams = overflowContainer.layoutParams as MarginLayoutParams
                    overflowParams.setMargins(pixelSize, pixelSize, pixelSize, pixelSize)
                    overflowContainer.layoutParams = overflowParams
                    overflowContainer.background = ContextCompat.getDrawable(context, R.drawable.over_flow_container_background)
                }
                val pill = buildPill(context, this, reaction, true)
                pill.setOnClickListener { v: View? ->
                    extended = true
                    displayReactions(Int.MAX_VALUE)
                }
                binding.layoutEmojiContainer.setPadding(16,0,0,0)
                overflowContainer.addView(pill)
            } else {
                val pill = buildPill(context, this, reaction, false)
                pill.tag = reaction
                pill.setOnTouchListener(this)
                val params = pill.layoutParams as MarginLayoutParams
                params.setMargins(pixelSize, pixelSize, pixelSize, pixelSize)
                pill.layoutParams = params
                binding.layoutEmojiContainer.addView(pill)
            }
        }
        val overflowChildren = overflowContainer.childCount
        val negativeMargin = ViewUtil.dpToPx(-8)
        for (i in 0 until overflowChildren) {
            val child = overflowContainer.getChildAt(i)
            val childParams = child.layoutParams as MarginLayoutParams
            if (i == 0 && overflowChildren > 1 || i + 1 < overflowChildren) {
                // if first and there is more than one child, or we are not the last child then set negative right margin
                childParams.setMargins(0, 0, negativeMargin, 0)
                child.layoutParams = childParams
            }
        }
        if (threshold == Int.MAX_VALUE) {
            binding.imageViewShowLess.visibility = VISIBLE
            binding.imageViewShowLess.setOnClickListener {
                extended = false
                displayReactions(DEFAULT_THRESHOLD)
            }
        } else {
            binding.imageViewShowLess.visibility = GONE
        }
    }
    private fun buildSortedReactionsList(records: List<ReactionRecord>, userPublicKey: String?, threshold: Int): List<Reaction> {
        val counters: MutableMap<String, Reaction> = LinkedHashMap()
        records.forEach {
            val baseEmoji = EmojiUtil.getCanonicalRepresentation(it.emoji)
            val info = counters[baseEmoji]
            if (info == null) {
                counters[baseEmoji] = Reaction(messageId, it.isMms, it.emoji, it.count, it.sortId, it.dateReceived, userPublicKey == it.author)
            }
            else {
                info.update(it.emoji, it.count, it.dateReceived, userPublicKey == it.author)
            }
        }
        val reactions: List<Reaction> = ArrayList(counters.values)
        Collections.sort(reactions, Collections.reverseOrder())
        return if (reactions.size >= threshold + 2 && threshold != Int.MAX_VALUE) {
            val shortened: MutableList<Reaction> = ArrayList(threshold + 2)
            shortened.addAll(reactions.subList(0, threshold + 2))
            shortened
        } else {
            reactions
        }
    }
    private fun buildPill(context: Context, parent: ViewGroup, reaction: Reaction, isCompact: Boolean): View {
        val root = LayoutInflater.from(context).inflate(R.layout.reactions_pill, parent, false)
        val emojiView = root.findViewById<EmojiImageView>(R.id.reactions_pill_emoji)
        if (isCompact) {
            root.setPaddingRelative(1, 1, 1, 1)
            val layoutParams = root.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            root.layoutParams = layoutParams
        }
        if (reaction.emoji != null) {
            emojiView.setImageEmoji(reaction.emoji)
        } else {
            emojiView.visibility = GONE
        }
        return root
    }
    private fun onReactionClicked(reaction: Reaction) {
        if (reaction.messageId != 0L) {
            val messageId = MessageId(reaction.messageId, reaction.isMms)
            delegate!!.onReactionClicked(reaction.emoji!!, messageId, reaction.userWasSender)
        }
    }
    private fun onDown(messageId: MessageId,emoji: String) {
        removeLongPressCallback()
        val newLongPressCallback = Runnable {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (delegate != null) {
                delegate!!.onReactionLongClicked(messageId,emoji)
            }
        }
        longPressCallback = newLongPressCallback
        gestureHandler.postDelayed(newLongPressCallback, longPressDurationThreshold)
        onDownTimestamp = Date().time
    }
    private fun removeLongPressCallback() {
        if (longPressCallback != null) {
            gestureHandler.removeCallbacks(longPressCallback!!)
        }
    }
    private fun onUp(reaction: Reaction) {
        if (Date().time - onDownTimestamp < longPressDurationThreshold) {
            removeLongPressCallback()
            if (pressCallback != null) {
                gestureHandler.removeCallbacks(pressCallback!!)
                pressCallback = null
            } else {
                val newPressCallback = Runnable {
                    onReactionClicked(reaction)
                    pressCallback = null
                }
                pressCallback = newPressCallback
                gestureHandler.postDelayed(newPressCallback, maxDoubleTapInterval)
            }
        }
    }
    internal class Reaction(
        internal val messageId: Long,
        internal val isMms: Boolean,
        internal var emoji: String?,
        internal var count: Long,
        internal val sortIndex: Long,
        internal var lastSeen: Long,
        internal var userWasSender: Boolean
    ) : Comparable<Reaction?> {
        fun update(emoji: String, count: Long, lastSeen: Long, userWasSender: Boolean) {
            if (!this.userWasSender) {
                if (userWasSender || lastSeen > this.lastSeen) {
                    this.emoji = emoji
                }
            }
            this.count = this.count + count
            this.lastSeen = Math.max(this.lastSeen, lastSeen)
            this.userWasSender = this.userWasSender || userWasSender
        }
        fun merge(other: Reaction): Reaction {
            count = count + other.count
            lastSeen = Math.max(lastSeen, other.lastSeen)
            userWasSender = userWasSender || other.userWasSender
            return this
        }
        override fun compareTo(other: Reaction?): Int {
            if (other == null) { return -1 }
            if (this.count == other.count) {
                return this.sortIndex.compareTo(other.sortIndex)
            }
            return this.count.compareTo(other.count)
        }
    }

    interface VisibleMessageViewAligned {
        fun showLessClicked(clicked: Boolean)
        // endregion
    }
}
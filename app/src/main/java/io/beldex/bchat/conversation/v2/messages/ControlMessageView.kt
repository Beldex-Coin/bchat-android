package io.beldex.bchat.conversation.v2.messages

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.R
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.databinding.ViewControlMessageBinding
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.DateUtils
import java.util.Locale


class ControlMessageView : LinearLayout {

    private lateinit var binding: ViewControlMessageBinding
    val controlContentView: View get() = binding.receiverStatusIconCardView

    // region Lifecycle
    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize()
    }

    private fun initialize() {
        binding = ViewControlMessageBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    /** Figma control-message chip: notched, #111111 fill, hairline; "You left this group" is red. */
    private fun styleChips(messageBody: CharSequence) {
        val density = resources.displayMetrics.density
        val left = messageBody.toString() == context.getString(R.string.MessageRecord_left_group)
        // Figma: #1A1A1A chip, no stroke; "You left this group" is red-tinted with a red hairline.
        binding.textView.background = io.beldex.bchat.compose_utils.NotchedDrawable(
            6 * density, if (left) 0.75f * density else 0f, if (left) 0xFF2B0A0A.toInt() else 0xFF1A1A1A.toInt(), 0xFFFF3D3D.toInt()
        )
        binding.textView.setTextColor(if (left) 0xFFFF3D3D.toInt() else 0xFF8D8D8D.toInt())
        binding.textView.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.roboto_mono)
        binding.dateBreakTextView.background = io.beldex.bchat.compose_utils.NotchedDrawable(
            8 * density, 0.5f * density, 0xFF111111.toInt(), 0xFF444444.toInt()
        )
    }

    // region Updating
    @SuppressLint("StringFormatMatches")
    fun bind(message: MessageRecord, previous: MessageRecord?, longPress: (() -> Unit)? = null) {
        binding.dateBreakTextView.showDateBreak(message, previous)
        binding.iconImageView.visibility = View.GONE

        val fontSize = TextSecurePreferences.getChatFontSize(context)!!
        // Control chips are a fixed 11sp in the design, regardless of the chat font size.
        binding.textView.textSize = 11f
        binding.receiverStatusIconTextView.textSize = fontSize.toFloat()
        binding.senderStatusIconTextView.textSize = fontSize.toFloat()
        binding.receivedCallText.textSize = 12f
        binding.dialledCallText.textSize = 12f
        binding.dateBreakTextView.textSize = fontSize.toFloat()

        //SteveJosephh21
        binding.receiverStatusIconCardView.visibility = View.GONE
        binding.senderStatusIconCardView.visibility = View.GONE
        binding.receivedCallCardView.visibility = View.GONE
        binding.dialledCallCardView.visibility = View.GONE

        /*Hales63*/
        var messageBody: CharSequence = message.getDisplayBody(context)
        var inlineIcon = 0
        when {
            message.isExpirationTimerUpdate -> {
                // Figma: the timer glyph sits inline at the start of the chip, not above it.
                inlineIcon = R.drawable.ic_disappear_timer
                binding.textView.textSize = 11f
                binding.textView.setTextColor(0xFFACACAC.toInt())
            }
            message.isMediaSavedNotification -> {
                inlineIcon = R.drawable.ic_control_media_saved
            }
            message.isCallLog -> {
                val isMissed = message.isMissedCall || message.isFirstMissedCall
                val title = context.getString(if (isMissed) R.string.ThreadRecord_missed_call else R.string.call_card_title)
                val iconRes = if (isMissed) R.drawable.ic_call_card_missed else R.drawable.ic_call_card_phone
                val iconTint: ColorStateList? = null
                val time = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
                if (message.isOutgoing && !isMissed) {
                    binding.dialledCallCardView.visibility = View.VISIBLE
                    binding.dialledCallBubble.background = VisibleMessageContentView.bubbleBackground(context, true)
                    binding.dialledCallIcon.setImageResource(iconRes)
                    binding.dialledCallIcon.imageTintList = iconTint
                    binding.dialledCallText.text = title
                    binding.dialledCallDuration.isVisible = true
                    binding.dialledMessageTime.text = time
                } else {
                    binding.receivedCallCardView.visibility = View.VISIBLE
                    binding.receivedCallBubble.background = VisibleMessageContentView.bubbleBackground(context, false)
                    binding.receivedCallIcon.setImageResource(iconRes)
                    binding.receivedCallIcon.imageTintList = iconTint
                    binding.receivedCallText.text = title
                    binding.receivedCallDuration.isVisible = !isMissed
                    binding.receivedMessageTime.text = time
                }
                messageBody = ""
            }
            message.isMessageRequestResponse -> {
                val msgRecipient=message.recipient.address.toString()
                val me=TextSecurePreferences.getLocalNumber(context)
                messageBody=if (me == msgRecipient) { // you accepted the user's request
                    val threadRecipient=DatabaseComponent.get(context).threadDatabase()
                        .getRecipientForThreadId(message.threadId)
                    context.getString(
                        R.string.message_request_you_have_accepted, threadRecipient?.name ?: ""
                    )
                } else { // they accepted your request
                    context.getString(R.string.message_requests_accepted)
                }
            }
        }
        val youLeftGroup = message.isOutgoing &&
            messageBody.toString() == context.getString(R.string.MessageRecord_left_group)
        binding.textView.isVisible = messageBody.trim().isNotEmpty() && !youLeftGroup
        binding.textView.text = messageBody
        val iconDrawable = if (inlineIcon != 0) ContextCompat.getDrawable(context, inlineIcon)?.mutate() else null
        iconDrawable?.let { it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight) }
        binding.textView.setCompoundDrawablesRelative(iconDrawable, null, null, null)
        binding.textView.compoundDrawablePadding = (6 * resources.displayMetrics.density).toInt()
        styleChips(messageBody)

        // handle long clicked if it was passed on
        longPress?.let {
            binding.receiverStatusIconCardView.setOnLongClickListener {
                longPress.invoke()
                true
            }
        }
    }


    fun recycle() {

    }
    // endregion
}
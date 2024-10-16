package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewControlMessageBinding
import java.util.Locale


class ControlMessageView : LinearLayout {

    private lateinit var binding: ViewControlMessageBinding

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
    // endregion

    // region Updating
    fun bind(message: MessageRecord, previous: MessageRecord?) {
        binding.dateBreakTextView.showDateBreak(message, previous)
        binding.iconImageView.visibility = View.GONE

        val fontSize = TextSecurePreferences.getChatFontSize(context)
        binding.textView.textSize = fontSize!!.toFloat()
        binding.receiverStatusIconTextView.textSize = fontSize.toFloat()
        binding.senderStatusIconTextView.textSize = fontSize.toFloat()
        binding.receivedCallText.textSize = fontSize.toFloat()
        binding.dialledCallText.textSize = fontSize.toFloat()
        binding.dateBreakTextView.textSize = fontSize.toFloat()

        //SteveJosephh21
        binding.receiverStatusIconCardView.visibility = View.GONE
        binding.senderStatusIconCardView.visibility = View.GONE
        binding.receivedCallCardView.visibility = View.GONE
        binding.dialledCallCardView.visibility = View.GONE

        /*Hales63*/
        var messageBody: CharSequence = message.getDisplayBody(context)
        when {
            message.isExpirationTimerUpdate -> {
                binding.iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_timer, context.theme)
                )
                binding.iconImageView.visibility = View.VISIBLE
            }
            message.isMediaSavedNotification -> {
                binding.iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_file_download_white_36dp,
                        context.theme
                    )
                )
                binding.iconImageView.visibility = View.VISIBLE
            }
            message.isCallLog -> {
                val drawable = when {
                    message.isIncomingCall -> {
                        binding.receivedCallText.text = context.resources.getString(R.string.voice_call)
                        R.drawable.ic_filled_circle_incoming_call
                    }
                    message.isOutgoingCall -> {
                        binding.dialledCallText.text = context.resources.getString(R.string.voice_call)
                        R.drawable.ic_filled_circle_outgoing_call
                    }
                    message.isMissedCall -> {
                        binding.receivedCallText.text = context.resources.getString(R.string.ThreadRecord_missed_call)
                        R.drawable.ic_filled_circle_missed_call
                    }
                    message.isFirstMissedCall -> {
                        binding.receivedCallText.text = context.resources.getString(R.string.ThreadRecord_missed_call)
                        /*binding.receivedCallTime.text = context.resources.getString(R.string.tap_to_callback)*/
                        R.drawable.ic_first_missed_call
                    }
                    else -> {
                        R.drawable.ic_filled_circle_missed_call
                    }
                }
               /* binding.iconImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        drawable,
                        context.theme
                    )
                )
                binding.iconImageView.visibility = View.VISIBLE*/
                message

                //SteveJosephh21
                if(message.isOutgoing){
                    binding.dialledCallCardView.visibility = View.VISIBLE
                    binding.dialledCallIcon.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            drawable,
                            context.theme
                        )
                    )
//                    binding.senderStatusIconTextView.text = messageBody
                    messageBody = ""
                    binding.dialledMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
                }else{
                    binding.receivedCallCardView.visibility = View.VISIBLE
                    binding.receivedCallIcon.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            drawable,
                            context.theme
                        )
                    )
//                    binding.receiverStatusIconTextView.text = messageBody
                    messageBody = ""
                    binding.receivedMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
                }
                if (message.isOutgoing) {
                    binding.dialledCallCardView.background=ContextCompat.getDrawable(
                        context,
                        R.drawable.message_bubble_background_sent_end
                    )
                    binding.dialledCallCardView.backgroundTintList=
                        ContextCompat.getColorStateList(context, R.color.button_green)
                } else {
                    binding.receivedCallCardView.background=ContextCompat.getDrawable(
                        context,
                        R.drawable.message_bubble_background_received_end
                    )
                    binding.receivedCallCardView.backgroundTintList=ContextCompat.getColorStateList(
                        context,
                        R.color.received_message_background
                    )
                }
            }
            message.isMessageRequestResponse -> {
                messageBody = context.getString(R.string.message_requests_accepted)
            }
        }
        binding.textView.isVisible = messageBody.trim().isNotEmpty()
        binding.textView.text = messageBody
    }

    fun recycle() {

    }
    // endregion
}
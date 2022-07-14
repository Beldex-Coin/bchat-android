package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewControlMessageBinding
import com.thoughtcrimes.securesms.database.model.MessageRecord

class ControlMessageView : LinearLayout {

    private lateinit var binding: ViewControlMessageBinding

    // region Lifecycle
    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        binding = ViewControlMessageBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }
    // endregion

    // region Updating
    fun bind(message: MessageRecord, previous: MessageRecord?) {
        binding.dateBreakTextView.showDateBreak(message, previous)
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
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_file_download_white_36dp, context.theme)
                )
                binding.iconImageView.visibility = View.VISIBLE
            }
            message.isMessageRequestResponse -> {
                messageBody = context.getString(R.string.message_requests_accepted)
            }
        }
        binding.textView.text = messageBody
    }

    fun recycle() {

    }
    // endregion
}
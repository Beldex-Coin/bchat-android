package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.OpenGroupUrlParser
import com.thoughtcrimes.securesms.database.model.MessageRecord
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewOpenGroupInvitationBinding
import io.beldex.bchat.databinding.ViewPaymentCardBinding

class PaymentCardView : LinearLayout {

    private lateinit var binding: ViewPaymentCardBinding
    private var data: UpdateMessageData.Kind.Payment? = null

    constructor(context: Context): super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        binding = ViewPaymentCardBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun bind(message: MessageRecord, @ColorInt textColor: Int) {
        // FIXME: This is a really weird approach...
        val umd = UpdateMessageData.fromJSON(message.body)!!
        val data = umd.kind as UpdateMessageData.Kind.Payment
        this.data = data
        val iconID = R.drawable.ic_beldex_white_logo
        with(binding){
            paymentCardViewBdxIconImageView.setImageResource(iconID)
            paymentCardViewBdxAmountTextView.text = data.amount
            paymentCardViewMessageTextView.text = if(message.isOutgoing) resources.getString(R.string.payment_success_message) else resources.getString(R.string.payment_received_message)
            paymentCardViewMessageTextView.textAlignment = if(message.isOutgoing) TEXT_ALIGNMENT_TEXT_END else TEXT_ALIGNMENT_TEXT_START
        }
    }
}
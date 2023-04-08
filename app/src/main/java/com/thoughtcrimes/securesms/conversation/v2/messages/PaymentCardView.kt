package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.database.model.MessageRecord
import io.beldex.bchat.R
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
            val fontSize = TextSecurePreferences.getChatFontSize(context)
            binding.paymentCardViewBdxAmountTextView.textSize = fontSize!!.toFloat()
            binding.paymentCardViewBdxTextView.textSize = fontSize.toFloat()
            binding.paymentCardViewMessageTextView.textSize = fontSize.toFloat()
            paymentCardViewBdxIconImageView.setImageResource(iconID)
            paymentCardViewBdxAmountTextView.text = data.amount
            paymentCardViewMessageTextView.text = if(message.isOutgoing) resources.getString(R.string.payment_success_message) else resources.getString(R.string.payment_received_message)
            paymentCardViewMessageTextView.textAlignment = if(message.isOutgoing) TEXT_ALIGNMENT_TEXT_END else TEXT_ALIGNMENT_TEXT_START
            viewPaymentCard.setOnClickListener{
                try {
                    //val url = "https://explorer.beldex.io/tx/${data.txnId}" // Mainnet
                    val url = "http://154.26.139.105/tx/${data.txnId}" // Testnet
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Can't open URL", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
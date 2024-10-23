package io.beldex.bchat.conversation.v2.messages

import android.R.color
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewPaymentCardBinding
import io.beldex.bchat.util.DateUtils
import java.util.Locale


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
        val iconID = R.drawable.ic_payment_beldex_logo
        with(binding) {
            val fontSize = TextSecurePreferences.getChatFontSize(context)
            binding.paymentCardViewBdxAmountTextView.textSize = fontSize!!.toFloat()
            binding.paymentCardViewBdxTextView.textSize = fontSize.toFloat()
            paymentCardViewBdxIconImageView.setImageResource(iconID)
            paymentCardViewBdxAmountTextView.text = data.amount
            paymentCardViewMessageTextView.text =
                if (message.isOutgoing) resources.getString(R.string.payment_success_message) else resources.getString(
                    R.string.payment_received_message
                )
            paymentCardViewMessageTextView.setTextColor(
                if (message.isOutgoing) context.getColor(R.color.white) else context.getColor(R.color.button_green)
            )
            paymentCardViewMessageImageView.setColorFilter(
                if (message.isOutgoing) context.getColor(R.color.white) else context.getColor(R.color.button_green)
            )
            paymentCardViewMessageTextView.textAlignment =
                if (message.isOutgoing) TEXT_ALIGNMENT_TEXT_END else TEXT_ALIGNMENT_TEXT_START
            paymentCardViewBdxTextView.setTextColor(
                if (message.isOutgoing) context.getColor(R.color.outgoing_message_bdx_text) else context.getColor(
                    R.color.incoming_message_bdx_text
                )
            )
            paymentCardViewBdxAmountTextView.setTextColor(
                if (message.isOutgoing) context.getColor(R.color.outgoing_message_amount_text) else context.getColor(
                    R.color.incoming_message_amount_text
                )
            )
            paymentCardViewBdxIconImageView.setColorFilter(
                if (message.isOutgoing) context.getColor(R.color.outgoing_message_amount_text) else context.getColor(
                    R.color.incoming_message_amount_text
                )
            )
            paymentCardView.setCardBackgroundColor(
                if (message.isOutgoing) context.getColor(R.color.outgoing_call_background) else context.getColor(
                    R.color.payment_card_view_background
                )
            )
            viewPaymentCard.setOnClickListener {
                try {
                    val url = "${BuildConfig.EXPLORER_URL}/tx/${data.txnId}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Can't open URL", Toast.LENGTH_LONG).show()
                }
            }

            paymentCardViewMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
            paymentCardViewMessageTime.setTextColor(
                VisibleMessageContentView.getTimeTextColor(
                    context,
                    message.isOutgoing
                )
            )
        }
    }

}
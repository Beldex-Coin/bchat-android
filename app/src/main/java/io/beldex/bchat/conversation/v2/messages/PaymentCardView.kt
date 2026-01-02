package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewPaymentCardBinding
import io.beldex.bchat.util.DateUtils
import java.util.Locale
import androidx.core.net.toUri


class PaymentCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: ViewPaymentCardBinding
    private var data: UpdateMessageData.Kind.Payment? = null
    private var boundMessageId: Long? = null
    private val locale = Locale.getDefault()

    init {
        binding = ViewPaymentCardBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        binding.viewPaymentCard.setOnClickListener {
            data?.let { payment ->
                try {
                    val url = "${BuildConfig.EXPLORER_URL}/tx/${payment.txnId}"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Can't open URL", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun bind(message: MessageRecord) {
        if (boundMessageId != message.id) {
            val umd = UpdateMessageData.fromJSON(message.body) ?: return
            data = umd.kind as? UpdateMessageData.Kind.Payment ?: return
            boundMessageId = message.id
        }
        val payment = data ?: return
        val iconID = R.drawable.ic_payment_beldex_logo
        val fontSize = TextSecurePreferences.getChatFontSize(context) ?: return
        val isOutgoing = message.isOutgoing
        with(binding) {
            binding.paymentCardViewBdxAmountTextView.textSize = fontSize.toFloat()
            binding.paymentCardViewBdxTextView.textSize = fontSize.toFloat()
            paymentCardViewBdxIconImageView.setImageResource(iconID)
            paymentCardViewBdxAmountTextView.text = payment.amount
            paymentCardViewMessageTextView.text =
                if (isOutgoing) resources.getString(R.string.payment_success_message) else resources.getString(
                    R.string.payment_received_message
                )
            paymentCardViewMessageTextView.setTextColor(
                if (isOutgoing) context.getColor(R.color.white) else context.getColor(R.color.button_green)
            )
            paymentCardViewMessageImageView.setColorFilter(
                if (isOutgoing) context.getColor(R.color.white) else context.getColor(R.color.button_green)
            )
            paymentCardViewMessageTextView.textAlignment =
                if (isOutgoing) TEXT_ALIGNMENT_TEXT_END else TEXT_ALIGNMENT_TEXT_START
            paymentCardViewBdxTextView.setTextColor(
                if (isOutgoing) context.getColor(R.color.outgoing_message_bdx_text) else context.getColor(
                    R.color.incoming_message_bdx_text
                )
            )
            paymentCardViewBdxAmountTextView.setTextColor(
                if (isOutgoing) context.getColor(R.color.outgoing_message_amount_text) else context.getColor(
                    R.color.incoming_message_amount_text
                )
            )
            paymentCardViewBdxIconImageView.setColorFilter(
                if (isOutgoing) context.getColor(R.color.outgoing_message_amount_text) else context.getColor(
                    R.color.incoming_message_amount_text
                )
            )
            paymentCardView.setCardBackgroundColor(
                if (isOutgoing) context.getColor(R.color.outgoing_call_background) else context.getColor(
                    R.color.payment_card_view_background
                )
            )

            paymentCardViewMessageTime.text = DateUtils.getTimeStamp(context, locale, message.timestamp)
            paymentCardViewMessageTime.setTextColor(
                VisibleMessageContentView.getTimeTextColor(
                    context,
                    isOutgoing
                )
            )
        }
    }

}
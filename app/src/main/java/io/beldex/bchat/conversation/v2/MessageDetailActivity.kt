package io.beldex.bchat.conversation.v2

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.beldex.libbchat.mnode.MnodeAPI
import io.beldex.bchat.R
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ExpirationUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.conversation.v2.utilities.ResendMessageUtilities
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.databinding.ActivityMessageDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailActivity: PassphraseRequiredActionBarActivity() {
    private lateinit var binding: ActivityMessageDetailBinding
    var messageRecord: MessageRecord? = null

    // region Settings
    companion object {
        // Extras
        const val MESSAGE_TIMESTAMP = "message_timestamp"
    }
    // endregion

    override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
        super.onCreate(savedInstanceState, ready)
        binding = ActivityMessageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = resources.getString(R.string.message_details)
        val timestamp = intent.getLongExtra(MESSAGE_TIMESTAMP, -1L)
        // We only show this screen for messages fail to send,
        // so the author of the messages must be the current user.
        val author = Address.fromSerialized(TextSecurePreferences.getLocalNumber(this)!!)
        messageRecord = DatabaseComponent.get(this).mmsSmsDatabase().getMessageFor(timestamp, author) ?: run {
            finish()
            return
        }
        updateContent()
        binding.resendButton.setOnClickListener {
            ResendMessageUtilities.resend(messageRecord!!)
            finish()
        }
    }

    fun updateContent() {
        val dateLocale = Locale.getDefault()
        val dateFormatter: SimpleDateFormat = DateUtils.getDetailedDateFormatter(this, dateLocale)
        binding.sentTime.text = dateFormatter.format(Date(messageRecord!!.dateSent))

        val errorMessage = DatabaseComponent.get(this).beldexMessageDatabase().getErrorMessage(messageRecord!!.getId())
        if (errorMessage != null) {
            binding.messageStatusTitle.text = getString(R.string.tx_failed)
            binding.messageStatusTitle.setTextColor(this.getColor(R.color.negative_red_button_border))
            binding.errorMessage.text = errorMessage
            binding.resendContainer.isVisible = true
            binding.errorContainer.isVisible = true
        } else {
            binding.messageStatusTitle.text = getString(R.string.message_details_header__sent)
            binding.messageStatusTitle.setTextColor(this.getColor(R.color.text))
            binding.errorContainer.isVisible = false
            binding.resendContainer.isVisible = false
        }

        if (messageRecord!!.expiresIn <= 0 || messageRecord!!.expireStarted <= 0) {
            binding.expiresContainer.visibility = View.GONE
        } else {
            binding.expiresContainer.visibility = View.VISIBLE
            val elapsed = MnodeAPI.nowWithOffset - messageRecord!!.expireStarted
            val remaining = messageRecord!!.expiresIn - elapsed

            val duration = ExpirationUtil.getExpirationDisplayValue(this, Math.max((remaining / 1000).toInt(), 1))
            binding.expiresIn.text = duration
        }
    }
}
package com.thoughtcrimes.securesms.conversation.v2

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import io.beldex.bchat.R
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ExpirationUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.conversation.v2.utilities.ResendMessageUtilities
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.DateUtils
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
        title = resources.getString(R.string.conversation_context__menu_message_details)
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
            binding.errorMessage.text = errorMessage
            binding.resendContainer.isVisible = true
            binding.errorContainer.isVisible = true
        } else {
            binding.errorContainer.isVisible = false
            binding.resendContainer.isVisible = false
        }

        if (messageRecord!!.expiresIn <= 0 || messageRecord!!.expireStarted <= 0) {
            binding.expiresContainer.visibility = View.GONE
        } else {
            binding.expiresContainer.visibility = View.VISIBLE
            val elapsed = System.currentTimeMillis() - messageRecord!!.expireStarted
            val remaining = messageRecord!!.expiresIn - elapsed

            val duration = ExpirationUtil.getExpirationDisplayValue(this, Math.max((remaining / 1000).toInt(), 1))
            binding.expiresIn.text = duration
        }
    }
}
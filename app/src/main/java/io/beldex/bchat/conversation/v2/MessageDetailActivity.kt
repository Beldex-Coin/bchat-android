package io.beldex.bchat.conversation.v2

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.appcompat.widget.Toolbar
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
import io.beldex.bchat.WindowInsetsUtil
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
        WindowInsetsUtil.applyTopInset(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = resources.getString(R.string.message_details)
        val timestamp = intent.getLongExtra(MESSAGE_TIMESTAMP, -1L)
        // Sent messages are matched on our own address first; received messages (opened from the
        // message options) have another author, so fall back to the message with that sent time.
        val author = Address.fromSerialized(TextSecurePreferences.getLocalNumber(this)!!)
        val mmsSmsDb = DatabaseComponent.get(this).mmsSmsDatabase()
        messageRecord = mmsSmsDb.getMessageFor(timestamp, author)
            ?: mmsSmsDb.getNonDeletedMessageForTimestamp(timestamp)
            ?: run {
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
        val record = messageRecord!!
        binding.sentTime.text = dateFormatter.format(Date(if (record.isOutgoing) record.dateSent else record.dateReceived))

        val errorMessage = DatabaseComponent.get(this).beldexMessageDatabase().getErrorMessage(messageRecord!!.getId())
        if (errorMessage != null) {
            binding.messageStatusTitle.text = getString(R.string.tx_failed)
            binding.messageStatusTitle.setTextColor(this.getColor(R.color.negative_red_button_border))
            binding.errorMessage.text = errorMessage
            binding.resendContainer.isVisible = true
            binding.errorContainer.isVisible = true
        } else {
            binding.messageStatusTitle.text = getString(
                if (record.isOutgoing) R.string.message_details_header__sent else R.string.message_details_header__received
            )
            binding.messageStatusTitle.setTextColor(this.getColor(R.color.disappear_icon_color))
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

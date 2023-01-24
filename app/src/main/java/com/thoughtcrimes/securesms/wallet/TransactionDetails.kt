package com.thoughtcrimes.securesms.wallet

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.util.Helper
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityAddressBookBinding
import io.beldex.bchat.databinding.ActivityTransactionDetailsBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetails : PassphraseRequiredActionBarActivity() {

    private lateinit var binding:ActivityTransactionDetailsBinding
    private val DATETIME_FORMATTER = SimpleDateFormat("dd-MM-yyyy HH:mm")

    val ARG_INFO = "info"

    private var outboundColour = 0
    private var inboundColour = 0
    private var pendingColour = 0
    private var failedColour = 0
    var info: TransactionInfo? = null

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.transaction_details_title)
        info = intent.extras?.getParcelable(ARG_INFO)

        inboundColour = ContextCompat.getColor(this, R.color.tx_plus)
        outboundColour = ContextCompat.getColor(this, R.color.wallet_send_button)//tx_minus
        pendingColour = ContextCompat.getColor(this, R.color.tx_pending)
        failedColour = ContextCompat.getColor(this, R.color.tx_failed)
        val cal = Calendar.getInstance()
        val tz = cal.timeZone //get the local time zone.
        this.DATETIME_FORMATTER.timeZone = tz


        with(binding) {
            //this.transitionName = getString(R.string.tx_item_transition_name, info!!.hash)
            val displayAmount: String = Helper.getDisplayAmount(info!!.amount, Helper.DISPLAY_DIGITS_INFO)
            Log.d("infoItem!!.direction", "${info!!.direction}")
            if (info!!.direction === TransactionInfo.Direction.Direction_Out) {
                transactionStatus.text = getString(R.string.tx_status_sent)
                if (displayAmount > 0.toString()) {
                    transactionAmount.text = getString(R.string.tx_list_amount_negative, displayAmount)
                    Log.d("Beldex", "Transaction list issue  value of amount - $displayAmount")
                    transactionAmount.setTextColor(
                        ContextCompat.getColor(
                            this@TransactionDetails,
                            R.color.wallet_send_button
                        )
                    )
                }
            } else {
                transactionStatus.text = getString(R.string.tx_status_received)
                if (displayAmount > 0.toString()) {
                    transactionAmount.text = getString(R.string.tx_list_amount_positive, displayAmount)
                    Log.d("Beldex", "Transaction list issue  value of amount + $displayAmount")
                    transactionAmount.setTextColor(
                        ContextCompat.getColor(
                            this@TransactionDetails,
                            R.color.wallet_receive_button
                        )
                    )
                }
            }
            transactionId.text = info!!.hash
            //SteveJosephh21
            if (transactionId.text.isNotEmpty()) {
                transactionId.setOnClickListener {
                    try {
                        val url = "https://explorer.beldex.io/tx/${transactionId.text}" // Mainnet
                        //val url = "http://154.26.139.105/tx/${transactionId.text}" // Testnet
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@TransactionDetails, "Can't open URL", Toast.LENGTH_LONG).show()
                    }
                }
            }
            when {
                info!!.isFailed -> {
                    transactionHeight.text = getString(R.string.tx_failed)
                }
                info!!.isPending -> {
                    transactionHeight.text = getString(R.string.tx_pending)
                }
                else -> {
                    transactionHeight.text = info!!.blockheight.toString()
                }
            }
            transactionDateAndTimeHead.text = getDateTime(info!!.timestamp)

            if (info!!.fee > 0) {
                val fee: String =
                    Helper.getDisplayAmount(info!!.fee, Helper.DISPLAY_DIGITS_INFO)
                transactionFee.text = getString(R.string.tx_list_fee, fee)
                transactionFee.visibility = View.VISIBLE
                transactionFeeTitle.visibility = View.VISIBLE
            } else {
                transactionFee.text = ""
                transactionFee.visibility = View.GONE
                transactionFeeTitle.visibility = View.GONE
            }
            when {
                info!!.isFailed -> {
                    transactionAmount.text = getString(R.string.tx_list_amount_failed, displayAmount)
                    transactionFee.text = getString(R.string.tx_list_failed_text)
                    transactionFee.visibility = View.VISIBLE
                    transactionFeeTitle.visibility = View.VISIBLE
                    setTxColour(failedColour)
                }
                info!!.isPending -> {
                    setTxColour(pendingColour)
                }
                info!!.direction === TransactionInfo.Direction.Direction_In -> {
                    setTxColour(inboundColour)
                }
                else -> {
                    setTxColour(outboundColour)
                }
            }
            transactionDateAndTime.text = getDateTime(info!!.timestamp)

            transactionRecipientAddressTitle.visibility = View.VISIBLE
            if(DatabaseComponent.get(this@TransactionDetails).bchatRecipientAddressDatabase().getRecipientAddress(info!!.hash)!=null) {
                transactionRecipientAddress.text = DatabaseComponent.get(this@TransactionDetails).bchatRecipientAddressDatabase()
                    .getRecipientAddress(info!!.hash)
            }//infoItem!!.address
            else{
                transactionRecipientAddressTitle.visibility = View.GONE
                transactionRecipientAddress.text=""
            }
        }
    }

        private fun getDateTime(time: Long): String {
            return DATETIME_FORMATTER.format(Date(time * 1000))
        }

        private fun setTxColour(clr: Int) {
            binding.transactionAmount.setTextColor(clr)
        }
}
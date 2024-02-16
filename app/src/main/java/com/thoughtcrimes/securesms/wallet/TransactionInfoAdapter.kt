package com.thoughtcrimes.securesms.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings.Global.getString
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.get
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.io.Resources.getResource
import com.thoughtcrimes.securesms.data.Crypto
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.util.Helper
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TransactionInfoAdapter(context: Context?)  :
    RecyclerView.Adapter<TransactionInfoAdapter.ViewHolder>() {
    private val DATETIME_FORMATTER = SimpleDateFormat("dd-MM-yyyy HH:mm")

    private var outboundColour = 0
    private var inboundColour = 0
    private var pendingColour = 0
    private var failedColour = 0


    var infoItems: ArrayList<TransactionInfo>? = null

    private var context: Context? = null

    init{
        this.context =context
        inboundColour = ContextCompat.getColor(context!!, R.color.tx_plus)
        outboundColour = ContextCompat.getColor(context, R.color.wallet_send_button)//tx_minus
        pendingColour = ContextCompat.getColor(context, R.color.tx_pending)
        failedColour = ContextCompat.getColor(context, R.color.tx_failed)
        infoItems = ArrayList()
        val cal = Calendar.getInstance()
        val tz = cal.timeZone //get the local time zone.
        this.DATETIME_FORMATTER.timeZone = tz
    }

    fun needsTransactionUpdateOnNewBlock(): Boolean {
        return infoItems!!.size > 0 && !infoItems!![0].isConfirmed
    }

    private class TransactionInfoDiff(
        oldList: List<TransactionInfo>,
        newList: List<TransactionInfo>
    ) :
        DiffCallback<TransactionInfo>(oldList, newList) {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return mOldList[oldItemPosition].hash.equals(mNewList[newItemPosition].hash)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem: TransactionInfo = mOldList[oldItemPosition]
            val newItem: TransactionInfo = mNewList[newItemPosition]
            return (oldItem.direction === newItem.direction
                    && oldItem.isPending === newItem.isPending
                    && oldItem.isFailed === newItem.isFailed
                    && (oldItem.confirmations === newItem.confirmations || oldItem.isConfirmed)
                    && oldItem.subaddressLabel.equals(newItem.subaddressLabel)
                    && oldItem.notes == newItem.notes)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.transaction_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(position)

        holder.itemView.setOnClickListener {
            if (holder.transactionDetailsLayout.visibility == View.GONE) {
                holder.transactionDetailsLayout.visibility = View.VISIBLE
                holder.tvAddressTitle.visibility = View.VISIBLE
                if (DatabaseComponent.get(context!!).bchatRecipientAddressDatabase()
                        .getRecipientAddress(holder.infoItem!!.hash) != null
                ) {
                    holder.tvAddress.text =
                        DatabaseComponent.get(context!!).bchatRecipientAddressDatabase()
                            .getRecipientAddress(holder.infoItem!!.hash)
                }//infoItem!!.address
                else {
                    holder.tvAddressTitle.visibility = View.GONE
                    holder.tvAddress.text = ""
                }
                holder.expandableArrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)
            } else {
                holder.transactionDetailsLayout.visibility = View.GONE
                holder.expandableArrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return infoItems!!.size
    }

    fun setInfos(newItems: List<TransactionInfo>?) {
        var newItems = newItems
        if (newItems == null) {
            newItems = ArrayList()
            Timber.d("setInfos null")
        } else {
            Timber.d("setInfos %s", newItems.size)
        }
        Collections.sort(newItems)
        val diffCallback: DiffCallback<TransactionInfo> = TransactionInfoDiff(infoItems!!, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        infoItems!!.clear()
        infoItems!!.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    fun removeItem(position: Int) {
        val newItems: ArrayList<TransactionInfo> = ArrayList(infoItems)
        if (newItems.size > position) newItems.removeAt(position)
        setInfos(newItems) // in case the nodeinfo has changed
    }

    fun getItem(position: Int): TransactionInfo {
        return infoItems!![position]
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private var tvAmount: TextView = itemView.findViewById(R.id.transaction_amount)
        private var tvFeeTitle:TextView = itemView.findViewById(R.id.transaction_fee_title)
        private var tvFee: TextView = itemView.findViewById(R.id.transaction_fee)
        var tvAddress: TextView = itemView.findViewById(R.id.transaction_recipient_address)
        var tvAddressTitle:TextView = itemView.findViewById(R.id.transaction_recipient_address_title)
        private var txId: TextView = itemView.findViewById(R.id.transaction_id)
        private var tvTxBlockHeight: TextView = itemView.findViewById(R.id.transaction_height)
        private var tvTxStatus: TextView = itemView.findViewById(R.id.transaction_status)
        private var tvTxStatusIcon: ImageView = itemView.findViewById(R.id.transaction_status_icon)
        private var tvDateTimeHead: TextView = itemView.findViewById(R.id.transaction_date_and_time_head)
        var expandableArrow:ImageView = itemView.findViewById(R.id.transaction_expandable_arrow)

        //var tvPaymentId: TextView
        private var tvDateTime: TextView = itemView.findViewById(R.id.transaction_date_and_time)

        var transactionDetailsLayout: LinearLayout = itemView.findViewById(R.id.transaction_details_layout)

        //val pbConfirmations: CircularProgressIndicator
        //var tvConfirmations: TextView
        var infoItem: TransactionInfo? = null
        private fun getDateTime(time: Long): String {
            return DATETIME_FORMATTER.format(Date(time * 1000))
        }

        private fun setTxColour(clr: Int) {
            tvAmount.setTextColor(clr)
        }

        fun bind(position: Int) {
            infoItem = infoItems!![position]
            itemView.transitionName = context!!.getString(R.string.tx_item_transition_name, infoItem!!.hash)
            val isBNS = infoItem!!.isBns
          /*  val userNotes = UserNotes(infoItem!!.notes)*/
         /*   if (userNotes.bdxtoKey != null) {
                val crypto: Crypto? = Crypto.withSymbol(userNotes.bdxtoCurrency)
                if (crypto != null) {
                    //ivTxType.setImageResource(crypto.getIconEnabledId())
                    //ivTxType.visibility = View.VISIBLE
                } else { // otherwirse pretend we don't know it's a shift
                    //ivTxType.visibility = View.GONE
                }
            } else {
                //ivTxType.visibility = View.GONE
            }*/
            val displayAmount: String = Helper.getDisplayAmount(infoItem!!.amount, Helper.DISPLAY_DIGITS_INFO)
            Log.d("infoItem!!.direction","${infoItem!!.direction}")
            if(isBNS){
                tvTxStatusIcon.setImageResource(R.drawable.bns_transaction)
                tvTxStatus.text = context!!.getString(R.string.tx_status_sent)
                if (displayAmount > 0.toString()) {
                    tvAmount.text =
                        context!!.getString(R.string.tx_list_amount_negative, displayAmount)
                    Log.d("Beldex", "Transaction list issue  value of amount - $displayAmount")
                    tvAmount.setTextColor(
                        ContextCompat.getColor(
                            context!!,
                            R.color.wallet_send_button
                        )
                    )
                }
            } else {
                if (infoItem!!.direction === TransactionInfo.Direction.Direction_Out) {
                    tvTxStatus.text = context!!.getString(R.string.tx_status_sent)
                    tvTxStatusIcon.setImageResource(R.drawable.ic_wallet_send_button)
                    if (displayAmount > 0.toString()) {
                        tvAmount.text =
                            context!!.getString(R.string.tx_list_amount_negative, displayAmount)
                        Log.d("Beldex", "Transaction list issue  value of amount - $displayAmount")
                        tvAmount.setTextColor(
                            ContextCompat.getColor(
                                context!!,
                                R.color.wallet_send_button
                            )
                        )
                    }
                } else {
                    tvTxStatus.text = context!!.getString(R.string.tx_status_received)
                    tvTxStatusIcon.setImageResource(R.drawable.ic_wallet_receive_button)
                    if (displayAmount > 0.toString()) {
                        tvAmount.text =
                            context!!.getString(R.string.tx_list_amount_positive, displayAmount)
                        Log.d("Beldex", "Transaction list issue  value of amount + $displayAmount")
                        tvAmount.setTextColor(
                            ContextCompat.getColor(
                                context!!,
                                R.color.wallet_receive_button
                            )
                        )
                    }
                }
            }
            txId.text = infoItem!!.hash
            //SteveJosephh21
            if(txId.text.isNotEmpty()){
                txId.setOnClickListener {
                    try {
                        val url = "${BuildConfig.EXPLORER_URL}/tx/${txId.text}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context!!.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Can't open URL", Toast.LENGTH_LONG).show()
                    }
                }
            }
            when {
                infoItem!!.isFailed -> {
                    tvTxBlockHeight.text = context!!.getString(R.string.tx_failed)
                }
                infoItem!!.isPending -> {
                    tvTxBlockHeight.text = context!!.getString(R.string.tx_pending)
                }
                else -> {
                    //tvTxBlockHeight.text ="ok"
                    tvTxBlockHeight.text = infoItem!!.blockheight.toString()
                }
            }
            tvDateTimeHead.text = getDateTime(infoItem!!.timestamp)

            if (infoItem!!.fee > 0) {
                val fee: String = Helper.getDisplayAmount(infoItem!!.fee, Helper.DISPLAY_DIGITS_INFO)
                tvFee.text = context!!.getString(R.string.tx_list_fee, fee)
                tvFee.visibility = View.VISIBLE
                tvFeeTitle.visibility = View.VISIBLE
            } else {
                tvFee.text = ""
                tvFee.visibility = View.GONE
                tvFeeTitle.visibility = View.GONE
            }
            if (infoItem!!.isFailed) {
                tvAmount.text = context!!.getString(R.string.tx_list_amount_failed, displayAmount)
                tvFee.text = context!!.getString(R.string.tx_list_failed_text)
                tvFee.visibility = View.VISIBLE
                tvFeeTitle.visibility = View.VISIBLE
                setTxColour(failedColour)
                //pbConfirmations.setVisibility(View.GONE)
                //tvConfirmations.visibility = View.GONE
            } else if (infoItem!!.isPending) {
                setTxColour(pendingColour)
                //pbConfirmations.setVisibility(View.GONE)
                //pbConfirmations.setIndeterminate(true)
                //pbConfirmations.setVisibility(View.VISIBLE)
                //tvConfirmations.visibility = View.GONE
            } else if (infoItem!!.direction === TransactionInfo.Direction.Direction_In) {
                setTxColour(inboundColour)
                if (!infoItem!!.isConfirmed) {
                    //pbConfirmations.setVisibility(View.VISIBLE)
                    val confirmations = infoItem!!.confirmations.toInt()
                    //pbConfirmations.setProgressCompat(confirmations, true)
                    val confCount = confirmations.toString()
                    //tvConfirmations.text = confCount
                   /* if (confCount.length == 1) // we only have space for character in the progress circle
                    {
                        //tvConfirmations.visibility = View.VISIBLE else tvConfirmations.visibility =View.GONE
                    }*/
                } /*else {
                    //pbConfirmations.setVisibility(View.GONE)
                    //tvConfirmations.visibility = View.GONE
                }*/
            } else {
                setTxColour(outboundColour)
                //pbConfirmations.setVisibility(View.GONE)
                //tvConfirmations.visibility = View.GONE
            }
            /*var tag: String? = null
            var info = ""
            if (infoItem!!.addressIndex !== 0 && infoItem!!.direction === TransactionInfo.Direction.Direction_In) tag =
                infoItem!!.displayLabel
            if (userNotes.note.isEmpty()) {
                if (!infoItem!!.paymentId.equals("0000000000000000")) {
                    info = infoItem!!.paymentId
                }
            } else {
                info = userNotes.note
            }
            if (tag == null) {
                //tvPaymentId.text = info
            } else {
                val label = Html.fromHtml(
                    context!!.getString(
                        R.string.tx_details_notes,
                        Integer.toHexString(
                            ContextCompat.getColor(
                                context!!,
                                R.color.text
                            ) and 0xFFFFFF
                        ),
                        Integer.toHexString(
                            ContextCompat.getColor(
                                context!!,
                                R.color.text
                            ) and 0xFFFFFF
                        ),
                        tag, if (info.isEmpty()) "" else "&nbsp; $info"
                    )
                )
                //tvPaymentId.text = label
            }*/
            tvDateTime.text = getDateTime(infoItem!!.timestamp)
        }

        /*init{
            //ivTxType = itemView.findViewById(R.id.ivTxType)
            //tvPaymentId = itemView.findViewById(R.id.tx_paymentid)
            //pbConfirmations = itemView.findViewById(R.id.pbConfirmations)
            //pbConfirmations.setMax(TransactionInfo.CONFIRMATION)
            //tvConfirmations = itemView.findViewById(R.id.tvConfirmations)
        }*/
    }

    fun updateList(list: ArrayList<TransactionInfo>) {
        infoItems = list
        notifyDataSetChanged()
    }

    fun updateList(){
        notifyDataSetChanged()
    }
}
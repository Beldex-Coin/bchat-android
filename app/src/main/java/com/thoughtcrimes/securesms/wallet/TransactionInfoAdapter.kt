package com.thoughtcrimes.securesms.wallet

import android.content.Context
import android.provider.Settings.Global.getString
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.thoughtcrimes.securesms.data.Crypto
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.util.Helper
import io.beldex.bchat.R
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TransactionInfoAdapter(context: Context?, listener: OnInteractionListener?)  :
    RecyclerView.Adapter<TransactionInfoAdapter.ViewHolder>() {
    private val DATETIME_FORMATTER = SimpleDateFormat("dd-MM-yyyy HH:mm")

    private var outboundColour = 0
    private var inboundColour = 0
    private var pendingColour = 0
    private var failedColour = 0

    interface OnInteractionListener {
        fun onInteraction(view: View?, item: TransactionInfo?)
    }

    private var infoItems: ArrayList<TransactionInfo>? = null
    private var listener: OnInteractionListener? = null

    private var context: Context? = null

    init{
        this.context =context
        inboundColour = ContextCompat.getColor(context!!, R.color.tx_plus)
        outboundColour = ContextCompat.getColor(context, R.color.tx_minus)
        pendingColour = ContextCompat.getColor(context, R.color.tx_pending)
        failedColour = ContextCompat.getColor(context, R.color.tx_failed)
        infoItems = ArrayList()
        this.listener = listener
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        //var ivTxType: ImageView
        private var tvAmount: TextView = itemView.findViewById(R.id.transaction_amount)
        private var tvFee: TextView = itemView.findViewById(R.id.transaction_fee)
        private var tvAddress: TextView = itemView.findViewById(R.id.transaction_recipient_address)
        private var txId: TextView = itemView.findViewById(R.id.transaction_id)
        private var tvTxBlockHeight: TextView = itemView.findViewById(R.id.transaction_height)
        private var tvTxStatus: TextView = itemView.findViewById(R.id.transaction_status)
        private var tvTxStatusIcon: ImageView = itemView.findViewById(R.id.transaction_status_icon)
        private var tvDateTimeHead: TextView = itemView.findViewById(R.id.transaction_date_and_time_head)

        //var tvPaymentId: TextView
        private var tvDateTime: TextView = itemView.findViewById(R.id.transaction_date_and_time)

        //val pbConfirmations: CircularProgressIndicator
        //var tvConfirmations: TextView
        private var infoItem: TransactionInfo? = null
        private fun getDateTime(time: Long): String {
            return DATETIME_FORMATTER.format(Date(time * 1000))
        }

        private fun setTxColour(clr: Int) {
            tvAmount.setTextColor(clr)
        }

        fun bind(position: Int) {
            infoItem = infoItems!!.get(position)
            itemView.transitionName = context!!.getString(R.string.tx_item_transition_name, infoItem!!.hash)
            val userNotes = UserNotes(infoItem!!.notes)
            if (userNotes.xmrtoKey != null) {
                val crypto: Crypto? = Crypto.withSymbol(userNotes.xmrtoCurrency)
                if (crypto != null) {
                    //ivTxType.setImageResource(crypto.getIconEnabledId())
                    //ivTxType.visibility = View.VISIBLE
                } else { // otherwirse pretend we don't know it's a shift
                    //ivTxType.visibility = View.GONE
                }
            } else {
                //ivTxType.visibility = View.GONE
            }
            val displayAmount: String = Helper.getDisplayAmount(infoItem!!.amount, Helper.DISPLAY_DIGITS_INFO)
            Log.d("infoItem!!.direction","${infoItem!!.direction}")
            if (infoItem!!.direction === TransactionInfo.Direction.Direction_Out) {
                tvTxStatus.text = context!!.getString(R.string.tx_status_sent)
                tvTxStatusIcon.setImageResource(R.drawable.ic_wallet_send_button)
                tvAmount.text = context!!.getString(R.string.tx_list_amount_negative, displayAmount)
            } else {
                tvTxStatus.text = context!!.getString(R.string.tx_status_received)
                tvTxStatusIcon.setImageResource(R.drawable.ic_wallet_receive_button)
                tvAmount.text = context!!.getString(R.string.tx_list_amount_positive, displayAmount)
            }

            tvAddress.text = infoItem!!.address
            txId.text = infoItem!!.hash
            if (infoItem!!.isFailed) {
                tvTxBlockHeight.text = context!!.getString(R.string.tx_failed)
            } else if (infoItem!!.isPending) {
                tvTxBlockHeight.text = context!!.getString(R.string.tx_pending)
            } else {
                tvTxBlockHeight.text = "" + infoItem!!.blockheight
            }
            tvDateTimeHead.text = getDateTime(infoItem!!.timestamp)

            if (infoItem!!.fee > 0) {
                val fee: String = Helper.getDisplayAmount(infoItem!!.fee, Helper.DISPLAY_DIGITS_INFO)
                tvFee.text = context!!.getString(R.string.tx_list_fee, fee)
                tvFee.visibility = View.VISIBLE
            } else {
                tvFee.text = ""
                tvFee.visibility = View.GONE
            }
            if (infoItem!!.isFailed) {
                tvAmount.setText(context!!.getString(R.string.tx_list_amount_failed, displayAmount))
                tvFee.setText(context!!.getString(R.string.tx_list_failed_text))
                tvFee.visibility = View.VISIBLE
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
                    if (confCount.length == 1) // we only have space for character in the progress circle
                    {
                        //tvConfirmations.visibility = View.VISIBLE else tvConfirmations.visibility =View.GONE
                    }
                } else {
                    //pbConfirmations.setVisibility(View.GONE)
                    //tvConfirmations.visibility = View.GONE
                }
            } else {
                setTxColour(outboundColour)
                //pbConfirmations.setVisibility(View.GONE)
                //tvConfirmations.visibility = View.GONE
            }
            var tag: String? = null
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
            }
            tvDateTime.text = getDateTime(infoItem!!.timestamp)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (listener != null) {
                val position = adapterPosition // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    listener!!.onInteraction(view, infoItems!!.get(position))
                }
            }
        }

        init{
            //ivTxType = itemView.findViewById(R.id.ivTxType)
            //tvPaymentId = itemView.findViewById(R.id.tx_paymentid)
            //pbConfirmations = itemView.findViewById(R.id.pbConfirmations)
            //pbConfirmations.setMax(TransactionInfo.CONFIRMATION)
            //tvConfirmations = itemView.findViewById(R.id.tvConfirmations)
        }
    }
}
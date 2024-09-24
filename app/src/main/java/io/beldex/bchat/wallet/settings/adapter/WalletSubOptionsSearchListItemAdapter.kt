package io.beldex.bchat.wallet.settings.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.beldex.bchat.R

class WalletSubOptionsSearchListItemAdapter internal constructor(context: Context?, data: List<String>, indexPosition:Int, option:Int) :
    RecyclerView.Adapter<WalletSubOptionsSearchListItemAdapter.ViewHolder>() {
    private var mData: List<String> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null
    private val contextVal = context
    private var rowIndex=indexPosition
    private var option=option

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.wallet_sub_options_list_item, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mData[position]
        holder.itemTextView.text = item
        if(rowIndex==position) {
            val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, contextVal!!.resources.displayMetrics)
            holder.itemCardView.radius=radius
            holder.itemCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    contextVal!!,
                    R.color.wallet_sub_options_selected_list_item_color
                )
            )
            holder.itemTextView.setTextColor(ContextCompat.getColor(contextVal!!, R.color.text))
            holder.itemCardView.elevation = 5F
        }else{
            holder.itemCardView.setCardBackgroundColor(ContextCompat.getColor(contextVal!!, R.color.transparent))
            holder.itemTextView.setTextColor(ContextCompat.getColor(contextVal!!, R.color.wallet_sub_options_list_item_color))
            holder.itemCardView.elevation = 0F
        }
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData.size
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var itemTextView: TextView = itemView.findViewById(R.id.itemTitle)
        var itemCardView: CardView = itemView.findViewById(R.id.itemCardView)
        override fun onClick(view: View?) {
            if (mClickListener != null) {
                mClickListener!!.onItemClicks(view, adapterPosition,option)
                rowIndex = adapterPosition
                notifyDataSetChanged()
            }
        }


        init {
            itemView.setOnClickListener(this)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): String {
        return mData[id]
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        mClickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClicks(view: View?, position: Int, option:Int)
    }

    fun updateList(list: List<String>) {
        mData = list
        notifyDataSetChanged()
    }

}
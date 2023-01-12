package com.thoughtcrimes.securesms.drawer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.beldex.bchat.R

class NavigationRVAdapter(private var items: ArrayList<NavigationItemModel>, private var currentPos: Int) :RecyclerView.Adapter<NavigationRVAdapter.NavigationItemViewHolder>() {

    private lateinit var context: Context

    class NavigationItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NavigationItemViewHolder {
        context = parent.context
        val navItem:View = LayoutInflater.from(context).inflate(R.layout.row_nav_drawer, parent, false)
        return NavigationItemViewHolder(navItem)
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: NavigationItemViewHolder, position: Int) {
        // To highlight the selected item, show different background color
        if (position == currentPos) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        holder.itemView.findViewById<ImageView>(R.id.navigation_icon)

        //holder.itemView.findViewById<ImageView>(R.id.navigation_icon).setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        //holder.itemView.findViewById<TextView>(R.id.navigation_title).setTextColor(Color.WHITE)
        //val font = ResourcesCompat.getFont(context, R.font.mycustomfont)
        //holder.itemView.navigation_text.typeface = font
        //holder.itemView.navigation_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.toFloat())

        holder.itemView.findViewById<TextView>(R.id.navigation_title).text = items[position].title

        holder.itemView.findViewById<ImageView>(R.id.navigation_icon).setImageResource(items[position].icon)
        
        holder.itemView.findViewById<ImageView>(R.id.navigation_SubIcon).setImageResource(items[position].subIcon)
    }

}
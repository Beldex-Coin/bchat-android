package io.beldex.bchat.contacts

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.RequestManager


class SelectContactsAdapter(
    private val context: Context,
    private val glide: RequestManager
) : RecyclerView.Adapter<SelectContactsAdapter.ViewHolder>() {

    val selectedMembers = mutableSetOf<String>()
    var members = listOf<String>()

    class ViewHolder(val view: UserView) : RecyclerView.ViewHolder(view)

    var selectionChangedListener: OnSelectionChangedListener? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // Use hashCode of the member string as stable ID
        return members[position].hashCode().toLong()
    }

    override fun getItemCount(): Int = members.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = UserView(context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bindView(holder, position)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.contains(Payload.MEMBER_CLICKED)) {
            val member = members[position]
            holder.view.toggleCheckbox(selectedMembers.contains(member))
        } else {
            bindView(holder, position)
        }
    }

    private fun bindView(holder: ViewHolder, position: Int) {
        val member = members[position]
        val isSelected = selectedMembers.contains(member)

        // Avoid reloading Glide if already loaded
        val currentTag = holder.view.tag as? String
        if (currentTag != member) {
            val recipient = Recipient.from(context, Address.fromSerialized(member), false)
            holder.view.bind(recipient, glide, UserView.ActionIndicator.Tick, isSelected)
            holder.view.tag = member
        } else {
            holder.view.toggleCheckbox(isSelected)
        }

        holder.view.setOnClickListener {
            onMemberClick(member)
        }
    }

    private fun onMemberClick(member: String) {
        if (selectedMembers.contains(member)) {
            selectedMembers.remove(member)
        } else {
            selectedMembers.add(member)
        }
        val index = members.indexOf(member)
        notifyItemChanged(index, Payload.MEMBER_CLICKED)
        selectionChangedListener?.onSelectionChanged(selectedMembers.size)
    }

    fun updateList(newList: List<String>) {
        val diffResult = DiffUtil.calculateDiff(ContactsDiffCallback(members, newList))
        members = newList
        diffResult.dispatchUpdatesTo(this)
    }

    interface OnSelectionChangedListener {
        fun onSelectionChanged(selectedCount: Int)
    }

    enum class Payload {
        MEMBER_CLICKED
    }

    class ContactsDiffCallback(
        private val oldList: List<String>,
        private val newList: List<String>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

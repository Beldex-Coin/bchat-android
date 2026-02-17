package io.beldex.bchat.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.databinding.ContactSelectionListDividerBinding
import com.bumptech.glide.RequestManager

class ContactSelectionListAdapter(
    private val context: Context,
    private val multiSelect: Boolean
) : ListAdapter<ContactSelectionListItem, RecyclerView.ViewHolder>(DiffCallback()) {
    lateinit var glide : RequestManager
    val selectedContacts=mutableSetOf<Recipient>()
    var contactClickListener : ContactClickListener?=null

    private object ViewType {
        const val Contact=0
        const val Divider=1
    }

    class UserViewHolder(val view : UserView) : RecyclerView.ViewHolder(view)
    class DividerViewHolder(
        private val binding : ContactSelectionListDividerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item : ContactSelectionListItem.Header) {
            with(binding) {
                label.text=item.name
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position : Int) : Long {
        return when (val item=currentList[position]) {
            is ContactSelectionListItem.Contact ->
                item.recipient.address.serialize().hashCode().toLong()

            is ContactSelectionListItem.Header ->
                item.name.hashCode().toLong()
        }
    }

    override fun getItemCount() : Int=currentList.size

    override fun onViewRecycled(holder : RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is UserViewHolder) {
            holder.view.unbind()
        }
    }

    override fun getItemViewType(position : Int) : Int {
        return when (currentList[position]) {
            is ContactSelectionListItem.Header -> ViewType.Divider
            else -> ViewType.Contact
        }
    }

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : RecyclerView.ViewHolder {
        return if (viewType == ViewType.Contact) {
            UserViewHolder(UserView(context))
        } else {
            DividerViewHolder(
                ContactSelectionListDividerBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(viewHolder : RecyclerView.ViewHolder, position : Int) {
        val item=currentList[position]
        if (viewHolder is UserViewHolder) {
            item as ContactSelectionListItem.Contact
            viewHolder.view.setOnClickListener { contactClickListener?.onContactClick(item.recipient) }
            val isSelected=selectedContacts.contains(item.recipient)
            viewHolder.view.bind(
                item.recipient,
                glide,
                if (multiSelect) UserView.ActionIndicator.Tick else UserView.ActionIndicator.None,
                isSelected
            )
        } else if (viewHolder is DividerViewHolder) {
            viewHolder.bind(item as ContactSelectionListItem.Header)
        }
    }

    fun onContactClick(recipient : Recipient) {
        if (selectedContacts.contains(recipient)) {
            selectedContacts.remove(recipient)
            contactClickListener?.onContactDeselected(recipient)
        } else if (multiSelect || selectedContacts.isEmpty()) {
            selectedContacts.add(recipient)
            contactClickListener?.onContactSelected(recipient)
        }
        val index=currentList.indexOfFirst {
            when (it) {
                is ContactSelectionListItem.Header -> false
                is ContactSelectionListItem.Contact -> it.recipient == recipient
            }
        }
        notifyItemChanged(index)
    }


    class DiffCallback : DiffUtil.ItemCallback<ContactSelectionListItem>() {

        override fun areItemsTheSame(
            oldItem : ContactSelectionListItem,
            newItem : ContactSelectionListItem
        ) : Boolean {
            return when {
                oldItem is ContactSelectionListItem.Contact &&
                        newItem is ContactSelectionListItem.Contact ->
                    oldItem.recipient.address == newItem.recipient.address

                oldItem is ContactSelectionListItem.Header &&
                        newItem is ContactSelectionListItem.Header ->
                    oldItem.name == newItem.name

                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem : ContactSelectionListItem,
            newItem : ContactSelectionListItem
        ) : Boolean {
            return oldItem == newItem
        }
    }
}

    interface ContactClickListener {
        fun onContactClick(contact : Recipient)
        fun onContactSelected(contact : Recipient)
        fun onContactDeselected(contact : Recipient)
    }
package com.thoughtcrimes.securesms.contacts.blocked

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.mms.GlideApp
import io.beldex.bchat.R
import io.beldex.bchat.databinding.BlockedContactLayoutBinding

class BlockedContactsAdapter(private val context: BlockedContactsActivity) : ListAdapter<Recipient, BlockedContactsAdapter.ViewHolder>(RecipientDiffer()) {

    class RecipientDiffer: DiffUtil.ItemCallback<Recipient>() {
        override fun areItemsTheSame(oldItem: Recipient, newItem: Recipient) = oldItem === newItem
        override fun areContentsTheSame(oldItem: Recipient, newItem: Recipient) = oldItem == newItem
    }

    private val selectedItems = mutableListOf<Recipient>()

    fun getSelectedItems() = selectedItems

    fun setSelectedItems() {
        selectedItems.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.blocked_contact_layout, parent, false)
        return ViewHolder(itemView)
    }

    private fun toggleSelection(recipient: Recipient, isSelected: Boolean, position: Int) {
        if (isSelected) {
            selectedItems -= recipient
        } else {
            selectedItems += recipient
        }
        notifyItemChanged(position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipient = getItem(position)
        val isSelected = recipient in selectedItems
        holder.bind(recipient, isSelected, context) {
            toggleSelection(recipient, isSelected, position)
        }
        holder.binding.unblockButtonBlockedList.setOnClickListener {
            context.unblockSingleUser(recipient,context)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.profilePictureView.root.recycle()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val glide = GlideApp.with(itemView)
        val binding = BlockedContactLayoutBinding.bind(itemView)

        fun bind(recipient: Recipient, isSelected: Boolean, context: BlockedContactsActivity,toggleSelection: () -> Unit) {
            val address = recipient.address.serialize()
            binding.recipientName.text =  if (recipient.isGroupRecipient) recipient.name else getUserDisplayName(address,context)
            with (binding.profilePictureView.root) {
                glide = this@ViewHolder.glide
                update(recipient)
            }
            if(!context.selectedAll){
                binding.unblockButtonBlockedList.visibility = View.GONE
                binding.selectButton.visibility = View.VISIBLE
                binding.root.setOnClickListener { toggleSelection() }
                binding.selectButton.isSelected = isSelected
            }else{
                binding.unblockButtonBlockedList.visibility = View.VISIBLE
                binding.selectButton.visibility = View.GONE
                binding.selectButton.isSelected = false
            }
            binding.selectButton.isClickable = !context.selectedAll

        }
        fun getUserDisplayName(publicKey: String, context: Context): String {
            val contact = DatabaseComponent.get(context).bchatContactDatabase()
                .getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
        }
    }

}
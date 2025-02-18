package io.beldex.bchat.preferences

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide;
import io.beldex.bchat.R
import io.beldex.bchat.databinding.NotificationContentItemSelectableBinding

class NotificationContentRadioOptionAdapter(
    private var selectedOptionPosition: Int = 0,
    private val onClickListener: (NotificationContentRadioOption) -> Unit
) : ListAdapter<NotificationContentRadioOption, NotificationContentRadioOptionAdapter.ViewHolder>(RadioOptionDiffer()) {

    class RadioOptionDiffer : DiffUtil.ItemCallback<NotificationContentRadioOption>() {
        override fun areItemsTheSame(oldItem: NotificationContentRadioOption, newItem: NotificationContentRadioOption) =
            oldItem.title == newItem.title

        override fun areContentsTheSame(oldItem: NotificationContentRadioOption, newItem: NotificationContentRadioOption) =
            oldItem.value == newItem.value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.notification_content_item_selectable, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = getItem(position)
        val isSelected = position == selectedOptionPosition
        holder.bind(option, isSelected) {
            onClickListener(it)
            selectedOptionPosition = position
            notifyItemRangeChanged(0, itemCount)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val glide = Glide.with(itemView)
        val binding = NotificationContentItemSelectableBinding.bind(itemView)

        fun bind(option: NotificationContentRadioOption, isSelected: Boolean, toggleSelection: (NotificationContentRadioOption) -> Unit) {
            binding.titleTextView.text = option.title
            binding.root.setOnClickListener { toggleSelection(option) }
            binding.selectButton.isSelected = isSelected
            binding.backgroundContainer.isSelected = isSelected
        }
    }

}

data class NotificationContentRadioOption(
    val value: String,
    val title: String,
)
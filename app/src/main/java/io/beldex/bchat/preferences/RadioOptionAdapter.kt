package io.beldex.bchat.preferences

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ItemSelectableBinding

class RadioOptionAdapter(
    private var selectedOptionPosition: Int = 0,
    private val onClickListener: (RadioOption) -> Unit
) : ListAdapter<RadioOption, RadioOptionAdapter.ViewHolder>(RadioOptionDiffer()) {

    class RadioOptionDiffer : DiffUtil.ItemCallback<RadioOption>() {
        override fun areItemsTheSame(oldItem: RadioOption, newItem: RadioOption) =
            oldItem.title == newItem.title

        override fun areContentsTheSame(oldItem: RadioOption, newItem: RadioOption) =
            oldItem.value == newItem.value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_selectable, parent, false)
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
        val binding = ItemSelectableBinding.bind(itemView)

        fun bind(option: RadioOption, isSelected: Boolean, toggleSelection: (RadioOption) -> Unit) {
            binding.titleTextView.text = option.title
            binding.subtitleTextView.text = option.subtitle
            binding.root.setOnClickListener { toggleSelection(option) }
            binding.selectButton.isSelected = isSelected
        }
    }

}

data class RadioOption(
    val value: String,
    val title: String,
    val subtitle: String,
)
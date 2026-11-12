package io.beldex.bchat.contacts

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewUserBinding
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getLocalNumber
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getProfileName
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.conversation.v2.utilities.MentionManagerUtilities
import io.beldex.bchat.dependencies.DatabaseComponent
import com.bumptech.glide.RequestManager
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter

class UserView : LinearLayout {
    private lateinit var binding: ViewUserBinding
    var openGroupThreadID: Long = -1 // FIXME: This is a bit ugly

    enum class ActionIndicator {
        None,
        Menu,
        Tick
    }

    // region Lifecycle
    constructor(context: Context) : super(context) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setUpViewHierarchy()
    }

    private fun setUpViewHierarchy() {
        binding = ViewUserBinding.inflate(LayoutInflater.from(context), this, true)
    }
    // endregion

    // region Updating
    fun bind(user: Recipient, glide: RequestManager, actionIndicator: ActionIndicator, isSelected: Boolean = false, groupAdmin : String? = null) {
        fun getUserDisplayName(publicKey : String) : String {
            return if (publicKey == getLocalNumber(context)) {
                getProfileName(context) ?: publicKey
            } else {
                val contact=DatabaseComponent.get(context).bchatContactDatabase()
                    .getContactWithBchatID(publicKey)
                contact?.displayName(Contact.ContactContext.REGULAR, context) ?: publicKey
            }
        }

        val threadID = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(user)
        MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(threadID, context) // FIXME: This is a bad place to do this
        val address = user.address.serialize()

        binding.profilePictureView.root.glide = glide
        binding.profilePictureView.root.update(user)
        binding.actionIndicatorImageView.setImageResource(R.drawable.ic_baseline_edit_group_name)
        binding.nameTextView.text = if (user.isGroupRecipient) user.name?.capitalizeFirstLetter() else getUserDisplayName(address).capitalizeFirstLetter()
        when (actionIndicator) {
            ActionIndicator.None -> {
                binding.actionIndicatorImageView.visibility = View.GONE
            }
            ActionIndicator.Menu -> {
                binding.actionIndicatorImageView.visibility = View.VISIBLE
                binding.actionIndicatorImageView.setImageResource(R.drawable.ic_block_request_background)
            }
            ActionIndicator.Tick -> {
                binding.userViewLayout.background = context.getDrawable(
                    if (isSelected) R.drawable.contact_list_background_selected else R.drawable.contact_list_background
                )
                binding.actionIndicatorImageView.visibility = View.VISIBLE
                binding.actionIndicatorImageView.setImageResource(
                        if (isSelected) R.drawable.ic_checkedbox else R.drawable.ic_checkbox
                )
            }
        }
        if(groupAdmin == address){
            binding.actionIndicatorImageView.visibility = View.VISIBLE
            binding.actionIndicatorImageView.setImageResource(R.drawable.ic_group_admin)
        }
    }

    fun toggleCheckbox(isSelected: Boolean = false) {
        binding.userViewLayout.background = context.getDrawable(
            if (isSelected) R.drawable.contact_list_background_selected else R.drawable.contact_list_background
        )
        binding.actionIndicatorImageView.visibility = View.VISIBLE
        binding.actionIndicatorImageView.setImageResource(
            if (isSelected) R.drawable.ic_checkedbox else R.drawable.ic_checkbox
        )
    }

    fun applyEditGroupStyle(isAdmin: Boolean, removable: Boolean) {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        binding.userViewLayout.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bordered_card_background)
        (binding.userViewLayout.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let {
            it.marginStart = dp(13); it.marginEnd = dp(13); it.topMargin = dp(8)
            binding.userViewLayout.layoutParams = it
        }
        binding.contentView.setPadding(dp(12), dp(10), dp(12), dp(10))
        (binding.contentView.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let {
            it.marginEnd = 0
            binding.contentView.layoutParams = it
        }
        binding.nameTextView.textSize = 14f
        binding.nameTextView.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.disappear_icon_color))
        binding.nameTextView.typeface = androidx.core.content.res.ResourcesCompat.getFont(
            context, if (isAdmin) R.font.open_sans_semi_bold else R.font.open_sans_regular
        )
        binding.actionIndicatorImageView.visibility = View.GONE
        if (removable) {
            binding.actionIndicatorImageView.visibility = View.VISIBLE
            binding.actionIndicatorImageView.setImageResource(R.drawable.ic_edit_group_remove_member)
            binding.actionIndicatorImageView.layoutParams = binding.actionIndicatorImageView.layoutParams.apply {
                width = dp(32); height = dp(32)
            }
        }
    }

    fun applyAddMemberStyle(isSelected: Boolean) {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        binding.userViewLayout.background = androidx.core.content.ContextCompat.getDrawable(
            context, if (isSelected) R.drawable.bg_add_member_row_selected else R.drawable.bordered_card_background
        )
        (binding.userViewLayout.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let {
            it.marginStart = 0; it.marginEnd = 0; it.topMargin = dp(8)
            binding.userViewLayout.layoutParams = it
        }
        binding.contentView.setPadding(dp(12), dp(12), dp(15), dp(12))
        (binding.contentView.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let {
            it.marginEnd = 0
            binding.contentView.layoutParams = it
        }
        binding.profilePictureView.root.layoutParams = binding.profilePictureView.root.layoutParams.apply {
            width = dp(32); height = dp(32)
        }
        (binding.nameTextView.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let {
            it.marginStart = dp(10)
            binding.nameTextView.layoutParams = it
        }
        binding.nameTextView.textSize = 14f
        binding.nameTextView.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.disappear_icon_color))
        binding.nameTextView.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.open_sans_semi_bold)
        binding.actionIndicatorImageView.layoutParams = binding.actionIndicatorImageView.layoutParams.apply {
            width = dp(18); height = dp(18)
        }
        binding.actionIndicatorImageView.visibility = View.VISIBLE
        binding.actionIndicatorImageView.setImageResource(
            if (isSelected) R.drawable.ic_add_member_check_on else R.drawable.ic_add_member_check_off
        )
    }

    fun unbind() {
        binding.profilePictureView.root.recycle()
    }
    // endregion
}

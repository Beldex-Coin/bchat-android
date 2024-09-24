package io.beldex.bchat.contacts

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewUserBinding
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.conversation.v2.utilities.MentionManagerUtilities
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.mms.GlideRequests

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
    fun bind(user: Recipient, glide: io.beldex.bchat.mms.GlideRequests, actionIndicator: ActionIndicator, isSelected: Boolean = false) {
        fun getUserDisplayName(publicKey: String): String {
            val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
        }
        val threadID = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(user)
        MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(threadID, context) // FIXME: This is a bad place to do this
        val address = user.address.serialize()
        unbind()
        binding.profilePictureView.root.glide = glide
        binding.profilePictureView.root.update(user)
        binding.actionIndicatorImageView.setImageResource(R.drawable.ic_baseline_edit_24)
        binding.nameTextView.text = if (user.isGroupRecipient) user.name else getUserDisplayName(address)
        when (actionIndicator) {
            ActionIndicator.None -> {
                binding.actionIndicatorImageView.visibility = View.GONE
            }
            ActionIndicator.Menu -> {
                binding.actionIndicatorImageView.visibility = View.VISIBLE
                binding.actionIndicatorImageView.setImageResource(R.drawable.ic_person_remove_from_group)
            }
            ActionIndicator.Tick -> {
                binding.userViewLayout.background = context.getDrawable(R.drawable.contact_list_background)
                binding.actionIndicatorImageView.visibility = View.VISIBLE
                binding.actionIndicatorImageView.setImageResource(
                        if (isSelected) R.drawable.ic_checkedbox else R.drawable.ic_checkbox
                )
            }
        }
    }

    fun toggleCheckbox(isSelected: Boolean = false) {
        binding.actionIndicatorImageView.visibility = View.VISIBLE
        binding.actionIndicatorImageView.setImageResource(
            if (isSelected) R.drawable.ic_checkedbox else R.drawable.ic_checkbox
        )
    }

    fun unbind() {
        binding.profilePictureView.root.recycle()
    }
    // endregion
}

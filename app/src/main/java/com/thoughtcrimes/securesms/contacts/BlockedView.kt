package com.thoughtcrimes.securesms.contacts

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionManagerUtilities
import com.thoughtcrimes.securesms.database.RecipientDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.mms.GlideRequests
import io.beldex.bchat.databinding.ActivityBlockedViewBinding

class BlockedView : LinearLayout {

    private lateinit var binding: ActivityBlockedViewBinding

    var recipientDatabase: RecipientDatabase? = null

    var thread: ThreadRecord? = null

    // region Lifecycle
    constructor(context: Context) : super(context) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setUpViewHierarchy()
    }

    private fun setUpViewHierarchy() {
        binding = ActivityBlockedViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    // region Updating
    fun bind(recipient: Recipient, glide: GlideRequests) {
        fun getUserDisplayName(publicKey: String): String {
            val contact = DatabaseComponent.get(context).bchatContactDatabase()
                .getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
        }

        val threadID =
            DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(recipient)
        MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(
            threadID,
            context
        ) // FIXME: This is a bad place to do this
        val address = recipient.address.serialize()

        binding.profilePictureView.root.glide = glide
        binding.profilePictureView.root.update(recipient)
        binding.nameTextView.text =
            if (recipient.isGroupRecipient) recipient.name else getUserDisplayName(address)
    }

    fun unblockButton(): Button {
        return binding.unblockButtonBlockedList
    }

}
// endregion
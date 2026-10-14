package io.beldex.bchat.conversation.v2.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.PopupBody
import io.beldex.bchat.compose_utils.boldedMessage
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.dependencies.DatabaseComponent

/** Shown upon sending a message to a user that's blocked. */
class BlockedDialog(private val recipient: Recipient) : BaseDialog() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val bchatID = recipient.address.toString()
        val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(bchatID)
        val name = contact?.displayName(Contact.ContactContext.REGULAR, context) ?: bchatID
        PopupBody(
            title = stringResource(R.string.dialog_blocked_title, name),
            message = boldedMessage(stringResource(R.string.dialog_blocked_explanation, name), name),
            cancelText = stringResource(R.string.cancel),
            onCancel = { dismiss() },
            confirmText = stringResource(R.string.ConversationActivity_unblock),
            onConfirm = { unblock() }
        )
    }

    private fun unblock() {
        DatabaseComponent.get(requireContext()).recipientDatabase().setBlocked(recipient, false)
        dismiss()
    }
}

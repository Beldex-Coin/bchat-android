package io.beldex.bchat.conversation.v2.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.jobs.AttachmentDownloadJob
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.utilities.recipients.Recipient
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.PopupBody
import io.beldex.bchat.compose_utils.boldedMessage
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.database.BchatContactDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import javax.inject.Inject

/** Shown when receiving media from a contact for the first time, to confirm that
 * they are to be trusted and files sent by them are to be downloaded. */
@AndroidEntryPoint
class DownloadDialog(private val recipient: Recipient) : BaseDialog() {

    @Inject lateinit var contactDB: BchatContactDatabase

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val bchatID = recipient.address.toString()
        val contact = contactDB.getContactWithBchatID(bchatID)
        val name = contact?.displayName(Contact.ContactContext.REGULAR, context) ?: bchatID
        PopupBody(
            icon = painterResource(R.drawable.ic_download_media),
            title = stringResource(R.string.dialog_download_title, name),
            message = boldedMessage(stringResource(R.string.dialog_download_explanation, name), name),
            cancelText = stringResource(R.string.cancel),
            onCancel = { dismiss() },
            confirmText = stringResource(R.string.audio_view__download_accessibility_description),
            onConfirm = { trust() }
        )
    }

    private fun trust() {
        val bchatID = recipient.address.toString()
        val contact = contactDB.getContactWithBchatID(bchatID) ?: return
        val threadID = DatabaseComponent.get(requireContext()).threadDatabase().getThreadIdIfExistsFor(recipient)
        contactDB.setContactIsTrusted(contact, true, threadID)
        JobQueue.shared.resumePendingJobs(AttachmentDownloadJob.KEY)
        dismiss()
    }
}

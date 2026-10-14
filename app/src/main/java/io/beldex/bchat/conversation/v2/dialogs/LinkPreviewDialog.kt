package io.beldex.bchat.conversation.v2.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.PopupBody
import io.beldex.bchat.conversation.v2.utilities.BaseDialog

/** Shown the first time the user inputs a URL that could generate a link preview, to
 * let them know that Bchat offers the ability to send and receive link previews. */
class LinkPreviewDialog(private val onEnabled: () -> Unit) : BaseDialog() {

    @Composable
    override fun Content() {
        PopupBody(
            icon = painterResource(R.drawable.ic_link_preview),
            title = stringResource(R.string.dialog_link_preview_title),
            message = AnnotatedString(stringResource(R.string.dialog_link_preview_explanation)),
            cancelText = stringResource(R.string.cancel),
            onCancel = { dismiss() },
            confirmText = stringResource(R.string.dialog_link_preview_enable_button_title),
            onConfirm = { enable() }
        )
    }

    private fun enable() {
        TextSecurePreferences.setLinkPreviewsEnabled(requireContext(), true)
        dismiss()
        onEnabled()
    }
}

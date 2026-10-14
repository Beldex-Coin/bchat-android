package io.beldex.bchat.conversation.v2.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.PopupBody
import io.beldex.bchat.conversation.v2.utilities.BaseDialog

/** Shown if the user is about to send their recovery phrase to someone. */
class SendSeedDialog(private val proceed: (() -> Unit)? = null) : BaseDialog() {

    @Composable
    override fun Content() {
        PopupBody(
            icon = painterResource(R.drawable.ic_seed_warning),
            title = stringResource(R.string.dialog_send_seed_title),
            message = AnnotatedString(stringResource(R.string.dialog_send_seed_explanation)),
            cancelText = stringResource(R.string.cancel),
            onCancel = { dismiss() },
            confirmText = stringResource(R.string.conversation_activity__send),
            onConfirm = { send() },
            destructive = true
        )
    }

    private fun send() {
        proceed?.invoke()
        dismiss()
    }
}

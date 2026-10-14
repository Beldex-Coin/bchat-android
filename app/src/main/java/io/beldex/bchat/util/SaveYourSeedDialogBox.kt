package io.beldex.bchat.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.PopupBody
import io.beldex.bchat.conversation.v2.utilities.BaseDialog

class SaveYourSeedDialogBox(
    private val showSeed: () -> Unit,
) : BaseDialog() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        PopupBody(
            icon = painterResource(R.drawable.ic_seed_warning),
            title = stringResource(R.string.save_your_seed),
            message = AnnotatedString(stringResource(R.string.save_your_seed_message)),
            confirmText = stringResource(R.string.ok),
            onConfirm = {
                TextSecurePreferences.setCopiedSeed(context, true)
                showSeed()
                dismiss()
            }
        )
    }
}

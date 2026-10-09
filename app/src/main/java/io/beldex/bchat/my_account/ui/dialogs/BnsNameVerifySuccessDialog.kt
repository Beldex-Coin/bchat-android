package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.SuccessPopup

@Composable
fun BNSNameVerifySuccessDialog(onDismiss: () -> Unit) {
    var isButtonEnabled by remember { mutableStateOf(true) }
    SuccessPopup(
        title = stringResource(id = R.string.bns_linked_successfully),
        okEnabled = isButtonEnabled,
        onDismiss = {
            if (isButtonEnabled) {
                isButtonEnabled = false
                onDismiss()
            }
        }
    )
}

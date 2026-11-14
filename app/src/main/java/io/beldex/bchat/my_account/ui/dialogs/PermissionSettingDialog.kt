package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.beldex.bchat.R
import androidx.compose.ui.text.AnnotatedString
import io.beldex.bchat.compose_utils.IconConfirmDialog

@Composable
fun PermissionSettingDialog(
    message: String,
    onDismissRequest: () -> Unit,
    gotoSettings: () -> Unit
) {
    IconConfirmDialog(
        icon = painterResource(id = R.drawable.ic_permission_setting),
        title = stringResource(id = R.string.Permissions_permission_required),
        message = AnnotatedString(message),
        cancelText = stringResource(id = R.string.cancel),
        confirmText = stringResource(id = R.string.activity_settings_title),
        onCancel = onDismissRequest,
        onConfirm = gotoSettings
    )
}

@Preview
@Composable
fun PermissionSettingDialogPreview() {
    PermissionSettingDialog(
        message = stringResource(R.string.library_access_required),
        onDismissRequest = {},
        gotoSettings = {}
    )
}
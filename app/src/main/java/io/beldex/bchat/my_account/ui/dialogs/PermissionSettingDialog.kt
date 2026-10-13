package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun PermissionSettingDialog(
    message: String,
    onDismissRequest: () -> Unit,
    gotoSettings: () -> Unit
) {
    io.beldex.bchat.compose_utils.IconConfirmDialog(
        icon = painterResource(id = R.drawable.ic_permission_setting),
        title = stringResource(id = R.string.Permissions_permission_required),
        message = androidx.compose.ui.text.AnnotatedString(message),
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
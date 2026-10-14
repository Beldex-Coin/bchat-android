package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.ui.text.AnnotatedString
import io.beldex.bchat.compose_utils.PopupBody
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.R

@Composable
fun RequestBlockConfirmationDialog(
    title: String,
    message: String,
    actionTitle: String,
    onConfirmation: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val isAccept = title == stringResource(R.string.accept_request)
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismissRequest
    ) {
        PopupBody(
            title = title,
            message = AnnotatedString(message),
            cancelText = stringResource(id = R.string.cancel),
            onCancel = onDismissRequest,
            confirmText = actionTitle,
            onConfirm = onConfirmation,
            destructive = !isAccept
        )
    }
}

@Composable
fun IgnoreRequestDialog(
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismissRequest
    ) {
        PopupBody(
            title = stringResource(id = R.string.activity_message_requests_title),
            message = AnnotatedString(stringResource(id = R.string.block_or_delete_request)),
            cancelText = stringResource(id = R.string.RecipientPreferenceActivity_block),
            onCancel = onBlock,
            confirmText = stringResource(id = R.string.delete),
            onConfirm = onDelete,
            destructive = true
        )
    }
}

@Preview
@Composable
private fun RequestConfirmationDialogPreview() {
    RequestBlockConfirmationDialog(
        title = "Message Request",
        message = "Are you sure you want to Block this user?",
        actionTitle = "Yes",
        onConfirmation = {},
        onDismissRequest = {}
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun IgnoreRequestDialogPreview() {
    BChatPreviewContainer {
        IgnoreRequestDialog(
            onDismissRequest = {},
            onBlock = {},
            onDelete = {}
        )
    }
}
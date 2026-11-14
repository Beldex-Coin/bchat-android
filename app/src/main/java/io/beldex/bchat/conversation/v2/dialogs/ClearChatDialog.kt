package io.beldex.bchat.conversation.v2.dialogs

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.R
import androidx.compose.ui.text.AnnotatedString
import io.beldex.bchat.compose_utils.IconConfirmDialog

@Composable
fun ClearChatDialog(
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconConfirmDialog(
        icon = painterResource(id = R.drawable.ic_pop_clear_chat),
        title = stringResource(id = R.string.clear_chat),
        message = AnnotatedString(stringResource(id = R.string.clear_chat_confirmation)),
        cancelText = stringResource(id = R.string.cancel),
        confirmText = stringResource(id = R.string.clear),
        onCancel = onCancel,
        onConfirm = onAccept,
        destructive = true
    )
}

//@Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_NO,
//    showSystemUi = true,
//    showBackground = true
//)
//@Composable
//fun ClearChatDialogPreview() {
//    BChatTheme {
//        ClearChatDialog(
//            onAccept = {},
//            onCancel = {}
//        )
//    }
//}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
    showBackground = true
)
@Composable
fun ClearChatDialogPreviewDark() {
    BChatTheme {
        /*ClearChatDialog(
            onAccept = {},
            onCancel = {},
                threadRecord = ThreadRecord,
            thread = {},
        )*/
    }
}
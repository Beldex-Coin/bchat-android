package io.beldex.bchat.conversation.v2.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun ClearChatDialog(
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    io.beldex.bchat.compose_utils.IconConfirmDialog(
        icon = painterResource(id = R.drawable.ic_clear_conversation),
        title = stringResource(id = R.string.clear_chat),
        message = androidx.compose.ui.text.AnnotatedString(stringResource(id = R.string.clear_chat_confirmation)),
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
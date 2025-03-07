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
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onCancel
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Image(
                painterResource(id = R.drawable.ic_clear_conversation),
                contentDescription = "",
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.appColors.iconBackground,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.clear_chat),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.clear_chat_confirmation),
                style = MaterialTheme.typography.bodyMedium.copy(

                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.negativeGreenButton
                    ),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(width = 0.5.dp, color = MaterialTheme.appColors.negativeGreenButtonBorder)
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            color = MaterialTheme.appColors.negativeGreenButtonText
                        ),
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.negativeRedButtonBorder
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.clear),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }
            }
        }
    }
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
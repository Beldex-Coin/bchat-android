package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.DialogCancelButton
import io.beldex.bchat.compose_utils.DialogConfirmButton
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono

@Composable
fun DeleteChatConfirmationDialog(
        message: String,
        onConfirmation: () -> Unit,
        onDismissRequest: () -> Unit
) {
    DialogContainer(
            dismissOnBackPress=true,
            dismissOnClickOutside=true,
            onDismissRequest=onDismissRequest
    ) {
        Column(
                modifier=Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 18.dp)
        ) {
            Text(
                text=stringResource(id=R.string.delete_conversation),
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier=Modifier.height(14.dp))

            Text(
                text=message,
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier=Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogCancelButton(
                    text = stringResource(id = R.string.no),
                    onClick = onDismissRequest,
                    fontFamily = RobotoMono,
                    modifier = Modifier.weight(1f)
                )
                DialogConfirmButton(
                    text = stringResource(id = R.string.yes),
                    onClick = onConfirmation,
                    destructive = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Preview
@Composable
fun DeleteChatConfirmationDialogPreview() {
    DeleteChatConfirmationDialog(
            message = "",
            onConfirmation = {},
            onDismissRequest={},
    )
}

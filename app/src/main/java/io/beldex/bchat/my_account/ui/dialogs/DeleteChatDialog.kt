package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

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
                horizontalAlignment=Alignment.CenterHorizontally,
                modifier=Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
        ) {
            Text(
                text=stringResource(id=R.string.delete_conversation),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.secondaryContentColor,
                    fontWeight = FontWeight(700),
                    fontSize = 16.sp
                )
            )

            Spacer(modifier=Modifier.height(16.dp))

            Text(
                text=message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.titleTextColor,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp
                ),
                textAlign=TextAlign.Center
            )

            Spacer(modifier=Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.no),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.appColors.onboardingHeadlineColor,
                        fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .clickable { onDismissRequest() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Text(
                    text = stringResource(id = R.string.yes),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.appColors.negativeRedButtonBorder,
                        fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .clickable { onConfirmation() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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

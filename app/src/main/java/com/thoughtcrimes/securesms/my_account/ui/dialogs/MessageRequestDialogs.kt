package com.thoughtcrimes.securesms.my_account.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.R

@Composable
fun RequestBlockConfirmationDialog(
    message: String,
    actionTitle: String,
    onConfirmation: () -> Unit,
    onDismissRequest: () -> Unit
) {
    DialogContainer(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.message_request),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.primaryButtonColor,
                    fontWeight = FontWeight(800)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.contactCardBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.cancelButtonTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onConfirmation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.primaryButtonColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = actionTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun IgnoreRequestDialog(
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    DialogContainer(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.message_request),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.primaryButtonColor,
                    fontWeight = FontWeight(800)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.block_or_delete_request),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.contactCardBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.recipient_preferences__block),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.contactCardBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.delete),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RequestConfirmationDialogPreview() {
    RequestBlockConfirmationDialog(
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
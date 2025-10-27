package io.beldex.bchat.conversation.v2.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter

@Composable
fun UnblockUserDialog(
    title: String,
    message: String,
    positiveButtonTitle: String,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    negativeButtonTitle: String = stringResource(id = R.string.cancel)
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
            Text(
                text = title.capitalizeFirstLetter(),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.secondaryContentColor,
                    fontWeight = FontWeight(700),
                    fontSize = 16.sp
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.titleTextColor,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.negativeGreenButton
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.appColors.negativeGreenButtonBorder),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.negativeGreenButtonText,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(positiveButtonTitle == stringResource(id = R.string.decline)) MaterialTheme.appColors.negativeRedButtonBorder else MaterialTheme.appColors.negativeGreenButtonBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = positiveButtonTitle,
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
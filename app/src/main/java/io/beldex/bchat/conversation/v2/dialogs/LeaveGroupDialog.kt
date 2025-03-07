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

@Composable
fun LeaveGroupDialog(title : String,
                     message : String,
                     positiveButtonTitle : String,
                     onLeave : () -> Unit,
                     onCancel : () -> Unit,
                     modifier : Modifier=Modifier,
                     negativeButtonTitle : String=stringResource(id=R.string.cancel)
) {
    DialogContainer(
            dismissOnBackPress=true,
            dismissOnClickOutside=true,
            onDismissRequest=onCancel
    ) {
        Column(
                horizontalAlignment=Alignment.CenterHorizontally,
                modifier=modifier
                        .fillMaxWidth()
                        .padding(16.dp)
        ) {
            Text(
                    text=title,
                    style=MaterialTheme.typography.titleMedium.copy(
                            fontWeight=FontWeight(700),
                            fontSize=16.sp,
                            color=MaterialTheme.appColors.secondaryContentColor
                    ),
            )

            Spacer(modifier=Modifier.height(8.dp))

            Text(
                    text=message,
                    style=MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.appColors.editTextColor,
                        fontWeight = FontWeight(400),
                        fontSize = 14.sp
                    ),
                    textAlign=TextAlign.Center
            )

            Spacer(modifier=Modifier.height(16.dp))

            Row(
                    horizontalArrangement=Arrangement.spacedBy(16.dp),
                    modifier=Modifier
                            .fillMaxWidth()
            ) {
                Button(
                    onClick=onCancel,
                    shape=RoundedCornerShape(12.dp),
                    colors=ButtonDefaults.buttonColors(
                            containerColor=MaterialTheme.appColors.negativeGreenButton
                    ),
                    modifier=Modifier.weight(1f),
                    border = BorderStroke(
                    width = 0.5.dp,
                        color = MaterialTheme.appColors.negativeGreenButtonBorder
                    ),
                ) {
                    Text(
                            text=stringResource(id=R.string.cancel),
                            style=MaterialTheme.typography.bodyMedium.copy(
                                fontWeight=FontWeight(400),
                                color=MaterialTheme.appColors.negativeGreenButtonText,
                                fontSize = 14.sp
                            ),
                            modifier=Modifier.padding(
                                    vertical=8.dp
                            )
                    )
                }

                Button(
                        onClick=onLeave,
                        shape=RoundedCornerShape(12.dp),
                        colors=ButtonDefaults.buttonColors(
                                containerColor=MaterialTheme.appColors.negativeRedButtonBorder
                        ),
                        modifier=Modifier.weight(1f)
                ) {
                    Text(
                            text=positiveButtonTitle,
                            style=MaterialTheme.typography.bodyMedium.copy(
                                fontWeight=FontWeight(400),
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            modifier=Modifier.padding(
                                    vertical=8.dp
                            )
                    )
                }
            }
        }
    }
}
package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
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
fun WalletSyncingDialog(
        onDismissRequest : () -> Unit,
        exit : () -> Unit
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
            Image(
                    painter=painterResource(id=R.drawable.wallet_syncing),
                    contentDescription="",
                    colorFilter=ColorFilter.tint(MaterialTheme.appColors.walletSyncingIcon),
                    modifier=Modifier
                            .padding(8.dp)

            )

            Spacer(modifier=Modifier.height(16.dp))

            Text(
                    text=stringResource(id=R.string.wallet_syncing_alert_title),
                    style=MaterialTheme.typography.titleMedium.copy(
                        color=MaterialTheme.appColors.primaryButtonColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight(700),
                    )
            )

            Spacer(modifier=Modifier.height(8.dp))

            Text(
                    text=stringResource(id=R.string.wallet_syncing_alert_message),
                    style=MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.appColors.editTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400)
                    ),
                    textAlign=TextAlign.Center,
            )

            Spacer(modifier=Modifier.height(16.dp))

            Row(
                    modifier=Modifier
                            .fillMaxWidth()
            ) {
                Button(
                    onClick=onDismissRequest,
                    shape = RoundedCornerShape(12.dp),
                    colors=ButtonDefaults.buttonColors(
                            containerColor=MaterialTheme.appColors.negativeGreenButton
                    ),
                    modifier=Modifier
                            .weight(1f),
                    border = BorderStroke(width = 0.5.dp, color = MaterialTheme.appColors.negativeGreenButtonBorder)
                ) {
                    Text(
                            text=stringResource(id=R.string.cancel),
                            style=MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.appColors.negativeGreenButtonText,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp
                            )
                    )
                }

                Spacer(modifier=Modifier.width(16.dp))

                Button(
                    onClick=exit,
                    shape = RoundedCornerShape(12.dp),
                    colors=ButtonDefaults.buttonColors(
                            containerColor=MaterialTheme.appColors.primaryButtonColor
                    ),
                    modifier=Modifier
                            .weight(1f)
                ) {
                    Text(
                            text=stringResource(id=R.string.exit),
                            style=MaterialTheme.typography.bodyMedium.copy(
                                color=Color.White,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp,
                            )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun WalletSyncingPreview() {
    WalletSyncingDialog(
            onDismissRequest={},
            exit={}
    )
}

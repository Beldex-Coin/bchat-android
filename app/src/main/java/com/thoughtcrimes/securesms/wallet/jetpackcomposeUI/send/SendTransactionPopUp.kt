package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.data.TxData
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.model.Wallet
import io.beldex.bchat.R


@Composable
fun TransactionConfirmPopUp(
        onDismiss: () -> Unit, pendingTransaction: PendingTransaction,
        txData: TxData?,
) {

    val beldexAddress by remember {
        mutableStateOf(txData?.destinationAddress.toString())
    }
    val transactionFee by remember {
        mutableStateOf(Wallet.getDisplayAmount(pendingTransaction.fee))
    }
    val transferAmount by remember {
        mutableStateOf(Wallet.getDisplayAmount(pendingTransaction.fee))
    }
    DialogContainer(
            onDismissRequest = onDismiss,
    ) {

        Box(contentAlignment = Alignment.Center, modifier = Modifier) {
            OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)) {
                    Text(text = stringResource(id = R.string.confirm_sending), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier
                            .padding(10.dp)
                            .background(color = MaterialTheme.appColors.settingsCardBackground, shape = RoundedCornerShape(12.dp))

                    ) {
                        Text(text = stringResource(id = R.string.amount), style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.textColor), modifier = Modifier.padding(10.dp))

                        Divider(modifier = Modifier
                                .height(70.dp)
                                .width(1.dp), color = MaterialTheme.appColors.dividerColor)
                        Row {


                            Text(text = transferAmount, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 22.sp, fontWeight = FontWeight(700), color = MaterialTheme.appColors.textColor), modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                                    .weight(1f), textAlign = TextAlign.Start)
                            Image(painter = painterResource(id = R.drawable.slide_with_pay_coin), contentDescription = "", modifier = Modifier.padding(10.dp))
                        }
                    }

                    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center, modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .background(color = MaterialTheme.appColors.settingsCardBackground, shape = RoundedCornerShape(12.dp))) {
                        Text(text = stringResource(id = R.string.address), style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight(400), color = MaterialTheme.appColors.textColor), modifier = Modifier.padding(10.dp))
                        Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 10.dp), colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.appColors.popUpAddressBackground,
                        )) {
                            Text(text = beldexAddress, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.textColor, fontSize = 13.sp, fontWeight = FontWeight(400)))
                        }

                        Text(buildAnnotatedString {
                            withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 12.sp)) {
                                append("Fee: ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 12.sp)) {
                                append(transactionFee)
                            }
                        }, modifier = Modifier.padding(10.dp)

                        )

                    }

                    Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)) {
                        Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.secondaryButtonColor), modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.cancel), style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.ok), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TransactionSuccessPopup(onDismiss: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.sent))
    val isPlaying by remember {
        mutableStateOf(true)
    }
    // for speed
    val speed by remember {
        mutableStateOf(1f)
    }
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever, isPlaying = isPlaying, speed = speed, restartOnPlay = false)
    DialogContainer(
            onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)) {
                LottieAnimation(composition, progress, modifier = Modifier
                        .size(120.dp)
                        .padding(20.dp)
                        .align(Alignment.CenterHorizontally))

                Text(text = stringResource(id = R.string.transaction_completed), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.padding(10.dp))

                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier
                        .height(50.dp)
                        .width(150.dp)) {
                    Text(text = stringResource(id = R.string.ok), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                }
            }

        }
    }

}

@Composable
fun TransactionLoadingPopUp(onDismiss: () -> Unit) {
    DialogContainer(
            onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)) {
                Image(painter = painterResource(id = R.drawable.slide_with_pay_coin), contentDescription = "", modifier = Modifier.padding(10.dp))

                Text(text = stringResource(id = R.string.initiating_transaction), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.padding(10.dp))

                Text(text = stringResource(id = R.string.transaction_progress_attention), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(400), color = MaterialTheme.appColors.textColor), modifier = Modifier.padding(10.dp))

            }
        }
    }

}


@Composable
fun TransactionFailedPopUp(onDismiss: () -> Unit) {
    val errorString by remember {
        mutableStateOf("")
    }
    DialogContainer(
            onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)) {
                Text(text = stringResource(id = R.string.dialog_title_send_failed), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.padding(10.dp))

                Text(text = errorString, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(400), color = MaterialTheme.appColors.textColor), modifier = Modifier.padding(10.dp))

                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier
                        .height(50.dp)
                        .width(150.dp)) {
                    Text(text = stringResource(id = R.string.ok), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                }

            }
        }
    }

}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TransactionFailedPopUpPreview() {
    BChatTheme {
        TransactionFailedPopUp(onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TransactionFailedPopUpLightPreview() {
    BChatTheme() {
        TransactionFailedPopUp(onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TransactionLoadingPopUpPreview() {
    BChatTheme {
        TransactionLoadingPopUp(onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TransactionLoadingPopUpLightPreview() {
    BChatTheme() {
        TransactionLoadingPopUp(onDismiss = {})
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TransactionSuccessPopupPreview() {
    BChatTheme {
        TransactionSuccessPopup(onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TransactionSuccessPopupLightPreview() {
    BChatTheme() {
        TransactionSuccessPopup(onDismiss = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ConfirmPopUpPreview() {
    BChatTheme {
        /*TransactionConfirmPopUp(
                onDismiss = {}
        )*/
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun ConfirmPopUpLightPreview() {/* BChatTheme() {
         TransactionConfirmPopUp(
                 onDismiss = {}
         )
     }*/
}

package io.beldex.bchat.wallet.jetpackcomposeUI.send

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.data.TxData
import io.beldex.bchat.model.PendingTransaction
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.wallet.send.SendFragment
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun TransactionConfirmPopUp(
        onDismiss: () -> Unit, pendingTransaction: PendingTransaction,
        txData: TxData?, onClick: () -> Unit
) {

    val beldexAddress by remember {
        mutableStateOf(txData?.destinationAddress.toString())
    }
    val transactionFee by remember {
        mutableStateOf(Wallet.getDisplayAmount(pendingTransaction.fee))
    }
    val transferAmount by remember {
        mutableStateOf(Wallet.getDisplayAmount(pendingTransaction.amount))
    }
    var isButtonEnabled by remember {
        mutableStateOf(true)
    }
    val scope = rememberCoroutineScope()

    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {

        Box(contentAlignment = Alignment.Center, modifier = Modifier) {
            OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier =Modifier
                        .fillMaxWidth()
                        .padding(horizontal=16.dp, vertical=16.dp)) {
                    Text(text = stringResource(id = R.string.confirm_sending), style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.secondaryContentColor), textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier =Modifier
                            .padding(horizontal=10.dp)
                            .background(color=MaterialTheme.appColors.settingsCardBackground, shape=RoundedCornerShape(12.dp))

                    ) {
                        Text(text = stringResource(id = R.string.send_amount_title), style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.textColor), modifier = Modifier.padding(vertical = 5.dp, horizontal = 10.dp))

                        Divider(modifier =Modifier
                                .height(70.dp)
                                .width(1.dp), color = MaterialTheme.appColors.dividerColor)
                        Row {


                            Text(text = transferAmount, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 22.sp, fontWeight = FontWeight(700), color = MaterialTheme.appColors.textColor), modifier =Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                                    .weight(1f), textAlign = TextAlign.Start)
                            Image(painter = painterResource(id = R.drawable.slide_with_pay_coin), contentDescription = "", modifier = Modifier.padding(10.dp))
                        }
                    }

                    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center, modifier =Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .background(color=MaterialTheme.appColors.settingsCardBackground, shape=RoundedCornerShape(12.dp))) {
                        Text(text = stringResource(id = R.string.address), style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight(400), color = MaterialTheme.appColors.textColor), modifier = Modifier.padding(vertical = 5.dp,horizontal = 20.dp))
                        Card(modifier =Modifier
                                .fillMaxWidth()
                                .padding(start=20.dp, top=10.dp, end=20.dp), colors = CardDefaults.cardColors(
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
                        }, modifier = Modifier.padding(vertical = 10.dp,horizontal = 20.dp)

                        )

                    }

                    Row(modifier =Modifier
                            .fillMaxWidth()
                            .padding(10.dp)) {
                        Button(onClick = {onDismiss() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.appColors.negativeGreenButtonBorder),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.negativeGreenButton), modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.cancel), style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp
                            ))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(onClick = {
                            if (isButtonEnabled) {
                                isButtonEnabled=false
                                onClick()
                            }
                        }, enabled = isButtonEnabled,
                            shape = RoundedCornerShape(12.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.appColors.primaryButtonColor), modifier=Modifier.weight(1f)) {
                            Text(text=stringResource(id=R.string.ok), style=MaterialTheme.typography.bodyMedium.copy(color = Color.White,
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TransactionSuccessPopup(onDismiss: () -> Unit) {
    var isButtonEnabled by remember {
        mutableStateOf(true)
    }
    val context = LocalContext.current
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(if(isDarkTheme) R.raw.sent else R.raw.sent_light))
    val isPlaying by remember {
        mutableStateOf(true)
    }
    // for speed
    val speed by remember {
        mutableFloatStateOf(1f)
    }
    val scope = rememberCoroutineScope()
    val progress by animateLottieCompositionAsState(composition, isPlaying = isPlaying, speed = speed, restartOnPlay = false)
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground), elevation=CardDefaults.cardElevation(defaultElevation=4.dp), modifier=Modifier.fillMaxWidth()) {
            Column(horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center, modifier=Modifier
                    .fillMaxWidth()
                    .padding(10.dp)) {
                LottieAnimation(composition, progress, modifier=Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally))

                Text(
                        text=stringResource(id=R.string.transaction_successful),
                        textAlign=TextAlign.Center,
                        style=MaterialTheme.typography.titleMedium.copy(
                                fontSize=16.sp,
                                fontWeight=FontWeight(800),
                                color=MaterialTheme.appColors.primaryButtonColor),
                )

                Button(onClick={
                    scope.launch {
                        if (isButtonEnabled) {
                            isButtonEnabled=false
                            onDismiss()
                        }
                    }
                }, enabled = isButtonEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.appColors.primaryButtonColor), modifier=Modifier
                        .padding(vertical=16.dp)
                        .height(50.dp)
                        .width(150.dp)) {
                    Text(text=stringResource(id=R.string.ok), style=MaterialTheme.typography.bodyMedium.copy(color = Color.White,
                        fontWeight = FontWeight(400),
                        fontSize = 14.sp))
                }
            }

        }
    }

}

@Composable
fun TransactionLoadingPopUp(onDismiss: () -> Unit) {
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier =Modifier
                    .fillMaxWidth()
                    .padding(15.dp)) {
                Box(
                    contentAlignment= Alignment.Center,
                    modifier =Modifier
                            .size(55.dp)
                            .background(color=MaterialTheme.appColors.circularProgressBarBackground, shape=CircleShape),
                ){
                    CircularProgressIndicator(
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.appColors.primaryButtonColor,
                        strokeWidth = 2.dp
                    )
                }

                Text(text = stringResource(id = R.string.initiating_transaction), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.padding(10.dp))

                Text(text = stringResource(id = R.string.transaction_progress_attention), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight(400), color = MaterialTheme.appColors.editTextColor), modifier = Modifier.padding(10.dp))

            }
        }
    }

}


@Composable
fun TransactionFailedPopUp(onDismiss: () -> Unit, errorString: String) {

    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {

        OutlinedCard(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_title_send_failed),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight(800),
                        color = MaterialTheme.appColors.secondaryContentColor
                    ),
                    modifier = Modifier.padding(10.dp)
                )

                Text(
                    text = errorString,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400),
                        color = MaterialTheme.appColors.editTextColor
                    ),
                    modifier = Modifier.padding(10.dp)
                )

                Button(
                    onClick = { onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.primaryButtonColor
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .width(150.dp)) {
                    Text(text = stringResource(id = R.string.ok), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White,
                        fontWeight = FontWeight(400),
                        fontSize = 14.sp))
                }

            }
        }
    }

}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TransactionFailedPopUpPreview() {
    BChatTheme {
        TransactionFailedPopUp(onDismiss = {}, "")
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TransactionFailedPopUpLightPreview() {
    BChatTheme() {
        TransactionFailedPopUp(onDismiss = {},"")
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

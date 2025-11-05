package io.beldex.bchat.wallet.jetpackcomposeUI

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
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
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.R


@Composable
fun StatWalletInfo(modifier: Modifier) {

    val context = LocalContext.current
    val activity = context as? Activity ?: return
    var isChecked by remember {
        mutableStateOf(false)
    }
    var showLoader by remember {
        mutableStateOf(false)
    }
    if (showLoader) {
        WalletSetupLoadingPopUp(onDismiss = {
            showLoader = false

        })
    }

    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier =modifier
                .wrapContentHeight()
                .padding(10.dp)
                .clip(shape=RoundedCornerShape(10.dp))
                .background(color=MaterialTheme.appColors.walletInfoBackground)
                .border(border=BorderStroke(width=1.dp, MaterialTheme.appColors.walletInfoBackgroundBorder), shape=RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState())

        ) {

            Row(modifier = Modifier.padding(start = 20.dp, top = 40.dp, end = 20.dp, bottom = 10.dp)) {
                Image(painter = painterResource(id = R.drawable.ic_connected), contentDescription = "", modifier = Modifier.padding(5.dp))
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 14.sp)) {
                        append("You can ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.walletInfoHighlightColor, fontSize = 14.sp)) {
                        append("Send and Receive BDX ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 14.sp)) {
                        append("with the BChat integrated wallet.")
                    }
                }, modifier = Modifier.padding(start = 10.dp)

                )
            }

            Row(modifier = Modifier.padding(top = 20.dp, end = 20.dp, start = 20.dp, bottom = 10.dp)) {
                Image(painter = painterResource(id = R.drawable.ic_connected), contentDescription = "", modifier = Modifier.padding(5.dp))

                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 14.sp)) {
                        append("The '")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.walletInfoHighlightColor, fontSize = 14.sp)) {
                        append("Pay as you Chat")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 14.sp)) {
                        append("' feature is an easy-pay feature. You can send BDX to your friends right from the chat window.")
                    }
                }, modifier = Modifier.padding(start = 10.dp)

                )
            }
            Row(modifier = Modifier.padding(top = 20.dp, end = 20.dp, start = 20.dp, bottom = 10.dp)) {
                Image(painter = painterResource(id = R.drawable.ic_connected), contentDescription = "", modifier = Modifier.padding(5.dp))

                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 14.sp)) {
                        append("The BChat wallet is beta. Constant updates are released in newer versions of the app to enhance the integrated wallet functionality.")
                    }
                }, modifier = Modifier.padding(start = 10.dp)

                )
            }

            Row(modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                Image(painter = painterResource(id = R.drawable.ic_connected), contentDescription = "", modifier = Modifier.padding(5.dp))

                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.textColor, fontSize = 14.sp)) {
                        append("You can enable or disable the wallet using the Start wallet feature under")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.appColors.walletInfoHighlightColor, fontSize = 14.sp)) {
                        append(" Settings > Wallet settings.")
                    }
                }, modifier = Modifier.padding(start = 10.dp)

                )
            }

            Row(horizontalArrangement = Arrangement.Center, modifier =Modifier
                    .fillMaxWidth()
                    .padding(top=15.dp, start=20.dp, end=20.dp), verticalAlignment = Alignment.CenterVertically) {

                Text(text = "Yes, I Understand", style = MaterialTheme.typography.labelMedium, fontSize = 15.sp, fontWeight = FontWeight(400), textAlign = TextAlign.Center)

                Checkbox(checked = isChecked, onCheckedChange = { isChecked = it }, colors = CheckboxDefaults.colors(checkedColor = colorResource(id = R.color.button_green), uncheckedColor = MaterialTheme.appColors.textColor, checkmarkColor = colorResource(id = R.color.white)),
                    modifier = Modifier.scale(0.65f))
            }

            PrimaryButton(onClick = {
                TextSecurePreferences.setBooleanPreference(context, TextSecurePreferences.IS_WALLET_ACTIVE, true)
                showLoader = true
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    showLoader = false
                    restartHome(context, activity)
                }, 2000)
            }, modifier=Modifier
                    .fillMaxWidth()
                    .padding(start=16.dp, end=16.dp, bottom=24.dp),
                    shape=RoundedCornerShape(12.dp), enabled=isChecked) {
                Text(
                        text=context.getString(R.string.enable_wallet),
                        style= if(isChecked)BChatTypography.bodyLarge.copy(color=Color.White) else BChatTypography.bodyLarge.copy(color=MaterialTheme.appColors.disabledNextButtonColor),
                        modifier=Modifier.padding(8.dp))
            }

        }
    }
}

private fun restartHome(context: Context, activity: Activity) {
    val intent = Intent(context, HomeActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
    activity.startActivity(intent)
}


@Composable
fun WalletSetupLoadingPopUp(onDismiss: () -> Unit) {
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

                Text(text = stringResource(id = R.string.wallet_setup), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.padding(10.dp))

                Text(text = stringResource(id = R.string.wallet_setup_loading_content), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp, fontWeight = FontWeight(400), color = MaterialTheme.appColors.textColor), modifier = Modifier.padding(10.dp))

            }
        }
    }

}


@Preview
@Composable
fun StartWalletInfoPreview() {
    StatWalletInfo(modifier = Modifier.fillMaxSize())
}




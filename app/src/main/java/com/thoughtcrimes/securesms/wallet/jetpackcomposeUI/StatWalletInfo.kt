package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.wallet.WalletSetupLoadingBar
import com.thoughtcrimes.securesms.wallet.startwallet.StartWalletInfo
import io.beldex.bchat.R


@Composable
fun StatWalletInfo(modifier: Modifier) {

    val context = LocalContext.current
    val activity = context as? Activity ?: return
    var isChecked by remember {
        mutableStateOf(false)
    }

    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier
            .wrapContentHeight()
            .padding(10.dp)
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.appColors.walletInfoBackground)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    MaterialTheme.appColors.walletInfoBackgroundBorder
                ), shape = RoundedCornerShape(10.dp)
            )
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

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {

                Text(text = "Yes, I Understand", modifier = Modifier.padding(top = 15.dp, end = 5.dp), style = MaterialTheme.typography.labelMedium, fontSize = 15.sp)

                Checkbox(checked = isChecked, onCheckedChange = { isChecked = it }, colors = CheckboxDefaults.colors(checkedColor = colorResource(id = R.color.button_green), uncheckedColor = MaterialTheme.appColors.textColor, checkmarkColor = colorResource(id = R.color.white)))
            }

            PrimaryButton(onClick = {
                TextSecurePreferences.setBooleanPreference(context, TextSecurePreferences.IS_WALLET_ACTIVE, true)
                showProgress(context)
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    hideProgress(context)
                    restartHome(context, activity)
                }, 2000)
                Toast.makeText(context, "Enabled Wallet", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp), shape = RoundedCornerShape(16.dp), enabled = isChecked) {
                Text(text = context.getString(R.string.enable_wallet), style = BChatTypography.bodyLarge.copy(color = Color.White), modifier = Modifier.padding(8.dp))
            }

        }
    }
}

private fun restartHome(context: Context, activity: Activity) {
    val intent = Intent(context, HomeActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
    activity.startActivity(intent)
}

private fun showProgress(context: Context) {
    val activity = context as? AppCompatActivity ?: return
    WalletSetupLoadingBar().show(activity.supportFragmentManager, "wallet_setup_progressbar_tag")
}

private fun hideProgress(context: Context) {
    val activity = context as? AppCompatActivity ?: return
    val fragment = activity.supportFragmentManager.findFragmentByTag("wallet_setup_progressbar_tag") as WalletSetupLoadingBar
    val dialogFragment = DialogFragment()
    try {
        dialogFragment.dismiss()
    } catch (ex: IllegalStateException) {
        Log.e("Beldex", "IllegalStateException $ex")
    }
}


@Preview
@Composable
fun StartWalletInfoPreview() {
    StatWalletInfo(modifier = Modifier.fillMaxSize())
}




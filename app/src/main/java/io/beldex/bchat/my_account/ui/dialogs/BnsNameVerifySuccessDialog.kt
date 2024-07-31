package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.R
import kotlinx.coroutines.launch

@Composable
fun BNSNameVerifySuccessDialog(onDismiss: () -> Unit) {
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

        OutlinedCard(colors= CardDefaults.cardColors(containerColor= MaterialTheme.appColors.dialogBackground), elevation= CardDefaults.cardElevation(defaultElevation=4.dp), modifier= Modifier.fillMaxWidth()) {
            Column(horizontalAlignment= Alignment.CenterHorizontally, verticalArrangement= Arrangement.Center, modifier= Modifier
                .fillMaxWidth()
                .padding(10.dp)) {
                LottieAnimation(composition, progress, modifier= Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally))

                Text(
                    text= stringResource(id= R.string.bns_linked_successfully),
                    textAlign= TextAlign.Center,
                    style= MaterialTheme.typography.titleMedium.copy(
                        fontSize=16.sp,
                        fontWeight= FontWeight(800),
                        color= MaterialTheme.appColors.primaryButtonColor),
                )

                Button(onClick={
                    scope.launch {
                        if (isButtonEnabled) {
                            isButtonEnabled=false
                            onDismiss()
                        }
                    }
                }, enabled = isButtonEnabled,colors= ButtonDefaults.buttonColors(containerColor= MaterialTheme.appColors.primaryButtonColor), modifier= Modifier
                    .padding(vertical=16.dp)
                    .height(50.dp)
                    .width(150.dp)) {
                    Text(text= stringResource(id= R.string.ok), style= MaterialTheme.typography.bodyMedium.copy(color= Color.White))
                }
            }

        }
    }

}
package io.beldex.bchat.compose_utils

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.beldex.bchat.R

@Composable
fun DialogContainer(
    dismissOnBackPress : Boolean = false,
    dismissOnClickOutside : Boolean = false,
    onDismissRequest: () -> Unit,
    containerColor: Color = MaterialTheme.appColors.dialogBackground,
    wrapContentWidth: Boolean = false,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.appColors.dividerColor
            ),
            modifier = Modifier
                .then(
                    if (wrapContentWidth)
                        Modifier.wrapContentWidth().padding(horizontal = 16.dp)
                    else
                        Modifier.fillMaxWidth(if (isLandscape) 0.5f else 0.9f)
                )
                .heightIn(max = 560.dp)
                .widthIn(max = if (isLandscape) 320.dp else 400.dp)
        ) {
            content()
        }
    }
}

// Revamp_2026 success popup (Figma 7546:22102): approval badge, title, compact OK button.
@Composable
fun SuccessPopup(
    title: String,
    onDismiss: () -> Unit,
    okEnabled: Boolean = true
) {
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_success_approval),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                textAlign = TextAlign.Center,
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(17.dp))
            PrimaryButton(
                onClick = onDismiss,
                enabled = okEnabled,
                shape = notchedCornerShape(12.dp),
                containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
                modifier = Modifier
                    .width(158.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.ok),
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
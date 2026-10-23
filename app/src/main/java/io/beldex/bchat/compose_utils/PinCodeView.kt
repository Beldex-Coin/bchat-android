package io.beldex.bchat.compose_utils

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PinCodeView(
    modifier: Modifier = Modifier,
    pinLength: Int = 4,
    pin: String = "",
    isError: Boolean = false
) {

    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }

    BoxWithConstraints {
        val spacing =
            when {
                isLandscape && pinLength == 6 -> 2.dp
                isLandscape -> 4.dp
                pinLength == 6 -> 8.dp
                else -> 17.dp
            }

        val separatorWidth = if (pinLength == 6) 27.dp else 0.dp
        val boxSize =
            (
                    (maxWidth - spacing * (pinLength - 1) - separatorWidth)
                            / pinLength
                    ).coerceIn(
                    if (isLandscape) 24.dp else 36.dp,
                    if (isLandscape && pinLength == 6) 32.dp else if (isLandscape) 40.dp else if (pinLength == 6) 45.dp else 56.dp
                )
        val boxHeight = if (pinLength == 6 && !isLandscape) 46.dp else boxSize

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            repeat(pinLength) { i ->
                val value = if (pin.length - 1 >= i) pin[i].toString() else ""
                val pinCode = if (value.isNotEmpty()) "*" else ""
                val isActiveBox = i == pin.length && pin.isNotEmpty() && pin.length < pinLength

                if (pinLength == 6 && i == 3) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.width(separatorWidth)
                    ) {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.appColors.restoreDescColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isLandscape) 14.sp else 20.sp
                            )
                        )
                    }
                }

                if (i > 0 && !(pinLength == 6 && i == 3)) {
                    Spacer(modifier = Modifier.width(spacing))
                }

                Card(
                    shape = notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchSmall),
                    border = BorderStroke(
                        // Design: filled box stroke 1px, empty box stroke 2px.
                        width = if (value.isNotEmpty() || isActiveBox) 1.dp else 2.dp,
                        color = when {
                            isError -> MaterialTheme.appColors.negativeRedButtonBorder
                            value.isNotEmpty() -> MaterialTheme.appColors.primaryButtonColor
                            isActiveBox -> MaterialTheme.appColors.primaryButtonColor
                            else -> MaterialTheme.appColors.pinBoxInactiveBorder
                        }
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground
                    ),
                    modifier = Modifier
                        .width(boxSize)
                        .height(boxHeight)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val displayText = when {
                            pinCode.isNotEmpty() -> pinCode
                            isActiveBox && cursorVisible -> "|"
                            else -> ""
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = OpenSans,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActiveBox && pinCode.isEmpty())
                                    MaterialTheme.appColors.textColor
                                else
                                    MaterialTheme.appColors.editTextColor
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PinCodePreview() {
    PinCodeView(
        pin = "1234",
        modifier = Modifier
            .fillMaxWidth()
    )
}
package io.beldex.bchat.compose_utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
    pin: String = ""
) {

    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }

    BoxWithConstraints {
        val spacing = if (pinLength == 6) 6.dp else 12.dp

        val boxSize = (
                (maxWidth - spacing * (pinLength - 1) - if (pinLength == 6) 24.dp else 0.dp) / pinLength
                ).coerceIn(40.dp, 60.dp)

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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.appColors.restoreDescColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (i > 0 && !(pinLength == 6 && i == 3)) {
                    Spacer(modifier = Modifier.width(spacing))
                }

                Card(
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            value.isNotEmpty() -> MaterialTheme.appColors.primaryButtonColor
                            isActiveBox -> MaterialTheme.appColors.primaryButtonColor
                            else -> MaterialTheme.colorScheme.outline
                        }
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .size(boxSize)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(25)
                        )
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
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActiveBox && pinCode.isEmpty())
                                    MaterialTheme.appColors.textColor
                                else
                                    MaterialTheme.typography.titleMedium.color
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
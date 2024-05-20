package com.thoughtcrimes.securesms.compose_utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinCodeView(
    modifier: Modifier = Modifier,
    length: Int = 4,
    pin: String = "",
    boxSize: Dp = 64.dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = modifier
    ) {
        repeat(length) { i ->
            val value = if (pin.length - 1 >= i) pin[i].toString() else ""
            val pinCode: String
            if(value.isNotEmpty()){
                pinCode = when (value.length) {
                    1 -> {
                        "*"
                    }
                    2 -> {
                        "**"
                    }
                    3 -> {
                        "***"
                    }
                    else -> {
                        "****"
                    }
                }
            }else{
                pinCode = ""
            }
            Card(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (value.isNotEmpty())
                        MaterialTheme.appColors.primaryButtonColor
                    else
                        MaterialTheme.colorScheme.outline
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
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Text(
                        text = pinCode,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
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
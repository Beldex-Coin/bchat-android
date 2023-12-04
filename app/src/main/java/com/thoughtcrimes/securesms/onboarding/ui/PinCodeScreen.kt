package com.thoughtcrimes.securesms.onboarding.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.PinCodeView
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun PinCodeScreen() {
    var pin by remember {
        mutableStateOf("")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_password),
            contentDescription = "",
            modifier = Modifier
                .weight(0.2f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(0.2f)
        ) {
            PinCodeView(
                pin = pin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Text(
                text = "Enter your PIN",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.backgroundColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
        ) {
            var cellSize by remember {
                mutableStateOf(IntSize.Zero)
            }
            val density = LocalDensity.current
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                repeat(12) {
                    when (val index = it + 1) {
                        10 -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.appColors.editTextBackground
                                    )
                                ) {

                                }
                            }
                        }
                        11 -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.appColors.editTextBackground
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            pin += "0"
                                        }
                                ) {
                                    Text(
                                        text = "0",
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                        12 -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.appColors.editTextBackground
                                    ),
                                    modifier = Modifier
                                        .size(
                                            width = with(density) {
                                                cellSize.width.toDp()
                                            },
                                            height = with(density) {
                                                cellSize.height.toDp()
                                            }
                                        )
                                        .clickable {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.substring(0, pin.length - 1)
                                            }
                                        }
                                ) {
                                    Icon(
                                        Icons.Outlined.Backspace,
                                        contentDescription = "",
                                        tint = MaterialTheme.appColors.editTextColor,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.appColors.editTextBackground
                                    ),
                                    modifier = Modifier
                                        .onSizeChanged { size ->
                                            cellSize = size
                                        }
                                        .clickable {
                                            pin += "$index"
                                        }
                                ) {
                                    Text(
                                        text = "$index",
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PrimaryButton(
                onClick = {},
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun PinCodeScreenPreview() {
    BChatTheme {
        Scaffold {
            PinCodeScreen()
        }
    }
}
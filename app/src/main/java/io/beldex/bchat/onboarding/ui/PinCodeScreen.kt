package io.beldex.bchat.onboarding.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.PinCodeView
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

const val EXTRA_PIN_CODE_ACTION = "action"

data class PinCodeState(
    val step:PinCodeSteps = PinCodeSteps.OldPin,
    val stepTitle: String = "",
    val pin: String = "",
    val newPin: String = "",
    val reEnteredPin: String = ""
)

enum class PinCodeAction(val action: Int) {
    ChangePinCode(1),
    VerifyPinCode(2),
    CreatePinCode(3),
    VerifyWalletPin(4),
    CreateWalletPin(5),
    ChangeWalletPin(6)
}

sealed interface PinCodeEvents {
    data object Submit: PinCodeEvents
    data class PinCodeChanged(val pinCode: String): PinCodeEvents
    data object ResetPinCode: PinCodeEvents
}

@Composable
fun PinCodeScreen(
    state: PinCodeState,
    onEvent: (PinCodeEvents) -> Unit
) {
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT
    val pin by remember(state) {
        mutableStateOf(
            value = when (state.step) {
                PinCodeSteps.EnterPin -> {
                    state.newPin
                }
                PinCodeSteps.OldPin,
                PinCodeSteps.VerifyPin -> {
                    state.pin
                }
                PinCodeSteps.ReEnterPin -> {
                    state.reEnteredPin
                }
            }
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(id = if(isDarkTheme) R.drawable.ic_password_dark else R.drawable.ic_password_light),
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
            )

            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )

            Text(
                text = state.stepTitle,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
        ) {
            val density = LocalDensity.current
            val availableHeight = with(density) {
                constraints.maxHeight.toDp()
            }
            var buttonSize by remember {
                mutableStateOf(IntSize.Zero)
            }
            val buttonHeight = with(density) {
                buttonSize.height.toDp()
            }
            val heightLessSpacing = availableHeight - 96.dp - buttonHeight
            val cellHeight = (heightLessSpacing / 4).coerceAtMost(64.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.appColors.backgroundColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    )
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 32.dp,
                            vertical = 16.dp
                        )
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
                                            .height(cellHeight)
                                            .clickable {
                                                if (pin.length < 4)
                                                    onEvent(PinCodeEvents.PinCodeChanged(pin + "0"))
                                            }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                        ) {
                                            Text(
                                                text = "0",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
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
                                            .height(cellHeight)
                                            .clickable {
                                                if (pin.isNotEmpty()) {
                                                    onEvent(
                                                        PinCodeEvents.PinCodeChanged(
                                                            pin.substring(
                                                                0,
                                                                pin.length - 1
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.Backspace,
                                                contentDescription = "",
                                                tint = MaterialTheme.appColors.editTextColor
                                            )
                                        }
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
                                            .height(cellHeight)
                                            .clickable {
                                                if (pin.length < 4)
                                                    onEvent(PinCodeEvents.PinCodeChanged(pin + "$index"))
                                            }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                        ) {
                                            Text(
                                                text = "$index",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (state.step != PinCodeSteps.VerifyPin) {
                    PrimaryButton(
                        onClick = {
                            onEvent(PinCodeEvents.Submit)
                        },
                        enabled = pin.length == 4,
                        modifier = Modifier
                            .fillMaxWidth().
                            padding(
                                start = 16.dp,
                                end = 16.dp,
                            )
                            .align(Alignment.CenterHorizontally)
                            .onSizeChanged {
                                buttonSize = it
                            },
                        shape = RoundedCornerShape(12.dp),
                        disabledContainerColor = MaterialTheme.appColors.beldexAddressBackground
                    ) {
                        Text(
                            text = stringResource(id = R.string.next),
                            style = BChatTypography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = if(pin.length == 4) Color.White else MaterialTheme.appColors.disabledNextButtonColor
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun PinCodeScreenPreview() {
    BChatTheme {
        Scaffold {
            PinCodeScreen(
                state = PinCodeState(),
                onEvent = {}
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(
    widthDp = 412,
    heightDp = 732
)
@Composable
fun PinCodeScreenPreview2() {
    BChatTheme {
        Scaffold {
            PinCodeScreen(
                state = PinCodeState(),
                onEvent = {}
            )
        }
    }
}
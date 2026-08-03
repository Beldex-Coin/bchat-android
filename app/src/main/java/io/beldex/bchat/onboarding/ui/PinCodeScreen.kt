package io.beldex.bchat.onboarding.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.PinCodeView
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.R

const val EXTRA_PIN_CODE_ACTION = "action"

data class PinCodeState(
    val step:PinCodeSteps = PinCodeSteps.OldPin,
    val stepTitle: String = "",
    val pin: String = "",
    val newPin: String = "",
    val reEnteredPin: String = "",
    val pinLength: Int = 4
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
    data object EnableSixDigitPin : PinCodeEvents
    data object EnableFourDigitPin : PinCodeEvents
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

    val configuration = LocalConfiguration.current

    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isTablet =
        configuration.smallestScreenWidthDp >= 600

    when {
        isTablet && isLandscape -> {
            LandscapePinCodeScreen(
                state,
                pin,
                onEvent,
                isDarkTheme,
                isTablet = true
            )
        }

        isTablet -> {
            PortraitPinCodeScreen(
                state,
                pin,
                onEvent,
                isDarkTheme,
                isTablet = true
            )
        }

        isLandscape -> {
            LandscapePinCodeScreen(
                state,
                pin,
                onEvent,
                isDarkTheme,
                isTablet = false
            )
        }

        else -> {
            PortraitPinCodeScreen(
                state,
                pin,
                onEvent,
                isDarkTheme,
                isTablet = false
            )
        }
    }


}

@Composable
private fun PortraitPinCodeScreen(
    state: PinCodeState,
    pin: String,
    onEvent: (PinCodeEvents) -> Unit,
    isDarkTheme: Boolean,
    isTablet: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(id = if(isDarkTheme) R.drawable.ic_password_dark else R.drawable.ic_password_light),
            contentDescription = "",
            modifier = Modifier.height(
                if (isTablet) 140.dp else 80.dp
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                PinCodeView(
                    pin = pin,
                    pinLength = state.pinLength,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.stepTitle,
                style =
                    if (isTablet)
                        MaterialTheme.typography.headlineSmall
                    else
                        MaterialTheme.typography.titleMedium
            )

            if (state.step == PinCodeSteps.EnterPin) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        if (state.pinLength == 4) {
                            onEvent(PinCodeEvents.EnableSixDigitPin)
                        } else {
                            onEvent(PinCodeEvents.EnableFourDigitPin)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.appColors.secondaryButtonColor),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (state.pinLength == 4) MaterialTheme.appColors.tertiaryButtonColor else MaterialTheme.appColors.primaryButtonColor
                    )
                ) {
                    Text(
                        text = stringResource(if (state.pinLength == 4) R.string.six_digit_pin else R.string.four_digit_pin),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(id = R.drawable.ic_arrow_pin),
                        tint = MaterialTheme.appColors.textColor,
                        contentDescription = "PIN digit change",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if(state.step == PinCodeSteps.EnterPin) 8.dp else 10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val density = LocalDensity.current
            var buttonSize by remember {
                mutableStateOf(IntSize.Zero)
            }
            val buttonHeight = with(density) {
                buttonSize.height.toDp()
            }
            val cellHeight =
                if (isTablet) 88.dp else 64.dp
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
                        .weight(1f)
                        .padding(
                            horizontal = if (isTablet) 48.dp else 24.dp,
                            vertical = if (isTablet) 32.dp else 16.dp
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
                                                if (pin.length < state.pinLength)
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
                                                Icons.Outlined.Backspace,
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
                                                if (pin.length < state.pinLength)
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

                Spacer(modifier = Modifier.height(12.dp))

                if (state.step != PinCodeSteps.VerifyPin) {
                    PrimaryButton(
                        onClick = {
                            onEvent(PinCodeEvents.Submit)
                        },
                        enabled = pin.length == state.pinLength,
                        modifier = Modifier
                            .fillMaxWidth().
                            padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
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
                                color = if(pin.length == state.pinLength) Color.White else MaterialTheme.appColors.disabledNextButtonColor
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun LandscapePinCodeScreen(
    state: PinCodeState,
    pin: String,
    onEvent: (PinCodeEvents) -> Unit,
    isDarkTheme: Boolean,
    isTablet: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.backgroundColor)
    ) {
        val density = LocalDensity.current
        var buttonSize by remember {
            mutableStateOf(IntSize.Zero)
        }
        val buttonHeight = with(density) {
            buttonSize.height.toDp()
        }

        // LEFT SIDE
        Column(
            modifier = Modifier
                .weight(
                    if (isTablet) 0.40f else 0.40f
                )
                .fillMaxHeight()
                .padding(if (isTablet) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Image(
                painter = painterResource(
                    if (isDarkTheme)
                        R.drawable.ic_password_dark
                    else
                        R.drawable.ic_password_light
                ),
                contentDescription = null,
                modifier = Modifier.height(
                    if (isTablet) 150.dp else 80.dp
                )
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                PinCodeView(
                    pin = pin,
                    pinLength = state.pinLength,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = state.stepTitle,
                style =
                    if (isTablet)
                        MaterialTheme.typography.headlineSmall
                    else
                        MaterialTheme.typography.titleMedium
            )

            if (state.step == PinCodeSteps.EnterPin) {

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        if (state.pinLength == 4) {
                            onEvent(PinCodeEvents.EnableSixDigitPin)
                        } else {
                            onEvent(PinCodeEvents.EnableFourDigitPin)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.appColors.secondaryButtonColor),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (state.pinLength == 4) MaterialTheme.appColors.tertiaryButtonColor else MaterialTheme.appColors.primaryButtonColor
                    )
                ) {
                    Text(
                        text = stringResource(if (state.pinLength == 4) R.string.six_digit_pin else R.string.four_digit_pin),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(id = R.drawable.ic_arrow_pin),
                        tint = MaterialTheme.appColors.textColor,
                        contentDescription = "PIN digit change",
                    )
                }
            }
        }

        // RIGHT SIDE
        Column(
            modifier = Modifier
                .weight(
                    if (isTablet) 0.60f else 0.60f
                )
                .fillMaxHeight()
        ) {


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val cellHeight =
                    if (isTablet) 84.dp else 48.dp
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(
                                horizontal = if (isTablet) 48.dp else 12.dp,
                                vertical = 8.dp
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
                                                .fillMaxWidth()
                                                .height(cellHeight)
                                                .clickable {
                                                    if (pin.length < state.pinLength)
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
                                                    Icons.Outlined.Backspace,
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
                                                    if (pin.length < state.pinLength)
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

                    if (state.step != PinCodeSteps.VerifyPin) {
                        PrimaryButton(
                            onClick = {
                                onEvent(PinCodeEvents.Submit)
                            },
                            enabled = pin.length == state.pinLength,
                            modifier = Modifier
                                .fillMaxWidth().
                                padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
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
                                    color = if(pin.length == state.pinLength) Color.White else MaterialTheme.appColors.disabledNextButtonColor
                                ),
                                modifier = Modifier
                                    .padding(8.dp)
                            )
                        }
                    }

                }
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
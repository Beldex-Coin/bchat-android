package io.beldex.bchat.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors

/**
 * Live "Register" onboarding step, hosted by [io.beldex.bchat.onboarding.RegisterActivity].
 * Matches the Revamp_2026 `Privacy_settings` frame (7546:3651) — misleadingly named in Figma, its
 * actual content is this Register/welcome step (BChat ID card, Beldex Address card, Continue).
 */
@Composable
fun RegisterScreen(
    headline: String,
    isAddressLoading: Boolean,
    beldexAddress: String,
    isPublicKeyLoading: Boolean,
    publicKey: String,
    registerEnabled: Boolean,
    onCopyPublicKey: () -> Unit,
    onCopyAddress: () -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.onboardingBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            OnboardingTopBar(
                title = stringResource(R.string.register),
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
            ) {
                Text(
                    text = headline,
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )

                RegisterKeyCard(
                    label = stringResource(R.string.chatid),
                    labelColor = MaterialTheme.appColors.userDetailsBchatIdText,
                    isLoading = isPublicKeyLoading,
                    value = publicKey,
                    onCopyClick = onCopyPublicKey,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Text(
                    text = stringResource(R.string.register_screen_chat_id_description_content),
                    color = MaterialTheme.appColors.onboardingBodyColor,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                RegisterKeyCard(
                    label = stringResource(R.string.beldex_address),
                    labelColor = MaterialTheme.appColors.onboardingHeadlineColor,
                    isLoading = isAddressLoading,
                    value = beldexAddress,
                    onCopyClick = onCopyAddress,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Text(
                    text = stringResource(R.string.register_screen_beldex_address_description_content),
                    color = MaterialTheme.appColors.onboardingBodyColor,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
            }

            OnboardingPrimaryButton(
                text = stringResource(R.string.continue_2),
                enabled = registerEnabled,
                onClick = onRegisterClick,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun RegisterKeyCard(
    label: String,
    labelColor: androidx.compose.ui.graphics.Color,
    isLoading: Boolean,
    value: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.appColors.onboardingInputBackground)
            .then(
                if (isLoading) Modifier else Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCopyClick
                )
            )
            .border(1.dp, MaterialTheme.appColors.userDetailsCancelBackground, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text(
            text = label,
            color = labelColor,
            fontFamily = OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (isLoading) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.load_animation))
            val progress by animateLottieCompositionAsState(composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever)
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(top = 10.dp)
            )
        } else {
            Text(
                text = value,
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = RobotoMono,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() {
    BChatTheme {
        RegisterScreen(
            headline = "Hey Beldex, welcome to BChat!",
            isAddressLoading = false,
            beldexAddress = "bxAAAA...1234",
            isPublicKeyLoading = true,
            publicKey = "",
            registerEnabled = true,
            onCopyPublicKey = {},
            onCopyAddress = {},
            onRegisterClick = {},
            onBackClick = {}
        )
    }
}

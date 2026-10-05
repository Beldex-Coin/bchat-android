package io.beldex.bchat.onboarding.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape

/**
 * Live seed-backup onboarding step (Figma `seed` 7546:982), hosted by
 * [io.beldex.bchat.onboarding.RecoveryPhraseActivity].
 */
@Composable
fun RecoveryPhraseScreen(
    title: String,
    seed: String,
    seedCopied: Boolean,
    onCopySeedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onContinueClick: () -> Unit,
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
            OnboardingTopBar(title = title, onBackClick = onBackClick)

            Column(modifier = Modifier.weight(1f).padding(horizontal = 22.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_recovery_seed_outline),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.onboardingInputText,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(R.string.copy_your_recovery_seed),
                    color = MaterialTheme.appColors.seedInfoTextColor,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )

                Text(
                    text = seed,
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = RobotoMono,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.appColors.onboardingInputBackground)
                        .border(1.dp, MaterialTheme.appColors.newChatIconBackground, RoundedCornerShape(16.dp))
                        .padding(19.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryButton(
                        onClick = onCopySeedClick,
                        shape = notchedCornerShape(12.dp),
                        containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground,
                        contentColor = MaterialTheme.appColors.onboardingSecondaryButtonText,
                        border = BorderStroke(1.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.copy_seed),
                            fontFamily = RobotoMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = null,
                            tint = MaterialTheme.appColors.textGreen,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(14.dp)
                        )
                    }

                    PrimaryButton(
                        onClick = onSaveClick,
                        shape = notchedCornerShape(12.dp),
                        containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                        contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontFamily = RobotoMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !seedCopied) {
                Text(
                    text = stringResource(R.string.copy_and_save_the_seed_to_continue),
                    color = MaterialTheme.appColors.onboardingCaptionColor,
                    fontFamily = OpenSans,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                )
            }

            OnboardingPrimaryButton(
                text = stringResource(R.string.continue_2),
                enabled = seedCopied,
                onClick = onContinueClick,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun RecoveryPhraseScreenPreview() {
    BChatTheme {
        RecoveryPhraseScreen(
            title = "Recovery Phrase",
            seed = "abandon ability able about above absent absorb abstract absurd abuse access accident",
            seedCopied = false,
            onCopySeedClick = {},
            onSaveClick = {},
            onContinueClick = {},
            onBackClick = {}
        )
    }
}

package io.beldex.bchat.onboarding.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun LandingScreen(
    onCreateAccountClick: () -> Unit,
    onRestoreAccountClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.onboardingBackground),
        contentAlignment = Alignment.BottomEnd
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_landing_decoration),
            contentDescription = null,
            modifier = Modifier
                .wrapContentSize(unbounded = true, align = Alignment.BottomEnd)
                .height(280.dp),
            contentScale = ContentScale.FillHeight
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bchat_logo),
                    contentDescription = null,
                    modifier = Modifier.height(40.dp)
                )

                Text(
                    text = stringResource(R.string.landing_headline),
                    color = MaterialTheme.appColors.onboardingHeadlineColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    modifier = Modifier.padding(top = 36.dp)
                )

                Text(
                    text = stringResource(R.string.landing_screen_content),
                    color = MaterialTheme.appColors.onboardingBodyColor,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    onClick = onCreateAccountClick,
                    shape = notchedCornerShape(14.5.dp),
                    containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                    contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = stringResource(R.string.activity_landing_register_button_title),
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                PrimaryButton(
                    onClick = onRestoreAccountClick,
                    shape = notchedCornerShape(14.5.dp),
                    containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground,
                    contentColor = MaterialTheme.appColors.onboardingSecondaryButtonText,
                    border = BorderStroke(1.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = stringResource(R.string.activity_landing_restore_button_title),
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = stringResource(R.string.terms_and_conditions),
                    color = MaterialTheme.appColors.onboardingTermsColor,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTermsClick
                        )
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun LandingScreenPreview() {
    BChatTheme {
        LandingScreen(onCreateAccountClick = {}, onRestoreAccountClick = {}, onTermsClick = {})
    }
}

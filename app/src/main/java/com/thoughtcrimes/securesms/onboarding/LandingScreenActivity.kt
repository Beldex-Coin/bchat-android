package com.thoughtcrimes.securesms.onboarding

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.OpenSans
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.onboarding.ui.OnBoardingActivity
import com.thoughtcrimes.securesms.onboarding.ui.OnBoardingScreens
import com.thoughtcrimes.securesms.util.push
import io.beldex.bchat.R

class LandingScreenActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        LandingScreen(
                            restoreAccount = {
                                restore()
                            },
                            createAccount = {
                                createAccount()
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it)
                        )
                    }
                }
            }
        }
    }

    private fun restore() {
        val intent = Intent(this, OnBoardingActivity::class.java).apply {
            putExtra(OnBoardingActivity.extraStartDestination, OnBoardingScreens.RestoreSeedScreen.route)
        }
        push(intent)
        finish()
    }

    private fun createAccount() {
        val intent = Intent(this, OnBoardingActivity::class.java).apply {
            putExtra(OnBoardingActivity.extraStartDestination, OnBoardingScreens.DisplayNameScreen.route)
        }
        push(intent)
        finish()
    }
}

@Composable
fun LandingScreen(
    restoreAccount: () -> Unit,
    createAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_landing_bubble),
                contentDescription = "",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 24.dp
                    )
            ) {
                Text(
                    text = "Welcome to",
                    style = BChatTypography.headlineSmall.copy(
                        color = MaterialTheme.appColors.onMainContainerTextColor,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    modifier = Modifier
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bchat_logo),
                        contentDescription = "",
                        modifier = Modifier
                            .width(48.dp)
                    )
                    Spacer(
                        modifier = Modifier
                            .width(16.dp)
                    )
                    Text(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = OpenSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 48.sp,
                                    color = MaterialTheme.appColors.primaryButtonColor
                                )
                            ) {
                                append("B")
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = OpenSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 48.sp,
                                    color = MaterialTheme.appColors.tertiaryButtonColor
                                )
                            ) {
                                append("Chat")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(id = R.string.hello),
                    style = BChatTypography.headlineSmall.copy(
                        color = MaterialTheme.appColors.onMainContainerTextColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.we_are_thrilled),
                    style = BChatTypography.headlineSmall.copy(
                        color = MaterialTheme.appColors.onMainContainerTextColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.landing_screen_content),
                    style = BChatTypography.bodyMedium.copy(
                        color = MaterialTheme.appColors.onMainContainerTextColor
                    )
                )
            }
        }

        PrimaryButton(
            onClick = {
                createAccount()
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(id = R.string.create_account),
                style = BChatTypography.bodyLarge.copy(
                    color = Color.White
                ),
                modifier = Modifier
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                restoreAccount()
            },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(id = R.string.restore_account),
                style = BChatTypography.bodyLarge.copy(
                    color = MaterialTheme.appColors.onMainContainerTextColor
                ),
                modifier = Modifier
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.terms_and_conditions),
            style = BChatTypography.labelMedium.copy(
                color = MaterialTheme.appColors.secondaryTextColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.ic_bchat_2),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 24.dp)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LandingScreenPreview() {
    BChatTheme {
        Scaffold {
            LandingScreen(
                restoreAccount = {},
                createAccount = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun LandingScreenPreviewLight() {
    BChatTheme {
        Scaffold {
            LandingScreen(
                restoreAccount = {},
                createAccount = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            )
        }
    }
}
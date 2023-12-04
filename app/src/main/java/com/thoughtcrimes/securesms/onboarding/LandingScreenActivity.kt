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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LandingScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_bchat_logo),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth(0.3f)
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
                            fontSize = 56.sp,
                            color = MaterialTheme.appColors.primaryButtonColor
                        )
                    ) {
                        append("B")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontFamily = OpenSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 56.sp,
                            color = MaterialTheme.appColors.tertiaryButtonColor
                        )
                    ) {
                        append("Chat")
                    }
                }
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_landing_bubble),
                contentDescription = "",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Hey You!\nChat anonymously now.",
                    style = BChatTypography.headlineSmall.copy(
                        color = MaterialTheme.appColors.onMainContainerTextColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "The Private Web 3 messaging app that protects your conversational freedom. Create an account instantly.",
                    style = BChatTypography.bodyMedium.copy(
                        color = MaterialTheme.appColors.onMainContainerTextColor
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
        ) {
            PrimaryButton(
                onClick = {

                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
            ) {
                Text(
                    text = "Create Account",
                    style = BChatTypography.bodyLarge.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, OnBoardingActivity::class.java))
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
            ) {
                Text(
                    text = "Restore Account",
                    style = BChatTypography.bodyLarge.copy(
                        color = MaterialTheme.appColors.onMainContainerTextColor
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp
                )
        ) {
            Text(
                text = "Terms &amp; Conditions",
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
                    .align(Alignment.BottomStart)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LandingScreenPreview() {
    BChatTheme {
        Scaffold {
            LandingScreen(
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            )
        }
    }
}
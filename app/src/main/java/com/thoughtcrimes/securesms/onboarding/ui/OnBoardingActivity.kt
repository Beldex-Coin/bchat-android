package com.thoughtcrimes.securesms.onboarding.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

class OnBoardingActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            BChatTheme {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        val navController = rememberNavController()
                        OnBoardingNavHost(
                            navController = navController,
                            modifier = Modifier
                                .padding(it)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnBoardingNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = OnBoardingScreens.EnterPinCode.route
    ) {
        composable(
            route = OnBoardingScreens.RestoreSeedScreen.route
        ) {
            ScreenContainer(
                title = "Restore Seed",
                onBackClick = {
                    (context as Activity).finish()
                }
            ) {
                RestoreSeedScreen(
                    navigateToNextScreen = {
                        navController.navigate(OnBoardingScreens.RestoreFromSeedScreen.route)
                    }
                )
            }
        }

        composable(
            route = OnBoardingScreens.RestoreFromSeedScreen.route
        ) {
            ScreenContainer(
                title = "Restore From Seed",
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                RestoreFromSeedScreen(
                    navigateToPinCode = {
                        navController.navigate(OnBoardingScreens.EnterPinCode.route)
                    }
                )
            }
        }

        composable(
            route = OnBoardingScreens.EnterPinCode.route
        ) {
            ScreenContainer(
                title = "Create Password",
                wrapInCard = false,
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                PinCodeScreen()
            }
        }
    }
}

@Composable
private fun ScreenContainer(
    title: String,
    wrapInCard: Boolean = true,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.appColors.editTextColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable {
                        onBackClick()
                    }
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (wrapInCard) {
            CardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.backgroundColor
        ),
        modifier = modifier
    ) {
        content()
    }
}
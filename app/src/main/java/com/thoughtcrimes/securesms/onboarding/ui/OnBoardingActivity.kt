package com.thoughtcrimes.securesms.onboarding.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnBoardingActivity: ComponentActivity() {

    private var destination = OnBoardingScreens.RestoreSeedScreen.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination = intent?.getStringExtra(extraStartDestination) ?: OnBoardingScreens.RestoreSeedScreen.route
        setContent { 
            BChatTheme {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        val navController = rememberNavController()
                        OnBoardingNavHost(
                            navController = navController,
                            startDestination = destination,
                            modifier = Modifier
                                .padding(it)
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val extraStartDestination = "io.beldex.EXTRA_START_DESTINATION"
    }
}

@Composable
fun OnBoardingNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: OnBoardingViewModel = hiltViewModel()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = OnBoardingScreens.RestoreSeedScreen.route
        ) {
            ScreenContainer(
                title = stringResource(R.string.restore_seed),
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
                title = stringResource(R.string.restore_from_seed),
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
            route = OnBoardingScreens.EnterPinCode.route,
            arguments = listOf(
                navArgument("action") {
                    type = NavType.IntType
                    defaultValue = PinCodeAction.VerifyPinCode.action
                },
                navArgument("finish") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "onboarding://manage_pin?finish={finish}&action={action}"
                }
            )
        ) {
            val finish = it.arguments?.getBoolean("finish") ?: false
            val pinCodeAction = it.arguments?.getInt("action")
            val viewModel: PinCodeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(key1 = true) {
                launch {
                    viewModel.errorMessage.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.successEvent.collectLatest { success ->
                        if (success) {
                            if (finish) {
                                (context as Activity).apply {
                                    val data = Intent().apply {
                                        putExtra("success", true)
                                    }
                                    setResult(Activity.RESULT_OK, data)
                                    finish()
                                }
                            } else {
                                navController.navigate(OnBoardingScreens.CopyRestoreSeedScreen.route)
                            }
                        }
                    }
                }
            }
            ScreenContainer(
                title = if (pinCodeAction == PinCodeAction.VerifyPinCode.action)
                    stringResource(R.string.verify_pin)
                else
                    stringResource(R.string.create_password),
                wrapInCard = false,
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                PinCodeScreen(
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }
        }

        composable(
            route = OnBoardingScreens.DisplayNameScreen.route
        ) {
            ScreenContainer(
                title = stringResource(id = R.string.display_name),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                val uiState by viewModel.uiState.collectAsState()
                DisplayNameScreen(
                    displayName = uiState.displayName ?: "",
                    proceed = {
                        navController.navigate(OnBoardingScreens.GenerateKeyScreen.route)
                    },
                    onEvent = viewModel::onEvent
                )
            }
        }

        composable(
            route = OnBoardingScreens.GenerateKeyScreen.route
        ) {
            ScreenContainer(
                title = stringResource(id = R.string.display_name),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                KeyGenerationScreen(
                    proceed = {
                        val intent = Intent(Intent.ACTION_VIEW, "onboarding://manage_pin?finish=false&action=${PinCodeAction.CreatePinCode.action}".toUri())
                        context.startActivity(intent)
                    }
                )
            }
        }
        
        composable(
            route = OnBoardingScreens.CopyRestoreSeedScreen.route
        ) {
            ScreenContainer(
                title = stringResource(id = R.string.restore_seed),
                onBackClick = {}
            ) {
                CopySeedScreen(
                    seed = "Ut34co m56m 77odo8 6ve66ne natis023 3diam0id 5accum s3an3 6383ut7 purus eges tas34f acilisis is0233 diam0 id5acc ums3an36383ut7p",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
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
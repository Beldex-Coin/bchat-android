package io.beldex.bchat.onboarding.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
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
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val view = LocalView.current
            val window = (view.context as Activity).window
            val statusBarColor = if (isDarkTheme) Color.Black else Color.White
            SideEffect {
                window.statusBarColor = statusBarColor.toArgb()
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
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
            var showPinChangedPopup by remember {
                mutableStateOf(false)
            }
            var showPinChangedPopupTitle by remember {
                mutableStateOf("")
            }

            if (showPinChangedPopup) {
                PassWordChangedPopup(
                        onDismiss={
                            showPinChangedPopup=false
                            if (finish) {
                                (context as Activity).apply {
                                    val data=Intent().apply {
                                        putExtra("success", true)
                                    }
                                    setResult(Activity.RESULT_OK, data)
                                    finish()
                                }
                            } else {
                                navController.navigate(OnBoardingScreens.CopyRestoreSeedScreen.route)
                            }
                        },
                        showPinChangedPopupTitle
                )
            }
            LaunchedEffect(key1 = true) {
                launch {
                    viewModel.errorMessage.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.successEvent.collectLatest { success ->
                        if (success) {
                            showPinChangedPopup = true
                        }else{
                            if (finish) {
                                (context as Activity).apply {
                                    val data=Intent().apply {
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
                launch {
                    viewModel.successContent.collectLatest { message ->
                        if (message != null) {
                            showPinChangedPopupTitle =message
                        }

                    }
                }
            }
            ScreenContainer(
                title =when (pinCodeAction) {
                    PinCodeAction.VerifyPinCode.action -> stringResource(R.string.verify_pin)
                    PinCodeAction.ChangePinCode.action -> stringResource(R.string.change_password)
                    else -> stringResource(R.string.create_password)
                },
                wrapInCard = false,
                onBackClick = {
                    if (finish) {
                        (context as Activity).apply {
                            finish()
                        }
                    } else
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
                    modifier =Modifier
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {
            Icon(
                painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.appColors.editTextColor,
                modifier =Modifier
                        .clickable {
                            onBackClick()
                        }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (wrapInCard) {
            CardContainer(
                modifier =Modifier
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
fun PassWordChangedPopup(onDismiss : () -> Unit, showPinChangedPopupTitle : String) {
    val context = LocalContext.current
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(if(isDarkTheme) R.raw.sent else R.raw.sent_light))
    val isPlaying by remember {
        mutableStateOf(true)
    }
    // for speed
    val speed by remember {
        mutableFloatStateOf(1f)
    }
    val progress by animateLottieCompositionAsState(composition, isPlaying = isPlaying, speed = speed, restartOnPlay = false)
    DialogContainer(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground), elevation=CardDefaults.cardElevation(defaultElevation=4.dp), modifier=Modifier.fillMaxWidth()) {
            Column(horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center, modifier=Modifier
                    .fillMaxWidth()
                    .padding(10.dp)) {
                LottieAnimation(composition, progress, modifier=Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally))

                Text(text=showPinChangedPopupTitle,
                        textAlign=TextAlign.Center,
                        style=MaterialTheme.typography.titleMedium.copy(
                                fontSize=16.sp,
                                fontWeight=FontWeight(800),
                                color=MaterialTheme.appColors.primaryButtonColor),
                        modifier=Modifier.padding(vertical=16.dp)
                        )

                Button(onClick={ onDismiss() }, shape = RoundedCornerShape(12.dp), colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.appColors.primaryButtonColor), modifier=Modifier
                        .height(50.dp)
                        .width(150.dp)
                ) {
                    Text(text=stringResource(id=R.string.ok), style=MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight(400),
                        fontSize = 14.sp,
                        color=Color.White
                    ))
                }
            }

        }
    }

}

@Preview
@Composable
fun PopUpPreview(){
    PassWordChangedPopup({

    }, showPinChangedPopupTitle = "")
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
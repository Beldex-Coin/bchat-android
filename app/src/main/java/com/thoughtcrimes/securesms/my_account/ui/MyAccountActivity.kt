package com.thoughtcrimes.securesms.my_account.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thoughtcrimes.securesms.applock.AppLockDetailsActivity
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.contacts.blocked.BlockedContactsActivity
import com.thoughtcrimes.securesms.preferences.ChatSettingsActivity
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R

@AndroidEntryPoint
class MyAccountActivity: ComponentActivity() {

    private var destination = MyAccountScreens.MyAccountScreen.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination = intent?.getStringExtra(extraStartDestination) ?: MyAccountScreens.MyAccountScreen.route
        setContent {
            BChatTheme(
                darkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        val navController = rememberNavController()
                        MyAccountNavHost(
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
fun MyAccountNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startActivity: (Intent) -> Unit = {
        context.startActivity(it)
    }
    val viewModel: MyAccountViewModel = hiltViewModel()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = MyAccountScreens.MyAccountScreen.route
        ) {
            val uiState by viewModel.uiState.collectAsState()
            MyAccountScreenContainer(
                title = stringResource(R.string.my_account),
                onBackClick = {}
            ) {
                MyAccountScreen(
                    uiState = uiState
                )
            }
        }

        composable(
            route = MyAccountScreens.SettingsScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.activity_settings_title),
                onBackClick = {}
            ) {
                SettingsScreen(
                    navigate = {
                        when (it) {
                            SettingItem.Hops -> {
//                                Intent(context, PathActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                viewModel.getPathNodes()
                                navController.navigate(MyAccountScreens.HopsScreen.route)
                            }
                            SettingItem.AppLock -> {
//                                Intent(context, AppLockDetailsActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                navController.navigate(MyAccountScreens.AppLockScreen.route)
                            }
                            SettingItem.ChatSettings -> {
                                Intent(context, ChatSettingsActivity::class.java).also { intent ->
                                    startActivity(intent)
                                }
                            }
                            SettingItem.BlockedContacts -> {
                                Intent(context, BlockedContactsActivity::class.java).also { intent ->
                                    startActivity(intent)
                                }
                            }
                            SettingItem.ClearData -> {

                            }
                            SettingItem.Feedback -> {
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse("mailto:") // only email apps should handle this
                                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@beldex.io"))
                                intent.putExtra(Intent.EXTRA_SUBJECT, "")
                                startActivity(intent)
                            }
                            SettingItem.FAQ -> {
                                try {
                                    val url = "https://bchat.beldex.io/faq"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Can't open URL", Toast.LENGTH_LONG).show()
                                }
                            }
                            SettingItem.ChangeLog -> {
//                                Intent(context, ChangeLogActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                navController.navigate(MyAccountScreens.ChangeLogScreen.route)
                            }
                        }
                    }
                )
            }
        }

        composable(
            route = MyAccountScreens.HopsScreen.route
        ) {
            val nodes by viewModel.pathState.collectAsState()
            MyAccountScreenContainer(
                title = stringResource(id = R.string.activity_path_title),
                onBackClick = {}
            ) {
                HopsScreen(
                    nodes = nodes
                )
            }
        }

        composable(
            route = MyAccountScreens.ChangeLogScreen.route
        ) {
            val changeLogViewModel: ChangeLogViewModel = hiltViewModel()
            val changeLogs by changeLogViewModel.changeLogs.collectAsState()

            MyAccountScreenContainer(
                title = stringResource(id = R.string.changelog),
                onBackClick = {}
            ) {
                ChangeLogScreen(
                    changeLogs = changeLogs
                )
            }
        }

        composable(
            route = MyAccountScreens.AppLockScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.changelog),
                onBackClick = {}
            ) {
                AppLockScreen()
            }
        }
    }
}

@Composable
private fun MyAccountScreenContainer(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.appColors.editTextColor,
                modifier = Modifier
                    .clickable {
                        onBackClick()
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
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
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.backgroundColor
        ),
        modifier = modifier
    ) {
        content()
    }
}
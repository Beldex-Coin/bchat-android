package com.thoughtcrimes.securesms.my_account.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cash.copper.flow.observeQuery
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.contacts.blocked.BlockedContactsViewModel
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.crypto.MnemonicUtilities
import com.thoughtcrimes.securesms.database.DatabaseContentProviders
import com.thoughtcrimes.securesms.messagerequests.MessageRequestsViewModel
import com.thoughtcrimes.securesms.my_account.ui.dialogs.ClearDataDialog
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeAction
import com.thoughtcrimes.securesms.preferences.ChatSettingsActivity
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.StatWalletInfo
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyAccountActivity: ComponentActivity() {

    private var destination = MyAccountScreens.SettingsScreen.route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination = intent?.getStringExtra(extraStartDestination) ?: MyAccountScreens.SettingsScreen.route
        setContent {
            BChatTheme(
                darkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary,
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
//        composable(
//            route = MyAccountScreens.MyAccountScreen.route
//        ) {
//            val uiState by viewModel.uiState.collectAsState()
//            MyAccountScreenContainer(
//                title = stringResource(R.string.my_account),
//                onBackClick = {
//                    (context as ComponentActivity).finish()
//                }
//            ) {
//                MyAccountScreen(
//                    uiState = uiState
//                )
//            }
//        }

        composable(
            route = MyAccountScreens.SettingsScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.activity_settings_title),
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                var showClearDataDialog by remember {
                    mutableStateOf(false)
                }
                if (showClearDataDialog) {
                    ClearDataDialog {
                        showClearDataDialog = false
                    }
                }
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
//                                navController.navigate(MyAccountScreens.ChatSettingsScreen.route)
                            }
                            SettingItem.BlockedContacts -> {
//                                Intent(context, BlockedContactsActivity::class.java).also { intent ->
//                                    startActivity(intent)
//                                }
                                navController.navigate(MyAccountScreens.BlockedContactScreen.route)
                            }
                            SettingItem.ClearData -> {
                                showClearDataDialog = true
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
                onBackClick = {
                    navController.navigateUp()
                }
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
                onBackClick = {
                    navController.navigateUp()
                }
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
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                AppLockScreen()
            }
        }

        composable(
            route = MyAccountScreens.ChatSettingsScreen.route
        ) {
            MyAccountScreenContainer(
                title = stringResource(id = R.string.preferences_chats__chats),
                onBackClick = {
                    navController.navigateUp()
                }
            ) {
                ChatSettingsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.BlockedContactScreen.route
        ) {
            val contactViewModel: BlockedContactsViewModel = hiltViewModel()
            var contacts by remember {
                mutableStateOf(emptyList<Recipient>())
            }
            val uiState by contactViewModel.uiState.collectAsState()
            val owner = LocalLifecycleOwner.current
            LaunchedEffect(key1 = Unit) {
                contactViewModel.subscribe(context).observe(owner) { newState ->
                    contacts = newState.blockedContacts
                }
            }
            MyAccountScreenContainer(
                title = stringResource(id = R.string.blocked_contacts),
                onBackClick = {
                    navController.navigateUp()
                },
                actionItems = {
                    if (contacts.isNotEmpty()) {
                        Icon(
                            painter = if (uiState.multiSelectedActivated)
                                painterResource(id = R.drawable.ic_selected_all)
                            else
                                painterResource(id = R.drawable.ic_unselected_all),
                            contentDescription = "",
                            tint = MaterialTheme.appColors.iconTint,
                            modifier = Modifier
                                .clickable {
                                    contactViewModel.onEvent(BlockedContactEvents.MultiSelectClicked)
                                }
                        )
                    }
                }
            ) {
                if (contacts.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_blocked_contact),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                } else {
                    BlockedContactScreen(
                        blockedList = contacts,
                        selectedList = uiState.selectedList,
                        multiSelectActivated = uiState.multiSelectedActivated,
                        unBlockSingleContact = {
                            contactViewModel.onEvent(BlockedContactEvents.UnblockSingleContact(it))
                        },
                        unBlockMultipleContacts = {
                            contactViewModel.onEvent(BlockedContactEvents.UnblockMultipleContact)
                        },
                        addRemoveContactToList = { contact, add ->
                            contactViewModel.onEvent(BlockedContactEvents.AddContactToUnBlockList(contact, add))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }

        composable(
            route = MyAccountScreens.AboutScreen.route
        ) {
            val aboutViewModel: ContentViewModel = hiltViewModel()
            val content by aboutViewModel.content.collectAsState()
            MyAccountScreenContainer(
                title = stringResource(R.string.about),
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                ContentScreen(
                    content = content,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(
            route = MyAccountScreens.RecoverySeedScreen.route
        ) {
            var markedAsSafe by  remember {
                mutableStateOf(false)
            }
            val resultLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    markedAsSafe = true
                } else {
                    markedAsSafe = false
                    Toast.makeText(context, "Failed to authenticate", Toast.LENGTH_SHORT).show()
                }
            }
            val verifyPin: () -> Unit  = {
                val intent = Intent(Intent.ACTION_VIEW, "onboarding://manage_pin?finish=true&action=${PinCodeAction.VerifyPinCode.action}".toUri())
                resultLauncher.launch(intent)
            }
            val seed by lazy {
                try {
                    var hexEncodedSeed = IdentityKeyUtil.retrieve(context, IdentityKeyUtil.BELDEX_SEED)
                    if (hexEncodedSeed == null) {
                        hexEncodedSeed = IdentityKeyUtil.getIdentityKeyPair(context).hexEncodedPrivateKey // Legacy account
                    }
                    val loadFileContents: (String) -> String = { fileName ->
                        MnemonicUtilities.loadFileContents(context, fileName)
                    }
                    MnemonicCodec(loadFileContents).encode(
                        hexEncodedSeed,
                        MnemonicCodec.Language.Configuration.english
                    )
                } catch (e: Exception) {
                    ""
                }
            }
            MyAccountScreenContainer(
                title = stringResource(R.string.recovery_seed),
                wrapInCard = markedAsSafe,
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                RecoverySeedScreen(
                    seed = seed,
                    markedAsSafe = markedAsSafe,
                    verifyPin = verifyPin,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        composable(route = MyAccountScreens.StartWalletInfoScreen.route) {
            MyAccountScreenContainer(title = stringResource(id = R.string.wallets), onBackClick = {
                navController.navigateUp()
            }) {
                StatWalletInfo(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp))

            }

        }

        composable(
            route = MyAccountScreens.MessageRequestsScreen.route
        ) {
            val requestViewModel: MessageRequestsViewModel = hiltViewModel()
            val uiState by requestViewModel.uiState.collectAsState()

            LaunchedEffect(key1 = Unit) {
                launch(Dispatchers.IO) {
                    context.contentResolver
                        .observeQuery(DatabaseContentProviders.ConversationList.CONTENT_URI)
                        .onEach { requestViewModel.refreshRequests() }
                        .collect()
                }
            }
            MyAccountScreenContainer(
                title = stringResource(R.string.activity_message_requests_title),
                onBackClick = {
                    (context as ComponentActivity).finish()
                }
            ) {
                MessageRequestsScreen(
                    requestsList = uiState.messageRequests,
                    onEvent = requestViewModel::onEvent,
                    onRequestClick = {
                        val returnIntent = Intent()
                        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,it.threadId)
                        (context as Activity).run {
                            setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                            finish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MyAccountScreenContainer(
    title: String,
    wrapInCard: Boolean = true,
    onBackClick: () -> Unit,
    actionItems: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
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
                ),
                modifier = Modifier
                    .weight(1f)
            )

            actionItems()
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
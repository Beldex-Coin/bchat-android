package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.my_account.ui.CardContainer
import com.thoughtcrimes.securesms.onboarding.ui.EXTRA_PIN_CODE_ACTION
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeAction
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.wallet.addressbook.AddressBookScreen
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.node.NodeComposeActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R

@AndroidEntryPoint
class WalletSettingComposeActivity : ComponentActivity() {

    private var destination=WalletSettingScreens.MyWalletSettingsScreen.route
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination=intent?.getStringExtra(extraStartDestination)
                ?: WalletSettingScreens.MyWalletSettingsScreen.route
        setContent {
            BChatTheme(
                    darkTheme=UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                Surface {
                    Scaffold(
                            containerColor=MaterialTheme.colorScheme.primary,
                    ) {
                        val navController=rememberNavController()
                        WalletSettingNavHost(
                                navController=navController,
                                startDestination=destination,
                                modifier=Modifier
                                        .padding(it)
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val extraStartDestination="io.beldex.EXTRA_START_DESTINATION"
    }
}

@SuppressLint("StateFlowValueCalledInComposition", "UnrememberedMutableState", "MutableCollectionMutableState")
@Composable
fun WalletSettingNavHost(
        navController: NavHostController,
        startDestination: String,
        modifier: Modifier=Modifier
) {
    val context=LocalContext.current
    val viewModel: WalletSettingViewModel=hiltViewModel()
    val startActivity: (Intent) -> Unit={
        context.startActivity(it)
    }
    NavHost(
            navController=navController,
            startDestination=startDestination,
            modifier=modifier
    ) {

        composable(
                route=WalletSettingScreens.MyWalletSettingsScreen.route
        ) {
            WalletSettingScreenContainer(
                    title=stringResource(id=R.string.wallet_settings),
                    onBackClick={
                        (context as ComponentActivity).finish()
                    }
            ) {
                var showDisplayBalanceDialog by remember {
                    mutableStateOf(false)
                }
                var showDecimalDialog by remember {
                    mutableStateOf(false)
                }
                var showCurrencyDialog by remember {
                    mutableStateOf(false)
                }
                var showFeePriorityDialog by remember {
                    mutableStateOf(false)
                }
                var resultText by remember {
                    mutableStateOf("")
                }
                val resultLauncher = rememberLauncherForActivityResult(
                        contract=ActivityResultContracts.StartActivityForResult()
                ){ result ->
                    if(result.resultCode == Activity.RESULT_OK){
                        result.data?.let { data ->
                            resultText = data.getStringExtra("selected_node_key") ?:""
                        }
                    }
                }
                if (showDisplayBalanceDialog) {
                    DisplayBalanceDialog({
                        showDisplayBalanceDialog=false
                    }) { value ->
                        TextSecurePreferences.setDisplayBalanceAs(context, value!!)
                        viewModel.updateDisplayBalance(value)
                        showDisplayBalanceDialog=false
                    }
                }
                if (showDecimalDialog) {
                    DecimalDialog({
                        showDecimalDialog=false
                    }) { value ->
                        TextSecurePreferences.setDecimals(context, value)
                        viewModel.updateDecimal(value!!)
                        showDecimalDialog=false
                    }
                }
                if (showCurrencyDialog) {
                    CurrencyDialog({
                        showCurrencyDialog=false
                    }) { value ->
                        TextSecurePreferences.setCurrency(context, value)
                        TextSecurePreferences.changeCurrency(context,true)
                        viewModel.updateCurrency(value!!)
                        showCurrencyDialog=false
                    }
                }
                if (showFeePriorityDialog) {
                    FeePriorityDialog({
                        showFeePriorityDialog=false
                    }) { value ->
                        TextSecurePreferences.setFeePriority(context, value!!)
                        viewModel.updateFeePriority(value)
                        showFeePriorityDialog=false
                    }
                }

                WalletSettingsScreen(
                        navigate={
                            when (it) {

                                WalletSettingsItem.DisplayBalance -> {
                                    showDisplayBalanceDialog=true
                                }

                                WalletSettingsItem.Decimals -> {
                                    showDecimalDialog=true
                                }

                                WalletSettingsItem.Currency -> {
                                    showCurrencyDialog=true
                                }

                                WalletSettingsItem.FeePriority -> {
                                    showFeePriorityDialog=true
                                }

                            }
                        },
                        navItem={
                            when (it) {
                                WalletSettingsNavItem.CurrentNode -> {
                                    // navController.navigate(WalletSettingScreens.NodeScreen.route)
                                    Intent(context, NodeComposeActivity::class.java).also { intent ->
                                        resultLauncher.launch(intent)
                                    }
                                }

                                WalletSettingsNavItem.ChangePin -> {
                                    changePin(context)
                                }

                                WalletSettingsNavItem.SaveRecipientAddress -> {

                                    Toast.makeText(context, "save address", Toast.LENGTH_SHORT).show()

                                }
                            }

                        },
                        false,
                        viewModel, navController,
                        resultText

                )

            }
        }
        composable(
                route=WalletSettingScreens.AddressBookScreen.route
        ) {
            WalletSettingScreenContainer(
                    title=stringResource(id=R.string.activity_address_book_page_title),
                onBackClick={ (context as ComponentActivity).finish() }, ) {
                AddressBookScreen()
            }
        }
    }
}

fun changePin(context: Context) {
    TextSecurePreferences.setChangePin(context, true)
    val lockManager: LockManager<CustomPinActivity> =LockManager.getInstance() as LockManager<CustomPinActivity>
    lockManager.enableAppLock(context, CustomPinActivity::class.java)
    val intent=Intent(context, CustomPinActivity::class.java)
    intent.putExtra(EXTRA_PIN_CODE_ACTION, PinCodeAction.ChangeWalletPin.action)
    intent.putExtra("change_pin", true)
    intent.putExtra("send_authentication", false)
    context.startActivity(intent)
}

@Composable
private fun WalletSettingScreenContainer(
        title: String,
        wrapInCard: Boolean=true,
        onBackClick: () -> Unit,
        actionItems: @Composable () -> Unit={},
        content: @Composable () -> Unit,
) {
    Column(
            modifier=Modifier
                    .fillMaxSize()
    ) {
        Row(
                verticalAlignment=Alignment.CenterVertically,
                modifier=Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
        ) {
            Icon(
                    painterResource(id = R.drawable.ic_back_arrow),
                    contentDescription=stringResource(R.string.back),
                    tint=MaterialTheme.appColors.editTextColor,
                    modifier=Modifier
                            .clickable {
                                onBackClick()
                            }
            )

            Spacer(modifier=Modifier.width(16.dp))

            Text(
                    text=title,
                    style=MaterialTheme.typography.titleLarge.copy(
                            color=MaterialTheme.appColors.editTextColor,
                            fontWeight=FontWeight.Bold,
                            fontSize=18.sp
                    ),
                    modifier=Modifier
                            .weight(1f)
            )

            actionItems()
        }

        Spacer(modifier=Modifier.height(24.dp))

        if (wrapInCard) {
            CardContainer(
                    modifier=Modifier
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

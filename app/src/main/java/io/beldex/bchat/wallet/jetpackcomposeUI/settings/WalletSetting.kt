package io.beldex.bchat.wallet.jetpackcomposeUI.settings

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.SharedPreferenceUtil.Companion.SELECTED_NODE_PREFS_NAME
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.R

enum class WalletSettingsItem(val title : Int) {
    DisplayBalance(R.string.display_balance_as), Decimals(R.string.decimals), Currency(R.string.currency), FeePriority(R.string.fee_priority),
}

enum class WalletSettingsNavItem(val title : Int) {
    CurrentNode(R.string.current_node), SaveRecipientAddress(R.string.save_recipient_address), ChangePin(R.string.change_pin)
}


@Composable
fun WalletSettingsScreen(navigate : (WalletSettingsItem) -> Unit, navItem : (WalletSettingsNavItem) -> Unit, saveRecipientAddress : Boolean, viewModel : WalletSettingViewModel, navController : NavHostController, selectedNode: String) {
    val scrollState=rememberScrollState()
    val context=LocalContext.current
    val lifecycleOwner=LocalLifecycleOwner.current

    val checkedState=remember { mutableStateOf(TextSecurePreferences.getSaveRecipientAddress(context)) }

    var displayBalanceSelectedItem by remember {
        mutableStateOf(TextSecurePreferences.getDisplayBalanceAs(context))
    }
    var decimalSelectedItem by remember {
        mutableStateOf(TextSecurePreferences.getDecimals(context))
    }
    var currencySelectedItem by remember {
        mutableStateOf(TextSecurePreferences.getCurrency(context))
    }
    var feePrioritySelectedItem by remember {
        mutableStateOf(TextSecurePreferences.getFeePriority(context))
    }

    val displayBalanceItemList=listOf("Beldex Full Balance", "Beldex Available Balance", "Beldex Hidden")
    val feePriorityItemList=listOf("Flash", "Slow")


    viewModel.displayBalance.observe(lifecycleOwner) { balance ->
        displayBalanceSelectedItem=balance

    }
    viewModel.decimal.observe(lifecycleOwner) { decimal ->
        decimalSelectedItem=decimal

    }
    viewModel.currency.observe(lifecycleOwner) { currency ->
        currencySelectedItem=currency

    }
    viewModel.feePriority.observe(lifecycleOwner) { priority ->
        feePrioritySelectedItem=priority

    }

    val currentSelectedNode by remember {
        mutableStateOf(selectedNode.split(":"))
    }

    fun getSelectedNodeId() : String? {
        return context.getSharedPreferences(SELECTED_NODE_PREFS_NAME, BaseActionBarActivity.MODE_PRIVATE).getString("0", null)
    }

    fun getNode() : String {
        val selectedNodeId=getSelectedNodeId()
        return selectedNodeId.toString()
    }
    Column(modifier=Modifier
            .verticalScroll(scrollState)
            .background(color=MaterialTheme.appColors.backgroundColor)) {
        Spacer(modifier=Modifier.height(16.dp))
        Text(text="Node", style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.primaryButtonColor, fontSize=16.sp, fontWeight=FontWeight(700)), modifier=Modifier.padding(horizontal=30.dp))
        Card(Modifier
                .fillMaxWidth()
                .padding(vertical=16.dp, horizontal=16.dp)
                .clickable {
                    navItem(WalletSettingsNavItem.CurrentNode)
                }, RoundedCornerShape(12.dp), CardDefaults.cardColors(containerColor=MaterialTheme.appColors.settingsCardBackground), CardDefaults.cardElevation(defaultElevation=0.dp)) {

            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center, modifier=Modifier.padding(vertical=16.dp, horizontal=24.dp)) {
                Icon(
                        painter=painterResource(id=R.drawable.ic_current_node),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                )

                Column(horizontalAlignment=Alignment.Start, verticalArrangement=Arrangement.Center, modifier=Modifier
                        .padding(10.dp)
                        .weight(0.7f)) {

                    Text(text=stringResource(id=R.string.current_node), style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.primaryButtonColor, fontSize=14.sp, fontWeight=FontWeight(400)))
                    Text(
                            text=currentSelectedNode[0].ifEmpty {
                                if (CheckOnline.isOnline(context)) {
                                    if (getNode().split(":")[0] != "null") getNode().split(":")[0]
                                    else context.getString(R.string.waiting_for_connection)
                                } else {
                                    context.getString(R.string.waiting_for_network)
                                }
                            },
                            style=BChatTypography.titleSmall.copy(color=MaterialTheme.appColors.editTextColor, fontSize=12.sp, fontWeight=FontWeight(400)), modifier=Modifier.padding(vertical=5.dp))
                }
                Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                )
            }
        }

        Text(
                text="Wallet",
                style=BChatTypography.titleMedium.copy(
                        color=MaterialTheme.appColors.primaryButtonColor,
                        fontSize=16.sp, fontWeight=FontWeight(700)),
                modifier=Modifier.padding(horizontal=30.dp))


        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.settingsCardBackground), shape=RoundedCornerShape(12.dp), elevation=CardDefaults.cardElevation(defaultElevation=0.dp), modifier=Modifier
                .fillMaxWidth()
                .padding(vertical=16.dp, horizontal=16.dp)

                .clickable {
                    /*changePin()*/
                }) {

            WalletSettingsItem.entries.forEach { item ->
                MyWalletSettingItem(title=stringResource(id=item.title), subTitle=when (item) {
                    WalletSettingsItem.DisplayBalance -> displayBalanceItemList[displayBalanceSelectedItem]
                    WalletSettingsItem.Decimals -> decimalSelectedItem
                    WalletSettingsItem.Currency -> currencySelectedItem
                    WalletSettingsItem.FeePriority -> feePriorityItemList[feePrioritySelectedItem]

                }!!, icon=when (item) {
                    WalletSettingsItem.DisplayBalance -> painterResource(id=R.drawable.ic_display_balance)
                    WalletSettingsItem.Decimals -> painterResource(id=R.drawable.ic_decimal)
                    WalletSettingsItem.Currency -> painterResource(id=R.drawable.ic_currency)
                    WalletSettingsItem.FeePriority -> painterResource(id=R.drawable.ic_fee_priority)
                }, drawDot=false, modifier=Modifier
                        .fillMaxWidth()
                        .padding(vertical=16.dp, horizontal=24.dp)
                        .clickable {
                            navigate(item)
                        })
            }

            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center, modifier=Modifier.padding(vertical=10.dp, horizontal=24.dp)) {
                Icon(
                        painter=painterResource(id=R.drawable.ic_save_recipient_address),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                )

                Text(
                        text=stringResource(id=R.string.save_recipient_address),
                        style=BChatTypography.titleMedium.copy(
                                color=MaterialTheme.appColors.editTextColor, fontSize=14.sp, fontWeight=FontWeight(400)),
                        modifier=Modifier
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 14.dp)
                        .weight(0.7f))
                Switch(checked=checkedState.value, onCheckedChange={
                    if (checkedState.value) {
                        TextSecurePreferences.setSaveRecipientAddress(context, it)
                    } else {
                        TextSecurePreferences.setSaveRecipientAddress(context, it)
                    }
                    checkedState.value=it
                }, colors=SwitchDefaults.colors(checkedTrackColor=MaterialTheme.appColors.switchTrackColor, uncheckedTrackColor=MaterialTheme.appColors.switchTrackColor, checkedBorderColor=MaterialTheme.appColors.switchTrackColor, uncheckedBorderColor=MaterialTheme.appColors.switchTrackColor, checkedThumbColor=MaterialTheme.appColors.primaryButtonColor, uncheckedThumbColor=Color.Gray

                ), modifier=Modifier.clickable {
                    navItem(WalletSettingsNavItem.SaveRecipientAddress)
                })

            }
        }
        Text(text="Personal", style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.primaryButtonColor, fontSize=16.sp, fontWeight=FontWeight(700)), modifier=Modifier.padding(horizontal=30.dp))
        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.settingsCardBackground), shape=RoundedCornerShape(12.dp), elevation=CardDefaults.cardElevation(defaultElevation=0.dp), modifier=Modifier
                .fillMaxWidth()
                .padding(vertical=16.dp, horizontal=16.dp)
                .clickable {
                    /*changePin()*/
                }) {

            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center, modifier=Modifier
                    .padding(vertical=16.dp, horizontal=24.dp)
                    .clickable {
                        addressBook(context)
                    }) {
                Icon(
                        painter=painterResource(id=R.drawable.ic_addressbook),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                )

                Text(text=stringResource(id=R.string.activity_address_book_page_title), style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.editTextColor, fontSize=14.sp, fontWeight=FontWeight(400)), modifier=Modifier
                        .padding(10.dp)
                        .weight(0.7f))
                Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                )
            }
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center, modifier=Modifier
                    .padding(vertical=16.dp, horizontal=24.dp)
                    .clickable {
                        navItem(WalletSettingsNavItem.ChangePin)
                    }) {
                Icon(
                        painter=painterResource(id=R.drawable.ic_change_pin),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                )

                Text(text=stringResource(id=R.string.change_pin), style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.editTextColor,  fontSize=14.sp, fontWeight=FontWeight(400)), modifier=Modifier
                        .padding(10.dp)
                        .weight(0.7f))
                Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                )
            }
        }

    }
}

fun addressBook(context : Context) {
    TextSecurePreferences.setSendAddressDisable(context,true)
    val intent=Intent(context, WalletSettingComposeActivity::class.java).apply {
        putExtra(WalletSettingComposeActivity.extraStartDestination, WalletSettingScreens.AddressBookScreen.route)
    }
    context.startActivity(intent)
}

@Composable
private fun MyWalletSettingItem(title : String, subTitle : Any, icon : Painter, drawDot : Boolean, modifier : Modifier=Modifier) {


    Row(modifier=modifier) {
        Icon(painter=icon, contentDescription="", tint=MaterialTheme.appColors.editTextColor, modifier=Modifier.align(Alignment.CenterVertically))

        Spacer(modifier=Modifier.width(16.dp))

        Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.weight(0.7f)) {
            Column(horizontalAlignment=Alignment.Start, verticalArrangement=Arrangement.Center) {

                Text(text=title, style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.editTextColor, fontSize=14.sp, fontWeight=FontWeight(400)))
                Text(text=subTitle.toString(), style=BChatTypography.titleSmall.copy(
                        color=Color(0xACACACAC),
                        fontSize=14.sp,
                        fontWeight=FontWeight(600),
                ), modifier=Modifier.padding(vertical=5.dp))
            }
            if (drawDot) {
                Spacer(modifier=Modifier.width(8.dp))

                Box(modifier=Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color=MaterialTheme.appColors.primaryButtonColor))
            }
        }

        Icon(Icons.Default.KeyboardArrowRight, contentDescription="", tint=MaterialTheme.appColors.editTextColor)
    }
}

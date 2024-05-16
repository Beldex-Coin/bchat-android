package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.settings

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.compose_utils.BChatOutlinedTextField
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import io.beldex.bchat.R

@Composable
fun DisplayBalanceDialog(onDismiss : () -> Unit, onClick : (Int?) -> Unit) {
    val itemList=listOf("Beldex Full Balance", "Beldex Available Balance", "Beldex Hidden")
    val context=LocalContext.current
    var selectedItemIndex by remember {
        mutableStateOf(TextSecurePreferences.getDisplayBalanceAs(context))
    }
    val isDarkTheme=UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT



    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest=onDismiss,
    ) {
        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground),
                elevation=CardDefaults.cardElevation(defaultElevation=4.dp),
                modifier=Modifier.fillMaxWidth()) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {

                Row(modifier = Modifier.padding(bottom = 20.dp)){
                    Text(text=stringResource(id=R.string.display_balance_as),
                        style=MaterialTheme.typography.titleMedium.copy(
                            fontSize=20.sp,
                            fontWeight=FontWeight(700),
                            color=MaterialTheme.appColors.primaryButtonColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically))

                    Icon(
                        painter=painterResource(id=R.drawable.ic_close),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                        modifier= Modifier
                            .clickable {
                                onDismiss()
                            }
                    )
                }

                LazyColumn(
                        verticalArrangement=Arrangement.spacedBy(16.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        modifier=Modifier
                                .fillMaxWidth()

                ) {
                    itemsIndexed(itemList) { index, item ->
                        Card(
                                colors=CardDefaults.cardColors(
                                        containerColor=MaterialTheme.appColors.changeLogBackground
                                ),
                                border=BorderStroke(
                                        width=2.dp,
                                        color=if (index == selectedItemIndex) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.colorScheme.outline
                                ),
                                elevation=CardDefaults.cardElevation(
                                        defaultElevation=if (isDarkTheme) 0.dp else 4.dp
                                ),
                                modifier= Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItemIndex = index
                                        onClick(selectedItemIndex)
                                    },
                                shape=RoundedCornerShape(16.dp)
                        ) {

                            Column(
                                    modifier= Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 5.dp),
                                    verticalArrangement=Arrangement.Center,
                                    horizontalAlignment=Alignment.CenterHorizontally,
                            ) {

                                Text(text=item, style=MaterialTheme.typography.titleMedium.copy(
                                        color=MaterialTheme.appColors.secondaryTextColor, fontSize=16.sp, fontWeight=FontWeight(600)
                                ), modifier=Modifier.padding(10.dp))
                            }
                        }

                    }
                }
            }
        }
    }

}

@Composable
fun DecimalDialog(onDismiss : () -> Unit, onClick : (String?) -> Unit) {
    val itemList=listOf("4 - Four (0.0000)", "3 - Three (0.000)", "2 - Two (0.00)", "0 - Zero (0)")
    val context=LocalContext.current
    var selectedItemIndex by remember {
        mutableStateOf(TextSecurePreferences.getDecimals(context))
    }
    val isDarkTheme=UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT

    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest=onDismiss,
    ) {
        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground),
                elevation=CardDefaults.cardElevation(defaultElevation=4.dp),
                modifier=Modifier.fillMaxWidth()) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {

                Row(modifier = Modifier.padding(bottom = 20.dp)){
                    Text(text=stringResource(id=R.string.decimals),
                        style=MaterialTheme.typography.titleMedium.copy(
                            fontSize=20.sp,
                            fontWeight=FontWeight(700),
                            color=MaterialTheme.appColors.primaryButtonColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically))

                    Icon(
                        painter=painterResource(id=R.drawable.ic_close),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                        modifier= Modifier
                            .clickable {
                                onDismiss()
                            }
                    )
                }

                LazyColumn(
                        verticalArrangement=Arrangement.spacedBy(16.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        modifier=Modifier
                                .fillMaxWidth()

                ) {
                    itemsIndexed(itemList) { index, item ->
                        Card(
                                colors=CardDefaults.cardColors(
                                        containerColor=MaterialTheme.appColors.changeLogBackground
                                ),
                                border=BorderStroke(
                                        width=2.dp,
                                        color=if (item == selectedItemIndex) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.colorScheme.outline
                                ),
                                elevation=CardDefaults.cardElevation(
                                        defaultElevation=if (isDarkTheme) 0.dp else 4.dp
                                ),
                                modifier= Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItemIndex = item
                                        onClick(selectedItemIndex)
                                    },
                                shape=RoundedCornerShape(16.dp)
                        ) {

                            Column(
                                    modifier= Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 5.dp),
                                    verticalArrangement=Arrangement.Center,
                                    horizontalAlignment=Alignment.CenterHorizontally,
                            ) {

                                Text(text=item, style=MaterialTheme.typography.titleMedium.copy(
                                        color=MaterialTheme.appColors.secondaryTextColor, fontSize=16.sp, fontWeight=FontWeight(600)
                                ), modifier=Modifier.padding(10.dp))
                            }
                        }

                    }
                }
            }
        }
    }

}

@Composable
fun CurrencyDialog(onDismiss : () -> Unit, onClick : (String?) -> Unit) {
    val currencyList : MutableList<String> =ArrayList()

    currencyList.add("AUD")
    currencyList.add("BRL")
    currencyList.add("CAD")
    currencyList.add("CHF")
    currencyList.add("CNY")
    currencyList.add("CZK")
    currencyList.add("EUR")
    currencyList.add("DKK")
    currencyList.add("GBP")
    currencyList.add("HKD")
    currencyList.add("HUF")
    currencyList.add("IDR")
    currencyList.add("ILS")
    currencyList.add("INR")
    currencyList.add("JPY")
    currencyList.add("KRW")
    currencyList.add("MXN")
    currencyList.add("MYR")
    currencyList.add("NOK")
    currencyList.add("NZD")
    currencyList.add("PHP")
    currencyList.add("PLN")
    currencyList.add("RUB")
    currencyList.add("SEK")
    currencyList.add("SGD")
    currencyList.add("THB")
    currencyList.add("USD")
    currencyList.add("VEF")
    currencyList.add("ZAR")

    var searchQuery by remember {
        mutableStateOf("")
    }

    var fiatCurrencyList by remember {
        mutableStateOf(currencyList)
    }

    val context=LocalContext.current
    var selectedItemIndex by remember {
        mutableStateOf(TextSecurePreferences.getCurrency(context))
    }
    val isDarkTheme=UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT

    fun filterFiatCurrency() {
        fiatCurrencyList = currencyList.filter {
            if(searchQuery.isNotEmpty()){
                it.lowercase().contains(searchQuery.lowercase())
            }else{
                it.isNotEmpty()
            }
        }.toMutableList()
    }

    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest=onDismiss,
    ) {
        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground),
                elevation=CardDefaults.cardElevation(defaultElevation=4.dp),
                modifier=Modifier.fillMaxWidth()) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {

                Row(modifier = Modifier.padding(bottom = 20.dp)){
                    Text(text=stringResource(id=R.string.currency),
                        style=MaterialTheme.typography.titleMedium.copy(
                            fontSize=20.sp,
                            fontWeight=FontWeight(700),
                            color=MaterialTheme.appColors.primaryButtonColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically))

                    Icon(
                        painter=painterResource(id=R.drawable.ic_close),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                        modifier= Modifier
                            .clickable {
                                onDismiss()
                            }
                    )
                }

                BChatOutlinedTextField(
                        value =searchQuery,
                        onValueChange ={
                            searchQuery=it
                            filterFiatCurrency()
                        },
                        placeHolder =stringResource(R.string.search_currency),
                        shape =RoundedCornerShape(36.dp),
                        trailingIcon ={
                            IconButton(onClick = {
                                if(searchQuery.isNotEmpty()){
                                    searchQuery = ""
                                    filterFiatCurrency()
                                }
                            }) {
                                Icon(
                                    imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                                    contentDescription = "Search and Clear Icon",
                                    tint = MaterialTheme.appColors.iconTint,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                )
                Row(
                        modifier= Modifier
                            .height(300.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)

                ) {


                    LazyColumn(
                            verticalArrangement=Arrangement.SpaceBetween,
                            horizontalAlignment=Alignment.CenterHorizontally,
                            modifier= Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .weight(1f)

                    ) {
                        itemsIndexed(fiatCurrencyList) { index, item ->

                            Column(
                                    modifier= Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            selectedItemIndex = item
                                            onClick(selectedItemIndex)
                                        },
                                    verticalArrangement=Arrangement.Center,
                                    horizontalAlignment=Alignment.CenterHorizontally,
                            ) {

                                Text(text=item, style=MaterialTheme.typography.titleMedium.copy(
                                        color=if (selectedItemIndex == item) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.secondaryTextColor,
                                        fontSize=16.sp,
                                        fontWeight=FontWeight(600),
                                        textAlign=TextAlign.Center
                                ), modifier=Modifier.padding(vertical=20.dp, horizontal=0.dp))

                                Divider(modifier= Modifier
                                    .width(150.dp)
                                    .padding(0.dp), color=MaterialTheme.appColors.dividerColor)
                            }
                        }

                    }
                    //BoxWithConstraints(fiatCurrencyList,300)
                   /* Box(
                            modifier=Modifier
                                    .height(300.dp)
                                    .padding(all=10.dp)
                                    .background(color=MaterialTheme.appColors.scrollBackground,
                                            shape=RoundedCornerShape(10.dp)),
                            contentAlignment=Alignment.TopCenter


                    ) {
                        // BoxWithConstraints(currencyList)
                    }*/

                }

            }
        }
    }

}

@Composable
fun BoxWithConstraints(fiatCurrencyList: MutableList<String>, dp: Int) {
    val scrollState = rememberScrollState()
    val viewMaxHeight = fiatCurrencyList.size
    val columnMaxScroll = scrollState.maxValue
    val scrollStateValue = scrollState.value
    val paddingSize = (scrollStateValue * viewMaxHeight) / columnMaxScroll
    val animation = animateDpAsState(targetValue = paddingSize.dp)

    Column(
        Modifier
            .verticalScroll(state = scrollState)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (scrollStateValue < columnMaxScroll) {
            Box(
                modifier = Modifier
                    .paddingFromBaseline(animation.value)
                    .padding(all = 4.dp)
                    .height(300.dp)
                    .width(4.dp)
                    .background(
                        color = MaterialTheme.appColors.primaryButtonColor,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {}
        }
    }
}

/*@Composable
fun BoxWithConstraints(items : List<String>) {
    val scrollState=rememberScrollState(1)
    val viewMaxHeight=1
    val columnMaxScroll=scrollState.maxValue
    val scrollStateValue=scrollState.value
    val paddingSize=(scrollStateValue * viewMaxHeight) / columnMaxScroll
    val animation=animateDpAsState(targetValue=paddingSize.dp, label="")


    Column(
            Modifier
                    .verticalScroll(state=scrollState),
            verticalArrangement=Arrangement.spacedBy(8.dp),
            horizontalAlignment=Alignment.CenterHorizontally
    ) {
        if (scrollStateValue < columnMaxScroll) {
            Box(
                    modifier=Modifier
                            .paddingFromBaseline(animation.value)
                            .padding(all=4.dp)
                            .height(50.dp)
                            .width(5.dp)
                            .background(
                                    color=MaterialTheme.appColors.primaryButtonColor,
                                    shape=RoundedCornerShape(40.dp)
                            )
                            .align(Alignment.CenterHorizontally),
            ) {}
        }
    }
}*/

@Composable
fun FeePriorityDialog(onDismiss : () -> Unit, onClick : (Int?) -> Unit) {
    val itemList=listOf("Flash", "Slow")
    val context=LocalContext.current
    var selectedItemIndex by remember {
        mutableStateOf(TextSecurePreferences.getFeePriority(context))
    }
    val isDarkTheme=UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT

    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest=onDismiss,
    ) {
        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground),
                elevation=CardDefaults.cardElevation(defaultElevation=4.dp),
                modifier=Modifier.fillMaxWidth()) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {

                Row(modifier = Modifier.padding(bottom = 20.dp)){
                    Text(text=stringResource(id=R.string.fee_priority),
                        style=MaterialTheme.typography.titleMedium.copy(
                            fontSize=20.sp,
                            fontWeight=FontWeight(700),
                            color=MaterialTheme.appColors.primaryButtonColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically))

                    Icon(
                        painter=painterResource(id=R.drawable.ic_close),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                        modifier= Modifier
                            .clickable {
                                onDismiss()
                            }
                    )
                }

                LazyColumn(
                        verticalArrangement=Arrangement.spacedBy(16.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        modifier=Modifier
                                .fillMaxWidth()

                ) {
                    itemsIndexed(itemList) { index, item ->
                        Card(
                                colors=CardDefaults.cardColors(
                                        containerColor=MaterialTheme.appColors.changeLogBackground
                                ),
                                border=BorderStroke(
                                        width=2.dp,
                                        color=if (index == selectedItemIndex) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.colorScheme.outline
                                ),
                                elevation=CardDefaults.cardElevation(
                                        defaultElevation=if (isDarkTheme) 0.dp else 4.dp
                                ),
                                modifier= Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItemIndex = index
                                        onClick(selectedItemIndex)
                                    },
                                shape=RoundedCornerShape(16.dp)
                        ) {

                            Column(
                                    modifier= Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 5.dp),
                                    verticalArrangement=Arrangement.Center,
                                    horizontalAlignment=Alignment.CenterHorizontally,
                            ) {

                                Text(text=item, style=MaterialTheme.typography.titleMedium.copy(
                                        color=MaterialTheme.appColors.secondaryTextColor, fontSize=16.sp, fontWeight=FontWeight(600)
                                ), modifier=Modifier.padding(10.dp))
                            }
                        }

                    }
                }
            }
        }
    }

}


@Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DisplayBalanceDialogPreview() {
    BChatTheme {
        DisplayBalanceDialog(onDismiss={}) {}
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_NO)
@Composable
fun DisplayBalanceDialogLightPreview() {
    BChatTheme() {
        DisplayBalanceDialog(onDismiss={}) {}
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DecimalDialogPreview() {
    BChatTheme {
        DecimalDialog(onDismiss={}, onClick={})
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_NO)
@Composable
fun DecimaleDialogLightPreview() {
    BChatTheme() {
        DecimalDialog(onDismiss={}, onClick={})
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CurrencyDialogPreview() {
    BChatTheme {
        CurrencyDialog(onDismiss={}, onClick={})
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CurrencyDialogLightPreview() {
    BChatTheme() {
        CurrencyDialog(onDismiss={}, onClick={})
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FeePriorityDialogPreview() {
    BChatTheme {
        FeePriorityDialog(onDismiss={}, onClick={})
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_NO)
@Composable
fun FeePriorityDialogLightPreview() {
    BChatTheme() {
        FeePriorityDialog(onDismiss={}, onClick={})
    }
}

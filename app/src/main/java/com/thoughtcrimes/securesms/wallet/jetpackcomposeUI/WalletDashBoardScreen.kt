package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.copyToClipBoard
import com.thoughtcrimes.securesms.wallet.WalletFragment
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.walletdashboard.FilterTransactionByDatePopUp
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDashBoardScreen(
    viewModels: WalletViewModels,
    activityCallback: WalletFragment.Listener,
    onScanListener: WalletFragment.OnScanListener?
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    val subDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
    val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy, HH:mm:ss a", Locale.US)

    var incomingTransactionIsChecked by remember {
        mutableStateOf(true)
    }

    var outgoingTransactionIsChecked by remember {
        mutableStateOf(true)
    }

    var expandHistory by remember {
        mutableStateOf(false)
    }

    val transactionInfo: TransactionInfo? = null
    var transactionInfoItem by remember {
        mutableStateOf(transactionInfo)
    }

    var sendCardViewButtonIsEnabled by remember {
        mutableStateOf(false)
    }

    viewModels.sendCardViewButtonIsEnabled.observe(lifecycleOwner) {
        sendCardViewButtonIsEnabled = it
    }

    var scanQRCodeButtonIsEnabled by remember {
        mutableStateOf(false)
    }

    viewModels.scanQRCodeButtonIsEnabled.observe(lifecycleOwner) {
        scanQRCodeButtonIsEnabled = it
    }

    var syncStatusTextColor by remember {
        mutableStateOf(R.color.icon_tint)
    }

    viewModels.syncStatusTextColor.observe(lifecycleOwner) {
        syncStatusTextColor = it
    }

    var progressBarColor by remember {
        mutableIntStateOf(R.color.green_color)
    }

    viewModels.progressBarColor.observe(lifecycleOwner) {
        progressBarColor = it
    }

    var progress by remember {
        mutableFloatStateOf(0f)
    }

    viewModels.progress.observe(lifecycleOwner) {
        progress = it
    }

    var sendCardViewButtonIsClickable by remember {
        mutableStateOf(false)
    }

    viewModels.sendCardViewButtonIsClickable.observe(lifecycleOwner) {
        sendCardViewButtonIsClickable = it
    }

    var receiveCardViewButtonIsClickable by remember {
        mutableStateOf(false)
    }

    viewModels.receiveCardViewButtonIsClickable.observe(lifecycleOwner) {
        receiveCardViewButtonIsClickable = it
    }

    var receiveCardViewButtonIsEnabled by remember {
        mutableStateOf(false)
    }

    viewModels.receiveCardViewButtonIsEnabled.observe(lifecycleOwner) {
        receiveCardViewButtonIsEnabled = it
    }

    var syncStatus by remember {
        mutableStateOf("")
    }

    viewModels.syncStatus.observe(lifecycleOwner) {
        if (it != null) {
            syncStatus = it
        }
    }

    var transactionListContainerIsVisible by remember {
        mutableStateOf(false)
    }

    viewModels.transactionListContainerIsVisible.observe(lifecycleOwner) {
        transactionListContainerIsVisible = it
    }

    var filterTransactionIconIsClickable by remember {
        mutableStateOf(false)
    }

    viewModels.filterTransactionIconIsClickable.observe(lifecycleOwner) {
        filterTransactionIconIsClickable = it
    }

    var walletBalance by remember {
        mutableStateOf("-.----")
    }

    viewModels.walletBalance.observe(lifecycleOwner) {
        walletBalance = it
    }

    var fiatCurrencyPrice: String by remember {
        mutableStateOf("---")
    }

    viewModels.fiatCurrency.observe(lifecycleOwner) {
        fiatCurrencyPrice = it
    }

    var transactionListMap = remember { mutableMapOf<String, List<TransactionInfo>>() }

    var byDateIsEnable by remember {
        mutableStateOf(false)
    }

    val emptySelectedDateList: MutableList<Date> = ArrayList()

    var selectedDates by remember {
        mutableStateOf<List<Date>>(emptySelectedDateList)
    }

    var showFilterOptions by remember {
        mutableStateOf(false)
    }

    viewModels.transactionInfoItems.observe(lifecycleOwner) { list ->
        val filterByDirectionList = list.filter {
            if (incomingTransactionIsChecked && outgoingTransactionIsChecked) {
                it.direction == TransactionInfo.Direction.Direction_In || it.direction == TransactionInfo.Direction.Direction_Out
            } else if (incomingTransactionIsChecked) {
                it.direction == TransactionInfo.Direction.Direction_In
            } else if (outgoingTransactionIsChecked) {
                it.direction == TransactionInfo.Direction.Direction_Out
            } else {
                it.direction != TransactionInfo.Direction.Direction_In || it.direction != TransactionInfo.Direction.Direction_Out
            }
        }

        val filterList = if (byDateIsEnable) {
            val temp: MutableList<TransactionInfo> = ArrayList()
            for (datesItem in selectedDates) {
                for (d in filterByDirectionList) {
                    if (dateFormat.format(Date(d.timestamp * 1000)) == dateFormat.format(datesItem)) {
                        temp.add(d)
                    }
                }
            }
            temp
        } else {
            filterByDirectionList
        }

        callIfTransactionListEmpty(filterList.size, viewModels, showFilter = {
            showFilterOptions = it
        })

        transactionListMap = filterList.groupBy {
            convertTimeStampToDateString(it.timestamp, dateFormat)
        }.toMutableMap()
    }

    var progressBarIsVisible by remember {
        mutableStateOf(true)
    }

    viewModels.progressBarIsVisible.observe(lifecycleOwner) {
        progressBarIsVisible = it
    }

    var showFilterTransactionByDatePopUp by remember {
        mutableStateOf(false)
    }

    if (showFilterTransactionByDatePopUp) {
        FilterTransactionByDatePopUp(
            onDismiss = {
                //byDateIsEnable = false
                showFilterTransactionByDatePopUp = false
            },
            selectedDates = { selectedDateList, list ->
                byDateIsEnable = true
                selectedDates = selectedDateList.toMutableList()
                viewModels.updateTransactionInfoItems(list)
                showFilterTransactionByDatePopUp = false
            },
            context,
            viewModels,
        )
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.fetch_balance))
    val isPlaying by remember {
        mutableStateOf(true)
    }
    // for speed
    val speed by remember {
        mutableFloatStateOf(1f)
    }

    val lottieProgress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever,isPlaying = isPlaying, speed = speed, restartOnPlay = false)

    var fetchBalanceStatus by remember {
        mutableStateOf(false)
    }

    viewModels.fetchBalanceStatus.observe(lifecycleOwner) {
        fetchBalanceStatus = it
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.my_wallet),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.appColors.editTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        activityCallback.walletOnBackPressed()
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = stringResource(
                                id = R.string.back
                            ),
                            tint = MaterialTheme.appColors.editTextColor,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.primary),
                actions = {
                    IconButton(onClick = {
                        activityCallback.callToolBarSettings()
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_wallet_dashboard_setting),
                            "wallet settings",
                            tint = MaterialTheme.appColors.editTextColor
                        )
                    }
                }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(it)
            )
            {

                Box {

                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                            .background(
                                MaterialTheme.appColors.walletDashboardShowBalanceCardBackground,
                                shape = RoundedCornerShape(18.dp)
                            )

                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {

                            Text(
                                text = syncStatus,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    colorResource(id = syncStatusTextColor),
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier
                                    .padding(5.dp)
                            )

                            Text(
                                text = fiatCurrencyPrice,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.appColors.restoreDescColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )

                        }

                        if (progressBarIsVisible) {
                            if (progress == 2f) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp),
                                    trackColor = MaterialTheme.appColors.textFieldUnfocusedColor,
                                    color = colorResource(id = progressBarColor)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp),
                                    progress = progress,
                                    trackColor = MaterialTheme.appColors.textFieldUnfocusedColor,
                                    color = colorResource(id = progressBarColor)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 50.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            Row(
                                 verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_beldex),
                                    contentDescription = "beldex logo",
                                    colorFilter = ColorFilter.tint(
                                        color = if (sendCardViewButtonIsEnabled) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.textColor
                                    )
                                )

                                Text(
                                    text = walletBalance,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.appColors.onMainContainerTextColor,
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .padding(10.dp)
                                )
                            }
                            if (fetchBalanceStatus) {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        stringResource(id = R.string.fetching_balance),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.appColors.editTextHint,
                                            fontSize = 12.sp
                                        ),
                                    )
                                    LottieAnimation(
                                        composition, lottieProgress, modifier = Modifier
                                            .width(10.dp)
                                            .height(5.dp)
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.padding(top = 55.dp)) {

                        Column(
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 30.dp,
                                    top = 80.dp,
                                    end = 30.dp,
                                    bottom = 0.dp
                                )
                                .background(
                                    MaterialTheme.appColors.walletDashboardMainMenuCardBackground,
                                    shape = RoundedCornerShape(18.dp)
                                )

                        ) {


                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                            ) {
                                FilledIconButton(
                                    modifier = Modifier.size(47.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.appColors.walletDashboardQRButtonBackground,
                                        contentColor = Color.Black,
                                        disabledContainerColor = MaterialTheme.appColors.disableWalletButtonColor,
                                        disabledContentColor = MaterialTheme.appColors.disableWalletButtonContentColor
                                    ),
                                    enabled = scanQRCodeButtonIsEnabled,
                                    onClick = {
                                        onScanListener?.onWalletScan()
                                    }) {
                                    Image(
                                        painter = painterResource(id = R.drawable.wallet_scan),
                                        contentDescription = stringResource(id = R.string.scan_qrcode_text),
                                        modifier = Modifier.padding(10.dp),
                                        colorFilter = ColorFilter.tint(color = if (scanQRCodeButtonIsEnabled) Color.Black else MaterialTheme.appColors.disableWalletButtonContentColor)
                                    )
                                }
                                FilledIconButton(
                                    modifier = Modifier.size(47.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.appColors.walletDashboardSendButtonBackground,
                                        contentColor = Color.White,
                                        disabledContainerColor = MaterialTheme.appColors.disableWalletButtonColor,
                                        disabledContentColor = MaterialTheme.appColors.disableWalletButtonContentColor
                                    ),
                                    enabled = sendCardViewButtonIsEnabled,
                                    onClick = {
                                        if (sendCardViewButtonIsClickable) {
                                            activityCallback.onSendRequest()
                                        }
                                    }) {
                                    Image(
                                        painter = painterResource(id = R.drawable.wallet_send),
                                        contentDescription = stringResource(id = R.string.send),
                                        modifier = Modifier.padding(10.dp),
                                        colorFilter = ColorFilter.tint(color = if (sendCardViewButtonIsEnabled) Color.White else MaterialTheme.appColors.disableWalletButtonContentColor)
                                    )
                                }
                                FilledIconButton(
                                    modifier = Modifier.size(47.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.appColors.walletDashboardReceiveButtonBackground,
                                        contentColor = Color.White,
                                    ),
                                    onClick = {
                                        if (receiveCardViewButtonIsClickable) {
                                            activityCallback.onWalletReceive()
                                        }
                                    }) {
                                    Image(
                                        painter = painterResource(id = R.drawable.wallet_receive),
                                        contentDescription = stringResource(id = R.string.activity_receive_page_title),
                                        modifier = Modifier.padding(10.dp),

                                        )
                                }
                                FilledIconButton(
                                    modifier = Modifier.size(47.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.appColors.walletDashboardRescanButtonBackground,
                                        contentColor = MaterialTheme.appColors.titleTextColor,
                                        disabledContainerColor = MaterialTheme.appColors.disableWalletButtonColor,
                                        disabledContentColor = MaterialTheme.appColors.disableWalletButtonContentColor
                                    ),
                                    enabled = activityCallback.isSynced,
                                    onClick = {
                                        if (activityCallback.isSynced) {
                                            activityCallback.callToolBarRescan()
                                        } else {
                                            Toast
                                                .makeText(
                                                    context,
                                                    context.getString(R.string.cannot_access_sync_option),
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    }) {
                                    Image(
                                        painter = painterResource(id = R.drawable.wallet_rescan),
                                        contentDescription = stringResource(id = R.string.menu_rescan),
                                        modifier = Modifier.padding(10.dp),
                                        colorFilter = ColorFilter.tint(color = if (activityCallback.isSynced) MaterialTheme.appColors.titleTextColor else MaterialTheme.appColors.disableWalletButtonContentColor)
                                    )
                                }
                            }
                        }

                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()

                ) {

                    Text(
                        text = stringResource(id = R.string.transactions),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.appColors.onMainContainerTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    IconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (showFilterOptions) MaterialTheme.appColors.transactionFilterBackground else Color.Transparent,
                            contentColor = if (showFilterOptions) MaterialTheme.appColors.transactionFilterIcon else MaterialTheme.appColors.disableButtonContentColor,
                        ),
                        onClick = {
                            if (filterTransactionIconIsClickable) {
                                showFilterOptions = !showFilterOptions
                            }
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.transaction_filter_disable),
                            contentDescription = stringResource(id = R.string.transaction_filter),
                            colorFilter = ColorFilter.tint(color = if (showFilterOptions) MaterialTheme.appColors.transactionFilterIcon else MaterialTheme.appColors.transactionFilterEnableColor),
                        )
                    }
                }

                if (transactionListContainerIsVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.appColors.transactionHistoryCardBackground,
                                shape = RoundedCornerShape(10.dp, 10.dp, 0.dp, 0.dp),
                            ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 10.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.appColors.transactionHistoryCardBackground,
                                ),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!expandHistory && showFilterOptions) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Checkbox(
                                                checked = incomingTransactionIsChecked,
                                                onCheckedChange = { isChecked ->
                                                    incomingTransactionIsChecked = isChecked
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.filter_applied),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    if (outgoingTransactionIsChecked && incomingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    } else if (incomingTransactionIsChecked && !outgoingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    } else if (!incomingTransactionIsChecked && outgoingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    } else if (!outgoingTransactionIsChecked && !incomingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MaterialTheme.appColors.primaryButtonColor,
                                                    uncheckedColor = MaterialTheme.appColors.onMainContainerTextColor,
                                                    checkmarkColor = MaterialTheme.appColors.onMainContainerTextColor
                                                )
                                            )

                                            Text(
                                                text = stringResource(id = R.string.incoming),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.appColors.transactionTypeTitle,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )

                                        }
                                        Image(
                                            painter = painterResource(id = R.drawable.divider_dot),
                                            contentDescription = "divider circle"
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = outgoingTransactionIsChecked,
                                                onCheckedChange = { isChecked ->
                                                    outgoingTransactionIsChecked = isChecked
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.filter_applied),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    if (incomingTransactionIsChecked && outgoingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    } else if (outgoingTransactionIsChecked && !incomingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    } else if (!outgoingTransactionIsChecked && incomingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    } else if (!incomingTransactionIsChecked && !outgoingTransactionIsChecked) {
                                                        viewModels.transactionInfoItems.value?.let { list ->
                                                            viewModels.updateTransactionInfoItems(
                                                                list
                                                            )
                                                        }
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MaterialTheme.appColors.primaryButtonColor,
                                                    uncheckedColor = MaterialTheme.appColors.onMainContainerTextColor,
                                                    checkmarkColor = MaterialTheme.appColors.onMainContainerTextColor
                                                )
                                            )

                                            Text(
                                                text = stringResource(id = R.string.outgoing),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.appColors.transactionTypeTitle,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                            )

                                        }
                                        Image(
                                            painter = painterResource(id = R.drawable.divider_dot),
                                            contentDescription = "divider circle"
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable(
                                                onClick = {
                                                    showFilterTransactionByDatePopUp =
                                                        !showFilterTransactionByDatePopUp
                                                }
                                            )
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.by_date_filter),
                                                contentDescription = "Filter By Date",
                                                colorFilter = ColorFilter.tint(MaterialTheme.appColors.onMainContainerTextColor)
                                            )

                                            Text(
                                                text = stringResource(id = R.string.by_date),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.appColors.transactionTypeTitle,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                            )

                                        }

                                    }
                                    Divider(
                                        color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                }

                                if (expandHistory) {
                                    Row(
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(5.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_transaction_history_arrow_back),
                                            contentDescription = "back arrow",
                                            tint = MaterialTheme.appColors.transactionHistoryArrowBackIconColor,
                                            modifier = Modifier
                                                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                                                .clickable(
                                                    onClick = {
                                                        expandHistory = false
                                                    }
                                                )
                                        )

                                        Text(
                                            text = stringResource(R.string.details),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(end = 10.dp, top = 10.dp, bottom = 10.dp),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = MaterialTheme.appColors.transactionHistoryArrowBackIconColor,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Divider(
                                        color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                }
                            }
                            if (!expandHistory) {

                                LazyColumn(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                ) {
                                    transactionListMap.forEach {
                                        item {
                                            CategoryHeader(it.key, modifier = Modifier, dateFormat)
                                        }
                                        items(it.value) { infoItems ->
                                            val isBNS = infoItems.isBns
                                            val displayAmount: String = Helper.getDisplayAmount(
                                                infoItems.amount,
                                                Helper.DISPLAY_DIGITS_INFO
                                            )
                                            val transactionStatusIcon: Int
                                            var transactionStatus: String? = ""
                                            var transactionAmount: String? = ""
                                            var transactionAmountTextColor: Int
                                            var transactionDate: String? = ""
                                            if (isBNS) {
                                                transactionStatusIcon =
                                                    R.drawable.bns_transaction
                                                transactionStatus =
                                                    context.getString(R.string.tx_status_sent)
                                                if (displayAmount > 0.toString()) {
                                                    transactionAmount = context.getString(
                                                        R.string.tx_list_amount_negative,
                                                        displayAmount
                                                    )
                                                    transactionAmountTextColor =
                                                        R.color.wallet_send_button
                                                }
                                            } else {
                                                if (infoItems.direction === TransactionInfo.Direction.Direction_Out) {
                                                    transactionStatus =
                                                        context.getString(R.string.tx_status_sent)
                                                    transactionStatusIcon =
                                                        R.drawable.ic_wallet_send_button
                                                    if (displayAmount > 0.toString()) {
                                                        transactionAmount = context.getString(
                                                            R.string.tx_list_amount_negative,
                                                            displayAmount
                                                        )
                                                        transactionAmountTextColor =
                                                            R.color.wallet_send_button
                                                    }
                                                } else {
                                                    transactionStatus =
                                                        context.getString(R.string.tx_status_received)
                                                    transactionStatusIcon =
                                                        R.drawable.ic_wallet_receive_button
                                                    if (displayAmount > 0.toString()) {
                                                        transactionAmount = context.getString(
                                                            R.string.tx_list_amount_positive,
                                                            displayAmount
                                                        )
                                                        transactionAmountTextColor =
                                                            R.color.wallet_receive_button
                                                    }
                                                }
                                            }
                                            transactionDate =
                                                getDateTime(infoItems.timestamp, subDateFormat)

                                            if (infoItems.isFailed) {
                                                transactionAmount = context.getString(
                                                    R.string.tx_list_amount_failed,
                                                    displayAmount
                                                )
                                                transactionAmountTextColor =
                                                    R.color.tx_failed
                                            } else if (infoItems.isPending) {
                                                transactionAmountTextColor =
                                                    R.color.tx_pending
                                            } else if (infoItems.direction === TransactionInfo.Direction.Direction_In) {
                                                transactionAmountTextColor = R.color.tx_plus
                                            } else {
                                                transactionAmountTextColor =
                                                    R.color.wallet_send_button
                                            }

                                            Column {
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = transactionStatusIcon),
                                                        contentDescription = ""
                                                    )

                                                    Column(
                                                        modifier = Modifier
                                                            .padding(10.dp)
                                                            .fillMaxWidth()
                                                            .weight(0.8f)
                                                    ) {
                                                        Text(
                                                            text = "$transactionAmount BDX",
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                color = colorResource(id = transactionAmountTextColor),
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Text(
                                                            text = transactionDate!!,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = MaterialTheme.appColors.transactionSubTitle,
                                                                fontSize = 12.sp
                                                            )
                                                        )

                                                    }
                                                    Text(
                                                        text = transactionStatus!!,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = MaterialTheme.appColors.transactionSubTitle,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Icon(painter = painterResource(id = R.drawable.ic_transaction_history_arrow_forward),
                                                        contentDescription = "",
                                                        tint = MaterialTheme.appColors.transactionHistoryArrowForwardIconColor,
                                                        modifier = Modifier
                                                            .padding(10.dp)
                                                            .clickable(
                                                                onClick = {
                                                                    expandHistory = true
                                                                    transactionInfoItem = infoItems
                                                                }
                                                            ))

                                                }


                                            }
                                        }
                                    }
                                }
                            }

                            if (expandHistory) {
                                val isBNS = transactionInfoItem!!.isBns
                                val displayAmount: String = Helper.getDisplayAmount(
                                    transactionInfoItem!!.amount,
                                    Helper.DISPLAY_DIGITS_INFO
                                )
                                val transactionStatusIcon: Int
                                var transactionStatus: String? = ""
                                var transactionAmount: String? = ""
                                var transactionAmountTextColor: Int
                                var transactionId: String? = ""
                                var transactionPaymentId: String? = ""
                                var transactionPaymentIdIsVisible: Boolean = false
                                var transactionBlockHeight: String? = ""
                                var transactionDateTime: String? = ""
                                var transactionDate: String? = ""
                                var transactionFee: String? = ""
                                var transactionFeeIsVisible: Boolean = false
                                var transactionRecipientAddress: String? = ""
                                if (isBNS) {
                                    transactionStatusIcon =
                                        R.drawable.bns_transaction
                                    transactionStatus =
                                        context.getString(R.string.tx_status_sent)
                                    if (displayAmount > 0.toString()) {
                                        transactionAmount = context.getString(
                                            R.string.tx_list_amount_negative,
                                            displayAmount
                                        )
                                        transactionAmountTextColor =
                                            R.color.wallet_send_button
                                    }
                                } else {
                                    if (transactionInfoItem!!.direction === TransactionInfo.Direction.Direction_Out) {
                                        transactionStatus =
                                            context.getString(R.string.tx_status_sent)
                                        transactionStatusIcon =
                                            R.drawable.ic_wallet_send_button
                                        if (displayAmount > 0.toString()) {
                                            transactionAmount = context.getString(
                                                R.string.tx_list_amount_negative,
                                                displayAmount
                                            )
                                            transactionAmountTextColor =
                                                R.color.wallet_send_button
                                        }
                                    } else {
                                        transactionStatus =
                                            context.getString(R.string.tx_status_received)
                                        transactionStatusIcon =
                                            R.drawable.ic_wallet_receive_button
                                        if (displayAmount > 0.toString()) {
                                            transactionAmount = context.getString(
                                                R.string.tx_list_amount_positive,
                                                displayAmount
                                            )
                                            transactionAmountTextColor =
                                                R.color.wallet_receive_button
                                        }
                                    }
                                }
                                transactionId =
                                    transactionInfoItem!!.hash

                                if (transactionInfoItem!!.paymentId != "0000000000000000") {
                                    transactionPaymentIdIsVisible =
                                        true
                                    transactionPaymentId =
                                        transactionInfoItem!!.paymentId
                                } else {
                                    transactionPaymentIdIsVisible =
                                        false
                                    transactionPaymentId = ""
                                }
                                transactionBlockHeight = when {
                                    transactionInfoItem!!.isFailed -> {
                                        context.getString(R.string.tx_failed)
                                    }

                                    transactionInfoItem!!.isPending -> {
                                        context.getString(R.string.tx_pending)
                                    }

                                    else -> {
                                        transactionInfoItem!!.blockheight.toString()
                                    }
                                }
                                transactionDate =
                                    getDateTime(transactionInfoItem!!.timestamp, subDateFormat)
                                transactionDateTime =
                                    getDateTime(transactionInfoItem!!.timestamp, dateTimeFormat)

                                if (transactionInfoItem!!.fee > 0) {
                                    val fee: String = Helper.getDisplayAmount(
                                        transactionInfoItem!!.fee,
                                        Helper.DISPLAY_DIGITS_INFO
                                    )
                                    transactionFee = context.getString(
                                        R.string.tx_list_fee,
                                        fee
                                    )
                                    transactionFeeIsVisible =
                                        true
                                } else {
                                    transactionFee = ""
                                    transactionFeeIsVisible =
                                        false
                                }
                                if (transactionInfoItem!!.isFailed) {
                                    transactionAmount = context.getString(
                                        R.string.tx_list_amount_failed,
                                        displayAmount
                                    )
                                    transactionFee =
                                        context.getString(R.string.tx_list_failed_text)
                                    transactionFeeIsVisible =
                                        true
                                    transactionAmountTextColor =
                                        R.color.tx_failed
                                } else if (transactionInfoItem!!.isPending) {
                                    transactionAmountTextColor =
                                        R.color.tx_pending
                                } else if (transactionInfoItem!!.direction === TransactionInfo.Direction.Direction_In) {
                                    transactionAmountTextColor =
                                        R.color.tx_plus
                                    if (!transactionInfoItem!!.isConfirmed) {
                                        val confirmations =
                                            transactionInfoItem!!.confirmations.toInt()
                                    }
                                } else {
                                    transactionAmountTextColor =
                                        R.color.wallet_send_button
                                }

                                transactionRecipientAddress =
                                    if (DatabaseComponent.get(context)
                                            .bchatRecipientAddressDatabase()
                                            .getRecipientAddress(transactionInfoItem!!.hash) != null
                                    ) {
                                        DatabaseComponent.get(context)
                                            .bchatRecipientAddressDatabase()
                                            .getRecipientAddress(transactionInfoItem!!.hash)
                                    } else {
                                        ""
                                    }

                                Column(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .verticalScroll(
                                            rememberScrollState()
                                        )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = transactionStatusIcon),
                                            contentDescription = ""
                                        )

                                        Column(
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .fillMaxWidth()
                                                .weight(0.8f)
                                        ) {
                                            Text(
                                                text = "$transactionAmount BDX",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = colorResource(id = transactionAmountTextColor),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = transactionDate!!,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.appColors.transactionSubTitle,
                                                    fontSize = 12.sp
                                                )
                                            )

                                        }
                                        Text(
                                            text = transactionStatus!!,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = colorResource(id = transactionAmountTextColor),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.transaction_id),
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.appColors.transactionDateTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = transactionId!!,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.appColors.transactionSubTitle,
                                                fontSize = 12.sp
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(10.dp)
                                                .clickable(
                                                    onClick = {
                                                        if (transactionId!!.isNotEmpty()) {
                                                            try {
                                                                val url =
                                                                    "${BuildConfig.EXPLORER_URL}/tx/${transactionId!!}"
                                                                val intent = Intent(
                                                                    Intent.ACTION_VIEW,
                                                                    Uri.parse(url)
                                                                )
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                Toast
                                                                    .makeText(
                                                                        context,
                                                                        "Can't open URL",
                                                                        Toast.LENGTH_LONG
                                                                    )
                                                                    .show()
                                                            }
                                                        }
                                                    }
                                                )
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_new_copy),
                                            tint = MaterialTheme.appColors.copyIcon,
                                            contentDescription = "",
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .clickable(
                                                    onClick = {
                                                        context.copyToClipBoard(
                                                            "Transaction Id",
                                                            transactionId
                                                        )
                                                    }
                                                )
                                        )
                                    }
                                    Divider(
                                        color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 10.dp)
                                    )
                                    if (transactionPaymentIdIsVisible) {
                                        Text(
                                            text = "Payment ID",
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.appColors.transactionDateTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = transactionPaymentId!!,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.appColors.transactionSubTitle,
                                                    fontSize = 12.sp
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .padding(10.dp)
                                            )
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_new_copy),
                                                tint = MaterialTheme.appColors.copyIcon,
                                                contentDescription = "",
                                                modifier = Modifier
                                                    .padding(10.dp)
                                                    .clickable(
                                                        onClick = {
                                                            context.copyToClipBoard(
                                                                "Payment Id",
                                                                transactionPaymentId
                                                            )
                                                        }
                                                    )
                                            )
                                        }
                                        Divider(
                                            color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 10.dp, end = 10.dp)
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "Date",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.appColors.transactionDateTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )
                                        Text(
                                            text = transactionDateTime!!,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.appColors.transactionDateTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )

                                    }
                                    Divider(
                                        color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 10.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "Height",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.appColors.transactionDateTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )
                                        Text(
                                            text = transactionBlockHeight!!,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.appColors.transactionDateTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )

                                    }
                                    Divider(
                                        color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 10.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "Amount",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.appColors.transactionDateTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )
                                        Text(
                                            text = "$transactionAmount BDX",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = colorResource(id = transactionAmountTextColor),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )

                                    }
                                    Divider(
                                        color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 10.dp)
                                    )
                                    if (transactionFeeIsVisible) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Fee",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.appColors.transactionDateTitle,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            )
                                            Text(
                                                text = transactionFee!!,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.appColors.transactionDateTitle,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            )

                                        }
                                        Divider(
                                            color = MaterialTheme.appColors.transactionHistoryCardDivider,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 10.dp, end = 10.dp)
                                        )
                                    }
                                    if (transactionRecipientAddress!!.isNotEmpty()) {
                                        Text(
                                            text = "Recipient Address",
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.appColors.transactionDateTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = transactionRecipientAddress,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.appColors.transactionSubTitle,
                                                    fontSize = 12.sp
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .padding(10.dp)
                                            )
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_new_copy),
                                                tint = MaterialTheme.appColors.copyIcon,
                                                contentDescription = "",
                                                modifier = Modifier.clickable(
                                                    onClick = {
                                                        context.copyToClipBoard("Recipient Address", transactionRecipientAddress)
                                                    }
                                                )
                                            )
                                        }
                                    }

                                }
                            }
                        }
                    }
                } else {

                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.appColors.transactionHistoryCardBackground,
                                shape = RoundedCornerShape(10.dp, 10.dp, 0.dp, 0.dp),
                            ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 10.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.appColors.transactionHistoryCardBackground,
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            if (activityCallback.isSynced) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    Image(
                                        painter = painterResource(id = R.drawable.ic_no_transactions),
                                        contentDescription = "",
                                    )

                                    Text(
                                        text = stringResource(id = R.string.no_transactions_yet),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.appColors.editTextHint,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.padding(top = 20.dp)
                                    )

                                    Text(
                                        text = stringResource(id = R.string.after_your_first_transaction_nyou_will_be_able_to_view_it_here),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.appColors.walletSyncingSubTitle,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.padding(top = 10.dp)
                                    )

                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    Image(
                                        painter = painterResource(id = R.drawable.wallet_syncing),
                                        contentDescription = "",
                                        colorFilter = ColorFilter.tint(MaterialTheme.appColors.walletSyncingIcon)
                                    )

                                    Text(
                                        text = "Wallet Syncing..",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.appColors.editTextHint,
                                            fontSize = 18.sp
                                        ),
                                        modifier = Modifier.padding(top = 20.dp)
                                    )

                                    Text(
                                        text = "Please wait while wallet getting synced",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.appColors.walletSyncingSubTitle,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.padding(top = 10.dp)
                                    )

                                }
                            }

                        }
                    }
                }
            }
        }
    )

}

data class Category(
    val dateTimeStamp: Long,
    val transactionItems: TransactionInfo
)

@SuppressLint("SimpleDateFormat")
@Composable
private fun CategoryHeader(
    dateTimeStamp: String,
    modifier: Modifier = Modifier,
    dateFormat: SimpleDateFormat
) {
    val nowDate = convertStringToDate(
        convertTodayTimeStampToDateString(
            System.currentTimeMillis(),
            dateFormat
        ), dateFormat
    )
    val date = convertStringToDate(dateTimeStamp, dateFormat)
    val diffDays: Long = date.time - System.currentTimeMillis()

    val dayCount: Int = (diffDays.toFloat() / (24 * 60 * 60 * 1000)).toInt()
    val isToday: Boolean = nowDate == date
    var title = ""

    title = if (isToday) {
        "Today"
    } else if (dayCount == 0) {
        "Yesterday"
    } else if (dayCount > -7 && dayCount < 0) {
        val dateFormat = SimpleDateFormat("EEEE")
        dateFormat.format(date)
    } else {
        val dateFormat = SimpleDateFormat("MMMM d")
        dateFormat.format(date)
    }
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    )
}

private fun callIfTransactionListEmpty(size: Int, viewModels: WalletViewModels,showFilter:(status : Boolean)-> Unit) {
    if (size > 0) {
        viewModels.setFilterTransactionIconIsClickable(true)
        viewModels.setTransactionListContainerIsVisible(true)
    } else {
        viewModels.setFilterTransactionIconIsClickable(false)
        viewModels.setTransactionListContainerIsVisible(false)
        showFilter(false)
    }
}

private fun getDateTime(time: Long, dateTimeFormat: SimpleDateFormat): String {
    return dateTimeFormat.format(Date(time * 1000))
}

private fun convertTimeStampToDateString(time: Long, dateFormat: SimpleDateFormat): String {
    return dateFormat.format(Date(time * 1000))
}

private fun convertTodayTimeStampToDateString(time: Long, dateFormat: SimpleDateFormat): String {
    return dateFormat.format(Date(time))
}

private fun convertStringToDate(date: String, dateFormat: SimpleDateFormat): Date {
    return dateFormat.parse(date) as Date
}
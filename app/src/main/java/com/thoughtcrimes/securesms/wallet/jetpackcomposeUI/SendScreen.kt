package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.compose_utils.BChatOutlinedTextField
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.ComposeBroadcastReceiver
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.data.Crypto
import com.thoughtcrimes.securesms.data.PendingTx
import com.thoughtcrimes.securesms.data.TxData
import com.thoughtcrimes.securesms.data.TxDataBtc
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.serializable
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send.TransactionConfirmPopUp
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send.TransactionFailedPopUp
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send.TransactionLoadingPopUp
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send.TransactionSuccessPopup
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.settings.WalletSettingComposeActivity
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.settings.WalletSettingScreens
import com.thoughtcrimes.securesms.wallet.send.SendFragment
import com.thoughtcrimes.securesms.wallet.utils.OpenAliasHelper
import com.thoughtcrimes.securesms.wallet.utils.WalletCallbackType
import com.thoughtcrimes.securesms.wallet.utils.helper.ServiceHelper
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import io.beldex.bchat.R
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale


@SuppressLint("SuspiciousIndentation")
@Composable
fun SendScreen(
    listener: SendFragment.Listener,
    viewModels: WalletViewModels,
    address: MutableState<String>,
    amount: MutableState<String>,
    beldexAddressErrorAction: MutableState<Boolean>,
    beldexAddressErrorText: MutableState<String>,
    beldexAddressErrorTextColorChanged: MutableState<Boolean>,
    resolvingOA: MutableState<Boolean>,
    scanFromGallery: MutableState<Boolean>
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var showTransactionLoading by remember {
        mutableStateOf(false)
    }
    var showTransactionConfirmPopup by remember {
        mutableStateOf(false)
    }
    var showTransactionSentPopup by remember {
        mutableStateOf(false)
    }
    var showTransactionSentFailedPopup by remember {
        mutableStateOf(false)
    }
    var transactionSentFailedError by remember {
        mutableStateOf("")
    }
    var totalBalance by remember {
        mutableStateOf("")
    }
    var estimatedFee by remember {
        mutableStateOf("")
    }
    var totalFunds by remember {
        mutableStateOf("")
    }

    val pendingTransactionObj: PendingTransaction? = null
    var pendingTransactions by remember {
        mutableStateOf(pendingTransactionObj)
    }
    val txDataObj = TxData()
    var txData by remember {
        mutableStateOf(txDataObj)
    }
    val pendingTxObj: PendingTx? = null
    var pendingTx by remember {
        mutableStateOf(pendingTxObj)
    }

    val committedTxObj: PendingTx? =  null
    var committedTx by remember {
        mutableStateOf(committedTxObj)
    }
    // Create a list of priority
    val options = listOf("Flash", "Slow")
    var selectedOptionText by remember { mutableStateOf(options[0]) }

    viewModels.walletBalance.observe(lifecycleOwner) { balance ->
        totalBalance = balance
    }

    viewModels.estimatedFee.observe(lifecycleOwner) { fee ->
        estimatedFee = fee
    }

    viewModels.unLockedBalance.observe(lifecycleOwner){unlockedBalance ->
        totalFunds = unlockedBalance.toString()
    }

    viewModels.selectedOption.observe(lifecycleOwner) { selectedOption ->
        selectedOptionText = selectedOption.toString()

    }

    println("selected option text value $selectedOptionText")


    ComposeBroadcastReceiver(systemAction = "io.beldex.WALLET_ACTION") {
        if (it?.action == "io.beldex.WALLET_ACTION") {
            it.extras?.getBundle("io.beldex.WALLET_DATA")?.let { data ->
                when (data.serializable<WalletCallbackType>("type")) {
                    WalletCallbackType.TransactionCreated -> {
                        pendingTransactions = data.serializable<PendingTransaction>("data")
                        val status = pendingTransactions?.status
                        if (status !== PendingTransaction.Status.Status_Ok) {
                            //Important
                            //getWallet()!!.disposePendingTransaction()
                            showTransactionLoading = false
                            showTransactionSentFailedPopup = true
                            transactionSentFailedError = pendingTransactions?.errorString.toString()
                        } else {
                            showTransactionLoading = false
                            try {
                                if (pendingTransactions?.firstTxId != null) {
                                    showTransactionConfirmPopup = true
                                    pendingTx = PendingTx(pendingTransactions)
                                }
                            } catch (e: java.lang.IllegalStateException) {
                                //Minimized app
                                //onTransactionProgress = true
                                return@ComposeBroadcastReceiver
                            } catch (e: IndexOutOfBoundsException) {
                                //Minimized app
                                showTransactionLoading = false
                                Toast.makeText(context, context.getString(R.string.please_try_again_later), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    WalletCallbackType.TransactionSent -> {
                        showTransactionConfirmPopup = false
                        showTransactionLoading = false
                        showTransactionSentPopup = true
                    }
                    WalletCallbackType.SendTransactionFailed -> {
                        val pendingTransaction = it.serializable<PendingTransaction>("data")
                        val tag = it.getStringExtra("tag")
                        transactionSentFailedError = pendingTransaction?.errorString.toString()
                        //Important
                        //getWallet()!!.disposePendingTransaction()
                        showTransactionSentFailedPopup = true
                        pendingTx = null
                        if (tag != null) {
                            transactionSentFailedError = tag
                        }
                    }
                    else -> Unit
                }
            }
        }
    }





    var beldexAddress by remember {
        mutableStateOf(address)
    }
    var beldexAmount by remember {
        mutableStateOf(amount)
    }

    var addressErrorAction by remember {
        mutableStateOf(beldexAddressErrorAction)
    }
    var addressErrorText by remember {
        mutableStateOf(beldexAddressErrorText)
    }
    var amountErrorAction by remember {
        mutableStateOf(false)
    }
    var amountErrorText by remember {
        mutableStateOf("")
    }
    var addressErrorTextColorChanged by remember {
        mutableStateOf(beldexAddressErrorTextColorChanged)
    }

    var scanFromGallery by remember {
        mutableStateOf(scanFromGallery)
    }

    var currencyValue by remember {
        mutableStateOf("0.00")
    }

    var onTransactionProgress by remember {
        mutableStateOf(false)
    }

    var resolvingOA by remember {
        mutableStateOf(resolvingOA)
    }




    val CLEAN_FORMAT = "%." + Helper.BDX_DECIMALS.toString() + "f"
    val price = 0.00
    val possibleCryptos: MutableSet<Crypto> = HashSet()
    var selectedCrypto: Crypto? = null
    val INTEGRATED_ADDRESS_LENGTH = 106
    var calledUnlockedBalance: Boolean = false
    val MIXIN = 0
    val pendingTransaction: PendingTransaction? = null
    var inProgress = false


    var mode: Mode = Mode.BDX




    val resultLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        val add = data?.getStringExtra("address_value")
        listener.setBarcodeData(null)
        if (add != null) {
            beldexAddress.value = add
        }
    }

    fun isStandardAddress(address: String): Boolean {
        return Wallet.isAddressValid(address)
    }

    fun isIntegratedAddress(address: String): Boolean {
        return (address.length == INTEGRATED_ADDRESS_LENGTH && Wallet.isAddressValid(address))
    }

     fun checkAddressNoError(): Boolean {
        return selectedCrypto != null
    }

     fun checkAddress(): Boolean {
        val ok = checkAddressNoError()
        if (possibleCryptos.isEmpty()) {
            addressErrorAction.value = true
            addressErrorText.value = context.getString(R.string.send_address_invalid)
            addressErrorTextColorChanged.value = false
        } else {
            addressErrorAction.value = false
        }
        return ok
    }

     fun processScannedData() {
        var barcodeData: BarcodeData? = listener.getBarcodeData()
        if (barcodeData != null) {
            if (!Helper.ALLOW_SHIFT && barcodeData.asset !== Crypto.BDX) {
                barcodeData = null
                listener.setBarcodeData(barcodeData)
            }
            if (barcodeData!!.address != null) {
                listener.setBarcodeData(null)
                beldexAddress.value = barcodeData.address
                beldexAmount.value = barcodeData.amount
                possibleCryptos.clear()
                selectedCrypto = null
                if (barcodeData.isAmbiguous) {
                    possibleCryptos.addAll(barcodeData.ambiguousAssets)
                } else {
                    possibleCryptos.add(barcodeData.asset)
                    selectedCrypto = barcodeData.asset
                }
                if (checkAddress()) {
                    if (barcodeData.security === BarcodeData.Security.OA_NO_DNSSEC) addressErrorText.value =
                            context.getString(R.string.send_address_no_dnssec) else if (barcodeData.security === BarcodeData.Security.OA_DNSSEC) addressErrorText.value =
                            context.getString(R.string.send_address_openalias)
                }
                if (isIntegratedAddress(barcodeData.address)) {
                    addressErrorAction.value = true
                    addressErrorText.value = context.getString(R.string.info_paymentid_integrated)
                    addressErrorTextColorChanged.value = true
                }
            } else {
                beldexAddress.value = ""
                beldexAmount.value = ""
            }
        } else Timber.d("barcodeData=null")
    }


    fun processOfScannedData(barcodeData: BarcodeData?) {
        scanFromGallery.value = true
        listener.setBarcodeData(barcodeData)
        processScannedData()
    }

    fun processOpenAlias(dnsOA: String?) {
        if (resolvingOA.value) return  // already resolving - just wait
        listener.popBarcodeData()
        if (dnsOA != null) {
            resolvingOA.value = true
            addressErrorAction.value = true
            addressErrorTextColorChanged.value = false
            addressErrorText.value = context.getString(R.string.send_address_resolve_openalias)
            OpenAliasHelper.resolve(dnsOA, object : OpenAliasHelper.OnResolvedListener {
                override fun onResolved(dataMap: Map<Crypto?, BarcodeData?>) {
                    resolvingOA.value = false
                    var barcodeData = dataMap[Crypto.BDX]
                    if (barcodeData == null) barcodeData = dataMap[Crypto.BTC]
                    if (barcodeData != null) {
                        processOfScannedData(barcodeData)
                    } else {
                        addressErrorAction.value = true
                        addressErrorTextColorChanged.value = false
                        addressErrorText.value = context.getString(R.string.send_address_not_openalias)
                    }
                }

                override fun onFailure() {
                    resolvingOA.value = false
                    addressErrorAction.value = true
                    addressErrorTextColorChanged.value = false
                    addressErrorText.value = context.getString(R.string.send_address_not_openalias)
                }
            })
        }
    }



    fun openAddressBookActivity() {
        TextSecurePreferences.setSendAddressDisable(context, false)
        val intent = Intent(context, WalletSettingComposeActivity::class.java).apply {
            putExtra(WalletSettingComposeActivity.extraStartDestination, WalletSettingScreens.AddressBookScreen.route)
        }
        resultLauncher.launch(intent)
    }


    fun validateBELDEXAmount(amount: String): Boolean {
        val maxValue = 150000000.00000
        val value = amount.replace(',', '.')
        val regExp = "^(([0-9]{0,9})?|[.][0-9]{0,5})?|([0-9]{0,9}+([.][0-9]{0,5}))\$"
        var isValid = false

        isValid = if (value.matches(Regex(regExp))) {
            if (value == ".") {
                false
            } else {
                try {
                    val dValue = value.toDouble()
                    (dValue <= maxValue && dValue > 0)
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            false
        }
        return isValid
    }

    fun getCleanAmountString(enteredAmount: String): String? {
        return try {
            val amount = enteredAmount.toDouble()
            if (amount >= 0) {
                String.format(Locale.US, CLEAN_FORMAT, amount)
            } else {
                null
            }
        } catch (ex: NumberFormatException) {
            null
        }
    }



    fun setMode(aMode: Mode) {
        if (mode != aMode) {
            mode = aMode
            txData = when (aMode) {
                Mode.BDX -> TxData()
                Mode.BTC -> TxDataBtc()
            }
            Timber.d("New Mode = %s", mode.toString())
        }
    }

    fun prepareSend(txData: TxData?) {
        listener.onPrepareSend(null, txData)
    }
    fun commitTransaction() {
        listener.onSend(txData.userNotes)
        committedTx = pendingTx
    }

    fun send() {
        commitTransaction()
        //Insert Recipient Address
        if(TextSecurePreferences.getSaveRecipientAddress(context)) {
            val insertRecipientAddress = DatabaseComponent.get(context).bchatRecipientAddressDatabase()
            try {
                if(pendingTransactions!!.firstTxId != null)
                    insertRecipientAddress.insertRecipientAddress(
                            pendingTransactions!!.firstTxId,
                            txData.destinationAddress
                    )
            }catch(e: IndexOutOfBoundsException){
                e.message?.let { Log.d("SendFragment->", it) }
            }
        }
       showTransactionLoading = true
    }

    fun refreshTransactionDetails() {
        if (pendingTransactions != null) {
            //val txData: TxData = txData()
            try {
                if (pendingTransactions!!.firstTxId != null) {
                    showTransactionConfirmPopup = true
                }
            } catch (e: java.lang.IllegalStateException) {
                //Minimized app
                onTransactionProgress = true
                return
            } catch (e: IndexOutOfBoundsException) {
                //Minimized app
                showTransactionLoading = false
                //hideProgress()
                Toast.makeText(context, context.getString(R.string.please_try_again_later), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onResumeFragment() {
        //Helper.hideKeyboard(activity)
        //isResume = true
        //val activity = activity
        /*if(isAdded && activity != null) {
                    this.activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }*/
        refreshTransactionDetails()
        if (pendingTransactions == null && !inProgress) {/* binding.sendButton.isEnabled=false
             binding.sendButton.isClickable=false*/
            showTransactionLoading = true
            //val txData = txData
            prepareSend(txData)
        }
    }

    val resultLaunchers = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onResumeFragment()
        }
    }

    fun createTransactionIfPossible() {
        if (CheckOnline.isOnline(context)) {
            if ((beldexAddress.value.length > 106 || beldexAddress.value.length < 95) && !(beldexAddress.value.takeLast(4).equals(".bdx", ignoreCase = true))) {
                addressErrorAction.value = true
                addressErrorTextColorChanged.value = false
                addressErrorText.value = context.getString(R.string.invalid_destination_address)
                return
            }
            if (beldexAddress.value.isNotEmpty() && beldexAmount.value.isNotEmpty() && validateBELDEXAmount(beldexAmount.value) && beldexAmount.value.toDouble() > 0.00) {
                //val txDatas: TxData = txData()
                txData.destinationAddress = beldexAddress.value.trim()
                ServiceHelper.ASSET = null
                if (getCleanAmountString(beldexAmount.value).equals(Wallet.getDisplayAmount(totalFunds.toLong()))) {
                    val amount = (totalFunds.toLong() - 10485760)// 10485760 == 050000000
                    val bdx = getCleanAmountString(beldexAmount.value)
                    if (bdx != null) {
                        txData.amount = amount
                    } else {
                        txData.amount = 0L
                    }
                } else {
                    val bdx = getCleanAmountString(beldexAmount.value)
                    if (bdx != null) {
                        txData.amount = Wallet.getAmountFromString(bdx)
                    } else {
                        txData.amount = 0L
                    }
                }
                txData.userNotes = UserNotes("-")//etNotes.getEditText().getText().toString()
                if (TextSecurePreferences.getFeePriority(context) == 0) {
                    txData.priority = PendingTransaction.Priority.Priority_Slow
                } else {
                    txData.priority = PendingTransaction.Priority.Priority_Flash
                }
                txData.mixin = MIXIN
                beldexAmount.value = ""
                beldexAddress.value = ""
                currencyValue = "0.00"

                //Important
                val lockManager: LockManager<CustomPinActivity> = LockManager.getInstance() as LockManager<CustomPinActivity>
                lockManager.enableAppLock(context, CustomPinActivity::class.java)
                val intent = Intent(context, CustomPinActivity::class.java)
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN)
                intent.putExtra("change_pin", false)
                intent.putExtra("send_authentication", true)
                resultLaunchers.launch(intent)
            } else if (beldexAmount.value.isEmpty()) {
                amountErrorAction = true
                amountErrorText = context.getString(R.string.beldex_amount_error_message)
            } else if (beldexAmount.value == ".") {
                amountErrorAction = true
                amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
            } else if (!validateBELDEXAmount(beldexAmount.value)) {
                if (beldexAmount.value.toDouble() <= 0.00) {
                    amountErrorAction = true
                    amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
                } else {
                    amountErrorAction = true
                    amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
                }
            } else {
                addressErrorAction.value = true
                addressErrorTextColorChanged.value = false
                addressErrorText.value = context.getString(R.string.beldex_address_error_message)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
        }
    }




    Column(modifier =Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(color=MaterialTheme.appColors.cardBackground)) {

        Row(verticalAlignment = Alignment.CenterVertically, modifier =Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.appColors.editTextColor, modifier = Modifier.clickable {
                listener.onBackPressedFun()
            })

            Spacer(modifier = Modifier.width(16.dp))

            Text(text = "Send", style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
            ), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }

        if (showTransactionLoading) {
            TransactionLoadingPopUp(onDismiss = {
                showTransactionLoading = false

            })
        }
        if (showTransactionConfirmPopup) {
            showTransactionLoading = false
            TransactionConfirmPopUp(onDismiss = {
                showTransactionConfirmPopup = false
            }, pendingTransactions!!, txData, onClick = { send()})
        }
        if (showTransactionSentPopup) {
            TransactionSuccessPopup(onDismiss = {
                showTransactionSentPopup = false

            })
        }

        if(showTransactionSentFailedPopup){
            TransactionFailedPopUp(onDismiss = {
                 showTransactionSentFailedPopup = false },
                    errorString = transactionSentFailedError)
        }

        Box(modifier =Modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp)
                .border(
                        width=0.8.dp,
                        color=MaterialTheme.appColors.primaryButtonColor.copy(alpha=0.5f),
                        shape=RoundedCornerShape(16.dp)
                )) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.padding(10.dp)) {
                Text(text = "Total Balance", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.appColors.totalBalanceColor, fontSize = 14.sp, fontWeight = FontWeight(700)), modifier =Modifier
                        .fillMaxWidth()
                        .padding(start=10.dp, top=10.dp, end=10.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 10.dp)

                ) {
                    Image(painter = painterResource(id = R.drawable.total_balance), contentDescription = "", modifier = Modifier)
                    Text(text = totalBalance, style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.appColors.textColor, fontSize = 24.sp, fontWeight = FontWeight(700)), modifier =Modifier
                            .fillMaxWidth()
                            .padding(10.dp), fontSize = 24.sp)
                }
            }
        }
        Column(modifier =Modifier
                .padding(10.dp)
                .background(
                        color=MaterialTheme.appColors.receiveCardBackground,
                        shape=RoundedCornerShape(18.dp)
                )

        ) {
            Column(modifier = Modifier.padding(10.dp)

            ) {

                Text(text = "Enter BDX Amount", modifier =Modifier
                        .fillMaxWidth()
                        .padding(top=10.dp, bottom=5.dp, start=10.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight(600), color = MaterialTheme.appColors.textColor))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {

                    BChatOutlinedTextField(
                            value = beldexAmount.value,
                            placeHolder = stringResource(id = R.string.hint),
                            onValueChange = {
                                beldexAmount.value = it
                                if (scanFromGallery.value) {
                                    if (it.isNotEmpty()) {
                                        scanFromGallery.value = false
                                        if (validateBELDEXAmount(it)) {
                                            amountErrorAction = false
                                            val bdx = getCleanAmountString(it.toString())
                                            val amount: BigDecimal = if (bdx != null) {
                                                BigDecimal(bdx.toDouble()).multiply(BigDecimal(price))
                                            } else {
                                                BigDecimal(0L).multiply(BigDecimal(price))
                                            }
                                            beldexAmount.value = String.format("%.4f", amount)
                                        } else {
                                            amountErrorAction = true
                                            amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
                                        }
                                    } else {
                                        amountErrorAction = false
                                        beldexAmount.value = "0.00"
                                    }
                                } else {
                                    if (it.isNotEmpty()) {
                                        if (validateBELDEXAmount(it)) {
                                            amountErrorAction = false
                                            val bdx = getCleanAmountString(it)
                                            val amount: BigDecimal = if (bdx != null) {
                                                BigDecimal(bdx.toDouble()).multiply(BigDecimal(price))
                                            } else {
                                                BigDecimal(0L).multiply(BigDecimal(price))
                                            }
                                            currencyValue = String.format("%.4f", amount)
                                        } else {
                                            amountErrorAction = true
                                            amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
                                        }
                                    } else {
                                        amountErrorAction = false
                                        currencyValue = "0.00"
                                    }
                                }

                            },
                            focusedBorderColor = MaterialTheme.appColors.textFiledBorderColor,
                            focusedLabelColor = MaterialTheme.appColors.textColor,
                            textColor = MaterialTheme.appColors.textColor,
                            maxLen = 16,
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(8.dp),
                    )

                    Box(
                            modifier = Modifier.background(color = MaterialTheme.appColors.maxAmountBackground, shape = RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                    ) {

                        Text(text = "Max", style = MaterialTheme.typography.titleMedium.copy(color = colorResource(id = R.color.white), fontSize = 16.sp), modifier = Modifier.padding(15.dp), textAlign = TextAlign.Center)
                    }


                }
                if (amountErrorAction) {
                    Text(text = amountErrorText, modifier = Modifier.padding(start = 20.dp, bottom = 10.dp), style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.errorMessageColor, fontSize = 13.sp, fontWeight = FontWeight(400)))
                }


                Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {}

                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically

                ) {
                    Text(text = "Beldex Address", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.textColor, fontSize = 16.sp, fontWeight = FontWeight(700)), modifier =Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(10.dp))

                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(32.dp)
                            .background(
                                colorResource(id = R.color.your_bchat_id_bg),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Image(painter = painterResource(id = R.drawable.qr_code_send), contentDescription = "", modifier = Modifier.clickable {
                            if (!CheckOnline.isOnline(context)) {
                                Toast.makeText(context, R.string.please_check_your_internet_connection, Toast.LENGTH_SHORT).show()
                            } else {
                                listener.onScan()
                            }
                        })
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier =Modifier
                            .width(32.dp)
                            .height(32.dp)
                            .background(colorResource(id=R.color.wallet_receive_background), shape=RoundedCornerShape(10.dp))
                            .clickable {
                                openAddressBookActivity()
                            }, contentAlignment = Alignment.Center) {

                        Image(painter = painterResource(id = R.drawable.address_book), contentDescription = "")
                    }

                }
                TextField(value = beldexAddress.value, placeholder = {
                    Text(text = stringResource(R.string.enter_address), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.appColors.primaryButtonColor)
                }, onValueChange = {
                    beldexAddress.value = it
                    addressErrorAction.value = false
                    possibleCryptos.clear()
                    selectedCrypto = null
                    //val address: String = binding.beldexAddress.EditTxtLayout.editText?.text.toString().trim()
                    if (isIntegratedAddress(beldexAddress.value)) {
                        possibleCryptos.add(Crypto.BDX)
                        selectedCrypto = Crypto.BDX
                        addressErrorAction.value = true
                        addressErrorText.value = context.getString(R.string.info_paymentid_integrated)
                        addressErrorTextColorChanged.value = true
                        // binding.beldexAddressErrorMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.button_green))
                        setMode(Mode.BDX)
                    } else if (isStandardAddress(beldexAddress.value)) {
                        possibleCryptos.add(Crypto.BDX)
                        selectedCrypto = Crypto.BDX
                        setMode(Mode.BDX)
                    }
                    if (possibleCryptos.isEmpty()) {
                        Timber.d("other")
                        setMode(Mode.BDX)
                    }
                }, modifier =Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(10.dp), shape = RoundedCornerShape(12.dp), colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.appColors.beldexAddressBackground, focusedContainerColor = MaterialTheme.appColors.beldexAddressBackground, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, cursorColor = colorResource(id = R.color.button_green)), textStyle = TextStyle(color = MaterialTheme.appColors.primaryButtonColor, fontSize = 13.sp, fontWeight = FontWeight(400)), maxLines = 106

                )

                if (addressErrorAction.value) {
                    Text(text = addressErrorText.value,
                            modifier =Modifier.
                            padding(start = 20.dp, bottom = 10.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                    color =  if(addressErrorTextColorChanged.value) {MaterialTheme.appColors.primaryButtonColor} else MaterialTheme.appColors.errorMessageColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight(400)))
                }


                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier) {
                    Text(text = "Transaction Priority", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.textColor, fontSize = 16.sp, fontWeight = FontWeight(700)), modifier = Modifier.padding(10.dp))
                    PriorityDropDown()

                }


                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 20.dp, 20.dp, 20.dp)
                    .border(
                        width = 0.8.dp,
                        color = colorResource(id = R.color.divider_color).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )) {
                    Text(text = estimatedFee, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.textHint, fontSize = 14.sp, fontWeight = FontWeight(700)), modifier = Modifier.padding(20.dp))
                }


            }
        }
        PrimaryButton(
                onClick = {
                    createTransactionIfPossible()
                    // context.startActivity(Intent(context, OnBoardingActivity::class.java))
                },
                modifier =Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                shape = RoundedCornerShape(16.dp),
        ) {
            Text(text = stringResource(id = R.string.send), style = BChatTypography.bodyLarge.copy(color = Color.White), modifier = Modifier.padding(8.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityDropDown() {
    val options = listOf("Flash", "Slow")
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0]) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        TextField(
                modifier = Modifier.menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                value = selectedOptionText,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                        focusedTextColor = colorResource(id = R.color.button_green),
                        unfocusedContainerColor = MaterialTheme.appColors.cardBackground,
                        focusedContainerColor = MaterialTheme.appColors.cardBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                ),

                textStyle = TextStyle(color = MaterialTheme.appColors.textColor, fontSize = 12.sp, fontWeight = FontWeight(400)),
        )
        ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(color = MaterialTheme.appColors.cardBackground)
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                        text = { Text(selectionOption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.appColors.textColor ) },
                        modifier =
                        Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                        onClick = {
                            selectedOptionText = selectionOption
                            expanded = false
                        },
                )
            }
        }
    }
}


enum class Mode {
    BDX, BTC
}



@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SendScreenPreview() {
    BChatTheme() {
        // SendScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SendScreenLightPreview() {
    BChatTheme() {
        //SendScreen()
    }
}

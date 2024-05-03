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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.fragment.app.FragmentActivity
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.compose_utils.BChatOutlinedTextField
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.ComposeBroadcastReceiver
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.data.Crypto
import com.thoughtcrimes.securesms.data.TxData
import com.thoughtcrimes.securesms.data.TxDataBtc
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.serializable
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.addressbook.AddressBookActivity
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send.TransactionConfirmPopUp
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send.TransactionLoadingPopUp
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.send.TransactionSuccessPopup
import com.thoughtcrimes.securesms.wallet.send.SendFragment
import com.thoughtcrimes.securesms.wallet.utils.WalletCallbackType
import com.thoughtcrimes.securesms.wallet.utils.helper.ServiceHelper
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import io.beldex.bchat.R
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale
import java.util.concurrent.Executor


@SuppressLint("SuspiciousIndentation")
@Composable
fun SendScreen(listener: SendFragment.Listener) {

    ComposeBroadcastReceiver(systemAction = "io.beldex.WALLET_ACTION") {
        if (it?.action == "io.beldex.WALLET_ACTION") {
            it.extras?.getBundle("io.beldex.WALLET_DATA")?.let { data ->
                when (data.serializable<WalletCallbackType>("type")) {
                    WalletCallbackType.TransactionCreated -> {
                        val pendingTransaction = data.serializable<PendingTransaction>("data")
                        val tag = data.getString("tag")
                    }
                    WalletCallbackType.TransactionSent -> {
                        val transactionId = data.getString("data")
                    }
                    else -> Unit
                }
            }
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fragment = (lifecycleOwner as? FragmentActivity)?.supportFragmentManager?.findFragmentById(R.id.activity_home_frame_layout_container) as? SendFragment
    val fragmentManager = rememberUpdatedState(newValue = (context as? FragmentActivity)?.supportFragmentManager)

    var beldexAddress by remember {
        mutableStateOf("")
    }
    var beldexAmount by remember {
        mutableStateOf("")
    }

    val estimatedFee by remember {
        mutableStateOf(context.getString(R.string.estimated_fee))
    }

    var addressErrorAction by remember {
        mutableStateOf(false)
    }
    var addressErrorText by remember {
        mutableStateOf("")
    }
    var amountErrorAction by remember {
        mutableStateOf(false)
    }
    var amountErrorText by remember {
        mutableStateOf("")
    }

    var scanFromGallery by remember {
        mutableStateOf(false)
    }

    var currencyValue by remember {
        mutableStateOf("0.00")
    }

    var onTransactionProgress by remember {
        mutableStateOf(false)
    }
    var showTransactionLoading by remember {
        mutableStateOf(false)
    }
    var showTransactionConfirmPopup by remember {
        mutableStateOf(false)
    }
    var showTransactionSentPopup by remember {
        mutableStateOf(false)
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf("Item 1") }


    val CLEAN_FORMAT = "%." + Helper.BDX_DECIMALS.toString() + "f"
    val price = 0.00
    val possibleCryptos: MutableSet<Crypto> = HashSet()
    var selectedCrypto: Crypto? = null
    val INTEGRATED_ADDRESS_LENGTH = 106
    var resolvingOA = false
    var totalFunds: Long = 0
    var calledUnlockedBalance: Boolean = false
    val MIXIN = 0
    val pendingTransaction: PendingTransaction? = null
    var inProgress = false


    var mode: Mode = Mode.BDX
    var txData = TxData()

    // Create a list of priority
    val options = listOf("Flash", "Slow")/*var expanded by remember { mutableStateOf(false) }*/
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    fun getTxData(): TxData {
        return txData
    }

    val resultLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        val add = data?.getStringExtra("address_value")
        listener.setBarcodeData(null)
        if (add != null) {
            beldexAddress = add
        }
    }


    fun openAddressBookActivity() {
        TextSecurePreferences.setSendAddressDisable(context, false)
        val intent = Intent(context, AddressBookActivity::class.java)
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

    fun isStandardAddress(address: String): Boolean {
        return Wallet.isAddressValid(address)
    }

    fun isIntegratedAddress(address: String): Boolean {
        return (address.length == INTEGRATED_ADDRESS_LENGTH && Wallet.isAddressValid(address))
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

    fun refreshTransactionDetails() {
        if (pendingTransaction != null) {
            val txData: TxData = getTxData()
            try {
                if (pendingTransaction.firstTxId != null) {
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
        if (pendingTransaction == null && !inProgress) {/* binding.sendButton.isEnabled=false
             binding.sendButton.isClickable=false*/
            showTransactionLoading = true
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
            if ((beldexAddress.length > 106 || beldexAddress.length < 95) && !(beldexAddress.takeLast(4).equals(".bdx", ignoreCase = true))) {
                addressErrorAction = true
                addressErrorText = context.getString(R.string.invalid_destination_address)
                return
            }
            if (beldexAddress.isNotEmpty() && beldexAmount.isNotEmpty() && validateBELDEXAmount(beldexAmount) && beldexAmount.toDouble() > 0.00) {
                val txData: TxData = getTxData()
                txData.destinationAddress = beldexAddress.trim()
                ServiceHelper.ASSET = null
                if (getCleanAmountString(beldexAmount).equals(Wallet.getDisplayAmount(totalFunds))) {
                    val amount = (totalFunds - 10485760)// 10485760 == 050000000
                    val bdx = getCleanAmountString(beldexAmount)
                    if (bdx != null) {
                        txData.amount = amount
                    } else {
                        txData.amount = 0L
                    }
                } else {
                    val bdx = getCleanAmountString(beldexAmount)
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
                beldexAmount = ""
                beldexAddress = ""
                currencyValue = "0.00"

                //Important
                val lockManager: LockManager<CustomPinActivity> = LockManager.getInstance() as LockManager<CustomPinActivity>
                lockManager.enableAppLock(context, CustomPinActivity::class.java)
                val intent = Intent(context, CustomPinActivity::class.java)
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN)
                intent.putExtra("change_pin", false)
                intent.putExtra("send_authentication", true)
                resultLaunchers.launch(intent)
            } else if (beldexAmount.isEmpty()) {
                amountErrorAction = true
                amountErrorText = context.getString(R.string.beldex_amount_error_message)
            } else if (beldexAmount == ".") {
                amountErrorAction = true
                amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
            } else if (!validateBELDEXAmount(beldexAmount)) {
                if (beldexAmount.toDouble() <= 0.00) {
                    amountErrorAction = true
                    amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
                } else {
                    amountErrorAction = true
                    amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
                }
            } else {
                addressErrorAction = true
                addressErrorText = context.getString(R.string.beldex_address_error_message)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
        }
    }




    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.appColors.cardBackground)) {

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.appColors.editTextColor, modifier = Modifier.clickable {
                //onBackClick()
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
            TransactionConfirmPopUp(onDismiss = {
                showTransactionLoading = false
            }, pendingTransaction!!, txData)
        }
        if (showTransactionSentPopup) {
            TransactionSuccessPopup(onDismiss = {
                showTransactionLoading = false

            })
        }



        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 10.dp)
            .border(
                width = 0.8.dp,
                color = MaterialTheme.appColors.primaryButtonColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.padding(10.dp)) {
                Text(text = "Total Balance", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.appColors.totalBalanceColor, fontSize = 14.sp, fontWeight = FontWeight(700)), modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 10.dp, end = 10.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 10.dp)

                ) {
                    Image(painter = painterResource(id = R.drawable.total_balance), contentDescription = "", modifier = Modifier)
                    Text(text = "99.34628923", style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.appColors.textColor, fontSize = 24.sp, fontWeight = FontWeight(700)), modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp), fontSize = 24.sp)
                }
            }
        }
        Column(modifier = Modifier
            .padding(10.dp)
            .background(
                color = MaterialTheme.appColors.receiveCardBackground,
                shape = RoundedCornerShape(18.dp)
            )

        ) {
            Column(modifier = Modifier.padding(10.dp)

            ) {

                Text(text = "Enter BDX Amount", modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 5.dp, start = 10.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight(600), color = MaterialTheme.appColors.textColor))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {

                    BChatOutlinedTextField(
                            value = beldexAmount,
                            placeHolder = stringResource(id = R.string.hint),
                            onValueChange = {
                                beldexAmount = it
                                if (scanFromGallery) {
                                    if (it.isNotEmpty()) {
                                        scanFromGallery = false
                                        if (validateBELDEXAmount(it)) {
                                            amountErrorAction = false
                                            val bdx = getCleanAmountString(it.toString())
                                            val amount: BigDecimal = if (bdx != null) {
                                                BigDecimal(bdx.toDouble()).multiply(BigDecimal(price))
                                            } else {
                                                BigDecimal(0L).multiply(BigDecimal(price))
                                            }
                                            beldexAmount = String.format("%.4f", amount)
                                        } else {
                                            amountErrorAction = true
                                            amountErrorText = context.getString(R.string.beldex_amount_valid_error_message)
                                        }
                                    } else {
                                        amountErrorAction = false
                                        beldexAmount = "0.00"
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
                    Text(text = "Beldex Address", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.textColor, fontSize = 16.sp, fontWeight = FontWeight(700)), modifier = Modifier
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
                                //listener.onScan()
                            }
                        })
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier
                            .width(32.dp)
                            .height(32.dp)
                            .background(MaterialTheme.appColors.copyIcon, shape = RoundedCornerShape(10.dp))
                            .clickable {
                                openAddressBookActivity()
                            }, contentAlignment = Alignment.Center) {

                        Image(painter = painterResource(id = R.drawable.address_book), contentDescription = "")
                    }

                }
                TextField(value = beldexAddress, placeholder = {
                    Text(text = stringResource(R.string.enter_address), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.appColors.primaryButtonColor)
                }, onValueChange = {
                    beldexAddress = it
                    addressErrorAction = false
                    possibleCryptos.clear()
                    selectedCrypto = null
                    //val address: String = binding.beldexAddressEditTxtLayout.editText?.text.toString().trim()
                    if (isIntegratedAddress(beldexAddress)) {
                        possibleCryptos.add(Crypto.BDX)
                        selectedCrypto = Crypto.BDX
                        addressErrorAction = true
                        addressErrorText = context.getString(R.string.info_paymentid_integrated)
                        // binding.beldexAddressErrorMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.button_green))
                        setMode(Mode.BDX)
                    } else if (isStandardAddress(beldexAddress)) {
                        possibleCryptos.add(Crypto.BDX)
                        selectedCrypto = Crypto.BDX
                        setMode(Mode.BDX)
                    }
                    if (possibleCryptos.isEmpty()) {
                        Timber.d("other")
                        setMode(Mode.BDX)
                    }
                }, modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(10.dp), shape = RoundedCornerShape(12.dp), colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.appColors.beldexAddressBackground, focusedContainerColor = MaterialTheme.appColors.beldexAddressBackground, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, cursorColor = colorResource(id = R.color.button_green)), textStyle = TextStyle(color = MaterialTheme.appColors.primaryButtonColor, fontSize = 13.sp, fontWeight = FontWeight(400)), maxLines = 106

                )

                if (addressErrorAction) {
                    Text(text = addressErrorText, modifier = Modifier.padding(start = 20.dp, bottom = 10.dp), style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.errorMessageColor, fontSize = 13.sp, fontWeight = FontWeight(400)))
                }


                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {

                    Text(text = "Transaction Priority", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.appColors.textColor, fontSize = 16.sp, fontWeight = FontWeight(700)), modifier = Modifier.padding(10.dp))

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
                modifier = Modifier
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
fun DropDownExample() {
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
                        unfocusedContainerColor = MaterialTheme.appColors.beldexAddressBackground,
                        focusedContainerColor = MaterialTheme.appColors.beldexAddressBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                ),

                textStyle = TextStyle(color = MaterialTheme.appColors.textColor, fontSize = 12.sp, fontWeight = FontWeight(400)),
        )
        ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                        text = { Text(selectionOption) },
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
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

class AsyncCalculateEstimatedFee(val priority: Int) : AsyncTaskCoroutine<Executor?, Double>() {
    override fun onPreExecute() {
        super.onPreExecute()
        //estimatedFee = context.getString(R.string.estimated_fee,"0.00")
    }

    override fun doInBackground(vararg params: Executor?): Double {
        return try {
            if (WalletManager.getInstance().wallet != null) {
                val wallet: Wallet = WalletManager.getInstance().wallet
                wallet.estimateTransactionFee(priority)
            } else {
                0.00
            }
        } catch (e: Exception) {
            Log.d("Estimated Fee exception ", e.toString())
            0.00
        }
    }

    override fun onPostExecute(result: Double?) {/*val activity = activity
                    if (isAdded && activity != null) {
                        estimatedFee = context.getString(R.string.estimated_fee,result.toString())
                    }*/

        //estimatedFee = context.getString(R.string.estimated_fee,result.toString())

    }
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

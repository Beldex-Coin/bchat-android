package com.thoughtcrimes.securesms.wallet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.BChatThreadPoolExecutor
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.NodePinger
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.WalletDashBoardScreen
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.WalletViewModels
import com.thoughtcrimes.securesms.wallet.utils.common.fetchPriceFor
import io.beldex.bchat.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.lang.ClassCastException
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import java.util.concurrent.Executor


class WalletFragment : Fragment(),OnBackPressedListener {

    private val formatter = NumberFormat.getInstance()

    private var syncText: String? = null

    private var adapterItems: ArrayList<TransactionInfo> = ArrayList()

    private var walletAvailableBalance: String? =null
    private var walletSynchronized:Boolean = false

    fun setProgress(text: String?) {
        if(text==getString(R.string.reconnecting) || text == getString(R.string.status_wallet_loading) || text==getString(R.string.status_wallet_connecting)){
           ///binding.syncStatusIcon.visibility=View.GONE
            ///binding.syncFailIcon.visibility = View.GONE
        }
        if(text==getString(R.string.reconnecting) || text == getString(R.string.status_wallet_loading) || text == getString(R.string.status_wallet_connecting)){
            viewModels.setSyncStatusTextColor(R.color.icon_tint)
            viewModels.setProgressBarColor(R.color.green_color)
        }
        syncText = text
        viewModels.setSyncStatus(text)
    }
    var onScanListener: OnScanListener? = null

    interface OnScanListener {
        fun onWalletScan()
    }

    private var syncProgress = 4f

    fun setProgress(n: Float) {
        syncProgress = n
        when {
            n==4f -> {
                viewModels.setProgress(1f)
                viewModels.progressBarIsVisible(false)
            }
            n==2f -> {
                viewModels.setProgress(1f)
                viewModels.progressBarIsVisible(true)
            }
            n==3f -> {
                viewModels.progressBarIsVisible(true)
                viewModels.setProgress(1f)
            }
            n<1f && n >= 0f -> {
                viewModels.setProgress(n)
                viewModels.progressBarIsVisible(true)
            }
            else -> { // <0
                viewModels.setProgress(n)
                viewModels.progressBarIsVisible(false)
            }
        }
    }

    private val dismissedTransactions: MutableList<String> = ArrayList()

    fun resetDismissedTransactions() {
        dismissedTransactions.clear()
    }

    var walletLoaded = false

    fun onLoaded() {
        walletLoaded = true
        showReceive()
    }

    private fun showReceive() {
        if (walletLoaded) {
            viewModels.receiveCardViewButtonIsEnabled(true)
        }
    }

    @SuppressLint("ResourceType")
    fun onSynced() {
        if (activityCallback!!.isSynced) {
            viewModels.sendCardViewButtonIsEnabled(true)
            viewModels.scanQRCodeButtonIsEnabled(true)
        }
    }

    fun unsync() {
        if (!activityCallback!!.isSynced) {
            viewModels.sendCardViewButtonIsEnabled(false)
            viewModels.scanQRCodeButtonIsEnabled(false)//binding.scanQrCodeImg.isEnabled = false
        }
        firstBlock = 0
    }

    private var walletTitle: String? = null
    private var mContext : Context? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mContext = context
        if (context is Listener) {
            activityCallback = context
            onScanListener =
                context as OnScanListener
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        this.mContext = null
    }

    override fun onResume() {
        super.onResume()
        viewModels.sendCardViewButtonIsClickable(true)
        viewModels.receiveCardViewButtonIsClickable(true)
        if(TextSecurePreferences.getDisplayBalanceAs(requireActivity())==2) {
            hideDisplayBalance()
        }else{
            Log.d("FiatCurrency Exception: ", "onResume 1")
            if(walletAvailableBalance!=null) {
                showSelectedDecimalBalance(walletAvailableBalance!!, walletSynchronized)
            }
            if(TextSecurePreferences.getChangedCurrency(requireActivity())) {
                Log.d("FiatCurrency Exception: ", "onResume 2")
                TextSecurePreferences.changeCurrency(requireActivity(),false)
                Log.d("FiatCurrency Exception: ", "onResume 3")
                callCurrencyConversionApi()
            }
        }

        setProgress(syncProgress)
        setProgress(syncText)
        showReceive()
    }

    private fun callCurrencyConversionApi(){
        val currency = TextSecurePreferences.getCurrency(requireActivity()).toString().lowercase()
        fetchPriceFor(
            TextSecurePreferences.getCurrency(requireActivity()).toString(),
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    price = 0.00
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        if (response.body != null) {
                            val json = JSONObject(response.body!!.string())
                            val result = json.getJSONObject("beldex")
                            if(result.length()!=0) {
                                price = result.getDouble(currency)
                                if(walletAvailableBalance!=null) {
                                    Log.d("FiatCurrency Exception: ", "one")
                                    updateFiatCurrency(walletAvailableBalance!!)
                                }
                                TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                            }else{
                                price = 0.00
                                if(walletAvailableBalance!=null) {
                                    Log.d("FiatCurrency Exception: ", "two")
                                    updateFiatCurrency(walletAvailableBalance!!)
                                }
                                TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                            }
                        }
                    } else {
                        price = 0.00
                        if(walletAvailableBalance!=null) {
                            Log.d("FiatCurrency Exception: ", "three")
                            updateFiatCurrency(walletAvailableBalance!!)
                        }
                        TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                    }
                }
            }
        )
    }

    inner class AsyncGetUnlockedBalance(val wallet: Wallet) :
        AsyncTaskCoroutine<Executor?, Boolean?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            if(mContext!=null && walletAvailableBalance!=null) {
                if(TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 1 || TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 0) {
                    if(walletAvailableBalance!!.replace(",","").toDouble()>0.0) {
                        showSelectedDecimalBalance(walletAvailableBalance!!, true)
                    }else{
                        refreshBalance(false)
                    }
                }
            }else {
                refreshBalance(false)
            }
        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            try {
                unlockedBalance = activityCallback!!.getUnLockedBalance
            }catch (e: Exception){
                Log.d("WalletFragment",e.toString())
            }
           return true
        }

        override fun onPostExecute(result: Boolean?) {
             refreshBalance(wallet.isSynchronized)
        }
    }

    inner class AsyncGetFullBalance(val wallet: Wallet) : AsyncTaskCoroutine<Executor?, Boolean?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            if (mContext != null && walletAvailableBalance != null) {
                if (TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 1 || TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 0) {
                    if (walletAvailableBalance!!.replace(",", "").toDouble() > 0.0) {
                        showSelectedDecimalBalance(walletAvailableBalance!!, true)
                    } else {
                        refreshBalance(false)
                    }
                }
            } else {
                refreshBalance(false)
            }
        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            try {
                balance = activityCallback!!.getFullBalance
            } catch (e: Exception) {
                Log.d("WalletFragment", e.toString())
            }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            refreshBalance(wallet.isSynchronized)
        }
    }

    fun autoselect(nodes: Set<NodeInfo?>): NodeInfo? {
        if (nodes.isEmpty()) return null
        NodePinger.execute(nodes, null)
        val nodeList: ArrayList<NodeInfo?> = ArrayList<NodeInfo?>(nodes)
        Collections.sort(nodeList, NodeInfo.BestNodeComparator)
        val rnd = Random().nextInt(nodeList.size)
        return nodeList[rnd]
    }

    override fun onPause() {
        super.onPause()
    }

    companion object{
        var syncingBlocks : Long = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    var price =0.00

    private val viewModels: WalletViewModels by activityViewModels()

    @SuppressLint("ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Get Selected Fiat Currency Price
        if(TextSecurePreferences.getFiatCurrencyApiStatus(requireActivity())) {
            TextSecurePreferences.callFiatCurrencyApi(requireActivity(),false)
            callCurrencyConversionApi()
        }else{
            price = if(TextSecurePreferences.getCurrencyAmount(requireActivity())!=null){
                TextSecurePreferences.getCurrencyAmount(requireActivity())!!.toDouble()
            }else{ 0.00}
        }
        viewModels.sendCardViewButtonIsEnabled(false)
        viewModels.scanQRCodeButtonIsEnabled(false)

        if(activityCallback!!.getNode() == null){
            setProgress("Failed to connect to node")
            setProgress(2f)
            viewModels.setSyncStatusTextColor(R.color.red)
            viewModels.setProgressBarColor(R.color.red)
        }

        viewModels.sendCardViewButtonIsClickable(true)
        viewModels.receiveCardViewButtonIsClickable(true)

        if (activityCallback!!.isSynced) {
            onSynced()
        }

        activityCallback!!.forceUpdate(requireActivity())

        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        WalletDashBoardScreen(viewModels, activityCallback!!,onScanListener)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnPreDrawListener {
            true
        }
    }
    private var firstBlock: Long = 0
    private var unlockedBalance: Long = -1
    private var balance: Long = 0

    private fun updateStatus(wallet: Wallet) {
        if (!isAdded) return
        if(CheckOnline.isOnline(requireContext())) {
            val sync: String
            check(activityCallback!!.hasBoundService()) { "WalletService not bound." }
            val daemonConnected: Wallet.ConnectionStatus = activityCallback!!.connectionStatus!!
            if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                if (!wallet.isSynchronized) {
                    ApplicationContext.getInstance(context).messageNotifier.setHomeScreenVisible(true)
                    val daemonHeight: Long = wallet.daemonBlockChainHeight
                    val walletHeight = wallet.blockChainHeight
                    val n = daemonHeight - walletHeight
                    sync = formatter.format(n) + " " + getString(R.string.status_remaining)
                    syncingBlocks = n
                    if (firstBlock == 0L) {
                        firstBlock = walletHeight
                    }
                    var x = (100 - Math.round(100f * n / (1f * daemonHeight  - firstBlock))).toInt()
                    if (x == 0) x = 1 // indeterminate
                    if(x>=0){
                        setProgress((x/100.0).toFloat())
                    }else{
                        setProgress(x.toFloat())
                    }
                    viewModels.setFilterTransactionIconIsClickable(false)
                    ///binding.syncStatusIcon.visibility=View.GONE
                    ///binding.syncFailIcon.visibility = View.GONE
                    viewModels.setSyncStatusTextColor(R.color.icon_tint)
                    viewModels.setProgressBarColor(R.color.green_color)
                } else {
                    syncingBlocks = 0
                    ApplicationContext.getInstance(context).messageNotifier.setHomeScreenVisible(false)
                    sync = getString(R.string.status_synchronized)
                    viewModels.setSyncStatusTextColor(R.color.green_color)
                    viewModels.setProgressBarColor(R.color.green_color)
                    setProgress(3f)
                    //viewModels.setFilterTransactionIconIsClickable(true)
                    ///binding.syncStatusIcon.visibility=View.VISIBLE
                    ///binding.syncFailIcon.visibility = View.GONE
                    ///
                    /*binding.syncStatusIcon.setOnClickListener {
                        if(CheckOnline.isOnline(requireActivity())){
                            if(wallet!=null) {
                                checkSyncInfo(requireActivity(),wallet.restoreHeight)
                            }
                        }
                    }*/
                }
            } else if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connecting) {
                ///binding.syncStatusIcon.visibility = View.GONE
                ///binding.syncFailIcon.visibility = View.GONE
                sync = getString(R.string.status_wallet_connecting)
                setProgress(2f)
                viewModels.setSyncStatusTextColor(R.color.icon_tint)
                viewModels.setProgressBarColor(R.color.green_color)

            } else {
                ///binding.syncStatusIcon.visibility=View.GONE
                ///binding.syncFailIcon.visibility=View.VISIBLE
                ///
                /*binding.syncFailIcon.setOnClickListener {
                    if(CheckOnline.isOnline(requireActivity())){
                            checkSyncFailInfo(requireActivity())
                        }
                    }*/
                sync = getString(R.string.failed_connected_to_the_node)
                setProgress(2f)
                viewModels.setSyncStatusTextColor(R.color.red)
                viewModels.setProgressBarColor(R.color.red)
            }
            setProgress(sync)
        }
        else
        {
            setProgress(getString(R.string.no_node_connection))
            viewModels.setSyncStatusTextColor(R.color.red)
            setProgress(2f)
            viewModels.setProgressBarColor(R.color.red)
            ///binding.syncStatusIcon.visibility=View.GONE
            ///binding.syncFailIcon.visibility=View.GONE
        }
    }

    private fun checkSyncInfo(requireActivity: FragmentActivity, restoreHeight: Long) {
        val dialog = AlertDialog.Builder(requireActivity)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.sync_info, null)
        dialog.setView(dialogView)
        val restoreFromHeight = dialogView.findViewById<TextView>(R.id.restoreFromHeight)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        restoreFromHeight.text = "$restoreHeight."
        val alert = dialog.create()
        alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alert.setCanceledOnTouchOutside(false)
        alert.show()
        okButton.setOnClickListener {
            alert.dismiss()
        }
    }

    private fun checkSyncFailInfo(requireActivity: FragmentActivity) {
        val dialog = AlertDialog.Builder(requireActivity)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.sync_fail_info, null)
        dialog.setView(dialogView)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        val alert = dialog.create()
        alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alert.setCanceledOnTouchOutside(false)
        alert.show()
        okButton.setOnClickListener {
            alert.dismiss()
        }
    }

    fun updateNodeConnectingStatus() {
        ///binding.syncStatusIcon.visibility = View.GONE
        ///binding.syncFailIcon.visibility = View.GONE
        setProgress(getString(R.string.status_wallet_connecting))
        setProgress(2f)
        viewModels.setSyncStatusTextColor(R.color.icon_tint)
        viewModels.setProgressBarColor(R.color.green_color)
    }

    private fun refreshBalance(synchronized: Boolean) {
        val amountBdx: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        val amountFullBdx: Double = Helper.getDecimalAmount(balance).toDouble()
        if(amountFullBdx > 0.0) {
            showBalance(
                Helper.getFormattedAmount(amountBdx, true),
                true,
                Helper.getFormattedAmount(amountFullBdx, true)
            )
        } else {
            showBalance(
                Helper.getFormattedAmount(amountBdx, true),
                synchronized,
                Helper.getFormattedAmount(amountFullBdx, true)
            )
        }
    }

    private fun hideDisplayBalance(){
        viewModels.updateWalletBalance("---")
        viewModels.updateFiatCurrency("---")
    }

    private fun showSelectedDecimalBalance(balance: String, synchronized: Boolean){
        if(!synchronized){
            viewModels.updateFetchBalanceStatus(true)
            when {
                TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                    viewModels.updateWalletBalance("-.--")
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                    viewModels.updateWalletBalance("-.---")
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (0)" -> {
                    viewModels.updateWalletBalance("-")
                }
                else -> {
                    viewModels.updateWalletBalance("-.----")
                }
            }
            if (!activityCallback!!.isSynced) {
                viewModels.sendCardViewButtonIsEnabled(false)
                viewModels.setSendCardViewButtonTextColor(R.color.send_button_disable_color)
                viewModels.scanQRCodeButtonIsEnabled(false)
            }
        }else{
            viewModels.updateFetchBalanceStatus(false)
            when {
                TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                    viewModels.updateWalletBalance(String.format("%.2f", balance.replace(",","").toDouble()))
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                    viewModels.updateWalletBalance(String.format("%.3f", balance.replace(",","").toDouble()))
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (0)" -> {
                    viewModels.updateWalletBalance(String.format("%.0f", balance.replace(",","").toDouble()))
                }
                else -> {
                    viewModels.updateWalletBalance(balance)
                }
            }
            //SteveJosephh21
            if (activityCallback!!.isSynced) {
                viewModels.sendCardViewButtonIsEnabled(true)
                viewModels.setSendCardViewButtonTextColor(R.color.white)
                viewModels.scanQRCodeButtonIsEnabled(true)
            }
        }
        Log.d("FiatCurrency Exception: ", "four")
        //Update Fiat Currency
        updateFiatCurrency(balance)
    }

    private fun updateFiatCurrency(balance: String) {
        if(balance.isNotEmpty() && balance!=null) {
            try {
                Log.d("FiatCurrency Exception: ", "$balance, $price")
                val amount: BigDecimal = BigDecimal(balance.replace(",","").toDouble()).multiply(BigDecimal(price))
                viewModels.updateFiatCurrency(getString(R.string.fiat_currency, amount.toDouble(), TextSecurePreferences.getCurrency(requireActivity()).toString()))
                Log.d("FiatCurrency Exception: ","try")
            }catch (e:NumberFormatException){
                viewModels.updateFiatCurrency(getString(R.string.fiat_currency, 0.00, TextSecurePreferences.getCurrency(requireActivity()).toString()))
                Log.d("FiatCurrency Exception: catch 1 ",e.message.toString())
            }catch(e:IllegalStateException){
                Log.d("FiatCurrency Exception: catch 2 ",e.message.toString())
            }
        }
    }

    private fun showBalance(balance: String?,synchronized: Boolean,fullBalance:String?) {
        if(mContext!=null) {
            when {
                TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 2 -> {
                    viewModels.updateFetchBalanceStatus(false)
                    hideDisplayBalance()
                }
                TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 0 -> {
                    walletAvailableBalance = fullBalance
                    walletSynchronized = synchronized
                    showSelectedDecimalBalance(fullBalance!!, synchronized)
                }
                else -> {
                    if(unlockedBalance.toString() == "-1"){
                        viewModels.updateFetchBalanceStatus(true)
                        when {
                            TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                                viewModels.updateWalletBalance("-.--")
                            }
                            TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                                viewModels.updateWalletBalance("-.---")
                            }
                            TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (0)" -> {
                                viewModels.updateWalletBalance("-")
                            }
                            else -> {
                                viewModels.updateWalletBalance("-.----")
                            }
                        }
                    }else {
                        walletAvailableBalance = balance
                        walletSynchronized = synchronized
                        showSelectedDecimalBalance(balance!!, synchronized)
                    }
                }
            }
        }
    }

    var activityCallback: Listener? = null

    // Container Activity must implement this interface
    interface Listener {
        fun hasBoundService(): Boolean
        fun forceUpdate(requireActivity: Context)
        val connectionStatus: Wallet.ConnectionStatus?
        val daemonHeight: Long

        fun onSendRequest()
        fun onTxDetailsRequest(view: View?, info: TransactionInfo?)
        val isSynced: Boolean
        val isStreetMode: Boolean
        val streetModeHeight: Long

        fun getTxKey(txId: String?): String?
        fun onWalletReceive()
        fun hasWallet(): Boolean
        fun getWallet(): Wallet?

        //Node Connection
        fun getFavouriteNodes(): MutableSet<NodeInfo>
        fun getOrPopulateFavourites(): MutableSet<NodeInfo>
        fun getNode(): NodeInfo?
        fun setNode(node: NodeInfo?)

        fun onNodePrefs()

        fun callFinishActivity()

        fun callToolBarRescan()
        fun callToolBarSettings()

        fun walletOnBackPressed() //-
        val getUnLockedBalance: Long
        val getFullBalance: Long
    }

    private var accountIndex = 0

    fun onRefreshed(wallet: Wallet, full: Boolean, list: MutableList<TransactionInfo>) {
        updateStatus(wallet)
        callRefreshHistory(wallet,list)
    }

    private fun callRefreshHistory(wallet:Wallet, list: MutableList<TransactionInfo>){
        viewModels.setTransactionInfoItems(list)
        if (accountIndex != wallet.accountIndex) {
            accountIndex = wallet.accountIndex
            ///binding.transactionList.scrollToPosition(0)
        }

        //Steve Josephh21 ANRS
        if(CheckOnline.isOnline(requireContext())) {
            check(activityCallback!!.hasBoundService()) { "WalletService not bound." }
            val daemonConnected: Wallet.ConnectionStatus = activityCallback!!.connectionStatus!!
            if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                if (TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 1) {
                    AsyncGetUnlockedBalance(wallet).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
                } else if (TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 0) {
                    AsyncGetFullBalance(wallet).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
                }
            }
        }
    }


    override fun onBackPressed(): Boolean {
        return false
    }
}
//endregion
package com.thoughtcrimes.securesms.wallet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.BChatThreadPoolExecutor
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.NodePinger
import com.thoughtcrimes.securesms.util.daterangepicker.DateRangePicker
import com.thoughtcrimes.securesms.wallet.utils.common.fetchPriceFor
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentWalletBinding
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
import java.text.SimpleDateFormat
import java.util.concurrent.Executor


class WalletFragment : Fragment(),OnBackPressedListener {

    private var adapter: TransactionInfoAdapter? = null
    private val formatter = NumberFormat.getInstance()

    private var syncText: String? = null

    private var adapterItems: ArrayList<TransactionInfo> = ArrayList()

    private var walletAvailableBalance: String? =null
    private var walletSynchronized:Boolean = false

    fun setProgress(text: String?) {
        if(text==getString(R.string.reconnecting) || text==getString(R.string.status_wallet_connecting)){
           binding.syncStatusIcon.visibility=View.GONE
            binding.syncFailIcon.visibility = View.GONE
        }
        if(text==getString(R.string.reconnecting) || text == getString(R.string.status_wallet_loading) || text == getString(R.string.status_wallet_connecting)){
            binding.syncStatus.setTextColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.green_color))
            binding.progressBar.indeterminateDrawable.setColorFilter(ContextCompat.getColor(requireActivity().applicationContext,R.color.green_color),
                PorterDuff.Mode.SRC_IN)
        }
        syncText = text
        binding.syncStatus.text = text
    }
    var onScanListener: OnScanListener? = null

    interface OnScanListener {
        fun onWalletScan(view: View?)
    }

    private var syncProgress = -1

    fun setProgress(n: Int) {
        syncProgress = n
        when {
            n > 100 -> {
                binding.progressBar.isIndeterminate = true
                binding.progressBar.visibility = View.VISIBLE
            }
            n >= 0 -> {
                binding.progressBar.isIndeterminate = false
                binding.progressBar.progress = n
                binding.progressBar.visibility = View.VISIBLE
            }
            n==-2 -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.isIndeterminate = false
                binding.progressBar.progress=100
            }
            else -> { // <0
                binding.progressBar.visibility = View.GONE
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
            binding.receiveCardViewButton.isEnabled = true
        }
    }

    @SuppressLint("ResourceType")
    fun onSynced() {
        if (activityCallback!!.isSynced) {
            binding.sendCardViewButton.isEnabled = true
            binding.sendCardViewButton.setBackgroundResource(R.drawable.send_card_enabled_background)
            binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.white))
            binding.scanQrCodeImg.isEnabled = true
            binding.scanQrCodeImg.setImageResource(R.drawable.ic_scan_qr)
        }
    }

    fun unsync() {
        if (!activityCallback!!.isSynced) {
            binding.sendCardViewButton.isEnabled = false
            binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.send_button_disable_color))
            binding.scanQrCodeImg.isEnabled = false
            binding.sendCardViewButton.setBackgroundResource(R.drawable.send_card_background)
            binding.scanQrCodeImg.setImageResource(R.drawable.ic_wallet_scan_qr_disable)
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
        binding.sendCardViewButton.isClickable= true
        binding.receiveCardViewButton.isClickable= true
        if(TextSecurePreferences.getDisplayBalanceAs(requireActivity())==2) {
            hideDisplayBalance()
        }else{
            if(walletAvailableBalance!=null) {
                showSelectedDecimalBalance(walletAvailableBalance!!, walletSynchronized)
            }
            if(TextSecurePreferences.getChangedCurrency(requireActivity())) {
                TextSecurePreferences.changeCurrency(requireActivity(),false)
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
                                    updateFiatCurrency(walletAvailableBalance!!)
                                }
                                TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                            }else{
                                price = 0.00
                                if(walletAvailableBalance!=null) {
                                    updateFiatCurrency(walletAvailableBalance!!)
                                }
                                TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                            }
                        }
                    } else {
                        price = 0.00
                        if(walletAvailableBalance!=null) {
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
                unlockedBalance = wallet.unlockedBalance
            }catch (e: Exception){
                Log.d("WalletFragment",e.toString())
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private lateinit var binding: FragmentWalletBinding

    var price =0.00

    @SuppressLint("ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        (activity as HomeActivity).setSupportActionBar(binding.toolbar)
        //Get Selected Fiat Currency Price
            if(TextSecurePreferences.getFiatCurrencyApiStatus(requireActivity())) {
                TextSecurePreferences.callFiatCurrencyApi(requireActivity(),false)
                callCurrencyConversionApi()
            }else{
                price = if(TextSecurePreferences.getCurrencyAmount(requireActivity())!=null){
                    TextSecurePreferences.getCurrencyAmount(requireActivity())!!.toDouble()
                }else{ 0.00}
            }
        binding.sendCardViewButton.isEnabled = false
        binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.send_button_disable_color))
        binding.scanQrCodeImg.isEnabled = false
        binding.scanQrCodeImg.setImageResource(R.drawable.ic_wallet_scan_qr_disable)
//        showBalance(Helper.getDisplayAmount(0),walletSynchronized,Helper.getDisplayAmount(0))

        adapter = TransactionInfoAdapter(activity)
        binding.transactionList.adapter = adapter
        adapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0 && binding.transactionList.computeVerticalScrollOffset() == 0)
                    binding.transactionList.scrollToPosition(positionStart)
            }
        })
        binding.transactionList.isNestedScrollingEnabled = false

        if(activityCallback!!.getNode() == null){
            setProgress("Failed to connect to node")
            setProgress(101)
            binding.syncStatus.setTextColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.red))
            binding.progressBar.indeterminateDrawable.setColorFilter(
                ContextCompat.getColor(requireActivity().applicationContext,R.color.red), PorterDuff.Mode.SRC_IN)
        }

        binding.sendCardViewButton.isClickable= true
        binding.receiveCardViewButton.isClickable= true
        binding.sendCardViewButton.setOnClickListener { v: View? ->
            binding.sendCardViewButton.isClickable= false
            activityCallback!!.onSendRequest(v)
        }
        binding.receiveCardViewButton.setOnClickListener { v: View? ->
            binding.receiveCardViewButton.isClickable= false
            activityCallback!!.onWalletReceive(v)
        }

        if (activityCallback!!.isSynced) {
            onSynced()
        }

        activityCallback!!.forceUpdate(requireActivity())

        //SteveJosephh21
        binding.filterTransactionsIcon.setOnClickListener {view->
            binding.filterTransactionsIcon.isClickable =false
            var dismissPopupMenu =false
            val wrapper: Context = ContextThemeWrapper(requireActivity(), R.style.custom_PopupMenu)
            val popupMenu = PopupMenu(wrapper, view,Gravity.END)
            popupMenu.inflate(R.menu.filter_transactions_popup_menu)
            popupMenu.setOnDismissListener{
                if(dismissPopupMenu)it.show()
                else {
                    binding.filterTransactionsIcon.isClickable =true
                    it.dismiss()
                }
                dismissPopupMenu=false
            }
            val spanString = SpannableString(popupMenu.menu[0].title.toString())
            spanString.setSpan(RelativeSizeSpan(1.2f), 0,spanString.length, 0)
            spanString.setSpan(StyleSpan(Typeface.BOLD), 0, spanString.length, 0)
            spanString.setSpan(
                ForegroundColorSpan(
                    ResourcesCompat
                        .getColor(resources, R.color.text, null)
                ),
                0, spanString.length, 0
            )
            popupMenu.menu[0].title = spanString
            popupMenu.menu[0].isEnabled = false
            popupMenu.menu[1].isChecked = TextSecurePreferences.getIncomingTransactionStatus(requireActivity())
            popupMenu.menu[2].isChecked = TextSecurePreferences.getOutgoingTransactionStatus(requireActivity())
            popupMenu.setOnMenuItemClickListener { item ->
                dismissPopupMenu=true
                val emptyList: ArrayList<TransactionInfo> = ArrayList()
                if (item.title == "Incoming") {
                    Toast.makeText(context, getString(R.string.filter_applied), Toast.LENGTH_SHORT).show()
                    item.isChecked = !item.isChecked
                    if(popupMenu.menu[2].isChecked && item.isChecked){
                        TextSecurePreferences.setIncomingTransactionStatus(requireActivity(), true)
                        filterAll(adapterItems)
                    }else if (item.isChecked && !popupMenu.menu[2].isChecked) {
                        TextSecurePreferences.setIncomingTransactionStatus(
                            requireActivity(),
                            true
                        )
                        filter(TransactionInfo.Direction.Direction_In, adapterItems)
                    } else if(!item.isChecked && popupMenu.menu[2].isChecked) {
                        TextSecurePreferences.setIncomingTransactionStatus(
                            requireActivity(),
                            false
                        )
                        filter(TransactionInfo.Direction.Direction_Out, adapterItems)
                    }
                    else if(!popupMenu.menu[2].isChecked && !item.isChecked){
                        //emptyList
                        filterAll(emptyList)
                        TextSecurePreferences.setIncomingTransactionStatus(requireActivity(), false)
                        TextSecurePreferences.setOutgoingTransactionStatus(requireActivity(), false)
                    }
                } else if (item.title == "Outgoing") {
                    Toast.makeText(context, getString(R.string.filter_applied), Toast.LENGTH_SHORT).show()
                    item.isChecked = !item.isChecked
                    if(popupMenu.menu[1].isChecked && item.isChecked){
                        TextSecurePreferences.setOutgoingTransactionStatus(requireActivity(), true)
                        filterAll(adapterItems)
                    }else if (item.isChecked && !popupMenu.menu[1].isChecked ) {
                        TextSecurePreferences.setOutgoingTransactionStatus(
                            requireActivity(),
                            true
                        )
                        filter(TransactionInfo.Direction.Direction_Out, adapterItems)
                    } else if (!item.isChecked && popupMenu.menu[1].isChecked ) {
                        TextSecurePreferences.setOutgoingTransactionStatus(
                            requireActivity(),
                            false
                        )
                        filter(TransactionInfo.Direction.Direction_In, adapterItems)
                    }
                    else if(!popupMenu.menu[1].isChecked && !item.isChecked){
                        // emptyList
                        filterAll(emptyList)
                        TextSecurePreferences.setIncomingTransactionStatus(requireActivity(), false)
                        TextSecurePreferences.setOutgoingTransactionStatus(requireActivity(), false)
                    }
                }else{
                    val dateRangePicker = DateRangePicker(requireContext(),
                        DateRangePicker.OnCalenderClickListener { selectedStartDate, selectedEndDate ->
                            Toast.makeText(
                                context,
                                getString(R.string.filter_applied),
                                Toast.LENGTH_LONG
                            ).show()
                             if(popupMenu.menu[1].isChecked && popupMenu.menu[2].isChecked){
                                 filterTransactionsByDate(getDaysBetweenDates(Date(selectedStartDate),Date(selectedEndDate)),adapterItems)
                             }else if(popupMenu.menu[1].isChecked){
                                 filterTransactionsByDate(getDaysBetweenDates(Date(selectedStartDate),Date(selectedEndDate)),filterTempList(TransactionInfo.Direction.Direction_In, adapterItems))
                             }else if(popupMenu.menu[2].isChecked){
                                 filterTransactionsByDate(getDaysBetweenDates(Date(selectedStartDate),Date(selectedEndDate)),filterTempList(TransactionInfo.Direction.Direction_Out, adapterItems))
                             }else{
                                 filterTransactionsByDate(getDaysBetweenDates(Date(selectedStartDate),Date(selectedEndDate)),emptyList)
                             }
                        })
                    dateRangePicker.show()
                    dateRangePicker.setBtnPositiveText("OK")
                    dateRangePicker.setBtnNegativeText("CANCEL")
                }
                false
            }
            popupMenu.show()
        }
        binding.scanQrCodeImg.setOnClickListener {
                onScanListener?.onWalletScan(view)
        }

        binding.toolBarRescan.setOnClickListener {
            activityCallback?.callToolBarRescan()
        }
        binding.toolBarSettings.setOnClickListener {
            activityCallback?.callToolBarSettings()
        }

        binding.exitButton.setOnClickListener {
            activityCallback?.walletOnBackPressed()
        }

        return binding.root
    }
    private val DATETIME_FORMATTER = SimpleDateFormat("dd-MM-yyyy")

    private fun getDateTime(time: Long): String {
        return DATETIME_FORMATTER.format(Date(time * 1000))
    }

    fun filter(text: TransactionInfo.Direction, arrayList: ArrayList<TransactionInfo>) {
        val temp: ArrayList<TransactionInfo> = ArrayList()
        for (d in arrayList) {
            if (d.direction == text) {
                temp.add(d)
            }
        }
        callIfTransactionListEmpty(temp.size)
        //update recyclerview
        adapter!!.updateList(temp)
    }

    private fun filterAll(arrayList: ArrayList<TransactionInfo>) {
        val temp: ArrayList<TransactionInfo> = ArrayList()
        for (d in arrayList) {
            temp.add(d)
        }
        callIfTransactionListEmpty(temp.size)
        //update recyclerview
        adapter!!.updateList(temp)
    }

    private fun filterTempList(text: TransactionInfo.Direction, arrayList: ArrayList<TransactionInfo>):ArrayList<TransactionInfo> {
        val temp: ArrayList<TransactionInfo> = ArrayList()
        for (d in arrayList) {
            if (d.direction == text) {
                temp.add(d)
            }
        }
        return temp
    }

    private fun filterTransactionsByDate(dates:List<Date>, arrayList: ArrayList<TransactionInfo>) {
        val temp: ArrayList<TransactionInfo> = ArrayList()
        for(datesItem in dates) {
            for (d in arrayList) {
                if (getDateTime(d.timestamp) == DATETIME_FORMATTER.format(datesItem)) {
                    temp.add(d)
                }
            }
        }
        callIfTransactionListEmpty(temp.size)
        //update recyclerview
        adapter!!.updateList(temp)
    }

    private fun callIfTransactionListEmpty(size: Int) {
        if (size > 0) {
            binding.transactionList.visibility = View.VISIBLE
            binding.emptyContainerLayout.visibility = View.GONE
        } else {
            binding.filterTransactionsIcon.isClickable = true
            binding.transactionList.visibility = View.GONE
            binding.emptyContainerLayout.visibility = View.VISIBLE
        }
    }

    private fun getDaysBetweenDates(startDate: Date, endDate: Date): List<Date> {
        val dates: MutableList<Date> = ArrayList()
        val calendar: Calendar = GregorianCalendar()
        calendar.time = startDate
        while (calendar.time.before(endDate)) {
            val result = calendar.time
            dates.add(result)
            calendar.add(Calendar.DATE, 1)
        }
        val calendarEndDate: Calendar = GregorianCalendar()
        calendarEndDate.time = endDate
        dates.add(calendarEndDate.time)
        return dates
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
                    if (firstBlock == 0L) {
                        firstBlock = walletHeight
                    }
                    var x = (100 - Math.round(100f * n / (1f * daemonHeight  - firstBlock))).toInt()
                    if (x == 0) x = 101 // indeterminate
                    setProgress(x)
                    binding.filterTransactionsIcon.isClickable = false
                    binding.syncStatusIcon.visibility=View.GONE
                    binding.syncFailIcon.visibility = View.GONE
                    binding.syncStatus.setTextColor(
                        ContextCompat.getColor(
                            requireActivity().applicationContext,
                            R.color.green_color
                        )
                    )
                } else {
                    balance = wallet.balance
                    ApplicationContext.getInstance(context).messageNotifier.setHomeScreenVisible(false)
                    sync = getString(R.string.status_synchronized)//getString(R.string.status_synced) + " " + formatter.format(wallet.blockChainHeight)
                    binding.syncStatus.setTextColor(
                        ContextCompat.getColor(
                            requireActivity().applicationContext,
                            R.color.green_color
                        )
                    )
                    setProgress(-2)
                    binding.filterTransactionsIcon.isClickable =
                        true //default = adapter!!.itemCount > 0
                    binding.syncStatusIcon.visibility=View.VISIBLE
                    binding.syncFailIcon.visibility = View.GONE
                    binding.syncStatusIcon.setOnClickListener {
                        if(CheckOnline.isOnline(requireActivity())){
                            if(wallet!=null) {
                                checkSyncInfo(requireActivity(),wallet.restoreHeight)
                            }
                        }
                    }
                }
            } else {
                binding.syncStatusIcon.visibility=View.GONE
                binding.syncFailIcon.visibility=View.VISIBLE
                binding.syncFailIcon.setOnClickListener {
                    if(CheckOnline.isOnline(requireActivity())){
                            checkSyncFailInfo(requireActivity())
                        }
                    }
                sync = getString(R.string.failed_connected_to_the_node)
                setProgress(101)
                binding.syncStatus.setTextColor(
                    ContextCompat.getColor(
                        requireActivity().applicationContext,
                        R.color.red
                    )
                )
            }
            setProgress(sync)
        }
        else
        {
            setProgress(getString(R.string.no_node_connection))
            binding.syncStatus.setTextColor(
                ContextCompat.getColor(
                    requireActivity().applicationContext,
                    R.color.red
                )
            )
            setProgress(101)
            binding.progressBar.indeterminateDrawable.setColorFilter(
                    ContextCompat.getColor(requireActivity().applicationContext,R.color.red),
                    android.graphics.PorterDuff.Mode.SRC_IN)
            binding.syncStatusIcon.visibility=View.GONE
            binding.syncFailIcon.visibility=View.GONE
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

    fun updateNodeFailureStatus() {
        binding.syncStatusIcon.visibility = View.GONE
        binding.syncFailIcon.visibility = View.VISIBLE
        binding.syncFailIcon.setOnClickListener {
            if (CheckOnline.isOnline(requireActivity())) {
                checkSyncFailInfo(requireActivity())
            }
        }
        setProgress(getString(R.string.failed_connected_to_the_node))
        setProgress(101)
        binding.syncStatus.setTextColor(
                ContextCompat.getColor(
                        requireActivity().applicationContext,
                        R.color.red
                )
        )
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
        binding.tvBalance.text ="---"
        binding.tvFiatCurrency.text="---"
    }

    private fun showSelectedDecimalBalance(balance: String, synchronized: Boolean){
        if(!synchronized){
            binding.fetchBalanceStatus.visibility =View.VISIBLE
            when {
                TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                    binding.tvBalance.text = "-.--"
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                    binding.tvBalance.text = "-.---"
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (000)" -> {
                    binding.tvBalance.text = "-"
                }
                else -> {
                    binding.tvBalance.text = "-.----"
                }
            }
            if (!activityCallback!!.isSynced) {
                binding.sendCardViewButton.isEnabled = false
                binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.send_button_disable_color))
                binding.scanQrCodeImg.isEnabled = false
                binding.sendCardViewButton.setBackgroundResource(R.drawable.send_card_background)
                binding.scanQrCodeImg.setImageResource(R.drawable.ic_wallet_scan_qr_disable)
            }
        }else{
            binding.fetchBalanceStatus.visibility =View.GONE
            when {
                TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                    binding.tvBalance.text = String.format("%.2f", balance.replace(",","").toDouble())
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                    binding.tvBalance.text = String.format("%.3f", balance.replace(",","").toDouble())
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (000)" -> {
                    binding.tvBalance.text = String.format("%.0f", balance.replace(",","").toDouble())
                }
                else -> {
                    binding.tvBalance.text = balance
                }
            }
            //SteveJosephh21
            if (activityCallback!!.isSynced) {
                binding.sendCardViewButton.isEnabled = true
                binding.sendCardViewButton.setBackgroundResource(R.drawable.send_card_enabled_background)
                binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.white))
                binding.scanQrCodeImg.isEnabled = true
                binding.scanQrCodeImg.setImageResource(R.drawable.ic_scan_qr)
            }
        }
        //Update Fiat Currency
        updateFiatCurrency(balance)
    }

    private fun updateFiatCurrency(balance: String) {
        if(balance.isNotEmpty() && balance!=null) {
            try {
                val amount: BigDecimal = BigDecimal(balance.replace(",","").toDouble()).multiply(BigDecimal(price))
                binding.tvFiatCurrency.text = getString(
                    R.string.fiat_currency,
                    amount.toDouble(),
                    TextSecurePreferences.getCurrency(requireActivity()).toString()
                )//"$price ${TextSecurePreferences.getCurrency(requireActivity()).toString()}"
            }catch (e:NumberFormatException){
                binding.tvFiatCurrency.text = getString(
                    R.string.fiat_currency,
                    0.00,
                    TextSecurePreferences.getCurrency(requireActivity()).toString())
            }catch(e:IllegalStateException){
                Log.d("FiatCurrency Exception: ",e.message.toString())
            }
        }
    }

    private fun showBalance(balance: String?,synchronized: Boolean,fullBalance:String?) {
        if(mContext!=null) {
            when {
                TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 2 -> {
                    binding.fetchBalanceStatus.visibility =View.GONE
                    hideDisplayBalance()
                }
                TextSecurePreferences.getDisplayBalanceAs(mContext!!) == 0 -> {
                    walletAvailableBalance = fullBalance
                    walletSynchronized = synchronized
                    showSelectedDecimalBalance(fullBalance!!, synchronized)
                }
                else -> {
                    if(unlockedBalance.toString() == "-1"){
                        binding.fetchBalanceStatus.visibility =View.VISIBLE
                        when {
                            TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                                binding.tvBalance.text = "-.--"
                            }
                            TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                                binding.tvBalance.text = "-.---"
                            }
                            TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (000)" -> {
                                binding.tvBalance.text = "-"
                            }
                            else -> {
                                binding.tvBalance.text = "-.----"
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

        val streetMode: Boolean = activityCallback!!.isStreetMode
        if (!streetMode) {
            binding.llBalance.visibility = View.VISIBLE
        } else {
            binding.llBalance.visibility = View.INVISIBLE
        }
    }

    var activityCallback: Listener? = null

    // Container Activity must implement this interface
    interface Listener {
        fun hasBoundService(): Boolean
        fun forceUpdate(requireActivity: Context)
        val connectionStatus: Wallet.ConnectionStatus?
        val daemonHeight: Long

        fun onSendRequest(view: View?)
        fun onTxDetailsRequest(view: View?, info: TransactionInfo?)
        val isSynced: Boolean
        val isStreetMode: Boolean
        val streetModeHeight: Long

        fun getTxKey(txId: String?): String?
        fun onWalletReceive(view: View?)
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
    }

    private var accountIndex = 0

    fun onRefreshed(wallet: Wallet, full: Boolean) {
        var full = full
        if (adapter!!.needsTransactionUpdateOnNewBlock()) {
            full = true
        }
        if (full && activityCallback!!.isSynced) {
            val list: MutableList<TransactionInfo> = ArrayList()
            val streetHeight: Long = activityCallback!!.streetModeHeight
            wallet.refreshHistory()
            for (info in wallet.history.all) {
                if ((info.isPending || info.blockheight >= streetHeight)
                    && !dismissedTransactions.contains(info.hash)
                ) list.add(info)
            }
            adapter!!.setInfos(list)
            adapterItems.clear()
            adapterItems.addAll(adapter!!.infoItems!!)
            if (accountIndex != wallet.accountIndex) {
                accountIndex = wallet.accountIndex
                binding.transactionList.scrollToPosition(0)
            }

            //SteveJosephh21
            if (adapter!!.itemCount > 0) {
                binding.transactionList.visibility = View.VISIBLE
                binding.emptyContainerLayout.visibility = View.GONE
            } else {
                binding.filterTransactionsIcon.isClickable = true // default = false
                binding.transactionList.visibility = View.GONE
                binding.emptyContainerLayout.visibility = View.VISIBLE
            }
            //Steve Josephh21 ANRS
            if(CheckOnline.isOnline(requireContext())) {
                check(activityCallback!!.hasBoundService()) { "WalletService not bound." }
                val daemonConnected: Wallet.ConnectionStatus = activityCallback!!.connectionStatus!!
                if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                    AsyncGetUnlockedBalance(wallet).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
                }
            }
        }
        updateStatus(wallet)
    }


    override fun onBackPressed(): Boolean {
        return false
    }
}
//endregion
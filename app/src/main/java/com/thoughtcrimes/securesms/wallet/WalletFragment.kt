package com.thoughtcrimes.securesms.wallet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.transition.MaterialElevationScale
import com.google.gson.GsonBuilder
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.NodePinger
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeApi
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeRate
import com.thoughtcrimes.securesms.wallet.utils.common.FiatCurrencyPrice
import com.thoughtcrimes.securesms.wallet.utils.common.fetchPriceFor
import com.thoughtcrimes.securesms.wallet.utils.helper.ServiceHelper
import com.thoughtcrimes.securesms.wallet.widget.Toolbar
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentWalletBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.lang.ClassCastException
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import java.text.SimpleDateFormat
import android.R.array





class WalletFragment : Fragment(), TransactionInfoAdapter.OnInteractionListener {

    private var adapter: TransactionInfoAdapter? = null
    private val formatter = NumberFormat.getInstance()

    private var syncText: String? = null

    private var adapterItems: ArrayList<TransactionInfo> = ArrayList()

    private var walletAvailableBalance: String? =null
    private var walletSynchronized:Boolean = false
    private val useSSL: Boolean = false
    private val isLightWeight: Boolean = false

    fun setProgress(text: String?) {
        if(text==getString(R.string.reconnecting) || text==getString(R.string.status_wallet_connecting)){
           binding.syncStatusIcon.visibility=View.GONE
        }
        syncText = text
        binding.syncStatus.text = text
    }
    var onScanListener: OnScanListener? = null

    interface OnScanListener {
        /*fun onScan()*/
        fun onWalletScan(view: View?)

    }

    private var syncProgress = -1

    fun setProgress(n: Int) {
        Log.d("Beldex","mConnection value of n $n")
        syncProgress = n
        if (n > 100) {
            binding.progressBar.isIndeterminate = true
            binding.progressBar.visibility = View.VISIBLE
        } else if (n >= 0) {
            binding.progressBar.isIndeterminate = false
            binding.progressBar.progress = n
            /*if (tvWalletAccountStatus.getText() === "") {
                tvWalletAccountStatus.setText("--")
            }*/
            /*if (binding.walletName.text === "") {
                binding.walletName.text = "--"
            }*/
            binding.progressBar.visibility = View.VISIBLE
        } else if(n==-2){
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.isIndeterminate = false
            binding.progressBar.progress=100
        }else { // <0
            binding.progressBar.visibility = View.GONE
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
        if (!activityCallback?.isWatchOnly!!) {
            binding.sendCardViewButton.isEnabled = true
            binding.sendCardViewButton.setBackgroundResource(R.drawable.send_card_enabled_background)
            binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.white))
            binding.scanQrCodeImg.isEnabled = true
            binding.scanQrCodeImg.setImageResource(R.drawable.ic_scan_qr)
        }
        //if (isVisible) enableAccountsList(true) //otherwise it is enabled in onResume()
    }

    fun unsync() {
        if (!activityCallback!!.isWatchOnly) {
            binding.sendCardViewButton.isEnabled = false
            binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.send_button_disable_color))
            binding.scanQrCodeImg.isEnabled = false
            binding.sendCardViewButton.setBackgroundResource(R.drawable.send_card_background)
            binding.scanQrCodeImg.setImageResource(R.drawable.ic_wallet_scan_qr_disable)
            //binding.progressBar.show()
        }
        //if (isVisible) enableAccountsList(false) //otherwise it is enabled in onResume()
        firstBlock = 0
    }

    private var walletTitle: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
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

    override fun onResume() {
        super.onResume()
        if(TextSecurePreferences.getDisplayBalanceAs(requireActivity())==2) {
            hideDisplayBalance()
        }else{
            showSelectedDecimalBalance(walletAvailableBalance!!, walletSynchronized)
        }
        if(TextSecurePreferences.getChangedCurrency(requireActivity())) {
            TextSecurePreferences.changeCurrency(requireActivity(),false)
            callCurrencyConversionApi()
        }

        exitTransition = null
        reenterTransition = null
        Timber.d("onResume()")
        activityCallback!!.setTitle(getString(R.string.my_wallet))
        activityCallback!!.setToolbarButton(Toolbar.BUTTON_BACK)
        //binding.walletName.text = walletTitle
        //Important
        //tvWalletAccountStatus.setText(walletSubtitle)
        setProgress(syncProgress)
        setProgress(syncText)
        showReceive()
        //if (activityCallback!!.isSynced) enableAccountsList(true)

        //SteveJosephh21 Log
        pingSelectedNode()
    }

    private fun callCurrencyConversionApi(){
        val currency = TextSecurePreferences.getCurrency(requireActivity()).toString().lowercase()
        fetchPriceFor(
            TextSecurePreferences.getCurrency(requireActivity()).toString(),
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("WalletFragment","onFailure")
                    price = 0.00
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("WalletFragment","onResponse()")
                    Log.d("Beldex", "Fiat ${response.isSuccessful}")
                    if (response.isSuccessful) {
                        Log.d("WalletFragment","onResponse() success")
                        if (response.body != null) {
                            val json = JSONObject(response.body!!.string())
                            val result = json.getJSONObject("beldex")
                            if(result.length()!=0) {
                                price = result.getDouble(currency)
                                TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                                Log.d("Beldex", "Fiat if wallet screen -- ${price}")
                            }else{
                                Log.d("FetchPriceFor -> ", "empty")
                                price = 0.00
                                TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                                Log.d("Beldex", "Fiat else wallet screen -- ${price}")
                            }
                        }
                    } else {
                        Log.d("WalletFragment","onResponse() fail")
                        price = 0.00
                        TextSecurePreferences.setCurrencyAmount(requireActivity(),price.toString())
                        Log.d("Beldex", "Fiat else wallet screen -- ${price}")
                    }
                }
            }
        )
    }

    fun pingSelectedNode() {
        Log.d("Beldex","Value of current node loadFav pinSelec")
        val PING_SELECTED = 0
        val FIND_BEST = 1
       /* if(TextSecurePreferences.getDaemon(requireActivity())) {
            AsyncFindBestNode(PING_SELECTED, FIND_BEST).execute<Int>(FIND_BEST)
        }else{
            AsyncFindBestNode(PING_SELECTED, FIND_BEST).execute<Int>(PING_SELECTED)
        }*/
        AsyncFindBestNode(PING_SELECTED, FIND_BEST).execute<Int>(PING_SELECTED)
    }

    inner class AsyncFindBestNode(val PING_SELECTED: Int, val FIND_BEST: Int) :
        AsyncTaskCoroutine<Int?, NodeInfo?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            //pbNode.setVisibility(View.VISIBLE)
            //showProgressDialogWithTitle("Connecting to Remote Node");
            //llNode.setVisibility(View.INVISIBLE)
        }

        override fun doInBackground(vararg params: Int?): NodeInfo? {
            Log.d("Beldex","called AsyncFindBestNode")

            val favourites: Set<NodeInfo?> = activityCallback!!.getOrPopulateFavourites()
            var selectedNode: NodeInfo?
            Log.d("Beldex","selected node 1 $favourites")
            if (params[0] == FIND_BEST) {
                Log.d("Beldex","called AsyncFindBestNode 1")
                selectedNode = autoselect(favourites)
                Log.d("Beldex","selected node 2 $selectedNode")
            } else if (params[0] == PING_SELECTED) {
                Log.d("Beldex","called AsyncFindBestNode 2")
                selectedNode = activityCallback!!.getNode()
                Log.d("Beldex","selected node 3 $selectedNode")
                Log.d("Beldex","called AsyncFindBestNode 2 ${selectedNode?.host}")

                if (!activityCallback!!.getFavouriteNodes().contains(selectedNode))
                    selectedNode = null // it's not in the favourites (any longer)
                if (selectedNode == null)
                    Log.d("Beldex","selected node 4 $selectedNode")
                    for (node in favourites) {
                        if (node!!.isSelected) {
                            Log.d("Beldex","selected node 5 $node")
                            selectedNode = node
                            break
                        }
                    }
                if (selectedNode == null) { // autoselect
                    selectedNode = autoselect(favourites)
                } else {
                    //Steve Josephh21 //BCA-402
                    if(selectedNode!=null) {
                        Log.d("Beldex", "selected node 6 $selectedNode")
                        selectedNode!!.testRpcService()
                    }
                }
            } else throw IllegalStateException()
            return if (selectedNode != null && selectedNode.isValid) {
                Log.d("Testing-->12", "true")
                activityCallback!!.setNode(selectedNode)
                selectedNode
            } else {
                Log.d("Testing-->13", "true")
                activityCallback!!.setNode(null)
                null
            }
        }

        override fun onPostExecute(result: NodeInfo?) {
            //if (!isAdded()) return
            //pbNode.setVisibility(View.INVISIBLE)
            //hideProgressDialogWithTitle();
            //llNode.setVisibility(View.VISIBLE)
           /* if (result != null) {
                Log.d("Beldex", "Called onPostExecute ${result?.host}")
                Toast.makeText(
                    requireActivity().applicationContext,
                    "Connected to ${result!!.name}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("WalletFragment", "AsyncFindBestNode Success")
            }
            else {
                Log.d("WalletFragment", "AsyncFindBestNode Fail")
            }*/
                //Important
                /*d("found a good node %s", result.toString())
                val ctx: Context = tvNodeAddress.getContext()
                val now = Calendar.getInstance().timeInMillis / 1000
                val secs: Long = now - result.getTimestamp()
                val mins = secs / 60 // in minutes
                val hours = mins / 60
                val days = hours / 24
                val msg: String
                msg = if (mins < 2) {
                    ctx.getString(R.string.node_updated_now, secs)
                } else if (hours < 2) {
                    ctx.getString(R.string.node_updated_mins, mins)
                } else if (days < 2) {
                    ctx.getString(R.string.node_updated_hours, hours)
                } else {
                    ctx.getString(R.string.node_updated_days, days)
                }
                Toast.makeText(
                    context,
                    result.getName().toString() + " connected\n" + msg,
                    Toast.LENGTH_SHORT
                ).show()
                showNode(result)*/

                //Important
                /* tvNodeName.setText(getResources().getText(R.string.node_create_hint))
                 tvNodeName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                 tvNodeAddress.setText(null)
                 tvNodeAddress.setVisibility(View.GONE)*/

        }

        /* override fun onCancelled(result: NodeInfo?) { //TODO: cancel this on exit from fragment
             Log.d("cancelled with %s", result)
         }*/
    }

    fun autoselect(nodes: Set<NodeInfo?>): NodeInfo? {
        if (nodes.isEmpty()) return null
        NodePinger.execute(nodes, null)
        val nodeList: ArrayList<NodeInfo?> = ArrayList<NodeInfo?>(nodes)
        Collections.sort(nodeList, NodeInfo.BestNodeComparator)
        val rnd = Random().nextInt(nodeList.size)
        return nodeList[rnd]
    }

    interface DrawerLocker {
        fun setDrawerEnabled(enabled: Boolean)
    }

    override fun onPause() {
        //enableAccountsList(false)
        super.onPause()
    }

    /*private fun enableAccountsList(enable: Boolean) {
        if (activityCallback is DrawerLocker) {
            (activityCallback as DrawerLocker).setDrawerEnabled(
                enable
            )
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /*if (activityCallback!!.hasWallet())
            inflater.inflate(R.menu.wallet_menu, menu)*/
        super.onCreateOptionsMenu(menu, inflater)
    }


    private lateinit var binding: FragmentWalletBinding

    var price =0.00

    @SuppressLint("ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Beldex","Value of current node ")
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        //Get Selected Fiat Currency Price
        //if(TextSecurePreferences.getFiatCurrencyCheckedStatus(requireActivity())) {
            if(TextSecurePreferences.getFiatCurrencyApiStatus(requireActivity())) {
                TextSecurePreferences.callFiatCurrencyApi(requireActivity(),false)
                Log.d("WalletFragment ","callFiatCurrencyApi true")
                callCurrencyConversionApi()
            }else{
                Log.d("WalletFragment ","callFiatCurrencyApi false")
                price = if(TextSecurePreferences.getCurrencyAmount(requireActivity())!=null){
                    TextSecurePreferences.getCurrencyAmount(requireActivity())!!.toDouble()
                }else{ 0.00}
            }
        /*}else{
            binding.tvFiatCurrency.text = "--"
        }*/
       /* Log.d("Beldex","isOnline 2 ${CheckOnline.isOnline(requireContext())}")
        if(!CheckOnline.isOnline(requireContext()))
        {
            Log.d("Beldex","isOnline 2 ${CheckOnline.isOnline(requireContext())}")
            setProgress(R.string.no_node_connection)
            binding.syncStatus.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
        }*/

        binding.sendCardViewButton.isEnabled = false
        binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.send_button_disable_color))
        binding.scanQrCodeImg.isEnabled = false
        binding.scanQrCodeImg.setImageResource(R.drawable.ic_wallet_scan_qr_disable)

        //binding.walletName.text = walletTitle
        //Important
        //tvWalletAccountStatus.setText(walletSubtitle)
        Log.d("showBalance->","onCreateView")
        showBalance(Helper.getDisplayAmount(0),walletSynchronized,Helper.getDisplayAmount(0))
        showUnconfirmed(0.0)

        adapter = TransactionInfoAdapter(activity, this)
        binding.transactionList.adapter = adapter
        /*adapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0 && binding.transactionList.computeVerticalScrollOffset() == 0) binding.transactionList.scrollToPosition(
                    positionStart
                )
            }
        })*/
        binding.transactionList.isNestedScrollingEnabled = false

        //      int count =  adapter.getItemCount();
//      Timber.d ("Adapter count %s", adapter.getItemCount());
//        anchorBehavior.setHideable(count == 0);
        /*binding.transactionList.addOnItemTouchListener(
            SwipeableRecyclerViewTouchListener(binding.transactionList,
                object : SwipeableRecyclerViewTouchListener.SwipeListener {
                    override fun canSwipeLeft(position: Int): Boolean {
                        return activityCallback!!.isStreetMode
                    }

                    override fun canSwipeRight(position: Int): Boolean {
                        return activityCallback!!.isStreetMode
                    }

                    override fun onDismissedBySwipeLeft(
                        recyclerView: RecyclerView?,
                        reverseSortedPositions: IntArray
                    ) {
                        for (position in reverseSortedPositions) {
                            dismissedTransactions.add(adapter!!.getItem(position).hash)
                            adapter!!.removeItem(position)
                        }
                    }

                    override fun onDismissedBySwipeRight(
                        recyclerView: RecyclerView?,
                        reverseSortedPositions: IntArray
                    ) {
                        for (position in reverseSortedPositions) {
                            dismissedTransactions.add(adapter!!.getItem(position).hash)
                            adapter!!.removeItem(position)
                        }
                    }
                })
        )*/

        binding.sendCardViewButton.setOnClickListener{ v: View? ->
            activityCallback!!.onSendRequest(v)
        }
        binding.receiveCardViewButton.setOnClickListener{ v: View? ->
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
            //val popupMenu = PopupMenu(activity?.applicationContext, it)
            popupMenu.inflate(R.menu.filter_transactions_popup_menu)
            popupMenu.setOnDismissListener{
                if(dismissPopupMenu)it.show()
                else {
                    binding.filterTransactionsIcon.isClickable =true
                    it.dismiss()
                }
                dismissPopupMenu=false
            }
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                popupMenu.setForceShowIcon(true)
            }*/
            val spanString = SpannableString(popupMenu.menu[0].title.toString())
            spanString.setSpan(RelativeSizeSpan(1.2f), 0,spanString.length, 0);
            spanString.setSpan(StyleSpan(Typeface.BOLD), 0, spanString.length, 0)
            spanString.setSpan(
                ForegroundColorSpan(
                    ResourcesCompat
                        .getColor(resources, R.color.text, null)
                ),
                0, spanString.length, 0
            )
           /* if (TextSecurePreferences.getIncomingTransactionStatus(requireActivity())) {
                filter(TransactionInfo.Direction.Direction_In, adapter!!.infoItems!!)
            }*/
            popupMenu.menu[0].title = spanString
            popupMenu.menu[0].isEnabled = false
            popupMenu.menu[1].isChecked =
                TextSecurePreferences.getIncomingTransactionStatus(requireActivity())
            popupMenu.menu[2].isChecked = TextSecurePreferences.getOutgoingTransactionStatus(requireActivity())
            //popupMenu.menu[3].isChecked = TextSecurePreferences.getTransactionsByDateStatus(requireActivity())
            popupMenu.setOnMenuItemClickListener { item ->
                dismissPopupMenu=true
                Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show()
                val emptyList: ArrayList<TransactionInfo> = ArrayList()
                if (item.title == "Incoming") {
                    Log.d("Beldex","filter issue incoming if 1")
                    item.isChecked = !item.isChecked
                    if(popupMenu.menu[2].isChecked && item.isChecked){
                        Log.d("Beldex","filter issue incoming if 2 ${adapterItems.size}")
                        TextSecurePreferences.setIncomingTransactionStatus(requireActivity(), true)
                        Log.d("Beldex","filter issue incoming if 2,, ${adapterItems.size}")
                        filterAll(adapterItems)

                    }else if (item.isChecked && !popupMenu.menu[2].isChecked) {
                            Log.d("Beldex","filter issue incoming if 3")
                            TextSecurePreferences.setIncomingTransactionStatus(
                                requireActivity(),
                                true
                            )
                            filter(TransactionInfo.Direction.Direction_In, adapterItems)
                        } else if(!item.isChecked && popupMenu.menu[2].isChecked) {
                            Log.d("Beldex", "filter issue incoming if 4")
                            TextSecurePreferences.setIncomingTransactionStatus(
                                requireActivity(),
                                false
                            )
                        Log.d("Beldex","filter issue incoming if 4 adapterItemss.size ${adapterItems.size}")
                            filter(TransactionInfo.Direction.Direction_Out, adapterItems)
                        }
                    else if(!popupMenu.menu[2].isChecked && !item.isChecked){
                        Log.d("Beldex","filter issue incoming if 5")
                        emptyList
                    }

                } else if (item.title == "Outgoing") {
                    Log.d("Beldex","filter issue outgoing if 1")
                    item.isChecked = !item.isChecked
                    if(popupMenu.menu[1].isChecked && item.isChecked){
                        Log.d("Beldex","filter issue outgoing if 2")
                        TextSecurePreferences.setOutgoingTransactionStatus(requireActivity(), true)
                        filterAll(adapterItems)

                    }else if (item.isChecked && !popupMenu.menu[1].isChecked ) {
                            Log.d("Beldex","filter issue outgoing if 3")
                            TextSecurePreferences.setOutgoingTransactionStatus(
                                requireActivity(),
                                true
                            )
                            filter(TransactionInfo.Direction.Direction_Out, adapterItems)
                        } else if (!item.isChecked && popupMenu.menu[1].isChecked ) {
                            Log.d("Beldex", "filter issue outgoing if 4")
                            TextSecurePreferences.setOutgoingTransactionStatus(
                                requireActivity(),
                                false
                            )
                            filter(TransactionInfo.Direction.Direction_In, adapterItems)
                        }
                        else if(!popupMenu.menu[1].isChecked && !item.isChecked){
                            Log.d("Beldex", "filter issue outgoing if 5")
                            emptyList
                        }

                }else{
                    val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                        .setTheme(R.style.MaterialCalendarTheme)
                        .build()
                    datePicker.show(requireActivity().supportFragmentManager, "DatePicker")

                    // Setting up the event for when ok is clicked
                    datePicker.addOnPositiveButtonClickListener {
                        Toast.makeText(context, "${datePicker.headerText} ${datePicker.selection!!.first}, ${datePicker.selection!!.second} ${Date(datePicker.selection!!.first!!)}, ${Date(datePicker.selection!!.second!!)}is selected", Toast.LENGTH_LONG).show()
                        if(popupMenu.menu[1].isChecked && popupMenu.menu[2].isChecked){
                            filterTransactionsByDate(getDaysBetweenDates(Date(datePicker.selection!!.first!!),Date(datePicker.selection!!.second!!)),adapterItems)
                        }else if(popupMenu.menu[1].isChecked){
                            filterTransactionsByDate(getDaysBetweenDates(Date(datePicker.selection!!.first!!),Date(datePicker.selection!!.second!!)),filterTempList(TransactionInfo.Direction.Direction_In, adapterItems))
                        }else if(popupMenu.menu[2].isChecked){
                            filterTransactionsByDate(getDaysBetweenDates(Date(datePicker.selection!!.first!!),Date(datePicker.selection!!.second!!)),filterTempList(TransactionInfo.Direction.Direction_Out, adapterItems))
                        }else{

                            filterTransactionsByDate(getDaysBetweenDates(Date(datePicker.selection!!.first!!),Date(datePicker.selection!!.second!!)),emptyList)
                        }
                    }

                    // Setting up the event for when cancelled is clicked
                    datePicker.addOnNegativeButtonClickListener {
                        Toast.makeText(context, "${datePicker.headerText} ${datePicker.selection!!.first}, ${datePicker.selection!!.second} is cancelled", Toast.LENGTH_LONG).show()
                    }

                    // Setting up the event for when back button is pressed
                    datePicker.addOnCancelListener {
                        Toast.makeText(context, "Date Picker Cancelled", Toast.LENGTH_LONG).show()
                    }
                    /*val callback = RangeDaysPickCallback {startDate,endDate->
                        Toast.makeText(requireActivity(),"${startDate.longDateString}, ${endDate.longDateString}",Toast.LENGTH_SHORT).show()
                        if(popupMenu.menu[1].isChecked && popupMenu.menu[2].isChecked){
                            filterTransactionsByDate(getDaysBetweenDates(startDate.getTime(),endDate.getTime()),adapterItems)
                        }else if(popupMenu.menu[1].isChecked){
                            filterTransactionsByDate(getDaysBetweenDates(startDate.getTime(),endDate.getTime()),filterTempList(TransactionInfo.Direction.Direction_In, adapterItems))
                        }else if(popupMenu.menu[2].isChecked){
                            filterTransactionsByDate(getDaysBetweenDates(startDate.getTime(),endDate.getTime()),filterTempList(TransactionInfo.Direction.Direction_Out, adapterItems))
                        }else{

                            filterTransactionsByDate(getDaysBetweenDates(startDate.getTime(),endDate.getTime()),emptyList)
                        }
                    }
                    val today: PrimeCalendar = CivilCalendar(locale = Locale.ENGLISH).also { civilCalendar->
                        civilCalendar.year = 2022                       // determines starting year
                        civilCalendar.month = 9                         // determines starting month
                        civilCalendar.firstDayOfWeek = Calendar.MONDAY  // sets first day of week to Monday
                    }

                    val datePicker = dialogWith(today)
                        .pickRangeDays(callback)
                        .build()
                    datePicker.show(requireActivity().supportFragmentManager, "SOME_TAG")*/
                }
                false
            }
            popupMenu.show()
        }
        binding.scanQrCodeImg.setOnClickListener {
            onScanListener?.onWalletScan(view)
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
        Log.d("Beldex","filter issue filterall called 1 $arrayList")
        Log.d("Beldex","filter issue filterall called 1,, ${arrayList.size}")
        for (d in arrayList) {
            Log.d("Beldex","filter issue filterall called 2")
            temp.add(d)
        }
        Log.d("Beldex","filter issue filterall called 3")
        callIfTransactionListEmpty(temp.size)
        //update recyclerview
        Log.d("Beldex","filter issue filterall called 4")
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
                Log.d("Transaction Date -> ${getDateTime(d.timestamp)} ","Selected Dates -> ${DATETIME_FORMATTER.format(datesItem)}")
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
        Log.d("Beldex","value of startDate 1 $startDate")
        Log.d("Beldex","value of endDate 1 $endDate")
        val dates: MutableList<Date> = ArrayList()
        val calendar: Calendar = GregorianCalendar()
        calendar.time = startDate
        Log.d("Beldex","value of startDate 2 $startDate")
        Log.d("Beldex","value of endDate 2 $endDate")
        while (calendar.time.before(endDate)) {
            val result = calendar.time
            dates.add(result)
            calendar.add(Calendar.DATE, 1)
        }
        return dates
    }


    /* fun onViewCreated(@NonNull view: View?, @Nullable savedInstanceState: Bundle?) {
         binding.notesRecyclerView.setVisibility(View.VISIBLE) //this hide/show recyclerview visibility
         Log.d("TAG", "hidden: ")
     }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.viewTreeObserver.addOnPreDrawListener {
            startPostponedEnterTransition()
            true
        }
    }

    private fun setActivityTitle(wallet: Wallet?) {
        if (wallet == null) return
        walletTitle = wallet.name
        //Important
        //walletSubtitle = wallet.accountLabel
        //binding.walletName.text = walletTitle
        binding.transactionTitle.visibility = View.VISIBLE
        binding.transactionLayoutCardView.visibility = View.VISIBLE
        //Important
        //tvWalletAccountStatus.setText(walletSubtitle)
        activityCallback!!.setTitle(getString(R.string.my_wallet))
        Timber.d("wallet title is %s", walletTitle)
    }

    private var firstBlock: Long = 0
    private var unlockedBalance: Long = 0
    private var balance: Long = 0
    private var accountIdx = -1

    private fun updateStatus(wallet: Wallet) {
        if (!isAdded) return
        Log.d("Beldex", "updateStatus()")
        if (walletTitle == null || accountIdx != wallet.accountIndex) {
            accountIdx = wallet.accountIndex
            setActivityTitle(wallet)
        }
        Log.d("Beldex", "isOnline 0  ${CheckOnline.isOnline(requireContext())}")
        if(CheckOnline.isOnline(requireContext())) {
            Log.d("Beldex", "isOnline 1  ${CheckOnline.isOnline(requireContext())}")
            balance = wallet.balance
            Log.d("Beldex", "value of balance $balance")
            unlockedBalance = wallet.unlockedBalance
            refreshBalance(wallet.isSynchronized)
            val sync: String
            check(activityCallback!!.hasBoundService()) { "WalletService not bound." }
            val daemonConnected: Wallet.ConnectionStatus = activityCallback!!.connectionStatus!!
            Log.d("Beldex","Value of daemon connection $daemonConnected")
            if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                if (!wallet.isSynchronized) {
                    val daemonHeight: Long = activityCallback!!.daemonHeight
                    val walletHeight = wallet.blockChainHeight
                    val n = daemonHeight - walletHeight
                    sync = formatter.format(n) + " " + getString(R.string.status_remaining)
                    if (firstBlock == 0L) {
                        firstBlock = walletHeight
                    }
                    var x = (100 - Math.round(100f * n / (1f * daemonHeight - firstBlock))).toInt()
                    if (x == 0) x = 101 // indeterminate
                    setProgress(x)
//                ivSynced.setVisibility(View.GONE);
                    binding.filterTransactionsIcon.isClickable = false
                    //activityCallback!!.hiddenRescan(false)
                    binding.syncStatusIcon.visibility=View.GONE
                } else {
                    Log.d("showBalance->","Synchronized")
                    sync =
                        getString(R.string.status_synchronized)//getString(R.string.status_synced) + " " + formatter.format(wallet.blockChainHeight)
                    //binding.syncStatus.setTextColor(resources.getColor(R.color.green_color))
                    binding.syncStatus.setTextColor(
                        ContextCompat.getColor(
                            requireActivity().applicationContext,
                            R.color.green_color
                        )
                    )
//                ivSynced.setVisibility(View.VISIBLE);
                    binding.filterTransactionsIcon.isClickable =
                        true //default = adapter!!.itemCount > 0
                    //activityCallback!!.hiddenRescan(true)
                    binding.syncStatusIcon.visibility=View.VISIBLE
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
                sync = getString(R.string.status_wallet_connecting)
                setProgress(101)
                binding.transactionTitle.visibility = View.INVISIBLE
                binding.transactionLayoutCardView.visibility = View.GONE
                //anchorBehavior.setHideable(true)
            }
            setProgress(sync)
        }
        else
        {
            Log.d("Beldex","isOnline else 2")
            setProgress(getString(R.string.no_node_connection))
            binding.syncStatus.setTextColor(
                ContextCompat.getColor(
                    requireActivity().applicationContext,
                    R.color.red
                )
            )
            binding.syncStatusIcon.visibility=View.GONE
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

    var balanceCurrency = Helper.BASE_CRYPTO
    var balanceRate = 1.0

    private val exchangeApi: ExchangeApi = ServiceHelper.getExchangeApi()

    private fun refreshBalance(synchronized: Boolean) {
        Log.d("showBalance->","refreshBalance()")
        val unconfirmedBdx: Double = Helper.getDecimalAmount(balance - unlockedBalance).toDouble()
        showUnconfirmed(unconfirmedBdx)

        val amountBdx: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        val amountFullBdx: Double = Helper.getDecimalAmount(balance).toDouble()
        Log.d("Beldex", "value of amountBdx $amountBdx")
        Log.d("Beldex", "value of amountFullBdx $amountFullBdx")
        Log.d("Beldex", "value of helper amountBdx" + Helper.getFormattedAmount(amountBdx, true))
        showBalance(Helper.getFormattedAmount(amountBdx, true),synchronized,Helper.getFormattedAmount(amountFullBdx, true))
    }

    //Important
    /* private fun refreshBalance() {
         val unconfirmedBdx: Double = Helper.getDecimalAmount(balance - unlockedBalance).toDouble()
         showUnconfirmed(unconfirmedBdx)
         if (sCurrency.getSelectedItemPosition() == 0) { // BDX
             val amountBdx: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
             showBalance(Helper.getFormattedAmount(amountBdx, true))
         } else { // not BDX
             val currency = sCurrency.getSelectedItem() as String
             Timber.d(currency)
             if (currency != balanceCurrency || balanceRate <= 0) {
                 showExchanging()
                 exchangeApi.queryExchangeRate(Helper.BASE_CRYPTO, currency,
                     object : ExchangeCallback {
                         override fun onSuccess(exchangeRate: ExchangeRate?) {
                             if (isAdded) Handler(Looper.getMainLooper()).post {
                                 exchange(
                                     exchangeRate!!
                                 )
                             }
                         }

                         override fun onError(e: Exception) {
                             Timber.e(e.localizedMessage)
                             if (isAdded) Handler(Looper.getMainLooper()).post { exchangeFailed() }
                         }
                     })
             } else {
                 updateBalance()
             }
         }
     }*/

    private fun showUnconfirmed(unconfirmedAmount: Double) {
        if (!activityCallback!!.isStreetMode) {
            val unconfirmed = Helper.getFormattedAmount(unconfirmedAmount, true)
            //Important
            /* tvUnconfirmedAmount.setText(
                 resources.getString(
                     R.string.bdx_unconfirmed_amount,
                     unconfirmed
                 )
             )*/
        } else {
            //Important
            //tvUnconfirmedAmount.setText(null)
        }
    }

    private fun updateBalance() {
        if (isExchanging) return  // wait for exchange to finbalanceCurrencyish - it will fire this itself then.
        // at this point selection is BDX in case of error
        val displayB: String
        val amountA: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        displayB = if (!Helper.BASE_CRYPTO.equals(balanceCurrency)) { // not BDX
            val amountB: Double = amountA * balanceRate
            Helper.getFormattedAmount(amountB, false)
        } else { // BDX
            Helper.getFormattedAmount(amountA, true)
        }


        val displayFullB: String
        val amountFullA: Double = Helper.getDecimalAmount(balance).toDouble()
        displayFullB = if (!Helper.BASE_CRYPTO.equals(balanceCurrency)) { // not BDX
            val amountFullB: Double = amountFullA * balanceRate
            Helper.getFormattedAmount(amountFullB, false)
        } else { // BDX
            Helper.getFormattedAmount(amountFullA, true)
        }
        Log.d("sync updateBalance()", "true")
        Log.d("showBalance->","UpdateBalance()")
        showBalance(displayB,walletSynchronized,displayFullB)
    }

    private fun hideDisplayBalance(){
        binding.tvBalance.text ="-.----"
        binding.tvFiatCurrency.text="-.----"
    }

    private fun showSelectedDecimalBalance(balance: String, synchronized: Boolean){
        TextSecurePreferences.getDecimals(requireActivity())?.let { Log.d("Decimal", it) }
        if(!synchronized){
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
        }else{
            when {
                TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                    binding.tvBalance.text = String.format("%.2f", balance.toDouble())
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                    binding.tvBalance.text = String.format("%.3f", balance.toDouble())
                }
                TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (000)" -> {
                    binding.tvBalance.text = String.format("%.0f", balance.toDouble())
                }
                else -> {
                    binding.tvBalance.text = balance
                }
            }
        }
        //Update Fiat Currency
        if(balance.isNotEmpty()) {
            val amount: BigDecimal = BigDecimal(balance.toDouble()).multiply(BigDecimal(price))
            binding.tvFiatCurrency.text = getString(R.string.fiat_currency,amount.toDouble(),TextSecurePreferences.getCurrency(requireActivity()).toString())//"$price ${TextSecurePreferences.getCurrency(requireActivity()).toString()}"
        }
    }

    private fun showBalance(balance: String?,synchronized: Boolean,fullBalance:String?) {
        when {
            TextSecurePreferences.getDisplayBalanceAs(requireActivity())==2 -> {
                hideDisplayBalance()
            }
            TextSecurePreferences.getDisplayBalanceAs(requireActivity())==0 -> {
                walletAvailableBalance = fullBalance
                walletSynchronized = synchronized
                showSelectedDecimalBalance(fullBalance!!,synchronized)
            }
            else -> {
                walletAvailableBalance = balance
                walletSynchronized = synchronized
                showSelectedDecimalBalance(balance!!,synchronized)
            }
        }

        val streetMode: Boolean = activityCallback!!.isStreetMode
        if (!streetMode) {
            binding.llBalance.visibility = View.VISIBLE
            //Important
            //tvStreetView.setVisibility(View.INVISIBLE)
        } else {
            binding.llBalance.visibility = View.INVISIBLE
            //Important
            //tvStreetView.setVisibility(View.VISIBLE)
        }
        setStreetModeBackground(streetMode)
    }

    var activityCallback: Listener? = null

    // Container Activity must implement this interface
    interface Listener {
        fun hasBoundService(): Boolean
        fun forceUpdate(requireActivity: Context)
        val connectionStatus: Wallet.ConnectionStatus?

        //mBoundService.getDaemonHeight();
        val daemonHeight: Long

        fun onSendRequest(view: View?)
        fun onTxDetailsRequest(view: View?, info: TransactionInfo?)
        val isSynced: Boolean
        val isStreetMode: Boolean
        val streetModeHeight: Long
        val isWatchOnly: Boolean

        fun getTxKey(txId: String?): String?
        fun onWalletReceive(view: View?)
        fun hasWallet(): Boolean
        fun getWallet(): Wallet?

        fun setToolbarButton(type: Int)
        fun setTitle(title: String?)
        fun setTitle(title: String?, subtitle: String?)
        fun setSubtitle(subtitle: String?)

        //Node Connection
        fun getFavouriteNodes(): MutableSet<NodeInfo>
        fun getOrPopulateFavourites(): MutableSet<NodeInfo>
        fun getNode(): NodeInfo?
        fun setNode(node: NodeInfo?)

        //fun showNet()
        fun onNodePrefs()

        //fun hiddenRescan(status:Boolean)

        fun callFinishActivity()
    }

    // called from activity
    // if account index has changed scroll to top?
    private var accountIndex = 0

    fun onRefreshed(wallet: Wallet, full: Boolean) {
        var full = full
        if (adapter!!.needsTransactionUpdateOnNewBlock()) {
            wallet.refreshHistory()
            full = true
            Log.d("TransactionList","full = true 1")
        }
        if (full) {
            Log.d("TransactionList","full = true 2")
            val list: MutableList<TransactionInfo> = ArrayList()
            val streetHeight: Long = activityCallback!!.streetModeHeight
            wallet.refreshHistory()
            for (info in wallet.history.all) {
                //Log.d("TxHeight=%d, Label=%s", info.blockheight.toString(), info.subaddressLabel)
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
        }
        updateStatus(wallet)
    }

    var isExchanging = false

    private fun showExchanging() {
        isExchanging = true
        binding.tvBalance.visibility = View.GONE
        //Important
        //flExchange.setVisibility(View.VISIBLE)
        //sCurrency.setEnabled(false)
    }

    private fun hideExchanging() {
        isExchanging = false
        binding.tvBalance.visibility = View.VISIBLE
        //Important
        //flExchange.setVisibility(View.GONE)
        //sCurrency.setEnabled(true)
    }

    fun exchangeFailed() {
        //Important
        //sCurrency.setSelection(0, true) // default to BDX
        val amountBdx: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        val amountFullBdx: Double = Helper.getDecimalAmount(balance).toDouble()
        showBalance(Helper.getFormattedAmount(amountBdx, true),walletSynchronized,Helper.getFormattedAmount(amountFullBdx, true))
        hideExchanging()
    }

    fun exchange(exchangeRate: ExchangeRate) {
        hideExchanging()
        if (!Helper.BASE_CRYPTO.equals(exchangeRate.baseCurrency)) {
            Timber.e("Not BDX")
            //Important
            //sCurrency.setSelection(0, true)
            balanceCurrency = Helper.BASE_CRYPTO
            balanceRate = 1.0
        } else {
            //Important
            /*val spinnerPosition =(sCurrency.getAdapter() as ArrayAdapter<*>).getPosition(exchangeRate!!.quoteCurrency)
            if (spinnerPosition < 0) { // requested currency not in list
                Timber.e("Requested currency not in list %s", exchangeRate.quoteCurrency)
                sCurrency.setSelection(0, true)
            } else {
                sCurrency.setSelection(spinnerPosition, true)
            }*/
            balanceCurrency = exchangeRate.quoteCurrency
            balanceRate = exchangeRate.rate
        }
        updateBalance()
    }

    // Callbacks from TransactionInfoAdapter
    override fun onInteraction(view: View?, infoItem: TransactionInfo?) {
        val exitTransition = MaterialElevationScale(false)
        exitTransition.duration =
            resources.getInteger(R.integer.tx_item_transition_duration).toLong()
        setExitTransition(exitTransition)
        val reenterTransition = MaterialElevationScale(true)
        reenterTransition.duration =
            resources.getInteger(R.integer.tx_item_transition_duration).toLong()
        setReenterTransition(reenterTransition)
        activityCallback!!.onTxDetailsRequest(view, infoItem)
    }

    private fun setStreetModeBackground(enable: Boolean) {
        //Important
        /*if (enable) {
            if (streetGunther == null) streetGunther =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_gunther_streetmode)
            ivStreetGunther.setImageDrawable(streetGunther)
        } else ivStreetGunther.setImageDrawable(null)*/
    }



}
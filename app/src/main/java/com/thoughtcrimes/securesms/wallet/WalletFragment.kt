package com.thoughtcrimes.securesms.wallet

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener
import com.google.android.material.transition.MaterialElevationScale
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.NetworkType
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeApi
import com.thoughtcrimes.securesms.wallet.service.exchange.ExchangeRate
import com.thoughtcrimes.securesms.wallet.utils.helper.ServiceHelper
import com.thoughtcrimes.securesms.wallet.widget.Toolbar
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentWalletBinding
import timber.log.Timber
import java.text.NumberFormat
import java.util.ArrayList

class WalletFragment : Fragment(), TransactionInfoAdapter.OnInteractionListener {

    private var adapter: TransactionInfoAdapter? = null
    private val formatter = NumberFormat.getInstance()

    private var syncText: String? = null

    fun setProgress(text: String?) {
        syncText = text
        binding.syncStatus.text = text
    }

    private var syncProgress = -1

    fun setProgress(n: Int) {
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
            if (binding.walletName.text === "") {
                binding.walletName.text = "--"
            }
            binding.progressBar.visibility = View.VISIBLE
        } else { // <0
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
            binding.receiveCardViewButton.visibility = View.VISIBLE
            binding.receiveCardViewButton.isEnabled = true
        }
    }

    fun onSynced() {
        if (!activityCallback?.isWatchOnly!!) {
            binding.sendCardViewButton.visibility = View.VISIBLE
            binding.sendCardViewButton.isEnabled = true
        }
        //if (isVisible) enableAccountsList(true) //otherwise it is enabled in onResume()
    }

    private var walletTitle: String? = null

    override fun onResume() {
        super.onResume()
        exitTransition = null
        reenterTransition = null
        Timber.d("onResume()")
        activityCallback!!.setTitle(walletTitle, "")
        activityCallback!!.setToolbarButton(Toolbar.BUTTON_NONE)
        binding.walletName.text = walletTitle
        //Important
        //tvWalletAccountStatus.setText(walletSubtitle)
        setProgress(syncProgress)
        setProgress(syncText)
        showReceive()
        //if (activityCallback!!.isSynced) enableAccountsList(true)
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
        if (activityCallback!!.hasWallet())
            inflater.inflate(R.menu.wallet_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }


    private lateinit var binding: FragmentWalletBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)

        binding.walletName.text = walletTitle
        //Important
        //tvWalletAccountStatus.setText(walletSubtitle)
        showBalance(Helper.getDisplayAmount(0))
        showUnconfirmed(0.0)

        adapter = TransactionInfoAdapter(activity, this)
        binding.transactionList.adapter = adapter
        adapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0 && binding.transactionList.computeVerticalScrollOffset() == 0) binding.transactionList.scrollToPosition(
                    positionStart
                )
            }
        })

        //      int count =  adapter.getItemCount();
//      Timber.d ("Adapter count %s", adapter.getItemCount());
//        anchorBehavior.setHideable(count == 0);
        binding.transactionList.addOnItemTouchListener(
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
        )

        binding.sendCardViewButton.setOnClickListener(View.OnClickListener { v: View? ->
            activityCallback!!.onSendRequest(v)
        })
        binding.receiveCardViewButton.setOnClickListener(View.OnClickListener { v: View? ->
            activityCallback!!.onWalletReceive(v)
        })

        if (activityCallback!!.isSynced) {
            onSynced()
        }

        activityCallback!!.forceUpdate()
        return binding.root
    }


   /* fun onViewCreated(@NonNull view: View?, @Nullable savedInstanceState: Bundle?) {
        binding.notesRecyclerView.setVisibility(View.VISIBLE) //this hide/show recyclerview visibility
        Log.d("TAG", "hidden: ")
    }*/

    private fun setActivityTitle(wallet: Wallet?) {
        if (wallet == null) return
        walletTitle = wallet.name
        //Important
        //walletSubtitle = wallet.accountLabel
        binding.walletName.text = walletTitle
        binding.transactionTitle.visibility = View.VISIBLE
        binding.transactionLayoutCardView.visibility = View.VISIBLE
        //Important
        //tvWalletAccountStatus.setText(walletSubtitle)
        activityCallback!!.setTitle(walletTitle, "")
        Timber.d("wallet title is %s", walletTitle)
    }

    private var firstBlock: Long = 0
    private var unlockedBalance: Long = 0
    private var balance: Long = 0
    private var accountIdx = -1

    private fun updateStatus(wallet: Wallet) {
        if (!isAdded) return
        Timber.d("updateStatus()")
        if (walletTitle == null || accountIdx != wallet.accountIndex) {
            accountIdx = wallet.accountIndex
            setActivityTitle(wallet)
        }
        balance = wallet.balance
        unlockedBalance = wallet.unlockedBalance
        refreshBalance()
        val sync: String
        check(activityCallback!!.hasBoundService()) { "WalletService not bound." }
        val daemonConnected: Wallet.ConnectionStatus = activityCallback!!.connectionStatus!!
        if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
            if (!wallet.isSynchronized) {
                val daemonHeight: Long = activityCallback!!.daemonHeight
                val walletHeight = wallet.blockChainHeight
                val n = daemonHeight - walletHeight
                sync =
                    getString(R.string.status_syncing) + " " + formatter.format(n) + " " + getString(
                        R.string.status_remaining
                    )
                if (firstBlock == 0L) {
                    firstBlock = walletHeight
                }
                var x = (100 - Math.round(100f * n / (1f * daemonHeight - firstBlock))).toInt()
                if (x == 0) x = 101 // indeterminate
                setProgress(x)
//                ivSynced.setVisibility(View.GONE);
            } else {
                sync = getString(R.string.status_synced) + " " + formatter.format(wallet.blockChainHeight)
                //binding.syncStatus.setTextColor(resources.getColor(R.color.green_color))
                binding.syncStatus.setTextColor(ContextCompat.getColor(requireActivity().applicationContext,R.color.green_color))
//                ivSynced.setVisibility(View.VISIBLE);
            }
        } else {
            sync = getString(R.string.status_wallet_connecting)
            setProgress(101)
            binding.transactionTitle.visibility = View.INVISIBLE
            binding.transactionLayoutCardView.visibility = View.GONE
            //anchorBehavior.setHideable(true)
        }
        setProgress(sync)
        // TODO show connected status somewhere
    }

    var balanceCurrency = Helper.BASE_CRYPTO
    var balanceRate = 1.0

    private val exchangeApi: ExchangeApi = ServiceHelper.getExchangeApi()

    private fun refreshBalance() {
        val unconfirmedBdx: Double = Helper.getDecimalAmount(balance - unlockedBalance).toDouble()
        showUnconfirmed(unconfirmedBdx)

        val amountBdx: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        showBalance(Helper.getFormattedAmount(amountBdx, true))
    }

    //Important
   /* private fun refreshBalance() {
        val unconfirmedBdx: Double = Helper.getDecimalAmount(balance - unlockedBalance).toDouble()
        showUnconfirmed(unconfirmedBdx)
        if (sCurrency.getSelectedItemPosition() == 0) { // XMR
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
        // at this point selection is XMR in case of error
        val displayB: String
        val amountA: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        displayB = if (!Helper.BASE_CRYPTO.equals(balanceCurrency)) { // not XMR
            val amountB: Double = amountA * balanceRate
            Helper.getFormattedAmount(amountB, false)
        } else { // XMR
            Helper.getFormattedAmount(amountA, true)
        }
        showBalance(displayB)
    }

    private fun showBalance(balance: String?) {
        binding.tvBalance.text = balance
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
        fun forceUpdate()
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
        fun setTitle(title: String?, subtitle: String?)
        fun setSubtitle(subtitle: String?)

        //Node Connection
        fun getFavouriteNodes(): MutableSet<NodeInfo>
        fun getOrPopulateFavourites(): MutableSet<NodeInfo>
        fun getNode(): NodeInfo?
        fun setNode(node: NodeInfo?)
        fun showNet()
        fun onNodePrefs()
    }

    // called from activity
    // if account index has changed scroll to top?
    private var accountIndex = 0

    fun onRefreshed(wallet: Wallet, full: Boolean) {
        var full = full
        Timber.d("onRefreshed(%b)", full)
        if (adapter!!.needsTransactionUpdateOnNewBlock()) {
            wallet.refreshHistory()
            full = true
        }
        if (full) {
            val list: MutableList<TransactionInfo> = ArrayList()
            val streetHeight: Long = activityCallback!!.streetModeHeight
            Timber.d("StreetHeight=%d", streetHeight)
            wallet.refreshHistory()
            for (info in wallet.history.all) {
                Timber.d("TxHeight=%d, Label=%s", info.blockheight, info.subaddressLabel)
                if ((info.isPending || info.blockheight >= streetHeight)
                    && !dismissedTransactions.contains(info.hash)
                ) list.add(info)
            }
            adapter!!.setInfos(list)
            if (accountIndex != wallet.accountIndex) {
                accountIndex = wallet.accountIndex
                binding.transactionList.scrollToPosition(0)
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
        showBalance(Helper.getFormattedAmount(amountBdx, true))
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
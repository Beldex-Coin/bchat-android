package com.thoughtcrimes.securesms.wallet.send

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.transition.MaterialContainerTransform
import com.thoughtcrimes.securesms.data.*
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.wallet.WalletActivity
import com.thoughtcrimes.securesms.wallet.listener.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.send.interfaces.SendConfirm
import com.thoughtcrimes.securesms.wallet.utils.ThemeHelper
import com.thoughtcrimes.securesms.wallet.widget.Toolbar
import io.beldex.bchat.R
import timber.log.Timber
import java.lang.IllegalArgumentException

class SendFragment : Fragment() {

    val MIXIN = 0

    private val activityCallback: Listener? = null

    interface Listener {
        val prefs: SharedPreferences?
        val totalFunds: Long
        val isStreetMode: Boolean

        fun onPrepareSend(tag: String?, data: TxData?)
        val walletName: String?

        fun onSend(notes: UserNotes?)
        fun onDisposeRequest()
        fun onFragmentDone()
        fun setToolbarButton(type: Int)
        fun setTitle(title: String?)
        fun setSubtitle(subtitle: String?)
        fun setOnUriScannedListener(onUriScannedListener: OnUriScannedListener?)
    }

    var sendConfirmListener: SendConfirmListener? = null

    interface SendConfirmListener {
        fun getActivityCallback(): Listener?
        val txData: TxData?

        fun commitTransaction()
        fun disposeTransaction()
        //val mode: Mode?
    }

    var sendAddressListener: SendAddressListener? = null

    interface SendAddressListener {
        var barcodeData: BarcodeData?

        fun popBarcodeData(): BarcodeData?
        //Important
        //fun setMode(mode: SendFragment.Mode?)
        val txData: TxData?
    }

    var onScanListener: OnScanListener? = null

    interface OnScanListener {
        fun onScan()
    }

    fun onCreateTransactionFailed(errorText: String?) {
        val confirm: SendConfirm? = getSendConfirm()
        if (confirm != null) {
            confirm.createTransactionFailed(errorText)
        }
    }

    fun getSendConfirm(): SendConfirm? {
        //Important
        /*val fragment: SendWizardFragment = pagerAdapter.getFragment(SendFragment.SpendPagerAdapter.POS_CONFIRM)
        return if (fragment is SendConfirm) {
            fragment!!
        } else {
            null
        }*/
        return null
    }

    var pendingTx: PendingTx? = null

    // callbacks from send service
    fun onTransactionCreated(txTag: String?, pendingTransaction: PendingTransaction?) {
        val confirm = getSendConfirm()
        if (confirm != null) {
            pendingTx = PendingTx(pendingTransaction)
            confirm.transactionCreated(txTag, pendingTransaction)
        } else {
            // not in confirm fragment => dispose & move on
            disposeTransaction()
        }
    }

    fun disposeTransaction() {
        pendingTx = null
        activityCallback!!.onDisposeRequest()
    }

    fun onTransactionSent(txId: String?) {
        //Important
        Timber.d("txid=%s", txId)
        //pagerAdapter.addSuccess()
        //Log.d("numPages=%d", spendViewPager.getAdapter().getCount())
        activityCallback!!.setToolbarButton(Toolbar.BUTTON_NONE)
        //spendViewPager.setCurrentItem(SendFragment.SpendPagerAdapter.POS_SUCCESS)
    }

    var committedTx: PendingTx? = null

    @SuppressLint("StringFormatMatches")
    fun onSendTransactionFailed(error: String?) {
        Timber.d("error=%s", error)
        committedTx = null
        val confirm = getSendConfirm()
        confirm?.sendFailed(getString(R.string.status_transaction_failed, error))
        enableNavigation()
    }

    fun enableNavigation() {
        //Important
        //spendViewPager.allowSwipe(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transform = MaterialContainerTransform()
        transform.drawingViewId = R.id.fragment_container
        transform.duration = resources.getInteger(R.integer.tx_item_transition_duration).toLong()
        transform.setAllContainerColors(
            ThemeHelper.getThemedColor(
                context,
                android.R.attr.colorBackground
            )
        )
        sharedElementEnterTransition = transform

    }

    companion object {
        @JvmStatic
        fun newInstance(uri: String?): SendFragment {
            val f = SendFragment()
            val args = Bundle()
            args.putString(WalletActivity.REQUEST_URI, uri)
            f.arguments = args
            return f
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }


    fun getTxData(): TxData? {
        return txData
    }

    private var txData = TxData()
    enum class Mode {
        XMR, BTC
    }

    private var mode:Mode = Mode.XMR

    fun setMode(aMode: Mode) {
        if (mode != aMode) {
            mode = aMode
            when (aMode) {
                Mode.XMR -> txData = TxData()
                Mode.BTC -> txData = TxDataBtc()
                else -> throw IllegalArgumentException("Mode " + aMode.toString() + " unknown!")
            }
            //Important
            //view!!.post { pagerAdapter.notifyDataSetChanged() }
            Timber.d("New Mode = %s", mode.toString())
        }
    }

    fun getMode(): Mode{
        return mode
    }
}
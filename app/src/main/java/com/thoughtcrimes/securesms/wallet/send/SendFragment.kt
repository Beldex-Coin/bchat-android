package com.thoughtcrimes.securesms.wallet.send

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.transition.MaterialContainerTransform
import com.thoughtcrimes.securesms.data.*
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.wallet.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.WalletActivity
import com.thoughtcrimes.securesms.wallet.send.interfaces.SendConfirm
import com.thoughtcrimes.securesms.wallet.utils.ThemeHelper
import com.thoughtcrimes.securesms.wallet.widget.Toolbar
import io.beldex.bchat.R
import timber.log.Timber
import com.thoughtcrimes.securesms.wallet.addressbook.AddressBookActivity

import android.content.Intent
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.wallet.utils.helper.ServiceHelper
import io.beldex.bchat.databinding.FragmentSendBinding
import java.lang.ClassCastException


class SendFragment : Fragment(), OnUriScannedListener,SendConfirm {

    val MIXIN = 0

    private var activityCallback: Listener? = null
    private var barcodeData: BarcodeData? = null
    lateinit var binding: FragmentSendBinding

    private var isResume:Boolean = false


    fun newInstance(listener: Listener): SendFragment? {
        val instance: SendFragment = SendFragment()
        instance.setSendListener(listener)
        return instance
    }

    private fun setSendListener(listener: Listener) {
        this.activityCallback = listener
    }

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
        fun setBarcodeData(data: BarcodeData?)

        fun getBarcodeData(): BarcodeData?

        fun popBarcodeData(): BarcodeData?

        fun setMode(mode: WalletActivity.Mode?)

        fun getTxData(): TxData?
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
        //Important
        /*val confirm: SendConfirm? = getSendConfirm()
        if (confirm != null) {
            confirm.createTransactionFailed(errorText)
        }*/
        createTransactionFailed(errorText)
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
        //Important
       /* val confirm = getSendConfirm()
        if (confirm != null) {
            pendingTx = PendingTx(pendingTransaction)
            confirm.transactionCreated(txTag, pendingTransaction)
        } else {
            // not in confirm fragment => dispose & move on
            disposeTransaction()
        }*/
        Log.d("onTransactionCreated Status_Ok","--")
        pendingTx = PendingTx(pendingTransaction)
        Log.d("onTransactionCreated Status_Ok","---")
        transactionCreated(txTag, pendingTransaction)
    }

    fun disposeTransaction() {
        pendingTx = null
        activityCallback!!.onDisposeRequest()
    }

    //If Transaction successfully completed after call this function
    fun onTransactionSent(txId: String?) {
        hideProgress()
        //Important
        Timber.d("txid=%s", txId)
        //pagerAdapter.addSuccess()
        //Log.d("numPages=%d", spendViewPager.getAdapter().getCount())
        activityCallback!!.setToolbarButton(Toolbar.BUTTON_BACK)
        Log.d("Beldex","Transaction Completed")
        val builder = AlertDialog.Builder(
            requireContext(), R.style.BChatAlertDialog
        )
        builder.setTitle(requireContext().getString(R.string.transaction_completed))
        builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, _: Int ->
            dialog!!.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
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
        binding = FragmentSendBinding.inflate(inflater, container, false)


        binding.scanQrCode.setOnClickListener {
            onScanListener?.onScan()
        }
        binding.addressBook.setOnClickListener {
            val intent = Intent(context, AddressBookActivity::class.java)
            startActivity(intent)
        }

        binding.sendButton.setOnClickListener {
            val txData: TxData = getTxData()
           /* if (txData is TxDataBtc) {
                txData.setBtcAddress(etAddress.getEditText().getText().toString())
                txData.setBtcSymbol(selectedCrypto.getSymbol())
                txData.setDestinationAddress(null)
                ServiceHelper.ASSET = selectedCrypto.getSymbol().toLowerCase()
            } else {*/
                txData.destinationAddress = binding.beldexAddressEditTxtLayout.editText?.text.toString()
                ServiceHelper.ASSET = null
            //}

            /* String bdx = etAmount.getNativeAmount();
                Timber.d("BDX Total Amount -> "+Wallet.getAmountFromString(bdx));
                if (bdx != null) {
                    sendListener.getTxData().setAmount(Wallet.getAmountFromString(bdx));
                } else {
                    sendListener.getTxData().setAmount(0L);
                }*/
          /*  if (binding.beldexAmountEditTxt.getNativeAmount()
                .equals(Wallet.getDisplayAmount(activityCallback!!.totalFunds))
        ) {
            val amount = (activityCallback!!.totalFunds - 10485760)
            val bdx: String = etAmount.getNativeAmount()
            Timber.d(
                "If BDX Total Amount -> " + Wallet.getAmountFromString(bdx)
                    .toString() + " " + bdx + "" + amount
            )
            if (bdx != null) {
                txData.amount = amount
            } else {
                txData.setAmount(0L)
            }
        } else {
            val bdx: String = etAmount.getNativeAmount()
            Timber.d("Else BDX Total Amount -> " + Wallet.getAmountFromString(bdx).toString() + " " + bdx)
            if (bdx != null) {
                txData.amount = Wallet.getAmountFromString(bdx)
            } else {
                txData.amount = 1L
            }
        }*/
            txData.amount = 1L
            txData.userNotes = UserNotes("Test")//etNotes.getEditText().getText().toString()
            txData.priority = PendingTransaction.Priority.Priority_Default
            txData.mixin = MIXIN
            onResumeFragment()
        }
        return binding.root

    }

    var inProgress = false

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        inProgress = false
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
        inProgress = true
    }

    private fun onResumeFragment(){
        Timber.d("onResumeFragment()")
        Helper.hideKeyboard(activity)
        isResume = true

        val txData: TxData = getTxData()
        //tvTxAddress.setText(txData.destinationAddress)
        //val notes: UserNotes = getTxData().userNotes
        /*if (notes != null && notes.note.isNotEmpty()) {
            //tvTxNotes.setText(notes.note)
            //fragmentSendConfirmNotesLinearLayout.setVisibility(View.VISIBLE)
        } else {
            //fragmentSendConfirmNotesLinearLayout.setVisibility(View.GONE)
        }*/
        refreshTransactionDetails()
        if (pendingTransaction == null && !inProgress) {
            showProgress()
            prepareSend(txData)
        }
    }

    // creates a pending transaction and calls us back with transactionCreated()
    // or createTransactionFailed()
    private fun prepareSend(txData: TxData?) {
        activityCallback!!.onPrepareSend(null, txData)
    }




    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityCallback = context as Listener
        activityCallback!!.setOnUriScannedListener(this)
        onScanListener = if (context is OnScanListener) {
            context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement ScanListener"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        activityCallback!!.setToolbarButton(Toolbar.BUTTON_BACK)
        activityCallback!!.setTitle(getString(R.string.send))
        processScannedData()
    }

    // QR Scan Stuff
    fun processScannedData(barcodeData: BarcodeData?) {
        activityCallback?.setBarcodeData(barcodeData)
        if (isResume) processScannedData()
    }

    private fun processScannedData() {
        var barcodeData: BarcodeData? = activityCallback?.getBarcodeData()
        if (barcodeData != null) {
            Timber.d("GOT DATA")
            if (!Helper.ALLOW_SHIFT && barcodeData.asset !== Crypto.BDX) {
                Timber.d("BUT ONLY BDX SUPPORTED")
                barcodeData = null
                activityCallback?.setBarcodeData(barcodeData)
            }
            if (barcodeData!!.address != null) {
                binding.beldexAddressEditTxtLayout.editText?.setText(barcodeData.address)
                binding.beldexAmountEditTxtLayout.editText?.setText(barcodeData.amount)
                /* possibleCryptos.clear()*/
                /* selectedCrypto = null*/
                /*if (barcodeData.isAmbiguous) {
                    possibleCryptos.addAll(barcodeData.ambiguousAssets)
                } else {
                    possibleCryptos.add(barcodeData.asset)
                    selectedCrypto = barcodeData.asset
                }*/
                /*if (Helper.ALLOW_SHIFT) updateCryptoButtons(false)
                if (checkAddress()) {
                    if (barcodeData.security === BarcodeData.Security.OA_NO_DNSSEC) etAddress.setError(
                        getString(R.string.send_address_no_dnssec)
                    ) else if (barcodeData.security === BarcodeData.Security.OA_DNSSEC) etAddress.setError(
                        getString(R.string.send_address_openalias)
                    )
                }*/
            } else {
                binding.beldexAddressEditTxtLayout.editText?.text?.clear()
                binding.beldexAmountEditTxtLayout.editText?.text?.clear()
            }
            //by hales
            /*var scannedNotes = barcodeData.addressName
            if (scannedNotes == null) {
                scannedNotes = barcodeData.description
            } else if (barcodeData.description != null) {
                scannedNotes = scannedNotes + ": " + barcodeData.description
            }*/
            /*if (scannedNotes != null) {
                etNotes.getEditText().setText(scannedNotes)
            } else {
                etNotes.getEditText().getText().clear()
                etNotes.setError(null)
            }*/
        } else Timber.d("barcodeData=null")
    }

    private fun getTxData(): TxData {
        return txData
    }

    private var txData = TxData()

    enum class Mode {
        BDX, BTC
    }

    private var mode: Mode = Mode.BDX

    fun setMode(aMode: Mode) {
        if (mode != aMode) {
            mode = aMode
            when (aMode) {
                Mode.BDX -> txData = TxData()
                Mode.BTC -> txData = TxDataBtc()
                else -> throw IllegalArgumentException("Mode " + aMode.toString() + " unknown!")
            }
            //Important
            //view!!.post { pagerAdapter.notifyDataSetChanged() }
            Timber.d("New Mode = %s", mode.toString())
        }
    }

    fun getMode(): Mode {
        return mode
    }

    override fun onUriScanned(barcodeData: BarcodeData?): Boolean {
        processScannedData(barcodeData)
        return true
    }

    override fun sendFailed(errorText: String?) {
        binding.progressBar.visibility = View.INVISIBLE
        showAlert(getString(R.string.send_create_tx_error_title), errorText!!)
    }

    private fun showAlert(title: String, message: String) {
        //AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        val builder = AlertDialog.Builder(
            requireActivity(), R.style.backgroundColor
        )
        builder.setCancelable(true).setTitle(title).setMessage(message).create().show()
    }

    override fun createTransactionFailed(errorText: String?) {
        hideProgress()
        showAlert(getString(R.string.send_create_tx_error_title), errorText!!)
    }

    override fun transactionCreated(txTag: String?, pendingTransaction: PendingTransaction?) {
        // ignore txTag - the app flow ensures this is the correct tx
        Log.d("onTransactionCreated Status_Ok","----")
        hideProgress()
        if (isResume) {
            this.pendingTransaction = pendingTransaction
            refreshTransactionDetails()
        } else {
            this.disposeTransaction()
        }
    }

    var pendingTransaction: PendingTransaction? = null

    private fun refreshTransactionDetails() {
        Timber.d("refreshTransactionDetails()")
        if (pendingTransaction != null) {
            val txData: TxData? = getTxData()
            SendConfirmDialog(pendingTransaction!!,txData, this).show(requireActivity().supportFragmentManager,"")
           /* tvTxAddress.setText(txData.destinationAddress)
            llConfirmSend.setVisibility(View.VISIBLE)
            bSend.setEnabled(true)
            Timber.d("getFee() SendConfirmWizardFragment ->%s", pendingTransaction!!.getAmount())
            Timber.d("getFee() SendConfirmWizardFragment ->%s", Wallet.getDisplayAmount(pendingTransaction!!.getAmount()))
            Timber.d("getFee() SendConfirmWizardFragment ->%s", Wallet.getDisplayAmount(pendingTransaction!!.getFee()))
            Timber.d("getFee() FEE PendingSendConfirmWizardFragment ->%s", pendingTransaction!!.getFee())
            tvTxFee.setText(Wallet.getDisplayAmount(pendingTransaction!!.getFee()))
            if (getActivityCallback().isStreetMode()
                && sendListener.getTxData().getAmount() === Wallet.SWEEP_ALL
            ) {
                tvTxAmount.setText(getString(R.string.street_sweep_amount))
                tvTxTotal.setText(getString(R.string.street_sweep_amount))
            } else {
                tvTxAmount.setText(Wallet.getDisplayAmount(pendingTransaction!!.getAmount()))
                tvTxTotal.setText(
                    Wallet.getDisplayAmount(
                        pendingTransaction!!.getFee() + pendingTransaction!!.getAmount()
                    )
                )
            }*/
        } else {
            //llConfirmSend.setVisibility(View.GONE)
            //bSend.setEnabled(false)
        }
    }

    fun send() {
       /* SendConfirmDialog(pendingTransaction!!,getTxData(), this).dismiss()*/
        commitTransaction()
        requireActivity().runOnUiThread { binding.progressBar.visibility = View.VISIBLE }
    }

    private fun commitTransaction() {
        Timber.d("REALLY SEND")
        //disableNavigation() // committed - disable all navigation
        activityCallback!!.onSend(txData.userNotes)
        committedTx = pendingTx
    }
}
package com.thoughtcrimes.securesms.wallet.send

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialContainerTransform
import com.thoughtcrimes.securesms.data.*
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.wallet.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.WalletActivity
import com.thoughtcrimes.securesms.wallet.send.interfaces.SendConfirm
import com.thoughtcrimes.securesms.wallet.utils.ThemeHelper
import com.thoughtcrimes.securesms.wallet.widget.Toolbar
import io.beldex.bchat.R
import com.thoughtcrimes.securesms.wallet.addressbook.AddressBookActivity

import android.content.Intent
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.*
import android.view.View.OnFocusChangeListener
import android.widget.TextView.OnEditorActionListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.wallet.utils.OpenAliasHelper
import com.thoughtcrimes.securesms.wallet.utils.helper.ServiceHelper
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import io.beldex.bchat.databinding.FragmentSendBinding
import java.lang.ClassCastException
import java.lang.NumberFormatException
import java.util.*
import timber.log.Timber





class SendFragment : Fragment(), OnUriScannedListener,SendConfirm {

    val MIXIN = 0

    private var activityCallback: Listener? = null
    private var barcodeData: BarcodeData? = null
    lateinit var binding: FragmentSendBinding

    private var isResume:Boolean = false

    private val possibleCryptos: MutableSet<Crypto> = HashSet()
    private var selectedCrypto: Crypto? = null
    val INTEGRATED_ADDRESS_LENGTH = 106
    private var resolvingOA = false


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

        fun onBackPressedFun()
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

   /* fun getSendConfirm(): SendConfirm? {
        //Important
        *//*val fragment: SendWizardFragment = pagerAdapter.getFragment(SendFragment.SpendPagerAdapter.POS_CONFIRM)
        return if (fragment is SendConfirm) {
            fragment!!
        } else {
            null
        }*//*
        return null
    }*/
   private fun openSomeActivityForResult() {
       TextSecurePreferences.setSendAddressDisable(requireContext(),false)
       val intent = Intent(context, AddressBookActivity::class.java)
       resultLauncher.launch(intent)
   }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val add = data?.getStringExtra("address_value")
            Log.d("beldex","value of add $add")
            if(add != null)
            {
                binding.beldexAddressEditTxtLayout.editText!!.setText(add.toString())
            }
        }
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
        activityCallback!!.setToolbarButton(Toolbar.BUTTON_BACK)
        Log.d("Beldex","Transaction Completed")
        SendSuccessDialog(this).show(requireActivity().supportFragmentManager,"")
       /* val builder = AlertDialog.Builder(
            requireContext(), R.style.BChatAlertDialog
        )
        builder.setTitle(requireContext().getString(R.string.transaction_completed))
        builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, _: Int ->
            sendButtonEnabled()
            dialog!!.dismiss()
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()*/
    }

    var committedTx: PendingTx? = null

    @SuppressLint("StringFormatMatches")
    fun onSendTransactionFailed(error: String?) {
        Timber.d("error=%s", error)
        committedTx = null
        /*val confirm = getSendConfirm()
        confirm?.sendFailed(getString(R.string.status_transaction_failed, error))
        enableNavigation()*/
        sendFailed(getString(R.string.status_transaction_failed, error))
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

    private val resultLaunchers = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            /* // There are no request codes
             val data: Intent? = result.data*/
            onResumeFragment()
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
            openSomeActivityForResult()
        }

        //Validate beldex address
        binding.beldexAddressEditTxtLayout.editText?.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        binding.beldexAddressEditTxtLayout.editText?.setOnEditorActionListener(OnEditorActionListener { v, actionId, event -> // ignore ENTER
                event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            })
        binding.beldexAddressEditTxtLayout.editText?.onFocusChangeListener =
            OnFocusChangeListener { v: View?, hasFocus: Boolean ->
                if (!hasFocus) {
                    val enteredAddress: String = binding.beldexAddressEditTxtLayout.editText?.text.toString().trim()
                    val dnsOA = dnsFromOpenAlias(enteredAddress)
                    Timber.d("OpenAlias is %s", dnsOA)
                    if (dnsOA != null) {
                        processOpenAlias(dnsOA)
                    }
                }
            }
        binding.beldexAddressEditTxtLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                Timber.d("AFTER: %s", editable.toString())
                binding.beldexAddressEditTxtLayout.error = null
                possibleCryptos.clear()
                selectedCrypto = null
                val address: String = binding.beldexAddressEditTxtLayout.editText?.text.toString()
                if (isIntegratedAddress(address)) {
                    Timber.d("isIntegratedAddress")
                    possibleCryptos.add(Crypto.BDX)
                    selectedCrypto = Crypto.BDX
                    binding.beldexAddressEditTxtLayout.error = getString(R.string.info_paymentid_integrated)
                    setMode(Mode.BDX)
                } else if (isStandardAddress(address)) {
                    Timber.d("isStandardAddress")
                    possibleCryptos.add(Crypto.BDX)
                    selectedCrypto = Crypto.BDX
                    setMode(Mode.BDX)
                }
               /* else{
                    binding.beldexAddressEditTxtLayout.error = getString(R.string.send_address_invalid)
                }*/
                if (possibleCryptos.isEmpty()) {
                    Timber.d("other")
                    setMode(Mode.BDX)
                }
                /*if (!Helper.ALLOW_SHIFT) return
                if (possibleCryptos.isEmpty()) {
                    Timber.d("isBitcoinAddress")
                    for (type in BitcoinAddressType.values()) {
                        if (BitcoinAddressValidator.validate(address, type)) {
                            possibleCryptos.add(Crypto.valueOf(type.name()))
                        }
                    }
                    if (!possibleCryptos.isEmpty()) // found something in need of shifting!
                        sendListener.setMode(Mode.BTC)
                    if (possibleCryptos.size == 1) {
                        selectedCrypto = possibleCryptos.toTypedArray().get(0) as Crypto
                    }
                }
                if (possibleCryptos.isEmpty()) {
                    Timber.d("other")
                    tvXmrTo.setVisibility(View.INVISIBLE)
                    setMode(Mode.BDX)
                }
                updateCryptoButtons(address.isEmpty())*/
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        binding.beldexAmountEditTxtLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                binding.beldexAmountEditTxtLayout.error = null

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        binding.sendButton.setOnClickListener {
            if (!checkAddressNoError()) {
                shakeAddress()
                val enteredAddress: String = binding.beldexAddressEditTxtLayout.editText?.text.toString().trim()
                val dnsOA = dnsFromOpenAlias(enteredAddress)
                if (dnsOA != null) {
                    Log.d("OpenAlias is %s", dnsOA)
                }
                dnsOA?.let { processOpenAlias(it) }
            }else {
                if (binding.beldexAddressEditTxtLayout.editText?.text!!.isNotEmpty() && binding.beldexAmountEditTxtLayout.editText?.text!!.isNotEmpty()) {
                    val txData: TxData = getTxData()
                    txData.destinationAddress =
                        binding.beldexAddressEditTxtLayout.editText?.text.toString()
                    ServiceHelper.ASSET = null

                    if (getCleanAmountString(binding.beldexAmountEditTxtLayout.editText?.text.toString()).equals(
                            Wallet.getDisplayAmount(activityCallback!!.totalFunds)
                        )
                    ) {
                        val amount =
                            (activityCallback!!.totalFunds - 10485760)// 10485760 == 050000000
                        val bdx = getCleanAmountString(binding.beldexAmountEditTxtLayout.editText?.text.toString())
                        Log.d("If BDX Total Amount -> " + Wallet.getAmountFromString(bdx).toString() + " " + bdx + "" + amount,"true")
                        if (bdx != null) {
                            txData.amount = amount
                        } else {
                            txData.amount = 0L
                        }
                    } else {
                        val bdx = getCleanAmountString(binding.beldexAmountEditTxtLayout.editText?.text.toString())
                        Log.d("Else BDX Total Amount -> " + Wallet.getAmountFromString(bdx).toString() + " " + bdx,"true")
                        if (bdx != null) {
                            txData.amount = Wallet.getAmountFromString(bdx)
                        } else {
                            txData.amount = 0L
                        }
                    }
                    txData.userNotes = UserNotes("-")//etNotes.getEditText().getText().toString()
                    txData.priority = PendingTransaction.Priority.Priority_Flash
                    txData.mixin = MIXIN
                    binding.beldexAddressEditTxtLayout.editText?.text?.clear()
                    binding.beldexAmountEditTxtLayout.editText?.text?.clear()

                    val lockManager: LockManager<CustomPinActivity> = LockManager.getInstance() as LockManager<CustomPinActivity>
                    lockManager.enableAppLock(requireActivity(), CustomPinActivity::class.java)
                    val intent = Intent(requireActivity(), CustomPinActivity::class.java)
                        intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN)
                        intent.putExtra("change_pin",false)
                        intent.putExtra("send_authentication",true)
                    resultLaunchers.launch(intent)
                } else if (binding.beldexAddressEditTxtLayout.editText?.text!!.isEmpty()) {
                    Log.d("Beldex","beldexAddressEditTxtLayout isEmpty()")
                    binding.beldexAddressEditTxtLayout.error = getString(R.string.beldex_address_error_message)
                } else {
                    Log.d("Beldex","beldexAmountEditTxtLayout isEmpty()")
                    binding.beldexAmountEditTxtLayout.error = getString(R.string.beldex_amount_error_message)
                }
            }
        }
        if(TextSecurePreferences.getFeePriority(requireActivity())==0){
            binding.estimatedFeeTextView.text = getString(R.string.estimated_fee,calculateEstimatedFee(1).toString())
            binding.estimatedFeeDescriptionTextView.text =getString(R.string.estimated_fee_description,"Slow")
        }else{
            binding.estimatedFeeTextView.text = getString(R.string.estimated_fee,calculateEstimatedFee(5).toString())
            binding.estimatedFeeDescriptionTextView.text =getString(R.string.estimated_fee_description,"Flash")
        }

        return binding.root

    }

    private fun calculateEstimatedFee(priority:Int): Double {
        val wallet: Wallet = WalletManager.getInstance().wallet
        return wallet.estimateTransactionFee(priority)
    }

    private val CLEAN_FORMAT = "%." + Helper.BDX_DECIMALS.toString() + "f"

    private fun getCleanAmountString(enteredAmount: String): String? {
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

    private fun checkAddressNoError(): Boolean {
        return selectedCrypto != null
    }

    private fun checkAddress(): Boolean {
        val ok = checkAddressNoError()
        if (possibleCryptos.isEmpty()) {
            binding.beldexAddressEditTxtLayout.error = getString(R.string.send_address_invalid)
        } else {
            binding.beldexAddressEditTxtLayout.error = null
        }
        return ok
    }

    private fun isStandardAddress(address: String): Boolean {
        return Wallet.isAddressValid(address)
    }

    private fun isIntegratedAddress(address: String): Boolean {
        return (address.length == INTEGRATED_ADDRESS_LENGTH
                && Wallet.isAddressValid(address))
    }

    private fun shakeAddress() {
        //if(possibleCryptos.size==1)
            binding.beldexAddressEditTxtLayout.startAnimation(Helper.getShakeAnimation(context))
            binding.beldexAddressEditTxtLayout.error = getString(R.string.send_address_invalid)
    }

    private fun processOpenAlias(dnsOA: String?) {
        if (resolvingOA) return  // already resolving - just wait
        activityCallback!!.popBarcodeData()
        if (dnsOA != null) {
            resolvingOA = true
            binding.beldexAddressEditTxtLayout.error = getString(R.string.send_address_resolve_openalias)
            OpenAliasHelper.resolve(dnsOA, object : OpenAliasHelper.OnResolvedListener {
                override fun onResolved(dataMap: Map<Crypto?, BarcodeData?>) {
                    resolvingOA = false
                    var barcodeData = dataMap[Crypto.BDX]
                    if (barcodeData == null) barcodeData = dataMap[Crypto.BTC]
                    if (barcodeData != null) {
                        Timber.d("Security=%s, %s", barcodeData.security.toString(), barcodeData.address)
                        processScannedData(barcodeData)
                    } else {
                        binding.beldexAddressEditTxtLayout.error = getString(R.string.send_address_not_openalias)
                        Timber.d("NO BDX OPENALIAS TXT FOUND")
                    }
                }

                override fun onFailure() {
                    resolvingOA = false
                    binding.beldexAddressEditTxtLayout.error = getString(R.string.send_address_not_openalias)
                    Timber.e("OA FAILED")
                }
            })
        } // else ignore
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

        //val txData: TxData = getTxData()
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
            binding.sendButton.isEnabled=false
            binding.sendButton.isClickable=false
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

      /*  if(txData.priority.value != null) {
            if (txData.priority.value == 1) {
                binding.estimatedFeeDescriptionTextView.text =
                    "Slow priority is set as the default fee.\\nGo to setting to change the transaction priority."
            } else {
                binding.estimatedFeeDescriptionTextView.text =
                    "Flash priority is set as the default fee.\\nGo to setting to change the transaction priority."
            }
        }*/
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

                //---------------------------------------------------------------------------
                possibleCryptos.clear()
                selectedCrypto = null
                if (barcodeData.isAmbiguous) {
                    possibleCryptos.addAll(barcodeData.ambiguousAssets)
                } else {
                    possibleCryptos.add(barcodeData.asset)
                    selectedCrypto = barcodeData.asset
                }
                //if (Helper.ALLOW_SHIFT) updateCryptoButtons(false)
                if (checkAddress()) {
                    if (barcodeData.security === BarcodeData.Security.OA_NO_DNSSEC) binding.beldexAddressEditTxtLayout.error =
                        getString(R.string.send_address_no_dnssec) else if (barcodeData.security === BarcodeData.Security.OA_DNSSEC) binding.beldexAddressEditTxtLayout.error =
                        getString(R.string.send_address_openalias)
                }
                //-------------------------------------------------------------------------//
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
        sendButtonEnabled()
        showAlert(getString(R.string.send_create_tx_error_title), errorText!!)
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(
            requireActivity(), R.style.backgroundColor
        )
        builder.setCancelable(true).setTitle(title).setMessage(message).create().show()
    }

    override fun createTransactionFailed(errorText: String?) {
        hideProgress()
        sendButtonEnabled()
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
            val txData: TxData = getTxData()
            SendConfirmDialog(pendingTransaction!!,txData, this).show(requireActivity().supportFragmentManager,"")
        }
    }

    fun sendButtonEnabled(){
        binding.sendButton.isEnabled=true
        binding.sendButton.isClickable=true
    }

    fun send() {
        commitTransaction()
        requireActivity().runOnUiThread { binding.progressBar.visibility = View.VISIBLE }
    }

    fun transactionFinished(){
        sendButtonEnabled()
        activityCallback!!.onBackPressedFun()
    }

    private fun commitTransaction() {
        Timber.d("REALLY SEND")
        //disableNavigation() // committed - disable all navigation
        activityCallback!!.onSend(txData.userNotes)
        committedTx = pendingTx
    }

    private fun dnsFromOpenAlias(openalias: String): String? {
        var openalias = openalias
        Timber.d("checking openalias candidate %s", openalias)
        if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias
        if (Patterns.EMAIL_ADDRESS.matcher(openalias).matches()) {
            openalias = openalias.replaceFirst("@".toRegex(), ".")
            if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias
        }
        return null // not an openalias
    }
}
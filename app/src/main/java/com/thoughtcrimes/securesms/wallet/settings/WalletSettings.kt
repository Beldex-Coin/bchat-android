package com.thoughtcrimes.securesms.wallet.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.addressbook.AddressBookActivity
import com.thoughtcrimes.securesms.wallet.node.activity.NodeActivity
import com.thoughtcrimes.securesms.wallet.settings.adapter.WalletSubOptionsListAdapter
import com.thoughtcrimes.securesms.wallet.settings.adapter.WalletSubOptionsSearchListItemAdapter
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityWalletSettingsBinding


class WalletSettings : BaseActionBarActivity(),WalletSubOptionsListAdapter.ItemClickListener,WalletSubOptionsSearchListItemAdapter.ItemClickListener {
    lateinit var binding:ActivityWalletSettingsBinding
    lateinit var dialog: Dialog
    lateinit var walletSubOptionsListAdapter:WalletSubOptionsListAdapter
    private val displayBalanceAsList: ArrayList<String> = ArrayList()
    private val feePriorityList: ArrayList<String> = ArrayList()
    private val decimalsList: ArrayList<String> = ArrayList()
    private val currencyList:ArrayList<String> = ArrayList()
    lateinit var walletSubOptionsSearchListItemAdapter:WalletSubOptionsSearchListItemAdapter
    private var selectedDecimalIndex:Int = 0
    private var selectedCurrencyIndex:Int = 30
    var nodeItem: NodeInfo? = null
    private val SELECTED_NODE_PREFS_NAME = "selected_node"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Wallet Settings",false)
        binding = ActivityWalletSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // data to populate the RecyclerView with
        displayBalanceAsList.add("Beldex Full Balance")
        displayBalanceAsList.add("Beldex Available Balance")
        displayBalanceAsList.add("Beldex Hidden")

        // data to populate the RecyclerView with
        feePriorityList.add("Slow")
        feePriorityList.add("Flash")

        // data to populate the RecyclerView with
        decimalsList.add("4 - Four (0.0000)")
        decimalsList.add("3 - Three (0.000)")
        decimalsList.add("2 - Two (0.00)")
        decimalsList.add("0 - Zero (000)")

        currencyList.add("AUD")
        currencyList.add("BRL")
        currencyList.add("CAD")
        currencyList.add("CHF")
        currencyList.add("CNY")
        currencyList.add("CZK")
        currencyList.add("EUR")
        currencyList.add("DKK")
        currencyList.add("GBP")
        currencyList.add("HKD")
        currencyList.add("HUF")
        currencyList.add("IDR")
        currencyList.add("ILS")
        currencyList.add("INR")
        currencyList.add("JPY")
        currencyList.add("KRW")
        currencyList.add("MXN")
        currencyList.add("MYR")
        currencyList.add("NOK")
        currencyList.add("NZD")
        currencyList.add("PHP")
        currencyList.add("PLN")
        currencyList.add("RUB")
        currencyList.add("SEK")
        currencyList.add("SGD")
        currencyList.add("THB")
        currencyList.add("USD")
        currencyList.add("VEF")
        currencyList.add("ZAR")

        with(binding){
            displayBalanceAsDescription.text = displayBalanceAsList[TextSecurePreferences.getDisplayBalanceAs(this@WalletSettings)]
            feePriorityDescription.text=feePriorityList[TextSecurePreferences.getFeePriority(this@WalletSettings)]
            decimalsDescription.text=TextSecurePreferences.getDecimals(this@WalletSettings).toString()
            currencyDescription.text=TextSecurePreferences.getCurrency(this@WalletSettings).toString()
            for(i in 0 until decimalsList.size){
                if(decimalsList[i]==decimalsDescription.text.toString()){
                    selectedDecimalIndex=i
                }
            }

            for(i in 0 until currencyList.size){
                if(currencyList[i]==currencyDescription.text.toString()){
                    selectedCurrencyIndex=i
                }
            }

            currentNodeCardView.setOnClickListener{
                val intent = Intent(this@WalletSettings, NodeActivity::class.java)
                push(intent)
            }
            addressBookLayout.setOnClickListener {
                TextSecurePreferences.setSendAddressDisable(this@WalletSettings,true)
                val intent = Intent(this@WalletSettings, AddressBookActivity::class.java)
                push(intent)
               /* intent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK)
                push(intent)*/
            }
            changePinLayout.setOnClickListener {
                TextSecurePreferences.setChangePin(this@WalletSettings,true)
                val lockManager: LockManager<CustomPinActivity> = LockManager.getInstance() as LockManager<CustomPinActivity>
                lockManager.enableAppLock(this@WalletSettings, CustomPinActivity::class.java)
                val intent = Intent(this@WalletSettings, CustomPinActivity::class.java)
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.CHANGE_PIN)
                intent.putExtra("change_pin",true)
                intent.putExtra("send_authentication",false)
                push(intent)
            }

            displayBalanceAsLayout.setOnClickListener {
                openDisplayBalanceAsDialogBox()
            }

            feePriorityLayout.setOnClickListener {
                openFeePriorityDialogBox()
            }

            decimalsLayout.setOnClickListener {
                openDecimalsDialogBox()
            }

            currencyLayout.setOnClickListener {
                openCurrencyDialogBox()
            }

            saveRecipientAddressSwitchCompat.setOnClickListener {
                if(TextSecurePreferences.getSaveRecipientAddress(this@WalletSettings)){
                    TextSecurePreferences.setSaveRecipientAddress(this@WalletSettings,false)
                    saveRecipientAddressSwitchCompat.isChecked = TextSecurePreferences.getSaveRecipientAddress(this@WalletSettings)
                }else{
                    TextSecurePreferences.setSaveRecipientAddress(this@WalletSettings,true)
                    saveRecipientAddressSwitchCompat.isChecked = TextSecurePreferences.getSaveRecipientAddress(this@WalletSettings)
                }
            }
            saveRecipientAddressSwitchCompat.isChecked = TextSecurePreferences.getSaveRecipientAddress(this@WalletSettings)

            /*enableFiatCurrencyConversionSwitchCompat.setOnClickListener {
                if(TextSecurePreferences.getFiatCurrencyCheckedStatus(this@WalletSettings)){
                    TextSecurePreferences.setFiatCurrencyCheckedStatus(this@WalletSettings,false)
                    enableFiatCurrencyConversionSwitchCompat.isChecked = TextSecurePreferences.getFiatCurrencyCheckedStatus(this@WalletSettings)
                }else{
                    TextSecurePreferences.setFiatCurrencyCheckedStatus(this@WalletSettings,true)
                    enableFiatCurrencyConversionSwitchCompat.isChecked = TextSecurePreferences.getFiatCurrencyCheckedStatus(this@WalletSettings)
                }
            }
            enableFiatCurrencyConversionSwitchCompat.isChecked = TextSecurePreferences.getFiatCurrencyCheckedStatus(this@WalletSettings)*/
        }
    }

    override fun onResume() {
        super.onResume()
        /*binding.currentNodeTextViewValue.text = getNode()?.host.toString()*/
        val parts = getNode().split(":")
        if (CheckOnline.isOnline(this)) {
            binding.currentNodeTextViewValue.text = parts[0]
        } else {
            binding.currentNodeTextViewValue.text = "Waiting for network.."
        }
    }

    private fun openDisplayBalanceAsDialogBox() {
        dialog = Dialog(this@WalletSettings)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.wallet_sub_options_list)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val dialogBoxTitle = dialog.findViewById(R.id.dialogBoxTitle) as TextView
        dialogBoxTitle.text = getString(R.string.display_balance_as)
        val walletSubOptionsList = dialog.findViewById(R.id.walletSubOptionsListRecyclerView) as RecyclerView
        val close = dialog.findViewById(R.id.closeDialogBox) as ImageView
        close.setOnClickListener {
            dialog.dismiss()
        }

        walletSubOptionsList.layoutManager = LinearLayoutManager(this)
        walletSubOptionsListAdapter = WalletSubOptionsListAdapter(this, displayBalanceAsList,TextSecurePreferences.getDisplayBalanceAs(this),1)
        walletSubOptionsListAdapter.setClickListener(this)
        walletSubOptionsList.adapter = walletSubOptionsListAdapter
        dialog.show()
    }

    private fun openFeePriorityDialogBox() {
        dialog = Dialog(this@WalletSettings)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.wallet_sub_options_list)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val dialogBoxTitle = dialog.findViewById(R.id.dialogBoxTitle) as TextView
        dialogBoxTitle.text = getString(R.string.fee_priority)
        val walletSubOptionsList = dialog.findViewById(R.id.walletSubOptionsListRecyclerView) as RecyclerView
        val close = dialog.findViewById(R.id.closeDialogBox) as ImageView
        close.setOnClickListener {
            dialog.dismiss()
        }

        walletSubOptionsList.layoutManager = LinearLayoutManager(this)
        walletSubOptionsListAdapter = WalletSubOptionsListAdapter(this, feePriorityList,TextSecurePreferences.getFeePriority(this),4)
        walletSubOptionsListAdapter.setClickListener(this)
        walletSubOptionsList.adapter = walletSubOptionsListAdapter
        dialog.show()
    }

    private fun openDecimalsDialogBox() {
        dialog = Dialog(this@WalletSettings)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.wallet_sub_options_list)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val dialogBoxTitle = dialog.findViewById(R.id.dialogBoxTitle) as TextView
        dialogBoxTitle.text = getString(R.string.decimals)
        val walletSubOptionsList = dialog.findViewById(R.id.walletSubOptionsListRecyclerView) as RecyclerView
        val close = dialog.findViewById(R.id.closeDialogBox) as ImageView
        close.setOnClickListener {
            dialog.dismiss()
        }

        walletSubOptionsList.layoutManager = LinearLayoutManager(this)
        walletSubOptionsListAdapter = WalletSubOptionsListAdapter(this, decimalsList,selectedDecimalIndex,2)
        walletSubOptionsListAdapter.setClickListener(this)
        walletSubOptionsList.adapter = walletSubOptionsListAdapter
        dialog.show()
    }

    /*private fun openDecimalsDialogBox() {
        dialog = Dialog(this@WalletSettings)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.wallet_sub_options_search_list)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val dialogBoxTitle = dialog.findViewById(R.id.dialogBoxTitle) as TextView
        dialogBoxTitle.text = "Decimals"
        val walletSubOptionsList = dialog.findViewById(R.id.walletSubOptionsListRecyclerView) as RecyclerView
        val close = dialog.findViewById(R.id.closeDialogBox) as ImageView
        close.setOnClickListener {
            dialog.dismiss()
        }
        val searchText = dialog.findViewById(R.id.searchTextEditText) as EditText

        searchText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                // filter your list from your input
                filter(s.toString(),decimalsList)
                //you can use runnable postDelayed like 500 ms to delay search text
            }
        })

        walletSubOptionsList.layoutManager = LinearLayoutManager(this)
        walletSubOptionsSearchListItemAdapter = WalletSubOptionsSearchListItemAdapter(this, decimalsList,selectedDecimalIndex,2)
        walletSubOptionsSearchListItemAdapter.setClickListener(this)
        walletSubOptionsList.adapter = walletSubOptionsSearchListItemAdapter
        dialog.show()
    }*/

    private fun openCurrencyDialogBox() {
        dialog = Dialog(this@WalletSettings)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.wallet_sub_options_search_list)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val dialogBoxTitle = dialog.findViewById(R.id.dialogBoxTitle) as TextView
        dialogBoxTitle.text = getString(R.string.currency)
        val walletSubOptionsList = dialog.findViewById(R.id.walletSubOptionsListRecyclerView) as RecyclerView
        val close = dialog.findViewById(R.id.closeDialogBox) as ImageView
        close.setOnClickListener {
            dialog.dismiss()
        }
        val searchText = dialog.findViewById(R.id.searchTextEditText) as EditText

        searchText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }
            override fun afterTextChanged(s: Editable) {
                filter(s.toString(),currencyList)
            }
        })

        walletSubOptionsList.layoutManager = LinearLayoutManager(this)
        walletSubOptionsSearchListItemAdapter = WalletSubOptionsSearchListItemAdapter(this, currencyList,selectedCurrencyIndex,3)
        walletSubOptionsSearchListItemAdapter.setClickListener(this)
        walletSubOptionsList.adapter = walletSubOptionsSearchListItemAdapter
        dialog.show()
    }

    override fun onItemClick(view: View?, position: Int,option:Int) {
        when (option) {
            1 -> {
                binding.displayBalanceAsDescription.text = walletSubOptionsListAdapter.getItem(position)
                TextSecurePreferences.setDisplayBalanceAs(this,position)
            }
            2 -> {
                binding.decimalsDescription.text = walletSubOptionsListAdapter.getItem(position)
                TextSecurePreferences.setDecimals(this,walletSubOptionsListAdapter.getItem(position))
                for(i in 0 until decimalsList.size){
                    if(decimalsList[i]==binding.decimalsDescription.text.toString()){
                        selectedDecimalIndex=i
                    }
                }
            }
            else -> {
                binding.feePriorityDescription.text = walletSubOptionsListAdapter.getItem(position)
                TextSecurePreferences.setFeePriority(this,position)
            }
        }
        dialog.dismiss()
    }

    override fun onItemClicks(view: View?, position: Int, option: Int) {
        /*if(option==2){
            binding.decimalsDescription.text = walletSubOptionsSearchListItemAdapter.getItem(position)
            TextSecurePreferences.setDecimals(this,walletSubOptionsSearchListItemAdapter.getItem(position))
            for(i in 0 until decimalsList.size){
                if(decimalsList[i]==binding.decimalsDescription.text.toString()){
                    selectedDecimalIndex=i
                }
            }
            dialog.dismiss()
        }
        else{
            binding.currencyDescription.text = walletSubOptionsSearchListItemAdapter.getItem(position)
            TextSecurePreferences.setCurrency(this,walletSubOptionsSearchListItemAdapter.getItem(position))
            for(i in 0 until currencyList.size){
                if(currencyList[i]==binding.currencyDescription.text.toString()){
                    selectedCurrencyIndex=i
                }
            }
            dialog.dismiss()
        }*/
        binding.currencyDescription.text = walletSubOptionsSearchListItemAdapter.getItem(position)
        TextSecurePreferences.setCurrency(this,walletSubOptionsSearchListItemAdapter.getItem(position))
        for(i in 0 until currencyList.size){
            if(currencyList[i]==binding.currencyDescription.text.toString()){
                selectedCurrencyIndex=i
            }
        }
        dialog.dismiss()
    }

    fun filter(text: String?, arrayList: ArrayList<String>) {
        val temp: MutableList<String> = ArrayList()
        for (d in arrayList) {
            //or use .equal(text) with you want equal match
            //use .toLowerCase() for better matches
            if (d.lowercase().contains(text!!)) {
                temp.add(d)
            }
        }
        //update recyclerview
        walletSubOptionsSearchListItemAdapter.updateList(temp)
    }

    /*fun getNode(): NodeInfo? {
        val selectedNodeId = getSelectedNodeId()
        Log.d("WalletSettings",selectedNodeId.toString())
        return NodeInfo.fromString(selectedNodeId)
    }*/

    fun getNode(): String {
        val selectedNodeId = getSelectedNodeId()
        Log.d("WalletSettings",selectedNodeId.toString())
        return selectedNodeId.toString()
    }

    private fun getSelectedNodeId(): String? {
        return getSharedPreferences(
            SELECTED_NODE_PREFS_NAME,
            MODE_PRIVATE
        ).getString("0", null)
    }

    override fun onBackPressed() {
        if (TextSecurePreferences.getDaemon(this)) {
            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            finish()
        }
        super.onBackPressed()
    }


}
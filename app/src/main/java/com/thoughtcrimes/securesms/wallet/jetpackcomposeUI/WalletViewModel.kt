package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thoughtcrimes.securesms.model.TransactionInfo

class WalletViewModels : ViewModel() {

    private val _balance = MutableLiveData<String>()
    val balance: LiveData<String> get () = _balance

    private val _estimatedFee = MutableLiveData<String>()
    val estimatedFee: LiveData<String> get () = _estimatedFee

    private val _unLockedBalance = MutableLiveData<Long>()
    val unLockedBalance: LiveData<Long> get () = _unLockedBalance

    private val _selectedOption = MutableLiveData<String>()
    val selectedOption: LiveData<String> get() = _selectedOption

    fun updateBalance(balance: String){
        _balance.value = balance
    }
    fun updateEstimatedFee(fee: String){
        _estimatedFee.value = fee
    }

    fun updateUnlockedBalance(unlockedBalance: Long){
        _unLockedBalance.postValue(unlockedBalance)
    }

    fun updateSelectedOption(selectedItem: String){
        _selectedOption.postValue(selectedItem)
    }

    private val _sendCardViewButtonIsEnabled = MutableLiveData<Boolean>()
    val sendCardViewButtonIsEnabled: LiveData<Boolean> get () = _sendCardViewButtonIsEnabled

    private val _sendCardViewButtonTextColor = MutableLiveData<Int>()
    val sendCardViewButtonTextColor: LiveData<Int> get () = _sendCardViewButtonTextColor

    private val _scanQRCodeButtonIsEnabled = MutableLiveData<Boolean>()
    val scanQRCodeButtonIsEnabled: LiveData<Boolean> get () = _scanQRCodeButtonIsEnabled

    private val _syncStatusTextColor = MutableLiveData<Int>()
    val syncStatusTextColor: LiveData<Int> get () = _syncStatusTextColor

    private val _progressBarColor = MutableLiveData<Int>()
    val progressBarColor: LiveData<Int> get () = _progressBarColor

    private val _sendCardViewButtonIsClickable = MutableLiveData<Boolean>()
    val sendCardViewButtonIsClickable: LiveData<Boolean> get () = _sendCardViewButtonIsClickable

    private val _receiveCardViewButtonIsClickable = MutableLiveData<Boolean>()
    val receiveCardViewButtonIsClickable: LiveData<Boolean> get () = _receiveCardViewButtonIsClickable

    private val _progressBarIsVisible = MutableLiveData<Boolean>()
    val progressBarIsVisible: LiveData<Boolean> get () = _progressBarIsVisible

    private val _progress = MutableLiveData<Float>()
    val progress: LiveData<Float> get () = _progress

    private val _syncStatus = MutableLiveData<String?>()
    val syncStatus: LiveData<String?> get () = _syncStatus

    private val _receiveCardViewButtonIsEnabled = MutableLiveData<Boolean>()
    val receiveCardViewButtonIsEnabled: LiveData<Boolean> get () = _receiveCardViewButtonIsEnabled

    private val _transactionListContainerIsVisible = MutableLiveData<Boolean>()
    val transactionListContainerIsVisible: LiveData<Boolean> get () = _transactionListContainerIsVisible

    private val _filterTransactionIconIsClickable = MutableLiveData<Boolean>()
    val filterTransactionIconIsClickable: LiveData<Boolean> get () = _filterTransactionIconIsClickable

    private val _walletBalance = MutableLiveData<String>()
    val walletBalance: LiveData<String> get () = _walletBalance

    private val _fiatCurrency = MutableLiveData<String>()
    val fiatCurrency: LiveData<String> get () = _fiatCurrency

    private val _fetchBalanceStatus = MutableLiveData<Boolean>()
    val fetchBalanceStatus: LiveData<Boolean> get () = _fetchBalanceStatus

    private val _transactionInfoItems = MutableLiveData<MutableList<TransactionInfo>>()
    val transactionInfoItems: LiveData<MutableList<TransactionInfo>> get () = _transactionInfoItems

    fun sendCardViewButtonIsEnabled(isEnabled: Boolean){
        _sendCardViewButtonIsEnabled.value = isEnabled
    }

    fun setSendCardViewButtonTextColor(color: Int){
        _sendCardViewButtonTextColor.value = color
    }

    fun scanQRCodeButtonIsEnabled(isEnabled: Boolean){
        _scanQRCodeButtonIsEnabled.value = isEnabled
    }

    fun setSyncStatusTextColor(color: Int){
        _syncStatusTextColor.value = color
    }

    fun setProgressBarColor(color: Int){
        _progressBarColor.value = color
    }

    fun sendCardViewButtonIsClickable(isClickable: Boolean){
        _sendCardViewButtonIsClickable.value = isClickable
    }

    fun receiveCardViewButtonIsClickable(isClickable: Boolean){
        _receiveCardViewButtonIsClickable.value = isClickable
    }

    fun progressBarIsVisible(isVisible: Boolean){
        _progressBarIsVisible.value = isVisible
    }

    fun setProgress(progress: Float){
        _progress.value = progress
    }

    fun setSyncStatus(status: String?){
        _syncStatus.value = status
    }

    fun receiveCardViewButtonIsEnabled(isEnabled: Boolean){
        _receiveCardViewButtonIsEnabled.value = isEnabled
    }

    fun setTransactionListContainerIsVisible(isVisible: Boolean){
        _transactionListContainerIsVisible.value = isVisible
    }

    fun setFilterTransactionIconIsClickable(isVisible: Boolean){
        _filterTransactionIconIsClickable.value = isVisible
    }

    fun updateWalletBalance(walletBalance: String){
        _walletBalance.value = walletBalance
    }

    fun updateFiatCurrency(fiatCurrency: String){
        _fiatCurrency.value = fiatCurrency
    }

    fun updateFetchBalanceStatus(status: Boolean){
        _fetchBalanceStatus.value = status
    }

    fun updateTransactionInfoItems(infoItems: MutableList<TransactionInfo>){
        _transactionInfoItems.value = infoItems
    }

    fun setTransactionInfoItems(newItems: MutableList<TransactionInfo>) {
        newItems.sort()
        _transactionInfoItems.value = newItems
    }
}
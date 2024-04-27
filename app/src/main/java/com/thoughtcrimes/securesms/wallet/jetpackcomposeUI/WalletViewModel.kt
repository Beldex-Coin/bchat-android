package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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
}
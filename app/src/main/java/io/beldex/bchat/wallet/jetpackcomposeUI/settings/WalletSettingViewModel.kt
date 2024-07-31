package io.beldex.bchat.wallet.jetpackcomposeUI.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WalletSettingViewModel: ViewModel() {

    private val _displayBalance = MutableLiveData<Int>()
    val displayBalance: LiveData<Int> get () = _displayBalance

    private val _decimal = MutableLiveData<String>()
    val decimal: LiveData<String> get () = _decimal

    private val _currency = MutableLiveData<String>()
    val currency: LiveData<String> get () = _currency

    private val _feePriority = MutableLiveData<Int>()
    val feePriority: LiveData<Int> get () = _feePriority

    fun updateDisplayBalance(selectOption: Int){
        _displayBalance.value = selectOption
    }
    fun updateDecimal(selectOption: String){
        _decimal.value = selectOption
    }
    fun updateCurrency(selectOption: String){
        _currency.value = selectOption
    }
    fun updateFeePriority(selectOption: Int){
        _feePriority.value = selectOption
    }
}
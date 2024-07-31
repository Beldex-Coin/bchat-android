package io.beldex.bchat.wallet.jetpackcomposeUI

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RescanViewModels : ViewModel() {
    private val _restoreFromDate = MutableLiveData<String>()
    val restoreFromDate: LiveData<String> get () = _restoreFromDate

    fun updateRestoreFromDate(date:String){
        _restoreFromDate.value = date
    }
}
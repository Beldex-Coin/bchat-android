package io.beldex.bchat.delegates

import android.content.Context

interface WalletDelegates {

    fun refreshBalance(
        synchronized: Boolean,
        unlockedBalance: Long,
        balance: Long,
        setBalance: (valueOfBalance: String, valueOfUnLockedBalance: String?, synchronized: Boolean) -> Unit
    )

    fun showBalance(
        walletBalance: String?,
        walletUnlockedBalance: String?,
        synchronized: Boolean,
        mContext: Context?,
        setBalance: (valueOfBalance: String, valueOfUnLockedBalance: String?) -> Unit
    )

}
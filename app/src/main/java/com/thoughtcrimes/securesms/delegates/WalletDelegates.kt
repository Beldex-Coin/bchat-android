package com.thoughtcrimes.securesms.delegates

import android.content.Context
import androidx.fragment.app.FragmentActivity

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
package com.thoughtcrimes.securesms.delegates

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.util.Helper

class WalletDelegatesImpl: WalletDelegates {

    override fun refreshBalance(
        synchronized: Boolean,
        unlockedBalance: Long,
        balance: Long,
        setBalance: (valueOfBalance: String, valueOfUnLockedBalance: String?, synchronized: Boolean) -> Unit
    ) {
        val unlockedBalanceLocal: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        val balanceLocal: Double = Helper.getDecimalAmount(balance).toDouble()
        if (balanceLocal > 0.0) {
            setBalance(
                Helper.getFormattedAmount(balanceLocal, true),
                Helper.getFormattedAmount(unlockedBalanceLocal, true),
                true
            )
        } else {
            setBalance(
                Helper.getFormattedAmount(balanceLocal, true),
                Helper.getFormattedAmount(unlockedBalanceLocal, true),
                synchronized
            )
        }
    }

    override fun showBalance(
        walletBalance: String?,
        walletUnlockedBalance: String?,
        synchronized: Boolean,
        mContext: Context?,
        setBalance: (valueOfBalance: String, valueOfUnLockedBalance: String?) -> Unit
    ) {
        if (mContext != null) {
            if (!synchronized) {
                when {
                    TextSecurePreferences.getDecimals(mContext) == "2 - Two (0.00)" -> {
                        setBalance(
                            "-.--",
                            "-.--"
                        )
                    }
                    TextSecurePreferences.getDecimals(mContext) == "3 - Three (0.000)" -> {
                        setBalance(
                            "-.---",
                            "-.---"
                        )
                    }
                    TextSecurePreferences.getDecimals(mContext) == "0 - Zero (000)" -> {
                        setBalance(
                            "-",
                            "-"
                        )
                    }
                    else -> {
                        setBalance(
                            "-.----",
                            "-.----"
                        )
                    }
                }
            } else {
                when {
                    TextSecurePreferences.getDecimals(mContext) == "2 - Two (0.00)" -> {
                        setBalance(
                            String.format("%.2f", walletBalance!!.replace(",", "").toDouble()),
                            String.format(
                                "%.2f",
                                walletUnlockedBalance!!.replace(",", "").toDouble()
                            )
                        )
                    }
                    TextSecurePreferences.getDecimals(mContext) == "3 - Three (0.000)" -> {
                        setBalance(
                            String.format("%.3f", walletBalance!!.replace(",", "").toDouble()),
                            String.format(
                                "%.3f",
                                walletUnlockedBalance!!.replace(",", "").toDouble()
                            )
                        )
                    }
                    TextSecurePreferences.getDecimals(mContext) == "0 - Zero (000)" -> {
                        setBalance(
                            String.format("%.0f", walletBalance!!.replace(",", "").toDouble()),
                            String.format(
                                "%.0f",
                                walletUnlockedBalance!!.replace(",", "").toDouble()
                            )
                        )
                    }
                    else -> {
                        setBalance(
                            walletBalance!!,
                            null
                        )
                    }
                }
            }
        }
    }
}
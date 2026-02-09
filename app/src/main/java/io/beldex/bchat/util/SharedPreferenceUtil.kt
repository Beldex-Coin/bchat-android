package io.beldex.bchat.util

import android.content.Context
import android.content.SharedPreferences
import com.beldex.libbchat.utilities.TextSecurePreferences

class SharedPreferenceUtil(
    private val context: Context
) {


    fun getPreference(prefKey: String, mode: Int): SharedPreferences {
        return context.getSharedPreferences(prefKey, mode)
    }


    fun getProfileName(): String? {
        return TextSecurePreferences.getProfileName(context)
    }

    fun getPublicKey(): String {
        return TextSecurePreferences.getLocalNumber(context)!!
    }

    fun getSavedPassword(): String? {
        return TextSecurePreferences.getMyPassword(context)
    }

    fun setPassword(pinCode: String) {
        TextSecurePreferences.setMyPassword(context, pinCode)
    }

    fun getWalletSavePassword():String? {
        return TextSecurePreferences.getWalletEntryPassword(context)
    }

}
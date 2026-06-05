package io.beldex.bchat.util

import android.app.LocaleManager
import android.os.Build
import androidx.core.os.ConfigurationCompat
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter
import android.content.Context
import java.util.Locale

fun shortNameAndAddress(name:String, address: String): String {
    return if(name == address) {
        if(address.length >= 7) {
            "${address.take(4).capitalizeFirstLetter()}....${address.takeLast(3)}"
        } else {
            address.capitalizeFirstLetter()
        }
    } else {
        name.capitalizeFirstLetter()
    }
}

fun isValidGroupName(name: String): Boolean {
    val groupNameRegex = Regex("^(?=.*[A-Za-z0-9])[A-Za-z0-9_\\-\\s]+$")
    return name.matches(groupNameRegex)
}

fun getDeviceSettingsLanguage(context: Context): String {
    val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        val localeManager = context.getSystemService(LocaleManager::class.java)
        val locales = localeManager.systemLocales

        if (locales.isEmpty) null else locales[0]
    } else {
        ConfigurationCompat.getLocales(
            context.resources.configuration
        )[0]
    }

    return (systemLocale ?: Locale.getDefault()).language
}
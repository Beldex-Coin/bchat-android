package com.thoughtcrimes.securesms.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import io.beldex.bchat.BuildConfig

class FirebaseRemoteConfigUtil {

    private val instance = FirebaseRemoteConfig.getInstance()

    fun init() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG)
                0 // Kept 0 for quick debug
            else
                60 * 60 * 24// Change this based on your requirement
        }
        instance.setConfigSettingsAsync(configSettings)
        instance.setDefaultsAsync(DEFAULT_DATA)
        instance.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println(">>>>firebase fetch successfull:${task.result}")
            } else {
                println(">>>>firebase fetch failed")
            }
        }
    }

    fun getInfoMessage() = instance.getString(INFO_MESSAGE)

    companion object {
        private const val INFO_MESSAGE = "info_message"
        private val DEFAULT_DATA = mapOf(
            "info_message" to ""
        )
    }
}
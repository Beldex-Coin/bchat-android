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

            } else {

            }
        }
    }

    fun getInfoMessage() = instance.getString(INFO_MESSAGE)

    fun showPromotionalOffer() = instance.getBoolean(SHOW_PROMOTION)

    fun getPromotionData() = instance.getString(PROMOTION_DATA)

    companion object {
        private const val INFO_MESSAGE = "info_message"
        private const val SHOW_PROMOTION = "show_promotion"
        private const val PROMOTION_DATA = "promotion_data"
        private val DEFAULT_DATA = mapOf(
            INFO_MESSAGE to "",
            SHOW_PROMOTION to false,
            PROMOTION_DATA to ""
        )
    }
}
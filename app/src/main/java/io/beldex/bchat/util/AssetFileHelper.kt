package io.beldex.bchat.util

import android.app.Application
import java.io.IOException
import javax.inject.Inject

class AssetFileHelper @Inject constructor(
    private val application: Application
) {

    fun loadChangeLogsFromAsset(): String? {
        return try {
            application.assets.open("changeLog.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
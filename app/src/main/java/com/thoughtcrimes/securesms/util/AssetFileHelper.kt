package com.thoughtcrimes.securesms.util

import android.app.Application
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import javax.inject.Inject

class AssetFileHelper @Inject constructor(
    private val application: Application
) {

    fun loadChangeLogsFromAsset(): String? {
        val json: String? = try {
            val `is`: InputStream = application.assets.open("changeLog.json")
            val size: Int = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

}
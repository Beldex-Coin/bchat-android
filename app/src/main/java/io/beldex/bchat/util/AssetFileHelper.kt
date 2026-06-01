package io.beldex.bchat.util

import android.app.Application
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Locale
import javax.inject.Inject

class AssetFileHelper @Inject constructor(
    private val application: Application
) {

    fun loadChangeLogsFromAsset(): String? {
        return try {
            val folder = when (Locale.getDefault().language) {
                "ar" -> "ar" // Arabic
                "zh" -> "zh" // Chinese
                "de" -> "de" // German
                "ja" -> "ja" // Japanese
                "ko" -> "ko" // Korean
                "pt" -> "pt" // Portuguese
                "ru" -> "ru" // Russian
                "es" -> "es" // Spanish
                "tr" -> "tr" // Turkish
                "vi" -> "vi" // Vietnamese
                else -> "en"
            }

            application.assets.open("changeLog/$folder/changeLog.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun loadAboutContent(): String? {
        val json: String? = try {
            val inputStream = application.assets.open("about.txt")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

}
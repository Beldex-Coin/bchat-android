package com.beldex.libbchat.utilities

import okhttp3.HttpUrl
import com.beldex.libbchat.messaging.file_server.FileServerAPIV2
import com.beldex.libsignal.utilities.HTTP
import com.beldex.libsignal.utilities.Log
import java.io.*

object DownloadUtilities {

    /**
     * Blocks the calling thread.
     */
    @JvmStatic
    fun downloadFile(destination: File, url: String) {
        val outputStream = FileOutputStream(destination) // Throws
        var remainingAttempts = 4
        var exception: Exception? = null
        while (remainingAttempts > 0) {
            remainingAttempts -= 1
            try {
                downloadFile(outputStream, url)
                exception = null
                break
            } catch (e: Exception) {
                exception = e
            }
        }
        if (exception != null) { throw exception }
    }

    /**
     * Blocks the calling thread.
     */
    @JvmStatic
    fun downloadFile(outputStream: OutputStream, urlAsString: String) {
        val url = HttpUrl.parse(urlAsString)!!
        val fileID = url.pathSegments().last()
        try {
            FileServerAPIV2.download(fileID.toLong()).get().let {
                outputStream.write(it)
            }
        } catch (e: Exception) {
            //Log.e("Beldex", "Couldn't download attachment.", e)
            when (e) {
                // No need for the stack trace for HTTP errors
                is HTTP.HTTPRequestFailedException -> Log.e("Beldex", "Couldn't download attachment due to error: ${e.message}")
                else -> Log.e("Beldex", "Couldn't download attachment", e)
            }
            throw e
        }
    }
}
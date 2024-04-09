package com.thoughtcrimes.securesms.util.nodelistasync

import android.content.Context
import android.util.Log
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import kotlinx.coroutines.NonCancellable.isCancelled
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


class DownloadNodeListFileAsyncTask(private val mContext: Context) :
    AsyncTaskCoroutine<String?, String?>() {
    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun doInBackground(vararg downloadUrl: String?): String? {
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(downloadUrl[0])
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.d("Error","Server returned HTTP  + ${connection.responseCode} \n +${connection.responseMessage}")
                return ("Server returned HTTP " + connection.responseCode
                        + " " + connection.responseMessage)
            }

            // download the file
            input = connection.inputStream

            val file = File(mContext.filesDir,"/${NodeListConstants.downloadNodeListFileName}")
            if(file.exists()){
                file.delete()
            }
            output = FileOutputStream(mContext.filesDir.toString() + "/${NodeListConstants.downloadNodeListFileName}")
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                // allow canceling with back button
                if (isCancelled) {
                    input.close()
                    return null
                }
                total += count.toLong()
                output.write(data, 0, count)
            }
        } catch (e: Exception) {
            return e.toString()
        } finally {
            try {
                output?.close()
                input?.close()
            } catch (ignored: IOException) {
            }
            connection?.disconnect()
        }
        return null
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        print("Download Node List onPostExecute -> $result")
    }
}
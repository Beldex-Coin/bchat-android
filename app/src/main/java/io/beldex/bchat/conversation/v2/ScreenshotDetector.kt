package io.beldex.bchat.conversation.v2

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

class ScreenshotDetector(private val context: Context, private val listener: ScreenshotDetectionListeners) {

    private var lastScreenshotTime = 0L
    private val screenshotCooldownMillis = 3000 // 3 seconds

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (uri != null && uri.toString().contains("images/media")) {
                val projection = arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN
                )
                val cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val dateTaken = cursor.getLong(1)
                    val now = System.currentTimeMillis()

                    if (now - dateTaken < 5000 && now - lastScreenshotTime > screenshotCooldownMillis) {
                        lastScreenshotTime = now
                        listener.onScreenCaptured()
                    }

                    cursor.close()
                }
            }
        }
    }

    fun register() {
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    fun unregister() {
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    interface ScreenshotDetectionListeners {
        fun onScreenCaptured()
    }
}
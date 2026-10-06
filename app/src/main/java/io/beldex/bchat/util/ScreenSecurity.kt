package io.beldex.bchat.util

import android.app.Activity
import android.view.WindowManager
import com.beldex.libbchat.utilities.TextSecurePreferences

object ScreenSecurity {
    @JvmStatic
    fun apply(activity: Activity) {
        val window = activity.window
        if (TextSecurePreferences.isScreenSecurityEnabled(activity)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

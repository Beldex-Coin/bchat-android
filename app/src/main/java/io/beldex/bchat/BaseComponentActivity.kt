package io.beldex.bchat

import android.app.ProgressDialog
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getAppSelectedLanguage
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageActivityHelper
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.ScreenSecurity

open class BaseComponentActivity : ComponentActivity() {
    protected override fun onResume() {
        ScreenSecurity.apply(this)
        super.onResume()
        DynamicLanguageActivityHelper.recreateIfNotInCorrectLanguage(
            this,
            getAppSelectedLanguage(this)
        )
    }

    protected override fun attachBaseContext(newBase: Context) {
        val selected = getAppSelectedLanguage(newBase)
        val wrapped = DynamicLanguageContextWrapper.updateContext(
            newBase,
            selected
        )
        super.attachBaseContext(wrapped)
        val nightBit = when (UiModeUtilities.getUserSelectedUiMode(newBase)) {
            UiMode.NIGHT -> Configuration.UI_MODE_NIGHT_YES
            UiMode.DAY -> Configuration.UI_MODE_NIGHT_NO
            UiMode.SYSTEM_DEFAULT -> null
        }
        if (nightBit != null) {
            val current = wrapped.resources.configuration.uiMode
            applyOverrideConfiguration(Configuration().apply {
                uiMode = (current and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightBit
            })
        }
    }

    // Mirrors BaseActionBarActivity's progress dialog helper, for Compose-migrated activities
    // that still drive async work (e.g. onboarding wallet creation) needing a blocking spinner.
    private var progressDialog: ProgressDialog? = null

    private inner class SimpleProgressDialog(context: Context, msgId: Int) : ProgressDialog(context) {
        init {
            setCancelable(false)
            setMessage(context.getString(msgId))
        }
    }

    fun showProgressDialog(msgId: Int, delayMillis: Long) {
        dismissProgressDialog() // just in case
        val dialog = SimpleProgressDialog(this, msgId)
        progressDialog = dialog
        if (delayMillis > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (progressDialog != null) dialog.show()
            }, delayMillis)
        } else {
            dialog.show()
        }
    }

    fun dismissProgressDialog() {
        val dialog = progressDialog ?: return
        if (dialog.isShowing) {
            dialog.dismiss()
        }
        progressDialog = null
    }
}
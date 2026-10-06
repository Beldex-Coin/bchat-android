package io.beldex.bchat

import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getAppSelectedLanguage
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageActivityHelper
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper

open class BaseComponentActivity : ComponentActivity() {
    protected override fun onResume() {
        io.beldex.bchat.util.ScreenSecurity.apply(this)
        super.onResume()
        DynamicLanguageActivityHelper.recreateIfNotInCorrectLanguage(
            this,
            getAppSelectedLanguage(this)
        )
    }

    protected override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            DynamicLanguageContextWrapper.updateContext(
                newBase,
                getAppSelectedLanguage(newBase)
            )
        )
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
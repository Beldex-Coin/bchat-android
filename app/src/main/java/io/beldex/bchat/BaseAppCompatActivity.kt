package io.beldex.bchat

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getAppSelectedLanguage
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageActivityHelper
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper


open class BaseAppCompatActivity : AppCompatActivity() {
    protected override fun onResume() {
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
}
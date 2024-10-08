package io.beldex.bchat.preferences

import android.os.Bundle
import io.beldex.bchat.R
import io.beldex.bchat.PassphraseRequiredActionBarActivity

class PrivacySettingsActivity : PassphraseRequiredActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        setContentView(R.layout.activity_fragment_wrapper)
        supportActionBar!!.title = getString(R.string.activity_settings_title)
        val fragment =
            AppProtectionPreferenceFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}
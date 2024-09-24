package io.beldex.bchat.preferences

import android.os.Bundle
import io.beldex.bchat.R
import io.beldex.bchat.PassphraseRequiredActionBarActivity

class NotificationSettingsActivity : PassphraseRequiredActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        setContentView(R.layout.activity_fragment_wrapper)
        supportActionBar!!.title = resources.getString(R.string.activity_notification_settings_title)
        val fragment =NotificationsPreferenceFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}
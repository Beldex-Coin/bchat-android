package com.thoughtcrimes.securesms.preferences

import android.os.Bundle
import io.beldex.bchat.R
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity

class ChatSettingsActivity : PassphraseRequiredActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        setContentView(R.layout.activity_fragment_wrapper)
        supportActionBar!!.title = resources.getString(R.string.activity_chat_settings_title)
        val fragment = ChatsPreferenceFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}
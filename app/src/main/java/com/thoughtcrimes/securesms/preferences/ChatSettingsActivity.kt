package com.thoughtcrimes.securesms.preferences

import android.os.Bundle
import android.view.LayoutInflater
import io.beldex.bchat.R
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import io.beldex.bchat.databinding.ActivityFragmentWrapperNewBinding

class ChatSettingsActivity : PassphraseRequiredActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        val binding = ActivityFragmentWrapperNewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        with(binding) {
            back.setOnClickListener { finish() }
            title.text = resources.getString(R.string.preferences_chats__chats)
        }
        val fragment = ChatsPreferenceFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}
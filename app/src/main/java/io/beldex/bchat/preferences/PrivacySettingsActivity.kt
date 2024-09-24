package io.beldex.bchat.preferences

import android.os.Bundle
import android.view.LayoutInflater
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityFragmentWrapperNewBinding

class PrivacySettingsActivity : PassphraseRequiredActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        val binding = ActivityFragmentWrapperNewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        with(binding) {
            back.setOnClickListener { finish() }
            title.text = getString(R.string.activity_settings_title)
        }
        val fragment =
            AppProtectionPreferenceFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}
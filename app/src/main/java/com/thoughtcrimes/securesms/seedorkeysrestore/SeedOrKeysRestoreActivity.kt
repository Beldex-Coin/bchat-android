package com.thoughtcrimes.securesms.seedorkeysrestore

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivitySeedOrKeysRestoreBinding
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.keys.RecoveryGetKeysDetailsActivity
import com.thoughtcrimes.securesms.onboarding.RecoveryPhraseRestoreActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class SeedOrKeysRestoreActivity : BaseActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySeedOrKeysRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Seed/Keys Restore")
        with(binding){
            seedOrKeysRestoreSeedCard.setOnClickListener {
                val intent = Intent(this@SeedOrKeysRestoreActivity, RecoveryPhraseRestoreActivity::class.java)
                push(intent)
            }
            seedOrKeysRestoreKeysCard.setOnClickListener {
                val intent = Intent(this@SeedOrKeysRestoreActivity, RecoveryGetKeysDetailsActivity::class.java)
                push(intent)
            }
        }
    }
}
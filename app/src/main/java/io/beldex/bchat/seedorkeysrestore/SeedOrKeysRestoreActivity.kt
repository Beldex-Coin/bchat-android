package io.beldex.bchat.seedorkeysrestore

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivitySeedOrKeysRestoreBinding
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.keys.RecoveryGetKeysDetailsActivity
import io.beldex.bchat.onboarding.RecoveryPhraseRestoreActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo

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
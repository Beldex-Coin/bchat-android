package io.beldex.bchat.keys

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivityRecoveryGetKeysDetailsBinding
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.onboarding.AppLockActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo

class RecoveryGetKeysDetailsActivity : BaseActionBarActivity() {
    private lateinit var binding:ActivityRecoveryGetKeysDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityRecoveryGetKeysDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Restore from Keys")
        with(binding){
            restoreKeysRestoreButton.setOnClickListener {
                val intent = Intent(this@RecoveryGetKeysDetailsActivity, AppLockActivity::class.java)
                push(intent)
            }
        }

    }
}
package com.thoughtcrimes.securesms.keys

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivityRecoveryGetKeysDetailsBinding
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.onboarding.AppLockActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

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
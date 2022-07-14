package com.thoughtcrimes.securesms.keys

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivityKeysPermissionBinding
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.onboarding.CheckPasswordActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class KeysPermissionActivity : PassphraseRequiredActionBarActivity() {
    private lateinit var binding:ActivityKeysPermissionBinding
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding= ActivityKeysPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("")
        with(binding){
            keysPermissionImportantConfirmButton.setOnClickListener {
                val intent = Intent(this@KeysPermissionActivity, CheckPasswordActivity::class.java)
                intent.putExtra("page",2)
                push(intent)
            }
        }
    }
}
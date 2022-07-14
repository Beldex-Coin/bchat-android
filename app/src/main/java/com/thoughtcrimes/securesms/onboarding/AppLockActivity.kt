package com.thoughtcrimes.securesms.onboarding

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivityAppLockBinding
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class AppLockActivity : BaseActionBarActivity() {
    private lateinit var binding : ActivityAppLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("App Lock",true)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            laterButton.setOnClickListener(){
                later()
            }
            enableButton.setOnClickListener()
            {
                enable()
            }
        }
    }

    private fun later() {
        val intent = Intent(this, HomeActivity::class.java)
        push(intent)
        finish()
    }

    private fun enable()
    {
        val intent = Intent(this,CreatePasswordActivity::class.java)
        push(intent)
        finish()
    }
}
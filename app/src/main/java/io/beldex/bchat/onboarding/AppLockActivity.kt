package io.beldex.bchat.onboarding

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivityAppLockBinding
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo

class AppLockActivity : BaseActionBarActivity() {
    private lateinit var binding : ActivityAppLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("App Lock",true)
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
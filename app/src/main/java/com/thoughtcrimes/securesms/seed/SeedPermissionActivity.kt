package com.thoughtcrimes.securesms.seed

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivitySeedPermissionBinding
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.onboarding.CheckPasswordActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class SeedPermissionActivity : BaseActionBarActivity() {
    private lateinit var binding:ActivitySeedPermissionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySeedPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("")
        with(binding){
            seedPermissionImportantConfirmButton.setOnClickListener {
                val intent = Intent(this@SeedPermissionActivity, CheckPasswordActivity::class.java)
                intent.putExtra("page",1)
                push(intent)
            }
        }
    }
}
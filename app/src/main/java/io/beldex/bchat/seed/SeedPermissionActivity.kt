package io.beldex.bchat.seed

import android.content.Intent
import android.os.Bundle
import io.beldex.bchat.databinding.ActivitySeedPermissionBinding
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.onboarding.CheckPasswordActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo

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
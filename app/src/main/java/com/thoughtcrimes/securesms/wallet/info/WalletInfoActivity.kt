package com.thoughtcrimes.securesms.wallet.info

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.onboarding.CheckPasswordActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySeedPermissionBinding
import io.beldex.bchat.databinding.ActivityWalletInfoBinding

class WalletInfoActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityWalletInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityWalletInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("")
        with(binding){
            walletInfoImportantConfirmButton.setOnClickListener {
                val intent = Intent(this@WalletInfoActivity, CheckPasswordActivity::class.java)
                intent.putExtra("page",2)
                push(intent)
                finish()
            }
        }
    }
}
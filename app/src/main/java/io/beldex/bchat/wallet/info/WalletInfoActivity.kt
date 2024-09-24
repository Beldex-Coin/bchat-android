package io.beldex.bchat.wallet.info

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.onboarding.CheckPasswordActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
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
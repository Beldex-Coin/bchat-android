package com.thoughtcrimes.securesms.wallet.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
import com.thoughtcrimes.securesms.wallet.SecureActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySettingsBinding
import io.beldex.bchat.databinding.ActivityWalletSettingsBinding

class WalletSettings : BaseActionBarActivity() {
    lateinit var binding:ActivityWalletSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Wallet Settings",false)
        binding = ActivityWalletSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
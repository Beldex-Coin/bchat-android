package com.thoughtcrimes.securesms.wallet.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySettingsBinding
import io.beldex.bchat.databinding.ActivityWalletSettingsBinding

class WalletSettings : AppCompatActivity() {
    lateinit var binding:ActivityWalletSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
package com.thoughtcrimes.securesms.wallet.utils.common

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.onboarding.LandingActivity
import com.thoughtcrimes.securesms.onboarding.PasswordActivity
import com.thoughtcrimes.securesms.wallet.WalletActivity
import io.beldex.bchat.databinding.ActivityLoadingBinding
import io.beldex.bchat.databinding.ActivitySplashScreenBinding
import timber.log.Timber

class LoadingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoadingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Handler(Looper.getMainLooper()).postDelayed({
            val walletName = TextSecurePreferences.getWalletName(this)
            val walletPassword = TextSecurePreferences.getWalletPassword(this)
            if (walletName != null && walletPassword!=null) {
                startWallet(walletName,walletPassword,
                    fingerprintUsed = false,
                    streetmode = false
                )
            }
        }, 3000)
    }

    private fun startWallet(
        walletName:String, walletPassword:String,
        fingerprintUsed:Boolean, streetmode:Boolean) {
        val REQUEST_ID = "id"
        val REQUEST_PW = "pw"
        val REQUEST_FINGERPRINT_USED = "fingerprint"
        val REQUEST_STREETMODE = "streetmode"
        val REQUEST_URI = "uri"

        Timber.d("startWallet()");
        TextSecurePreferences.setIncomingTransactionStatus(this, true)
        TextSecurePreferences.setOutgoingTransactionStatus(this, true)
        TextSecurePreferences.setTransactionsByDateStatus(this,false)
        val intent = Intent(this, WalletActivity::class.java)
        intent.putExtra(REQUEST_ID, walletName)
        intent.putExtra(REQUEST_PW, walletPassword)
        intent.putExtra(REQUEST_FINGERPRINT_USED, fingerprintUsed)
        intent.putExtra(REQUEST_STREETMODE, streetmode)
        //Important
        /*if (uri != null) {
            intent.putExtra(REQUEST_URI, uri)
            uri = null // use only once
        }*/
        startActivity(intent)
        finish()
    }
}
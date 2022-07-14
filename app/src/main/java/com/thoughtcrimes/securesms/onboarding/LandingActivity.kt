package com.thoughtcrimes.securesms.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityLandingBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.service.KeyCachingService
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.util.push

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            fakeChatView.startAnimating()
            registerButton.setOnClickListener() { register() }
            restoreButton.setOnClickListener { restore() }
            TermsandCondtionsTxt.setOnClickListener { link() }
            val isDayUiMode = UiModeUtilities.isDayUiMode(this@LandingActivity)
            (if (isDayUiMode) R.raw.landing_animation_light_theme else R.raw.landing_animation_dark_theme).also { img.setAnimation(it)}
        }
        IdentityKeyUtil.generateIdentityKeyPair(this)
        TextSecurePreferences.setPasswordDisabled(this, true)
        // AC: This is a temporary workaround to trick the old code that the screen is unlocked.
        KeyCachingService.setMasterSecret(applicationContext, Object())
    }

    private fun register() {
        val intent = Intent(this, DisplayNameActivity::class.java)
        push(intent)
        finish()
    }

    private fun restore() {
        /*val intent = Intent(this, RecoveryPhraseRestoreActivity::class.java)
        push(intent)*/
        //val intent = Intent(this, SeedOrKeysRestoreActivity::class.java)
        val intent = Intent(this,RecoveryPhraseRestoreActivity::class.java)
        push(intent)
        finish()
    }

    private fun link() {
        val viewIntent = Intent(
            "android.intent.action.VIEW",
            Uri.parse("https://bchat.beldex.io/terms-and-conditions")
        )
        startActivity(viewIntent)
        /*val intent = Intent(this, LinkDeviceActivity::class.java)
        push(intent)*/

    }
}
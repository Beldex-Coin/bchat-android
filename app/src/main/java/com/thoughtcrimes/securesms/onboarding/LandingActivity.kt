package com.thoughtcrimes.securesms.onboarding

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.permissions.Permissions
import com.thoughtcrimes.securesms.service.KeyCachingService
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.util.push
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityLandingBinding

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TextSecurePreferences.setCopiedSeed(this, false)
        with(binding) {
            fakeChatView.startAnimating()
            registerButton.setOnClickListener() { register() }
            restoreButton.setOnClickListener { restore() }
            TermsandCondtionsTxt.setOnClickListener { link() }
            val isDayUiMode = UiModeUtilities.isDayUiMode(this@LandingActivity)
            (if (isDayUiMode) R.raw.landing_animation_light_theme else R.raw.landing_animation_dark_theme).also {
                img.setAnimation(
                    it
                )
            }
        }
        IdentityKeyUtil.generateIdentityKeyPair(this)
        TextSecurePreferences.setPasswordDisabled(this, true)
        // AC: This is a temporary workaround to trick the old code that the screen is unlocked.
        KeyCachingService.setMasterSecret(applicationContext, Object())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !(getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager).areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            enableNotification()
        }
    }

    private fun enableNotification() {
        val dialog = AlertDialog.Builder(this)
        val li = LayoutInflater.from(dialog.context)
        val promptsView = li.inflate(R.layout.alert_notification_enable, null)

        dialog.setView(promptsView)
        val enable = promptsView.findViewById<Button>(R.id.enableButton)
        val alertDialog: AlertDialog = dialog.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        enable.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !(getSystemService(
                            NOTIFICATION_SERVICE
                    ) as NotificationManager).areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                Permissions.with(this)
                        .request(Manifest.permission.POST_NOTIFICATIONS)
                        .execute()
            }
            alertDialog.dismiss()
        }
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
        val intent = Intent(this, RecoveryPhraseRestoreActivity::class.java)
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
package io.beldex.bchat.onboarding

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.onboarding.ui.LandingScreen
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.util.nodelistasync.DownloadNodeListFileAsyncTask
import io.beldex.bchat.util.nodelistasync.NodeListConstants
import io.beldex.bchat.util.push

class LandingActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        TextSecurePreferences.setCopiedSeed(this, false)

        setContent {
            BChatTheme {
                LandingScreen(
                    onCreateAccountClick = { register() },
                    onRestoreAccountClick = { restore() },
                    onTermsClick = { link() }
                )
            }
        }

        IdentityKeyUtil.generateIdentityKeyPair(this)
        TextSecurePreferences.setPasswordDisabled(this, true)
        // AC: This is a temporary workaround to trick the old code that the screen is unlocked.
        KeyCachingService.setMasterSecret(applicationContext, Object())

        if (!(getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager).areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val async = DownloadNodeListFileAsyncTask(this)
        async.execute<String>(NodeListConstants.downloadNodeListUrl)
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
            if (!(getSystemService(
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
        val intent = Intent(this, RecoveryPhraseRestoreActivity::class.java)
        push(intent)
        finish()
    }

    private fun link() {
        try {
            val viewIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://bchat.beldex.io/terms-and-conditions")
            )
            startActivity(viewIntent)
        } catch(ex: ActivityNotFoundException) {
            Log.d("LandingActivity",ex.message.toString())
        }
    }
}

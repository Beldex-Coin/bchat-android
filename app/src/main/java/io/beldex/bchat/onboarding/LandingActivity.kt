package io.beldex.bchat.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.WindowCompat
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.IconConfirmDialog
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.onboarding.ui.LandingScreen
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.util.nodelistasync.DownloadNodeListFileAsyncTask
import io.beldex.bchat.util.nodelistasync.NodeListConstants
import io.beldex.bchat.util.push

class LandingActivity : BaseComponentActivity() {

    private var showEnableNotification by mutableStateOf(false)

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
                if (showEnableNotification) {
                    IconConfirmDialog(
                        icon = painterResource(id = R.drawable.ic_notification_outline),
                        title = stringResource(id = R.string.activity_settings_notifications_button_title),
                        message = AnnotatedString(stringResource(id = R.string.notification_enable_content)),
                        cancelText = stringResource(id = R.string.cancel),
                        confirmText = stringResource(id = R.string.dialog_link_preview_enable_button_title),
                        onCancel = { showEnableNotification = false },
                        onConfirm = {
                            showEnableNotification = false
                            requestNotificationPermission()
                        }
                    )
                }
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
        showEnableNotification = true
    }

    private fun requestNotificationPermission() {
        if (!(getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager).areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            Permissions.with(this)
                .request(Manifest.permission.POST_NOTIFICATIONS)
                .execute()
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

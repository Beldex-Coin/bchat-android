package com.thoughtcrimes.securesms.my_account.ui

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.my_account.ui.dialogs.LockOptionsDialog
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeAction
import com.thoughtcrimes.securesms.service.KeyCachingService
import io.beldex.bchat.R
import java.util.concurrent.TimeUnit

@Composable
fun AppLockScreen() {
    val context = LocalContext.current
    val changePin: () -> Unit  = {
        val intent = Intent(Intent.ACTION_VIEW, "onboarding://manage_pin?finish=true&action=${PinCodeAction.ChangePinCode.action}".toUri())
        context.startActivity(intent)
    }
    var showLockOptionsDialog by remember {
        mutableStateOf(false)
    }
    val lockOptions = remember {
        ScreenTimeoutOptions.entries.map { it.displayValue }.toList()
    }
    var selectedLockOptions by remember {
        mutableStateOf(IdentityKeyUtil.retrieve(
            context,
            IdentityKeyUtil.SCREEN_TIMEOUT_VALUES_KEY
        ) ?: "None")
    }
    val onLockTimerChanged: (String, Int) -> Unit = { value, index ->
        showLockOptionsDialog = false
        IdentityKeyUtil.save(context, IdentityKeyUtil.SCREEN_TIMEOUT_KEY, index.toString())
        IdentityKeyUtil.save(context, IdentityKeyUtil.SCREEN_TIMEOUT_VALUES_KEY, value)
        selectedLockOptions = value

        TextSecurePreferences.setScreenLockEnabled(context, true)

        val intent = Intent(context, KeyCachingService::class.java)
        intent.action = KeyCachingService.LOCK_TOGGLED_EVENT
        context.startService(intent)

        when (value) {
            "None" -> {
                TextSecurePreferences.setScreenLockTimeout(context, 950400)
            }
            "30 Seconds" -> {
                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(30000)
                TextSecurePreferences.setScreenLockTimeout(context, timeoutSeconds)
            }
            "1 Minute" -> {
                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(60000)
                TextSecurePreferences.setScreenLockTimeout(context, timeoutSeconds)
            }
            "2 Minutes" -> {
                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(120000)
                TextSecurePreferences.setScreenLockTimeout(context, timeoutSeconds)
            }
            "5 Minutes" -> {
                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(300000)
                TextSecurePreferences.setScreenLockTimeout(context, timeoutSeconds)
            }
            "15 Minutes" -> {
                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(900000)
                TextSecurePreferences.setScreenLockTimeout(context, timeoutSeconds)
            }
            "30 Minutes" -> {
                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(1800000)
                TextSecurePreferences.setScreenLockTimeout(context, timeoutSeconds)
            }
            else -> Unit
        }

    }
    if (showLockOptionsDialog) {
        LockOptionsDialog(
            title = stringResource(R.string.screen_inactivity_timeout),
            options = lockOptions,
            currentValue = selectedLockOptions,
            onDismiss = {
                showLockOptionsDialog = false
            },
            onValueChanged = { value, index ->
                onLockTimerChanged(value, index)
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    changePin()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_change_password),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.iconTint
                )

                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = stringResource(id = R.string.change_password),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showLockOptionsDialog = true
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_screen_lock),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.iconTint
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.screenlock_inactivity_timeout),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = selectedLockOptions,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.appColors.lockTimerColor
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun AppLockScreenPreview() {
    BChatTheme {
        AppLockScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AppLockScreenPreviewDark() {
    BChatTheme {
        AppLockScreen()
    }
}
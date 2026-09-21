package io.beldex.bchat.my_account.ui

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
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.my_account.ui.dialogs.LockOptionsDialog
import io.beldex.bchat.onboarding.ui.PinCodeAction
import io.beldex.bchat.service.KeyCachingService
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
    val lockOptions = ScreenTimeoutOptions.entries
    var selectedLockOption by remember {
        mutableStateOf(
            lockOptions.getOrNull(
                IdentityKeyUtil.retrieve(
                    context,
                    IdentityKeyUtil.SCREEN_TIMEOUT_KEY
                )?.toIntOrNull() ?: 0
            ) ?: ScreenTimeoutOptions.None
        )
    }

    val noTimeoutSeconds = 950400L

    val onLockTimerChanged: (ScreenTimeoutOptions, Int) -> Unit = { option, index ->
        showLockOptionsDialog = false

        IdentityKeyUtil.save(context, IdentityKeyUtil.SCREEN_TIMEOUT_KEY, index.toString())

        selectedLockOption = option

        TextSecurePreferences.setScreenLockEnabled(context, true)

        val intent = Intent(context, KeyCachingService::class.java)
        intent.action = KeyCachingService.LOCK_TOGGLED_EVENT
        context.startService(intent)

        val timeoutSeconds =
            if (option == ScreenTimeoutOptions.None) {
                noTimeoutSeconds
            } else {
                TimeUnit.MILLISECONDS.toSeconds(option.timeoutMillis)
            }

        TextSecurePreferences.setScreenLockTimeout(
            context,
            timeoutSeconds
        )

    }
    if (showLockOptionsDialog) {
        LockOptionsDialog(
            title = stringResource(R.string.screen_inactivity_timeout),

            options = lockOptions.map {
                stringResource(it.labelRes)
            },

            currentValue = stringResource(selectedLockOption.labelRes),

            onDismiss = {
                showLockOptionsDialog = false
            },

            onValueChanged = { value, index ->
                lockOptions.getOrNull(index)?.let { option ->
                    onLockTimerChanged(option, index)
                }
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
                defaultElevation = 0.dp
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
                        color = MaterialTheme.appColors.editTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
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
                defaultElevation = 0.dp
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
                    contentDescription = null,
                    tint = MaterialTheme.appColors.iconTint
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.screenlock_inactivity_timeout),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.appColors.editTextColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    )

                    Text(
                        text = stringResource(selectedLockOption.labelRes),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.appColors.lockTimerColor,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
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
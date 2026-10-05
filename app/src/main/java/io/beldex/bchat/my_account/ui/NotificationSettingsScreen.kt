package io.beldex.bchat.my_account.ui

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.my_account.ui.dialogs.LockOptionsDialog
import io.beldex.bchat.notifications.NotificationChannels
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Revamp_2026 Notification Settings screen (Figma `Notification_Settings` 7546:5356), hosted by
 * [MyAccountActivity] via [MyAccountScreens.NotificationSettingsScreen]. Mirrors the toggle/data
 * backed by [TextSecurePreferences] that the legacy `NotificationsPreferenceFragment` /
 * `preferences_notifications.xml` managed, since that screen is being retired in favor of this one.
 */
@Composable
fun NotificationSettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var notificationsEnabled by remember {
        mutableStateOf(TextSecurePreferences.isNotificationsEnabled(context))
    }
    var fastModeEnabled by remember {
        mutableStateOf(TextSecurePreferences.isPushEnabled(context))
    }
    var vibrateEnabled by remember {
        mutableStateOf(TextSecurePreferences.isNotificationVibrateEnabled(context))
    }
    var inChatSoundsEnabled by remember {
        mutableStateOf(TextSecurePreferences.isInThreadNotifications(context))
    }
    var ringtoneUri by remember {
        mutableStateOf(TextSecurePreferences.getNotificationRingtone(context))
    }
    val ringtoneTitle = remember(ringtoneUri) {
        if (ringtoneUri.toString().isEmpty()) {
            null
        } else {
            RingtoneManager.getRingtone(context, ringtoneUri)?.getTitle(context)
        }
    }

    val privacyValues = stringArrayResource(R.array.pref_notification_privacy_values)
    val privacyEntries = stringArrayResource(R.array.pref_notification_privacy_entries)
    var privacyValue by remember {
        mutableStateOf(
            TextSecurePreferences.getStringPreference(context, TextSecurePreferences.NOTIFICATION_PRIVACY_PREF, "all")
                ?: "all"
        )
    }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri == Settings.System.DEFAULT_NOTIFICATION_URI) {
                NotificationChannels.updateMessageRingtone(context, uri)
                TextSecurePreferences.removeNotificationRingtone(context)
                ringtoneUri = TextSecurePreferences.getNotificationRingtone(context)
            } else {
                val resolved = uri ?: Uri.EMPTY
                NotificationChannels.updateMessageRingtone(context, resolved)
                TextSecurePreferences.setNotificationRingtone(context, resolved.toString())
                ringtoneUri = resolved
            }
        }
    }

    if (showPrivacyDialog) {
        LockOptionsDialog(
            title = stringResource(id = R.string.preferences_notifications__show),
            options = privacyEntries.toList(),
            currentValue = privacyEntries.getOrElse(privacyValues.indexOf(privacyValue)) { privacyEntries[0] },
            onDismiss = {
                showPrivacyDialog = false
            },
            onValueChanged = { _, index ->
                showPrivacyDialog = false
                val newValue = privacyValues.getOrElse(index) { "all" }
                privacyValue = newValue
                TextSecurePreferences.setStringPreference(context, TextSecurePreferences.NOTIFICATION_PRIVACY_PREF, newValue)
            }
        )
    }

    Column(
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, MaterialTheme.appColors.dividerColor),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences__notifications),
                    settingIcon = painterResource(id = R.drawable.ic_all_nodification_setting),
                    isEnabled = notificationsEnabled,
                    onSwitchChanged = { checked ->
                        notificationsEnabled = checked
                        TextSecurePreferences.setNotificationsEnabled(context, checked)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences_notifications__priority),
                    settingIcon = painterResource(id = R.drawable.ic_priority_setting),
                    containsSwitch = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            intent.putExtra(
                                Settings.EXTRA_CHANNEL_ID,
                                NotificationChannels.getMessagesChannel(context)
                            )
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.preferences_notifications_strategy_category_title),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.appColors.textGreen,
                fontFamily = OpenSans,
                fontWeight = FontWeight(600),
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, MaterialTheme.appColors.dividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences_notifications_strategy_category_fast_mode_title),
                    settingIcon = painterResource(id = R.drawable.ic_fast_mode_setting),
                    isEnabled = fastModeEnabled,
                    settingDesc = stringResource(id = R.string.preferences_notifications_strategy_category_fast_mode_summary),
                    onSwitchChanged = { checked ->
                        fastModeEnabled = checked
                        TextSecurePreferences.setPushEnabled(context, checked)
                        val job = ApplicationContext.getInstance(context).pushRegistry.refresh(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            job.join()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.activity_notification_settings_style_section_title),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.appColors.textGreen,
                fontFamily = OpenSans,
                fontWeight = FontWeight(600),
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, MaterialTheme.appColors.dividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences__sound),
                    settingIcon = painterResource(id = R.drawable.ic_sound_setting),
                    containsSwitch = false,
                    trailingText = ringtoneTitle ?: stringResource(id = R.string.preferences__silent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            intent.putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_NOTIFICATION
                            )
                            intent.putExtra(
                                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                Settings.System.DEFAULT_NOTIFICATION_URI
                            )
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
                            ringtoneLauncher.launch(intent)
                        },
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences__vibrate),
                    settingIcon = painterResource(id = R.drawable.ic_vibrate_setting),
                    isEnabled = vibrateEnabled,
                    onSwitchChanged = { checked ->
                        vibrateEnabled = checked
                        TextSecurePreferences.setNotificationVibrateEnabled(context, checked)
                        NotificationChannels.updateMessageVibrate(context, checked)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences_notifications__in_chat_sounds),
                    settingIcon = painterResource(id = R.drawable.ic_chat_sound_setting),
                    isEnabled = inChatSoundsEnabled,
                    onSwitchChanged = { checked ->
                        inChatSoundsEnabled = checked
                        TextSecurePreferences.setBooleanPreference(
                            context,
                            TextSecurePreferences.IN_THREAD_NOTIFICATION_PREF,
                            checked
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.activity_notification_settings_content_section_title),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.appColors.textGreen,
                fontFamily = OpenSans,
                fontWeight = FontWeight(600),
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, MaterialTheme.appColors.dividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences_notifications__show),
                    settingIcon = painterResource(id = R.drawable.ic_show_setting),
                    containsSwitch = false,
                    trailingText = privacyEntries.getOrElse(privacyValues.indexOf(privacyValue)) { privacyEntries[0] },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPrivacyDialog = true
                        },
                )
            }
        }
    }
}

@Preview
@Composable
fun NotificationSettingsScreenPreview() {
    BChatTheme {
        NotificationSettingsScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NotificationSettingsScreenPreviewDark() {
    BChatTheme {
        NotificationSettingsScreen()
    }
}

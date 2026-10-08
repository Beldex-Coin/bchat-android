package io.beldex.bchat.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.ScreenSecurity
import android.app.Activity
import androidx.compose.foundation.clickable
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape

/**
 * Revamp_2026 Privacy / App Protection settings (Figma `Settings` 7546:23529), reached from the
 * drawer's Settings row. Covers the toggles that have real backing preferences; the Figma rows
 * "Screen Security", "Clear conversation History" and the onion-routing hop count have no existing
 * functionality in the app and are intentionally not rendered.
 */
@Composable
fun PrivacySettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var incognitoKeyboard by remember { mutableStateOf(TextSecurePreferences.isIncognitoKeyboardEnabled(context)) }
    var readReceipts by remember { mutableStateOf(TextSecurePreferences.isReadReceiptsEnabled(context)) }
    var typingIndicators by remember { mutableStateOf(TextSecurePreferences.isTypingIndicatorsEnabled(context)) }
    var linkPreviews by remember { mutableStateOf(TextSecurePreferences.isLinkPreviewsEnabled(context)) }
    var voiceVideoCalls by remember { mutableStateOf(TextSecurePreferences.isCallNotificationsEnabled(context)) }
    var onionRouting by remember { mutableStateOf(TextSecurePreferences.isOnionRoutingEnabled(context)) }
    var screenSecurity by remember { mutableStateOf(TextSecurePreferences.isScreenSecurityEnabled(context)) }
    var showOnionConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    if (showOnionConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.preferences__onion_routing),
            message = stringResource(R.string.onion_routing_turn_off_confirmation),
            onConfirm = {
                showOnionConfirm = false
                onionRouting = false
                TextSecurePreferences.setOnionRoutingEnabled(context, false)
            },
            onCancel = {
                showOnionConfirm = false
            }
        )
    }

    if (showClearHistoryConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.preferences__clear_conversation_history),
            message = stringResource(R.string.clear_all_conversation_history_confirmation),
            onConfirm = {
                showClearHistoryConfirm = false
                ApplicationContext.getInstance(context).let { app ->
                    Thread {
                        DatabaseComponent.get(app).threadDatabase().trimAllThreads(0) { _, _ -> }
                    }.start()
                }
            },
            onCancel = {
                showClearHistoryConfirm = false
            }
        )
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SectionTitle(stringResource(R.string.preferences_app_protection__app_access))
        SectionCard {
            SettingsItem(
                settingTitle = stringResource(R.string.preferences__screen_security),
                settingIcon = painterResource(id = R.drawable.ic_screen_lock),
                settingDesc = stringResource(R.string.preferences__disable_screen_security_to_allow_screen_shots),
                isEnabled = screenSecurity,
                onSwitchChanged = { checked ->
                    screenSecurity = checked
                    TextSecurePreferences.setScreenSecurityEnabled(context, checked)
                    (context as? Activity)?.let { ScreenSecurity.apply(it) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                settingTitle = stringResource(R.string.preferences__incognito_keyboard),
                settingIcon = painterResource(id = R.drawable.ic_incognito_keyboard),
                isEnabled = incognitoKeyboard,
                onSwitchChanged = { checked ->
                    incognitoKeyboard = checked
                    TextSecurePreferences.setBooleanPreference(
                        context,
                        TextSecurePreferences.INCOGNITO_KEYBORAD_PREF,
                        checked
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle(stringResource(R.string.preferences_app_protection__communication))
        SectionCard {
            SettingsItem(
                settingTitle = stringResource(R.string.preferences__read_receipts),
                settingIcon = painterResource(id = R.drawable.ic_read_receipet_setting),
                settingDesc = stringResource(R.string.preferences__if_read_receipts_are_disabled_you_wont_be_able_to_see_read_receipts),
                isEnabled = readReceipts,
                onSwitchChanged = { checked ->
                    readReceipts = checked
                    TextSecurePreferences.setReadReceiptsEnabled(context, checked)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                settingTitle = stringResource(R.string.preferences__typing_indicators),
                settingIcon = painterResource(id = R.drawable.ic_type_indicater_setting),
                settingDesc = stringResource(R.string.preferences__if_typing_indicators_are_disabled_you_wont_be_able_to_see_typing_indicators),
                isEnabled = typingIndicators,
                onSwitchChanged = { checked ->
                    typingIndicators = checked
                    TextSecurePreferences.setTypingIndicatorsEnabled(context, checked)
                    if (!checked) {
                        ApplicationContext.getInstance(context).typingStatusRepository.clear()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                settingTitle = stringResource(R.string.preferences__send_link_previews),
                settingIcon = painterResource(id = R.drawable.ic_send_link_setting),
                isEnabled = linkPreviews,
                onSwitchChanged = { checked ->
                    linkPreviews = checked
                    TextSecurePreferences.setLinkPreviewsEnabled(context, checked)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                settingTitle = stringResource(R.string.preferences__voice_video_calls),
                settingIcon = painterResource(id = R.drawable.ic_video_call_setting),
                isEnabled = voiceVideoCalls,
                onSwitchChanged = { checked ->
                    voiceVideoCalls = checked
                    TextSecurePreferences.setBooleanPreference(
                        context,
                        TextSecurePreferences.CALL_NOTIFICATIONS_ENABLED,
                        checked
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                settingTitle = stringResource(R.string.preferences__clear_conversation_history),
                settingIcon = painterResource(id = R.drawable.ic_clear_data),
                containsSwitch = false,
                onSwitchChanged = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearHistoryConfirm = true },
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                settingTitle = stringResource(R.string.preferences__onion_routing),
                settingIcon = painterResource(id = R.drawable.ic_onion_routing),
                isEnabled = onionRouting,
                onSwitchChanged = { checked ->
                    if (checked) {
                        onionRouting = true
                        TextSecurePreferences.setOnionRoutingEnabled(context, true)
                    } else {
                        showOnionConfirm = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.appColors.editTextColor,
            fontFamily = OpenSans,
            fontWeight = FontWeight(600),
            fontSize = 16.sp
        ),
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.appColors.dividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onCancel,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                onClick = onConfirm,
                shape = notchedCornerShape(12.dp),
                containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.ok),
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryButton(
                onClick = onCancel,
                shape = notchedCornerShape(12.dp),
                containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground,
                contentColor = MaterialTheme.appColors.onboardingSecondaryButtonText,
                border = BorderStroke(1.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PrivacySettingsScreenPreview() {
    BChatTheme {
        PrivacySettingsScreen()
    }
}

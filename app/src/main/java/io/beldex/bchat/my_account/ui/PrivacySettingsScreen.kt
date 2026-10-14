package io.beldex.bchat.my_account.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import android.Manifest
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
import io.beldex.bchat.compose_utils.DialogConfirmButton
import io.beldex.bchat.compose_utils.DialogCancelButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.permissions.Permissions

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
    var showHopsDialog by remember { mutableStateOf(false) }
    val sharedPrefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    var hops by remember {
        mutableStateOf(
            sharedPrefs.getInt("pref_onion_hops", if (TextSecurePreferences.isOnionRoutingEnabled(context)) 3 else 0)
        )
    }
    var showVoiceVideoConfirm by remember { mutableStateOf(false) }

    if (showHopsDialog) {
        HopsDialog(
            selected = hops,
            onSelect = { value ->
                hops = value
                sharedPrefs.edit().putInt("pref_onion_hops", value).apply()
                // No Hops = direct delivery; 1 or 3 hops = onion routing.
                TextSecurePreferences.setOnionRoutingEnabled(context, value != 0)
                showHopsDialog = false
            },
            onCancel = { showHopsDialog = false }
        )
    }

    if (showVoiceVideoConfirm) {
        io.beldex.bchat.compose_utils.IconConfirmDialog(
            icon = painterResource(id = R.drawable.ic_headset_popup),
            title = stringResource(R.string.dialog_voice_video_title),
            message = androidx.compose.ui.text.AnnotatedString(stringResource(R.string.dialog_voice_video_message)),
            cancelText = stringResource(R.string.cancel),
            confirmText = stringResource(R.string.dialog_voice_video_enable),
            onConfirm = {
                showVoiceVideoConfirm = false
                Permissions.with(context as Activity)
                    .request(Manifest.permission.RECORD_AUDIO)
                    .onAllGranted {
                        voiceVideoCalls = true
                        TextSecurePreferences.setBooleanPreference(
                            context,
                            TextSecurePreferences.CALL_NOTIFICATIONS_ENABLED,
                            true
                        )
                    }
                    .onAnyDenied { voiceVideoCalls = false }
                    .execute()
            },
            onCancel = {
                showVoiceVideoConfirm = false
                voiceVideoCalls = false
            }
        )
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SectionTitle(stringResource(R.string.preferences_app_protection__app_access))
        SectionCard {
            SettingsItem(
                settingTitle = stringResource(R.string.preferences__incognito_keyboard),
                settingIcon = painterResource(id = R.drawable.ic_incognito_keyboard),
                settingDesc = stringResource(R.string.preferences__request_keyboard_to_disable_personalized_learning),
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
                settingDesc = stringResource(R.string.preferences__previews_are_supported_for),
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
                settingDesc = stringResource(R.string.preferences__allow_access_voice_video),
                isEnabled = voiceVideoCalls,
                onSwitchChanged = { checked ->
                    if (checked) {
                        showVoiceVideoConfirm = true
                    } else {
                        voiceVideoCalls = false
                        TextSecurePreferences.setBooleanPreference(
                            context,
                            TextSecurePreferences.CALL_NOTIFICATIONS_ENABLED,
                            false
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                settingTitle = stringResource(R.string.preferences__onion_routing),
                settingIcon = painterResource(id = R.drawable.ic_onion_routing),
                containsSwitch = false,
                trailingText = hopsLabel(hops),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHopsDialog = true },
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
        shape = androidx.compose.ui.graphics.RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder),
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
    onCancel: () -> Unit,
    destructive: Boolean = false
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onCancel,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 18.dp)) {
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
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogCancelButton(
                    text = stringResource(R.string.cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                DialogConfirmButton(
                    text = stringResource(R.string.ok),
                    onClick = onConfirm,
                    destructive = destructive,
                    modifier = Modifier.weight(1f)
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


@Composable
private fun hopsLabel(hops: Int): String = when (hops) {
    0 -> stringResource(R.string.hops_none)
    1 -> stringResource(R.string.hops_one)
    else -> stringResource(R.string.hops_three)
}

/** Figma "Hops" popup: pick how many intermediary nodes messages are routed through. */
@Composable
private fun HopsDialog(selected: Int, onSelect: (Int) -> Unit, onCancel: () -> Unit) {
    val options = listOf(0, 1, 3)
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onCancel
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 18.dp)) {
            Text(
                text = stringResource(R.string.preferences__onion_routing),
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEach { option ->
                val isSelected = option == selected
                val shape = notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium)
                val accent = MaterialTheme.appColors.userDetailsConfirmBackground
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .height(56.dp)
                        .background(
                            if (isSelected) MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground else MaterialTheme.appColors.backgroundColor,
                            shape
                        )
                        .border(1.dp, if (isSelected) accent else MaterialTheme.appColors.dividerColor, shape)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 20.dp)
                ) {
                    io.beldex.bchat.compose_utils.BChatRadioButton(selected = isSelected, onClick = { onSelect(option) })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = hopsLabel(option),
                        color = if (isSelected) accent else MaterialTheme.appColors.onboardingBodyColor,
                        fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                        fontSize = 16.sp
                    )
                }
            }
            Text(
                text = stringResource(
                    when (selected) {
                        0 -> R.string.hops_none_description
                        1 -> R.string.hops_one_description
                        else -> R.string.hops_three_description
                    }
                ),
                color = MaterialTheme.appColors.onboardingBodyColor,
                fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            DialogCancelButton(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                textColor = MaterialTheme.appColors.editTextColor,
                modifier = Modifier.width(162.dp).align(androidx.compose.ui.Alignment.CenterHorizontally)
            )
        }
    }
}

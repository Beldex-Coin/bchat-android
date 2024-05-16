package com.thoughtcrimes.securesms.my_account.ui

import android.content.res.Configuration
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun ChatSettingsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.chat),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.appColors.primaryButtonColor
            ),
            modifier = Modifier
                .padding(16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.settingsCardBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences__pref_enter_sends_title),
                    settingIcon = painterResource(id = R.drawable.ic_enter_key),
                    isEnabled = true,
                    onSwitchChanged = {},
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsItem(
                    settingTitle = stringResource(id = R.string.preferences_chats__font_size),
                    painterResource(id = R.drawable.ic_fonts),
                    containsSwitch = false,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Message Timing",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.appColors.primaryButtonColor
            ),
            modifier = Modifier
                .padding(16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.settingsCardBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SettingsItem(
                    settingTitle = "Delete Old Messages",
                    settingIcon = painterResource(id = R.drawable.ic_delete_old),
                    isEnabled = false,
                    onSwitchChanged = {},
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsItem(
                    settingTitle = "Conversation Length Limit",
                    painterResource(id = R.drawable.ic_conversation_length),
                    containsSwitch = false,
                    settingDesc = "500 messages per conversation",
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingsItem(
                    settingTitle = "Trim all conversations now",
                    painterResource(id = R.drawable.ic_trim_conversation),
                    containsSwitch = false,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
fun ChatSettingsScreenPreview() {
    BChatTheme {
        ChatSettingsScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChatSettingsScreenPreviewDark() {
    BChatTheme {
        ChatSettingsScreen()
    }
}
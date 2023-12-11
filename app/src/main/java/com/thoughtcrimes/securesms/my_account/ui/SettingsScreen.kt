package com.thoughtcrimes.securesms.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

enum class SettingItem(val title: String) {
    Hops("Hops"),
    AppLock("App Lock"),
    ChatSettings("Chat Settings"),
    BlockedContacts("Blocked Contacts"),
    ClearData("Clear Data"),
    Feedback("Feedback"),
    FAQ("FAQ"),
    ChangeLog("Changelog")
}

@Composable
fun SettingsScreen(
    navigate: (SettingItem) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
        SettingItem.values().forEach { item ->
            MyAccountItem(
                title = item.title,
                icon = when (item) {
                    SettingItem.Hops -> painterResource(id = R.drawable.ic_hops)
                    SettingItem.AppLock -> painterResource(id = R.drawable.ic_app_lock)
                    SettingItem.ChatSettings -> painterResource(id = R.drawable.ic_chat_settings)
                    SettingItem.BlockedContacts -> painterResource(id = R.drawable.ic_blocked_contacts)
                    SettingItem.ClearData -> painterResource(id = R.drawable.ic_clear_data)
                    SettingItem.Feedback -> painterResource(id = R.drawable.ic_feedback)
                    SettingItem.FAQ -> painterResource(id = R.drawable.ic_faq)
                    SettingItem.ChangeLog -> painterResource(id = R.drawable.ic_changelog)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 16.dp,
                        horizontal = 24.dp
                    )
                    .clickable {
                        navigate(item)
                    }
            )
        }
    }
}

@Composable
private fun MyAccountItem(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        Icon(
            painter = icon,
            contentDescription = "",
            tint = MaterialTheme.appColors.editTextColor
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = BChatTypography.titleMedium.copy(
                color = MaterialTheme.appColors.editTextColor
            ),
            modifier = Modifier
                .weight(0.7f)
        )

        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = "",
            tint = MaterialTheme.appColors.editTextColor
        )
    }
}

@Preview
@Composable
fun SettingScreenPreview() {
    SettingsScreen(navigate = {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingScreenPreviewDark() {
    SettingsScreen(navigate = {})
}
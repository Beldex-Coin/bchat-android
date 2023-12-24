package com.thoughtcrimes.securesms.my_account.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun SettingsItem(
    settingTitle: String,
    settingIcon: Painter,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = false,
    containsSwitch: Boolean = true,
    settingDesc: String? = null,
    onSwitchChanged: (Boolean) -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = settingIcon,
            contentDescription = "",
            tint = MaterialTheme.appColors.iconTint
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = settingTitle,
                style = MaterialTheme.typography.bodyMedium,
            )

            settingDesc?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xACACACAC)
                    ),
                )
            }
        }

        if (containsSwitch) {
            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onSwitchChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.appColors.primaryButtonColor,
                    uncheckedThumbColor = MaterialTheme.appColors.unCheckedSwitchThumb,
                    checkedTrackColor = MaterialTheme.appColors.switchTrackColor,
                    uncheckedTrackColor = MaterialTheme.appColors.switchTrackColor
                )
            )
        }
    }
}
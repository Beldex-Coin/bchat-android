package io.beldex.bchat.my_account.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.home.PathStatusView

enum class SettingItem(val title: Int) {
    Hops(R.string.activity_path_title),
    AppLock(R.string.activity_settings_app_lock_button_title),
    ChatSettings(R.string.preferences_chats__chats),
    BlockedContacts(R.string.blocked_contacts),
    ClearData(R.string.activity_settings_clear_all_data_button_title),
    Feedback(R.string.activity_settings_survey_feedback),
    FAQ(R.string.activity_settings_faq_button_title),
    ChangeLog(R.string.changelog)
}

@Composable
fun SettingsScreen(
    navigate: (SettingItem) -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        SettingItem.entries.forEach { item ->
            MyAccountItem(
                title = stringResource(id = item.title),
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
                drawDot = when (item) {
                    SettingItem.Hops -> true
                    else -> false
                },
                    expendArrow=when (item) {
                        SettingItem.Hops -> true
                        SettingItem.AppLock -> true
                        SettingItem.ChatSettings -> true
                        SettingItem.BlockedContacts -> true
                        SettingItem.ClearData -> false
                        SettingItem.Feedback -> false
                        SettingItem.ChangeLog -> true
                        SettingItem.FAQ -> false
                    },
                modifier =Modifier
                        .fillMaxWidth()
                        .padding(
                                vertical=16.dp,
                                horizontal=24.dp
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
    drawDot: Boolean,
    expendArrow:Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
    ) {
        Icon(
                painter=icon,
                contentDescription="",
                tint=MaterialTheme.appColors.editTextColor
        )

        Spacer(modifier=Modifier.width(16.dp))

        Row(
                verticalAlignment=Alignment.CenterVertically,
                modifier=Modifier
                        .weight(0.7f)
        ) {
            Text(
                    text=title,
                    style=BChatTypography.titleMedium.copy(
                            color=MaterialTheme.appColors.editTextColor
                    )
            )
            if (drawDot) {
                Spacer(modifier=Modifier.width(8.dp))

                AndroidView(modifier=Modifier
                        .size(8.dp), factory={
                    PathStatusView(context=context)
                })
            }
        }
        if (expendArrow) {
            Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription="",
                    tint=MaterialTheme.appColors.editTextColor
            )
        }
    }
}

/*
@Preview
@Composable
fun SettingScreenPreview() {
    SettingsScreen(navigate = {}, viewModel = MyAccountViewModel)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingScreenPreviewDark() {
    SettingsScreen(navigate = {}, viewModel = MyAccountViewModel)
}*/

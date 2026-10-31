package io.beldex.bchat.conversation.v2.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.appColors

/**
 * The chat screen header's call + overflow icons (Figma `chat_menu` 7546:10498), replacing the
 * system-rendered ActionBar call action item + auto overflow icon this app used to show (see
 * [ConversationMenuHelper.onPrepareOptionsMenu]'s now-empty body). Same `DropdownMenu` pattern
 * already used and verified for Home's long-press menu (`ConversationListView.kt`), so this
 * reuses that component and its `appColors.editTextColor` item-text token rather than inventing a
 * new one — its hex (#EBEBEB dark) is an exact match to this menu's Figma spec too.
 */
@Composable
fun ConversationHeaderActions(
    showCallIcon: Boolean,
    expirationLabel: String? = null,
    onExpirationClick: () -> Unit = {},
    menuEntries: List<ConversationMenuEntry>,
    onCallClick: () -> Unit,
    onMenuOpen: () -> Unit = {},
    onMenuItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (expirationLabel != null) {
            IconButton(onClick = onExpirationClick) {
                Box(
                    modifier = Modifier.size(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_timer_new),
                        contentDescription = stringResource(id = R.string.disappearing_messages),
                        tint = MaterialTheme.appColors.editTextColor,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopCenter)
                    )

                    Text(
                        text = expirationLabel,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 8.sp,
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .background(
                                color = MaterialTheme.appColors.backgroundColor,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (showCallIcon) {
            IconButton(onClick = onCallClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_call_new),
                    contentDescription = stringResource(id = R.string.conversation_context__menu_call),
                    tint = MaterialTheme.appColors.editTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box {
            IconButton(onClick = { onMenuOpen(); showMenu = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu_dots),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.editTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                containerColor = androidx.compose.ui.graphics.Color(0xF2111111),
                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0x33FFFFFF))
            ) {
                for (entry in menuEntries) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = entry.label,
                                fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                                fontSize = 14.sp,
                                color = MaterialTheme.appColors.editTextColor
                            )
                        },
                        onClick = {
                            showMenu = false
                            onMenuItemClick(entry.id)
                        }
                    )
                }
            }
        }
    }
}

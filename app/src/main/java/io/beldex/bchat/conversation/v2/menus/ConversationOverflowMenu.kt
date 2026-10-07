package io.beldex.bchat.conversation.v2.menus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.appColors

/**
 * The chat screen header's call + overflow icons (Figma `chat_menu` 7546:10498), replacing the
 * system-rendered ActionBar call action item + auto overflow icon this app used to show (see
 * [ConversationMenuHelper.onPrepareOptionsMenu]'s now-empty body). Same `DropdownMenu` pattern
 * already used and verified for Home's long-press menu (`ConversationListView.kt`), so this
 * reuses that component and its `appColors.homeRowTitle` item-text token rather than inventing a
 * new one — its hex (#EBEBEB dark) is an exact match to this menu's Figma spec too.
 */
@Composable
fun ConversationHeaderActions(
    showCallIcon: Boolean,
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
        if (showCallIcon) {
            IconButton(onClick = onCallClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_call_new),
                    contentDescription = stringResource(id = R.string.conversation_context__menu_call),
                    tint = MaterialTheme.appColors.titleTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box {
            IconButton(onClick = { onMenuOpen(); showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.titleTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                for (entry in menuEntries) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = entry.label,
                                fontFamily = OpenSans,
                                fontSize = 14.sp,
                                color = MaterialTheme.appColors.homeRowTitle
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

package io.beldex.bchat.conversation.v2.menus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.RobotoMono

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
            // Figma chat header timer (7546:18384 / 7762:21257): 20dp stopwatch in a 22dp box, the
            // "5s" label in Open Sans Bold over its bottom-right with a thin halo in the header's
            // border colour (no filled box), icon and label #EBEBEB / #222222.
            val timerColor = colorResource(id = R.color.disappear_icon_color)
            val haloColor = colorResource(id = R.color.chat_header_border)
            val labelStyle = TextStyle(
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 9.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
            IconButton(onClick = onExpirationClick) {
                Box(modifier = Modifier.size(22.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_timer_new),
                        contentDescription = stringResource(id = R.string.disappearing_messages),
                        tint = timerColor,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 10.dp, y = 12.dp)
                            .wrapContentSize(unbounded = true)
                    ) {
                        Text(
                            text = expirationLabel,
                            maxLines = 1,
                            style = labelStyle.copy(color = haloColor, drawStyle = Stroke(width = 2.75f))
                        )
                        Text(
                            text = expirationLabel,
                            maxLines = 1,
                            style = labelStyle.copy(color = timerColor)
                        )
                    }
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
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.appColors.selectedItemBackground.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MaterialTheme.appColors.optionBorder)
            ) {
                for (entry in menuEntries) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = entry.label,
                                fontFamily = RobotoMono,
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

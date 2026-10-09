package io.beldex.bchat.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.bumptech.glide.RequestManager
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.ContextMenuItem
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.database.model.ThreadRecord

/**
 * The row's context menu actions (Figma has no spec for this — it's the existing
 * `menu_conversation_v2` long-press menu, ported 1:1). [HomeActivity] maps each action to its
 * existing handler methods (block/mute/pin/etc.) exactly as the old RecyclerView+MenuAdapter path
 * did.
 */
enum class ConversationRowAction {
    Details, Pin, Unpin, Block, Unblock, Archive, Mute, Unmute, NotificationSettings, MarkRead, Delete
}

private data class RowMenuItem(val action: ConversationRowAction, val labelRes: Int) {
    val iconRes: Int
        get() = when (action) {
            ConversationRowAction.Details -> R.drawable.ic_details_menu
            ConversationRowAction.Pin -> R.drawable.ic_pin_menu
            ConversationRowAction.Unpin -> R.drawable.ic_unpin
            ConversationRowAction.Block -> R.drawable.ic_block
            ConversationRowAction.Unblock -> R.drawable.ic_unblock
            ConversationRowAction.Archive -> R.drawable.ic_archive_chats
            ConversationRowAction.Mute -> R.drawable.ic_mute_notification_menu
            ConversationRowAction.Unmute -> R.drawable.ic_unmute_notification_menu
            ConversationRowAction.NotificationSettings -> R.drawable.ic_notification_settings_menu
            ConversationRowAction.MarkRead -> R.drawable.ic_mark_as_read_menu
            ConversationRowAction.Delete -> R.drawable.ic_delete_menu
        }
}

private fun menuItemsFor(thread: ThreadRecord, isSecretGroupActive: Boolean): List<RowMenuItem> {
    val recipient = thread.recipient
    val items = mutableListOf<RowMenuItem>()
    val showDetailsBlockUnblock = !recipient.isGroupRecipient && !recipient.isLocalNumber
    if (showDetailsBlockUnblock) {
        items += RowMenuItem(ConversationRowAction.Details, R.string.details)
    }
    items += if (thread.isPinned) {
        RowMenuItem(ConversationRowAction.Unpin, R.string.conversation_unpin)
    } else {
        RowMenuItem(ConversationRowAction.Pin, R.string.conversation_pin)
    }
    if (showDetailsBlockUnblock) {
        items += if (recipient.isBlocked) {
            RowMenuItem(ConversationRowAction.Unblock, R.string.ConversationActivity_unblock)
        } else {
            RowMenuItem(ConversationRowAction.Block, R.string.RecipientPreferenceActivity_block)
        }
    }
    items += RowMenuItem(ConversationRowAction.Archive, R.string.archive_chat_title)
    if (!recipient.isLocalNumber) {
        items += if (recipient.isMuted) {
            RowMenuItem(ConversationRowAction.Unmute, R.string.conversation_muted__unmute)
        } else {
            RowMenuItem(ConversationRowAction.Mute, R.string.MuteDialog_mute_notifications)
        }
    }
    if (recipient.isGroupRecipient && !recipient.isMuted && isSecretGroupActive) {
        items += RowMenuItem(ConversationRowAction.NotificationSettings, R.string.RecipientPreferenceActivity_notification_settings)
    }
    if (thread.unreadCount > 0) {
        items += RowMenuItem(ConversationRowAction.MarkRead, R.string.MessageNotifier_mark_all_as_read)
    }
    items += RowMenuItem(ConversationRowAction.Delete, R.string.delete)
    return items
}

/**
 * Revamp_2026 conversation list (Figma `home page` 7546:2060). Hosts the existing, proven
 * [ConversationView] (Android View) per row via [AndroidView] — its message-snippet formatting
 * (mentions, attachment labels, typing indicator) is intricate legacy logic not worth
 * reimplementing in Compose — while the list container, swipe-to-delete and long-press menu are
 * genuinely Compose (LazyColumn / SwipeToDismissBox / DropdownMenu).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationListView(
    conversations: List<ThreadRecord>,
    typingThreadIds: Set<Long>,
    glide: RequestManager,
    isSecretGroupActive: (ThreadRecord) -> Boolean,
    onClick: (ThreadRecord) -> Unit,
    onAction: (ThreadRecord, ConversationRowAction) -> Unit,
    onSwipeDelete: (ThreadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (conversations.isEmpty()) {
        EmptyConversationList(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items = conversations, key = { it.threadId }) { thread ->
            ConversationRow(
                thread = thread,
                isTyping = typingThreadIds.contains(thread.threadId),
                glide = glide,
                isSecretGroupActive = isSecretGroupActive(thread),
                onClick = { onClick(thread) },
                onAction = { action -> onAction(thread, action) },
                onSwipeDelete = { onSwipeDelete(thread) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    thread: ThreadRecord,
    isTyping: Boolean,
    glide: RequestManager,
    isSecretGroupActive: Boolean,
    onClick: () -> Unit,
    onAction: (ConversationRowAction) -> Unit,
    onSwipeDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
            }
            // Reset rather than staying dismissed: the actual delete goes through a confirmation
            // dialog (ConversationActionDialog), same as the old ItemTouchHelper swipe behavior.
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.appColors.deleteOptionColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_24),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.appColors.deleteOptionIconColor
                )
            }
        }
    ) {
        var showMenu by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
        ) {
            AndroidView(
                factory = { ctx -> ConversationView(ctx) },
                update = { view -> view.bind(thread, isTyping, glide) },
                // Opaque base so the swipe-to-delete layer underneath doesn't show through flat rows.
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.appColors.homeBackground)
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.appColors.backgroundColor.copy(alpha = 0.96f),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .zIndex(1f)
                    .padding(vertical = 2.dp)
            ) {
                for (item in menuItemsFor(thread, isSecretGroupActive)) {
                    ContextMenuItem(
                        iconRes = item.iconRes,
                        label = stringResource(item.labelRes),
                        destructive = item.action == ConversationRowAction.Delete,
                        onClick = {
                            showMenu = false
                            onAction(item.action)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_chats_doodle),
            contentDescription = null,
            modifier = Modifier.size(width = 138.dp, height = 118.dp)
        )
        Text(
            text = stringResource(R.string.bchat_empty_state_message),
            color = MaterialTheme.appColors.homeRowPreview,
            fontFamily = OpenSans,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

package io.beldex.bchat.my_account.ui

import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.BorderStroke
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.bumptech.glide.Glide
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ContextMenuItem
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.conversation.v2.dialogs.UnblockUserDialog
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.home.ConversationView
import io.beldex.bchat.home.NotificationSettingDialog
import io.beldex.bchat.my_account.ui.dialogs.DeleteChatConfirmationDialog
import io.beldex.bchat.my_account.ui.dialogs.LockOptionsDialog
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import kotlinx.coroutines.launch
import io.beldex.bchat.compose_utils.RobotoMono


@Composable
fun ArchiveChatScreen(
    requestsList : List<ThreadRecord>,
    keepArchiveChat: Boolean,
    onRequestClick : (ThreadRecord) -> Unit,
    onTabChatSetting : () -> Unit,
    archiveChatViewModel : ArchiveChatViewModel,
    groupDatabase : GroupDatabase,
    modifier : Modifier=Modifier
) {
    val context=LocalContext.current
    var selectedThreadId by remember { mutableStateOf<Long?>(null) }


    val threadRecord = requestsList.find { it.threadId == selectedThreadId }
    var showMenu by remember { mutableStateOf(false) }

    var showBlockPopup by remember {
        mutableStateOf(false)
    }
    var showUnBlockPopup by remember {
        mutableStateOf(false)
    }
    var showMuteNotification by remember {
        mutableStateOf(false)
    }
    var showNotificationSettings by remember {
        mutableStateOf(false)
    }
    var showDeletePopup by remember {
        mutableStateOf(false)
    }
    if (showBlockPopup) {
        BChatTheme(
            darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
        ) {
            UnblockUserDialog(title=stringResource(id=R.string.block_contact),
                message=stringResource(id=R.string.block_user_confirmation),
                positiveButtonTitle=stringResource(id=R.string.yes),
                onAccept={
                    showBlockPopup=false
                    threadRecord?.let { thread ->
                        archiveChatViewModel.onEvent(
                            ArchiveChatsEvents.BlockConversation(
                                thread
                            )
                        )
                    }
                },
                onCancel={
                    showBlockPopup=false

                })
        }
    }

    if (showUnBlockPopup) {
        BChatTheme(
            darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
        ) {
            UnblockUserDialog(title=stringResource(id=R.string.unblock_contact),
                message=stringResource(id=R.string.unblock_user_confirmation),
                positiveButtonTitle=stringResource(id=R.string.ConversationActivity_unblock),
                onAccept={
                    threadRecord?.let { thread ->
                        archiveChatViewModel.onEvent(
                            ArchiveChatsEvents.UnBlockConversation(
                                thread
                            )
                        )
                    }
                    showUnBlockPopup=false
                },
                onCancel={
                    showUnBlockPopup=false
                })
        }
    }

    if (showMuteNotification) {
        threadRecord?.let { thread ->
            if (thread.recipient.isMuted) {
                LaunchedEffect(key1=true) {
                    launch {
                        DatabaseComponent.get(context).recipientDatabase()
                            .setMuted(thread.recipient, 0)
                        showMuteNotification=false
                    }
                }

            } else {
                BChatTheme(
                    darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
                ) {
                    val timesOption=context.resources.getStringArray(R.array.mute_durations)

                    LockOptionsDialog(
                        title=stringResource(R.string.conversation_unmuted__mute_notifications),
                        options=timesOption.toList(),
                        currentValue=timesOption[thread.recipient.mutedUntil.toInt()],
                        onDismiss={
                            showMuteNotification=false
                        },
                        onValueChanged={ _, index ->
                            archiveChatViewModel.onEvent(
                                ArchiveChatsEvents.MuteNotification(
                                    thread, index, context
                                )
                            )
                            showMuteNotification=false
                        })
                }
            }
        }
    }

    if (showNotificationSettings) {
        threadRecord?.let { thread ->
            BChatTheme(
                darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
            ) {
                val option=context.resources.getStringArray(R.array.notify_types)
                NotificationSettingDialog(
                    onDismiss={
                        showNotificationSettings=false
                    },
                    onClick={
                        showNotificationSettings=false
                    },
                    options=option.toList(),
                    currentValue=option[thread.recipient.notifyType],
                    onValueChanged={ _, index ->
                        archiveChatViewModel.onEvent(
                            ArchiveChatsEvents.NotificationSettings(
                                thread, index, context
                            )
                        )
                        showNotificationSettings=false
                    })
            }
        }
    }

    if (showDeletePopup) {
        threadRecord?.let { thread ->
            val message=if (thread.recipient.isGroupRecipient) {
                val group=groupDatabase.getGroup(thread.recipient.address.toString()).orNull()
                if (group != null && group.admins.map { it.toString() }
                        .contains(TextSecurePreferences.getLocalNumber(context))) {
                    context.resources.getString(R.string.group_delete_confirmation_message)
                } else {
                    context.resources.getString(R.string.ConversationActivity_are_you_sure_you_want_to_leave_this_group)
                }
            } else {
                context.resources.getString(R.string.activity_home_delete_conversation_dialog_message)
            }
            BChatTheme(
                darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
            ) {
                DeleteChatConfirmationDialog(
                    message=message,
                    onConfirmation={
                        archiveChatViewModel.onEvent(
                            ArchiveChatsEvents.DeleteConversation(
                                thread, context
                            )
                        )
                        showDeletePopup=false
                    },
                    onDismissRequest={
                        //archiveChatViewModel.onEvent(ArchiveChatsEvents.DeleteConversation(thread,context))
                        showDeletePopup=false
                    },
                )
            }
        }
    }
    // Long-press menu items; shown in the same anchored glass dropdown as the Home rows.
    val menuItems: @Composable (ThreadRecord) -> Unit = { thread ->
        if (!thread.recipient.isGroupRecipient && !thread.recipient.isLocalNumber) {
            if (thread.recipient.isBlocked) {
                ContextMenuItem(
                    iconRes = R.drawable.ic_unblock,
                    label = stringResource(id = R.string.ConversationActivity_unblock),
                    onClick = { showMenu = false; showUnBlockPopup = true }
                )
            } else {
                ContextMenuItem(
                    iconRes = R.drawable.ic_block,
                    label = stringResource(id = R.string.RecipientPreferenceActivity_block),
                    onClick = { showMenu = false; showBlockPopup = true }
                )
            }
        }
        ContextMenuItem(
            iconRes = R.drawable.ic_unarchive_chats,
            label = stringResource(id = R.string.un_archive_chat_title),
            onClick = {
                showMenu = false
                archiveChatViewModel.onEvent(ArchiveChatsEvents.UnArchiveChats(thread))
            }
        )
        ContextMenuItem(
            iconRes = if (thread.isPinned) R.drawable.ic_unpin else R.drawable.ic_pin_menu,
            label = stringResource(id = if (thread.isPinned) R.string.conversation_unpin else R.string.conversation_pin),
            onClick = {
                showMenu = false
                archiveChatViewModel.onEvent(ArchiveChatsEvents.TogglePin(thread))
            }
        )
        if (thread.unreadCount > 0) {
            ContextMenuItem(
                iconRes = R.drawable.ic_mark_as_read_menu,
                label = stringResource(id = R.string.MessageNotifier_mark_all_as_read),
                onClick = {
                    showMenu = false
                    archiveChatViewModel.onEvent(ArchiveChatsEvents.MarkAsRead(thread))
                }
            )
        }
        if (!thread.recipient.isLocalNumber) {
            ContextMenuItem(
                iconRes = if (thread.recipient.isMuted) R.drawable.ic_unmute_notification_menu else R.drawable.ic_mute_notification_menu,
                label = stringResource(
                    id = if (thread.recipient.isMuted) R.string.conversation_muted__unmute
                    else R.string.conversation_unmuted__mute_notifications
                ),
                onClick = { showMenu = false; showMuteNotification = true }
            )
        }
        val isSecretGroupActive = thread.recipient.isClosedGroupRecipient &&
            (groupDatabase.getGroup(thread.recipient.address.toString()).orNull()?.isActive == true)
        if (thread.recipient.isGroupRecipient && !thread.recipient.isMuted && isSecretGroupActive) {
            ContextMenuItem(
                iconRes = R.drawable.ic_notification_settings_menu,
                label = stringResource(id = R.string.RecipientPreferenceActivity_notification_settings),
                onClick = { showMenu = false; showNotificationSettings = true }
            )
        }
        ContextMenuItem(
            iconRes = R.drawable.ic_delete_menu,
            label = stringResource(id = R.string.delete),
            destructive = true,
            onClick = { showMenu = false; showDeletePopup = true }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {

            Text(
                text=stringResource(
                    id = if (keepArchiveChat)
                        R.string.archive_chat_content
                    else
                        R.string.unarchive_chat_content),
                    style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight(400),
                    fontFamily = RobotoMono,
                    color = MaterialTheme.appColors.onboardingHeadlineColor
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
                    .clickable {
                        onTabChatSetting()
                    }
            )

            Divider(
                color = MaterialTheme.appColors.dividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier
            ) {
                items(
                    count = requestsList.size,
                    key = { requestsList[it].recipient.address }
                ) {

                    val archivedList = requestsList[it]

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        ArchiveChatItem(
                            context = context,
                            thread = archivedList,
                            keepArchiveChat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onRequestClick(archivedList) },
                                        onLongPress = {
                                            selectedThreadId = archivedList.threadId
                                            showMenu = true
                                        }
                                    )
                                }
                        )
                        // Same menu as the Home row: anchored to the pressed row, 104dp in.
                        Box(modifier = Modifier.matchParentSize()) {
                            DropdownMenu(
                                expanded = showMenu && selectedThreadId == archivedList.threadId,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = 104.dp, y = 0.dp),
                                containerColor = MaterialTheme.appColors.selectedItemBackground.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, MaterialTheme.appColors.optionBorder),
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .zIndex(1f)
                                    .padding(vertical = 2.dp)
                            ) {
                                menuItems(archivedList)
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun ArchiveChatItem(
    context : Context,
    thread : ThreadRecord,
    keepArchiveChat: Boolean,
    modifier : Modifier=Modifier
) {
    val glide = remember(context) { Glide.with(context) }
    AndroidView(
        factory = { ctx -> ConversationView(ctx) },
        update = { view ->
            view.bind(thread, false, glide)
        },
        modifier = modifier
    )
}

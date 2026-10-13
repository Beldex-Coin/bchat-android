package io.beldex.bchat.my_account.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.IntrinsicSize
import io.beldex.bchat.compose_utils.ContextMenuItem
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter
import io.beldex.bchat.conversation.v2.contact_sharing.flattenData
import io.beldex.bchat.conversation.v2.dialogs.UnblockUserDialog
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.database.RecipientDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.home.NotificationSettingDialog
import io.beldex.bchat.my_account.ui.dialogs.DeleteChatConfirmationDialog
import io.beldex.bchat.my_account.ui.dialogs.LockOptionsDialog
import io.beldex.bchat.textformatter.TextFormatter
import io.beldex.bchat.textformatter.TextFormatter.toAnnotatedString
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.shortNameAndAddress
import kotlinx.coroutines.launch
import java.util.Locale


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
    val itemPositions = remember { mutableStateMapOf<Long, MenuPositionState>() }
    var menuPositionState by remember { mutableStateOf(MenuPositionState()) }
    val popupOffset = rememberMenuPosition(menuPositionState, 320.dp)

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
    if (showMenu) {
        threadRecord?.let { thread ->
            Popup(
                alignment = Alignment.TopStart,
                offset = popupOffset,
                onDismissRequest = { showMenu = false },
            ) {
                Card(
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.backgroundColor.copy(alpha = 0.96f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier.wrapContentWidth().widthIn(max = 320.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .padding(vertical = 10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
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
                }
            }
        }
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
                    fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                    color = MaterialTheme.appColors.onboardingHeadlineColor
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .padding(16.dp)
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

                    ArchiveChatItem(
                        context = context,
                        thread = archivedList,
                        keepArchiveChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                            .onGloballyPositioned { coordinates ->
                                itemPositions[archivedList.threadId] = MenuPositionState(
                                    offset = coordinates.positionInWindow(),
                                    itemHeight = coordinates.size.height
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onRequestClick(archivedList) },
                                    onLongPress = {
                                        itemPositions[archivedList.threadId]?.let { pos ->
                                            menuPositionState = pos
                                        }
                                        selectedThreadId = archivedList.threadId
                                        showMenu = true
                                    }
                                )
                            }
                    )
                }
            }
        }

        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap={
                                showMenu=false
                            })
                    }
            )
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
    // Revamp_2026: archived rows are the exact same row as Home (notched avatar, mono preview,
    // square unread badge, new mute/mention/pin icons), so reuse the shared ConversationView.
    val glide = androidx.compose.runtime.remember(context) { com.bumptech.glide.Glide.with(context) }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx -> io.beldex.bchat.home.ConversationView(ctx) },
        update = { view -> view.bind(thread, false, glide) },
        modifier = modifier
    )
}

private fun getDisplayName(context : Context, publicKey : String) : String {
    val contact=
        DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR, context) ?: publicKey
}

private fun getUserDisplayName(context : Context, recipient : Recipient?) : String? {
    return if (recipient?.isLocalNumber == true) {
        context.getString(R.string.note_to_self)
    } else {
        recipient?.name // Internally uses the Contact API
    }
}

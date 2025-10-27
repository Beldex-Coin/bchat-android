package io.beldex.bchat.my_account.ui

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.GroupRecord
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.conversation.v2.dialogs.UnblockUserDialog
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.database.RecipientDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.home.NotificationSettingDialog
import io.beldex.bchat.my_account.ui.dialogs.DeleteChatConfirmationDialog
import io.beldex.bchat.my_account.ui.dialogs.LockOptionsDialog
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import kotlinx.coroutines.launch
import java.util.Locale


@Composable
fun ArchiveChatScreen(
    requestsList : List<ThreadRecord>,
    onRequestClick : (ThreadRecord) -> Unit,
    archiveChatViewModel : ArchiveChatViewModel,
    groupDatabase : GroupDatabase,
    modifier : Modifier=Modifier
) {
    val context=LocalContext.current
    val activity = (context as? Activity)
    val requestToTakeAction : ThreadRecord?=null
    var threadRecord by remember {
        mutableStateOf(requestToTakeAction)
    }
    var showMenu by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val recipient=threadRecord?.recipient

    var showBlockPopup by remember {
        mutableStateOf(false)
    }
    var showUnBlockPopup by remember {
        mutableStateOf(false)
    }
    var showMuteNotification by remember {
        mutableStateOf(false)
    }
    var showUnMuteNotification by remember {
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
                    archiveChatViewModel.onEvent(ArchiveChatsEvents.BlockConversation(threadRecord!!))
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
                positiveButtonTitle=stringResource(id=R.string.unblock),
                onAccept={
                    archiveChatViewModel.onEvent(ArchiveChatsEvents.UnBlockConversation(threadRecord!!))
                    showUnBlockPopup=false
                },
                onCancel={
                    showUnBlockPopup=false
                })
        }
    }

    if (showMuteNotification) {
        if (threadRecord!!.recipient.isMuted) {
            LaunchedEffect(key1=true) {
                launch {
                    DatabaseComponent.get(context).recipientDatabase()
                        .setMuted(threadRecord!!.recipient, 0)
                    showMuteNotification=false
                }
            }

        } else {
            BChatTheme(
                darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
            ) {
                val timesOption=context.resources.getStringArray(R.array.mute_durations)

                LockOptionsDialog(title=stringResource(R.string.conversation_unmuted__mute_notifications),
                    options=timesOption.toList(),
                    currentValue=timesOption[threadRecord!!.recipient.mutedUntil.toInt()],
                    onDismiss={
                        showMuteNotification=false
                    },
                    onValueChanged={ _, index ->
                        archiveChatViewModel.onEvent(
                            ArchiveChatsEvents.MuteNotification(
                                threadRecord!!, index, context
                            )
                        )
                        showMuteNotification=false
                    })
            }
        }
    }

    if (showNotificationSettings) {
        BChatTheme(
            darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
        ) {
            val option=context.resources.getStringArray(R.array.notify_types)
            NotificationSettingDialog(onDismiss={
                showNotificationSettings=false
            },
                onClick={
                    showNotificationSettings=false
                },
                options=option.toList(),
                currentValue=option[threadRecord!!.recipient.notifyType],
                onValueChanged={ _, index ->
                    archiveChatViewModel.onEvent(
                        ArchiveChatsEvents.NotificationSettings(
                            threadRecord!!, index, context
                        )
                    )
                    showNotificationSettings=false
                })
        }
    }

    if (showDeletePopup) {
        val message=if (recipient!!.isGroupRecipient) {
            val group=groupDatabase.getGroup(recipient.address.toString()).orNull()
            if (group != null && group.admins.map { it.toString() }
                    .contains(TextSecurePreferences.getLocalNumber(context))) {
                "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
            } else {
                context.resources.getString(R.string.activity_home_leave_group_dialog_message)
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
                            threadRecord!!, context
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

    fun getGroup(recipient: Recipient): GroupRecord? = groupDatabase.getGroup(recipient.address.toGroupString()).orNull()

    fun isSecretGroupIsActive(recipient: Recipient):Boolean {
        return if (recipient.isClosedGroupRecipient) {
            val group = getGroup(recipient)
            val isActive = (group?.isActive == true)
            isActive
        } else {
            true
        }
    }

    if (showMenu) {
        Popup(
            offset=IntOffset(x=offset.x.toInt(), y=offset.y.toInt()),
            onDismissRequest={
                showMenu=false
            },
        ) {
            Card(
                modifier=Modifier.width(200.dp)
            ) {
                Column(
                    modifier=Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!recipient!!.isBlocked) {
                        if (!recipient.isGroupRecipient && !recipient.isLocalNumber) {
                            Row(
                                horizontalArrangement=Arrangement.Start,
                                verticalAlignment=Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter=painterResource(id=R.drawable.ic_block),
                                    contentDescription="",
                                    tint=MaterialTheme.appColors.iconTint,

                                    )
                                TextButton(onClick={
                                    // Handle action
                                    showMenu=false
                                    showBlockPopup=true

                                }) {
                                    Text(stringResource(id=R.string.RecipientPreferenceActivity_block))
                                }
                            }
                        }
                    }
                    if (recipient.isBlocked) {
                        if (!recipient.isGroupRecipient && !recipient.isLocalNumber) {
                            Row(
                                horizontalArrangement=Arrangement.Start,
                                verticalAlignment=Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter=painterResource(id=R.drawable.ic_unblock),
                                    contentDescription="",
                                    tint=MaterialTheme.appColors.iconTint,
                                )
                                TextButton(onClick={
                                    // Handle action
                                    showMenu=false
                                    showUnBlockPopup=true
                                }) {
                                    Text(stringResource(id=R.string.RecipientPreferenceActivity_unblock))
                                }
                            }
                        }
                    }
                    Row(
                        horizontalArrangement=Arrangement.Start,
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Icon(
                            painter=painterResource(id=R.drawable.ic_unarchive_chats),
                            contentDescription="",
                            tint=MaterialTheme.appColors.iconTint,
                            modifier=Modifier.size(14.dp)
                        )
                        TextButton(onClick={
                            // Handle action
                            showMenu=false
                            archiveChatViewModel.onEvent(
                                ArchiveChatsEvents.UnArchiveChats(
                                    threadRecord!!
                                )
                            )
                        }) {
                            Text(stringResource(id=R.string.un_archive_chat_title))
                        }
                    }
                    Row(
                        horizontalArrangement=Arrangement.Start,
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Icon(
                            painter=painterResource(id=if (threadRecord!!.recipient.isMuted) R.drawable.ic_unmute_notification_menu else R.drawable.ic_mute_notification_menu),
                            contentDescription="",
                            tint=MaterialTheme.appColors.iconTint
                        )
                        TextButton(onClick={
                            // Handle action
                            showMenu = false
                            showMuteNotification=true
                        }) {
                            Text(stringResource(id=if (threadRecord!!.recipient.isMuted) R.string.conversation_muted__unmute else R.string.conversation_unmuted__mute_notifications))
                        }
                    }
                    if (recipient.isGroupRecipient && !recipient.isMuted && !recipient.isLocalNumber && isSecretGroupIsActive(recipient)) {
                        Row(
                            horizontalArrangement=Arrangement.Start,
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            Icon(
                                painter=painterResource(id=R.drawable.ic_notification_settings_menu),
                                contentDescription="",
                                tint=MaterialTheme.appColors.iconTint
                            )
                            TextButton(onClick={
                                // Handle action
                                showMenu=false
                                showNotificationSettings=true
                                //onDismiss(false)
                            }) {
                                Text(stringResource(id=R.string.RecipientPreferenceActivity_notification_settings))
                            }
                        }
                    }
                    if (threadRecord!!.unreadCount > 0) {
                        Row(
                            horizontalArrangement=Arrangement.Start,
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            Icon(
                                painter=painterResource(id=R.drawable.ic_mark_as_read_menu),
                                contentDescription="",
                                tint=MaterialTheme.appColors.iconTint
                            )
                            TextButton(onClick={
                                // Handle action
                                showMenu=false
                                archiveChatViewModel.onEvent(
                                    ArchiveChatsEvents.MarkAsRead(
                                        threadRecord!!
                                    )
                                )
                            }) {
                                Text(stringResource(id=R.string.MessageNotifier_mark_all_as_read))
                            }
                        }
                    }
                    Row(
                        horizontalArrangement=Arrangement.Start,
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Icon(
                            painter=painterResource(id=R.drawable.ic_delete_menu),
                            contentDescription="",
                            tint=MaterialTheme.appColors.deleteOptionColor,
                        )
                        TextButton(onClick={
                            // Handle action
                            showMenu=false
                            showDeletePopup=true
                        }){
                            Text(
                                stringResource(
                                    id=R.string.delete),
                                color=MaterialTheme.appColors.deleteOptionColor,
                            )
                        }
                    }
                }
            }
        }
    }

    Text(
        text=stringResource(id=R.string.archive_chat_content),
        style=MaterialTheme.typography.titleSmall.copy(
            fontSize=12.sp, fontWeight=FontWeight(400), color=MaterialTheme.appColors.textColor
        ),
        textAlign=TextAlign.Start,
        modifier=Modifier.padding(16.dp)
    )
    /*BottomSheetContent()*/


    LazyColumn(
        verticalArrangement=Arrangement.spacedBy(8.dp), modifier=modifier
    ) {
        items(count=requestsList.size, key={
            requestsList[it].recipient.address
        }) {
            val archivedList=requestsList[it]

            ArchiveChatItem(context=context, thread=archivedList, modifier=Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap={
                        onRequestClick(archivedList)
                    }, onLongPress={
                        threadRecord=archivedList
                        offset=it
                        showMenu=true
                    })
                })
        }
    }
}

@Composable
fun ArchiveChatItem(
    context : Context,
    thread : ThreadRecord,
    modifier : Modifier=Modifier
) {
    val unreadCount by remember {
        mutableIntStateOf(thread.unreadCount)
    }
    val formattedUnreadCount=if (thread.isRead) {
        null
    } else {
        if (unreadCount < 100) unreadCount.toString() else "99+"
    }

    Row(
        verticalAlignment=Alignment.CenterVertically, modifier=modifier
    ) {

        val isOpenGroupWithProfilePicture=
            thread.recipient.isOpenGroupRecipient && thread.recipient.groupAvatarId != null
        if (thread.recipient.isGroupRecipient && !isOpenGroupWithProfilePicture) {
            val pictureType=ProfilePictureMode.GroupPicture
            val members=DatabaseComponent.get(context).groupDatabase()
                .getGroupMemberAddresses(thread.recipient.address.toGroupString(), true).sorted()
                .take(2).toMutableList()
            /*val pk=members.getOrNull(0)?.serialize() ?: ""
            val displayName=getDisplayName(context, pk)*/
            val additionalPk=members.getOrNull(1)?.serialize() ?: ""
            val additionalDisplay=getDisplayName(context, additionalPk)
            ProfilePictureComponent(
                publicKey=thread.recipient.address.toString() ?: "",
                displayName=thread.recipient.name.toString() ?: "",
                additionalPublicKey=additionalPk,
                additionalDisplayName=additionalDisplay,
                containerSize=ProfilePictureMode.SmallPicture.size,
                pictureMode=pictureType
            )
        } else {
            val pictureType=ProfilePictureMode.SmallPicture
            val displayName=getDisplayName(context, thread.recipient.address.toString())
            ProfilePictureComponent(
                publicKey=thread.recipient.address.toString(),
                displayName=displayName,
                containerSize=pictureType.size,
                pictureMode=pictureType
            )
        }

        Spacer(modifier=Modifier.width(8.dp))

        val senderName= getUserDisplayName(context, thread.recipient) ?: thread.recipient.address.toString()
        val rawSnippet=thread.getDisplayBody(context)
        val timeStamp= DateUtils.getDisplayFormattedTimeSpanString(context, Locale.getDefault(), thread.date)

        Column(
            verticalArrangement=Arrangement.SpaceEvenly,
            modifier=modifier
                .weight(1f)
                .align(Alignment.Top)
        ) {
            Text(
                text=senderName, style=MaterialTheme.typography.titleMedium.copy(
                    fontWeight=FontWeight(700), fontSize=14.sp

                ), maxLines=1, overflow=TextOverflow.Ellipsis
            )

            Spacer(modifier=Modifier.height(8.dp))

            if (thread.isSharedContact) {
                val contactName = UpdateMessageData.fromJSON(thread.body)?.let {
                    val data = it.kind as UpdateMessageData.Kind.SharedContact
                    data.name
                } ?: "No Name"
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.ic_profile_default),
                        contentDescription = "",
                        tint = MaterialTheme.appColors.iconTint,
                        modifier = Modifier
                            .size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight(400),
                            fontSize = 12.sp
                        ),
                        maxLines=1,
                        overflow=TextOverflow.Ellipsis,
                    )
                }
            } else {
                val snippet=MentionUtilities.highlightMentions(rawSnippet, thread.threadId, context)
                Text(
                    text=snippet, style=MaterialTheme.typography.bodySmall.copy(
                        fontWeight=FontWeight(400), fontSize=12.sp
                    ), maxLines=1, overflow=TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier=Modifier.width(8.dp))

        Column(
            horizontalAlignment=Alignment.End,
            verticalArrangement=Arrangement.Top,
            modifier=Modifier.align(Alignment.Top)
        ) {

            Text(
                text=timeStamp, style=MaterialTheme.typography.bodySmall.copy(
                    fontWeight=FontWeight(400),
                    fontSize=10.sp,
                    color = if(thread.unreadCount !=0 && !thread.isRead) MaterialTheme.appColors.textGreen else MaterialTheme.appColors.textColor
                ), maxLines=1, overflow=TextOverflow.Ellipsis, modifier=Modifier.padding(top=12.dp)
            )
            Spacer(modifier=Modifier.height(8.dp))

            Row(
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically,
                modifier=Modifier.align(Alignment.CenterHorizontally)
            ) {
                if (thread.recipient.isMuted) {
                    Image(
                        painter=painterResource(id=R.drawable.ic_mute_home), contentDescription=""
                    )
                }
                if(thread.recipient.notifyType == RecipientDatabase.NOTIFY_TYPE_MENTIONS && !thread.recipient.isMuted){
                    Image(
                        painter=painterResource(id=R.drawable.ic_mention_home), contentDescription=""
                    )
                }
                Spacer(modifier=Modifier.width(4.dp))
                if (thread.unreadCount != 0 && !thread.isRead) {
                    Box(
                        contentAlignment=Alignment.Center, modifier=Modifier
                            .size(24.dp)
                            .background(
                                color=MaterialTheme.appColors.textSelectionColor, shape=CircleShape
                            )
                    ) {
                        Text(
                            text="$formattedUnreadCount",
                            color=Color.White,
                            fontWeight=FontWeight.Bold,
                            fontSize=if (unreadCount < 100) 12.sp else 10.sp
                        )
                    }
                }
            }
        }
    }
}

private fun getDisplayName(context : Context, publicKey : String) : String {
    val contact=
        DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
}

private fun getUserDisplayName(context : Context, recipient : Recipient?) : String? {
    return if (recipient?.isLocalNumber == true) {
        context.getString(R.string.note_to_self)
    } else {
        recipient?.name // Internally uses the Contact API
    }
}
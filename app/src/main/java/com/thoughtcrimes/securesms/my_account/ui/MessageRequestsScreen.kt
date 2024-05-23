package com.thoughtcrimes.securesms.my_account.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureComponent
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureMode
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.my_account.ui.dialogs.RequestBlockConfirmationDialog
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MessageRequestsScreen(
    requestsList: List<ThreadRecord>,
    onEvent: (MessageRequestEvents) -> Unit,
    onRequestClick: (ThreadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val requestToTakeAction: ThreadRecord? = null
    var threadRecord by remember {
        mutableStateOf(requestToTakeAction)
    }
    if (requestsList.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_no_requests),
                contentDescription = stringResource(R.string.no_pending_requests)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_pending_requests),
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        var showBlockConfirmationDialog by remember {
            mutableStateOf(false)
        }
        var showDeleteConfirmationDialog by remember {
            mutableStateOf(false)
        }
        val coroutineScope = rememberCoroutineScope()
        if (showBlockConfirmationDialog) {
            RequestBlockConfirmationDialog(
                message = stringResource(id = R.string.message_requests_block_message),
                actionTitle = stringResource(id = R.string.yes),
                onConfirmation = {
                    threadRecord?.let {
                        onEvent(MessageRequestEvents.BlockRequest(it))
                    }
                    threadRecord = null
                },
                onDismissRequest = {
                    showBlockConfirmationDialog = false
                }
            )
        }
        if (showDeleteConfirmationDialog) {
            RequestBlockConfirmationDialog(
                message = stringResource(id = R.string.message_requests_delete_message),
                actionTitle = stringResource(id = R.string.yes),
                onConfirmation = {
                    threadRecord?.let {
                        onEvent(MessageRequestEvents.DeleteRequest(it))
                        coroutineScope.launch(Dispatchers.IO) {
                            ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(context)
                        }
                    }
                    threadRecord = null
                },
                onDismissRequest = {
                    showDeleteConfirmationDialog = false
                }
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
        ) {
            items(
                count = requestsList.size,
                key = {
                    requestsList[it].recipient.address
                }
            ) {
                val recipient = requestsList[it]
                MessageRequestItem(
                    context = context,
                    request = recipient,
                    deleteRequest = { request ->
                        threadRecord = request
                        showDeleteConfirmationDialog = true
                    },
                    blockRequest = { request ->
                        threadRecord = request
                        showBlockConfirmationDialog = true
                    },
                    modifier =Modifier
                            .fillMaxWidth()
                            .background(
                                    color=MaterialTheme.appColors.settingsCardBackground,
                                    shape=RoundedCornerShape(50)
                            )
                            .padding(16.dp)
                            .clickable {
                                onRequestClick(recipient)
                            }
                )
            }
        }
    }
}

@Composable
fun MessageRequestItem(
    context: Context,
    request: ThreadRecord,
    deleteRequest: (ThreadRecord) -> Unit,
    blockRequest: (ThreadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val isOpenGroupWithProfilePicture = request.recipient.isOpenGroupRecipient && request.recipient.groupAvatarId != null
        if (request.recipient.isGroupRecipient && !isOpenGroupWithProfilePicture) {
            val pictureType = ProfilePictureMode.GroupPicture
            val members = DatabaseComponent.get(context).groupDatabase()
                .getGroupMemberAddresses(request.recipient.address.toGroupString(), true)
                .sorted()
                .take(2)
                .toMutableList()
            val pk = members.getOrNull(0)?.serialize() ?: ""
            val displayName = getDisplayName(context, pk)
            val additionalPk = members.getOrNull(1)?.serialize() ?: ""
            val additionalDisplay = getDisplayName(context, additionalPk)
            ProfilePictureComponent(
                publicKey = pk,
                displayName = displayName,
                additionalPublicKey = additionalPk,
                additionalDisplayName = additionalDisplay,
                containerSize = pictureType.size,
                pictureMode = pictureType
            )
        } else {
            val pictureType = ProfilePictureMode.SmallPicture
            val displayName = getDisplayName(context, request.recipient.address.toString())
            ProfilePictureComponent(
                publicKey = request.recipient.address.toString(),
                displayName = displayName,
                containerSize = pictureType.size,
                pictureMode = pictureType
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val senderName = getUserDisplayName(context, request.recipient) ?: request.recipient.address.toString()
        Text(
            text = senderName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight(700)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier =Modifier
                    .size(32.dp)
                    .background(
                            color=MaterialTheme.appColors.actionIconBackground,
                            shape=RoundedCornerShape(15)
                    )
                    .clickable {

                        deleteRequest(request)
                    }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_delete_24),
                contentDescription = "",
                modifier = Modifier
                    .size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier =Modifier
                    .size(32.dp)
                    .background(
                            color=MaterialTheme.appColors.actionIconBackground,
                            shape=RoundedCornerShape(15)
                    )
                    .clickable {
                        blockRequest(request)
                    }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_block_request),
                contentDescription = "",
                colorFilter = ColorFilter.tint(color = MaterialTheme.appColors.iconTint),
                modifier = Modifier
                    .size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier =Modifier
                    .size(32.dp)
                    .background(
                            color=MaterialTheme.appColors.primaryButtonColor,
                            shape=RoundedCornerShape(15)
                    )
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "",
                tint = Color.White
            )
        }
    }
}

private fun getDisplayName(context: Context, publicKey: String): String {
    val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
}

private fun getUserDisplayName(context: Context, recipient: Recipient?): String? {
    return if (recipient?.isLocalNumber == true) {
        context.getString(R.string.note_to_self)
    } else {
        recipient?.name // Internally uses the Contact API
    }
}

@Preview
@Composable
fun MessageRequestsScreenPreview() {
    MessageRequestsScreen(
        requestsList = emptyList(),
        onEvent = {},
        onRequestClick = {},
        modifier =Modifier
                .fillMaxSize()
                .padding(16.dp)
    )
}
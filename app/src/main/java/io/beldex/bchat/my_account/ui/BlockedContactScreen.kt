package io.beldex.bchat.my_account.ui

import android.content.res.Configuration
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.compose_utils.BChatCheckBox
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.Colors

@Composable
fun BlockedContactScreen(
    blockedList: List<Recipient>,
    selectedList: List<Recipient>,
    multiSelectActivated: Boolean,
    unBlockSingleContact: (Recipient) -> Unit,
    unBlockMultipleContacts: () -> Unit,
    addRemoveContactToList: (Recipient, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmationDialog by remember {
        mutableStateOf(false)
    }
    if (showConfirmationDialog) {
        DialogContainer(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            onDismissRequest = {
                showConfirmationDialog = false
            },
        ) {
            val context = LocalContext.current
            val title = if (selectedList.size == 1) {
                stringResource(id = R.string.Unblock_dialog__title_single).format(selectedList.first().name)
            } else {
                stringResource(id = R.string.Unblock_dialog__title_multiple)
            }
            val message = if (selectedList.size == 1) {
                stringResource(id = R.string.Unblock_dialog__message).format(selectedList.first().name)
            } else {
                val stringBuilder = StringBuilder()
                val iterator = selectedList.iterator()
                var numberAdded = 0
                while (iterator.hasNext() && numberAdded < 3) {
                    val nextRecipient = iterator.next()
                    if (numberAdded > 0) stringBuilder.append(", ")

                    stringBuilder.append(nextRecipient.name)
                    numberAdded++
                }
                val overflow = selectedList.size - numberAdded
                if (overflow > 0) {
                    stringBuilder.append(" ")
                    val string = context.resources.getQuantityString(R.plurals.Unblock_dialog__message_multiple_overflow, overflow)
                    stringBuilder.append(string.format(overflow))
                }
                stringResource(id = R.string.Unblock_dialog__message, stringBuilder.toString())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clickable {
                                showConfirmationDialog = false
                            }
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    Text(
                        text = stringResource(id = R.string.continue_2),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clickable {
                                unBlockMultipleContacts()
                                showConfirmationDialog = false
                            }
                    )
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                count = blockedList.size,
                key = {
                    "${blockedList[it].address.contactIdentifier()}_$it"
                }
            ) { index ->
                val contact = blockedList[index]
                BlockedContactItem(
                    contact = contact,
                    multiSelectActivated = multiSelectActivated,
                    isSelected = selectedList.contains(contact),
                    unblockContact = {
                        unBlockSingleContact(contact)
                    },
                    selectContact = {
                        addRemoveContactToList(contact, it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        if (multiSelectActivated && selectedList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = {
                    showConfirmationDialog = true
                },
                containerColor = MaterialTheme.appColors.primaryButtonColor,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.unblock_selected),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight(400),
                        fontSize = 16.sp
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun BlockedContactItem(
    contact: Recipient,
    multiSelectActivated: Boolean,
    isSelected: Boolean,
    unblockContact: () -> Unit,
    selectContact: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(15),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.settingsCardBackground
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    if (multiSelectActivated) 8.dp else 16.dp
                )
        ) {

            Box(
                    modifier=Modifier
                            .height(36.dp)
                            .width(36.dp)
                            .clip(RoundedCornerShape(15)),
                    contentAlignment=Alignment.Center,
            ) {
                ProfilePictureComponent(
                        publicKey=contact.address.toString(),
                        displayName=contact.name.toString(),
                        containerSize=36.dp,
                        pictureMode=ProfilePictureMode.SmallPicture)
            }
            Text(
                text = contact.name ?: "",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 8.dp
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (multiSelectActivated) {
                BChatCheckBox(
                    checked = isSelected,
                    onCheckedChange = {
                        selectContact(it)
                    },
                    modifier = Modifier.padding(end = 10.dp)
                )
            } else {
                Text(
                    text = "Unblock",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight(600),
                        fontSize = 12.sp
                    ),
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.appColors.popUpAddressBackground,
                            shape = RoundedCornerShape(15)
                        )
                        .padding(
                            8.dp
                        )
                        .clickable {
                            unblockContact()
                        }
                )
            }
        }
    }
}

@Preview
@Composable
fun BlockedContactScreenPreview() {
    BChatTheme {
        BlockedContactScreen(
            blockedList = emptyList(),
            selectedList = emptyList(),
            multiSelectActivated = false,
            unBlockSingleContact = {},
            unBlockMultipleContacts = {},
            addRemoveContactToList = {_,_ ->},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BlockedContactScreenPreviewDark() {
    BChatTheme {
        BlockedContactScreen(
            blockedList = emptyList(),
            selectedList = emptyList(),
            multiSelectActivated = false,
            unBlockSingleContact = {},
            unBlockMultipleContacts = {},
            addRemoveContactToList = {_,_ ->},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}
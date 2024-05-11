package com.thoughtcrimes.securesms.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.compose_utils.BChatCheckBox
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

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
                containerColor = MaterialTheme.appColors.secondaryButtonColor,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.unblock_selected),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.primaryButtonColor
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
            Image(
                painter = painterResource(id = R.drawable.dummy_user),
                contentDescription = "",
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(15))
            )

            Text(
                text = contact.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 8.dp
                    )
            )

            if (multiSelectActivated) {
                BChatCheckBox(
                    checked = isSelected,
                    onCheckedChange = {
                        selectContact(it)
                    }
                )
            } else {
                Text(
                    text = "Unblock",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.appColors.cardBackground,
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
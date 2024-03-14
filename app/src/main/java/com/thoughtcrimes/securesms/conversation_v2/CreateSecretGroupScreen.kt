package com.thoughtcrimes.securesms.conversation_v2

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.groupSizeLimit
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.compose_utils.BChatCheckBox
import com.thoughtcrimes.securesms.compose_utils.BChatOutlinedTextField
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.ui.BChatPreviewContainer
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import io.beldex.bchat.R
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

@Composable
fun CreateSecretGroup(
    searchQuery: String,
    contacts: List<Recipient>,
    selectedContact: List<String>,
    onEvent: (SecretGroupEvents) -> Unit
) {
    var groupName by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    val activity = (context as? Activity)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.create_secret_group),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight(800)
                )
            )
            BChatOutlinedTextField(
                value = groupName,
                label = stringResource(R.string.enter_group_name),
                onValueChange = {
                    groupName = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 8.dp
                    ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Divider(
            color = colorResource(id = R.color.divider_color),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.5f))

        BChatOutlinedTextField(
            value = searchQuery,
            onValueChange = { onEvent(SecretGroupEvents.SearchQueryChanged(it)) },
            label = stringResource(R.string.search_contact),
            shape = RoundedCornerShape(36.dp),
            trailingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "",
                    tint = MaterialTheme.appColors.iconTint
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
                .weight(1f)
        ) {
            items(contacts) {
                GroupContact(
                    recipient = it,
                    isSelected = selectedContact.contains(it.address.toString()),
                    onSelectionChanged = { contact, isSelected ->
                        onEvent(SecretGroupEvents.RecipientSelectionChanged(contact, isSelected))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 8.dp
                        )
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(
                    color = MaterialTheme.appColors.createButtonBackground
                ),
            contentAlignment = Alignment.Center,
        ) {
            PrimaryButton(
                onClick = {
                    createClosedGroup(groupName,context,activity,selectedContact)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.create),
                    style = BChatTypography.bodyLarge.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun GroupContact(
    recipient: Recipient,
    isSelected: Boolean,
    onSelectionChanged: (Recipient, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.appColors.contactCardBorder
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.contactCardBackground
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_camera_profile_pic),
                contentDescription = ""
            )

            Text(
                text = recipient.name.toString(),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f)
            )

            BChatCheckBox(
                checked = isSelected,
                onCheckedChange = {
                    onSelectionChanged(recipient, it)
                },
            )
        }
    }
}

fun getUserDisplayName(publicKey: String, context: Context): String {
    val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
}

private fun createClosedGroup(name: String, context: Context, activity: Activity?,selected: Collection<String>) {

    if (name.isEmpty()) {
        return Toast.makeText(context, R.string.activity_create_closed_group_group_name_missing_error, Toast.LENGTH_LONG).show()
    }
    if (name.length >= 64) {
        return Toast.makeText(context, R.string.activity_create_closed_group_group_name_too_long_error, Toast.LENGTH_LONG).show()
    }
    if (selected.isEmpty()) {
        return Toast.makeText(
            context,
            R.string.activity_create_closed_group_not_enough_group_members_error,
            Toast.LENGTH_LONG
        ).show()
    }
    if (selected.count() >= groupSizeLimit) { // Minus one because we're going to include self later
        return Toast.makeText(context, R.string.activity_create_closed_group_too_many_group_members_error, Toast.LENGTH_LONG).show()
    }
    val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
    MessageSender.createClosedGroup(name, selected + setOf(userPublicKey)).successUi { groupID ->
        val threadID = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(
            Recipient.from(context, Address.fromSerialized(groupID), false))
        if (!activity!!.isFinishing) {
            openConversationActivity(threadID, Recipient.from(context, Address.fromSerialized(groupID), false),activity)
            activity.finish()
        }
    }.failUi {
        Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CreateSecretGroupScreenPreview() {
    BChatPreviewContainer {
        CreateSecretGroup(
            searchQuery = "",
            contacts = emptyList(),
            selectedContact = emptyList(),
            onEvent = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CreateSecretGroupScreenPreviewLight() {
    BChatPreviewContainer {
        CreateSecretGroup(
            searchQuery = "",
            contacts = emptyList(),
            selectedContact = emptyList(),
            onEvent = {}
        )
    }
}



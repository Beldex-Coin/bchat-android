package com.thoughtcrimes.securesms.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun BlockedContactScreen(
    blockedList: List<Recipient>,
    unBlockSingleContact: (Recipient) -> Unit,
    unBlockMultipleContacts: (List<Recipient>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        blockedList.forEach { contact ->
            BlockedContactItem(
                contact = contact,
                unblockContact = {
                    unBlockSingleContact(contact)
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BlockedContactItem(
    contact: Recipient,
    unblockContact: () -> Unit,
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
                .padding(16.dp)
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

@Preview
@Composable
fun BlockedContactScreenPreview() {
    BChatTheme {
        BlockedContactScreen(
            blockedList = emptyList(),
            unBlockSingleContact = {},
            unBlockMultipleContacts = {},
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
            unBlockSingleContact = {},
            unBlockMultipleContacts = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}
package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.TextColor

@Composable
fun SharedContactView(
    contacts: List<ContactModel>,
    timeStamp: String,
    timeStampColor: Color,
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    backgroundColor: Color = Color.DarkGray,
    titleColor: Color = Color.White,
    subtitleColor: Color = TextColor,
    isQuoted: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = columnModifier
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors =CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            modifier = modifier
        ) {
            SharedContactContent(
                contacts = contacts,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Text(
            text = timeStamp,
            style = MaterialTheme.typography.bodySmall.copy(
                color = timeStampColor,
                fontSize = 11.sp
            ),
            modifier = Modifier
                .padding(
                    end = 8.dp
                )
        )
    }
}

@Composable
fun SharedContactContent(
    contacts: List<ContactModel>,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.White,
    subtitleColor: Color = TextColor,
) {
    val addressString by remember(contacts) {
        var address = "aaaaaaaaaaa.........zzzzzzz"
        if (contacts.isNotEmpty()) {
            val address0 = contacts[0].address.serialize()
            address = address0.substring(0, 7)
            if (contacts.size > 1) {
                val lastAddress = contacts.last().address.serialize()
                address = "$address.........${lastAddress.takeLast(7)}"
            } else {
                address = "$address.........${address0.takeLast(7)}"
            }
        }
        mutableStateOf(address)
    }
    Row(
        modifier = modifier
            .padding(
                8.dp
            )
    ) {
        Column(
            modifier = Modifier
                .weight(0.8f)
        ) {
            var contactName = "No Name"
            if (contacts.isNotEmpty()) {
                contactName = if (contacts.size == 1)
                    contacts[0].name
                else
                    "${contacts[0].name} and ${contacts.size -1} others"
            }
            Text(
                contactName,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = titleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = subtitleColor,
                    modifier = Modifier
                        .size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    addressString,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = subtitleColor
                    ),
                )
            }

        }

        Spacer(modifier = Modifier.width(12.dp))

        if (contacts.isNotEmpty()) {
            val contact = contacts[0]
            key(contact.address.serialize()) {
                ProfilePictureComponent(
                    publicKey = contact.address.serialize(),
                    displayName = contact.name,
                    containerSize = 36.dp,
                    pictureMode = ProfilePictureMode.SmallPicture
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.Green,
                        shape = RoundedCornerShape(100)
                    )
            )
        }
    }
}

@Preview
@Composable
private fun SharedContactViewPreview() {
    BChatTheme {
        SharedContactView(
            contacts = listOf(),
            timeStampColor = Color.White,
            timeStamp = ""
        )
    }
}
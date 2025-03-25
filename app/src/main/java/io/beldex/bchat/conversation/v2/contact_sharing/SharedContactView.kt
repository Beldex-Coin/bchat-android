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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beldex.libbchat.utilities.Address
import io.beldex.bchat.compose_utils.BChatTheme

@Composable
fun SharedContactView(
    contacts: List<ContactModel>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    16.dp
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(0.8f)
            ) {
                Text(
                    if (contacts.size == 1)
                        contacts[0].name
                    else
                        "${contacts[0].name} and ${contacts.size -1} others",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "32456ghfhdsmbnlgjkhjklrg",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.Green,
                        shape = RoundedCornerShape(100)
                    )
            )
//            if (contact != null) {
//                ProfilePictureComponent(
//                    publicKey = contact.recipient.address.toString(),
//                    displayName = contact.recipient.name.toString(),
//                    containerSize = 36.dp,
//                    pictureMode = ProfilePictureMode.SmallPicture
//                )
//            }
        }
    }
}

@Preview
@Composable
private fun SharedContactViewPreview() {
    BChatTheme {
        SharedContactView(
            contacts = listOf(
                ContactModel(
                    threadId = 1L,
                    address = Address.fromSerialized("wqertyui313245678iuyjhgnbv"),
                    name = "vijay1"
                ),
                ContactModel(
                    threadId = 1L,
                    address = Address.fromSerialized("wqertyui313245678iuyjhgnbv"),
                    name = "vijay2"
                ),
                ContactModel(
                    threadId = 1L,
                    address = Address.fromSerialized("wqertyui313245678iuyjhgnbv"),
                    name = "vijay3"
                )
            )
        )
    }
}
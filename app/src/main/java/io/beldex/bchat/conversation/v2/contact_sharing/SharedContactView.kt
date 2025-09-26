package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OutlineLight
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.TextColor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Composable
fun SharedContactView(
    contacts: List<ContactModel>,
    timeStamp: String,
    timeStampColor: Color,
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    backgroundColor: Color = Color.DarkGray,
    titleColor: Color = Color.White,
    subtitleColor: Color = OutlineLight,
    isQuoted: Boolean = false
) {
    val numberOfContacts = flattenData(contacts[0].address.serialize())

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
                backgroundColor = backgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Row(
            modifier = modifier
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

                Row(
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Text(
                        text= if(numberOfContacts.size > 1) stringResource(R.string.view_all) else stringResource(R.string.chat),
                        style=MaterialTheme.typography.bodySmall.copy(
                            color=timeStampColor,
                            fontSize=16.sp
                        )
                    )

                    Icon(
                        imageVector=Icons.Default.ChevronRight, // or your custom drawable
                        contentDescription="view all",
                        tint=timeStampColor,
                        modifier=Modifier
                            .size(18.dp)
                            .padding(start=2.dp)
                    )
                }

            Text(
                text = timeStamp,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = timeStampColor,
                    fontSize = 11.sp
                ),
                modifier = Modifier.
                padding(top = 2.dp)
            )
        }


    }
}

@Composable
fun SharedContactContent(
    contacts: List<ContactModel>,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.White,
    backgroundColor : Color,
    subtitleColor: Color = TextColor,
) {
    val names = if (contacts.isNotEmpty()) {
        flattenData(contacts[0].name)
    } else {
        emptyList()
    }

    val addresses = if (contacts.isNotEmpty()) {
        flattenData(contacts[0].address.serialize())
    } else {
        emptyList()
    }

    val displayName = when {
        names.size > 2 -> "${names.first()} and ${names.size - 1} others"
        names.size == 2 -> "${names[0]} and ${names[1]}"
        names.size == 1 -> names.first()
        else -> addresses[0]
    }

    val addressString by remember(contacts) {
        var address = "aaaaaaaaaaa.........zzzzzzz"
        if (addresses.isNotEmpty()) {
            val address0 = addresses[0]
            address = address0.take(7)
            if (addresses.size > 1) {
                val lastAddress = addresses.last()
                address = "$address.........${lastAddress.takeLast(7)}"
            } else {
                address = "$address.........${address0.takeLast(7)}"
            }
        }
        mutableStateOf(address)
    }
    Row(
        modifier = modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(0.8f)
        ) {

            Text(
                text = displayName.ifEmpty { addresses[0] },
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
                   painter = painterResource(R.drawable.ic_contact_person),
                    contentDescription = "shared contact person",
                    tint = subtitleColor,
                    modifier = Modifier.size(16.dp)
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

        if (addresses.isNotEmpty() && names.isNotEmpty()) {
            val multiContact: Boolean = addresses.size > 1
            Box {
                if (multiContact) {
                    ProfilePictureComponent(
                        publicKey = addresses[1],
                        displayName = names[1],
                        containerSize = 30.dp,
                        pictureMode = ProfilePictureMode.SmallPicture,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                    )
                }
                ProfilePictureComponent(
                    publicKey = addresses[0],
                    displayName = names[0],
                    containerSize = 36.dp,
                    pictureMode = ProfilePictureMode.SmallPicture,
                    modifier = Modifier.then(
                        if (multiContact) {
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = backgroundColor,
                                    shape = CircleShape
                                )
                        } else {
                            Modifier
                        }
                    )
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


fun flattenData(json: String): List<String> {
    return try {
        if (json.trim().startsWith("[[")) {
            val nested: List<List<String>> = Json.decodeFromString(json)
            nested.firstOrNull()?.map { it.trim() } ?: emptyList()
        } else {
            val list: List<String> = Json.decodeFromString(json)
            list.map { it.trim() }
        }
    } catch (e: Exception) {
        emptyList()
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
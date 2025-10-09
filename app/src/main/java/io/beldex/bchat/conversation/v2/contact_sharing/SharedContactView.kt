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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OutlineLight
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.TextColor
import io.beldex.bchat.dependencies.DatabaseComponent

@Composable
fun SharedContactView(
    contacts: List<ContactModel>,
    timeStamp: String,
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    backgroundColor: Color = Color.DarkGray,
    titleColor: Color = Color.White,
    subtitleColor: Color = OutlineLight,
    isQuoted: Boolean = false,
    isOutgoing: Boolean = false,
    searchQuery: String = ""
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
                    .fillMaxWidth(),
                searchQuery = searchQuery,
                isOutgoing = isOutgoing
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
                        text= if(numberOfContacts.size > 1) stringResource(R.string.view_all) else stringResource(R.string.message),
                        style=MaterialTheme.typography.bodySmall.copy(
                            color= colorResource(if(isOutgoing) R.color.view_all_text_out else R.color.view_all_text),
                            fontSize=16.sp
                        )
                    )

                    Icon(
                        imageVector=Icons.Default.ChevronRight,
                        contentDescription="view all",
                        tint=colorResource(if(isOutgoing) R.color.view_all_text_out else R.color.view_all_text),
                        modifier=Modifier
                            .size(18.dp)
                            .padding(start=2.dp)
                    )
                }

            Text(
                text = timeStamp,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colorResource(if(isOutgoing) R.color.timestamp_out else R.color.timestamp_in),
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
    searchQuery: String = "",
    isOutgoing: Boolean = false,
) {
    val context = LocalContext.current

    data class ContactDisplay(val name: String, val address: String)

    val contactList: List<ContactDisplay> = contacts.firstOrNull()?.let { contact ->
        val names = flattenData(contact.name)
        val addresses = flattenData(contact.address.serialize())
        names.zip(addresses) { name, address -> ContactDisplay(name, address) }
    } ?: emptyList()

    val displayName = when (contactList.size) {
        0 -> "No Name"
        1 -> contactList[0].name.capitalizeFirstLetter()
        2 -> "${contactList[0].name.capitalizeFirstLetter()} and ${contactList[1].name.capitalizeFirstLetter()}"
        else -> "${contactList[0].name.capitalizeFirstLetter()} and ${contactList.size - 1} others"
    }

    val addressString by remember(contactList) {
        val address = if (contactList.isNotEmpty()) {
            val first = contactList.first().address
            val last = contactList.last().address
            "${first.take(7)}.........${last.takeLast(7)}"
        } else ""
        mutableStateOf(address)
    }

    fun getUserIsBNSHolderStatus(publicKey: String): Boolean? {
        return DatabaseComponent.get(context)
            .bchatContactDatabase()
            .getContactWithBchatID(publicKey)
            ?.isBnsHolder
    }

    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.8f)) {
            val annotatedDisplayName = highlightText(
                fullText = displayName.ifEmpty { contactList.firstOrNull()?.address.orEmpty() },
                query = searchQuery,
                highlightBackground = if(isOutgoing) Color(0xFF000000) else Color(0xFF4B4B64)
            )

            Text(
                text = annotatedDisplayName,
                style = MaterialTheme.typography.titleMedium.copy(color = if (searchQuery.isBlank()){ titleColor }else{ if(isOutgoing) { titleColor } else { Color(0xFFFFFFFF) } }),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (contactList.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_contact_person),
                        contentDescription = "shared contact person",
                        tint = subtitleColor,
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = addressString,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(color = subtitleColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (contactList.isNotEmpty()) {
            val multiContact = contactList.size > 1
            Box {
                contactList.getOrNull(1)?.let { second ->
                    key(second.address) {
                        ProfilePictureComponent(
                            publicKey=second.address,
                            displayName=second.name,
                            containerSize=30.dp,
                            pictureMode=ProfilePictureMode.SmallPicture,
                            modifier=Modifier
                                .align(Alignment.TopEnd)
                                .offset(x=3.dp, y=(-3).dp)
                        )
                    }
                }

                contactList.getOrNull(0)?.let { first ->
                    key(first.address) {
                        ProfilePictureComponent(
                            publicKey=first.address,
                            displayName=first.name,
                            containerSize=36.dp,
                            pictureMode=ProfilePictureMode.SmallPicture,
                            modifier=Modifier.then(
                                if (multiContact && getUserIsBNSHolderStatus(first.address) != true) {
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, backgroundColor, CircleShape)
                                } else Modifier
                            )
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Green, RoundedCornerShape(100))
            )
        }
    }
}

fun highlightText(
    fullText: String,
    query: String,
    highlightBackground: Color
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(fullText)

    val lowerFullText = fullText.lowercase()
    val lowerQuery = query.lowercase()

    return buildAnnotatedString {
        var startIndex = 0
        while (true) {
            val index = lowerFullText.indexOf(lowerQuery, startIndex)
            if (index == -1) {
                append(fullText.substring(startIndex))
                break
            }
            append(fullText.substring(startIndex, index))
            withStyle(
                style = SpanStyle(
                    background = highlightBackground
                )
            ) {
                append(fullText.substring(index, index + query.length))
            }
            startIndex = index + query.length
        }
    }
}



@Preview
@Composable
private fun SharedContactViewPreview() {
    BChatTheme {
        SharedContactView(
            contacts = listOf(),
            timeStamp = ""
        )
    }
}
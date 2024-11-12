package io.beldex.bchat.wallet.addressbook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.conversation.v2.utilities.MentionManagerUtilities
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.ContactUtilities
import io.beldex.bchat.R

@SuppressLint("MutableCollectionMutableState")
@Composable
fun AddressBookScreen(
) {

    val context = LocalContext.current

    val fromSendScreen by remember {
        mutableStateOf(TextSecurePreferences.getSendAddress(context))
    }

    fun listOfContacts(context: Context): List<String> {
        val contacts = ContactUtilities.getAllContacts(context)
        return contacts.filter { it.isApproved }.map { it.address.toString() }
    }

    fun getBeldexAddress(publicKey: String): String {
        val contact = DatabaseComponent.get(context).bchatContactDatabase()
            .getContactWithBchatID(publicKey)
        return contact?.displayBeldexAddress(Contact.ContactContext.REGULAR) ?: ""
    }

    fun getContactList(): List<String> {
        return listOfContacts(context).filter {
            getBeldexAddress(it).isNotEmpty()
        }
    }

    var listOfContacts by remember {
        mutableStateOf(getContactList())
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    fun getUserDisplayName(publicKey: String): String {
        val contact = DatabaseComponent.get(context).bchatContactDatabase()
            .getContactWithBchatID(publicKey)
        return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
    }

    fun filterContacts() {
        listOfContacts = getContactList().filter {
            if (searchQuery.isNotEmpty()) {
                getUserDisplayName(it).lowercase().contains(searchQuery.lowercase())
            } else {
                it.isNotEmpty()
            }
        }.toMutableList()
    }

    fun copyAddress(beldexAddress: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Beldex Address", beldexAddress)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun redirectToSend(beldexAddress: String, context: Context) {
        val returnIntent = Intent()
        returnIntent.putExtra("address_value", beldexAddress)
        (context as Activity).setResult(RESULT_OK, returnIntent)
        context.finish()
    }

    Card(
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
                .padding(16.dp)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                filterContacts()
            },
            placeholder = {
                Text(
                    text = stringResource(id = R.string.search_contact),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.appColors.inputHintColor
                    )
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            colors = TextFieldDefaults.colors(
                disabledTextColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.appColors.searchBackground,
                unfocusedContainerColor = MaterialTheme.appColors.searchBackground,
                cursorColor = MaterialTheme.appColors.primaryButtonColor,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.appColors.primaryButtonColor,
                    backgroundColor = MaterialTheme.appColors.primaryButtonColor
                )
            ),
            trailingIcon = {
                IconButton(onClick = {
                    if (searchQuery.isNotEmpty()) {
                        searchQuery = ""
                        filterContacts()
                    }
                }) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = "Search and Clear Icon",
                        tint = MaterialTheme.appColors.iconTint,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
    if(searchQuery.isNotEmpty() && listOfContacts.isEmpty()) {
        Card(
                colors=CardDefaults.cardColors(
                        containerColor=MaterialTheme.appColors.searchBackground
                ),
                shape=RoundedCornerShape(50),
                modifier=Modifier
                        .fillMaxWidth()
                        .padding(horizontal=16.dp)
        ) {

            Text(
                    text=stringResource(id=R.string.no_contacts),
                    style=MaterialTheme.typography.titleMedium.copy(
                            color=MaterialTheme.appColors.editTextHint,
                            fontSize=14.sp,
                            fontWeight=FontWeight.Normal,
                            textAlign=TextAlign.Center
                    ),
                    modifier=Modifier.padding(horizontal=16.dp, vertical=16.dp)
            )
        }
    }

    if (listOfContacts.isEmpty() && searchQuery.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.drawable.ic_no_addresses),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(color = MaterialTheme.appColors.noAddressIcon)
                )

                Text(
                    text = stringResource(id = R.string.no_addresses),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.editTextHint,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 20.dp)
                )

                Text(
                    text = stringResource(id = R.string.save_address_to_show),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.appColors.walletSyncingSubTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 10.dp)
                )

            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(listOfContacts) { index, item ->

                    val recipient = Recipient.from(
                        context,
                        Address.fromSerialized(item), false
                    )
                    val threadID = DatabaseComponent.get(context).threadDatabase()
                        .getOrCreateThreadIdFor(recipient)
                    MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(
                        threadID,
                        context
                    )
                    val address = recipient.address.serialize()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,

                            ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.appColors.settingsCardBackground
                                ),
                                shape = RoundedCornerShape(9.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(10.dp)
                                    .clickable {
                                    }
                            ) {

                                Text(
                                    text = getUserDisplayName(address),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight(400)
                                    ),
                                    modifier = Modifier
                                        .padding(all = 12.dp),

                                    )
                            }
                            if (fromSendScreen) {
                                Box(
                                    modifier = Modifier
                                        .width(38.dp)
                                        .height(40.dp)
                                        .background(
                                            color = MaterialTheme.appColors.primaryButtonColor,
                                            shape = RoundedCornerShape(9.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Image(
                                        painter = painterResource(id = R.drawable.ic_copy_address),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .clickable {
                                                copyAddress(getBeldexAddress(address))
                                            })
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .width(38.dp)
                                        .height(40.dp)
                                        .background(
                                            color = MaterialTheme.appColors.cardBackground,
                                            shape = RoundedCornerShape(9.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {


                                    Image(
                                        painter = painterResource(id = R.drawable.wallet_send),
                                        contentDescription = "",
                                        colorFilter = ColorFilter.tint(
                                            color = MaterialTheme.appColors.primaryButtonColor
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                redirectToSend(getBeldexAddress(address), context)

                                            })
                                }
                            }
                        }
                        Text(
                            text = getBeldexAddress(address),
                            style = BChatTypography.bodySmall.copy(
                                color = MaterialTheme.appColors.secondaryTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight(600)
                            ),
                            modifier = Modifier
                                .padding(horizontal = 15.dp)

                        )
                    }
                }
            }

        }
    }
}


@Preview
@Composable
fun AddressBookScreenPreview() {
    BChatTheme {
        AddressBookScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddressBookScreenPreviewDark() {
    BChatTheme {
        AddressBookScreen()
    }
}


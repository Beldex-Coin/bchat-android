package com.thoughtcrimes.securesms.wallet.addressbook

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import io.beldex.bchat.R

@Composable
fun AddressBookScreen(
        contactsList: List<String>,
        searchFilter: String,
        modifier: Modifier=Modifier
) {


    Column(
            verticalArrangement=Arrangement.spacedBy(8.dp),
            modifier=modifier
    ) {
        LazyColumn(
                verticalArrangement=Arrangement.spacedBy(16.dp),
                modifier=Modifier
                        .fillMaxWidth()
                        .weight(1f)
        ) {
            items(
                    count=contactsList.size,
                    key={
                        contactsList[it]
                    }
            ) { index ->
                val contactListIndex=contactsList[index]
                ListOfContactsItem(
                        contactsList=contactListIndex,
                        searchFilter=searchFilter,
                        modifier=Modifier
                                .fillMaxWidth()
                )
            }
        }

    }
}


@Composable
fun ListOfContactsItem(
        contactsList: String,
        searchFilter: String,
        modifier: Modifier=Modifier
) {
    val context=LocalContext.current

    val fromSendScreen by remember {
        mutableStateOf(TextSecurePreferences.getSendAddress(context))
    }


    println("list of contacts value on Item $contactsList")

    fun getBeldexAddress(publicKey: String): String {
        val contact=DatabaseComponent.get(context).bchatContactDatabase()
                .getContactWithBchatID(publicKey)
        return contact?.displayBeldexAddress(Contact.ContactContext.REGULAR) ?: publicKey
    }

    fun getUserDisplayName(publicKey: String): String {
        val contact=DatabaseComponent.get(context).bchatContactDatabase()
                .getContactWithBchatID(publicKey)
        return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
    }

    fun copyAddress(beldexAddress: String) {
        val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip=ClipData.newPlainText("Beldex Address", beldexAddress)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun redirectToSend(beldexAddress: String, context: Context) {
        val returnIntent=Intent()
        returnIntent.putExtra("address_value", beldexAddress)
        (context as Activity).setResult(RESULT_OK, returnIntent)
        context.finish()
    }

    Column(
            modifier=Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {


        Row(
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.Center,

                ) {
            Card(
                    colors=CardDefaults.cardColors(
                            containerColor=MaterialTheme.appColors.settingsCardBackground
                    ),
                    shape=RoundedCornerShape(9.dp),
                    elevation=CardDefaults.cardElevation(
                            defaultElevation=4.dp
                    ),
                    modifier=Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(10.dp)
                            .clickable {
                            }
            ) {

                Text(
                        text=getUserDisplayName(contactsList),
                        style=BChatTypography.titleMedium.copy(
                                color=MaterialTheme.appColors.textColor,
                                fontSize=14.sp,
                                fontWeight=FontWeight(700)
                        ),
                        modifier=Modifier
                                .padding(all=12.dp),

                        )


            }
            if (fromSendScreen) {
                Box(
                        modifier=Modifier
                                .width(38.dp)
                                .height(38.dp)
                                .padding(10.dp)
                                .background(
                                        color=MaterialTheme.appColors.primaryButtonColor,
                                        shape=RoundedCornerShape(9.dp)
                                ),
                        contentAlignment=Alignment.Center
                ) {

                    Image(
                            painter=painterResource(id=R.drawable.copy_icon),
                            contentDescription="",
                            modifier=Modifier
                                    .clickable {
                                        copyAddress(getBeldexAddress(contactsList))
                                    })
                }

            } else {
                Box(
                        modifier=Modifier
                                .width(38.dp)
                                .height(38.dp)
                                .background(
                                        color=MaterialTheme.appColors.cardBackground,
                                        shape=RoundedCornerShape(9.dp)
                                ),
                        contentAlignment=Alignment.Center
                ) {


                    Image(
                            painter=painterResource(id=R.drawable.share),
                            contentDescription="",
                            modifier=Modifier
                                    .clickable {
                                        redirectToSend(getBeldexAddress(contactsList), context)

                                    })
                }
            }
        }
        Text(
                text=getBeldexAddress(contactsList),
                style=BChatTypography.bodySmall.copy(
                        color=MaterialTheme.appColors.secondaryTextColor,
                        fontSize=12.sp,
                        fontWeight=FontWeight(600)
                ),
                modifier=Modifier
                        .padding(horizontal=15.dp)

        )
    }
}


@Preview
@Composable
fun AddressBookScreenPreview() {
    BChatTheme {
        AddressBookScreen(
                contactsList=emptyList(),
                searchFilter="",
                modifier=Modifier
                        .fillMaxSize()
                        .padding(16.dp))
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddressBookScreenPreviewDark() {
    BChatTheme {
        AddressBookScreen(
                contactsList=emptyList(),
                searchFilter=" ",
                modifier=Modifier
                        .fillMaxSize()
                        .padding(16.dp))
    }
}


@Preview
@Composable
fun ListOfContactItemPreview() {
    ListOfContactsItem(
            contactsList="abc",
            searchFilter="",
            modifier=Modifier
                    .fillMaxSize()
                    .padding(16.dp))
}

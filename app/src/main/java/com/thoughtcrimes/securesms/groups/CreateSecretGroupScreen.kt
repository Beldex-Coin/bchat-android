package com.thoughtcrimes.securesms.groups

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.groupSizeLimit
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.compose_utils.BChatOutlinedTextField
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

@AndroidEntryPoint
class CreateSecretGroupScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme() {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CreateSecretGroup()
                }
            }
        }
    }
}

@Composable
fun CreateSecretGroup() {

    var groupName by remember {
        mutableStateOf("")
    }
    var searchFilter by remember {
        mutableStateOf("")
    }


    val context = LocalContext.current
    val activity = (context as? Activity)

    var contacts by remember {
        mutableStateOf(emptyList<String>())
    }
    val owner = LocalLifecycleOwner.current
    val contactViewModel: CreateSecretGroupViewModel = hiltViewModel()

    contactViewModel.recipients.observe(owner) { recipients ->
        contacts = recipients.map { it.address.serialize() }
    }

    println("list of contact $contacts")

    val selectedContact by remember {
        mutableStateOf(emptyList<String>())
    }
    


    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.screen_background))
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)

        ) {
            Text(
                text = "Create Secret Group",
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor
                ),
                fontSize = 22.sp
            )
            TextField(
                value = groupName,
                placeholder = { Text(text = "Enter Group Name") },
                onValueChange = {
                    groupName = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = colorResource(id = R.color.your_bchat_id_bg),
                    focusedContainerColor = colorResource(id = R.color.your_bchat_id_bg),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = colorResource(id = R.color.button_green)
                ),
            )

        }
        Divider(
            color = colorResource(id = R.color.divider_color),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.5f))

        BChatOutlinedTextField(
            value = searchFilter,
            onValueChange = { searchFilter = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            label = "Search Contact",
            shape = RoundedCornerShape(36.dp),
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_baseline_search_24),
                    contentDescription = ""
                )
            }
        )
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .weight(1f)
        ) {
            println("members list ${contactViewModel.recipients.value}")
            contactViewModel.recipients.value?.let {
                items(it.size) { i ->
                    OutlinedCard(
                        modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                    ) {

                        var selectedContact by remember {
                            mutableStateOf(false)
                        }

                        fun onMemberClick(member: String) {
                            println("list value of selected contact index ${selectedContact.toString()}")

                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_camera_profile_pic),
                                contentDescription = ""

                            )
                            println("members list name ${it[i].name}")
                            Text(
                                text = it[i].name.toString(),
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .padding(all = 10.dp)
                                    .weight(1f)
                            )
                            Checkbox(
                                checked = selectedContact,
                                onCheckedChange = {
                                    selectedContact = !selectedContact
                                    onMemberClick(contactViewModel.recipients.value!![i].toString())
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colorResource(id = R.color.text),
                                    uncheckedColor = colorResource(id = R.color.text),
                                    checkmarkColor = colorResource(id = R.color.button_green)
                                ),
                                modifier = Modifier
                            )
                        }
                    }

                }
            }

        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(colorResource(id = R.color.your_bchat_id_bg)),
            contentAlignment = Alignment.Center,
        ) {
            PrimaryButton(
                onClick = {
                          createClosedGroup(groupName,context,activity,selectedContact)
                    // context.startActivity(Intent(context, OnBoardingActivity::class.java))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "Create",
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

private fun openConversationActivity(threadId: Long, recipient: Recipient, activity: Activity) {
    val returnIntent = Intent()
    returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,threadId)
    returnIntent.putExtra(ConversationFragmentV2.ADDRESS,recipient.address)
    activity.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CreateSecretGroupScreenPreview() {
    BChatTheme() {
        CreateSecretGroup()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CreateSecretGroupScreenPreviewLight() {
    BChatTheme {
        CreateSecretGroup()
    }
}



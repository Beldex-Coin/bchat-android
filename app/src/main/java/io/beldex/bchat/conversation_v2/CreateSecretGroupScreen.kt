package io.beldex.bchat.conversation_v2

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.groupSizeLimit
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.Device
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.compose_utils.BChatCheckBox
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

@Composable
fun CreateSecretGroup(
    searchQuery: String,
    contacts: List<Recipient>,
    selectedContact: List<String>,
    onEvent: (SecretGroupEvents) -> Unit,
    activity: NewGroupConversationActivity
) {
    var groupName by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    val device: Device = Device.ANDROID
    val keyboardController = LocalSoftwareKeyboardController.current
    if (TextSecurePreferences.isScreenSecurityEnabled(context))
        activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else {
        activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    var showLoader by remember {
        mutableStateOf(false)
    }
    val composition by rememberLottieComposition(
        LottieCompositionSpec
            .RawRes(R.raw.load_animation)
    )
    val isPlaying by remember {
        mutableStateOf(true)
    }
    // for speed
    val speed by remember {
        mutableFloatStateOf(1f)
    }
    var isButtonEnabled by remember {
        mutableStateOf(true)
    }
    val scope = rememberCoroutineScope()

    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying,
        speed = speed,
        restartOnPlay = false
    )

    var updateProfile by remember {
        mutableStateOf(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.ime.asPaddingValues())
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 5.dp, start = 16.dp, end = 16.dp)
            ) {
                TextField(
                    value = groupName,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.enter_group_name),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onValueChange = {
                        groupName = it
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                        focusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                        cursorColor = colorResource(id = R.color.button_green)
                    )
                )
            }

            Divider(
                color = colorResource(id = R.color.divider_color),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
            )

            TextField(
                value = searchQuery,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_contact),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                onValueChange = {
                    updateProfile = !updateProfile
                    onEvent(SecretGroupEvents.SearchQueryChanged(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.appColors.textFiledBorderColor,
                        shape = RoundedCornerShape(36.dp)
                    ),
                shape = RoundedCornerShape(36.dp),
                trailingIcon = {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = "search contact and clear search text",
                        tint = MaterialTheme.appColors.iconTint,
                        modifier = Modifier.clickable {
                            if(searchQuery.isNotEmpty()){
                                onEvent(SecretGroupEvents.SearchQueryChanged(""))
                            }
                        }
                    )
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                    focusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                    cursorColor = colorResource(id = R.color.button_green)
                )
            )
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
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
                            onEvent(
                                SecretGroupEvents.RecipientSelectionChanged(
                                    contact,
                                    isSelected
                                )
                            )
                        },
                        updateProfile = updateProfile,
                        modifier = Modifier
                            .fillMaxWidth()
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
                        if(isButtonEnabled) {
                            keyboardController?.hide()
                            isButtonEnabled = false
                            scope.launch(Dispatchers.Main) {
                                if(CheckOnline.isOnline(context)) {
                                    createClosedGroup(device, groupName.trim(), context, activity, selectedContact, showLoader={
                                        showLoader=it
                                    })
                                }else {
                                    Toast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                                }
                                delay(2000)
                                isButtonEnabled = true
                            }
                        }


                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = groupName.isNotEmpty(),
                    disabledContainerColor = MaterialTheme.appColors.disabledCreateButtonContainer,
                ) {
                    Text(
                        text = stringResource(id = R.string.create),
                        style = BChatTypography.bodyLarge.copy(
                            color = if (groupName.isNotEmpty()) {
                                Color.White
                            } else {
                                MaterialTheme.appColors.disabledButtonContent
                            },
                            fontWeight = FontWeight(400),
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }
            }
        }
        if (showLoader) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.appColors.loaderBackground.copy(alpha = 0.5f))
                    .clickable(
                        enabled = true,
                        onClick = {

                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition,
                    progress,
                    modifier = Modifier.size(70.dp)
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
    updateProfile: Boolean = true,
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
            modifier= modifier
                .padding(bottom = 10.dp)
                .clickable {
                    onSelectionChanged(recipient, !isSelected)
                }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                    .padding(vertical = 4.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                if(updateProfile) {
                    ProfilePictureComponent(
                        publicKey = recipient.address.toString(),
                        displayName = recipient.name.toString(),
                        containerSize = 36.dp,
                        pictureMode = ProfilePictureMode.SmallPicture
                    )
                }else {
                    ProfilePictureComponent(
                        publicKey = recipient.address.toString(),
                        displayName = recipient.name.toString(),
                        containerSize = 36.dp,
                        pictureMode = ProfilePictureMode.SmallPicture
                    )
                }
            }

            Text(
                text = if(recipient.name != null) recipient.name.toString() else recipient.address.toString(),
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
                fontWeight = FontWeight(400),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Image(painter = painterResource(id = if(isSelected) R.drawable.ic_checkedbox else R.drawable.ic_checkbox), contentDescription = "check box", modifier = Modifier.padding(end = 25.dp))
        }
    }
}

fun getUserDisplayName(publicKey: String, context: Context): String {
    val contact =
        DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
}

private fun createClosedGroup(
    device: Device,
    name: String,
    context: Context,
    activity: Activity?,
    selected: Collection<String>,
    showLoader: (status : Boolean) -> Unit
) {
    if (name.isEmpty()) {
        return Toast.makeText(
            context,
            R.string.activity_create_closed_group_group_name_missing_error,
            Toast.LENGTH_LONG
        ).show()
    }
    else if (name.length >= 26) {
        return Toast.makeText(
            context,
            R.string.activity_create_closed_group_group_name_too_long_error,
            Toast.LENGTH_LONG
        ).show()
    }
    else if (selected.isEmpty()) {
        return Toast.makeText(
            context,
            R.string.activity_create_closed_group_not_enough_group_members_error,
            Toast.LENGTH_LONG
        ).show()
    }
    else if (selected.count() >= groupSizeLimit) { // Minus one because we're going to include self later
        return Toast.makeText(
            context,
            R.string.activity_create_closed_group_too_many_group_members_error,
            Toast.LENGTH_LONG
        ).show()
    }else {
        showLoader(true)
        val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
        MessageSender.createClosedGroup(device, name, selected + setOf(userPublicKey))
            .successUi { groupID ->
                val threadID =
                    DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(
                        Recipient.from(context, Address.fromSerialized(groupID), false)
                    )
                if (!activity!!.isFinishing) {
                    openConversationActivity(
                        threadID,
                        Recipient.from(context, Address.fromSerialized(groupID), false),
                        activity
                    )
                    showLoader(false)
                    activity.finish()
                }
            }.failUi {
                showLoader(false)
            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }
}


/*@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
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
}*/



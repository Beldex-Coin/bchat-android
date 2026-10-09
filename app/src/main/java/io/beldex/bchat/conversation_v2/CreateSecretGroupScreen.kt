package io.beldex.bchat.conversation_v2

import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ColorFilter
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
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter
import io.beldex.bchat.util.isValidGroupName
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
    var groupName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val device: Device = Device.ANDROID
    val keyboardController = LocalSoftwareKeyboardController.current
    var showLoader by remember { mutableStateOf(false) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.load_animation))
    val isPlaying by remember { mutableStateOf(true) }
    val speed by remember { mutableFloatStateOf(1f) }
    var isButtonEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying,
        speed = speed,
        restartOnPlay = false
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.ime.asPaddingValues())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                GroupNameField(
                    groupName = groupName,
                    onGroupNameChange = { groupName = it },
                    showLoader = showLoader
                )
                Divider(
                    color = MaterialTheme.appColors.dividerColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                )
                SearchField(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { onEvent(SecretGroupEvents.SearchQueryChanged(it)) },
                    showLoader = showLoader
                )
                if (contacts.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_doodle_3_1),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                        Text(
                            text = stringResource(R.string.no_contacts_yet),
                            fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                            color = MaterialTheme.appColors.homeSearchBarHint,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    contacts.forEach { recipient ->
                        GroupContact(
                            recipient = recipient,
                            isSelected = selectedContact.contains(recipient.address.toString()),
                            onSelectionChanged = { contact, isSelected ->
                                onEvent(SecretGroupEvents.RecipientSelectionChanged(contact, isSelected))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            CreateGroupButton(
                groupName = groupName,
                showLoader = showLoader,
                context = context,
                scope = scope,
                device = device,
                keyboardController = keyboardController,
                selectedContact = selectedContact,
                activity = activity,
                isButtonEnabled = isButtonEnabled,
                onButtonEnabledChange = { isButtonEnabled = it },
                onShowLoaderChange = { showLoader = it }
            )
        }

        if (showLoader) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.appColors.loaderBackground.copy(alpha = 0.5f))
                    .clickable(enabled = true, onClick = {}),
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
private fun GroupNameField(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    showLoader: Boolean
) {
    Box(
        modifier = Modifier.padding(bottom = 5.dp, start = 16.dp, end = 16.dp)
    ) {
        TextField(
            value = groupName,
            placeholder = {
                Text(
                    text = stringResource(R.string.enter_group_name).uppercase(),
                    fontFamily = RobotoMono,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                    fontSize = 12.sp,
                    color = MaterialTheme.appColors.homeSearchBarHint
                )
            },
            onValueChange = onGroupNameChange,
            enabled = !showLoader,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder),
            shape = androidx.compose.ui.graphics.RectangleShape,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.appColors.homeSearchBarBackground,
                focusedContainerColor = MaterialTheme.appColors.homeSearchBarBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                cursorColor = MaterialTheme.appColors.textSelectionColor
            )
        )
    }
}

@Composable
private fun SearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showLoader: Boolean
) {
    TextField(
        value = searchQuery,
        placeholder = {
            Text(
                text = stringResource(R.string.search_contact).uppercase(),
                fontFamily = RobotoMono,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                fontSize = 12.sp,
                color = MaterialTheme.appColors.homeSearchBarHint
            )
        },
        singleLine = true,
        enabled = !showLoader,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder),
        shape = androidx.compose.ui.graphics.RectangleShape,
        trailingIcon = {
            Icon(
                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Clear else Icons.Default.Search,
                contentDescription = "search contact and clear search text",
                tint = MaterialTheme.appColors.homeSearchBarHint,
                modifier = Modifier.clickable {
                    if (searchQuery.isNotEmpty()) {
                        onSearchQueryChange("")
                    }
                }
            )
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.appColors.homeSearchBarBackground,
            focusedContainerColor = MaterialTheme.appColors.homeSearchBarBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
            cursorColor = MaterialTheme.appColors.textSelectionColor
        )
    )
}

@Composable
private fun CreateGroupButton(
    groupName: String,
    showLoader: Boolean,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    device: Device,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    selectedContact: List<String>,
    activity: NewGroupConversationActivity,
    isButtonEnabled: Boolean,
    onButtonEnabledChange: (Boolean) -> Unit,
    onShowLoaderChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(color = MaterialTheme.appColors.homeBackground),
        contentAlignment = Alignment.Center
    ) {
        PrimaryButton(
            onClick = {
                if (isButtonEnabled) {
                    keyboardController?.hide()
                    onButtonEnabledChange(false)
                    scope.launch(Dispatchers.Main) {
                        if (CheckOnline.isOnline(context)) {
                            createClosedGroup(device, groupName.trim(), context, activity, selectedContact, showLoader = onShowLoaderChange)
                        } else {
                            Toast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                        }
                        delay(2000)
                        onButtonEnabledChange(true)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(54.dp),
            shape = io.beldex.bchat.compose_utils.notchedCornerShape(14.5.dp),
            enabled = groupName.isNotEmpty(),
            containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
            contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
            disabledContainerColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground,
            disabledContentColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledText
        ) {
            Text(
                text = stringResource(id = R.string.create),
                fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
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
    val publicKey = recipient.address.toString()

    val rememberedProfile by remember(publicKey) {
        mutableStateOf(publicKey)
    }

    Box(
        modifier = modifier
            .padding(bottom = 8.dp)
            .background(
                color = if (isSelected) MaterialTheme.appColors.homeRowBackground else Color.Transparent,
                shape = io.beldex.bchat.compose_utils.notchedCornerShape(12.dp)
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.appColors.homeRowTimestampUnread,
                    shape = io.beldex.bchat.compose_utils.notchedCornerShape(12.dp)
                ) else Modifier
            )
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
                ProfilePictureComponent(
                    publicKey = rememberedProfile,
                    displayName = recipient.name.toString(),
                    containerSize = 36.dp,
                    pictureMode = ProfilePictureMode.SmallPicture
                )
            }

            Text(
                text = recipient.name?.capitalizeFirstLetter()
                    ?: recipient.address.toString().capitalizeFirstLetter(),
                textAlign = TextAlign.Start,
                fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                color = MaterialTheme.appColors.homeRowTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Image(
                painter = painterResource(id = if (isSelected) R.drawable.ic_checkedbox else R.drawable.ic_checkbox),
                contentDescription = "check box",
                modifier = Modifier.padding(end = 16.dp),
                colorFilter = ColorFilter.tint(
                    if (isSelected) MaterialTheme.appColors.homeRowTimestampUnread
                    else MaterialTheme.appColors.homeRowTimestamp
                )
            )
        }
    }
}

fun getUserDisplayName(publicKey: String, context: Context): String {
    val contact =
        DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR, context) ?: publicKey
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
    }
    else if (!isValidGroupName(name)) {
        Toast.makeText(
            context,
            R.string.nickname_special_char_not_allowed,
            Toast.LENGTH_SHORT
        ).show()
    } else {
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



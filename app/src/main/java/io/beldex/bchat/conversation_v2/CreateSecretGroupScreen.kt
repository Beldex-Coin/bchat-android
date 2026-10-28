package io.beldex.bchat.conversation_v2

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.offset
import io.beldex.bchat.util.BChatToast
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.notchedCornerShape
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
    isLoading: Boolean,
    isCreateEnabled: Boolean,
    onCreateClick: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val showLoader = isLoading
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.load_animation))
    val isPlaying by remember { mutableStateOf(true) }
    val speed by remember { mutableFloatStateOf(1f) }

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
                modifier = Modifier.weight(1f)
            ) {
                GroupNameField(
                    groupName = groupName,
                    onGroupNameChange = { groupName = it },
                    showLoader = showLoader
                )
                // Figma: search + contact list live inside one hairline container under the name field.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
                        .padding(12.dp)
                ) {
                SearchField(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { onEvent(SecretGroupEvents.SearchQueryChanged(it)) },
                    showLoader = showLoader
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (contacts.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.no_contacts_empty_state),
                            contentDescription = null,
                            modifier = Modifier.size(width = 202.03.dp, height = 81.445.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_contacts_yet),
                            fontFamily = RobotoMono,
                            color = Color(0xFF555555),
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Only the contact list scrolls; the name field and search stay put (N22).
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(
                            count = contacts.size,
                            key = { index -> contacts[index].address.toString() }
                        ) { index ->
                            val recipient = contacts[index]
                            GroupContact(
                                recipient = recipient,
                                isSelected = selectedContact.contains(recipient.address.toString()),
                                onSelectionChanged = { contact, isSelected ->
                                    onEvent(SecretGroupEvents.RecipientSelectionChanged(contact, isSelected))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                }
            }
            CreateGroupButton(
                groupName = groupName,
                isButtonEnabled = isCreateEnabled,
                onClick = {
                    keyboardController?.hide()
                    onCreateClick(groupName)
                }
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
                io.beldex.bchat.compose_utils.BChatLoader(size = 44.dp)
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
                    color = MaterialTheme.appColors.onboardingInputHint
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
                unfocusedContainerColor = MaterialTheme.appColors.dialogBackground,
                focusedContainerColor = MaterialTheme.appColors.dialogBackground,
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
    // Figma: 48dp box, 15dp search icon 12dp from the edge, text 11dp after the icon.
    androidx.compose.foundation.text.BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        enabled = !showLoader,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = RobotoMono,
            fontSize = 14.sp,
            color = MaterialTheme.appColors.editTextColor
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.appColors.textSelectionColor),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.appColors.dialogBackground)
            .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_secret_group),
                    contentDescription = stringResource(R.string.search_contact),
                    tint = MaterialTheme.appColors.onboardingInputHint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(11.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_contact).uppercase(),
                            fontFamily = RobotoMono,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = MaterialTheme.appColors.onboardingInputHint
                        )
                    }
                    innerTextField()
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear),
                        tint = MaterialTheme.appColors.onboardingInputHint,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }
        }
    )
}

@Composable
private fun CreateGroupButton(
    groupName: String,
    isButtonEnabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        PrimaryButton(
            onClick = { if (isButtonEnabled) onClick() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(54.dp),
            shape = io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchLarge),
            enabled = groupName.isNotEmpty(),
            containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
            contentColor = MaterialTheme.appColors.backgroundColor,
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
                color = if (isSelected) MaterialTheme.appColors.dialogBackground else Color.Transparent,
                shape = io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium)
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.appColors.userDetailsConfirmBackground,
                    shape = io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium)
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
                color = MaterialTheme.appColors.editTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            io.beldex.bchat.util.CustomCheckBox(
                checked = isSelected,
                onCheckedChange = { onSelectionChanged(recipient, it) },
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}

fun getUserDisplayName(publicKey: String, context: Context): String {
    val contact =
        DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR, context) ?: publicKey
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



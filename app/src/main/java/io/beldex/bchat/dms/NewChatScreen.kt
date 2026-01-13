package io.beldex.bchat.dms

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.PublicKeyValidation
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter
import io.beldex.bchat.conversation_v2.NewChatScreenViewModel
import io.beldex.bchat.conversation_v2.OpenActivity
import io.beldex.bchat.conversation_v2.getUserDisplayName
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.wallet.CheckOnline
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    searchQuery: String,
    contacts: List<Recipient>,
    onEvent: (String) -> Unit,
    openActivity: (OpenActivity) -> Unit,
    openConversation: (Recipient) -> Unit,
    onBackPress: () -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSearchOption by remember {
        mutableStateOf(false)
    }

    var showNewChatPopup by remember {
        mutableStateOf(false)
    }

    var bnsLoader by remember {
        mutableStateOf(false)
    }

    val privateChatScanQRCodeActivityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val hexEncodedPublicKey = result.data!!.getStringExtra(ConversationFragmentV2.HEX_ENCODED_PUBLIC_KEY)
            val bnsName = result.data!!.getStringExtra(ConversationFragmentV2.BNS_NAME)
            if(hexEncodedPublicKey!=null) {
                createPrivateChat(hexEncodedPublicKey, context, bnsName.toString())
            }
        }
    }

    if(showNewChatPopup){
        NewChatPopUp(context = context, onDismiss = {
            showNewChatPopup = false
        }, onClick = {
            keyboardController?.hide()
        }, updateBnsLoader = {
            bnsLoader = it
        })
    }

    if(bnsLoader) {
        BnsLoadingPopUp(onDismiss = {
            bnsLoader = false

        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.new_chat_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.appColors.editTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBackPress()
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = stringResource(
                                id = R.string.back
                            ),
                            tint = MaterialTheme.appColors.editTextColor,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color=MaterialTheme.colorScheme.primary),
                actions = {
                    IconButton(onClick = {
                        if(showSearchOption){
                            onEvent("")
                        }
                        showSearchOption = !showSearchOption
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_search_contact),
                            "wallet settings",
                            tint = MaterialTheme.appColors.editTextColor
                        )
                    }
                }
            )
        },
        content = { it ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(it)
            )
            {
                if(showSearchOption) {
                    TextField(
                        value = searchQuery,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_people_and_groups),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        onValueChange = {
                            onEvent(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start=7.dp, end=7.dp)
                            .border(
                                width=1.dp,
                                color=MaterialTheme.appColors.textFiledBorderColor,
                                shape=RoundedCornerShape(36.dp)
                            ),
                        shape = RoundedCornerShape(36.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "clear search text",
                                tint = MaterialTheme.appColors.iconTint,
                                modifier=Modifier.clickable {
                                    if (searchQuery.isNotEmpty()) {
                                        onEvent("")
                                    } else {
                                        showSearchOption=false
                                    }
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "search contact",
                                tint = MaterialTheme.appColors.iconTint,
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                            focusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            selectionColors = TextSelectionColors(
                                MaterialTheme.appColors.textSelectionColor,
                                MaterialTheme.appColors.textSelectionColor
                            ),
                            cursorColor = colorResource(id = R.color.button_green)
                        )
                    )
                }
                NewChatItem(image = if(isDarkTheme) R.drawable.ic_new_chat else R.drawable.ic_new_chat_light, title = stringResource(id = R.string.activity_create_private_chat_title),MaterialTheme.appColors.textGreen, PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp),true, onClick = {
                    showNewChatPopup = !showNewChatPopup
                }, onClickScanQRCode = {
                    val intent = Intent(
                        context,
                        PrivateChatScanQRCodeActivity::class.java
                    )
                    privateChatScanQRCodeActivityResultLauncher.launch(intent)
                })
                NewChatItem(
                    image = if(isDarkTheme) R.drawable.ic_secret_group else R.drawable.ic_secret_group_light,
                    title = stringResource(id = R.string.home_screen_secret_groups_title),
                    MaterialTheme.appColors.textColor,
                    PaddingValues(start = 10.dp, end = 10.dp, top = 5.dp),
                    onClick = {
                        openActivity(OpenActivity.SecretGroup)
                    },
                    onClickScanQRCode = {}
                )
                NewChatItem(
                    image = if(isDarkTheme) R.drawable.ic_social_group else R.drawable.ic_social_group_light,
                    title = stringResource(id = R.string.home_screen_social_groups_title),
                    MaterialTheme.appColors.textColor,
                    PaddingValues(start = 10.dp, end = 10.dp, top = 5.dp),
                    onClick = {
                        openActivity(OpenActivity.PublicGroup)
                    },
                    onClickScanQRCode = {}
                )
                NewChatItem(
                    image = if(isDarkTheme) R.drawable.ic_note_to_self else R.drawable.ic_note_to_self_light,
                    title = stringResource(id = R.string.note_to_self),
                    MaterialTheme.appColors.textColor,
                    PaddingValues(start = 10.dp, end = 10.dp, top = 5.dp),
                    onClick = {
                        openActivity(OpenActivity.NoteToSelf)
                    },
                    onClickScanQRCode = {}
                )
                Text(
                    text = "Contact list",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight(400),
                        color = MaterialTheme.appColors.textHint,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                )
                NewChatItem(
                    image = R.drawable.ic_invite_a_friend,
                    title = stringResource(id = R.string.activity_settings_invite_button_title),
                    MaterialTheme.appColors.textColor,
                    PaddingValues(start = 10.dp, end = 10.dp),
                    onClick = {
                        openActivity(OpenActivity.InviteAFriend)
                    },
                    onClickScanQRCode = {}
                )
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal=16.dp
                        )
                        .weight(1f)
                ) {
                    items(contacts) {
                        GroupContact(
                            recipient = it,
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                openConversation(it)
                            },
                            context
                        )
                    }
                }
            }
        }
    )
}

private fun createPrivateChat(hexEncodedPublicKey: String, context: Context,bnsName: String ) {
    val activity = (context as? Activity)
    val recipient = Recipient.from(context, Address.fromSerialized(hexEncodedPublicKey), false)
    val bundle = Bundle()
    val intent = Intent()
    bundle.putParcelable(ConversationFragmentV2.URI, intent.data)
    bundle.putString(ConversationFragmentV2.TYPE, intent.type)
    bundle.putString(ConversationFragmentV2.BNS_NAME,bnsName)
    val returnIntent = Intent()
    returnIntent.putExtra(ConversationFragmentV2.ADDRESS, recipient.address)
    //returnIntent.setDataAndType(intent.data, intent.type)
    val existingThread =
        DatabaseComponent.get(context).threadDatabase().getThreadIdIfExistsFor(recipient)
    returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, existingThread)
    returnIntent.putExtras(bundle)
    activity?.setResult(RESULT_OK, returnIntent)
    activity?.finish()
}

@Composable
fun NewChatItem(
    image: Int,
    title: String,
    color: Color,
    padding: PaddingValues,
    showQrCode: Boolean = false,
    onClick: () -> Unit,
    onClickScanQRCode: () -> Unit,
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .clickable {
                onClick()
            }
    ) {

        Image(
            painterResource(id = image),
            contentDescription = "",
            modifier = Modifier
                .padding(5.dp)
                .size(40.dp),
            alignment = Alignment.CenterStart,
        )

        Text(
            text = title,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start=10.dp, end=5.dp),
        )

        if(showQrCode) {
            Image(
                painterResource(id = R.drawable.qr_code_send),
                contentDescription = "",
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.appColors.editTextColor
                ),
                modifier = Modifier
                    .size(35.dp)
                    .padding(end=15.dp)
                    .clickable {
                        onClickScanQRCode()
                    },
                alignment = Alignment.CenterEnd
            )
        }

    }
}

@Composable
private fun GroupContact(
    recipient: Recipient,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    context: Context
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical=4.dp)
            .clickable {
                onClick()
            }
    ) {
        Box(modifier = Modifier.padding(vertical = 5.dp)) {
            if(recipient.isGroupRecipient){
                val groupRecipients = remember {
                    DatabaseComponent.get(context).groupDatabase()
                        .getGroupMemberAddresses(recipient.address.toGroupString(), true)
                        .sorted()
                        .take(2)
                        .toMutableList()
                }
                val pictureMode = if (groupRecipients.size >= 2)
                    ProfilePictureMode.GroupPicture
                else
                    ProfilePictureMode.SmallPicture
                /*val pk = groupRecipients.getOrNull(0)?.serialize() ?: ""*/
                val additionalPk = groupRecipients.getOrNull(1)?.serialize() ?: ""
                val additionalDisplay =
                    getUserDisplayName(additionalPk, context)

                ProfilePictureComponent(
                    publicKey =recipient.address.toString(),
                    displayName =recipient.name.toString(),
                    additionalPublicKey = additionalPk,
                    additionalDisplayName = additionalDisplay,
                    containerSize = pictureMode.size,
                    pictureMode = pictureMode
                )
            }else{
                ProfilePictureComponent(
                    publicKey = recipient.address.toString(),
                    displayName = recipient.name.toString(),
                    containerSize = 40.dp,
                    pictureMode = ProfilePictureMode.SmallPicture
                )
            }
        }

        Text(
            text = if(recipient.name != null) recipient.name.toString().capitalizeFirstLetter() else recipient.address.toString().capitalizeFirstLetter(),
            textAlign = TextAlign.Start,
            fontSize = 16.sp,
            fontWeight = FontWeight(400),
            modifier = Modifier
                .weight(1f)
                .padding(start=if (recipient.isGroupRecipient) 5.dp else 15.dp, end=15.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NewChatPopUp(context: Context, onDismiss: () -> Unit, onClick: (String) -> Unit, updateBnsLoader: (Boolean) -> Unit){
    var bChatId by remember {
        mutableStateOf("")
    }

    var bchatIdErrorStatus by remember {
        mutableStateOf(false)
    }

    fun createPrivateChatIfPossible(bnsNameOrPublicKey: String, context: Context) {
        if (PublicKeyValidation.isValid(bnsNameOrPublicKey)) {
            createPrivateChat(bnsNameOrPublicKey, context, bnsNameOrPublicKey)
        } else {
            //Toast.makeText(context, R.string.invalid_bchat_id, Toast.LENGTH_SHORT).show()
            // This could be an BNS name
            updateBnsLoader(true)
            MnodeAPI.getBchatID(bnsNameOrPublicKey).successUi { hexEncodedPublicKey ->
                updateBnsLoader(false)
                createPrivateChat(hexEncodedPublicKey,context,bnsNameOrPublicKey)
            }.failUi { exception ->
                bchatIdErrorStatus = true
                updateBnsLoader(false)
                var message = context.resources.getString(R.string.fragment_enter_public_key_error_message)
                exception.localizedMessage?.let {
                    message = context.resources.getString(R.string.fragment_enter_public_key_error_message)
                    Log.d("Beldex","BNS exception $it")
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    DialogContainer(containerColor = MaterialTheme.appColors.newChatCardBackground, onDismissRequest = {}) {

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth(),
        ) {

            Text(
                text = stringResource(R.string.activity_create_private_chat_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight(700)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=14.dp, end=14.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, if(bchatIdErrorStatus) MaterialTheme.appColors.negativeRedButtonBorder else MaterialTheme.appColors.textFieldUnfocusedColor)
            ) {
                TextField(
                    value = bChatId,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.enter_chat_id),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.appColors.secondaryTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight(400)
                            )
                        )
                    },
                    onValueChange = {
                        if(it.isNotEmpty()){
                            bchatIdErrorStatus = false
                        }
                        bChatId = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .padding(end=10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.appColors.editTextBackground,
                        focusedContainerColor = MaterialTheme.appColors.editTextBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        selectionColors = TextSelectionColors(
                            MaterialTheme.appColors.textSelectionColor,
                            MaterialTheme.appColors.textSelectionColor
                        ),
                        cursorColor = colorResource(id = R.color.button_green),
                    )
                )
            }

            Row(
                modifier= Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Button(
                    onClick={ onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    colors= ButtonDefaults.buttonColors(
                        containerColor=MaterialTheme.appColors.negativeGreenButton,
                        contentColor = MaterialTheme.appColors.negativeGreenButtonText
                    ),
                    modifier=Modifier
                        .weight(1f),
                    border = BorderStroke(width = 0.5.dp, color = MaterialTheme.appColors.negativeGreenButtonBorder)
                ) {
                    Text(
                        text=stringResource(id=R.string.cancel),
                        style=MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            color = MaterialTheme.appColors.negativeGreenButtonText
                        ),
                        modifier=Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier=Modifier.width(7.dp))

                Button(
                    onClick={
                        if (CheckOnline.isOnline(context)) {
                            if (bChatId.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Please enter BChat ID",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                createPrivateChatIfPossible(bChatId, context)
                                onClick(bChatId)
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = bChatId.isNotEmpty() && !bchatIdErrorStatus,
                    shape = RoundedCornerShape(12.dp),
                    colors= ButtonDefaults.buttonColors(
                        containerColor=MaterialTheme.appColors.primaryButtonColor,
                        disabledContainerColor = MaterialTheme.appColors.disabledLetsBchatButton,
                        disabledContentColor = MaterialTheme.appColors.disabledLetsBchatContent
                    ),
                    modifier=Modifier
                        .weight(1f)
                ) {
                    Text(
                        text= stringResource(id = R.string.let_s_bchat),
                        style=MaterialTheme.typography.bodyMedium.copy(
                            color = if (bChatId.isNotEmpty() && !bchatIdErrorStatus) {
                                Color.White
                            } else {
                                MaterialTheme.appColors.disabledLetsBchatContent
                            },
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
                        ),
                        modifier=Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BnsLoadingPopUp(onDismiss: () -> Unit) {
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)) {
                Box(
                    contentAlignment= Alignment.Center,
                    modifier = Modifier
                        .size(55.dp)
                        .background(
                            color=MaterialTheme.appColors.circularProgressBarBackground,
                            shape=CircleShape
                        ),
                ){
                    CircularProgressIndicator(
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.appColors.primaryButtonColor,
                        strokeWidth = 2.dp
                    )
                }
                Text(text = stringResource(id = R.string.verify_bns), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.padding(10.dp))
            }
        }
    }

}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NewChatScreenNightPreview() {
    val contactViewModel: NewChatScreenViewModel = hiltViewModel()
    val searchQuery by contactViewModel.searchQuery.collectAsState()
    val contacts by contactViewModel.recipients.collectAsState(initial = listOf())
    BChatTheme {
        NewChatScreen(searchQuery, contacts,contactViewModel::onEvent, openActivity = {}, openConversation = {}, onBackPress = {}, isDarkTheme = true)
    }
}
package io.beldex.bchat.dms

import io.beldex.bchat.util.BChatToast
import android.app.Activity
import io.beldex.bchat.compose_utils.bchatBackground
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter
import io.beldex.bchat.conversation_v2.NewChatScreenViewModel
import io.beldex.bchat.conversation_v2.OpenActivity
import io.beldex.bchat.conversation_v2.getUserDisplayName
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.conversation.v2.ConversationActivityV2
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
    isDarkTheme: Boolean,
    isBnsLoading: Boolean = false,
    chatIdError: Boolean = false,
    onStartChat: (String) -> Unit = {},
    onChatIdErrorCleared: () -> Unit = {},
    onScannedChat: (hexEncodedPublicKey: String, bnsName: String) -> Unit = { _, _ -> },
    groupAvatarInfo: (Recipient) -> io.beldex.bchat.conversation_v2.NewChatScreenViewModel.GroupAvatarInfo =
        { io.beldex.bchat.conversation_v2.NewChatScreenViewModel.GroupAvatarInfo(false, "", "") }
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSearchOption by remember {
        mutableStateOf(false)
    }

    var showNewChatPopup by remember {
        mutableStateOf(false)
    }

    val privateChatScanQRCodeActivityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val hexEncodedPublicKey = result.data!!.getStringExtra(ConversationActivityV2.HEX_ENCODED_PUBLIC_KEY)
            val bnsName = result.data!!.getStringExtra(ConversationActivityV2.BNS_NAME)
            if(hexEncodedPublicKey!=null) {
                onScannedChat(hexEncodedPublicKey, bnsName.toString())
            }
        }
    }

    if(showNewChatPopup){
        NewChatPopUp(
            hasError = chatIdError,
            onDismiss = { showNewChatPopup = false },
            onStartChat = {
                keyboardController?.hide()
                onStartChat(it)
            },
            onInputChanged = onChatIdErrorCleared
        )
    }

    if(isBnsLoading) {
        BnsLoadingPopUp(onDismiss = {})
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier
            .bchatBackground(MaterialTheme.appColors.backgroundColor)
            .windowInsetsPadding(
                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
            ),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.appColors.backgroundColor
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.new_chat_screen_title),
                        fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                        color = MaterialTheme.appColors.editTextColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
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
                    .background(color = MaterialTheme.appColors.backgroundColor),
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
                    .padding(it)
                    .verticalScroll(rememberScrollState())
            )
            {
                if(showSearchOption) {
                    TextField(
                        value = searchQuery,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_name_bns_bchat_id),
                modifier = Modifier.padding(start = 4.dp),
                                fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.appColors.onboardingInputHint
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "clear search text",
                                tint = MaterialTheme.appColors.onboardingInputHint,
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
                                tint = MaterialTheme.appColors.onboardingInputHint,
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.appColors.dialogBackground,
                            focusedContainerColor = MaterialTheme.appColors.dialogBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            selectionColors = TextSelectionColors(
                                MaterialTheme.appColors.textSelectionColor,
                                MaterialTheme.appColors.textSelectionColor
                            ),
                            cursorColor = MaterialTheme.appColors.textSelectionColor
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.appColors.dialogBackground)
                        .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
                        .padding(vertical = 4.dp)
                ) {
                    NewChatItem(
                        image = R.drawable.ic_newchat_new_chat,
                        title = stringResource(id = R.string.activity_create_private_chat_title),
                        color = MaterialTheme.appColors.userDetailsBchatIdText,
                        padding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        showQrCode = true,
                        highlighted = true,
                        onClick = {
                            showNewChatPopup = !showNewChatPopup
                        },
                        onClickScanQRCode = {
                            val intent = Intent(
                                context,
                                PrivateChatScanQRCodeActivity::class.java
                            )
                            privateChatScanQRCodeActivityResultLauncher.launch(intent)
                        }
                    )
                    NewChatItem(
                        image = R.drawable.ic_newchat_secret,
                        title = stringResource(id = R.string.home_screen_secret_groups_title),
                        MaterialTheme.appColors.editTextColor,
                        PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        onClick = {
                            openActivity(OpenActivity.SecretGroup)
                        },
                        onClickScanQRCode = {}
                    )
                    NewChatItem(
                        image = R.drawable.ic_newchat_social,
                        title = stringResource(id = R.string.home_screen_social_groups_title),
                        MaterialTheme.appColors.editTextColor,
                        PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        onClick = {
                            openActivity(OpenActivity.PublicGroup)
                        },
                        onClickScanQRCode = {}
                    )
                    NewChatItem(
                        image = R.drawable.ic_newchat_note,
                        title = stringResource(id = R.string.note_to_self),
                        MaterialTheme.appColors.editTextColor,
                        PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        onClick = {
                            openActivity(OpenActivity.NoteToSelf)
                        },
                        onClickScanQRCode = {}
                    )
                }

                Text(
                    text = stringResource(R.string.contact_list),
                    fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.appColors.onboardingBodyColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                NewChatItem(
                    image = R.drawable.ic_newchat_invite,
                    title = stringResource(id = R.string.activity_settings_invite_button_title),
                    color = MaterialTheme.appColors.editTextColor,
                    padding = PaddingValues(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    highlighted = true,
                    onClick = {
                        openActivity(OpenActivity.InviteAFriend)
                    },
                    onClickScanQRCode = {}
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal=16.dp
                        )
                ) {
                    contacts.forEach {
                        GroupContact(
                            recipient = it,
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                openConversation(it)
                            },
                            groupAvatarInfo = groupAvatarInfo
                        )
                    }
                }
            }
        }
    )
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
    highlighted: Boolean = false,
    plainIcon: Boolean = true,
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

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.appColors.userDetailsCancelBackground,
                    shape = io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchSmall)
                )
                .border(
                    1.dp,
                    if (highlighted) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.pinBoxInactiveBorder,
                    io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchSmall)
                )
        ) {
            Image(
                painterResource(id = image),
                contentDescription = "",
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = title,
            textAlign = TextAlign.Start,
            fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(start=10.dp, end=5.dp),
        )

        if(showQrCode) {
            Image(
                painterResource(id = R.drawable.ic_newchat_scan),
                contentDescription = "",
                modifier = Modifier
                    .size(26.dp)
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
    groupAvatarInfo: (Recipient) -> io.beldex.bchat.conversation_v2.NewChatScreenViewModel.GroupAvatarInfo
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
                val avatarInfo = remember(recipient.address) { groupAvatarInfo(recipient) }
                val pictureMode = if (avatarInfo.hasTwoMembers)
                    ProfilePictureMode.GroupPicture
                else
                    ProfilePictureMode.SmallPicture
                val additionalPk = avatarInfo.additionalPublicKey
                val additionalDisplay = avatarInfo.additionalDisplayName

                ProfilePictureComponent(
                    publicKey = recipient.address.toString() ?: "",
                    displayName = recipient.name.toString() ?: "",
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
            fontFamily = io.beldex.bchat.compose_utils.OpenSans,
            color = MaterialTheme.appColors.editTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(start=if (recipient.isGroupRecipient) 5.dp else 15.dp, end=15.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NewChatPopUp(
    hasError: Boolean,
    onDismiss: () -> Unit,
    onStartChat: (String) -> Unit,
    onInputChanged: () -> Unit
){
    var bChatId by remember {
        mutableStateOf("")
    }
    val bchatIdErrorStatus = hasError

    DialogContainer(onDismissRequest = {}) {

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 18.dp)) {

            Text(
                text = stringResource(R.string.activity_create_private_chat_title),
                fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                color = MaterialTheme.appColors.editTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(MaterialTheme.appColors.dialogBackground)
                    .border(
                        1.dp,
                        if (bchatIdErrorStatus) MaterialTheme.appColors.negativeRedButtonBorder else Color(0xFF444444)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (bChatId.isEmpty()) {
                    Text(
                        text = stringResource(R.string.enter_the_bchat_id).uppercase(),
                        fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                        color = MaterialTheme.appColors.onboardingInputHint,
                        fontSize = 12.sp
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = bChatId,
                    onValueChange = {
                        if (it.isNotEmpty()) {
                            onInputChanged()
                        }
                        bChatId = it
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                        color = MaterialTheme.appColors.editTextColor,
                        fontSize = 14.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.appColors.textSelectionColor),
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (bchatIdErrorStatus) {
                Text(
                    text = stringResource(R.string.enter_correct_bchat_id),
                    fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                    color = MaterialTheme.appColors.negativeRedButtonBorder,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                io.beldex.bchat.compose_utils.DialogCancelButton(
                    text = stringResource(id = R.string.cancel),
                    onClick = { onDismiss() },
                    modifier = Modifier.weight(1f)
                )

                val canStart = bChatId.isNotEmpty() && !bchatIdErrorStatus
                io.beldex.bchat.compose_utils.PrimaryButton(
                    onClick = { onStartChat(bChatId) },
                    enabled = canStart,
                    shape = io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium),
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color(0xFF0B0B0B),
                    disabledContainerColor = Color(0xFF1A1A1A),
                    disabledContentColor = Color(0xFF737373),
                    border = if (canStart) BorderStroke(1.dp, Color(0xFF0A370A)) else null,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.let_s_bchat),
                        fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
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
        io.beldex.bchat.compose_utils.BChatLoaderContent(text = stringResource(id = R.string.verify_bns))
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
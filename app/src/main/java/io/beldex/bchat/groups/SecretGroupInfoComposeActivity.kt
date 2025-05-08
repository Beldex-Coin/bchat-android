package io.beldex.bchat.groups

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.messages.control.ExpirationTimerUpdate
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.leave
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ExpirationUtil
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getLocalNumber
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getProfileName
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.toHexString
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatOutlinedTextField
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.dialogs.LeaveGroupDialog
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.home.NotificationSettingDialog
import io.beldex.bchat.my_account.ui.CardContainer
import io.beldex.bchat.my_account.ui.dialogs.LockOptionsDialog
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

@AndroidEntryPoint
class SecretGroupInfoComposeActivity : ComponentActivity() {

    companion object {
        const val secretGroupID="secret_group_id"
        const val callback="callback"
        lateinit var listenerCallback : socialGroupInfoInterface
        fun setOnActionSelectedListener(socialGroupInfoInterface : socialGroupInfoInterface?) {
            if (socialGroupInfoInterface != null) {
                listenerCallback=socialGroupInfoInterface
            }
        }
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme(
                darkTheme=UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                Surface(
                    modifier=Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background
                ) {

                    val context=LocalContext.current
                    val activity=(context as? Activity)
                    val lifecycleOwner=LocalLifecycleOwner.current
                    val groupID : String=activity?.intent?.getStringExtra(secretGroupID)!!
                    val secretGroupInfoViewModelFactory=
                        SecretGroupViewModelFactory(groupID, context)
                    val secretGroupInfoViewModel=ViewModelProvider(
                        this, secretGroupInfoViewModelFactory
                    )[SecretGroupInfoViewModel::class.java]
                    val groupMembers by secretGroupInfoViewModel.groupMembers.collectAsState()

                    var tileName by remember {
                        mutableStateOf(context.getString(R.string.group_info))
                    }
                    secretGroupInfoViewModel.isShowBottomSheet.observe(lifecycleOwner) { isVisible ->
                        title=if (isVisible) {
                            context.getString(R.string.search_member_title)
                        } else {
                            context.getString(R.string.group_info)
                        }
                    }

                    SecretGroupInfoScreenContainer(
                        titleChange = {
                            tileName = context.getString(R.string.group_info)
                        },
                        context = context,
                        secretGroupInfoViewModel = secretGroupInfoViewModel,
                        title=tileName,
                        onBackClick={
                            (context as ComponentActivity).finish()
                        }
                    ) {
                        GroupDetailsScreen(
                            groupMembers,
                            listenerCallback,
                            secretGroupInfoViewModel,
                            showSearchView = {
                                tileName = context.getString(R.string.search_member_title)
                            }
                        )
                    }
                }
            }
        }
    }

    interface socialGroupInfoInterface {
        fun showAllMedia(recipient : Recipient)
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun GroupDetailsScreen(
    groupMembers : GroupMembers?,
    listenerCallback : SecretGroupInfoComposeActivity.socialGroupInfoInterface?,
    secretGroupInfoViewModel : SecretGroupInfoViewModel,
    showSearchView : () -> Unit
) {
    lateinit var groupID : String
    val context=LocalContext.current
    val activity=(context as? Activity)
    val lifecycleOwner=LocalLifecycleOwner.current
    val option=context.resources.getStringArray(R.array.notify_types)
    val timesOption=context.resources.getIntArray(R.array.expiration_times)
    val profileSize=36.dp
    val disAppearOption=remember {
        timesOption.map {
            ExpirationUtil.getExpirationDisplayValue(
                context,
                it
            )
        }
    }

    var allMembers=mutableListOf<String>()
    groupID=activity?.intent?.getStringExtra(SecretGroupInfoComposeActivity.secretGroupID)!!
    val groupInfo by remember {
        mutableStateOf(DatabaseComponent.get(context).groupDatabase().getGroup(groupID).get())
    }

    groupMembers?.members?.let { allMembers.addAll(it) }

    val recipient=Recipient.from(
        context, Address.fromSerialized(groupID), false
    )
    var groupName by remember {
        mutableStateOf(groupInfo.title)
    }
    var groupMembersCount by remember {
        mutableIntStateOf(
            groupMembers?.members?.count() ?: 0
        )
    }

    val memberCount by remember(groupMembers) {
        mutableIntStateOf(groupMembers?.members?.size ?: 0)
    }
    val searchQuery by secretGroupInfoViewModel.searchQuery.collectAsState()
    val filteredMembers=remember { MutableStateFlow<List<String>>(emptyList()) }

    val resultLauncher=rememberLauncherForActivityResult(
        contract=ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                groupName=data.getStringExtra("group_name") ?: ""
                groupMembersCount=data.getIntExtra("group_members_count", 0)
                if (memberCount != groupMembersCount) {
                    secretGroupInfoViewModel.fetchGroupMembers()
                    val newList=secretGroupInfoViewModel.groupMembers.value
                    if (newList != null) {
                        filteredMembers.value=newList.members
                    }
                }
            }
        }
    }


    var showLeaveGroupDialog by remember {
        mutableStateOf(false)
    }

    var showNotificationSettingsDialog by remember {
        mutableStateOf(false)
    }

    var showExpireMessageDialog by remember {
        mutableStateOf(false)
    }

    var showMemberOptionDialog by remember {
        mutableStateOf(false)
    }

    var selectedItem by remember {
        mutableStateOf<String?>(null)
    }

    var showNotificationSettingsItem by remember {
        mutableStateOf(option[recipient.notifyType])
    }

    var showExpirationItem by remember {
        mutableIntStateOf(timesOption.indexOf(recipient.expireMessages))
    }

    secretGroupInfoViewModel.isEnableNotification.observe(lifecycleOwner) { item ->
        showNotificationSettingsItem=item
    }

    secretGroupInfoViewModel.isExpirationItem.observe(lifecycleOwner) { item ->
        showExpirationItem=item
    }


    if (showLeaveGroupDialog) {
        val admins=groupInfo.admins
        val bchatID=getLocalNumber(context)
        val isCurrentUserAdmin=admins.any { it.toString() == bchatID }
        val message=if (isCurrentUserAdmin) {
            "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
        } else {
            context.resources.getString(R.string.ConversationActivity_are_you_sure_you_want_to_leave_this_group)
        }
        BChatTheme(
            darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
        ) {
            LeaveGroupDialog(
                title=stringResource(id=R.string.ConversationActivity_leave_group),
                message=message,
                positiveButtonTitle=stringResource(id=R.string.leave),
                onLeave={
                    var groupPublicKey : String?
                    var isClosedGroup : Boolean
                    try {
                        groupPublicKey=
                            GroupUtil.doubleDecodeGroupID(recipient.address.toString())
                                .toHexString()
                        isClosedGroup=DatabaseComponent.get(context).beldexAPIDatabase()
                            .isClosedGroup(groupPublicKey)
                    } catch (e : IOException) {
                        groupPublicKey=null
                        isClosedGroup=false
                    }
                    try {
                        if (isClosedGroup) {
                            MessageSender.leave(groupPublicKey!!, true)
                            activity.finish()
                        } else {
                            Toast.makeText(
                                context,
                                R.string.ConversationActivity_error_leaving_group,
                                Toast.LENGTH_LONG
                            ).show()
                            showLeaveGroupDialog=false
                        }
                    } catch (e : Exception) {
                        Toast.makeText(
                            context,
                            R.string.ConversationActivity_error_leaving_group,
                            Toast.LENGTH_LONG
                        ).show()
                        showLeaveGroupDialog=false
                    }
                },
                onCancel={
                    showLeaveGroupDialog=false
                }
            )
        }
    }

    if (showNotificationSettingsDialog) {
        BChatTheme(
            darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
        ) {
            NotificationSettingDialog(
                onDismiss={
                    showNotificationSettingsDialog=false
                },
                onClick={
                    showNotificationSettingsDialog=false
                },
                options=option.toList(),
                currentValue=option[recipient.notifyType],
                onValueChanged={ _, index ->
                    DatabaseComponent.get(context).recipientDatabase()
                        .setNotifyType(recipient, index.toString().toInt())
                    secretGroupInfoViewModel.updateNotificationType(option[index])
                    showNotificationSettingsDialog=false
                }
            )
        }
    }

    if (showExpireMessageDialog) {
        BChatTheme(
            darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
        ) {
            LockOptionsDialog(
                title=stringResource(R.string.disappearing_messages),
                options=disAppearOption,
                currentValue=disAppearOption[timesOption.indexOf(recipient.expireMessages)],
                onDismiss={
                    showExpireMessageDialog=false
                },
                onValueChanged={ _, index ->
                    showExpireMessageDialog=false
                    if (disAppearOption[timesOption.indexOf(recipient.expireMessages)] != disAppearOption[index]) {
                        val expirationTime=timesOption[index]
                        val message=ExpirationTimerUpdate(expirationTime)
                        message.recipient=recipient.address.serialize()
                        message.sentTimestamp=MnodeAPI.nowWithOffset
                        val expiringMessageManager=
                            ApplicationContext.getInstance(context).expiringMessageManager
                        expiringMessageManager.setExpirationTimer(message)
                        MessageSender.send(message, recipient.address)
                        secretGroupInfoViewModel.updateExpirationItem(expirationTime)
                    }
                }
            )
        }
    }

    if(showMemberOptionDialog){
        BChatTheme(
            darkTheme=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
        ) {
            MemberDetailsDialog(
                onDismiss={
                    showMemberOptionDialog=false
                },
                context = context,
                member = selectedItem ?: ""
            )
        }
    }

    fun getUserDisplayName(publicKey : String) : String {
        return if(publicKey == getLocalNumber(context)) {
            getProfileName(context) ?: publicKey
        } else {
            val contact=DatabaseComponent.get(context).bchatContactDatabase()
                .getContactWithBchatID(publicKey)
            contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
        }
    }

    fun editSecretGroup(context : Context, thread : Recipient) {
        if (!thread.isClosedGroupRecipient) {
            return
        }
        val intent=Intent(context, EditClosedGroupActivity::class.java)
        intent.putExtra(EditClosedGroupActivity.groupIDKey, groupID)
        resultLauncher.launch(intent)
    }

    val groupAdmin by remember {
        mutableStateOf(groupInfo.admins.toString().trim('[', ']'))
    }

    fun isSecretGroupIsActive():Boolean {
        return if (recipient.isClosedGroupRecipient) {
            val isActive = (groupInfo?.isActive == true)
            isActive
        } else {
            true
        }
    }
    var updateProfile by remember {
        mutableStateOf(true)
    }
    val membersToDisplay by filteredMembers.collectAsState()


    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            val combinedAllMembers=groupMembers!!.members
            val filtered=combinedAllMembers.filter { member ->
                val displayName=getUserDisplayName(member)
                displayName.contains(searchQuery, ignoreCase=true)
            }
            filteredMembers.value=filtered
            updateProfile = !updateProfile
        } else {
            filteredMembers.value=groupMembers!!.members
            updateProfile = !updateProfile
        }
    }


    fun String.capitalizeFirstLetter() : String {
        return this.replaceFirstChar { it.uppercase() }
    }

    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true
    )
    var isShowSearchBottomSheet by remember {
        mutableStateOf(false)
    }
    secretGroupInfoViewModel.isShowBottomSheet.observe(lifecycleOwner){ isVisible ->
        isShowSearchBottomSheet = isVisible
    }

    if (isShowSearchBottomSheet) {
        ModalBottomSheetLayout(
            sheetState=modalSheetState,
            sheetShape=RoundedCornerShape(topStart=12.dp, topEnd=12.dp),
            sheetContent={}
        ) {
            val focusRequester=remember { FocusRequester() }
            val coroutineScope=rememberCoroutineScope()

            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    focusRequester.requestFocus()
                }
            }

            Column(modifier=Modifier.padding(16.dp)) {

                BChatOutlinedTextField(
                    value=searchQuery,
                    onValueChange={
                        secretGroupInfoViewModel.updateSearchQuery(it)
                    },
                    placeHolder=stringResource(id=R.string.enter_name),
                    unFocusedContainerColor=MaterialTheme.appColors.searchBackground,
                    focusedContainerColor=MaterialTheme.appColors.searchBackground,
                    focusedBorderColor=Color.Transparent,
                    unFocusedBorderColor=Color.Transparent,
                    selectionColors=MaterialTheme.appColors.textSelectionColor,
                    cursorColor=colorResource(id=R.color.button_green),
                    shape=RoundedCornerShape(26.dp),
                    leadingIcon={
                        Icon(
                            imageVector=Icons.Default.Search,
                            contentDescription="clear search text",
                            tint=MaterialTheme.appColors.iconTint,
                        )
                    },
                    trailingIcon={
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector=Icons.Default.Clear,
                                contentDescription="clear search text",
                                tint=MaterialTheme.appColors.iconTint,
                                modifier=Modifier.clickable {
                                    secretGroupInfoViewModel._searchQuery.value=""
                                }
                            )
                        }
                    },
                    modifier=Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)

                )

                LazyColumn(modifier=Modifier.padding(top=16.dp)) {

                    items(membersToDisplay) { member ->

                        Row(
                            modifier=Modifier
                                .fillMaxWidth()
                                .padding(vertical=8.dp),
                            verticalAlignment=Alignment.CenterVertically
                        )
                        {
                            Box(
                                modifier=Modifier
                                    .padding(4.dp)
                                    .height(30.dp)
                                    .width(30.dp),
                                contentAlignment=Alignment.Center,
                            ) {
                                if (updateProfile) {
                                    ProfilePictureComponent(
                                        publicKey=member,
                                        displayName=getUserDisplayName(member),
                                        containerSize=profileSize,
                                        pictureMode=ProfilePictureMode.SmallPicture
                                    )
                                } else {
                                    ProfilePictureComponent(
                                        publicKey=member,
                                        displayName=getUserDisplayName(member),
                                        containerSize=profileSize,
                                        pictureMode=ProfilePictureMode.SmallPicture
                                    )
                                }
                            }
                            Spacer(modifier=Modifier.width(16.dp))
                            Column(modifier=Modifier.weight(1f)) {
                                Text(
                                    text=getUserDisplayName(member).capitalizeFirstLetter(),
                                    style=MaterialTheme.typography.bodyMedium.copy(
                                        fontSize=14.sp,
                                        color=MaterialTheme.appColors.textColor
                                    ),
                                    modifier=Modifier.padding(end=8.dp)
                                )
                            }
                            Spacer(modifier=Modifier.width(16.dp))
                            if (groupAdmin.contains(member)) {
                                Image(
                                    painter=painterResource(id=R.drawable.ic_admin_crown),
                                    contentDescription="admin crown",
                                    modifier=Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier=Modifier.width(8.dp))
                        }
                    }
                    item {
                        if (membersToDisplay.isEmpty()) {
                            Text(
                                text="No records found! ",
                                style=MaterialTheme.typography.bodyMedium.copy(
                                    fontSize=14.sp,
                                    color=MaterialTheme.appColors.textColor
                                ),
                                modifier=Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }


    Column() {

        Box(
            modifier=Modifier
                .padding(4.dp)
                .size(90.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment=Alignment.Center
        ) {
            val pictureType=ProfilePictureMode.GroupPicture
            val members=DatabaseComponent.get(context).groupDatabase()
                .getGroupMemberAddresses(recipient.address.toGroupString(), true)
                .sorted()
                .take(2).toMutableList()
            val additionalPk=members.getOrNull(1)?.serialize() ?: ""
            val additionalDisplay=getUserDisplayName(additionalPk)
            ProfilePictureComponent(
                publicKey=recipient.address.toString(),
                displayName=recipient.name.toString(),
                additionalPublicKey=additionalPk,
                additionalDisplayName=additionalDisplay,
                containerSize=70.dp,
                pictureMode=pictureType,
                isGroupInfo = true
            )
        }

        Row(
            modifier=Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.Center
        ) {
            Column(
                horizontalAlignment=Alignment.CenterHorizontally
            ) {
                Text(
                    text=groupName.capitalizeFirstLetter(),
                    style=MaterialTheme.typography.titleMedium.copy(
                        fontSize=18.sp,
                        fontWeight=FontWeight(800),
                        color=MaterialTheme.appColors.textColor
                    )
                )
            }
        }
        Spacer(modifier=Modifier.height(8.dp))

        Column(modifier=Modifier.padding(12.dp)) {

            LazyColumn(
                modifier=Modifier
                    .background(
                        color=MaterialTheme.appColors.secretGroupInfoBackground,
                        shape=RoundedCornerShape(12.dp)
                    )
            )
            {
                item() {
                    Column(
                        modifier=Modifier.padding(
                            start=8.dp,
                            top=16.dp,
                            end=8.dp,
                            bottom=8.dp
                        )
                    ) {
                        NavigationItem(
                            "All Media",
                            painterResource(id=R.drawable.ic_all_media),
                            onItemClick={ listenerCallback?.showAllMedia(recipient) },
                            checked=false,
                            showSwitch=false,
                            null
                        )
                        NavigationItem(
                            "Disappearing Messages",
                            painterResource(id=R.drawable.ic_disappearing_message),
                            onItemClick={
                                if (isSecretGroupIsActive()) {
                                    showExpireMessageDialog=true
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.no_participate_content),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            checked=showExpirationItem != 0,
                            showSwitch=true,
                            subTitle=context.getString(R.string.disappearing_info)
                        )
                        NavigationItem(
                            "Edit Group",
                            painterResource(id=R.drawable.ic_block_request),
                            onItemClick={
                                if (isSecretGroupIsActive()) {
                                    editSecretGroup(context, recipient)
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.no_participate_content),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            checked=false,
                            false,
                            null
                        )
                        NavigationItem(
                            "Notify for Mentions Only",
                            painterResource(id=R.drawable.ic_mention_only),
                            onItemClick={
                                if (isSecretGroupIsActive()) {
                                    showNotificationSettingsDialog=true
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.no_participate_content),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            },
                            checked=showNotificationSettingsItem == "Mentions",
                            showSwitch=true,
                            subTitle=context.getString(R.string.notification_info)
                        )

                        Row(
                            verticalAlignment=Alignment.CenterVertically,
                            modifier=Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable {
                                    if (isSecretGroupIsActive()) {
                                        showLeaveGroupDialog=true
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.no_participate_content),
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }
                        ) {
                            Icon(
                                painterResource(id=R.drawable.ic_block_request),
                                contentDescription="exit group",
                                tint=Color.Red,
                                modifier=Modifier.size(24.dp)
                            )
                            Spacer(modifier=Modifier.width(16.dp))
                            Text(
                                text="Leave Group",
                                color=Color.Red,
                                style=MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(
                            modifier=Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )
                    }
                }

                item {
                    Divider(
                        color=colorResource(id=R.color.contact_list_border),
                        modifier=Modifier
                            .fillMaxWidth()
                            .alpha(0.5f)
                    )
                }

                item {
                    Row(
                        modifier=Modifier.padding(16.dp)
                    ) {
                        Text(
                            text="$memberCount members",
                            color=Color.Gray,
                            style=MaterialTheme.typography.titleSmall.copy(
                                color=MaterialTheme.appColors.editTextHint,
                                fontSize=16.sp,
                                fontWeight=FontWeight(400)
                            ),
                            modifier=Modifier.weight(1f)
                        )
                        Icon(
                            imageVector=Icons.Default.Search,
                            contentDescription="search members",
                            tint=Color.Gray,
                            modifier=Modifier
                                .size(20.dp)
                                .clickable {
                                    showSearchView()
                                    secretGroupInfoViewModel.updateVisibleBottomSheet(true)
                                }
                        )
                    }
                }

                items(groupMembers!!.members) { member ->
                    Row(
                        modifier=Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                if (getLocalNumber(context) != member) {
                                    selectedItem=member
                                    showMemberOptionDialog=true
                                }
                            },
                        verticalAlignment=Alignment.CenterVertically,

                        ) {
                        Box(
                            modifier=Modifier
                                .padding(4.dp)
                                .height(30.dp)
                                .width(30.dp),
                            contentAlignment=Alignment.Center,
                        ) {
                            ProfilePictureComponent(
                                publicKey=member,
                                displayName=getUserDisplayName(member),
                                containerSize=profileSize,
                                pictureMode=ProfilePictureMode.SmallPicture
                            )
                        }
                        Spacer(modifier=Modifier.width(16.dp))
                        Column(modifier=Modifier.weight(1f)) {
                            Text(
                                text=getUserDisplayName(member).capitalizeFirstLetter(),
                                style=MaterialTheme.typography.bodyMedium.copy(
                                    fontSize=14.sp,
                                    color=MaterialTheme.appColors.textColor
                                ),
                                modifier=Modifier.padding(end=8.dp)
                            )
                        }
                        Spacer(modifier=Modifier.width(16.dp))
                        if (groupAdmin.contains(member)) {
                            Image(
                                painter=painterResource(id=R.drawable.ic_admin_crown),
                                contentDescription="admin crown",
                                modifier=Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier=Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationItem(
    label : String,
    icon : Painter,
    onItemClick : () -> Unit,
    checked : Boolean,
    showSwitch : Boolean,
    subTitle : String?
) {

    Column(modifier=Modifier) {

        Row(
            modifier=Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    onItemClick()
                },
            verticalAlignment=Alignment.Top,

            ) {
            Image(
                painter=icon,
                contentDescription=label,
                colorFilter=ColorFilter.tint(
                    color=MaterialTheme.appColors.editTextColor
                ),
                modifier=Modifier.size(24.dp)
            )
            Spacer(modifier=Modifier.width(16.dp))

            Column(
                modifier=Modifier.weight(1f)
            ) {
                Text(
                    text=label,
                    style=MaterialTheme.typography.titleMedium.copy(
                        fontSize=14.sp,
                        fontWeight=FontWeight(400),
                        color=MaterialTheme.appColors.textColor
                    )
                )
                if (subTitle != null) {
                    Text(
                        text=subTitle,
                        style=MaterialTheme.typography.labelSmall.copy(
                            color=Color(0xACACACAC),
                            fontWeight=FontWeight(600),
                            fontSize=12.sp
                        ),
                        modifier=Modifier
                            .padding(top=8.dp, end=16.dp)
                    )
                }
            }
            Spacer(modifier=Modifier.width(8.dp))

            if (!showSwitch) {
                Icon(
                    imageVector=Icons.Default.ArrowForwardIos,
                    contentDescription=label,
                    tint=MaterialTheme.appColors.editTextColor,
                    modifier=Modifier.size(16.dp)
                )
            } else {
                Switch(
                    checked=checked,
                    onCheckedChange={ onItemClick() },
                    colors=SwitchDefaults.colors(
                        checkedThumbColor=MaterialTheme.appColors.primaryButtonColor,
                        uncheckedThumbColor=MaterialTheme.appColors.unCheckedSwitchThumb,
                        checkedTrackColor=MaterialTheme.appColors.switchTrackColor,
                        uncheckedTrackColor=MaterialTheme.appColors.switchTrackColor
                    ),
                    modifier=Modifier
                        .size(30.dp)
                        .padding(end=4.dp)
                )
            }
        }
    }
}

@Composable
private fun SecretGroupInfoScreenContainer(
    titleChange : () ->  Unit,
    context : Context,
    secretGroupInfoViewModel : SecretGroupInfoViewModel,
    title : String,
    wrapInCard : Boolean=true,
    onBackClick : () -> Unit,
    actionItems : @Composable () -> Unit={},
    content : @Composable () -> Unit,
) {

    BackHandler {
        if (title == context.getString(R.string.search_member_title)) {
            secretGroupInfoViewModel.updateVisibleBottomSheet(false)
            titleChange()
            secretGroupInfoViewModel._searchQuery.value = ""
        } else {
            onBackClick()
            secretGroupInfoViewModel._searchQuery.value = ""
        }
    }

    Column(
        modifier=Modifier
            .fillMaxSize()

    ) {
        Row(
            verticalAlignment=Alignment.CenterVertically,
            modifier=Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Icon(painterResource(id=R.drawable.ic_back_arrow),
                contentDescription=stringResource(R.string.back),
                tint=MaterialTheme.appColors.editTextColor,
                modifier=Modifier.clickable {
                    if (title == context.getString(R.string.search_member_title)) {
                        secretGroupInfoViewModel.updateVisibleBottomSheet(false)
                        titleChange()
                        secretGroupInfoViewModel._searchQuery.value = ""
                    } else {
                        onBackClick()
                        secretGroupInfoViewModel._searchQuery.value = ""
                    }
                })

            Spacer(modifier=Modifier.width(16.dp))

            Text(
                text=title, style=MaterialTheme.typography.titleLarge.copy(
                    color=MaterialTheme.appColors.editTextColor,
                    fontWeight=FontWeight.Bold,
                    fontSize=18.sp
                ), modifier=Modifier.weight(1f)
            )

            actionItems()
        }

        Spacer(modifier=Modifier.height(16.dp))

        if (wrapInCard) {
            CardContainer(
                modifier=Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun MemberDetailsDialog(
    onDismiss : () -> Unit,
    context : Context,
    member : String
) {
    val activity=(context as? Activity)

    val options=context.resources.getStringArray(R.array.members_options)

    fun truncatedPublicKey(text : String) : String {
        if (text.length <= 8) {
            return text
        }
        return "${text.substring(0, 4)}...${text.substring(text.length - 4, text.length)}"
    }

    fun copyPublicKey(bchatID : String) {
        val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip=ClipData.newPlainText("Chat ID", bchatID)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun getUserDisplayName(publicKey : String) : String {
        return if (publicKey == getLocalNumber(context)) {
            getProfileName(context) ?: truncatedPublicKey(publicKey)
        } else {
            val contact=DatabaseComponent.get(context).bchatContactDatabase()
                .getContactWithBchatID(publicKey)
            contact?.displayName(Contact.ContactContext.REGULAR) ?: truncatedPublicKey(publicKey)
        }
    }

    DialogContainer(
        dismissOnBackPress=false,
        dismissOnClickOutside=false,
        onDismissRequest=onDismiss,
    ) {
        OutlinedCard(
            colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground),
            elevation=CardDefaults.cardElevation(defaultElevation=4.dp),
            modifier=Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start=20.dp, end=20.dp, top=25.dp, bottom=25.dp),
                Arrangement.Center,
                Alignment.CenterHorizontally
            ) {

                Row(modifier=Modifier.padding(bottom=8.dp)) {
                    LazyColumn(
                        verticalArrangement=Arrangement.spacedBy(8.dp),
                        modifier=Modifier
                            .weight(1f)

                    ) {
                        itemsIndexed(options) { index, item ->
                            Column(
                                modifier=Modifier
                                    .fillMaxSize()
                                    .padding(vertical=4.dp),
                                verticalArrangement=Arrangement.Center,
                                horizontalAlignment=Alignment.Start,
                            ) {
                                Text(
                                    text=if (index == 0) {
                                        item + " " + getUserDisplayName(member)
                                    } else item,
                                    style=MaterialTheme.typography.titleMedium.copy(
                                        color=MaterialTheme.appColors.secondaryTextColor,
                                        fontSize=16.sp,
                                        fontWeight=FontWeight(400)
                                    ),
                                    modifier=Modifier
                                        .padding(10.dp)
                                        .clickable {
                                            when (index) {
                                                0 -> {
                                                    val recipient=Recipient.from(
                                                        context,
                                                        Address.fromSerialized(member),
                                                        false
                                                    )
                                                    val existingThread=DatabaseComponent
                                                        .get(context)
                                                        .threadDatabase()
                                                        .getThreadIdIfExistsFor(recipient)
                                                    if (activity != null) {
                                                        createConversation(
                                                            threadId=existingThread,
                                                            address=recipient.address,
                                                            activity=activity,
                                                            context=context
                                                        )
                                                    }
                                                    onDismiss()
                                                }

                                                1 -> {
                                                    copyPublicKey(member)
                                                    onDismiss()
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                    Icon(
                        painter=painterResource(id=R.drawable.ic_close),
                        contentDescription="",
                        tint=MaterialTheme.appColors.editTextColor,
                        modifier=Modifier
                            .padding(end=8.dp, bottom=16.dp)
                            .clickable {
                                onDismiss()
                            }
                    )
                }

            }
        }
    }
}

private fun getBaseShareIntent(target : Class<*>, context : Context) : Intent {
    val intent=Intent(context, target)
    val bundle=Bundle()
    bundle.putParcelable(ConversationFragmentV2.URI, intent.data)
    bundle.putString(ConversationFragmentV2.TYPE, intent.type)
    return intent
}

private fun createConversation(
    threadId : Long,
    address : Address,
    activity : Activity,
    context : Context
) {
    val intent : Intent=getBaseShareIntent(HomeActivity::class.java, context)
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.putExtra(ConversationFragmentV2.ADDRESS, address)
    intent.putExtra(ConversationFragmentV2.THREAD_ID, threadId)
    intent.putExtra(HomeActivity.SHORTCUT_LAUNCHER, true)
    activity.startActivity(intent)
}
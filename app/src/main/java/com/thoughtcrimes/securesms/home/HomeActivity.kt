package com.thoughtcrimes.securesms.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityHomeBinding
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import com.thoughtcrimes.securesms.home.search.GlobalSearchAdapter
import com.thoughtcrimes.securesms.home.search.GlobalSearchInputLayout
import com.thoughtcrimes.securesms.home.search.GlobalSearchViewModel
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.database.*
import com.thoughtcrimes.securesms.onboarding.*
import com.thoughtcrimes.securesms.preferences.*
import com.thoughtcrimes.securesms.util.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfilePictureModifiedEvent
import com.beldex.libbchat.utilities.recipients.Recipient
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.thoughtcrimes.securesms.calls.WebRtcCallActivity
import com.thoughtcrimes.securesms.components.ProfilePictureView
import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.conversation.v2.ConversationViewModel
import com.thoughtcrimes.securesms.conversation.v2.messages.VoiceMessageViewDelegate
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.dms.CreateNewPrivateChatActivity
import com.thoughtcrimes.securesms.groups.CreateClosedGroupActivity
import com.thoughtcrimes.securesms.groups.JoinPublicChatNewActivity
import com.thoughtcrimes.securesms.keys.KeysPermissionActivity
import com.thoughtcrimes.securesms.messagerequests.MessageRequestsActivity
import com.thoughtcrimes.securesms.seed.SeedPermissionActivity
import com.thoughtcrimes.securesms.wallet.info.WalletInfoActivity
import com.thoughtcrimes.securesms.wallet.node.*
import com.thoughtcrimes.securesms.wallet.receive.ReceiveFragment
import com.thoughtcrimes.securesms.wallet.scanner.ScannerFragment
import com.thoughtcrimes.securesms.wallet.scanner.WalletScannerFragment
import com.thoughtcrimes.securesms.wallet.send.SendFragment
import com.thoughtcrimes.securesms.wallet.service.WalletService
import com.thoughtcrimes.securesms.wallet.utils.LegacyStorageHelper
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class HomeActivity : PassphraseRequiredActionBarActivity(),SeedReminderViewDelegate,HomeFragment.HomeFragmentListener,ConversationFragmentV2.Listener,UserDetailsBottomSheet.UserDetailsBottomSheetListener,VoiceMessageViewDelegate, ActivityDispatcher {

    private lateinit var binding: ActivityHomeBinding

    private val globalSearchViewModel by viewModels<GlobalSearchViewModel>()

    @Inject
    lateinit var threadDb: ThreadDatabase

    @Inject
    lateinit var mmsSmsDatabase: MmsSmsDatabase
    @Inject
    lateinit var recipientDatabase: RecipientDatabase
    @Inject
    lateinit var groupDatabase: GroupDatabase
    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences
    @Inject
    lateinit var viewModelFactory: ConversationViewModel.AssistedFactory

    //New Line App Update
    private var appUpdateManager: AppUpdateManager? = null
    private val IMMEDIATE_APP_UPDATE_REQ_CODE = 124

    private val reportIssueBChatID = "bdb890a974a25ef50c64cc4e3270c4c49c7096c433b8eecaf011c1ad000e426813"

    companion object{
        const val SHORTCUT_LAUNCHER = "short_cut_launcher"
    }


    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        // Set content view
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //-Wallet
        LegacyStorageHelper.migrateWallets(this)


        if(intent.getBooleanExtra(SHORTCUT_LAUNCHER,false)){
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS,intent.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, intent.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }else {
            val homeFragment: Fragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.activity_home_frame_layout_container,
                    homeFragment,
                    HomeFragment::class.java.name
                ).commit()
        }

        IP2Country.configureIfNeeded(this@HomeActivity)
        EventBus.getDefault().register(this@HomeActivity)

        //New Line App Update
        /*binding.airdropIcon.setAnimation(R.raw.airdrop_animation_top)
        binding.airdropIcon.setOnClickListener { callAirdropUrl() }*/
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        checkUpdate()

        /*if(TextSecurePreferences.getAirdropAnimationStatus(this)) {
            //New Line AirDrop
            TextSecurePreferences.setAirdropAnimationStatus(this,false)
            launchSuccessLottieDialog()
        }*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateProfileEvent(event: ProfilePictureModifiedEvent) {
        if (event.recipient.isLocalNumber) {
            val homeFragment:HomeFragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container) as HomeFragment
            if(homeFragment!=null) {
                homeFragment.updateProfileButton()
            }
        }
    }

    //New Line
    /*private fun launchSuccessLottieDialog() {
        val button = Button(this)
        button.text = "Claim BDX"
        button.setTextColor(Color.WHITE)
        button.isAllCaps=false
        val greenColor = ContextCompat.getColor(this, R.color.button_green)
        val backgroundColor = ContextCompat.getColor(this, R.color.animation_popup_background)
        button.backgroundTintList = ColorStateList.valueOf(greenColor)
        val dialog: LottieDialog = LottieDialog(this)
            .setAnimation(R.raw.airdrop_animation_dialog)
            .setAnimationRepeatCount(LottieDialog.INFINITE)
            .setAutoPlayAnimation(true)
            .setDialogBackground(backgroundColor)
            .setMessageColor(Color.WHITE)
            .addActionButton(button)
        dialog.show()
        button.setOnClickListener {
            callAirdropUrl()
            dialog.dismiss()
        }
    }
    private fun callAirdropUrl(){
        try {
            val url = "https://gleam.io/BT60O/bchat-launch-airdrop"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open URL", Toast.LENGTH_LONG).show()
        }
    }*/
    //New Line App Update
    private fun checkUpdate() {
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager!!.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startUpdateFlow(appUpdateInfo)
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo)
            }
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager!!.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.IMMEDIATE,
                this,
                this.IMMEDIATE_APP_UPDATE_REQ_CODE
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //New Line App Update
        if (requestCode == IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == PassphraseRequiredActionBarActivity.RESULT_CANCELED) {
                Toast.makeText(
                    applicationContext,
                    "Update canceled by user! Result Code: $resultCode", Toast.LENGTH_LONG
                ).show();
            } else if (resultCode == PassphraseRequiredActionBarActivity.RESULT_OK) {
                Toast.makeText(
                    applicationContext,
                    "Update success! Result Code: $resultCode",
                    Toast.LENGTH_LONG
                ).show();
            } else {
                Toast.makeText(
                    applicationContext,
                    "Update Failed! Result Code: $resultCode",
                    Toast.LENGTH_LONG
                ).show();
                checkUpdate();
            }
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
        if(fragment is ConversationFragmentV2) {
            (fragment as ConversationFragmentV2).onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun callLifeCycleScope(
        recyclerView: RecyclerView,
        globalSearchInputLayout: GlobalSearchInputLayout,
        mmsSmsDatabase: MmsSmsDatabase,
        globalSearchAdapter: GlobalSearchAdapter,
        publicKey: String,
        profileButton: ProfilePictureView,
        drawerProfileName: TextView,
        drawerProfileIcon: ProfilePictureView
    ) {
        lifecycleScope.launchWhenStarted {
            launch(Dispatchers.IO) {
                // Double check that the long poller is up
                (applicationContext as ApplicationContext).startPollingIfNeeded()
                // update things based on TextSecurePrefs (profile info etc)
                // Set up typing observer
                withContext(Dispatchers.Main) {
                    ApplicationContext.getInstance(this@HomeActivity).typingStatusRepository.typingThreads.observe(
                        this@HomeActivity,
                        Observer<Set<Long>> { threadIDs ->
                            val adapter = recyclerView.adapter as HomeAdapter
                            adapter.typingThreadIDs = threadIDs ?: setOf()
                        })
                    updateProfileButton(profileButton,drawerProfileName,drawerProfileIcon,publicKey)
                    TextSecurePreferences.events.filter { it == TextSecurePreferences.PROFILE_NAME_PREF }
                        .collect {
                            updateProfileButton(
                                profileButton,
                                drawerProfileName,
                                drawerProfileIcon,
                                publicKey
                            )
                        }
                }
                // Set up remaining components if needed
                val application = ApplicationContext.getInstance(this@HomeActivity)
                application.registerForFCMIfNeeded(false)
                val userPublicKey = TextSecurePreferences.getLocalNumber(this@HomeActivity)
                if (userPublicKey != null) {
                    OpenGroupManager.startPolling()
                    JobQueue.shared.resumePendingJobs()
                }
            }
            // monitor the global search VM query
            launch {
                globalSearchInputLayout.query
                    .onEach(globalSearchViewModel::postQuery)
                    .collect()
            }
            // Get group results and display them
            launch {
                globalSearchViewModel.result.collect { result ->
                    val currentUserPublicKey = publicKey
                    val contactAndGroupList =
                        result.contacts.map { GlobalSearchAdapter.Model.Contact(it) } +
                                result.threads.map { GlobalSearchAdapter.Model.GroupConversation(it) }

                    val contactResults = contactAndGroupList.toMutableList()

                    if (contactResults.isEmpty()) {
                        contactResults.add(
                            GlobalSearchAdapter.Model.SavedMessages(
                                currentUserPublicKey
                            )
                        )
                    }

                    val userIndex =
                        contactResults.indexOfFirst { it is GlobalSearchAdapter.Model.Contact && it.contact.bchatID == currentUserPublicKey }
                    if (userIndex >= 0) {
                        contactResults[userIndex] =
                            GlobalSearchAdapter.Model.SavedMessages(currentUserPublicKey)
                    }

                    if (contactResults.isNotEmpty()) {
                        contactResults.add(
                            0,
                            GlobalSearchAdapter.Model.Header(R.string.global_search_contacts_groups)
                        )
                    }

                    val unreadThreadMap = result.messages
                        .groupBy { it.threadId }.keys
                        .map { it to mmsSmsDatabase.getUnreadCount(it) }
                        .toMap()

                    val messageResults: MutableList<GlobalSearchAdapter.Model> = result.messages
                        .map { messageResult ->
                            GlobalSearchAdapter.Model.Message(
                                messageResult,
                                unreadThreadMap[messageResult.threadId] ?: 0
                            )
                        }.toMutableList()

                    if (messageResults.isNotEmpty()) {
                        messageResults.add(
                            0,
                            GlobalSearchAdapter.Model.Header(R.string.global_search_messages)
                        )
                    }

                    val newData = contactResults + messageResults
                    globalSearchAdapter.setNewData(result.query, newData)
                }
            }
        }


    }

    override fun onConversationClick(threadId: Long) {
        val extras = Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID, threadId)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    private fun replaceFragment(newFragment: Fragment, stackName: String?, extras: Bundle?) {
        if (extras != null) {
            newFragment.arguments = extras
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_home_frame_layout_container, newFragment)
            .addToBackStack(stackName)
            .commit()
    }

    private fun updateProfileButton(
        profileButton: ProfilePictureView,
        drawerProfileName: TextView,
        drawerProfileIcon: ProfilePictureView,
        publicKey: String
    ) {
        profileButton.publicKey = publicKey
        profileButton.displayName = TextSecurePreferences.getProfileName(this)
        profileButton.recycle()
        profileButton.update()

        //New Line
        drawerProfileName.text = TextSecurePreferences.getProfileName(this)
        drawerProfileIcon.publicKey = publicKey
        drawerProfileIcon.displayName = TextSecurePreferences.getProfileName(this)
        drawerProfileIcon.recycle()
        drawerProfileIcon.update()
    }

    //Important
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action === MotionEvent.ACTION_DOWN) {
            val touch = PointF(event.x, event.y)
            val homeFragment: Fragment? = getCurrentFragment()
            if((homeFragment is HomeFragment)) {
                homeFragment.dispatchTouchEvent()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onBackPressed() {
        val fragment: Fragment? = getCurrentFragment()
        if((fragment is HomeFragment)) {
            fragment.onBackPressed()
            super.onBackPressed()
        }else if(fragment is ConversationFragmentV2){
            replaceFragment(HomeFragment(), null, null)
        }
    }

    override fun handleSeedReminderViewContinueButtonTapped() {
        val intent = Intent(this, SeedActivity::class.java)
        show(intent)
    }

    override fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        callSettingsActivityResultLauncher.launch(intent)
    }

    private var callSettingsActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    override fun openMyWallet() {
        val walletName = TextSecurePreferences.getWalletName(this)
        val walletPassword = TextSecurePreferences.getWalletPassword(this)
        if (walletName != null && walletPassword !=null) {
            //startWallet(walletName, walletPassword, fingerprintUsed = false, streetmode = false)
            val lockManager: LockManager<CustomPinActivity> = LockManager.getInstance() as LockManager<CustomPinActivity>
            lockManager.enableAppLock(this, CustomPinActivity::class.java)
            val intent = Intent(this, CustomPinActivity::class.java)
            if(TextSecurePreferences.getWalletEntryPassword(this)!=null) {
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN)
                intent.putExtra("change_pin",false)
                intent.putExtra("send_authentication",false)
                push(intent)
            }else{
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK)
                intent.putExtra("change_pin",false)
                intent.putExtra("send_authentication",false)
                push(intent)
            }
        }else{
            val intent = Intent(this, WalletInfoActivity::class.java)
            push(intent)
        }
    }

    override fun showNotificationSettings() {
        val intent = Intent(this, NotificationSettingsActivity::class.java)
        push(intent)
    }

    override fun showPrivacySettings() {
        val intent = Intent(this, PrivacySettingsActivity::class.java)
        push(intent)
    }

    override fun showQRCode() {
        val intent = Intent(this, ShowQRCodeWithScanQRCodeActivity::class.java)
        showQRCodeWithScanQRCodeActivityResultLauncher.launch(intent)
    }

    private var showQRCodeWithScanQRCodeActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    override fun showSeed() {
        val intent = Intent(this, SeedPermissionActivity::class.java)
        show(intent)
    }

    override fun showKeys() {
        val intent = Intent(this, KeysPermissionActivity::class.java)
        show(intent)
    }

    override fun showAbout() {
        val intent = Intent(this, AboutActivity::class.java)
        show(intent)
    }

    override fun showPath() {
        val intent = Intent(this, PathActivity::class.java)
        show(intent)
    }

    override fun createNewPrivateChat() {
        val intent = Intent(this, CreateNewPrivateChatActivity::class.java)
        createNewPrivateChatResultLauncher.launch(intent)
    }
    private var createNewPrivateChatResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    override fun createNewSecretGroup() {
        val intent = Intent(this,CreateClosedGroupActivity::class.java)
        createClosedGroupActivityResultLauncher.launch(intent)
    }

    private var createClosedGroupActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
        if (result.resultCode == CreateClosedGroupActivity.closedGroupCreatedResultCode) {
            createNewPrivateChat()
        }
    }

    override fun joinSocialGroup() {
        val intent = Intent(this, JoinPublicChatNewActivity::class.java)
        joinPublicChatNewActivityResultLauncher.launch(intent)
    }

    private var joinPublicChatNewActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    /*Hales63*/
    override fun showMessageRequests() {
        val intent = Intent(this, MessageRequestsActivity::class.java)
        resultLauncher.launch(intent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    override fun sendMessageToSupport() {
        /*val recipient = Recipient.from(this, Address.fromSerialized(reportIssueBChatID), false)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS, recipient.address)
        intent.setDataAndType(getIntent().data, getIntent().type)
        val existingThread =
            DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        intent.putExtra(ConversationActivityV2.THREAD_ID, existingThread)
        startActivity(intent)*/
        val recipient = Recipient.from(this, Address.fromSerialized(reportIssueBChatID), false)
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS, recipient.address)
        val existingThread =
            DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        extras.putLong(ConversationFragmentV2.THREAD_ID,existingThread)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    override fun help() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@beldex.io"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        startActivity(intent)
    }

    override fun sendInvitation(hexEncodedPublicKey:String) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        val invitation =
            "Hey, I've been using BChat to chat with complete privacy and security. Come join me! Download it at https://play.google.com/store/apps/details?id=io.beldex.bchat. My Chat ID is $hexEncodedPublicKey !"
        intent.putExtra(Intent.EXTRA_TEXT, invitation)
        intent.type = "text/plain"
        val chooser =
            Intent.createChooser(intent, getString(R.string.activity_settings_invite_button_title))
        startActivity(chooser)
    }

    override fun toolBarCall() {
        val intent = Intent(this, WebRtcCallActivity::class.java)
        push(intent)
    }

    override fun callAppPermission() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", this@HomeActivity.packageName, null)
        intent.data = uri
        push(intent)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this@HomeActivity)
        super.onDestroy()
    }

    override fun getConversationViewModel(): ConversationViewModel.AssistedFactory {
        return viewModelFactory
    }

    override fun gettextSecurePreferences(): TextSecurePreferences {
        return textSecurePreferences
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
    }

    override fun passGlobalSearchAdapterModelMessageValue(
        threadId: Long,
        timestamp: Long,
        author: Address
    ) {
        val extras = Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID,threadId)
        extras.putLong(ConversationFragmentV2.SCROLL_MESSAGE_ID,timestamp)
        extras.putParcelable(ConversationFragmentV2.SCROLL_MESSAGE_AUTHOR,author)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    override fun passGlobalSearchAdapterModelSavedMessagesValue(address: Address) {
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS,address)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    override fun passGlobalSearchAdapterModelContactValue(address: Address) {
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS,address)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    override fun passGlobalSearchAdapterModelGroupConversationValue(threadId: Long) {
        val extras = Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID,threadId)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    override fun callConversationFragmentV2(address: Address, threadId: Long) {
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS,address)
        extras.putLong(ConversationFragmentV2.THREAD_ID,threadId)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    override fun playVoiceMessageAtIndexIfPossible(indexInAdapter: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
        if(fragment is ConversationFragmentV2) {
            (fragment as ConversationFragmentV2).playVoiceMessageAtIndexIfPossible(indexInAdapter)
        }
    }

    override fun getSystemService(name: String): Any? {
        if (name == ActivityDispatcher.SERVICE) {
            return this
        }
        return super.getSystemService(name)
    }

    override fun dispatchIntent(body: (Context) -> Intent?) {
        val intent = body(this) ?: return
        push(intent, false)
    }

    override fun showDialog(baseDialog: BaseDialog, tag: String?) {
        baseDialog.show(supportFragmentManager, tag)
    }
}

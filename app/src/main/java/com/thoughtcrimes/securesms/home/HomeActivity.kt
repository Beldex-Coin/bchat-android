package com.thoughtcrimes.securesms.home

import android.content.Intent
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityHomeBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfilePictureModifiedEvent
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.drawer.ClickListener
import com.thoughtcrimes.securesms.drawer.NavigationItemModel
import com.thoughtcrimes.securesms.drawer.NavigationRVAdapter
import com.thoughtcrimes.securesms.drawer.RecyclerTouchListener
import com.thoughtcrimes.securesms.groups.CreateClosedGroupActivity
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import javax.inject.Inject
import android.widget.CompoundButton
import androidx.core.view.isVisible
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.database.*
import com.thoughtcrimes.securesms.dms.CreateNewPrivateChatActivity
import com.thoughtcrimes.securesms.groups.JoinPublicChatNewActivity
import com.thoughtcrimes.securesms.keys.KeysPermissionActivity
import com.thoughtcrimes.securesms.mms.GlideApp
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.onboarding.*
import com.thoughtcrimes.securesms.preferences.*
import com.thoughtcrimes.securesms.seed.SeedPermissionActivity
import com.thoughtcrimes.securesms.util.*
import kotlinx.coroutines.flow.filter
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.tasks.Task
import android.content.IntentSender.SendIntentException
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.thoughtcrimes.securesms.calls.WebRtcCallActivity
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.messagerequests.MessageRequestsActivity
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.info.WalletInfoActivity
import com.thoughtcrimes.securesms.wallet.node.*
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import com.thoughtcrimes.securesms.webrtc.CallViewModel
import kotlinx.coroutines.*
import org.apache.commons.lang3.time.DurationFormatUtils
import java.util.*
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.thoughtcrimes.securesms.home.search.GlobalSearchAdapter
import com.thoughtcrimes.securesms.home.search.GlobalSearchInputLayout
import com.thoughtcrimes.securesms.home.search.GlobalSearchViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach


@AndroidEntryPoint
class HomeActivity : PassphraseRequiredActionBarActivity(),
    SeedReminderViewDelegate,NewConversationButtonSetViewDelegate,HomeFragment.HomeFragmentListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var glide: GlideRequests

    private var uiJob: Job? = null
    private val viewModel by viewModels<CallViewModel>()
    private val CALLDURATIONFORMAT = "HH:mm:ss"

    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences



    private val publicKey: String
        get() = TextSecurePreferences.getLocalNumber(this)!!



    private val toolbar: Toolbar? = null


    private var node1: NodeInfo? = null

    private val favouriteNodeslist = mutableSetOf<NodeInfo>()
    private val reportIssueBChatID = "bdb890a974a25ef50c64cc4e3270c4c49c7096c433b8eecaf011c1ad000e426813"


    //New Line
    private lateinit var adapter: NavigationRVAdapter

    private var items = arrayListOf(
        NavigationItemModel(R.drawable.ic_my_account, "My Account",0),
        NavigationItemModel(R.drawable.ic_wallet, "My Wallet",R.drawable.ic_beta),
        NavigationItemModel(R.drawable.ic_notifications, "Notification",0),
        NavigationItemModel(R.drawable.ic_message_requests, "Message Requests",0),
        NavigationItemModel(R.drawable.ic_privacy, "Privacy",0),
        NavigationItemModel(R.drawable.ic_app_permissions, "App Permissions",0),
        NavigationItemModel(R.drawable.ic_recovery_seed, "Recovery Seed",0),
        NavigationItemModel(R.drawable.ic_report_issue,"Report Issue",0),
        NavigationItemModel(R.drawable.ic_help, "Help",0),
        NavigationItemModel(R.drawable.ic_invite, "Invite",0),
        NavigationItemModel(R.drawable.ic_about, "About",0)
    )

    // NavigationItemModel(R.drawable.ic_recovery_key, "Recovery Key"),
    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    //New Line App Update
    private var appUpdateManager: AppUpdateManager? = null
    private val IMMEDIATE_APP_UPDATE_REQ_CODE = 124

    private var homeFragment: HomeFragment? = null

    private val globalSearchViewModel by viewModels<GlobalSearchViewModel>()

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        // Set content view
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //New Line
        val notification = TextSecurePreferences.isNotificationsEnabled(this)
        Log.d("NotificationLog", notification.toString())
        // Set custom toolbar
        setSupportActionBar(binding.toolbar)
        // Set up Glide
        glide = GlideApp.with(this)
        // Set up toolbar buttons
        binding.profileButton.glide = glide
        //binding.profileButton.setOnClickListener { openSettings() }

        //New Line
        // Setup Recyclerview's Layout
        binding.navigationRv.layoutManager = LinearLayoutManager(this)
        binding.navigationRv.setHasFixedSize(true)
        updateAdapter(0)
        // Add Item Touch Listener
        binding.navigationRv.addOnItemTouchListener(RecyclerTouchListener(this, object :
            ClickListener {
            override fun onClick(view: View, position: Int) {
                when (position) {
                    0 -> {
                        // # Account Activity
                        openSettings()
                    }
                    1 -> {
                        // # My Wallet Activity
                        if(CheckOnline.isOnline(this@HomeActivity)) {
                            Log.d("Beldex","isOnline value ${CheckOnline.isOnline(this@HomeActivity)}")
                            openMyWallet()
                        }
                        else {
                            Log.d("Beldex","isOnline value ${CheckOnline.isOnline(this@HomeActivity)}")
                            Toast.makeText(this@HomeActivity,getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        //New Line
                        val notification =
                            TextSecurePreferences.isNotificationsEnabled(this@HomeActivity)
                        Log.d("NotificationLog1", notification.toString())
                        // # Notification Activity
                        showNotificationSettings()
                    }
                    3 -> {
                        // # Message Requests Activity
                        showMessageRequests()
                    }
                    4 -> {
                        // # Privacy Activity
                        showPrivacySettings()
                    }
                    5 -> {
                        // # App Permissions Activity
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", this@HomeActivity.packageName, null)
                        intent.data = uri
                        push(intent)
                    }
                    6 -> {
                        // # Recovery Seed Activity
                        showSeed()
                    }
                    /* 6 -> {
                         // # Recovery Key Activity
                         showKeys()
                     }*/
                    7 -> {
                        // # Support
                        sendMessageToSupport()
                    }
                    8 -> {
                        // # Help Activity
                        help()
                    }
                    9 -> {
                        // # Invite Activity
                        sendInvitation()
                    }
                    10 -> {
                        // # About Activity
                        showAbout()
                    }
                }
                // Don't highlight the 'Profile' and 'Like us on Facebook' item row
                if (position != 6 && position != 4) {
                    updateAdapter(position)
                }
                Handler().postDelayed({
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                }, 200)
            }
        }))
        binding.profileButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
            /*val intent = Intent(this, CreatePasswordActivity::class.java)
            show(intent)*/
        }
        binding.drawerCloseIcon.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.END) }
        val activeUiMode = UiModeUtilities.getUserSelectedUiMode(this)
        Log.d("beldex", "activeUiMode $activeUiMode")
        binding.drawerAppearanceToggleButton.isChecked = activeUiMode == UiMode.NIGHT

        binding.drawerAppearanceToggleButton.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener() { compoundButton: CompoundButton, b: Boolean ->
            if (b) {
                val uiMode = UiMode.values()[1]
                UiModeUtilities.setUserSelectedUiMode(this, uiMode)
            } else {
                val uiMode = UiMode.values()[0]
                UiModeUtilities.setUserSelectedUiMode(this, uiMode)
            }

        })
        binding.drawerQrcodeImg.setOnClickListener {
            showQRCode()
            Handler().postDelayed({
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }, 200)
        }
        binding.drawerProfileIcon.glide = glide
        binding.drawerProfileId.text = "ID: $hexEncodedPublicKey"

        binding.bchatToolbar.disableClipping()

        //Comment-->
        /*// Set up seed reminder view
        val hasViewedSeed = TextSecurePreferences.getHasViewedSeed(this)
        if (!hasViewedSeed) {
            binding.seedReminderView.isVisible = true
            binding.seedReminderView.title =
                SpannableString("You're almost finished! 80%") // Intentionally not yet translated
            binding.seedReminderView.subtitle =
                resources.getString(R.string.view_seed_reminder_subtitle_1)
            binding.seedReminderView.setProgress(80, false)
            binding.seedReminderView.delegate = this@HomeActivity
        } else {
            binding.seedReminderView.isVisible = false
        }*/

        val adapter = HomeViewPagerAdapter(this)

        adapter.addFragment(HomeFragment(),"Chat")
        adapter.addFragment(WalletFragment(),"Wallet")
        val viewPager: ViewPager2 = binding.viewPager
        viewPager.adapter = adapter
        viewPager.currentItem = 0
        val tabs: TabLayout = binding.tabLayout
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        /*Hales63*/

        setupHeaderImage()
        IP2Country.configureIfNeeded(this@HomeActivity)
        homeFragment = supportFragmentManager.findFragmentById(R.id.viewPager) as HomeFragment?

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

    override fun callLifeCycleScope(
        recyclerView: RecyclerView,
        globalSearchInputLayout: GlobalSearchInputLayout,
        mmsSmsDatabase: MmsSmsDatabase,
        globalSearchAdapter: GlobalSearchAdapter,
        publicKey: String
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
                    updateProfileButton()
                    TextSecurePreferences.events.filter { it == TextSecurePreferences.PROFILE_NAME_PREF }
                        .collect {
                            updateProfileButton()
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


    private fun setupCallActionBar() {

        val startTimeNew = viewModel.callStartTime
        if (startTimeNew == -1L) {
            binding.toolbarCall.isVisible = false
        } else {
            binding.toolbarCall.isVisible = true
            uiJob = lifecycleScope.launch {
                launch {
                    while (isActive) {
                        val startTime = viewModel.callStartTime
                        if (startTime == -1L) {
                            binding.toolbarCall.isVisible = false
                        } else {
                            binding.toolbarCall.isVisible = true
                            binding.callDurationCall.text = DurationFormatUtils.formatDuration(
                                System.currentTimeMillis() - startTime,
                                CALLDURATIONFORMAT
                            )
                        }

                        delay(1_000)
                    }
                }
            }
        }
        binding.hanUpCall.setOnClickListener {
            startService(WebRtcCallService.hangupIntent(this))
            binding.toolbarCall.isVisible = false
            Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
        }
        binding.toolbarCall.setOnClickListener {
            val intent = Intent(this, WebRtcCallActivity::class.java)
            push(intent)
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
        } catch (e: SendIntentException) {
            e.printStackTrace()
        }
    }

    //Important
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.getAction() === MotionEvent.ACTION_DOWN) {

            val touch = PointF(event.x, event.y)
            if(homeFragment!=null) {
                homeFragment!!.dispatchTouchEvent()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    //New Line
    private fun updateAdapter(highlightItemPos: Int) {
        adapter = NavigationRVAdapter(items, highlightItemPos)
        binding.navigationRv.adapter = adapter
        adapter.notifyDataSetChanged()
    }


    private fun setupHeaderImage() {
        val isDayUiMode = UiModeUtilities.isDayUiMode(this)
        val headerTint = if (isDayUiMode) R.color.black else R.color.white
        binding.bchatHeaderImage.setTextColor(getColor(headerTint))
        //binding.bchatHeaderImage.setColorFilter(getColor(headerTint))
    }



    /*//New Line
    private fun setupMessageRequestsBanner() {
        val messageRequestCount = threadDb.unapprovedConversationCount
        // Set up message requests
        if (messageRequestCount > 0 && !textSecurePreferences.hasHiddenMessageRequests()) {
            //New Line
            textSecurePreferences.setHasShowMessageRequests(true)
        }

        //New Line
        if(textSecurePreferences.hasShowMessageRequests()) {
            with(ViewMessageRequestBannerBinding.inflate(layoutInflater)) {
                unreadCountTextView.text = messageRequestCount.toString()
                if(messageRequestCount>0) {
                    timestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(
                        this@HomeActivity,
                        Locale.getDefault(),
                        threadDb.latestUnapprovedConversationTimestamp
                    )
                }
                root.setOnClickListener { showMessageRequests() }
                expandMessageRequest.setOnClickListener{ showMessageRequests() }
                root.setOnLongClickListener { hideMessageRequests(); true }
                root.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
                homeAdapter.headerView = root
                homeAdapter.notifyItemChanged(0)
            }
        } else {
            homeAdapter.headerView = null
        }
    }*/

    private fun checkInternetConnectivity() {
        if (OnionRequestAPI.paths.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT)
                .show();
        }
    }

    override fun onResume() {
        super.onResume()
        setupCallActionBar()
        ApplicationContext.getInstance(this).messageNotifier.setHomeScreenVisible(true)
        if (TextSecurePreferences.getLocalNumber(this) == null) {
            return; } // This can be the case after a secondary device is auto-cleared
        IdentityKeyUtil.checkUpdate(this)
        binding.profileButton.recycle() // clear cached image before update tje profilePictureView
        binding.profileButton.update()

        //New Line
        binding.drawerProfileIcon.recycle()
        binding.drawerProfileIcon.update()
        //checkInternetConnectivity()

        //Comment-->
        /*val hasViewedSeed = TextSecurePreferences.getHasViewedSeed(this)
        if (hasViewedSeed) {
            binding.seedReminderView.isVisible = false
        }*/

        if (TextSecurePreferences.getConfigurationMessageSynced(this)) {
            lifecycleScope.launch(Dispatchers.IO) {
                ConfigurationMessageUtilities.syncConfigurationIfNeeded(this@HomeActivity)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("Beldex","HomeActivity() onPause called")
        ApplicationContext.getInstance(this).messageNotifier.setHomeScreenVisible(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == CreateClosedGroupActivity.closedGroupCreatedResultCode) {
            createNewPrivateChat()
        }

        //New Line App Update
        if (requestCode == IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(
                    applicationContext,
                    "Update canceled by user! Result Code: $resultCode", Toast.LENGTH_LONG
                ).show();
            } else if (resultCode == RESULT_OK) {
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
    }

    override fun onDestroy() {
        Log.d("Beldex","onDestroy in Home")
        EventBus.getDefault().unregister(this)
        super.onDestroy()

    }
    // endregion

    // region Updating

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateProfileEvent(event: ProfilePictureModifiedEvent) {
        if (event.recipient.isLocalNumber) {
            updateProfileButton()
        }
    }

    override fun updateProfileButton() {
        binding.profileButton.publicKey = publicKey
        binding.profileButton.displayName = TextSecurePreferences.getProfileName(this)
        binding.profileButton.recycle()
        binding.profileButton.update()

        //New Line
        binding.drawerProfileName.text = TextSecurePreferences.getProfileName(this)
        binding.drawerProfileIcon.publicKey = publicKey
        binding.drawerProfileIcon.displayName = TextSecurePreferences.getProfileName(this)
        binding.drawerProfileIcon.recycle()
        binding.drawerProfileIcon.update()
    }
    // endregion

    // region Interaction
    override fun onBackPressed() {
       /* if (binding.globalSearchRecycler.isVisible) {
            binding.globalSearchInputLayout.clearSearch(true)
            return
        }*/ //- Important
        super.onBackPressed()
    }

    override fun handleSeedReminderViewContinueButtonTapped() {
        val intent = Intent(this, SeedActivity::class.java)
        show(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        show(intent, isForResult = true)
    }


    private fun openMyWallet() {
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

    /*private fun startWallet(
        walletName:String, walletPassword:String,
        fingerprintUsed:Boolean, streetmode:Boolean) {
        val REQUEST_ID = "id"
        val REQUEST_PW = "pw"
        val REQUEST_FINGERPRINT_USED = "fingerprint"
        val REQUEST_STREETMODE = "streetmode"
        val REQUEST_URI = "uri"

        Timber.d("startWallet()");
        TextSecurePreferences.setIncomingTransactionStatus(this, true)
        TextSecurePreferences.setOutgoingTransactionStatus(this, true)
        TextSecurePreferences.setTransactionsByDateStatus(this,false)
        val intent = Intent(this, WalletActivity::class.java)
        intent.putExtra(REQUEST_ID, walletName)
        intent.putExtra(REQUEST_PW, walletPassword)
        intent.putExtra(REQUEST_FINGERPRINT_USED, fingerprintUsed)
        intent.putExtra(REQUEST_STREETMODE, streetmode)
        //Important
        *//*if (uri != null) {
            intent.putExtra(REQUEST_URI, uri)
            uri = null // use only once
        }*//*
        startActivity(intent)
    }*/

    private fun showNotificationSettings() {
        val intent = Intent(this, NotificationSettingsActivity::class.java)
        push(intent)
    }

    private fun showPrivacySettings() {
        val intent = Intent(this, PrivacySettingsActivity::class.java)
        push(intent)
    }

    private fun sendInvitation() {
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

    //New Line
    private fun help() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@beldex.io"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        startActivity(intent)
    }

    private fun showQRCode() {
        val intent = Intent(this, ShowQRCodeWithScanQRCodeActivity::class.java)
        push(intent)
    }

    private fun showSeed() {
        val intent = Intent(this, SeedPermissionActivity::class.java)
        show(intent)
    }

    private fun showKeys() {
        val intent = Intent(this, KeysPermissionActivity::class.java)
        show(intent)
    }

    private fun showAbout() {
        val intent = Intent(this, AboutActivity::class.java)
        show(intent)
    }

    private fun sendMessageToSupport() {
        val recipient = Recipient.from(this, Address.fromSerialized(reportIssueBChatID), false)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS, recipient.address)
        intent.setDataAndType(getIntent().data, getIntent().type)
        val existingThread =
            DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        intent.putExtra(ConversationActivityV2.THREAD_ID, existingThread)
        startActivity(intent)
    }

    private fun showPath() {
        val intent = Intent(this, PathActivity::class.java)
        show(intent)
    }

    override fun createNewPrivateChat() {
        val intent = Intent(this, CreateNewPrivateChatActivity::class.java)
        show(intent)
    }

    override fun createNewSecretGroup() {
        val intent = Intent(this, CreateClosedGroupActivity::class.java)
        show(intent, true)
    }

    override fun joinSocialGroup() {
        val intent = Intent(this, JoinPublicChatNewActivity::class.java)
        show(intent)

    }

    /*Hales63*/
    private fun showMessageRequests() {
        val intent = Intent(this, MessageRequestsActivity::class.java)
        push(intent)
    }

    override fun passGlobalSearchAdapterModelMessageValues(
        threadId: Long,
        timestamp: Long,
        author: Address
    ) {
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.THREAD_ID, threadId)
        intent.putExtra(ConversationActivityV2.SCROLL_MESSAGE_ID, timestamp)
        intent.putExtra(ConversationActivityV2.SCROLL_MESSAGE_AUTHOR, author)
        push(intent)
    }

    override fun passGlobalSearchAdapterModelSavedMessagesValues(model: GlobalSearchAdapter.Model.SavedMessages) {
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(
            ConversationActivityV2.ADDRESS,
            Address.fromSerialized(model.currentUserPublicKey)
        )
        push(intent)
    }

    override fun passGlobalSearchAdapterModelContactValues(address: String) {
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS, Address.fromSerialized(address))
        push(intent)
    }

    override fun passGlobalSearchAdapterModelGroupConversationValues(threadId: Long) {
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.THREAD_ID, threadId)
        push(intent)
    }

    override fun onConversationClick(threadId: Long) {
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.THREAD_ID, threadId)
        push(intent)
    }

    override fun callJoinSocialGroup() {
        this.joinSocialGroup()
    }

    override fun callCreateNewPrivateChat() {
        this.createNewPrivateChat()
    }

    override fun callCreateNewSecretGroup() {
        this.createNewSecretGroup()
    }
    // endregion
}

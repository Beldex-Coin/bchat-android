package io.beldex.bchat.home

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupRecord
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.ProfilePictureModifiedEvent
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getIsReactionOverlayVisible
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setIsReactionOverlayVisible
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils
import com.beldex.libsignal.utilities.toHexString
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.MediaOverviewActivity
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.components.ProfilePictureView
import io.beldex.bchat.compose_utils.ComposeDialogContainer
import io.beldex.bchat.compose_utils.DialogType
import io.beldex.bchat.conversation.v2.ConversationViewModel
import io.beldex.bchat.conversation.v2.contact_sharing.ViewAllContactFragment
import io.beldex.bchat.database.BeldexMessageDatabase
import io.beldex.bchat.database.MmsDatabase
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.database.ReactionDatabase
import io.beldex.bchat.database.SmsDatabase
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.databinding.ActivityHomeBinding
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.home.search.GlobalSearchAdapter
import io.beldex.bchat.home.search.GlobalSearchViewModel
import io.beldex.bchat.notifications.PushRegistry
import io.beldex.bchat.onboarding.SeedActivity
import io.beldex.bchat.onboarding.SeedReminderViewDelegate
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.FirebaseRemoteConfigUtil
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.IP2Country
import io.beldex.bchat.util.parcelable
import io.beldex.bchat.util.push
import io.beldex.bchat.util.show
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import io.beldex.bchat.conversation_v2.NewChatConversationActivity
import io.beldex.bchat.conversation_v2.NewGroupConversationActivity
import io.beldex.bchat.conversation_v2.NewGroupConversationType
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.database.BeldexThreadDatabase
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.database.RecipientDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.drawer.ClickListener
import io.beldex.bchat.drawer.NavigationItemModel
import io.beldex.bchat.drawer.NavigationRVAdapter
import io.beldex.bchat.drawer.RecyclerTouchListener
import io.beldex.bchat.groups.CreateClosedGroupActivity
import io.beldex.bchat.home.search.GlobalSearchInputLayout
import io.beldex.bchat.home.search.RecyclerViewDivider
import io.beldex.bchat.my_account.ui.MyAccountActivity
import io.beldex.bchat.my_account.ui.MyAccountScreens
import io.beldex.bchat.preferences.NotificationSettingsActivity
import io.beldex.bchat.preferences.PrivacySettingsActivity
import io.beldex.bchat.repository.ConversationRepository
import io.beldex.bchat.search.SearchActivityResults
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.util.SaveYourSeedDialogBox
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.disableClipping
import io.beldex.bchat.util.getScreenWidth
import io.beldex.bchat.util.toPx
import io.beldex.bchat.wallet.OnBackPressedListener
import io.beldex.bchat.webrtc.CallViewModel
import io.beldex.bchat.webrtc.NetworkChangeReceiver
import io.beldex.bchat.webrtc.WebRTCComposeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.apache.commons.lang3.time.DurationFormatUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt


@AndroidEntryPoint
class HomeActivity : PassphraseRequiredActionBarActivity(), SeedReminderViewDelegate,
    UserDetailsBottomSheet.UserDetailsBottomSheetListener, ConversationClickListener,
    NewConversationButtonSetViewDelegate,
    GlobalSearchInputLayout.GlobalSearchInputLayoutListener, ConversationActionDialog.ConversationActionDialogListener {

    private lateinit var binding: ActivityHomeBinding

    private val globalSearchViewModel by viewModels<GlobalSearchViewModel>()
    private val archiveChatViewModel by viewModels<ArchiveChatViewModel>()
    private val callViewModel by viewModels<CallViewModel>()
    private val homeViewModel: HomeFragmentViewModel by viewModels()
    private val globalSearchAdapter = GlobalSearchAdapter {model ->  }


    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences
    @Inject
    lateinit var viewModelFactory: ConversationViewModel.AssistedFactory
    @Inject lateinit var pushRegistry: PushRegistry
    @Inject
    lateinit var mmsSmsDatabase: MmsSmsDatabase
    @Inject
    lateinit var recipientDatabase: RecipientDatabase
    @Inject
    lateinit var groupDb: GroupDatabase
    @Inject
    lateinit var beldexThreadDb: BeldexThreadDatabase
    @Inject
    lateinit var repository : ConversationRepository

    private lateinit var glide: RequestManager
    private var broadcastReceiver: BroadcastReceiver? = null
    private var uiJob: Job? = null
    private val callDurationFormat = "HH:mm:ss"

    private val publicKey: String
        get() = TextSecurePreferences.getLocalNumber(this)!!

    /*Hales63*/
    private val homeAdapter: HomeAdapter by lazy {
        HomeAdapter(context = this, listener = this, threadDB = threadDb)
    }

    private lateinit var adapter: NavigationRVAdapter

    private var items = arrayListOf(
        NavigationItemModel(R.drawable.ic_settings_outline, "Settings"),
        NavigationItemModel(R.drawable.ic_notification_outline, "Notification"),
        NavigationItemModel(R.drawable.ic_msg_rqst_outline, "Message Requests"),
        NavigationItemModel(R.drawable.ic_recovery_seed_outline, "Recovery Seed"),
        NavigationItemModel(R.drawable.ic_report_issue_outline,"Report Issue"),
        NavigationItemModel(R.drawable.ic_help_outline, "Help"),
        NavigationItemModel(R.drawable.ic_invite_outline, "Invite"),
        NavigationItemModel(R.drawable.ic_about_outline, "About")
    )
    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    private val searchResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.extras?.parcelable<SearchActivityResults>(SearchActivity.EXTRA_SEARCH_DATA)?.let { result ->
                when (result) {
                    is SearchActivityResults.Contact -> {
                        passGlobalSearchAdapterModelContactValue(result.address)
                    }
                    is SearchActivityResults.GroupConversation -> {
                        val groupAddress = Address.fromSerialized(result.groupEncodedId)
                        val threadId = threadDb.getThreadIdIfExistsFor(Recipient.from(this, groupAddress, false))
                        if (threadId >= 0) {
                            passGlobalSearchAdapterModelGroupConversationValue(threadId)
                        }
                    }
                    is SearchActivityResults.Message -> {
                        passGlobalSearchAdapterModelMessageValue(result.threadId,result.timeStamp,result.author)
                    }
                    is SearchActivityResults.SavedMessage -> {
                        passGlobalSearchAdapterModelSavedMessagesValue(result.address)
                    }
                    else -> {}
                }
            }
        }
    }


    //New Line App Update
    private var appUpdateManager: AppUpdateManager? = null
    private val immediateAppUpdateRequestCode = 125

    companion object {
        const val SHORTCUT_LAUNCHER = "short_cut_launcher"

        var REQUEST_URI = "uri"
        const val reportIssueBChatID = BuildConfig.REPORT_ISSUE_ID
    }
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfigUtil
    private var networkChangedReceiver: NetworkChangeReceiver? = null
    private val broadcastReceivers = mutableListOf<BroadcastReceiver>()

    @Inject
    lateinit var reactionDb: ReactionDatabase
    @Inject
    lateinit var beldexMessageDb: BeldexMessageDatabase
    @Inject
    lateinit var smsDb: SmsDatabase
    @Inject
    lateinit var mmsDb: MmsDatabase
    @Inject
    lateinit var threadDb: ThreadDatabase
    private val reactWithAnyEmojiStartPage = -1

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        // Set content view
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        glide = Glide.with(this)
        binding.profileButton.root.glide = glide

        binding.navigationMenu.navigationRv.layoutManager = LinearLayoutManager(this)
        binding.navigationMenu.navigationRv.setHasFixedSize(true)
        updateAdapter(0)
        // Add Item Touch Listener
        binding.navigationMenu.navigationRv.addOnItemTouchListener(RecyclerTouchListener(this, object :
            ClickListener {
            override fun onClick(view: View, position: Int) {
                when (position) {
                    0 -> {
                        // # Privacy Activity
                        showPrivacySettings()
                    }
                    1 -> {
                        // # Notification Activity
                        showNotificationSettings()
                    }
                    2 -> {
                        // # Message Requests Activity
                        showMessageRequests()
                    }
                    3 -> {
                        // # Recovery Seed Activity
                        showSeed()
                    }
                    4 -> {
                        // # Support
                        sendMessageToSupport()
                        binding.drawerLayout.closeDrawer(GravityCompat.END)
                    }
                    5 -> {
                        // # Help Activity
                        help()
                    }
                    6 -> {
                        // # Invite Activity
                        sendInvitation(hexEncodedPublicKey)
                    }
                    7 -> {
                        // # About Activity
                        showAbout()
                    }
                }
                // Don't highlight the 'Profile' and 'Like us on Facebook' item row
                if (position != 4 && position != 3) {
                    updateAdapter(position)
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                }, 200)
            }
        }))

        binding.profileButton.root.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
        binding.navigationMenu.drawerCloseIcon.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.END) }
        val activeUiMode = UiModeUtilities.getUserSelectedUiMode(this)
        binding.navigationMenu.drawerAppearanceToggleButton.isChecked = activeUiMode == UiMode.NIGHT

        binding.navigationMenu.drawerAppearanceToggleButton.setOnClickListener{
            if(binding.navigationMenu.drawerAppearanceToggleButton.isChecked){
                val uiMode = UiMode.entries[1]
                UiModeUtilities.setUserSelectedUiMode(this, uiMode)
            }
            else{
                val uiMode = UiMode.entries[0]
                UiModeUtilities.setUserSelectedUiMode(this, uiMode)
            }
        }
        binding.navigationMenu.drawerAppearanceToggleButton.setOnTouchListener { _, event ->
            event.actionMasked == MotionEvent.ACTION_MOVE
        }
        binding.navigationMenu.profileContainer.setOnClickListener{
            openSettings()
        }
        binding.navigationMenu.drawerProfileIcon.root.setOnClickListener {
            openSettings()
        }
        binding.navigationMenu.drawerProfileIcon.root.glide = glide
        binding.navigationMenu.drawerProfileIcon.root.isClickable = true
        binding.navigationMenu.drawerProfileId.text = String.format(this.resources.getString(R.string.id_format), hexEncodedPublicKey)
        binding.bchatToolbar.disableClipping()
        setupHeaderImage()

        homeAdapter.glide = glide
        binding.recyclerView.adapter = homeAdapter
        swipeHelper.attachToRecyclerView(binding.recyclerView)
        val itemDecorator = RecyclerViewDivider(this,
            R.drawable.ic_divider
            ,0,
            0
        )
        binding.recyclerView.addItemDecoration(itemDecorator)

        binding.createNewPrivateChatButton.setOnClickListener { openNewConversationChat() }

        homeViewModel.getObservable(this).observe(this) { newData ->
            val manager = binding.recyclerView.layoutManager as LinearLayoutManager
            val firstPos = manager.findFirstCompletelyVisibleItemPosition()
            val offsetTop = if(firstPos >= 0) {
                manager.findViewByPosition(firstPos)?.let { view ->
                    manager.getDecoratedTop(view) - manager.getTopDecorationHeight(view)
                } ?: 0
            } else 0
            val messageRequestCount = threadDb.unapprovedConversationCount
            var request = emptyList<ThreadRecord>()
            if (messageRequestCount > 0 && !TextSecurePreferences.hasHiddenMessageRequests(this)) {
                threadDb.unapprovedConversationList.use { openCursor ->
                    val reader = threadDb.readerFor(openCursor)
                    val threads = mutableListOf<ThreadRecord>()
                    while (true) {
                        threads += reader.next ?: break
                    }
                    threads.sortedByDescending { it.dateReceived }
                    request = threads
                }
            }
            binding.requests.setContent {
                BChatTheme {
                    MessageRequestsView(
                        requests = request,
                        openSearch = {
                            Intent(this, SearchActivity::class.java).also {
                                searchResultLauncher.launch(it)
                            }
                        },
                        ignoreRequest = {
                            val dialog = ConversationActionDialog()
                            dialog.apply {
                                arguments = Bundle().apply {
                                    putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, it)
                                    putSerializable(ConversationActionDialog.EXTRA_DIALOG_TYPE, HomeDialogType.IgnoreRequest)
                                }
                                setListener(this@HomeActivity)
                            }
                            dialog.show(supportFragmentManager, ConversationActionDialog.TAG)
                        },
                        openChat = {
                            onConversationClick(it.threadId)
                        },
                        modifier =Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal=16.dp
                            )
                    )
                }
            }
            homeAdapter.data = newData
            if(firstPos >= 0) { manager.scrollToPositionWithOffset(firstPos, offsetTop) }
            //setupMessageRequestsBanner()
            updateEmptyState()
        }
        ApplicationContext.getInstance(this).typingStatusRepository.typingThreads.observe(this) { threadIds ->
            homeAdapter.typingThreadIDs = (threadIds ?: setOf())
        }
        homeViewModel.tryUpdateChannel()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ArchiveChatCountRepository.archiveCount.collect { count ->
                    updateArchiveCountUI(count)
                }
            }
        }

        // Observe blocked contacts changed events
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.recyclerView.adapter!!.notifyDataSetChanged()
            }
        }
        this.broadcastReceiver = broadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter("blockedContactsChanged"))
        //PathStatus
        registerObservers()
        callLifeCycleScope(binding.recyclerView, mmsSmsDatabase,globalSearchAdapter,publicKey,binding.profileButton.root,binding.navigationMenu.drawerProfileName,binding.navigationMenu.drawerProfileIcon.root)
        binding.chatButtons.setContent {
            BChatTheme {
                NewChatButtons(
                    openNewConversationChat = {
                        openNewConversationChat()
                    },
                )
            }
        }

        binding.navigationMenu.menuContainer.run {
            post {
                val params = layoutParams as DrawerLayout.LayoutParams
                params.width = (getScreenWidth() * 0.7).toInt()
                layoutParams = params
                translationX = -(this@HomeActivity.toPx(16))
            }
        }
        binding.navigationMenu.version.text = resources.getString(R.string.version_name).format(BuildConfig.VERSION_NAME)

        networkChangedReceiver = NetworkChangeReceiver(::networkChange)
        networkChangedReceiver!!.register(this)

        IP2Country.configureIfNeeded(this@HomeActivity)
        EventBus.getDefault().register(this@HomeActivity)

        //New Line App Update
        /*binding.airdropIcon.setAnimation(R.raw.airdrop_animation_top)
        binding.airdropIcon.setOnClickListener { callAirdropUrl() }*/
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        checkUpdate()


        lifecycleScope.launch {
            delay(2000)
            val showPromotion = remoteConfig.showPromotionalOffer()
            val dialogClicked = textSecurePreferences.getPromotionDialogClicked()
            val ignoredCount = textSecurePreferences.getPromotionDialogIgnoreCount()
            if (showPromotion && !dialogClicked && ignoredCount < 3) {
                val dialog = PromotionOfferDialog.newInstance()
                dialog.isCancelable = false
                dialog.show(supportFragmentManager, PromotionOfferDialog.TAG)
            }
        }

        /*if(TextSecurePreferences.getAirdropAnimationStatus(this)) {
            //New Line AirDrop
            TextSecurePreferences.setAirdropAnimationStatus(this,false)
            launchSuccessLottieDialog()
        }*/
    }

    private fun networkChange(networkAvailable: Boolean) {
        if (networkAvailable) {
            checkIsBnsHolder()
        }
    }

    private fun updateArchiveCountUI(count: Int) {
        if (count != 0) {
            binding.archiveChatCardView.visibility=View.VISIBLE
            binding.archiveChatDivider.visibility=View.VISIBLE
            binding.archiveChatCardView.setContent {
                BChatTheme {
                    ArchiveChatView(
                        count = count,
                        onRequestClick={
                            showArchiveChats()
                        },
                        context=this
                    )
                }
            }
        } else {
            binding.archiveChatCardView.visibility=View.GONE
            binding.archiveChatDivider.visibility=View.GONE
        }
    }

    private fun checkIsBnsHolder(){
        val isBnsHolder = TextSecurePreferences.getIsBNSHolder(this)
        val publicKey = TextSecurePreferences.getLocalNumber(this)
        if(!isBnsHolder.isNullOrEmpty() && !publicKey.isNullOrEmpty()){
            verifyBNS(isBnsHolder,publicKey,this) {
                if (!it) {
                    TextSecurePreferences.setIsBNSHolder(this, null)
                    MessagingModuleConfiguration.shared.storage.setIsBnsHolder(publicKey, false)
                    updateProfileButton()
                }
            }
        }
    }

    private fun verifyBNS(bnsName: String, publicKey: String?, context: Context, result: (status: Boolean) -> Unit) {
        // This could be an BNS name
        MnodeAPI.getBchatID(bnsName).successUi { hexEncodedPublicKey ->
            if(hexEncodedPublicKey == publicKey){
                result(true)
            }else{
                result(false)
                Toast.makeText(context, context.resources.getString(R.string.invalid_bns_warning_message), Toast.LENGTH_SHORT).show()
            }
        }.failUi { exception ->
            var message =
                context.resources.getString(R.string.bns_name_changed_warning_message)
            exception.localizedMessage?.let {
                message = context.resources.getString(R.string.bns_name_changed_warning_message)
                Log.d("Beldex", "BNS exception $it")
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            result(false)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateProfileEvent(event: ProfilePictureModifiedEvent) {
        if (event.recipient.isLocalNumber) {
            updateProfileButton()
        }else { 
            homeViewModel.tryUpdateChannel()
            updateAdapter()
        }
    }

    fun updateAdapter(){
        homeAdapter.notifyDataSetChanged()
    }

    fun updateProfileButton() {
        binding.profileButton.root.publicKey = publicKey
        binding.profileButton.root.displayName = TextSecurePreferences.getProfileName(this)
        binding.profileButton.root.recycle()
        binding.profileButton.root.update(TextSecurePreferences.getProfileName(this))

        //New Line
        binding.navigationMenu.drawerProfileName.text = TextSecurePreferences.getProfileName(this)
        binding.navigationMenu.drawerProfileIcon.root.publicKey = publicKey
        binding.navigationMenu.drawerProfileIcon.root.displayName = TextSecurePreferences.getProfileName(this)
        binding.navigationMenu.drawerProfileIcon.root.recycle()
        binding.navigationMenu.drawerProfileIcon.root.update(TextSecurePreferences.getProfileName(this))
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
                this.immediateAppUpdateRequestCode
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    private fun setupToolbarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val statusBarHeight =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarCall) { view, insets ->
            val statusBarHeight =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }
    }

    @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //New Line App Update
        if (requestCode == immediateAppUpdateRequestCode) {
            when (resultCode) {
                RESULT_CANCELED -> {
                    Toast.makeText(
                        applicationContext,
                        "Update canceled by user! Result Code: $resultCode", Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                RESULT_OK -> {
                    Toast.makeText(
                        applicationContext,
                        "Update success! Result Code: $resultCode",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    Toast.makeText(
                        applicationContext,
                        "Update Failed! Result Code: $resultCode",
                        Toast.LENGTH_LONG
                    ).show()
                    checkUpdate()
                }
            }
        }

        /*val fragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
        if(fragment is ConversationFragmentV2) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }*/
    }

     fun callLifeCycleScope(
        recyclerView: RecyclerView,
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
                // Set up remaining components if needed
                pushRegistry.refresh(false)
                val userPublicKey = TextSecurePreferences.getLocalNumber(this@HomeActivity)
                if (userPublicKey != null) {
                    OpenGroupManager.startPolling()
                    JobQueue.shared.resumePendingJobs()
                }
                // Set up typing observer
                withContext(Dispatchers.Main) {
                    updateProfileButton(profileButton,drawerProfileName,drawerProfileIcon,publicKey)
                    TextSecurePreferences.events.filter { it == TextSecurePreferences.PROFILE_NAME_PREF }.collect {
                        updateProfileButton(profileButton,drawerProfileName,drawerProfileIcon,publicKey)
                    }
                }
            }
            // monitor the global search VM query
//            launch {
//                globalSearchInputLayout.query
//                    .onEach(globalSearchViewModel::postQuery)
//                    .collect()
//            }
            // Get group results and display them
            launch {
                globalSearchViewModel.result.collect { result ->
                    val contactAndGroupList =
                        result.contacts.map { GlobalSearchAdapter.Model.Contact(it) } +
                                result.threads.map { GlobalSearchAdapter.Model.GroupConversation(it) }

                    val contactResults = contactAndGroupList.toMutableList()

                    if (contactResults.isEmpty()) {
                        contactResults.add(
                            GlobalSearchAdapter.Model.SavedMessages(
                                publicKey
                            )
                        )
                    }

                    val userIndex =
                        contactResults.indexOfFirst { it is GlobalSearchAdapter.Model.Contact && it.contact.bchatID == publicKey }
                    if (userIndex >= 0) {
                        contactResults[userIndex] =
                            GlobalSearchAdapter.Model.SavedMessages(publicKey)
                    }

                    if (contactResults.isNotEmpty()) {
                        contactResults.add(
                            0,
                            GlobalSearchAdapter.Model.Header(R.string.global_search_contacts_groups)
                        )
                    }

                    val unreadThreadMap = result.messages
                        .groupBy { it.threadId }.keys.associateWith {
                            mmsSmsDatabase.getUnreadCount(
                                it
                            )
                        }

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
    fun replaceFragment(newFragment: Fragment, stackName: String?, extras: Bundle?) {
/*        if (extras != null) {
            newFragment.arguments = extras
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_home_frame_layout_container, newFragment)
            .addToBackStack(stackName)
            .commit()*/
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
        profileButton.update(TextSecurePreferences.getProfileName(this))

        //New Line
        drawerProfileName.text = TextSecurePreferences.getProfileName(this)
        drawerProfileIcon.publicKey = publicKey
        drawerProfileIcon.displayName = TextSecurePreferences.getProfileName(this)
        drawerProfileIcon.recycle()
        drawerProfileIcon.update(TextSecurePreferences.getProfileName(this))
    }

    private fun passGlobalSearchAdapterModelSavedMessagesValue(address: Address) {
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS,address)
        startActivity(intent)
    }

    private fun passGlobalSearchAdapterModelContactValue(address: Address) {
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS,address)
        startActivity(intent)
    }

    private fun passGlobalSearchAdapterModelGroupConversationValue(threadId: Long) {
        val extras = Bundle()
        val intent = Intent(this, ConversationActivityV2::class.java)
        extras.putLong(ConversationActivityV2.THREAD_ID,threadId)
        intent.putExtras(extras)
        startActivity(intent)
    }

    private fun passGlobalSearchAdapterModelMessageValue(
        threadId: Long,
        timestamp: Long,
        author: Address
    ) {
        val extras = Bundle()
        extras.putLong(ConversationActivityV2.THREAD_ID,threadId)
        extras.putLong(ConversationActivityV2.SCROLL_MESSAGE_ID,timestamp)
        extras.putParcelable(ConversationActivityV2.SCROLL_MESSAGE_AUTHOR,author)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtras(extras)
        startActivity(intent)
    }

    private val swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) = true

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val thread =homeAdapter.data[position]
            deleteConversation(thread)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val width = Resources.getSystem().displayMetrics.widthPixels
            val deleteIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_24, null)
            when {
                abs(dX) < width / 3 -> {
                    println(">>>>>swipe1")
                }
                dX > width / 3 -> {
                    println(">>>>>swipe2")
                }
                else -> {
                    println(">>>>>swipe3")
                }
            }
            val textMargin = resources.getDimension(R.dimen.fab_margin).roundToInt()
            deleteIcon ?: return
            val top = viewHolder.itemView.top + (viewHolder.itemView.bottom - viewHolder.itemView.top) / 2 - deleteIcon.intrinsicHeight / 2
            deleteIcon.bounds = Rect(
                width - textMargin - deleteIcon.intrinsicWidth,
                top,
                width - textMargin,
                top + deleteIcon.intrinsicHeight
            )
            if (dX < 0) deleteIcon.draw(c)
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    })

    private fun updateAdapter(highlightItemPos: Int) {
        adapter = NavigationRVAdapter(items, highlightItemPos)
        binding.navigationMenu.navigationRv.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun showPrivacySettings() {
        Intent(this, PrivacySettingsActivity::class.java).also {
            push(it)
        }
    }

    private fun showNotificationSettings() {
        Intent(this, NotificationSettingsActivity::class.java).also {
            push(it)
        }
    }

    private fun onConversationClick(threadId: Long) {
        val extras = Bundle()
        extras.putLong(ConversationActivityV2.THREAD_ID, threadId)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtras(extras)
        startActivity(intent)
    }

    private fun openSettings() {
        Intent(this, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.SettingsScreen.route)
            startActivity(it)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }, 200)
    }

    private fun setupHeaderImage() {
        val isDayUiMode = UiModeUtilities.isDayUiMode(this)
        val headerTint = if (isDayUiMode) R.color.black else R.color.white
        binding.bchatHeaderImage.setTextColor(
            ContextCompat.getColor(
                this,
                headerTint
            )
        )
    }

    override fun onConversationClick(thread : ThreadRecord) {
        onConversationClick(thread.threadId)
    }

    override fun onLongConversationClick(thread : ThreadRecord, view : View) {
        val recipient = thread.recipient
        val popupMenu = PopupMenu(this, view, R.style.PopupMenu)
        popupMenu.menuInflater.inflate(R.menu.menu_conversation_v2, popupMenu.menu)
        popupMenu.gravity = Gravity.END
        popupMenu.setForceShowIcon(true)
        val item : MenuItem= popupMenu.menu.findItem(R.id.menu_delete)
        val s=SpannableString("Delete")
        s.setSpan(ForegroundColorSpan(this.getColor(R.color.red)), 0, s.length, 0)
        item.setTitle(s)
        with(popupMenu.menu) {
            if (recipient.isGroupRecipient && !recipient.isLocalNumber) {
                findItem(R.id.menu_details).setVisible(false)
                findItem(R.id.menu_unblock).setVisible(false)
                findItem(R.id.menu_block).setVisible(false)
            } else if(recipient.isLocalNumber){
                findItem(R.id.menu_details).setVisible(false)
                findItem(R.id.menu_unblock).setVisible(false)
                findItem(R.id.menu_block).setVisible(false)
            }else{
                findItem(R.id.menu_details).setVisible(true)
                findItem(R.id.menu_unblock).setVisible(recipient.isBlocked)
                findItem(R.id.menu_block).setVisible(!recipient.isBlocked)
            }

            findItem(R.id.menu_unmute_notifications).setVisible(recipient.isMuted && !recipient.isLocalNumber)
            findItem(R.id.menu_mute_notifications).setVisible(!recipient.isMuted && !recipient.isLocalNumber)
            findItem(R.id.menu_notification_settings).setVisible(recipient.isGroupRecipient && !recipient.isMuted && isSecretGroupIsActive(recipient))
            findItem(R.id.menu_mark_read).setVisible(thread.unreadCount > 0)
            findItem(R.id.menu_pin).setVisible(!thread.isPinned)
            findItem(R.id.menu_unpin).setVisible(thread.isPinned)
            findItem(R.id.menu_archive_chat).setVisible(true)
        }
        popupMenu.setOnMenuItemClickListener {
            handlePopUpMenuClickListener(it, thread)
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

    override fun showMessageRequests() {
        Intent(this, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.MessageRequestsScreen.route)
            resultLauncher.launch(it)
        }
    }

    override fun hideMessageRequests() {
        val dialog = AlertDialog.Builder(this, R.style.BChatAlertDialog_New)
            .setTitle("Hide message requests?")
            .setMessage("Once they are hidden, you can access them from Settings > Message Requests")
            .setPositiveButton(R.string.yes) { _, _ ->
               textSecurePreferences.setHasHiddenMessageRequests()
                homeViewModel.tryUpdateChannel()
            }
            .setNegativeButton(R.string.no) { _, _ ->
                // Do nothing
            }.show()

        //SteveJosephh21
        val message: TextView = dialog.findViewById(android.R.id.message)
        val messageFace: Typeface= Typeface.createFromAsset(this.assets, "fonts/open_sans_medium.ttf")
        message.typeface = messageFace
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(ConversationActivityV2.THREAD_ID, result.data!!.getLongExtra(ConversationActivityV2.THREAD_ID,-1))
            val intent = Intent(this, ConversationActivityV2::class.java)
            intent.putExtras(extras)
            startActivity(intent)
        }
    }

    override fun openNewConversationChat() {
        val intent = Intent(this, NewChatConversationActivity::class.java)
        createNewPrivateChatResultLauncher.launch(intent)
    }

    private var createNewPrivateChatResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            when(result.data!!.getIntExtra(ConversationActivityV2.ACTIVITY_TYPE,1)){
                1 -> { //New Chat
                    val extras = Bundle()
                    extras.putParcelable(ConversationActivityV2.ADDRESS,result.data!!.parcelable(ConversationActivityV2.ADDRESS))
                    extras.putLong(ConversationActivityV2.THREAD_ID, result.data!!.getLongExtra(ConversationActivityV2.THREAD_ID,-1))
                    extras.putParcelable(ConversationActivityV2.URI,result.data!!.parcelable(ConversationActivityV2.URI))
                    extras.putString(ConversationActivityV2.TYPE,result.data!!.getStringExtra(ConversationActivityV2.TYPE))
                    extras.putString(ConversationActivityV2.BNS_NAME,result.data!!.getStringExtra(ConversationActivityV2.BNS_NAME))
                    val intent = Intent(this, ConversationActivityV2::class.java)
                    intent.putExtras(extras)
                    startActivity(intent)
                }
                2 -> { // Secret Group
                    createNewSecretGroup()
                }
                3 -> { // Social Group
                    joinSocialGroup()
                }
                4 -> { // Note to Self
                    val recipient = Recipient.from(this, Address.fromSerialized(hexEncodedPublicKey), false)
                    passGlobalSearchAdapterModelContactValue(recipient.address)
                }
                5 -> { // Invite a Friend
                    sendInvitation(hexEncodedPublicKey)
                }
                6 -> { // Individual Conversation
                    val extras = Bundle()
                    extras.putParcelable(ConversationActivityV2.ADDRESS,result.data!!.parcelable(ConversationActivityV2.ADDRESS))
                    val intent = Intent(this, ConversationActivityV2::class.java)
                    intent.putExtras(extras)
                    startActivity(intent)
                }
                else -> return@registerForActivityResult
            }
        }
    }

    override fun createNewSecretGroup() {
        val intent = Intent(this, NewGroupConversationActivity::class.java).apply {
            putExtra(NewGroupConversationActivity.EXTRA_DESTINATION, NewGroupConversationType.SecretGroup.destination)
        }
        createClosedGroupActivityResultLauncher.launch(intent)
    }

    private var createClosedGroupActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(ConversationActivityV2.THREAD_ID, result.data!!.getLongExtra(ConversationActivityV2.THREAD_ID,-1))
            extras.putParcelable(ConversationActivityV2.ADDRESS,result.data!!.parcelable(ConversationActivityV2.ADDRESS))
            val intent = Intent(this, ConversationActivityV2::class.java)
            intent.putExtras(extras)
            startActivity(intent)
        }
        if (result.resultCode == CreateClosedGroupActivity.closedGroupCreatedResultCode) {
            openNewConversationChat()
        }
    }

    override fun joinSocialGroup() {
        val intent = Intent(this, NewGroupConversationActivity::class.java).apply {
            putExtra(NewGroupConversationActivity.EXTRA_DESTINATION, NewGroupConversationType.PublicGroup.destination)
        }
        joinPublicChatNewActivityResultLauncher.launch(intent)
    }

    private var joinPublicChatNewActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(
                ConversationActivityV2.THREAD_ID,
                result.data!!.getLongExtra(ConversationActivityV2.THREAD_ID, -1)
            )
            extras.putParcelable(
                ConversationActivityV2.ADDRESS,
                result.data!!.parcelable(ConversationActivityV2.ADDRESS)
            )
            val intent = Intent(this, ConversationActivityV2::class.java)
            intent.putExtras(extras)
            startActivity(intent)
        }
    }

    private fun toolBarCall() {
        Intent(this, WebRTCComposeActivity::class.java).also {
            push(it)
        }
    }

    private fun sendInvitation(hexEncodedPublicKey:String) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        val invitation = String.format(this.resources.getString(R.string.invitation_msg), hexEncodedPublicKey)
        intent.putExtra(Intent.EXTRA_TEXT, invitation)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.activity_settings_invite_button_title))
        startActivity(chooser)
    }


    private fun showAbout() {
        Intent(this, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.AboutScreen.route)
            startActivity(it)
        }
    }

    private fun help() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@beldex.io"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        startActivity(intent)
    }

    private fun showSeed() {
//        Intent(requireContext(), SeedPermissionActivity::class.java).also {
//            show(it)
//        }
        Intent(this, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.RecoverySeedScreen.route)
            startActivity(it)
        }
    }

    private fun handlePopUpMenuClickListener(item: MenuItem, thread: ThreadRecord) {
        when (item.itemId) {
            R.id.menu_details -> {
                val userDetailsBottomSheet = UserDetailsBottomSheet()
                val bundle = bundleOf(
                    UserDetailsBottomSheet.ARGUMENT_PUBLIC_KEY to thread.recipient.address.toString(),
                    UserDetailsBottomSheet.ARGUMENT_THREAD_ID to thread.threadId
                )
                userDetailsBottomSheet.arguments = bundle
                userDetailsBottomSheet.show(supportFragmentManager, UserDetailsBottomSheet.TAG)
            }
            R.id.menu_pin -> {
                setConversationPinned(thread.threadId, true)
            }
            R.id.menu_unpin -> {
                setConversationPinned(thread.threadId, false)
            }
            R.id.menu_block -> {
                if (!thread.recipient.isBlocked) {
                    blockConversation(thread)
                }
            }
            R.id.menu_unblock -> {
                if (thread.recipient.isBlocked) {
                    unblockConversation(thread)
                }
            }
            R.id.menu_mute_notifications -> {
                setConversationMuted(thread, true)
            }
            R.id.menu_unmute_notifications -> {
                setConversationMuted(thread, false)
            }
            R.id.menu_notification_settings -> {
                val dialog = ConversationActionDialog()
                dialog.apply {
                    arguments = Bundle().apply {
                        putInt(ConversationActionDialog.EXTRA_ARGUMENT_3, thread.recipient.notifyType)
                        putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                        putSerializable(ConversationActionDialog.EXTRA_DIALOG_TYPE, HomeDialogType.NotificationSettings)
                    }
                    setListener(this@HomeActivity)
                }
                dialog.show(supportFragmentManager, ConversationActionDialog.TAG)
            }
            R.id.menu_mark_read -> {
                markAllAsRead(thread)
            }
            R.id.menu_delete -> {
                deleteConversation(thread)
            }
            R.id.menu_archive_chat ->{
                archiveChatViewModel?.let { archiveConversation(thread, it) }
            }
            else -> Unit
        }
    }

    private fun blockConversation(thread: ThreadRecord) {
        val blockDialog = ConversationActionDialog()
        blockDialog.apply {
            arguments = Bundle().apply {
                putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                putSerializable(ConversationActionDialog.EXTRA_DIALOG_TYPE, HomeDialogType.BlockUser)
            }
            setListener(this@HomeActivity)
        }
        blockDialog.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    private fun unblockConversation(thread: ThreadRecord) {
        val unBlockDialog = ConversationActionDialog()
        unBlockDialog.apply {
            arguments = Bundle().apply {
                putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                putSerializable(ConversationActionDialog.EXTRA_DIALOG_TYPE, HomeDialogType.UnblockUser)
            }
            setListener(this@HomeActivity)
        }
        unBlockDialog.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    private fun setConversationMuted(thread: ThreadRecord, isMuted: Boolean) {
        if (!isMuted) {
            lifecycleScope.launch(Dispatchers.IO) {
                recipientDatabase.setMuted(thread.recipient, 0)
                withContext(Dispatchers.Main) {
                    binding.recyclerView.adapter!!.notifyDataSetChanged()
                }
            }
        } else {
            val dialog = ConversationActionDialog()
            dialog.apply {
                arguments = Bundle().apply {
                    putSerializable(ConversationActionDialog.EXTRA_ARGUMENT_3, thread.recipient.mutedUntil)
                    putSerializable(ConversationActionDialog.EXTRA_DIALOG_TYPE, HomeDialogType.MuteChat)
                    putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                }
                setListener(this@HomeActivity)
            }
            dialog.show(supportFragmentManager, ConversationActionDialog.TAG)
        }
    }

    private fun setNotifyType(thread: ThreadRecord, newNotifyType: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            recipientDatabase.setNotifyType(thread.recipient, newNotifyType)
            withContext(Dispatchers.Main) {
                binding.recyclerView.adapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun setConversationPinned(
        threadId: Long,
        pinned: Boolean
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            threadDb.setPinned(threadId, pinned)
            homeViewModel.tryUpdateChannel()
        }
    }

    private fun markAllAsRead(thread: ThreadRecord) {
        ThreadUtils.queue {
            threadDb.markAllAsRead(thread.threadId, thread.recipient.isOpenGroupRecipient)
        }
    }

    private fun deleteConversation(thread: ThreadRecord) {
        val recipient = thread.recipient
        val message = if (recipient.isGroupRecipient) {
            val group = groupDb.getGroup(recipient.address.toString()).orNull()
            if (group != null && group.admins.map { it.toString() }
                    .contains(TextSecurePreferences.getLocalNumber(this))) {
                "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
            } else {
                resources.getString(R.string.activity_home_leave_group_dialog_message)
            }
        } else {
            resources.getString(R.string.activity_home_delete_conversation_dialog_message)
        }
        val deleteConversation = ConversationActionDialog()
        deleteConversation.apply {
            arguments = Bundle().apply {
                putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                putString(ConversationActionDialog.EXTRA_ARGUMENT_1, message)
                putSerializable(ConversationActionDialog.EXTRA_DIALOG_TYPE, HomeDialogType.DeleteChat)
            }
            setListener(this@HomeActivity)
        }
        deleteConversation.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    private fun archiveConversation(thread : ThreadRecord, archiveChatViewModel : ArchiveChatViewModel){
        val threadID = thread.threadId
        DatabaseComponent.get(this).bchatJobDatabase()
            .cancelPendingMessageSendJobs(threadID)
        lifecycleScope.launch(Dispatchers.IO) {
            threadDb.setThreadArchived(threadID)
            ArchiveChatCountRepository.updateArchiveCount(threadDb.archivedConversationList.count)
        }
    }

    private fun showArchiveChats(){
        Intent(this, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.ArchiveChatScreen.route)
            resultLauncher.launch(it)
        }
    }

    private fun updateEmptyState() {
        val threadCount=(binding.recyclerView.adapter)!!.itemCount
        binding.emptyStateContainer.isVisible=
            threadCount == 0 && binding.recyclerView.isVisible && threadDb.archivedConversationList.count == 0
        binding.emptyStateContainerText.isVisible=
            threadCount == 0 && binding.recyclerView.isVisible && threadDb.archivedConversationList.count == 0

        val isDayUiMode=UiModeUtilities.isDayUiMode(this)
        (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
            binding.emptyStateImageView.setImageResource(
                it
            )
        }
    }

    private fun registerObservers() {
        val buildingPathsReceiver : BroadcastReceiver=object : BroadcastReceiver() {

            override fun onReceive(context : Context, intent : Intent) {
                handleBuildingPathsEvent()
            }
        }
        broadcastReceivers.add(buildingPathsReceiver)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(buildingPathsReceiver, IntentFilter("buildingPaths"))
        val pathsBuiltReceiver : BroadcastReceiver=object : BroadcastReceiver() {

            override fun onReceive(context : Context, intent : Intent) {
                handlePathsBuiltEvent()
            }
        }
        broadcastReceivers.add(pathsBuiltReceiver)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(pathsBuiltReceiver, IntentFilter("pathsBuilt"))
    }

    private fun handleBuildingPathsEvent() { update() }
    private fun handlePathsBuiltEvent() { update() }

    private fun update() {
        binding.hopsWarningLayout.visibility = when {
            OnionRequestAPI.paths.isNotEmpty() -> View.GONE
            OnionRequestAPI.paths.isEmpty() && CheckOnline.isOnline(this) -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun isSecretGroupIsActive(recipient: Recipient):Boolean {
        return if (recipient.isClosedGroupRecipient) {
            val group = getGroup(recipient)
            val isActive = (group?.isActive == true)
            isActive
        } else {
            true
        }
    }
    fun getGroup(recipient: Recipient): GroupRecord? = groupDb.getGroup(recipient.address.toGroupString()).orNull()

    private fun setupCallActionBar() {
        val startTimeNew = callViewModel.callStartTime
        if (startTimeNew == -1L) {
            binding.toolbarCall.isVisible = false
        } else {
            binding.toolbarCall.isVisible = true
            uiJob = lifecycleScope.launch {
                launch {
                    while (isActive) {
                        val startTime = callViewModel.callStartTime
                        if (startTime == -1L) {
                            binding.toolbarCall.isVisible = false
                        } else {
                            binding.toolbarCall.isVisible = true
                            binding.callDurationCall.text = DurationFormatUtils.formatDuration(
                                System.currentTimeMillis() - startTime,
                                callDurationFormat
                            )
                        }

                        delay(1_000)
                    }
                }
            }
        }
        binding.hanUpCall.setOnClickListener {
            this.startService(WebRtcCallService.hangupIntent(this))
            binding.toolbarCall.isVisible = false
            Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
        }
        binding.toolbarCall.setOnClickListener {
            toolBarCall()
        }
    }



    override fun handleSeedReminderViewContinueButtonTapped() {
        val intent = Intent(this, SeedActivity::class.java)
        show(intent)
    }

    fun sendMessageToSupport() {
        val recipient = Recipient.from(this, Address.fromSerialized(reportIssueBChatID), false)
        val extras = Bundle()
        extras.putParcelable(ConversationActivityV2.ADDRESS, recipient.address)
        val existingThread =
            DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        extras.putLong(ConversationActivityV2.THREAD_ID, existingThread)
        extras.putParcelable(ConversationActivityV2.URI, intent.data)
        extras.putString(ConversationActivityV2.TYPE, intent.type)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtras(extras)
        startActivity(intent)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this@HomeActivity)

        //Wallet
        Timber.d("onDestroy")
        dismissProgressDialog()
        //Important
        //unregisterDetachReceiver()
        //Ledger.disconnect()
        networkChangedReceiver?.unregister(this)
        networkChangedReceiver = null
        super.onDestroy()
    }


    override fun callConversationFragmentV2(address: Address, threadId: Long) {
        val extras = Bundle()
        extras.putParcelable(ConversationActivityV2.ADDRESS,address)
        extras.putLong(ConversationActivityV2.THREAD_ID,threadId)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtras(extras)
        startActivity(intent)
    }

    fun playVoiceMessageAtIndexIfPossible(indexInAdapter: Int) {
        /*val fragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
        if(fragment is ConversationFragmentV2) {
            fragment.playVoiceMessageAtIndexIfPossible(indexInAdapter)
        }*/
    }

    override fun getSystemService(name: String): Any? {
        if (name == ActivityDispatcher.SERVICE) {
            return this
        }
        return super.getSystemService(name)
    }


    override fun onResume() {
        super.onResume()
        Timber.d("onResume()-->")
        //Important
        //if (!Ledger.isConnected()) attachLedger()
        if(!CheckOnline.isOnline(this)){
            Toast.makeText(this,getString(R.string.please_check_your_internet_connection),Toast.LENGTH_SHORT).show()
        }
        setupCallActionBar()
        ApplicationContext.getInstance(this).messageNotifier.setHomeScreenVisible(false)
        if (TextSecurePreferences.getLocalNumber(this) == null) {
            return; } // This can be the case after a secondary device is auto-cleared
        IdentityKeyUtil.checkUpdate(this)
        binding.profileButton.root.recycle() // clear cached image before update tje profilePictureView
        binding.profileButton.root.update(TextSecurePreferences.getProfileName(this))

        binding.navigationMenu.drawerProfileIcon.root.recycle()
        binding.navigationMenu.drawerProfileIcon.root.update(TextSecurePreferences.getProfileName(this))

        if (TextSecurePreferences.getConfigurationMessageSynced(this)) {
            lifecycleScope.launch(Dispatchers.IO) {
                ConfigurationMessageUtilities.syncConfigurationIfNeeded(this@HomeActivity)
            }
        }
        if (TextSecurePreferences.isUnBlocked(this)) {
            homeAdapter.notifyDataSetChanged()
            TextSecurePreferences.setUnBlockStatus(this, false)
        }
        if(!TextSecurePreferences.isCopiedSeed(this)){
            showSaveYourSeedDialog()
        }
        ArchiveChatCountRepository.updateArchiveCount(
            threadDb.archivedConversationList.count
        )
    }

    override fun onPause() {
        super.onPause()
        val dialog = supportFragmentManager.findFragmentByTag(ConversationActionDialog.TAG)
        if (dialog is DialogFragment) {
            dialog.dismissAllowingStateLoss()
        }
        val bottomSheet = supportFragmentManager.findFragmentByTag(UserDetailsBottomSheet.TAG)
        if (bottomSheet is UserDetailsBottomSheet) {
            bottomSheet.dismissAllowingStateLoss()
        }
    }

    private fun showSaveYourSeedDialog(){
        try {
            SaveYourSeedDialogBox(
                    showSeed = {
                        showSeed()
                    }
                ).show(supportFragmentManager, "")
        } catch (exception: Exception) {
            Timber.tag("Beldex").d("Save your seed dialog box exception $exception")
        }
    }

    private var startScanFragment = false

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Helper.PERMISSIONS_REQUEST_CAMERA) { // If request is cancelled, the result arrays are empty.

            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startScanFragment = true
            } else {
                val permissionDialog = ComposeDialogContainer(
                    dialogType = DialogType.PermissionDialog,
                    onConfirm = {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }.let(::startActivity)
                    },
                    onCancel = {}
                )
                permissionDialog.arguments = bundleOf(ComposeDialogContainer.EXTRA_ARGUMENT_1 to String.format(getString(R.string.permissionsCameraDenied), getString(R.string.app_name)))
                permissionDialog.show(this.supportFragmentManager, ComposeDialogContainer.TAG)
            }
        }
    }

    private val passSharedMessageToConversationScreen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if(result.data!=null){
                val extras = Bundle()
                val address = intent.parcelable<Address>(ConversationActivityV2.ADDRESS)
                extras.putParcelable(ConversationActivityV2.ADDRESS, address)
                extras.putLong(ConversationActivityV2.THREAD_ID, result.data!!.getLongExtra(ConversationActivityV2.THREAD_ID,-1))
                val uri = intent.parcelable<Uri>(ConversationActivityV2.URI)
                extras.putParcelable(ConversationActivityV2.URI, uri)
                extras.putString(ConversationActivityV2.TYPE,result.data!!.getStringExtra(ConversationActivityV2.TYPE))
                extras.putCharSequence(Intent.EXTRA_TEXT,result.data!!.getCharSequenceExtra(Intent.EXTRA_TEXT))
                //Shortcut launcher
                extras.putBoolean(ConversationActivityV2.SHORTCUT_LAUNCHER,true)
                val intent = Intent(this, ConversationActivityV2::class.java)
                intent.putExtras(extras)
                startActivity(intent)
            }
        }
    }

    override fun onInputFocusChanged(hasFocus : Boolean) {
        TODO("Not yet implemented")
    }

    override fun onConfirm(dialogType: HomeDialogType, threadRecord: ThreadRecord?) {
        when (dialogType) {
            HomeDialogType.UnblockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, false)
                        withContext(Dispatchers.Main) {
                            binding.recyclerView.adapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
            HomeDialogType.BlockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, true)
                        withContext(Dispatchers.Main) {
                            binding.recyclerView.adapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
            HomeDialogType.DeleteChat -> {
                threadRecord?.let {
                    val threadID = it.threadId
                    val recipient = it.recipient
                    lifecycleScope.launch(Dispatchers.Main) {
                        // Cancel any outstanding jobs
                        DatabaseComponent.get(this@HomeActivity).bchatJobDatabase()
                            .cancelPendingMessageSendJobs(threadID)
                        // Send a leave group message if this is an active closed group
                        if (recipient.address.isClosedGroup && DatabaseComponent.get(this@HomeActivity)
                                .groupDatabase().isActive(recipient.address.toGroupString())
                        ) {
                            var isClosedGroup: Boolean
                            var groupPublicKey: String?
                            try {
                                groupPublicKey =
                                    GroupUtil.doubleDecodeGroupID(recipient.address.toString())
                                        .toHexString()
                                isClosedGroup = DatabaseComponent.get(this@HomeActivity).beldexAPIDatabase()
                                    .isClosedGroup(groupPublicKey)
                            } catch (e: IOException) {
                                groupPublicKey = null
                                isClosedGroup = false
                            }
                            if (isClosedGroup) {
                                MessageSender.explicitLeave(groupPublicKey!!, false)
                            }
                        }
                        // Delete the conversation
                        val v2OpenGroup =
                            DatabaseComponent.get(this@HomeActivity).beldexThreadDatabase()
                                .getOpenGroupChat(threadID)
                        if (v2OpenGroup != null) {
                            OpenGroupManager.delete(
                                v2OpenGroup.server,
                                v2OpenGroup.room,
                                this@HomeActivity
                            )
                        } else {
                            lifecycleScope.launch(Dispatchers.IO) {
                                threadDb.deleteConversation(threadID)
                            }
                        }
                        // Update the badge count
                        ApplicationContext.getInstance(this@HomeActivity).messageNotifier.updateNotification(
                            this@HomeActivity
                        )
                        // Notify the user
                        val toastMessage =
                            if (recipient.isGroupRecipient) R.string.MessageRecord_left_group else R.string.activity_home_conversation_deleted_message
                        Toast.makeText(this@HomeActivity, toastMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            HomeDialogType.IgnoreRequest -> {
                threadRecord.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val recipient = threadRecord?.recipient
                        if (recipient!!.isContactRecipient) {
                            repository.setBlocked(recipient, true)
                            repository.deleteMessageRequest(threadRecord)
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    override fun onCancel(dialogType: HomeDialogType, threadRecord: ThreadRecord?) {
        when (dialogType) {
            HomeDialogType.DeleteChat -> {
                binding.recyclerView.adapter?.notifyDataSetChanged()
            }
            HomeDialogType.IgnoreRequest -> {
                threadRecord.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.deleteMessageRequest(threadRecord!!)
                    }
                }
            }
            else -> Unit
        }
    }

    override fun onConfirmationWithData(
        dialogType: HomeDialogType,
        data: Any?,
        threadRecord: ThreadRecord?
    ) {
        when (dialogType) {
            HomeDialogType.MuteChat -> {
                val index = data as Int
                val muteUntil = when (index) {
                    1 -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
                    2 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
                    3 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
                    4 -> Long.MAX_VALUE
                    else -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    threadRecord?.let {
                        DatabaseComponent.get(this@HomeActivity).recipientDatabase().setMuted(it.recipient, muteUntil)
                        withContext(Dispatchers.Main) {
                            binding.recyclerView.adapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
            HomeDialogType.NotificationSettings -> {
                threadRecord?.let {
                    val index = data as Int
                    DatabaseComponent.get(this@HomeActivity).recipientDatabase().setNotifyType(it.recipient, index.toString().toInt())
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                }
            }
            HomeDialogType.BlockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, true)
                        withContext(Dispatchers.Main) {
                            binding.recyclerView.adapter!!.notifyDataSetChanged()
                        }
                    }
                }

            }
            HomeDialogType.UnblockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, false)
                        withContext(Dispatchers.Main) {
                            binding.recyclerView.adapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}
//endregion

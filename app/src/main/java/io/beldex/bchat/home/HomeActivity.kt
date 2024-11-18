package io.beldex.bchat.home

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfilePictureModifiedEvent
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getWalletName
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getWalletPassword
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.gms.tasks.Task
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.MediaOverviewActivity
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.components.ProfilePictureView
import io.beldex.bchat.compose_utils.ComposeDialogContainer
import io.beldex.bchat.compose_utils.DialogType
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.ConversationViewModel
import io.beldex.bchat.conversation.v2.messages.VoiceMessageViewDelegate
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.data.BarcodeData
import io.beldex.bchat.data.NodeInfo
import io.beldex.bchat.data.TxData
import io.beldex.bchat.data.UserNotes
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.home.search.GlobalSearchAdapter
import io.beldex.bchat.home.search.GlobalSearchViewModel
import io.beldex.bchat.model.AsyncTaskCoroutine
import io.beldex.bchat.model.NetworkType
import io.beldex.bchat.model.PendingTransaction
import io.beldex.bchat.model.TransactionInfo
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.model.WalletManager
import io.beldex.bchat.onboarding.SeedActivity
import io.beldex.bchat.onboarding.SeedReminderViewDelegate
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.FirebaseRemoteConfigUtil
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.IP2Country
import io.beldex.bchat.util.NodePinger
import io.beldex.bchat.util.SharedPreferenceUtil
import io.beldex.bchat.util.parcelable
import io.beldex.bchat.util.push
import io.beldex.bchat.util.show
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.wallet.OnBackPressedListener
import io.beldex.bchat.wallet.OnUriScannedListener
import io.beldex.bchat.wallet.OnUriWalletScannedListener
import io.beldex.bchat.wallet.WalletFragment
import io.beldex.bchat.wallet.info.WalletInfoActivity
import io.beldex.bchat.wallet.jetpackcomposeUI.settings.WalletSettingComposeActivity
import io.beldex.bchat.wallet.jetpackcomposeUI.settings.WalletSettingScreens
import io.beldex.bchat.wallet.node.NodeFragment
import io.beldex.bchat.wallet.receive.ReceiveFragment
import io.beldex.bchat.wallet.rescan.RescanDialog
import io.beldex.bchat.wallet.scanner.ScannerFragment
import io.beldex.bchat.wallet.scanner.WalletScannerFragment
import io.beldex.bchat.wallet.send.SendFragment
import io.beldex.bchat.wallet.service.WalletService
import io.beldex.bchat.wallet.utils.LegacyStorageHelper
import io.beldex.bchat.webrtc.NetworkChangeReceiver
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : PassphraseRequiredActionBarActivity(),SeedReminderViewDelegate,HomeFragment.HomeFragmentListener,ConversationFragmentV2.Listener,UserDetailsBottomSheet.UserDetailsBottomSheetListener,VoiceMessageViewDelegate, ActivityDispatcher,
    WalletFragment.Listener, WalletService.Observer, WalletScannerFragment.OnScannedListener,SendFragment.OnScanListener,SendFragment.Listener,ReceiveFragment.Listener,WalletFragment.OnScanListener,
    ScannerFragment.OnWalletScannedListener,WalletScannerFragment.Listener,NodeFragment.Listener {

    private lateinit var binding: ActivityHomeBinding

    private val globalSearchViewModel by viewModels<GlobalSearchViewModel>()

    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences
    @Inject
    lateinit var viewModelFactory: ConversationViewModel.AssistedFactory

    @Inject
    lateinit var walletManager: WalletManager

    //New Line App Update
    private var appUpdateManager: AppUpdateManager? = null
    private val immediateAppUpdateRequestCode = 125

    companion object {
        const val SHORTCUT_LAUNCHER = "short_cut_launcher"

        var REQUEST_URI = "uri"
        const val reportIssueBChatID = BuildConfig.REPORT_ISSUE_ID
    }

    //Wallet
    private var streetMode: Long = 0
    private var uri: String? = null

    //Node Connection
    private val prefDaemonTestNet = "daemon_testnet"
    private val prefDaemonStageNet = "daemon_stagenet"
    private val prefDaemonMainNet = "daemon_mainnet"

    private var node: NodeInfo? = null
    private var onUriScannedListener: OnUriScannedListener? = null
    private var onUriWalletScannedListener: OnUriWalletScannedListener? = null
    private var barcodeData: BarcodeData? = null


    private val useSSL: Boolean = false
    private val isLightWallet:  Boolean = false

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var sharedPreferenceUtil: SharedPreferenceUtil
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfigUtil
    private var favouriteNodes: Set<NodeInfo> = setOf()
    val list: MutableList<TransactionInfo> = ArrayList()
    private var networkChangedReceiver: NetworkChangeReceiver? = null

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        // Set content view
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //-Wallet
        LegacyStorageHelper.migrateWallets(this)

        networkChangedReceiver = NetworkChangeReceiver(::networkChange)
        networkChangedReceiver!!.register(this)

        if(intent.getBooleanExtra(SHORTCUT_LAUNCHER,false)){
           //Shortcut launcher
            intent.removeExtra(SHORTCUT_LAUNCHER)
            val extras = Bundle()
            val address = intent.parcelable<Address>(ConversationFragmentV2.ADDRESS)

            extras.putParcelable(ConversationFragmentV2.ADDRESS, address)
            extras.putLong(ConversationFragmentV2.THREAD_ID, intent.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putBoolean(ConversationFragmentV2.SHORTCUT_LAUNCHER,true)

            //SetDataAndType
            val uri = intent.parcelable<Uri>(ConversationFragmentV2.URI)

            extras.putParcelable(ConversationFragmentV2.URI, uri)
            extras.putString(ConversationFragmentV2.TYPE,intent.getStringExtra(ConversationFragmentV2.TYPE))
            extras.putCharSequence(Intent.EXTRA_TEXT,intent.getCharSequenceExtra(Intent.EXTRA_TEXT))

            val oldFragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
            if (oldFragment != null) {
                supportFragmentManager.beginTransaction().remove(oldFragment).commit()
            }
            val homeFragment: Fragment = HomeFragment()
            homeFragment.arguments = extras
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.activity_home_frame_layout_container,homeFragment)
                .commit()
        }else {
            val oldFragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
            if (oldFragment != null) {
                supportFragmentManager.beginTransaction().remove(oldFragment).commit()
            }
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.activity_home_frame_layout_container, HomeFragment())
                .commit()
        }

        IP2Country.configureIfNeeded(this@HomeActivity)
        EventBus.getDefault().register(this@HomeActivity)

        //New Line App Update
        /*binding.airdropIcon.setAnimation(R.raw.airdrop_animation_top)
        binding.airdropIcon.setOnClickListener { callAirdropUrl() }*/
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        checkUpdate()
        viewModel.loadFavouritesWithNetwork()

        lifecycleScope.launch {
            viewModel.favouritesNodes.collectLatest { nodes ->
                nodes?.let {
                    favouriteNodes = it
                    if (favouriteNodes.isEmpty()) {
                        loadLegacyList()
                    }
                }
            }
        }

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

    private fun checkIsBnsHolder(){
        val isBnsHolder = TextSecurePreferences.getIsBNSHolder(this)
        val publicKey = TextSecurePreferences.getLocalNumber(this)
        if(!isBnsHolder.isNullOrEmpty() && !publicKey.isNullOrEmpty()){
            verifyBNS(isBnsHolder,publicKey,this) {
                if (!it) {
                    TextSecurePreferences.setIsBNSHolder(this, null)
                    MessagingModuleConfiguration.shared.storage.setIsBnsHolder(publicKey, false)
                    val currentFragment = getCurrentFragment()
                    if(currentFragment is HomeFragment) {
                        currentFragment.updateProfileButton()
                    }
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
            val currentFragment = getCurrentFragment()
            if(currentFragment is HomeFragment) {
                currentFragment.updateProfileButton()
            }
        }else {
            val currentFragment = getCurrentFragment()
            if(currentFragment is HomeFragment) {
                currentFragment.homeViewModel.tryUpdateChannel()
                currentFragment.updateAdapter()
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
                this.immediateAppUpdateRequestCode
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
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

        val fragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
        if(fragment is ConversationFragmentV2) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun callLifeCycleScope(
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
                val application = ApplicationContext.getInstance(this@HomeActivity)
                application.registerForFCMIfNeeded(false)
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
        if (extras != null) {
            newFragment.arguments = extras
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_home_frame_layout_container, newFragment)
            .addToBackStack(stackName)
            .commit()
    }

    private fun replaceFragmentWithTransition(newFragment: Fragment, stackName: String?, extras: Bundle?) {
        if (extras != null) {
            newFragment.arguments = extras
        }
        supportFragmentManager.beginTransaction()
                .add(R.id.activity_home_frame_layout_container, newFragment)
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
        profileButton.update(TextSecurePreferences.getProfileName(this))

        //New Line
        drawerProfileName.text = TextSecurePreferences.getProfileName(this)
        drawerProfileIcon.publicKey = publicKey
        drawerProfileIcon.displayName = TextSecurePreferences.getProfileName(this)
        drawerProfileIcon.recycle()
        drawerProfileIcon.update(TextSecurePreferences.getProfileName(this))
    }

    //Important
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action === MotionEvent.ACTION_DOWN) {
            val touch = PointF(event.x, event.y)
            when (val currentFragment: Fragment? = getCurrentFragment()) {
                is HomeFragment -> {
                    currentFragment.dispatchTouchEvent()
                }
                is ConversationFragmentV2 -> {
                    currentFragment.dispatchTouchEvent()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragment: Fragment? = getCurrentFragment()
        if (fragment is ConversationFragmentV2 || fragment is SendFragment || fragment is ReceiveFragment || fragment is ScannerFragment || fragment is WalletScannerFragment || fragment is WalletFragment) {
            if (!(fragment as OnBackPressedListener).onBackPressed()) {
                TextSecurePreferences.callFiatCurrencyApi(this,false)
                try {
                    if (fragment is ConversationFragmentV2) {
                        if (!fragment.transactionInProgress) {
                            super.onBackPressed()
                        }
                    } else {
                        super.onBackPressed()
                    }
                }catch(e : IllegalStateException){
                    replaceHomeFragment()
                }
            }
        }else if(fragment is HomeFragment){
            backToHome(fragment)
        }
    }

    private fun replaceHomeFragment(){
        val homeFragment: Fragment = HomeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.activity_home_frame_layout_container, homeFragment, HomeFragment::class.java.name).commit()
    }

    private fun backToHome(fragment: HomeFragment?) {
        when {
            !synced && TextSecurePreferences.isWalletActive(this) -> {
                val walletSyncDialog = ComposeDialogContainer(
                        dialogType = DialogType.WalletSyncing,
                        onConfirm = {
                            if (CheckOnline.isOnline(this)) {
                                onDisposeRequest()
                            }
                            setBarcodeData(null)
                            fragment!!.onBackPressed()
                            finish()
                        },
                        onCancel = {}
                )
                walletSyncDialog.show(this.supportFragmentManager, ComposeDialogContainer.TAG)
            }
            else -> {
                if (CheckOnline.isOnline(this)) {
                    onDisposeRequest()
                }
                setBarcodeData(null)
                fragment!!.onBackPressed()
                finish()
            }
        }
    }

    override fun handleSeedReminderViewContinueButtonTapped() {
        val intent = Intent(this, SeedActivity::class.java)
        show(intent)
    }

    private var setUpWalletPinActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun sendMessageToSupport() {
        val recipient = Recipient.from(this, Address.fromSerialized(reportIssueBChatID), false)
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS, recipient.address)
        val existingThread =
            DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        extras.putLong(ConversationFragmentV2.THREAD_ID, existingThread)
        extras.putParcelable(ConversationFragmentV2.URI, intent.data)
        extras.putString(ConversationFragmentV2.TYPE, intent.type)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this@HomeActivity)

        //Wallet
        Timber.d("onDestroy")
        dismissProgressDialog()
        //Important
        //unregisterDetachReceiver()
        //Ledger.disconnect()

        if(TextSecurePreferences.isWalletActive(this)) {
            if (CheckOnline.isOnline(this)) {
                if (mBoundService != null && getWallet() != null) {
                    saveWallet()
                }
            }
            stopWalletService()
        }
        networkChangedReceiver?.unregister(this)
        networkChangedReceiver = null
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

    override fun callConversationFragmentV2(address: Address, threadId: Long) {
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS,address)
        extras.putLong(ConversationFragmentV2.THREAD_ID,threadId)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    override fun playVoiceMessageAtIndexIfPossible(indexInAdapter: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.activity_home_frame_layout_container)
        if(fragment is ConversationFragmentV2) {
            fragment.playVoiceMessageAtIndexIfPossible(indexInAdapter)
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

    override fun showBottomSheetDialog(bottomSheetDialogFragment: BottomSheetDialogFragment, tag: String?) {
        bottomSheetDialogFragment.show(supportFragmentManager,tag)
    }

    override fun showBottomSheetDialogWithBundle(bottomSheetDialogFragment: BottomSheetDialogFragment, tag: String?, bundle: Bundle) {
        bottomSheetDialogFragment.arguments = bundle
        bottomSheetDialogFragment.show(supportFragmentManager,tag)
    }

    //Wallet

    private var mBoundService: WalletService? = null
    private var mIsBound = false

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = (service as WalletService.WalletServiceBinder).service
            mBoundService!!.setObserver(this@HomeActivity)
            updateProgress()
            Log.d("CONNECTED","")
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null
            Log.d("DISCONNECTED", "")
        }
    }

    private fun connectWalletService(walletName: String?, walletPassword: String?) {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Log.d("Beldex","value of walletName & walletPassword $walletName , $walletPassword")
        if (CheckOnline.isOnline(this)) {
            var intent: Intent? = null
            if(intent==null) {
                intent = Intent(applicationContext, WalletService::class.java)
                intent.putExtra(WalletService.REQUEST_WALLET, walletName)
                intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_LOAD)
                intent.putExtra(WalletService.REQUEST_CMD_LOAD_PW, walletPassword)
                startService(intent)
                bindService(intent, mConnection, BIND_AUTO_CREATE)
                mIsBound = true
                Timber.d("BOUND")
            }
        }
    }

    private fun updateProgress() {
        if (hasBoundService()) {
            Log.d("Beldex","mConnection called updateProgress()")
            onProgress(mBoundService!!.progressText)
            onProgress(mBoundService!!.progressValue)
        }
    }

//////////////////////////////////////////
// WalletFragment.Listener
//////////////////////////////////////////

    // refresh and return true if successful
    override fun hasBoundService(): Boolean {
        return mBoundService != null
    }

    override fun forceUpdate(requireActivity: Context) {
        val currentFragment = getCurrentFragment()
        try {
            if(getWallet()!=null) {
                when (currentFragment) {
                    is ConversationFragmentV2 -> {
                        onRefreshed(getWallet(), getWallet()!!.isSynchronized)
                    }
                    is WalletFragment -> {
                        onRefreshed(getWallet(), false)
                    }
                }

            }else{
                val currentFragment = getCurrentFragment()
                if (currentFragment is WalletFragment) {
                    currentFragment.updateNodeConnectingStatus()
                }
                if(!CheckOnline.isOnline(this)) {
                    Toast.makeText(
                        requireActivity,
                        getString(R.string.please_check_your_internet_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (ex: IllegalStateException) {
            Timber.e(ex.localizedMessage)
        }
    }

    override val connectionStatus: Wallet.ConnectionStatus?
        get() = mBoundService!!.connectionStatus
    override val daemonHeight: Long
        get() = mBoundService!!.daemonHeight

    override fun onSendRequest() {
        if(CheckOnline.isOnline(this)) {
            replaceFragment(SendFragment(),null, extras = null)
            //replaceFragmentWithTransition(SendFragment(),null,null)
            uri = null // only use uri once
        }else{
            Toast.makeText(this, getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTxDetailsRequest(view: View?, info: TransactionInfo?) {
        //Important
        /*val args = Bundle()
        args.putParcelable(TxFragment.ARG_INFO, info)
        replaceFragment(TxFragment(), null, args)*/
    }

    private var synced = false

    override val isSynced: Boolean
        get() = synced
    override val streetModeHeight: Long
        get() = streetMode

    override fun getTxKey(txId: String?): String? {
        return getWallet()!!.getTxKey(txId)
    }

    override fun onWalletReceive() {
        if(CheckOnline.isOnline(this)) {
            //replaceFragmentWithTransition(ReceiveFragment(), null, null)
            replaceFragment(ReceiveFragment(),null,null)
        } else {
            Toast.makeText(this, getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
        }
    }

    private var haveWallet = false

    override fun hasWallet(): Boolean {
        return haveWallet
    }

    override fun getWallet(): Wallet? {
        return if(mBoundService!=null) {
            checkNotNull(mBoundService) { "WalletService not bound." }
            mBoundService!!.wallet
        }else{
            null
        }
    }

    override fun getStorageRoot(): File {
        TODO("Not yet implemented")
    }

    override fun getFavouriteNodes(): MutableSet<NodeInfo> {
        return favouriteNodes.toHashSet()
    }

    override fun getOrPopulateFavourites(context: Context): MutableSet<NodeInfo> {
        return viewModel.getOrPopulateFavourites(context)
    }

    override fun getOrPopulateFavouritesRemoteNodeList(context: Context, storeNodes: Boolean): MutableSet<NodeInfo> {
        return viewModel.getOrPopulateFavouritesRemoteNodeList(context, storeNodes)
    }

    override fun setFavouriteNodes(nodes: MutableCollection<NodeInfo>?) {
        viewModel.setFavouriteNodes(nodes)
    }

    override fun getNode(): NodeInfo? {
        return if(TextSecurePreferences.getDaemon(this)){
            TextSecurePreferences.changeDaemon(this,false)
            val selectedNodeId = sharedPreferenceUtil.getSelectedNodeId()
            var nodeInfo = node
            val storedNodes=sharedPreferenceUtil.getStoredNodes().all
            for (nodeEntry in storedNodes.entries) {
                if (nodeEntry != null) { // just in case, ignore possible future errors
                    val nodeId=nodeEntry.value as String
                    if (nodeId == selectedNodeId) {
                        nodeInfo = NodeInfo.fromString(selectedNodeId)
                    }
                }
            }
            nodeInfo
        }else {
            node
        }
    }

    override fun setNode(node: NodeInfo?) {
        setNode(node, true)
    }

    private fun setNode(node: NodeInfo?, save: Boolean) {
        if (node !== this.node) {
            require(!(node != null && node.networkType !== walletManager.networkType)) { "network type does not match" }
            this.node = node
            for (nodeInfo in favouriteNodes) {
                nodeInfo.isSelected = nodeInfo === node
            }
            walletManager.setDaemon(node)
            if (save) sharedPreferenceUtil.saveSelectedNode(getNode())

            //SteveJosephh21
            startWalletService()
        }
    }

    private fun checkServiceRunning(): Boolean {
        return if (WalletService.Running) {
            Toast.makeText(this, getString(R.string.service_busy), Toast.LENGTH_SHORT).show()
            true
        } else {
            false
        }
    }

    override fun onNodePrefs() {
        if (checkServiceRunning()) return
    }


    override fun callFinishActivity() {
    }

///////////////////////////
// WalletService.Observer
///////////////////////////

    override fun onRefreshed(wallet: Wallet?, full: Boolean): Boolean {
        try {
            //WalletFragment Functionality --
            val currentFragment = getCurrentFragment()
            if (wallet != null) {
                if (wallet.isSynchronized) {
                    if(full) {
                        list.clear()
                        wallet.refreshHistory()
                        val streetHeight: Long = streetModeHeight
                        for (info in wallet.history.all) {
                            if ((info.isPending || info.blockheight >= streetHeight)
                                /*&& !dismissedTransactions.contains(info.hash)*/
                            ) list.add(info)
                        }
                    }
                    if (!synced) { // first sync
                        onProgress(3f)
                        saveWallet() // save on first sync
                        synced = true
                        //WalletFragment Functionality --
                        when (currentFragment) {
                            is WalletFragment -> {
                                runOnUiThread(currentFragment::onSynced)
                            }
                        }
                    }
                }
                runOnUiThread {
                    //WalletFragment Functionality --
                    when (currentFragment) {
                        is ConversationFragmentV2 -> {
                            currentFragment.onRefreshed(wallet, full)
                        }
                        is WalletFragment -> {
                            currentFragment.onRefreshed(wallet, full,list)
                        }
                    }
                }
            }
            return true
        } catch (ex: ClassCastException) {
            Timber.d(ex.localizedMessage)
        }
        return false
    }

    override fun onProgress(text: String?) {
        try {
            //WalletFragment Functionality --
            when (val currentFragment = getCurrentFragment()) {
                is ConversationFragmentV2 -> {
                    runOnUiThread { currentFragment.setProgress(text) }
                }
                is WalletFragment -> {
                    runOnUiThread { currentFragment.setProgress(text) }
                }
            }
        } catch (ex: ClassCastException) {
            Timber.d(ex.localizedMessage)
        }
    }

    override fun onProgress(n: Float) {
        runOnUiThread {
            try {
                //WalletFragment Functionality --
                when (val currentFragment = getCurrentFragment()) {
                    is ConversationFragmentV2 -> {
                        currentFragment.setProgress(n)
                    }
                    is WalletFragment -> {
                        currentFragment.setProgress(n)
                    }
                }
            } catch (ex: ClassCastException) {
                Timber.d(ex.localizedMessage)
            }
        }
    }

    override fun onWalletStored(success: Boolean) {
        runOnUiThread {
            if (success) {
                Toast.makeText(
                    this@HomeActivity,
                    getString(R.string.wallet_synced_text),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@HomeActivity,
                    getString(R.string.status_wallet_unload_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onTransactionCreated(tag: String?, pendingTransaction: PendingTransaction?) {
        try {
            //WalletFragment Functionality --
            val currentFragment = getCurrentFragment()
            runOnUiThread {
                val status = pendingTransaction!!.status
                if (status !== PendingTransaction.Status.Status_Ok) {
                    val errorText = pendingTransaction.errorString
                    getWallet()!!.disposePendingTransaction()
                    if(currentFragment is ConversationFragmentV2){
                        currentFragment.onCreateTransactionFailed(errorText)
                    }/*else if(currentFragment is SendFragment){
                        currentFragment.onCreateTransactionFailed(errorText)
                    }*/
                } else {
                     if(currentFragment is ConversationFragmentV2){
                        currentFragment.onTransactionCreated("txTag", pendingTransaction)
                    }/*else if(currentFragment is SendFragment){
                        currentFragment.onTransactionCreated("txTag", pendingTransaction)
                    }*/
                }
            }
        } catch (ex: ClassCastException) {
            // not in spend fragment
            Timber.d(ex.localizedMessage)
            // don't need the transaction any more
            if(getWallet()!=null) {
                getWallet()!!.disposePendingTransaction()
            }
        }
    }

    override fun onTransactionSent(txId: String?) {
        try {
            //WalletFragment Functionality --
            val currentFragment = getCurrentFragment()
            if(currentFragment is ConversationFragmentV2){
                runOnUiThread { currentFragment.onTransactionSent(txId) }
            }/*else if(currentFragment is SendFragment){
                runOnUiThread { currentFragment.onTransactionSent(txId) }
            }*/
        } catch (ex: ClassCastException) {
            // not in spend fragment
            Timber.d(ex.localizedMessage)
        }
    }

    override fun onSendTransactionFailed(error: String?) {
        try {
            val sendFragment = getCurrentFragment() as SendFragment?
            runOnUiThread { sendFragment!!.onSendTransactionFailed(error) }
        } catch (ex: ClassCastException) {
            // not in spend fragment
            Timber.d(ex.localizedMessage)
        }
    }

    override fun onWalletStarted(walletStatus: Wallet.Status?) {
        runOnUiThread {
            dismissProgressDialog()
            if (walletStatus == null) {
                // guess what went wrong
                Toast.makeText(
                    this@HomeActivity,
                    getString(R.string.status_wallet_connect_failed),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                if (Wallet.ConnectionStatus.ConnectionStatus_WrongVersion === walletStatus.connectionStatus) {
                    Toast.makeText(
                        this@HomeActivity,
                        getString(R.string.status_wallet_connect_wrong_version),
                        Toast.LENGTH_LONG
                    ).show() }else if (!walletStatus.isOk) {
                    if(walletStatus.errorString.isNotEmpty()) {
                        Toast.makeText(
                            this@HomeActivity,
                            walletStatus.errorString,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        if (walletStatus == null || Wallet.ConnectionStatus.ConnectionStatus_Connected !== walletStatus.connectionStatus) {
            Log.d("Beldex","WalletActivity finished called")
            /*finish()*/
        } else {
            haveWallet = true
            invalidateOptionsMenu()
            //WalletFragment Functionality --
            val currentFragment = getCurrentFragment()
            runOnUiThread {
                //WalletFragment Functionality --
                when (currentFragment) {
                    is WalletFragment -> {
                        currentFragment.onLoaded()
                    }
                }
            }
        }
    }

    override fun onWalletOpen(device: Wallet.Device?) {
        //Important
        /*if (device === Wallet.Device.Device_Ledger) {
            runOnUiThread { showLedgerProgressDialog(LedgerProgressDialog.TYPE_RESTORE) }
        }*/
    }

    override fun onWalletFinish() {
        finish()
    }

    override fun onScanned(qrCode: String?): Boolean {
        // #gurke
        val bcData = BarcodeData.fromString(qrCode)
        return if (bcData != null) {
            popFragmentStack(null)
            Timber.d("AAA")
            onUriScanned(bcData)
            true
        } else {
            false
        }
    }

    override fun setOnBarcodeScannedListener(onUriScannedListener: OnUriScannedListener?) {
        this.onUriScannedListener = onUriScannedListener
    }

    /// QR scanner callbacks
    override fun onScan() {
        if (Helper.getCameraPermission(this)) {
            replaceFragmentWithTransition(ScannerFragment(),null,null)
        } else {
            Timber.i("Waiting for permissions")
        }
    }

//////////////////////////////////////////
// SendFragment.Listener
//////////////////////////////////////////

    override val prefs: SharedPreferences?
        get() = getPreferences(MODE_PRIVATE)
    override val getUnLockedBalance: Long
        get() = if(getWallet()!=null){getWallet()!!.unlockedBalance}else{0}
    override val getFullBalance: Long
        get() = if(getWallet()!=null){getWallet()!!.balance}else{0}
    override val isStreetMode: Boolean
        get() = streetMode > 0

    override fun onPrepareSend(tag: String?, data: TxData?) {
        if (mIsBound) { // no point in talking to unbound service
            var intent: Intent? = null
            if(intent==null) {
                intent = Intent(applicationContext, WalletService::class.java)
                intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_TX)
                intent.putExtra(WalletService.REQUEST_CMD_TX_DATA, data)
                intent.putExtra(WalletService.REQUEST_CMD_TX_TAG, tag)
                startService(intent)
            }
        } else {
            Timber.e("Service not bound")
        }
    }

    override val walletName: String?
        get() = getWallet()!!.name

    override fun onSend(notes: UserNotes?) {
        if (mIsBound) { // no point in talking to unbound service
            var intent: Intent? = null
            if(intent==null) {
                intent = Intent(applicationContext, WalletService::class.java)
                intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_SEND)
                intent.putExtra(WalletService.REQUEST_CMD_SEND_NOTES, notes!!.txNotes)
                startService(intent)
                Timber.d("SEND TX request sent")
            }
        } else {
            Timber.e("Service not bound")
        }
    }

    override fun onDisposeRequest() {
        if(getWallet()!=null) {
            getWallet()!!.disposePendingTransaction()
        }
    }

    override fun onFragmentDone() {
        popFragmentStack(null)
    }

    private fun popFragmentStack(name: String?) {
        if (name == null) {
            supportFragmentManager.popBackStack()
        } else {
            supportFragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    override fun setOnUriScannedListener(onUriScannedListener: OnUriScannedListener?) {
        this.onUriScannedListener = onUriScannedListener
    }

    override fun setOnUriWalletScannedListener(onUriWalletScannedListener: OnUriWalletScannedListener?) {
        this.onUriWalletScannedListener = onUriWalletScannedListener
    }

    override fun setBarcodeData(data: BarcodeData?) {
        barcodeData = data
    }

    override fun getBarcodeData(): BarcodeData? {
        return barcodeData
    }

    override fun popBarcodeData(): BarcodeData {
        val data = barcodeData!!
        barcodeData = null
        return data
    }

    enum class Mode {
        BDX, BTC
    }

    override fun setMode(mode: Mode?) {
        TODO("Not yet implemented")
    }

    override fun getTxData(): TxData? {
        TODO("Not yet implemented")
    }


    private fun startWalletService() {
        val walletName = getWalletName(this)
        val walletPassword = getWalletPassword(this)
        if (walletName != null && walletPassword != null) {
            // acquireWakeLock()
            // we can set the streetmode height AFTER opening the wallet
            if (CheckOnline.isOnline(this)) {
                connectWalletService(walletName, walletPassword)
            }
        } else {
            val intent = Intent(this, WalletInfoActivity::class.java)
            push(intent)
        }
    }

    override fun onBackPressedFun() {
        if(CheckOnline.isOnline(this)) {
            onDisposeRequest()
        }
        setBarcodeData(null)
        onBackPressed()
    }

    override fun onWalletScan() {
        if(CheckOnline.isOnline(this)) {
            if (Helper.getCameraPermission(this)) {
                val extras = Bundle()
                replaceFragmentWithTransition(WalletScannerFragment(), null, extras)
            } else {
                Timber.i("Waiting for permissions")
            }
        } else {
            Toast.makeText(this, getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
        }

    }

    override fun onWalletScanned(qrCode: String?): Boolean {
        // #gurke
        val bcData = BarcodeData.fromString(qrCode)
        return if (bcData != null) {
            popFragmentStack(null)
            onUriWalletScanned(bcData)
            true
        } else {
            false
        }
    }

    //SecureActivity
    override fun onUriScanned(barcodeData: BarcodeData?) {
        var processed = false
        if (onUriScannedListener != null) {

            processed = onUriScannedListener!!.onUriScanned(barcodeData)
        }
        if (!processed || onUriScannedListener == null) {
            Toast.makeText(this, getString(R.string.nfc_tag_read_what), Toast.LENGTH_LONG).show()
        }
    }

    private fun onUriWalletScanned(barcodeData: BarcodeData?) {
        var processed = false
        if (onUriWalletScannedListener != null) {

            processed = onUriWalletScannedListener!!.onUriWalletScanned(barcodeData)
        }
        if (!processed || onUriWalletScannedListener == null) {
            Toast.makeText(this, getString(R.string.nfc_tag_read_what), Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume()-->")
        //Important
        //if (!Ledger.isConnected()) attachLedger()
        if(!CheckOnline.isOnline(this)){
            Toast.makeText(this,getString(R.string.please_check_your_internet_connection),Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopWalletService() {
        disconnectWalletService()
    }

    private fun disconnectWalletService() {
        if (mIsBound) {
            // Detach our existing connection.
            if(mBoundService != null){
                mBoundService!!.setObserver(null)
            }
            unbindService(mConnection)
            mIsBound = false
            Timber.d("UNBOUND")
        }
    }

     fun saveWallet() {
        if (mIsBound) { // no point in talking to unbound service
            var intent: Intent? = null
            if(intent==null) {
                intent = Intent(this, WalletService::class.java)
                intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_STORE)
                try {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                            Handler(Looper.getMainLooper()).post {
                                ContextCompat.startForegroundService(this, intent)
                            }
                        }
                        else -> {
                            this.startService(intent)
                        }
                    }
                }catch(ex: Exception){
                    Log.d("Exception ",ex.message.toString())
                }
                Timber.d("STORE request sent")
            }
        } else {
            Timber.e("Service not bound")
        }
    }

    private var startScanFragment = false

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (startScanFragment) {
            startScanFragment()
            startScanFragment = false
        }
    }

    private fun startScanFragment() {
        val extras = Bundle()
        replaceFragment(WalletScannerFragment(), null, extras)
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Helper.PERMISSIONS_REQUEST_CAMERA) { // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startScanFragment = true
            } else {
                val msg = getString(R.string.message_camera_not_permitted)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun onWalletRescan(restoreHeight: Long) {
        try {
            val currentWallet = getCurrentFragment()

            if(getWallet()!=null) {
                // The height entered by user
                getWallet()!!.restoreHeight = restoreHeight
                getWallet()!!.rescanBlockchainAsync()
            }
            synced = false
            if(currentWallet is WalletFragment){
                currentWallet.unsync()
                //invalidateOptionsMenu()
            }
        } catch (ex: java.lang.ClassCastException) {
            Timber.d(ex.localizedMessage)
        }
    }

    override fun setToolbarButton(type: Int) {
        /*binding.toolbar.setButton(type)*/
    }

    override fun setSubtitle(title: String?) {
        /* binding.toolbar.setSubtitle(subtitle)*/
    }

    override fun setTitle(titleId: Int) {

    }

    override fun callToolBarRescan(){
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this, R.style.BChatAlertDialog_Syncing_Option)
        val li = LayoutInflater.from(dialog.context)
        val promptsView = li.inflate(R.layout.alert_sync_options, null)

        dialog.setView(promptsView)
        val reConnect  = promptsView.findViewById<Button>(R.id.reConnectButton_Alert)
        val reScan = promptsView.findViewById<Button>(R.id.rescanButton_Alert)
        val alertDialog: AlertDialog = dialog.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        reConnect.setOnClickListener {
            if (CheckOnline.isOnline(this)) {
                onWalletReconnect(node, useSSL, isLightWallet)
                alertDialog.dismiss()
            } else {
                Toast.makeText(
                    this,
                    R.string.please_check_your_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
                alertDialog.dismiss()
            }
        }

        reScan.setOnClickListener {
            if (CheckOnline.isOnline(this)) {
                if (getWallet() != null) {
                    if (isSynced) {
                        if (getWallet()!!.daemonBlockChainHeight != null) {
                            RescanDialog(this, getWallet()!!.daemonBlockChainHeight).show(
                                supportFragmentManager,
                                ""
                            )
                        }
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.cannot_rescan_while_wallet_is_syncing),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }

                    //onWalletRescan()
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_check_your_internet_connection),
                    Toast.LENGTH_SHORT
                ).show()
            }
            alertDialog.dismiss()
        }
    }

    private fun onWalletReconnect(node: NodeInfo?, useSSL: Boolean, isLightWallet: Boolean) {
        val currentWallet = getCurrentFragment()
        if (CheckOnline.isOnline(this)) {
            if (getWallet() != null) {
                val isOnline =
                    getWallet()?.reConnectToDaemon(node, useSSL, isLightWallet) as Boolean
                if (isOnline) {
                    synced = false
                    setNode(node)
                    if(currentWallet is WalletFragment) {
                        currentWallet.setProgress(getString(R.string.reconnecting))
                        currentWallet.setProgress(2f)
                        invalidateOptionsMenu()
                    }
                } else {
                    if(currentWallet is WalletFragment) {
                        currentWallet.setProgress(getString(R.string.failed_connected_to_the_node))
                    }
                }
            } else {
                Toast.makeText(this, "Wait for connection..", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, R.string.please_check_your_internet_connection, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun callToolBarSettings() {
        openWalletSettings()
    }

    private fun openWalletSettings() {
        Intent(this, WalletSettingComposeActivity::class.java).also {
            it.putExtra(WalletSettingComposeActivity.extraStartDestination, WalletSettingScreens.MyWalletSettingsScreen.route)
            walletSettingsResultLauncher.launch(it)
        }

    }

    private var walletSettingsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pingSelectedNode()
        }
    }

    private fun pingSelectedNode() {
        val PING_SELECTED = 0
        val FIND_BEST = 1
        AsyncFindBestNode(PING_SELECTED, FIND_BEST).execute<Int>(PING_SELECTED)
    }

    inner class AsyncFindBestNode(val PING_SELECTED: Int, val FIND_BEST: Int) :
        AsyncTaskCoroutine<Int?, NodeInfo?>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: Int?): NodeInfo? {
            val favourites: Set<NodeInfo?> = getOrPopulateFavourites(this@HomeActivity)
            var selectedNode: NodeInfo?
            if (params[0] == FIND_BEST) {
                selectedNode = autoselect(favourites)
            } else if (params[0] == PING_SELECTED) {
                selectedNode = getNode()
                if (selectedNode == null) {
                    Log.d("Beldex", "selected node null")
                    for (node in favourites) {
                        if (node!!.isSelected) {
                            selectedNode = node
                            break
                        }
                    }
                }
                if (selectedNode == null) { // autoselect
                    selectedNode = autoselect(favourites)
                } else {
                    //Steve Josephh21
                    if(selectedNode!=null) {
                        selectedNode!!.testRpcService()
                    }
                }
            } else throw java.lang.IllegalStateException()
            return if (selectedNode != null && selectedNode.isValid) {
                setNode(selectedNode)
                selectedNode
            } else {
                setNode(null)
                null
            }
        }

        override fun onPostExecute(result: NodeInfo?) {
            Log.d("Beldex", "daemon connected to  ${result?.host}")
        }
    }

    fun autoselect(nodes: Set<NodeInfo?>): NodeInfo? {
        if (nodes.isEmpty()) return null
        NodePinger.execute(nodes, null)
        val nodeList: ArrayList<NodeInfo?> = ArrayList<NodeInfo?>(nodes)
        Collections.sort(nodeList, NodeInfo.BestNodeComparator)
        val rnd = Random().nextInt(nodeList.size)
        return nodeList[rnd]
    }

    override fun walletOnBackPressed(){
        val fragment: Fragment = getCurrentFragment()!!
        if (fragment is ConversationFragmentV2 || fragment is SendFragment || fragment is ReceiveFragment || fragment is ScannerFragment || fragment is WalletScannerFragment || fragment is WalletFragment) {
            if (!(fragment as OnBackPressedListener).onBackPressed()) {
                TextSecurePreferences.callFiatCurrencyApi(this,false)
                try {
                    super.onBackPressed()
                }catch(e : IllegalStateException){
                    replaceHomeFragment()
                }
            }
        }
    }

    //SetDataAndType
    override fun passSharedMessageToConversationScreen(thread:Recipient) {
        val intent = Intent(this, MediaOverviewActivity::class.java)
        intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, thread.address)
        passSharedMessageToConversationScreen.launch(intent)
    }

    private val passSharedMessageToConversationScreen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if(result.data!=null){
                val extras = Bundle()
                val address = intent.parcelable<Address>(ConversationFragmentV2.ADDRESS)
                extras.putParcelable(ConversationFragmentV2.ADDRESS, address)
                extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
                val uri = intent.parcelable<Uri>(ConversationFragmentV2.URI)
                extras.putParcelable(ConversationFragmentV2.URI, uri)
                extras.putString(ConversationFragmentV2.TYPE,result.data!!.getStringExtra(ConversationFragmentV2.TYPE))
                extras.putCharSequence(Intent.EXTRA_TEXT,result.data!!.getCharSequenceExtra(Intent.EXTRA_TEXT))
                //Shortcut launcher
                extras.putBoolean(ConversationFragmentV2.SHORTCUT_LAUNCHER,true)
                replaceFragment(HomeFragment(),null,extras)
            }
        }
    }

    private fun loadLegacyList() {
        val sharedPref = getPreferences(MODE_PRIVATE)
        when (walletManager.networkType) {
            NetworkType.NetworkType_Mainnet -> {
                viewModel.loadLegacyList(sharedPref.getString(prefDaemonMainNet, null))
                sharedPref.edit().remove(prefDaemonMainNet).apply()
            }
            NetworkType.NetworkType_Stagenet -> {
                viewModel.loadLegacyList(sharedPref.getString(prefDaemonStageNet, null))
                sharedPref.edit().remove(prefDaemonStageNet).apply()
            }
            NetworkType.NetworkType_Testnet -> {
                viewModel.loadLegacyList(sharedPref.getString(prefDaemonTestNet, null))
                sharedPref.edit().remove(prefDaemonTestNet).apply()
            }
            else -> throw java.lang.IllegalStateException("unsupported net " + walletManager.networkType)
        }
    }
}
//endregion

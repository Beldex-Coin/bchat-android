package com.thoughtcrimes.securesms.home

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
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
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfilePictureModifiedEvent
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getWalletName
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getWalletPassword
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.MediaOverviewActivity
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.components.ProfilePictureView
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.conversation.v2.ConversationViewModel
import com.thoughtcrimes.securesms.conversation.v2.messages.VoiceMessageViewDelegate
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.data.TxData
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.database.MmsSmsDatabase
import com.thoughtcrimes.securesms.delegates.NodeDelegates
import com.thoughtcrimes.securesms.delegates.NodeDelegatesImpl
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import com.thoughtcrimes.securesms.home.search.GlobalSearchAdapter
import com.thoughtcrimes.securesms.home.search.GlobalSearchInputLayout
import com.thoughtcrimes.securesms.home.search.GlobalSearchViewModel
import com.thoughtcrimes.securesms.model.NetworkType
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.model.TransactionInfo
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.onboarding.SeedActivity
import com.thoughtcrimes.securesms.onboarding.SeedReminderViewDelegate
import com.thoughtcrimes.securesms.util.ActivityDispatcher
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.IP2Country
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import com.thoughtcrimes.securesms.util.parcelable
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.show
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.OnUriWalletScannedListener
import com.thoughtcrimes.securesms.wallet.WalletFragment
import com.thoughtcrimes.securesms.wallet.info.WalletInfoActivity
import com.thoughtcrimes.securesms.wallet.node.NodeFragment
import com.thoughtcrimes.securesms.wallet.receive.ReceiveFragment
import com.thoughtcrimes.securesms.wallet.rescan.RescanDialog
import com.thoughtcrimes.securesms.wallet.scanner.ScannerFragment
import com.thoughtcrimes.securesms.wallet.scanner.WalletScannerFragment
import com.thoughtcrimes.securesms.wallet.send.SendFragment
import com.thoughtcrimes.securesms.wallet.service.WalletService
import com.thoughtcrimes.securesms.wallet.settings.WalletSettings
import com.thoughtcrimes.securesms.wallet.utils.LegacyStorageHelper
import com.thoughtcrimes.securesms.wallet.utils.common.LoadingActivity
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : PassphraseRequiredActionBarActivity(),SeedReminderViewDelegate,HomeFragment.HomeFragmentListener,ConversationFragmentV2.Listener,UserDetailsBottomSheet.UserDetailsBottomSheetListener,VoiceMessageViewDelegate, ActivityDispatcher,
    WalletFragment.Listener, WalletService.Observer, WalletScannerFragment.OnScannedListener,SendFragment.OnScanListener,SendFragment.Listener,ReceiveFragment.Listener,WalletFragment.OnScanListener,
    ScannerFragment.OnWalletScannedListener,WalletScannerFragment.Listener,NodeFragment.Listener {

    private lateinit var binding: ActivityHomeBinding

    private val globalSearchViewModel by viewModels<GlobalSearchViewModel>()

    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences
//    @Inject
//    lateinit var viewModelFactory: ConversationViewModel.AssistedFactory

    @Inject
    lateinit var walletManager: WalletManager

    //New Line App Update
    private var appUpdateManager: AppUpdateManager? = null
    private val immediateAppUpdateRequestCode = 124

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
    private var favouriteNodes: Set<NodeInfo> = setOf()

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        // Set content view
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //-Wallet
        LegacyStorageHelper.migrateWallets(this)


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

            val homeFragment: Fragment = HomeFragment()
            homeFragment.arguments = extras
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.activity_home_frame_layout_container,
                    homeFragment,
                    HomeFragment::class.java.name
                )
                .commit()
        }else {
            val homeFragment: Fragment = HomeFragment()
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.activity_home_frame_layout_container,
                    homeFragment,
                    HomeFragment::class.java.name
                )
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

        /*if(TextSecurePreferences.getAirdropAnimationStatus(this)) {
            //New Line AirDrop
            TextSecurePreferences.setAirdropAnimationStatus(this,false)
            launchSuccessLottieDialog()
        }*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateProfileEvent(event: ProfilePictureModifiedEvent) {
        if (event.recipient.isLocalNumber) {
            val currentFragment = getCurrentFragment()
            if(currentFragment is HomeFragment) {
                currentFragment.updateProfileButton()
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
                        this@HomeActivity
                    ) { threadIDs ->
                        val adapter = recyclerView.adapter as HomeAdapter
                        adapter.typingThreadIDs = threadIDs ?: setOf()
                    }
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
            !synced -> {
                val dialog: AlertDialog.Builder =
                    AlertDialog.Builder(this, R.style.BChatAlertDialog_Wallet_Syncing_Exit_Alert)
                dialog.setTitle(getString(R.string.wallet_syncing_alert_title))
                dialog.setMessage(getString(R.string.wallet_syncing_alert_message))

                dialog.setPositiveButton(R.string.exit) { _, _ ->
                    if (CheckOnline.isOnline(this)) {
                        onDisposeRequest()
                    }
                    setBarcodeData(null)
                    fragment!!.onBackPressed()
                    finish()
                }
                dialog.setNegativeButton(R.string.cancel) { _, _ ->
                    // Do nothing
                }
                val alert: AlertDialog = dialog.create()
                alert.show()
                alert.getButton(DialogInterface.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.text))
                alert.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.alert_ok))
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

        if(CheckOnline.isOnline(this)) {
            if (mBoundService != null && getWallet() != null) {
                saveWallet()
            }
        }
        stopWalletService()
        super.onDestroy()
    }

//    override fun getConversationViewModel(): ConversationViewModel.AssistedFactory {
//        return viewModelFactory
//    }

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
        try {
            if(getWallet()!=null) {
                onRefreshed(getWallet(), true)
            }else{
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

    override fun onSendRequest(view: View?) {
        if(CheckOnline.isOnline(this)) {
            replaceFragment(SendFragment.newInstance(uri), null, null)
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
    override val isWatchOnly: Boolean
        get() = if(getWallet()!=null){getWallet()!!.isWatchOnly}else{false}

    override fun getTxKey(txId: String?): String? {
        return getWallet()!!.getTxKey(txId)
    }

    override fun onWalletReceive(view: View?) {
        replaceFragment(ReceiveFragment(), null, null)
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

    override fun getOrPopulateFavourites(): MutableSet<NodeInfo> {
        return viewModel.getOrPopulateFavourites()
    }

    override fun setFavouriteNodes(nodes: MutableCollection<NodeInfo>?) {
        viewModel.setFavouriteNodes(nodes)
    }

    override fun getNode(): NodeInfo? {
        return if(TextSecurePreferences.getDaemon(this)){
            TextSecurePreferences.changeDaemon(this,false)
            val selectedNodeId = sharedPreferenceUtil.getSelectedNodeId()
            val nodeInfo = NodeInfo.fromString(selectedNodeId)
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

    private var numAccounts = -1

    override fun onRefreshed(wallet: Wallet?, full: Boolean): Boolean {
        if (numAccounts != wallet!!.numAccounts) {
            numAccounts = wallet.numAccounts
        }
        try {
            //WalletFragment Functionality --
            val currentFragment = getCurrentFragment()
            if (wallet.isSynchronized) {
                if (!synced) { // first sync
                    onProgress(-2)//onProgress(-1)
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
                        currentFragment.onRefreshed(wallet,full)
                    }
                    is WalletFragment -> {
                        currentFragment.onRefreshed(wallet,full)
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

    override fun onProgress(n: Int) {
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
                    getString(R.string.status_wallet_unloaded),
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

    override fun onTransactionCreated(tag: String, pendingTransaction: PendingTransaction) {
        try {
            //WalletFragment Functionality --
            val currentFragment = getCurrentFragment()
            runOnUiThread {
                val status = pendingTransaction.status
                if (status !== PendingTransaction.Status.Status_Ok) {
                    val errorText = pendingTransaction.errorString
                    getWallet()!!.disposePendingTransaction()
                    if(currentFragment is ConversationFragmentV2){
                        currentFragment.onCreateTransactionFailed(errorText)
                    }else if(currentFragment is SendFragment){
                        currentFragment.onCreateTransactionFailed(errorText)
                    }
                } else {
                     if(currentFragment is ConversationFragmentV2){
                        currentFragment.onTransactionCreated("txTag", pendingTransaction)
                    }else if(currentFragment is SendFragment){
                        currentFragment.onTransactionCreated("txTag", pendingTransaction)
                    }
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
            }else if(currentFragment is SendFragment){
                runOnUiThread { currentFragment.onTransactionSent(txId) }
            }
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
            startWalletScanFragment()
        } else {
            Timber.i("Waiting for permissions")
        }
    }

//////////////////////////////////////////
// SendFragment.Listener
//////////////////////////////////////////

    override val prefs: SharedPreferences?
        get() = getPreferences(MODE_PRIVATE)
    override val totalFunds: Long
        get() = if(getWallet()!=null){getWallet()!!.unlockedBalance}else{0}
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

    override fun onWalletScan(view: View?) {
        if (Helper.getCameraPermission(this)) {
            val extras = Bundle()
            replaceFragment(WalletScannerFragment(), null, extras)
        } else {
            Timber.i("Waiting for permissions")
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

    private fun saveWallet() {
        if (mIsBound) { // no point in talking to unbound service
            var intent: Intent? = null
            if(intent==null) {
                intent = Intent(applicationContext, WalletService::class.java)
                intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_STORE)
                startService(intent)
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

    private fun startWalletScanFragment() {
        val extras = Bundle()
        replaceFragment(ScannerFragment(), null, extras)
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
            invalidateOptionsMenu()
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
                        currentWallet.setProgress(101)
                        invalidateOptionsMenu()
                    }
                } else {
                    if(currentWallet is WalletFragment) {
                        currentWallet.setProgress(R.string.failed_connected_to_the_node)
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
        val intent = Intent(this, WalletSettings::class.java)
        walletSettingsResultLauncher.launch(intent)
    }

    private var walletSettingsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, LoadingActivity::class.java)
            push(intent)
            finish()
        }
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

package io.beldex.bchat.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfilePictureModifiedEvent
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
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
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.ConversationViewModel
import io.beldex.bchat.conversation.v2.messages.VoiceMessageViewDelegate
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.home.search.GlobalSearchAdapter
import io.beldex.bchat.home.search.GlobalSearchInputLayout
import io.beldex.bchat.home.search.GlobalSearchViewModel
import io.beldex.bchat.onboarding.SeedActivity
import io.beldex.bchat.onboarding.SeedReminderViewDelegate
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.FirebaseRemoteConfigUtil
import io.beldex.bchat.util.IP2Country
import io.beldex.bchat.util.SharedPreferenceUtil
import io.beldex.bchat.util.parcelable
import io.beldex.bchat.util.push
import io.beldex.bchat.util.show
import io.beldex.bchat.model.CheckOnline
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : PassphraseRequiredActionBarActivity(),SeedReminderViewDelegate,HomeFragment.HomeFragmentListener,ConversationFragmentV2.Listener,UserDetailsBottomSheet.UserDetailsBottomSheetListener,VoiceMessageViewDelegate, ActivityDispatcher {

    private lateinit var binding: ActivityHomeBinding

    private val globalSearchViewModel by viewModels<GlobalSearchViewModel>()

    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences
    @Inject
    lateinit var viewModelFactory: ConversationViewModel.AssistedFactory

    //New Line App Update
    private var appUpdateManager: AppUpdateManager? = null
    private val immediateAppUpdateRequestCode = 125

    companion object {
        const val SHORTCUT_LAUNCHER = "short_cut_launcher"

        var REQUEST_URI = "uri"
        const val reportIssueBChatID = BuildConfig.REPORT_ISSUE_ID
    }

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var sharedPreferenceUtil: SharedPreferenceUtil
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfigUtil

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        // Set content view
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


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

    private fun replaceFragmentWithTransition(view: View?, newFragment: Fragment, stackName: String?, extras: Bundle?) {
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
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onBackPressed() {
        val fragment: Fragment? = getCurrentFragment()
        if (fragment is ConversationFragmentV2) {
            TextSecurePreferences.callFiatCurrencyApi(this,false)
            try {
                super.onBackPressed()

            }catch(e : IllegalStateException){
                replaceHomeFragment()
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
                fragment!!.onBackPressed()
                finish()
    }

    override fun handleSeedReminderViewContinueButtonTapped() {
        val intent = Intent(this, SeedActivity::class.java)
        show(intent)
    }

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
        Timber.d("onDestroy")
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

    override fun onResume() {
        super.onResume()
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
    }



    //SetDataAndType
    override fun passSharedMessageToConversationScreen(thread:Recipient) {
        val intent = Intent(this, MediaOverviewActivity::class.java)
        intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, thread.address)
        passSharedMessageToConversationScreen.launch(intent)
    }
    override fun onScreenBackPressed() {
       super.onBackPressed()
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
}
//endregion

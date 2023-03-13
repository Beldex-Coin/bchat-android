package com.thoughtcrimes.securesms.home

import android.app.AlertDialog
import android.content.*
import android.database.Cursor
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.*
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils
import com.beldex.libsignal.utilities.toHexString
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.MuteDialog
import com.thoughtcrimes.securesms.components.ProfilePictureView
import com.thoughtcrimes.securesms.conversation.v2.utilities.NotificationUtils
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.database.MmsSmsDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.drawer.ClickListener
import com.thoughtcrimes.securesms.drawer.NavigationItemModel
import com.thoughtcrimes.securesms.drawer.NavigationRVAdapter
import com.thoughtcrimes.securesms.drawer.RecyclerTouchListener
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import com.thoughtcrimes.securesms.home.search.GlobalSearchAdapter
import com.thoughtcrimes.securesms.home.search.GlobalSearchInputLayout
import com.thoughtcrimes.securesms.mms.GlideApp
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.util.*
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.webrtc.CallViewModel
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentHomeBinding
import io.beldex.bchat.databinding.ViewMessageRequestBannerBinding
import kotlinx.coroutines.*
import org.apache.commons.lang3.time.DurationFormatUtils
import java.io.IOException
import java.util.*

class HomeFragment : Fragment(),ConversationClickListener,
    NewConversationButtonSetViewDelegate,
    LoaderManager.LoaderCallbacks<Cursor>,
    GlobalSearchInputLayout.GlobalSearchInputLayoutListener{

    private lateinit var binding: FragmentHomeBinding
    private lateinit var glide: GlideRequests
    private var broadcastReceiver: BroadcastReceiver? = null
    private var uiJob: Job? = null
    private var viewModel : CallViewModel? =null // by viewModels<CallViewModel>()
    private val CALLDURATIONFORMAT = "HH:mm:ss"

    private val publicKey: String
        get() = TextSecurePreferences.getLocalNumber(requireActivity().applicationContext)!!

    /*Hales63*/
    private val homeAdapter: HomeAdapter by lazy {
        HomeAdapter(context = requireActivity(), cursor = (activity as HomeActivity).threadDb.approvedConversationList, listener = this)
    }

    private val globalSearchAdapter = GlobalSearchAdapter { model ->
        when (model) {
            is GlobalSearchAdapter.Model.Message -> {
                val threadId = model.messageResult.threadId
                val timestamp = model.messageResult.receivedTimestampMs
                val author = model.messageResult.messageRecipient.address
                if (binding.globalSearchRecycler.isVisible) {
                    binding.globalSearchInputLayout.clearSearch(true)
                }
                activityCallback?.passGlobalSearchAdapterModelMessageValue(threadId,timestamp,author)
            }
            is GlobalSearchAdapter.Model.SavedMessages -> {
                if (binding.globalSearchRecycler.isVisible) {
                    binding.globalSearchInputLayout.clearSearch(true)
                }
                activityCallback?.passGlobalSearchAdapterModelSavedMessagesValue(Address.fromSerialized(model.currentUserPublicKey))
            }
            is GlobalSearchAdapter.Model.Contact -> {
                val address = model.contact.bchatID
                if (binding.globalSearchRecycler.isVisible) {
                    binding.globalSearchInputLayout.clearSearch(true)
                }
                activityCallback?.passGlobalSearchAdapterModelContactValue(Address.fromSerialized(address))
            }
            is GlobalSearchAdapter.Model.GroupConversation -> {
                val groupAddress = Address.fromSerialized(model.groupRecord.encodedId)
                val threadId =
                    (activity as HomeActivity).threadDb.getThreadIdIfExistsFor(Recipient.from(requireActivity().applicationContext, groupAddress, false))
                if (threadId >= 0) {
                    if (binding.globalSearchRecycler.isVisible) {
                        binding.globalSearchInputLayout.clearSearch(true)
                    }
                    activityCallback?.passGlobalSearchAdapterModelGroupConversationValue(threadId)
                }
            }
            else -> {
                Log.d("Beldex", "callback with model: $model")
            }
        }
    }

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
            return TextSecurePreferences.getLocalNumber(requireActivity().applicationContext)!!
        }

    private var mContext : Context? = null
    var activityCallback: HomeFragmentListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mContext = context
        if (context is HomeFragmentListener) {
            activityCallback = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater,container,false)
        // Set custom toolbar
        (activity as HomeActivity).setSupportActionBar(binding.toolbar)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //New Line
        val notification = TextSecurePreferences.isNotificationsEnabled(requireActivity().applicationContext)
        Log.d("NotificationLog", notification.toString())
        viewModel = ViewModelProvider(requireActivity()).get(CallViewModel::class.java)

        // Set up Glide
        glide = GlideApp.with(this)
        // Set up toolbar buttons
        binding.profileButton.glide = glide

        //New Line
        // Setup Recyclerview's Layout
        binding.navigationRv.layoutManager = LinearLayoutManager(requireActivity().applicationContext)
        binding.navigationRv.setHasFixedSize(true)
        updateAdapter(0)
        // Add Item Touch Listener
        binding.navigationRv.addOnItemTouchListener(RecyclerTouchListener(requireActivity().applicationContext, object :
            ClickListener {
            override fun onClick(view: View, position: Int) {
                when (position) {
                    0 -> {
                        // # Account Activity
                        activityCallback?.openSettings()
                    }
                    1 -> {
                        // # My Wallet Activity
                        if(CheckOnline.isOnline(requireActivity().applicationContext)) {
                            Log.d("Beldex","isOnline value ${CheckOnline.isOnline(requireActivity().applicationContext)}")
                            activityCallback?.openMyWallet()
                        }
                        else {
                            Log.d("Beldex","isOnline value ${CheckOnline.isOnline(requireActivity().applicationContext)}")
                            Toast.makeText(requireActivity().applicationContext,getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        //New Line
                        val notification =
                            TextSecurePreferences.isNotificationsEnabled(requireActivity().applicationContext)
                        Log.d("NotificationLog1", notification.toString())
                        // # Notification Activity
                        activityCallback?.showNotificationSettings()
                    }
                    3 -> {
                        // # Message Requests Activity
                        activityCallback?.showMessageRequests()
                    }
                    4 -> {
                        // # Privacy Activity
                        activityCallback?.showPrivacySettings()
                    }
                    5 -> {
                        // # App Permissions Activity
                        activityCallback?.callAppPermission()
                    }
                    6 -> {
                        // # Recovery Seed Activity
                        activityCallback?.showSeed()
                    }
                    /* 6 -> {
                         // # Recovery Key Activity
                         showKeys()
                     }*/
                    7 -> {
                        // # Support
                        activityCallback?.sendMessageToSupport()
                    }
                    8 -> {
                        // # Help Activity
                        activityCallback?.help()
                    }
                    9 -> {
                        // # Invite Activity
                        activityCallback?.sendInvitation(hexEncodedPublicKey)
                    }
                    10 -> {
                        // # About Activity
                        activityCallback?.showAbout()
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
        }
        binding.drawerCloseIcon.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.END) }
        val activeUiMode = UiModeUtilities.getUserSelectedUiMode(requireActivity().applicationContext)
        Log.d("beldex", "activeUiMode $activeUiMode")
        binding.drawerAppearanceToggleButton.isChecked = activeUiMode == UiMode.NIGHT

        binding.drawerAppearanceToggleButton.setOnClickListener{
            if(binding.drawerAppearanceToggleButton.isChecked){
                val uiMode = UiMode.values()[1]
                UiModeUtilities.setUserSelectedUiMode(requireActivity().applicationContext, uiMode)
            }
            else{
                val uiMode = UiMode.values()[0]
                UiModeUtilities.setUserSelectedUiMode(requireActivity().applicationContext, uiMode)
            }
        }
        binding.drawerQrcodeImg.setOnClickListener {
            activityCallback?.showQRCode()
            Handler().postDelayed({
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }, 200)
        }
        binding.drawerProfileIcon.glide = glide
        binding.drawerProfileId.text = "ID: $hexEncodedPublicKey"

        binding.searchViewContainer.setOnClickListener {
            binding.globalSearchInputLayout.requestFocus()
        }
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
        setupMessageRequestsBanner()
        setupHeaderImage()
        // Set up recycler view
        binding.globalSearchInputLayout.listener = this
        homeAdapter.setHasStableIds(true)
        homeAdapter.glide = glide
        binding.recyclerView.adapter = homeAdapter
        binding.globalSearchRecycler.adapter = globalSearchAdapter
        // Set up empty state view
        binding.createNewPrivateChatButton.setOnClickListener { createNewPrivateChat() }
        // This is a workaround for the fact that CursorRecyclerViewAdapter doesn't actually auto-update (even though it says it will)
        LoaderManager.getInstance(this).restartLoader(0, null, this)
        // Set up new conversation button set
        binding.newConversationButtonSet.delegate = this
        // Observe blocked contacts changed events
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.recyclerView.adapter!!.notifyDataSetChanged()
            }
        }
        this.broadcastReceiver = broadcastReceiver
        LocalBroadcastManager.getInstance(requireActivity().applicationContext)
            .registerReceiver(broadcastReceiver, IntentFilter("blockedContactsChanged"))
        activityCallback?.callLifeCycleScope(binding.recyclerView,binding.globalSearchInputLayout,(activity as HomeActivity).mmsSmsDatabase,globalSearchAdapter,publicKey,binding.profileButton,binding.drawerProfileName,binding.drawerProfileIcon)

    }


    /*Hales63*/
    private fun setupMessageRequestsBanner() {
            val messageRequestCount = (activity as HomeActivity).threadDb.unapprovedConversationCount
            // Set up message requests
            if (messageRequestCount > 0 && !(activity as HomeActivity).textSecurePreferences.hasHiddenMessageRequests()) {
                with(ViewMessageRequestBannerBinding.inflate(layoutInflater)) {
                    unreadCountTextView.text = messageRequestCount.toString()
                    timestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(
                        requireActivity().applicationContext,
                        Locale.getDefault(),
                        (activity as HomeActivity).threadDb.latestUnapprovedConversationTimestamp
                    )
                    root.setOnClickListener { activityCallback?.showMessageRequests() }
                    expandMessageRequest.setOnClickListener { activityCallback?.showMessageRequests() }
                    root.setOnLongClickListener { hideMessageRequests(); true }
                    root.layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    )
                    homeAdapter.headerView = root
                    homeAdapter.notifyItemChanged(0)
                }
            } else {
                homeAdapter.headerView = null
            }
    }

    private fun hideMessageRequests() {
        val dialog = AlertDialog.Builder(requireActivity().applicationContext, R.style.BChatAlertDialog_New)
            .setTitle("Hide message requests?")
            .setMessage("Once they are hidden, you can access them from Settings > Message Requests")
            .setPositiveButton(R.string.yes) { _, _ ->
                (activity as HomeActivity).textSecurePreferences.setHasHiddenMessageRequests()
                /*//New Line
                textSecurePreferences.setHasShowMessageRequests(false)*/

                setupMessageRequestsBanner()
                LoaderManager.getInstance(this).restartLoader(0, null, this)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                // Do nothing
            }.show()

        //SteveJosephh21

        val message: TextView = dialog.findViewById(android.R.id.message)
        val messageFace: Typeface = Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        message.typeface = messageFace
    }

    private fun setupCallActionBar() {

        val startTimeNew = viewModel!!.callStartTime
        if (startTimeNew == -1L) {
            binding.toolbarCall.isVisible = false
        } else {
            binding.toolbarCall.isVisible = true
            uiJob = lifecycleScope.launch {
                launch {
                    while (isActive) {
                        val startTime = viewModel!!.callStartTime
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
            requireActivity().applicationContext.startService(WebRtcCallService.hangupIntent(requireActivity().applicationContext))
            binding.toolbarCall.isVisible = false
            Toast.makeText(requireActivity().applicationContext, "Call ended", Toast.LENGTH_SHORT).show()
        }
        binding.toolbarCall.setOnClickListener {
            activityCallback?.toolBarCall()
        }
    }

    //New Line
    private fun updateAdapter(highlightItemPos: Int) {
        adapter = NavigationRVAdapter(items, highlightItemPos)
        binding.navigationRv.adapter = adapter
        adapter.notifyDataSetChanged()
    }


    private fun setupHeaderImage() {
        val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity())
        val headerTint = if (isDayUiMode) R.color.black else R.color.white
        binding.bchatHeaderImage.setTextColor(getColor(requireActivity().applicationContext,headerTint))
        //binding.bchatHeaderImage.setColorFilter(getColor(headerTint))
    }

    override fun onInputFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            setSearchShown(true)
        } else {
            setSearchShown(!binding.globalSearchInputLayout.query.value.isNullOrEmpty())
        }
    }

    private fun setSearchShown(isShown: Boolean) {
        //New Line
        binding.searchBarLayout.isVisible = isShown
        binding.searchBarLayout.setOnClickListener {
            onBackPressed()
        }

        binding.searchToolbar.isVisible = isShown
        binding.searchViewCard.isVisible = !isShown
        binding.bchatToolbar.isVisible = !isShown
        binding.recyclerView.isVisible = !isShown
        binding.emptyStateContainer.isVisible =
            (binding.recyclerView.adapter as HomeAdapter).itemCount == 0 && binding.recyclerView.isVisible
        binding.emptyStateContainerText.isVisible =
            (binding.recyclerView.adapter as HomeAdapter).itemCount == 0 && binding.recyclerView.isVisible
        val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity())
        (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
            binding.emptyStateImageView.setImageResource(
                it
            )
        }
        //Comment-->
        /*binding.seedReminderView.isVisible =
            !TextSecurePreferences.getHasViewedSeed(this) && !isShown*/

        binding.gradientView.isVisible = !isShown
        binding.globalSearchRecycler.isVisible = isShown
        binding.newConversationButtonSet.isVisible = !isShown
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


    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor> {
        return HomeLoader(requireActivity().applicationContext)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        homeAdapter.changeCursor(cursor)
        setupMessageRequestsBanner()
        updateEmptyState()
    }

    override fun onLoaderReset(cursor: Loader<Cursor>) {
        homeAdapter.changeCursor(null)
    }

    private fun checkInternetConnectivity() {
        if (OnionRequestAPI.paths.isEmpty()) {
            Toast.makeText(requireActivity().applicationContext, getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT)
                .show();
        }
    }

    override fun onResume() {
        super.onResume()
        setupCallActionBar()
        ApplicationContext.getInstance(requireActivity().applicationContext).messageNotifier.setHomeScreenVisible(true)
        if (TextSecurePreferences.getLocalNumber(requireActivity().applicationContext) == null) {
            return; } // This can be the case after a secondary device is auto-cleared
        IdentityKeyUtil.checkUpdate(requireActivity().applicationContext)
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

        if (TextSecurePreferences.getConfigurationMessageSynced(requireActivity().applicationContext)) {
            lifecycleScope.launch(Dispatchers.IO) {
                ConfigurationMessageUtilities.syncConfigurationIfNeeded(requireActivity().applicationContext)
            }
        }

        /*Hales63*/
        if (TextSecurePreferences.isUnBlocked(requireActivity().applicationContext)) {
            homeAdapter.notifyDataSetChanged()
            TextSecurePreferences.setUnBlockStatus(requireActivity().applicationContext, false)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("Beldex","HomeActivity() onPause called")
        ApplicationContext.getInstance(requireActivity().applicationContext).messageNotifier.setHomeScreenVisible(false)
    }

    override fun onDestroy() {
        Log.d("Beldex","onDestroy in Home")
        val broadcastReceiver = this.broadcastReceiver
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(requireActivity().applicationContext).unregisterReceiver(broadcastReceiver)
        }
        super.onDestroy()

    }
    // endregion

    // region Updating
    private fun updateEmptyState() {
        val threadCount = (binding.recyclerView.adapter as HomeAdapter).itemCount
        binding.emptyStateContainer.isVisible = threadCount == 0 && binding.recyclerView.isVisible
        binding.emptyStateContainerText.isVisible =
            threadCount == 0 && binding.recyclerView.isVisible
        val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity())
        (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
            binding.emptyStateImageView.setImageResource(
                it
            )
        }
    }

    fun updateProfileButton() {
        binding.profileButton.publicKey = publicKey
        binding.profileButton.displayName = TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.profileButton.recycle()
        binding.profileButton.update()

        //New Line
        binding.drawerProfileName.text = TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.drawerProfileIcon.publicKey = publicKey
        binding.drawerProfileIcon.displayName = TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.drawerProfileIcon.recycle()
        binding.drawerProfileIcon.update()
    }
    // endregion

    // region Interaction
    fun onBackPressed() {
        if (binding.globalSearchRecycler.isVisible) {
            binding.globalSearchInputLayout.clearSearch(true)
        }
    }


    override fun onConversationClick(thread: ThreadRecord) {
        activityCallback?.onConversationClick(thread.threadId)
    }

    override fun onLongConversationClick(thread: ThreadRecord) {
        val bottomSheet = ConversationOptionsBottomSheet()
        bottomSheet.thread = thread
        bottomSheet.onViewDetailsTapped = {
            bottomSheet.dismiss()
            val userDetailsBottomSheet = UserDetailsBottomSheet()
            val bundle = bundleOf(
                UserDetailsBottomSheet.ARGUMENT_PUBLIC_KEY to thread.recipient.address.toString(),
                UserDetailsBottomSheet.ARGUMENT_THREAD_ID to thread.threadId
            )
            userDetailsBottomSheet.arguments = bundle
            userDetailsBottomSheet.show(requireActivity().supportFragmentManager, userDetailsBottomSheet.tag)
        }
        bottomSheet.onBlockTapped = {
            bottomSheet.dismiss()
            if (!thread.recipient.isBlocked) {
                blockConversation(thread)
            }
        }
        bottomSheet.onUnblockTapped = {
            bottomSheet.dismiss()
            if (thread.recipient.isBlocked) {
                unblockConversation(thread)
            }
        }
        bottomSheet.onDeleteTapped = {
            bottomSheet.dismiss()
            deleteConversation(thread)
        }
        bottomSheet.onSetMuteTapped = { muted ->
            bottomSheet.dismiss()
            setConversationMuted(thread, muted)
        }
        bottomSheet.onNotificationTapped = {
            bottomSheet.dismiss()
            NotificationUtils.showNotifyDialog(requireActivity(), thread.recipient) { notifyType ->
                setNotifyType(thread, notifyType)
            }
        }
        bottomSheet.onPinTapped = {
            bottomSheet.dismiss()
            setConversationPinned(thread.threadId, true,this)
        }
        bottomSheet.onUnpinTapped = {
            bottomSheet.dismiss()
            setConversationPinned(thread.threadId, false, this)
        }
        bottomSheet.onMarkAllAsReadTapped = {
            bottomSheet.dismiss()
            markAllAsRead(thread)
        }
        bottomSheet.show(requireActivity().supportFragmentManager, bottomSheet.tag)
    }

    private fun blockConversation(thread: ThreadRecord) {
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            .setTitle(R.string.RecipientPreferenceActivity_block_this_contact_question)
            .setMessage(R.string.RecipientPreferenceActivity_you_will_no_longer_receive_messages_and_calls_from_this_contact)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.RecipientPreferenceActivity_block) { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    (activity as HomeActivity).recipientDatabase.setBlocked(thread.recipient, true)
                    withContext(Dispatchers.Main) {
                        binding.recyclerView.adapter!!.notifyDataSetChanged()
                        dialog.dismiss()
                    }
                }
            }.show()
        //New Line
        val textView: TextView = dialog.findViewById(android.R.id.message)
        val face: Typeface = Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView.typeface = face
    }

    private fun unblockConversation(thread: ThreadRecord) {
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            .setTitle(R.string.RecipientPreferenceActivity_unblock_this_contact_question)
            .setMessage(R.string.RecipientPreferenceActivity_you_will_once_again_be_able_to_receive_messages_and_calls_from_this_contact)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.RecipientPreferenceActivity_unblock) { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    (activity as HomeActivity).recipientDatabase.setBlocked(thread.recipient, false)
                    withContext(Dispatchers.Main) {
                        binding.recyclerView.adapter!!.notifyDataSetChanged()
                        dialog.dismiss()
                    }
                }
            }.show()

        //New Line
        val textView: TextView = dialog.findViewById(android.R.id.message)
        val face: Typeface = Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView.typeface = face
    }

    private fun setConversationMuted(thread: ThreadRecord, isMuted: Boolean) {
        if (!isMuted) {
            lifecycleScope.launch(Dispatchers.IO) {
                (activity as HomeActivity).recipientDatabase.setMuted(thread.recipient, 0)
                withContext(Dispatchers.Main) {
                    binding.recyclerView.adapter!!.notifyDataSetChanged()
                }
            }
        } else {
            MuteDialog.show(requireActivity()) { until: Long ->
                lifecycleScope.launch(Dispatchers.IO) {
                    (activity as HomeActivity).recipientDatabase.setMuted(thread.recipient, until)
                    withContext(Dispatchers.Main) {
                        binding.recyclerView.adapter!!.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun setNotifyType(thread: ThreadRecord, newNotifyType: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            (activity as HomeActivity).recipientDatabase.setNotifyType(thread.recipient, newNotifyType)
            withContext(Dispatchers.Main) {
                binding.recyclerView.adapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun setConversationPinned(
        threadId: Long,
        pinned: Boolean,
        homeFragment: HomeFragment
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            (activity as HomeActivity).threadDb.setPinned(threadId, pinned)
            withContext(Dispatchers.Main) {
                LoaderManager.getInstance(homeFragment).restartLoader(0, null, homeFragment)
            }
        }
    }

    private fun markAllAsRead(thread: ThreadRecord) {
        ThreadUtils.queue {
            (activity as HomeActivity).threadDb.markAllAsRead(thread.threadId, thread.recipient.isOpenGroupRecipient)
        }
    }

    private fun deleteConversation(thread: ThreadRecord) {
        val threadID = thread.threadId
        val recipient = thread.recipient
        val message = if (recipient.isGroupRecipient) {
            val group = (activity as HomeActivity).groupDatabase.getGroup(recipient.address.toString()).orNull()
            if (group != null && group.admins.map { it.toString() }
                    .contains(TextSecurePreferences.getLocalNumber(requireActivity()))) {
                "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
            } else {
                resources.getString(R.string.activity_home_leave_group_dialog_message)
            }
        } else {
            resources.getString(R.string.activity_home_delete_conversation_dialog_message)
        }
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch(Dispatchers.Main) {
                    val context = requireActivity() as Context
                    // Cancel any outstanding jobs
                    DatabaseComponent.get(context).bchatJobDatabase()
                        .cancelPendingMessageSendJobs(threadID)
                    // Send a leave group message if this is an active closed group
                    if (recipient.address.isClosedGroup && DatabaseComponent.get(context)
                            .groupDatabase().isActive(recipient.address.toGroupString())
                    ) {
                        var isClosedGroup: Boolean
                        var groupPublicKey: String?
                        try {
                            groupPublicKey =
                                GroupUtil.doubleDecodeGroupID(recipient.address.toString())
                                    .toHexString()
                            isClosedGroup = DatabaseComponent.get(context).beldexAPIDatabase()
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
                        DatabaseComponent.get(requireActivity()).beldexThreadDatabase()
                            .getOpenGroupChat(threadID)
                    if (v2OpenGroup != null) {
                        OpenGroupManager.delete(
                            v2OpenGroup.server,
                            v2OpenGroup.room,
                            requireActivity()
                        )
                    } else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            (activity as HomeActivity).threadDb.deleteConversation(threadID)
                        }
                    }
                    // Update the badge count
                    ApplicationContext.getInstance(context).messageNotifier.updateNotification(
                        context
                    )
                    // Notify the user
                    val toastMessage =
                        if (recipient.isGroupRecipient) R.string.MessageRecord_left_group else R.string.activity_home_conversation_deleted_message
                    Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.no) { _, _ ->
                // Do nothing
            }.show()

        //New Line
        val textView: TextView? = dialog.findViewById(android.R.id.message)
        val face: Typeface = Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    override fun createNewPrivateChat() {
        activityCallback?.createNewPrivateChat()
    }

    override fun createNewSecretGroup() {
        activityCallback?.createNewSecretGroup()
    }

    override fun joinSocialGroup() {
        activityCallback?.joinSocialGroup()
    }

    interface HomeFragmentListener{
        fun callLifeCycleScope(
            recyclerView: RecyclerView,
            globalSearchInputLayout: GlobalSearchInputLayout,
            mmsSmsDatabase: MmsSmsDatabase,
            globalSearchAdapter: GlobalSearchAdapter,
            publicKey: String,
            profileButton: ProfilePictureView,
            drawerProfileName: TextView,
            drawerProfileIcon: ProfilePictureView
        )
        fun onConversationClick(threadId:Long)
        fun openSettings()
        fun openMyWallet()
        fun showNotificationSettings()
        fun showPrivacySettings()
        fun showQRCode()
        fun showSeed()
        fun showKeys()
        fun showAbout()
        fun showPath()
        fun showMessageRequests()
        fun sendMessageToSupport()
        fun help()
        fun sendInvitation(hexEncodedPublicKey:String)
        fun createNewPrivateChat()
        fun createNewSecretGroup()
        fun joinSocialGroup()
        fun toolBarCall()
        fun callAppPermission()
        fun passGlobalSearchAdapterModelMessageValue(
            threadId: Long,
            timestamp: Long,
            author: Address
        )
        fun passGlobalSearchAdapterModelSavedMessagesValue(address: Address)
        fun passGlobalSearchAdapterModelContactValue(address: Address)
        fun passGlobalSearchAdapterModelGroupConversationValue(threadId: Long)
    }

    fun dispatchTouchEvent() {
        if (binding.newConversationButtonSet.isExpanded) {
            binding.newConversationButtonSet.collapse()
        } else {
            //binding.newConversationButtonSet.collapse()
        }
    }
}
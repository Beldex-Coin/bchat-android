package com.thoughtcrimes.securesms.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.utilities.*
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils
import com.beldex.libsignal.utilities.toHexString
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.MuteDialog
import com.thoughtcrimes.securesms.calls.WebRtcCallActivity
import com.thoughtcrimes.securesms.components.ProfilePictureView
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.ComposeDialogContainer
import com.thoughtcrimes.securesms.compose_utils.DialogType
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.conversation.v2.utilities.NotificationUtils
import com.thoughtcrimes.securesms.conversation_v2.NewConversationActivity
import com.thoughtcrimes.securesms.conversation_v2.NewConversationType
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.database.*
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.drawer.ClickListener
import com.thoughtcrimes.securesms.drawer.NavigationItemModel
import com.thoughtcrimes.securesms.drawer.NavigationRVAdapter
import com.thoughtcrimes.securesms.drawer.RecyclerTouchListener
import com.thoughtcrimes.securesms.groups.CreateClosedGroupActivity
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import com.thoughtcrimes.securesms.home.search.GlobalSearchAdapter
import com.thoughtcrimes.securesms.home.search.GlobalSearchInputLayout
import com.thoughtcrimes.securesms.mms.GlideApp
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.my_account.ui.MyAccountActivity
import com.thoughtcrimes.securesms.my_account.ui.MyAccountScreens
import com.thoughtcrimes.securesms.my_account.ui.MyProfileActivity
import com.thoughtcrimes.securesms.preferences.NotificationSettingsActivity
import com.thoughtcrimes.securesms.preferences.PrivacySettingsActivity
import com.thoughtcrimes.securesms.search.SearchActivityResults
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.util.*
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.WalletFragment
import com.thoughtcrimes.securesms.wallet.info.WalletInfoActivity
import com.thoughtcrimes.securesms.wallet.startwallet.StartWalletInfo
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import com.thoughtcrimes.securesms.webrtc.CallViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentHomeBinding
import kotlinx.coroutines.*
import org.apache.commons.lang3.time.DurationFormatUtils
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt


@AndroidEntryPoint
class HomeFragment : BaseFragment(),ConversationClickListener,
    NewConversationButtonSetViewDelegate,
    GlobalSearchInputLayout.GlobalSearchInputLayoutListener {

    //Shortcut launcher
    companion object{
        @JvmStatic
        fun newInstance(threadId: Long, address: Address?,shortcut:Boolean) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putLong(ConversationFragmentV2.THREAD_ID, threadId)
                    putParcelable(ConversationFragmentV2.ADDRESS, address)
                    putBoolean(ConversationFragmentV2.SHORTCUT_LAUNCHER,shortcut)
                }
            }
    }

    val homeViewModel: HomeFragmentViewModel by viewModels()

    @Inject
    lateinit var threadDb: ThreadDatabase
    @Inject
    lateinit var mmsSmsDatabase: MmsSmsDatabase
    @Inject
    lateinit var recipientDatabase: RecipientDatabase
    @Inject
    lateinit var groupDb: GroupDatabase
    @Inject
    lateinit var beldexThreadDb: BeldexThreadDatabase
    @Inject
    lateinit var bchatContactDb: BchatContactDatabase
    @Inject
    lateinit var beldexApiDb: BeldexAPIDatabase
    @Inject
    lateinit var smsDb: SmsDatabase
    @Inject
    lateinit var mmsDb: MmsDatabase
    @Inject
    lateinit var beldexMessageDb: BeldexMessageDatabase
    private lateinit var binding: FragmentHomeBinding
    private lateinit var glide: GlideRequests
    private var broadcastReceiver: BroadcastReceiver? = null
    private var uiJob: Job? = null
    private var viewModel : CallViewModel? =null // by viewModels<CallViewModel>()
    private val callDurationFormat = "HH:mm:ss"

    private val publicKey: String
        get() = TextSecurePreferences.getLocalNumber(requireActivity().applicationContext)!!

    /*Hales63*/
    private val homeAdapter: HomeAdapter by lazy {
        HomeAdapter(context = requireActivity(), listener = this, threadDB = threadDb)
    }

    private val globalSearchAdapter = GlobalSearchAdapter { model ->
//        when (model) {
//            is GlobalSearchAdapter.Model.Message -> {
//                val threadId = model.messageResult.threadId
//                val timestamp = model.messageResult.sentTimestampMs
//                val author = model.messageResult.messageRecipient.address
//                if (binding.globalSearchRecycler.isVisible) {
//                    binding.globalSearchInputLayout.clearSearch(true)
//                }
//                passGlobalSearchAdapterModelMessageValue(threadId,timestamp,author)
//            }
//            is GlobalSearchAdapter.Model.SavedMessages -> {
//                if (binding.globalSearchRecycler.isVisible) {
//                    binding.globalSearchInputLayout.clearSearch(true)
//                }
//                passGlobalSearchAdapterModelSavedMessagesValue(Address.fromSerialized(model.currentUserPublicKey))
//            }
//            is GlobalSearchAdapter.Model.Contact -> {
//                val address = model.contact.bchatID
//                if (binding.globalSearchRecycler.isVisible) {
//                    binding.globalSearchInputLayout.clearSearch(true)
//                }
//                passGlobalSearchAdapterModelContactValue(Address.fromSerialized(address))
//            }
//            is GlobalSearchAdapter.Model.GroupConversation -> {
//                val groupAddress = Address.fromSerialized(model.groupRecord.encodedId)
//                val threadId = threadDb.getThreadIdIfExistsFor(Recipient.from(requireActivity().applicationContext, groupAddress, false))
//                if (threadId >= 0) {
//                    if (binding.globalSearchRecycler.isVisible) {
//                        binding.globalSearchInputLayout.clearSearch(true)
//                    }
//                    passGlobalSearchAdapterModelGroupConversationValue(threadId)
//                }
//            }
//            else -> {
//                Log.d("Beldex", "callback with model: $model")
//            }
//        }
    }

    //New Line
    private lateinit var adapter: NavigationRVAdapter

    private var items = arrayListOf(
        NavigationItemModel(R.drawable.ic_menu_outline, "My Account",0),
        NavigationItemModel(R.drawable.ic_settings_outline, "Settings",0),
        NavigationItemModel(R.drawable.ic_notification_outline, "Notification",0),
        NavigationItemModel(R.drawable.ic_msg_rqst_outline, "Message Requests",0),
        NavigationItemModel(R.drawable.ic_recovery_seed_outline, "Recovery Seed",0),
        NavigationItemModel(R.drawable.ic_wallet_outline, "Wallet",R.drawable.ic_beta),
        NavigationItemModel(R.drawable.ic_report_issue_outline,"Report Issue",0),
        NavigationItemModel(R.drawable.ic_help_outline, "Help",0),
        NavigationItemModel(R.drawable.ic_invite_outline, "Invite",0),
        NavigationItemModel(R.drawable.ic_about_outline, "About",0)
    )

    // NavigationItemModel(R.drawable.ic_recovery_key, "Recovery Key"),
    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(requireActivity().applicationContext)!!
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
                        val threadId = threadDb.getThreadIdIfExistsFor(Recipient.from(requireActivity().applicationContext, groupAddress, false))
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
                }
            }
        }
    }

    private var mContext : Context? = null
    var activityCallback: HomeFragmentListener? = null

    private val swipeController = SwipeController(
    object : SwipeControllerActions {
        override fun onRightClicked(position: Int) {
//            mAdapter.players.remove(position)
//            mAdapter.notifyItemRemoved(position)
//            mAdapter.notifyItemRangeChanged(position, mAdapter.getItemCount())
        }

        override fun onLeftClicked(position: Int) {

        }
    })

    private val swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) = true

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            Toast.makeText(requireContext(), "Message Deleted", Toast.LENGTH_SHORT).show()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater,container,false)
        // Set custom toolbar
        (activity as HomeActivity).setSupportActionBar(binding.toolbar)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //New Line
        viewModel = ViewModelProvider(requireActivity())[CallViewModel::class.java]

        // Set up Glide
        glide = GlideApp.with(this)
        // Set up toolbar buttons
        binding.profileButton.root.glide = glide

//        binding.bchatVersion.text = "BChat V${BuildConfig.VERSION_NAME}"
        //New Line
        // Setup Recyclerview's Layout
        binding.navigationMenu.navigationRv.layoutManager = LinearLayoutManager(requireActivity().applicationContext)
        binding.navigationMenu.navigationRv.setHasFixedSize(true)
        updateAdapter(0)
        // Add Item Touch Listener
        binding.navigationMenu.navigationRv.addOnItemTouchListener(RecyclerTouchListener(requireActivity().applicationContext, object :
            ClickListener {
            override fun onClick(view: View, position: Int) {
                when (position) {
                    0 -> {
                        // # Account Activity
                        openSettings()
                    }
                    1 -> {
                        // # Privacy Activity
                        showPrivacySettings()
                    }
                    2 -> {
                        // # Notification Activity
                        showNotificationSettings()
                    }
                    3 -> {
                        // # Message Requests Activity
                        showMessageRequests()
                    }
//                    4 -> {
//                        // # App Permissions Activity
//                        callAppPermission()
//                    }
                    4 -> {
                        // # Recovery Seed Activity
                        showSeed()
                    }
                    5 -> {
                        // # My Wallet Activity
                        if (CheckOnline.isOnline(requireActivity().applicationContext)) {
                            if (TextSecurePreferences.isWalletActive(requireContext())) {
                                openMyWallet()
                            } else {
                                openStartWalletInfo()
                            }
                        } else {
                            Toast.makeText(requireActivity().applicationContext, getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                        }
                    }
                    6 -> {
                        // # Support
                        activityCallback?.sendMessageToSupport()
                    }
                    7 -> {
                        // # Help Activity
                        help()
                    }
                    8 -> {
                        // # Invite Activity
                        sendInvitation(hexEncodedPublicKey)
                    }
                    9 -> {
                        // # About Activity
                        showAbout()
                    }
                }
                // Don't highlight the 'Profile' and 'Like us on Facebook' item row
                if (position != 6 && position != 4) {
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
        val activeUiMode = UiModeUtilities.getUserSelectedUiMode(requireActivity())
        binding.navigationMenu.drawerAppearanceToggleButton.isChecked = activeUiMode == UiMode.NIGHT

        binding.navigationMenu.drawerAppearanceToggleButton.setOnClickListener{
            if(binding.navigationMenu.drawerAppearanceToggleButton.isChecked){
                val uiMode = UiMode.values()[1]
                UiModeUtilities.setUserSelectedUiMode(requireActivity(), uiMode)
            }
            else{
                val uiMode = UiMode.values()[0]
                UiModeUtilities.setUserSelectedUiMode(requireActivity(), uiMode)
            }
        }
        binding.navigationMenu.drawerAppearanceToggleButton.setOnTouchListener { _, event ->
            event.actionMasked == MotionEvent.ACTION_MOVE
        }
        binding.navigationMenu.drawerQrcodeImg.setOnClickListener {
            showQRCode()
            Handler(Looper.getMainLooper()).postDelayed({
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }, 200)
        }
        binding.navigationMenu.drawerProfileIcon.root.glide = glide
        binding.navigationMenu.drawerProfileIcon.root.isClickable = true
        binding.navigationMenu.drawerProfileId.text = String.format(requireContext().resources.getString(R.string.id_format), hexEncodedPublicKey)

//        binding.searchViewContainer.setOnClickListener {
//            binding.globalSearchInputLayout.requestFocus()
//            Intent(requireContext(), SearchActivity::class.java).also {
//                searchResultLauncher.launch(it)
//            }
//        }
        binding.bchatToolbar.disableClipping()

//        setupMessageRequestsBanner()
        setupHeaderImage()
        // Set up recycler view
//        binding.globalSearchInputLayout.listener = this
        /*homeAdapter.setHasStableIds(true)*/
        homeAdapter.glide = glide
        binding.recyclerView.adapter = homeAdapter
        swipeHelper.attachToRecyclerView(binding.recyclerView)
//        val itemTouchHelper = ItemTouchHelper(swipeController)
//        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
//        binding.recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
//            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
//                swipeController.onDraw(c)
//            }
//        })
//        binding.globalSearchRecycler.adapter = globalSearchAdapter
        // Set up empty state view
        binding.createNewPrivateChatButton.setOnClickListener { createNewPrivateChat() }
        homeViewModel.getObservable(requireActivity().applicationContext).observe(requireActivity()) { newData ->
//            val manager = binding.recyclerView.layoutManager as LinearLayoutManager
//            val firstPos = manager.findFirstCompletelyVisibleItemPosition()
//            val offsetTop = if(firstPos >= 0) {
//                manager.findViewByPosition(firstPos)?.let { view ->
//                    manager.getDecoratedTop(view) - manager.getTopDecorationHeight(view)
//                } ?: 0
//            } else 0
            val messageRequestCount = threadDb.unapprovedConversationCount
            var request = emptyList<ThreadRecord>()
            if (messageRequestCount > 0 && !TextSecurePreferences.hasHiddenMessageRequests(requireContext())) {
                threadDb.unapprovedConversationList.use { openCursor ->
                    val reader = threadDb.readerFor(openCursor)
                    val threads = mutableListOf<ThreadRecord>()
                    while (true) {
                        threads += reader.next ?: break
                    }
                    request = threads
                }
            }
            binding.requests.setContent {
                BChatTheme {
                    MessageRequestsView(
                        requests = request,
                        openSearch = {
                            Intent(requireContext(), SearchActivity::class.java).also {
                                searchResultLauncher.launch(it)
                            }
                        },
                        ignoreRequest = {
                            val dialog = ComposeDialogContainer(
                                dialogType = DialogType.IgnoreRequest,
                                onConfirm = {
                                    showRequestDeleteDialog(it)
                                },
                                onCancel = {
                                    showRequestBlockDialog(it)
                                }
                            )
                            dialog.show(childFragmentManager, ComposeDialogContainer.TAG)
                        },
                        openChat = {
                            onConversationClick(it.threadId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp
                            )
                    )
                }
            }
            homeAdapter.data = newData
//            if(firstPos >= 0) { manager.scrollToPositionWithOffset(firstPos, offsetTop) }
//            setupMessageRequestsBanner()
            updateEmptyState()
        }
        ApplicationContext.getInstance(requireActivity()).typingStatusRepository.typingThreads.observe(requireActivity()) { threadIds ->
            homeAdapter.typingThreadIDs = (threadIds ?: setOf())
        }
        homeViewModel.tryUpdateChannel()
        // Set up new conversation button set
//        binding.newConversationButtonSet.delegate = this
        // Observe blocked contacts changed events
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.recyclerView.adapter!!.notifyDataSetChanged()
            }
        }
        this.broadcastReceiver = broadcastReceiver
        LocalBroadcastManager.getInstance(requireActivity().applicationContext)
            .registerReceiver(broadcastReceiver, IntentFilter("blockedContactsChanged"))
        activityCallback?.callLifeCycleScope(binding.recyclerView, mmsSmsDatabase,globalSearchAdapter,publicKey,binding.profileButton.root,binding.navigationMenu.drawerProfileName,binding.navigationMenu.drawerProfileIcon.root)
        binding.chatButtons.setContent {
            val isExpanded by homeViewModel.isButtonExpanded.collectAsState()
            BChatTheme {
                NewChatButtons(
                    isExpanded = isExpanded,
                    changeExpandedStatus = homeViewModel::setButtonExpandedStatus,
                    createPrivateChat = {
                        createNewPrivateChat()
                    },
                    createSecretGroup = {
                        createNewSecretGroup()
                    },
                    joinPublicGroup = {
                        joinSocialGroup()
                    }
                )
            }
        }

        binding.navigationMenu.menuContainer.run {
            post {
                val params = layoutParams as DrawerLayout.LayoutParams
                params.width = (getScreenWidth() * 0.7).toInt()
                layoutParams = params
                translationX = -(requireContext().toPx(16))
            }
        }
        binding.navigationMenu.version.text = resources.getString(R.string.version_name).format(BuildConfig.VERSION_NAME)
    }

    private fun showRequestDeleteDialog(record: ThreadRecord) {
        val dialog = ComposeDialogContainer(
            dialogType = DialogType.DeleteRequest,
            onConfirm = {
                homeViewModel.deleteMessageRequest(record)
            },
            onCancel = {}
        )
        dialog.show(childFragmentManager, ComposeDialogContainer.TAG)
    }

    private fun showRequestBlockDialog(record: ThreadRecord) {
        val dialog = ComposeDialogContainer(
            dialogType = DialogType.BlockRequest,
            onConfirm = {
                homeViewModel.blockMessageRequest(record)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(requireContext())
                }
            },
            onCancel = {}
        )
        dialog.show(childFragmentManager, ComposeDialogContainer.TAG)
    }


    /*Hales63*/
//    private fun setupMessageRequestsBanner() {
//        println(">>>>>setting banner")
//            val messageRequestCount = threadDb.unapprovedConversationCount
//            // Set up message requests
//            if (messageRequestCount > 0 && !(activity as HomeActivity).textSecurePreferences.hasHiddenMessageRequests()) {
//                println(">>>>>setting banner:$messageRequestCount")
//                with(ViewMessageRequestBannerBinding.inflate(layoutInflater)) {
//                    unreadCountTextView.text = messageRequestCount.toString()
//                    timestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(
//                        requireActivity().applicationContext,
//                        Locale.getDefault(),
//                        threadDb.latestUnapprovedConversationTimestamp
//                    )
//                    root.setOnClickListener { showMessageRequests() }
//                    expandMessageRequest.setOnClickListener { showMessageRequests() }
//                    root.setOnLongClickListener { hideMessageRequests(); true }
//                    root.layoutParams = RecyclerView.LayoutParams(
//                        RecyclerView.LayoutParams.MATCH_PARENT,
//                        RecyclerView.LayoutParams.WRAP_CONTENT
//                    )
//                    val hadHeader = homeAdapter.hasHeaderView()
//                    homeAdapter.header = root
//                    if (hadHeader) {
//                        println(">>>>updating item")
//                        homeAdapter.notifyItemRemoved(0)
//                    } else {
//                        println(">>>>adding item")
//                        homeAdapter.notifyItemInserted(0)
//                    }
//                }
//            } else {
//                val hadHeader = homeAdapter.hasHeaderView()
//                homeAdapter.header = null
//                if (hadHeader) {
//                    homeAdapter.notifyItemRemoved(0)
//                }
//            }
//    }

    override fun hideMessageRequests() {
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_New)
            .setTitle("Hide message requests?")
            .setMessage("Once they are hidden, you can access them from Settings > Message Requests")
            .setPositiveButton(R.string.yes) { _, _ ->
                (activity as HomeActivity).textSecurePreferences.setHasHiddenMessageRequests()
//                setupMessageRequestsBanner()
                homeViewModel.tryUpdateChannel()
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
                                callDurationFormat
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
            toolBarCall()
        }
    }

    //New Line
    private fun updateAdapter(highlightItemPos: Int) {
        adapter = NavigationRVAdapter(items, highlightItemPos)
        binding.navigationMenu.navigationRv.adapter = adapter
        adapter.notifyDataSetChanged()
    }


    private fun setupHeaderImage() {
        val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity())
        val headerTint = if (isDayUiMode) R.color.black else R.color.white
        binding.bchatHeaderImage.setTextColor(getColor(requireActivity().applicationContext,headerTint))
    }

    override fun onInputFocusChanged(hasFocus: Boolean) {
//        if (hasFocus) {
//            setSearchShown(true)
//        } else {
//            setSearchShown(!binding.globalSearchInputLayout.query.value.isNullOrEmpty())
//        }
    }

    private fun setSearchShown(isShown: Boolean) {
        //New Line
//        binding.searchBarLayout.isVisible = isShown
//        binding.searchBarBackButton.setOnClickListener {
//            binding.globalSearchInputLayout.onFocus()
//            binding.globalSearchInputLayout.clearSearch(true)
//            onBackPressed()
//        }
//
//        binding.searchToolbar.isVisible = isShown
//        binding.searchViewCard.isVisible = !isShown
//        binding.bchatToolbar.isVisible = !isShown
//        binding.recyclerView.isVisible = !isShown
//        binding.emptyStateContainer.isVisible =
//            (binding.recyclerView.adapter as HomeAdapter).itemCount == 0 && binding.recyclerView.isVisible
//        binding.emptyStateContainerText.isVisible =
//            (binding.recyclerView.adapter as HomeAdapter).itemCount == 0 && binding.recyclerView.isVisible
//        val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity())
//        (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
//            binding.emptyStateImageView.setImageResource(
//                it
//            )
//        }
//        binding.gradientView.isVisible = !isShown
//        binding.globalSearchRecycler.isVisible = isShown
//        binding.newConversationButtonSet.isVisible = !isShown
    }

    override fun onResume() {
        super.onResume()
        setupCallActionBar()
        if(TextSecurePreferences.isWalletActive(requireContext())) {
            pingSelectedNode()
        }
        ApplicationContext.getInstance(requireActivity().applicationContext).messageNotifier.setHomeScreenVisible(false)
        if (TextSecurePreferences.getLocalNumber(requireActivity().applicationContext) == null) {
            return; } // This can be the case after a secondary device is auto-cleared
        IdentityKeyUtil.checkUpdate(requireActivity().applicationContext)
        binding.profileButton.root.recycle() // clear cached image before update tje profilePictureView
        binding.profileButton.root.update()

        //New Line
        binding.navigationMenu.drawerProfileIcon.root.recycle()
        binding.navigationMenu.drawerProfileIcon.root.update()

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

        //Shortcut launcher
        if(arguments?.getBoolean(ConversationFragmentV2.SHORTCUT_LAUNCHER,false) == true){
            arguments?.remove(ConversationFragmentV2.SHORTCUT_LAUNCHER)
            callConversationScreen(
                requireArguments().getLong(ConversationFragmentV2.THREAD_ID,-1L),
                requireArguments().parcelable(ConversationFragmentV2.ADDRESS),
                requireArguments().parcelable(ConversationFragmentV2.URI),
                requireArguments().getString(ConversationFragmentV2.TYPE),
                requireArguments().getCharSequence(Intent.EXTRA_TEXT)
            )
        }
        if(!TextSecurePreferences.isCopiedSeed(requireActivity().applicationContext)){
            showSaveYourSeedDialog()
        }
    }

    private fun showSaveYourSeedDialog(){
        try {
            activityCallback?.let {
                SaveYourSeedDialogBox(
                    showSeed = {
                        showSeed()
                    }
                ).show(requireActivity().supportFragmentManager, "")
            }
        } catch (exception: Exception) {
            Timber.tag("Beldex").d("Save your seed dialog box exception $exception")
        }
    }

    override fun onPause() {
        super.onPause()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
//        imm?.hideSoftInputFromWindow(binding.globalSearchInputLayout.windowToken, 0)
//        binding.globalSearchInputLayout.clearFocus()
    }

    override fun onDestroy() {
        val broadcastReceiver = this.broadcastReceiver
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(requireActivity().applicationContext).unregisterReceiver(broadcastReceiver)
        }
        super.onDestroy()

    }

    private fun updateEmptyState() {
        val threadCount = (binding.recyclerView.adapter)!!.itemCount
        binding.emptyStateContainer.isVisible = threadCount == 0 && binding.recyclerView.isVisible
        binding.emptyStateContainerText.isVisible =
            threadCount == 0 && binding.recyclerView.isVisible
        val activity =activity
        if(isAdded && activity !=null) {
            val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity())
            (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
                binding.emptyStateImageView.setImageResource(
                    it
                )
            }
        }
    }

    fun updateProfileButton() {
        binding.profileButton.root.publicKey = publicKey
        binding.profileButton.root.displayName = TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.profileButton.root.recycle()
        binding.profileButton.root.update()

        //New Line
        binding.navigationMenu.drawerProfileName.text = TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.navigationMenu.drawerProfileIcon.root.publicKey = publicKey
        binding.navigationMenu.drawerProfileIcon.root.displayName = TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.navigationMenu.drawerProfileIcon.root.recycle()
        binding.navigationMenu.drawerProfileIcon.root.update()
    }

    fun onBackPressed() {
//        if (binding.globalSearchRecycler.isVisible) {
//            binding.globalSearchInputLayout.clearSearch(true)
//        }
    }


    override fun onConversationClick(thread: ThreadRecord) {
        onConversationClick(thread.threadId)
    }

    override fun onLongConversationClick(thread: ThreadRecord, view: View) {
        val recipient = thread.recipient
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.menu_conversation_v2, popupMenu.menu)
        popupMenu.gravity = Gravity.END
        popupMenu.setForceShowIcon(true)
        with(popupMenu.menu) {
            if (recipient.isGroupRecipient && !recipient.isLocalNumber) {
                findItem(R.id.menu_details).setVisible(true)
                findItem(R.id.menu_unblock).setVisible(recipient.isBlocked)
                findItem(R.id.menu_block).setVisible(!recipient.isBlocked)
            } else {
                findItem(R.id.menu_details).setVisible(false)
            }
            findItem(R.id.menu_unmute_notifications).setVisible(recipient.isMuted && !recipient.isLocalNumber)
            findItem(R.id.menu_mute_notifications).setVisible(!recipient.isMuted && !recipient.isLocalNumber)
            findItem(R.id.menu_notification_settings).setVisible(recipient.isGroupRecipient && !recipient.isMuted)
            findItem(R.id.menu_mark_read).setVisible(thread.unreadCount > 0)
            findItem(R.id.menu_pin).setVisible(!thread.isPinned)
            findItem(R.id.menu_unpin).setVisible(thread.isPinned)
        }
        popupMenu.setOnMenuItemClickListener {
            handlePopUpMenuClickListener(it, thread)
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
//        val bottomSheet = ConversationOptionsBottomSheet()
//        bottomSheet.thread = thread
//        bottomSheet.onViewDetailsTapped = {
//            bottomSheet.dismiss()
//            val userDetailsBottomSheet = UserDetailsBottomSheet()
//            val bundle = bundleOf(
//                UserDetailsBottomSheet.ARGUMENT_PUBLIC_KEY to thread.recipient.address.toString(),
//                UserDetailsBottomSheet.ARGUMENT_THREAD_ID to thread.threadId
//            )
//            userDetailsBottomSheet.arguments = bundle
//            userDetailsBottomSheet.show(childFragmentManager, userDetailsBottomSheet.tag)
//        }
//        bottomSheet.onBlockTapped = {
//            bottomSheet.dismiss()
//            if (!thread.recipient.isBlocked) {
//                blockConversation(thread)
//            }
//        }
//        bottomSheet.onUnblockTapped = {
//            bottomSheet.dismiss()
//            if (thread.recipient.isBlocked) {
//                unblockConversation(thread)
//            }
//        }
//        bottomSheet.onDeleteTapped = {
//            bottomSheet.dismiss()
//            deleteConversation(thread)
//        }
//        bottomSheet.onSetMuteTapped = { muted ->
//            bottomSheet.dismiss()
//            setConversationMuted(thread, muted)
//        }
//        bottomSheet.onNotificationTapped = {
//            bottomSheet.dismiss()
//            NotificationUtils.showNotifyDialog(requireActivity(), thread.recipient) { notifyType ->
//                setNotifyType(thread, notifyType)
//            }
//        }
//        bottomSheet.onPinTapped = {
//            bottomSheet.dismiss()
//            setConversationPinned(thread.threadId, true)
//        }
//        bottomSheet.onUnpinTapped = {
//            bottomSheet.dismiss()
//            setConversationPinned(thread.threadId, false)
//        }
//        bottomSheet.onMarkAllAsReadTapped = {
//            bottomSheet.dismiss()
//            markAllAsRead(thread)
//        }
//        bottomSheet.show(requireActivity().supportFragmentManager, bottomSheet.tag)
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
                userDetailsBottomSheet.show(childFragmentManager, userDetailsBottomSheet.tag)
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
                NotificationUtils.showNotifyDialog(requireActivity(), thread.recipient) { notifyType ->
                    setNotifyType(thread, notifyType)
                }
            }
            R.id.menu_mark_read -> {
                markAllAsRead(thread)
            }
            R.id.menu_delete -> {
                deleteConversation(thread)
            }
            else -> Unit
        }
    }

    private fun blockConversation(thread: ThreadRecord) {
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            .setTitle(R.string.RecipientPreferenceActivity_block_this_contact_question)
            .setMessage(R.string.RecipientPreferenceActivity_you_will_no_longer_receive_messages_and_calls_from_this_contact)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.RecipientPreferenceActivity_block) { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    recipientDatabase.setBlocked(thread.recipient, true)
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
                    recipientDatabase.setBlocked(thread.recipient, false)
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
                recipientDatabase.setMuted(thread.recipient, 0)
                withContext(Dispatchers.Main) {
                    binding.recyclerView.adapter!!.notifyDataSetChanged()
                }
            }
        } else {
            MuteDialog.show(requireActivity()) { until: Long ->
                lifecycleScope.launch(Dispatchers.IO) {
                    recipientDatabase.setMuted(thread.recipient, until)
                    withContext(Dispatchers.Main) {
                        binding.recyclerView.adapter!!.notifyDataSetChanged()
                    }
                }
            }
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
        val threadID = thread.threadId
        val recipient = thread.recipient
        val message = if (recipient.isGroupRecipient) {
            val group = groupDb.getGroup(recipient.address.toString()).orNull()
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
                            threadDb.deleteConversation(threadID)
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
        val face: Typeface =
            Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    interface HomeFragmentListener{
        fun callLifeCycleScope(
            recyclerView: RecyclerView,
            mmsSmsDatabase: MmsSmsDatabase,
            globalSearchAdapter: GlobalSearchAdapter,
            publicKey: String,
            profileButton: ProfilePictureView,
            drawerProfileName: TextView,
            drawerProfileIcon: ProfilePictureView
        )
        fun sendMessageToSupport()
        //Node Connection
        fun getFavouriteNodes(): MutableSet<NodeInfo>
        fun getOrPopulateFavourites(): MutableSet<NodeInfo>
        fun getNode(): NodeInfo?
        fun setNode(node: NodeInfo?)

        //Wallet
        fun hasBoundService(): Boolean
        val connectionStatus: Wallet.ConnectionStatus?
    }

    fun dispatchTouchEvent() {
    }

    private fun pingSelectedNode() {
        val pingSelected = 0
        val findBest = 1
        AsyncFindBestNode(pingSelected, findBest).execute<Int>(pingSelected)
    }

    inner class AsyncFindBestNode(private val pingSelected: Int, private val findBest: Int) :
        AsyncTaskCoroutine<Int?, NodeInfo?>() {

        override fun doInBackground(vararg params: Int?): NodeInfo? {
            val favourites: Set<NodeInfo?> = activityCallback!!.getOrPopulateFavourites()
            var selectedNode: NodeInfo?
            if (params[0] == findBest) {
                selectedNode = autoselect(favourites)
            } else if (params[0] == pingSelected) {
                selectedNode = activityCallback!!.getNode()
                if (selectedNode == null) {
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
            } else throw IllegalStateException()
            return if (selectedNode != null && selectedNode.isValid) {
                activityCallback!!.setNode(selectedNode)
                selectedNode
            } else {
                activityCallback!!.setNode(null)
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

    private fun showAbout() {
//        Intent(requireContext(), AboutActivity::class.java).also {
//            show(it)
//        }
        Intent(activity, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.AboutScreen.route)
            startActivity(it)
        }
    }

    private fun sendInvitation(hexEncodedPublicKey:String) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        val invitation = String.format(requireContext().resources.getString(R.string.invitation_msg), hexEncodedPublicKey)
        intent.putExtra(Intent.EXTRA_TEXT, invitation)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.activity_settings_invite_button_title))
        startActivity(chooser)
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
        Intent(activity, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.RecoverySeedScreen.route)
            startActivity(it)
        }
    }

    private fun callAppPermission() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        push(intent)
    }

    private fun showPrivacySettings() {
        Intent(requireContext(), PrivacySettingsActivity::class.java).also {
            push(it)
        }
    }

    private fun showNotificationSettings() {
        Intent(requireContext(), NotificationSettingsActivity::class.java).also {
            push(it)
        }
    }

    private fun onConversationClick(threadId: Long) {
        val extras = Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID, threadId)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    private fun openSettings() {
        val activity = activity
        if(isAdded && activity !=null) {
            Intent(activity, MyAccountActivity::class.java).also {
                it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.SettingsScreen.route)
                startActivity(it)
            }
        }
    }

    private var callSettingsActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.parcelable(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.URI,result.data!!.parcelable(ConversationFragmentV2.URI))
            extras.putString(ConversationFragmentV2.TYPE,result.data!!.getStringExtra(ConversationFragmentV2.TYPE))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }else {
            homeAdapter.notifyDataSetChanged()
        }
    }

    private fun openMyWallet() {
        val walletName = TextSecurePreferences.getWalletName(requireContext())
        val walletPassword = TextSecurePreferences.getWalletPassword(requireContext())
        if (walletName != null && walletPassword !=null) {
            //startWallet(walletName, walletPassword, fingerprintUsed = false, streetmode = false)
            val lockManager: LockManager<CustomPinActivity> = LockManager.getInstance() as LockManager<CustomPinActivity>
            lockManager.enableAppLock(requireContext(), CustomPinActivity::class.java)
            Intent(requireContext(), CustomPinActivity::class.java).also {
                if(TextSecurePreferences.getWalletEntryPassword(requireContext())!=null) {
                    it.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN)
                    it.putExtra("change_pin",false)
                    it.putExtra("send_authentication",false)
                    customPinActivityResultLauncher.launch(it)
                } else{
                    it.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK)
                    it.putExtra("change_pin",false)
                    it.putExtra("send_authentication",false)
                    customPinActivityResultLauncher.launch(it)
                }
            }
        }else{
            Intent(requireContext(), WalletInfoActivity::class.java).also {
                push(it)
            }
        }
    }

    private fun openStartWalletInfo(){
        /*Intent(requireContext(), StartWalletInfo::class.java).also {
            push(it)
        }*/
        Intent(activity, MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.StartWalletInfoScreen.route)
            startActivity(it)
        }
    }

    private var customPinActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            replaceFragment(WalletFragment(), WalletFragment::class.java.name, null)
        }
    }

    private fun showQRCode() {
//        Intent(requireContext(), ShowQRCodeWithScanQRCodeActivity::class.java).also {
//            showQRCodeWithScanQRCodeActivityResultLauncher.launch(it)
//        }
        //Intent(activity, MyProfileActivity::class.java).also {
//            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.MyAccountScreen.route)
            //startActivity(it)
        //}

        val intent = Intent(activity,MyProfileActivity::class.java)
        intent.putExtra("profile_editable",true)
        startActivity(intent)
    }

    private var showQRCodeWithScanQRCodeActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.parcelable(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.URI,result.data!!.parcelable(ConversationFragmentV2.URI))
            extras.putString(ConversationFragmentV2.TYPE,result.data!!.getStringExtra(ConversationFragmentV2.TYPE))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    private fun showPath() {
        Intent(requireContext(), PathActivity::class.java).also {
            show(it)
        }
    }

    override fun showMessageRequests() {
        Intent(requireContext(), MyAccountActivity::class.java).also {
            it.putExtra(MyAccountActivity.extraStartDestination, MyAccountScreens.MessageRequestsScreen.route)
            resultLauncher.launch(it)
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    override fun createNewPrivateChat() {
        val intent = Intent(requireContext(), NewConversationActivity::class.java).apply {
            putExtra(NewConversationActivity.EXTRA_DESTINATION, NewConversationType.PrivateChat.destination)
        }
        createNewPrivateChatResultLauncher.launch(intent)
    }

    private var createNewPrivateChatResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.parcelable(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.URI,result.data!!.parcelable(ConversationFragmentV2.URI))
            extras.putString(ConversationFragmentV2.TYPE,result.data!!.getStringExtra(ConversationFragmentV2.TYPE))
            extras.putString(ConversationFragmentV2.BNS_NAME,result.data!!.getStringExtra(ConversationFragmentV2.BNS_NAME))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    override fun createNewSecretGroup() {
        val intent = Intent(requireContext(), NewConversationActivity::class.java).apply {
            putExtra(NewConversationActivity.EXTRA_DESTINATION, NewConversationType.SecretGroup.destination)
        }
        createClosedGroupActivityResultLauncher.launch(intent)
    }

    private var createClosedGroupActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.ADDRESS,result.data!!.parcelable(ConversationFragmentV2.ADDRESS))
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
        if (result.resultCode == CreateClosedGroupActivity.closedGroupCreatedResultCode) {
            createNewPrivateChat()
        }
    }

    override fun joinSocialGroup() {
        val intent = Intent(requireContext(), NewConversationActivity::class.java).apply {
            putExtra(NewConversationActivity.EXTRA_DESTINATION, NewConversationType.PublicGroup.destination)
        }
        joinPublicChatNewActivityResultLauncher.launch(intent)
    }

    private var joinPublicChatNewActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putLong(
                ConversationFragmentV2.THREAD_ID,
                result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID, -1)
            )
            extras.putParcelable(
                ConversationFragmentV2.ADDRESS,
                result.data!!.parcelable(ConversationFragmentV2.ADDRESS)
            )
            replaceFragment(ConversationFragmentV2(), null, extras)
        }
    }

    private fun toolBarCall() {
        Intent(requireContext(), WebRtcCallActivity::class.java).also {
            push(it)
        }
    }

    private fun passGlobalSearchAdapterModelSavedMessagesValue(address: Address) {
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS,address)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    private fun passGlobalSearchAdapterModelContactValue(address: Address) {
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS,address)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    private fun passGlobalSearchAdapterModelGroupConversationValue(threadId: Long) {
        val extras = Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID,threadId)
        replaceFragment(ConversationFragmentV2(),null,extras)
    }

    private fun passGlobalSearchAdapterModelMessageValue(
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

    private fun callConversationScreen(threadId: Long, address: Address?, uri: Uri?, type: String?, extraText: CharSequence?) {
        val extras = Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS,address)
        extras.putLong(ConversationFragmentV2.THREAD_ID, threadId)
        extras.putParcelable(ConversationFragmentV2.URI,uri)
        extras.putString(ConversationFragmentV2.TYPE,type)
        extras.putCharSequence(Intent.EXTRA_TEXT,extraText)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }
}
//endregion
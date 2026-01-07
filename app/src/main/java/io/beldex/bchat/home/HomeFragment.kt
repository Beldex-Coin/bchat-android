package io.beldex.bchat.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupRecord
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils
import com.beldex.libsignal.utilities.toHexString
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.components.ProfilePictureView
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation_v2.NewChatConversationActivity
import io.beldex.bchat.conversation_v2.NewGroupConversationActivity
import io.beldex.bchat.conversation_v2.NewGroupConversationType
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.data.NodeInfo
import io.beldex.bchat.database.BchatContactDatabase
import io.beldex.bchat.database.BeldexAPIDatabase
import io.beldex.bchat.database.BeldexMessageDatabase
import io.beldex.bchat.database.BeldexThreadDatabase
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.database.MmsDatabase
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.database.RecipientDatabase
import io.beldex.bchat.database.SmsDatabase
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.databinding.FragmentHomeBinding
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.drawer.ClickListener
import io.beldex.bchat.drawer.NavigationItemModel
import io.beldex.bchat.drawer.NavigationRVAdapter
import io.beldex.bchat.drawer.RecyclerTouchListener
import io.beldex.bchat.groups.CreateClosedGroupActivity
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.home.search.GlobalSearchAdapter
import io.beldex.bchat.home.search.GlobalSearchInputLayout
import io.beldex.bchat.home.search.RecyclerViewDivider
import io.beldex.bchat.model.AsyncTaskCoroutine
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.my_account.ui.MyAccountActivity
import io.beldex.bchat.my_account.ui.MyAccountScreens
import io.beldex.bchat.my_account.ui.MyProfileActivity
import io.beldex.bchat.onboarding.ui.EXTRA_PIN_CODE_ACTION
import io.beldex.bchat.onboarding.ui.PinCodeAction
import io.beldex.bchat.preferences.NotificationSettingsActivity
import io.beldex.bchat.preferences.PrivacySettingsActivity
import io.beldex.bchat.repository.ConversationRepository
import io.beldex.bchat.search.SearchActivityResults
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.util.BaseFragment
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.util.NodePinger
import io.beldex.bchat.util.SaveYourSeedDialogBox
import io.beldex.bchat.util.SwipeController
import io.beldex.bchat.util.SwipeControllerActions
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.disableClipping
import io.beldex.bchat.util.getScreenWidth
import io.beldex.bchat.util.nodelistasync.NodeListConstants
import io.beldex.bchat.util.parcelable
import io.beldex.bchat.util.toPx
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.wallet.WalletFragment
import io.beldex.bchat.wallet.info.WalletInfoActivity
import io.beldex.bchat.wallet.utils.pincodeview.CustomPinActivity
import io.beldex.bchat.wallet.utils.pincodeview.managers.LockManager
import io.beldex.bchat.webrtc.CallViewModel
import io.beldex.bchat.webrtc.WebRTCComposeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.time.DurationFormatUtils
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.Random
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt


@AndroidEntryPoint
class HomeFragment : BaseFragment(), ConversationClickListener,
    NewConversationButtonSetViewDelegate,
    GlobalSearchInputLayout.GlobalSearchInputLayoutListener,
    ConversationActionDialog.ConversationActionDialogListener {

    //Shortcut launcher
    companion object {
        @JvmStatic
        fun newInstance(threadId : Long, address : Address?, shortcut : Boolean)=
            HomeFragment().apply {
                arguments=Bundle().apply {
                    putLong(ConversationFragmentV2.THREAD_ID, threadId)
                    putParcelable(ConversationFragmentV2.ADDRESS, address)
                    putBoolean(ConversationFragmentV2.SHORTCUT_LAUNCHER, shortcut)
                }
            }
    }

    val homeViewModel : HomeFragmentViewModel by viewModels()

    @Inject
    lateinit var threadDb : ThreadDatabase

    @Inject
    lateinit var mmsSmsDatabase : MmsSmsDatabase

    @Inject
    lateinit var recipientDatabase : RecipientDatabase

    @Inject
    lateinit var groupDb : GroupDatabase

    @Inject
    lateinit var beldexThreadDb : BeldexThreadDatabase

    @Inject
    lateinit var bchatContactDb : BchatContactDatabase

    @Inject
    lateinit var beldexApiDb : BeldexAPIDatabase

    @Inject
    lateinit var smsDb : SmsDatabase

    @Inject
    lateinit var mmsDb : MmsDatabase

    @Inject
    lateinit var beldexMessageDb : BeldexMessageDatabase
    private lateinit var binding : FragmentHomeBinding
    private lateinit var glide : RequestManager
    private var broadcastReceiver : BroadcastReceiver?=null
    private var uiJob : Job?=null
    private var viewModel : CallViewModel?=null // by viewModels<CallViewModel>()
    private val callDurationFormat="HH:mm:ss"
    private var archiveChatViewModel : ArchiveChatViewModel?=null

    @Inject
    lateinit var repository : ConversationRepository

    private val publicKey : String
        get()=TextSecurePreferences.getLocalNumber(requireActivity().applicationContext)!!

    /*Hales63*/
    private val homeAdapter : HomeAdapter by lazy {
        HomeAdapter(context=requireActivity(), listener=this, threadDB=threadDb)
    }

    private val globalSearchAdapter=GlobalSearchAdapter { model ->
    }

    //New Line
    private lateinit var adapter : NavigationRVAdapter

    private var items=arrayListOf(
        NavigationItemModel(R.drawable.ic_settings_outline, "Settings", 0),
        NavigationItemModel(R.drawable.ic_notification_outline, "Notification", 0),
        NavigationItemModel(R.drawable.ic_msg_rqst_outline, "Message Requests", 0),
        NavigationItemModel(R.drawable.ic_recovery_seed_outline, "Recovery Seed", 0),
        NavigationItemModel(R.drawable.ic_wallet_outline, "Wallet", R.drawable.ic_beta),
        NavigationItemModel(R.drawable.ic_report_issue_outline, "Report Issue", 0),
        NavigationItemModel(R.drawable.ic_help_outline, "Help", 0),
        NavigationItemModel(R.drawable.ic_invite_outline, "Invite", 0),
        NavigationItemModel(R.drawable.ic_about_outline, "About", 0)
    )
    private val hexEncodedPublicKey : String
        get() {
            return TextSecurePreferences.getLocalNumber(requireActivity().applicationContext)!!
        }

    private val searchResultLauncher=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.extras?.parcelable<SearchActivityResults>(SearchActivity.EXTRA_SEARCH_DATA)
                    ?.let { result ->
                        when (result) {
                            is SearchActivityResults.Contact -> {
                                passGlobalSearchAdapterModelContactValue(result.address)
                            }

                            is SearchActivityResults.GroupConversation -> {
                                val groupAddress=Address.fromSerialized(result.groupEncodedId)
                                val threadId=threadDb.getThreadIdIfExistsFor(
                                    Recipient.from(
                                        requireActivity().applicationContext,
                                        groupAddress,
                                        false
                                    )
                                )
                                if (threadId >= 0) {
                                    passGlobalSearchAdapterModelGroupConversationValue(threadId)
                                }
                            }

                            is SearchActivityResults.Message -> {
                                passGlobalSearchAdapterModelMessageValue(
                                    result.threadId,
                                    result.timeStamp,
                                    result.author
                                )
                            }

                            is SearchActivityResults.SavedMessage -> {
                                passGlobalSearchAdapterModelSavedMessagesValue(result.address)
                            }

                            else -> {}
                        }
                    }
            }
        }

    private var mContext : Context?=null
    var activityCallback : HomeFragmentListener?=null

    private val swipeController=SwipeController(
        object : SwipeControllerActions {
            override fun onRightClicked(position : Int) {
            }

            override fun onLeftClicked(position : Int) {

            }
        })

    private val swipeHelper=
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView : RecyclerView,
                viewHolder : RecyclerView.ViewHolder,
                target : RecyclerView.ViewHolder
            )=true

            override fun onSwiped(viewHolder : RecyclerView.ViewHolder, direction : Int) {
                val position=viewHolder.adapterPosition
                val thread=homeAdapter.data[position]
                deleteConversation(thread)
            }

            override fun onChildDraw(
                c : Canvas,
                recyclerView : RecyclerView,
                viewHolder : RecyclerView.ViewHolder,
                dX : Float,
                dY : Float,
                actionState : Int,
                isCurrentlyActive : Boolean
            ) {
                val width=Resources.getSystem().displayMetrics.widthPixels
                val deleteIcon=ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_24, null)
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
                val textMargin=resources.getDimension(R.dimen.fab_margin).roundToInt()
                deleteIcon ?: return
                val top=
                    viewHolder.itemView.top + (viewHolder.itemView.bottom - viewHolder.itemView.top) / 2 - deleteIcon.intrinsicHeight / 2
                deleteIcon.bounds=Rect(
                    width - textMargin - deleteIcon.intrinsicWidth,
                    top,
                    width - textMargin,
                    top + deleteIcon.intrinsicHeight
                )
                if (dX < 0) deleteIcon.draw(c)
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        })

    override fun onAttach(context : Context) {
        super.onAttach(context)
        this.mContext=context
        if (context is HomeFragmentListener) {
            activityCallback=context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    private val broadcastReceivers=mutableListOf<BroadcastReceiver>()

    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?,
        savedInstanceState : Bundle?
    ) : View {
        binding=FragmentHomeBinding.inflate(inflater, container, false)
        // Set custom toolbar
        (activity as HomeActivity).setSupportActionBar(binding.toolbar)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //New Line
        viewModel=ViewModelProvider(requireActivity())[CallViewModel::class.java]
        archiveChatViewModel=ViewModelProvider(requireActivity())[ArchiveChatViewModel::class.java]

        // Set up Glide
        glide=Glide.with(this)
        // Set up toolbar buttons
        binding.profileButton.root.glide=glide
        //New Line
        // Setup Recyclerview's Layout
        binding.navigationMenu.navigationRv.layoutManager=
            LinearLayoutManager(requireActivity().applicationContext)
        binding.navigationMenu.navigationRv.setHasFixedSize(true)
        updateAdapter(0)
        // Add Item Touch Listener
        binding.navigationMenu.navigationRv.addOnItemTouchListener(
            RecyclerTouchListener(
                requireActivity().applicationContext,
                object :
                    ClickListener {
                    override fun onClick(view : View, position : Int) {
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
                                // # My Wallet Activity
                                if (CheckOnline.isOnline(requireActivity().applicationContext)) {
                                    if (TextSecurePreferences.isWalletActive(requireContext())) {
                                        openMyWallet()
                                    } else {
                                        openStartWalletInfo()
                                    }
                                } else {
                                    Toast.makeText(
                                        requireActivity().applicationContext,
                                        getString(R.string.please_check_your_internet_connection),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            5 -> {
                                // # Support
                                activityCallback?.sendMessageToSupport()
                                binding.drawerLayout.closeDrawer(GravityCompat.END)
                            }

                            6 -> {
                                // # Help Activity
                                help()
                            }

                            7 -> {
                                // # Invite Activity
                                sendInvitation(hexEncodedPublicKey)
                            }

                            8 -> {
                                // # About Activity
                                showAbout()
                            }
                        }
                        // Don't highlight the 'Profile' and 'Like us on Facebook' item row
                        if (position != 5 && position != 3) {
                            updateAdapter(position)
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.drawerLayout.closeDrawer(GravityCompat.END)
                        }, 200)
                    }
                })
        )
        binding.profileButton.root.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
        binding.navigationMenu.drawerCloseIcon.setOnClickListener {
            binding.drawerLayout.closeDrawer(
                GravityCompat.END
            )
        }
        val activeUiMode=UiModeUtilities.getUserSelectedUiMode(requireActivity())
        binding.navigationMenu.drawerAppearanceToggleButton.isChecked=activeUiMode == UiMode.NIGHT

        binding.navigationMenu.drawerAppearanceToggleButton.setOnClickListener {
            if (binding.navigationMenu.drawerAppearanceToggleButton.isChecked) {
                val uiMode=UiMode.entries[1]
                UiModeUtilities.setUserSelectedUiMode(requireActivity(), uiMode)
            } else {
                val uiMode=UiMode.entries[0]
                UiModeUtilities.setUserSelectedUiMode(requireActivity(), uiMode)
            }
        }
        binding.navigationMenu.drawerAppearanceToggleButton.setOnTouchListener { _, event ->
            event.actionMasked == MotionEvent.ACTION_MOVE
        }
        binding.navigationMenu.profileContainer.setOnClickListener {
            openSettings()
        }
        binding.navigationMenu.drawerProfileIcon.root.setOnClickListener {
            openSettings()
        }
        binding.navigationMenu.drawerProfileIcon.root.glide=glide
        binding.navigationMenu.drawerProfileIcon.root.isClickable=true
        binding.navigationMenu.drawerProfileId.text=String.format(
            requireContext().resources.getString(R.string.id_format),
            hexEncodedPublicKey
        )

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
        homeAdapter.glide=glide
        binding.recyclerView.adapter=homeAdapter
        swipeHelper.attachToRecyclerView(binding.recyclerView)
        val itemDecorator=RecyclerViewDivider(
            requireContext(),
            R.drawable.ic_divider, 0,
            0
        )
        binding.recyclerView.addItemDecoration(itemDecorator)
        // Set up empty state view
        binding.createNewPrivateChatButton.setOnClickListener { openNewConversationChat() }
        homeViewModel.getObservable(requireActivity().applicationContext)
            .observe(requireActivity()) { newData ->
                val manager=binding.recyclerView.layoutManager as LinearLayoutManager
                val firstPos = manager.findFirstVisibleItemPosition()
                val offsetTop = if (firstPos >= 0) {
                    manager.findViewByPosition(firstPos)?.let { view ->
                        view.top - manager.paddingTop
                    } ?: 0
                } else 0
                val messageRequestCount=threadDb.unapprovedConversationCount
                var request=emptyList<ThreadRecord>()
                if (messageRequestCount > 0 && !TextSecurePreferences.hasHiddenMessageRequests(
                        requireContext()
                    )
                ) {
                    threadDb.unapprovedConversationList.use { openCursor ->
                        val reader=threadDb.readerFor(openCursor)
                        val threads=mutableListOf<ThreadRecord>()
                        while (true) {
                            threads+=reader.next ?: break
                        }
                        threads.sortedByDescending { it.dateReceived }
                        request=threads
                    }
                }
                binding.requests.setContent {
                    BChatTheme {
                        MessageRequestsView(
                            requests=request,
                            openSearch={
                                Intent(requireContext(), SearchActivity::class.java).also {
                                    searchResultLauncher.launch(it)
                                }
                            },
                            ignoreRequest={
                                val dialog=ConversationActionDialog()
                                dialog.apply {
                                    arguments=Bundle().apply {
                                        putSerializable(
                                            ConversationActionDialog.EXTRA_THREAD_RECORD,
                                            it
                                        )
                                        putSerializable(
                                            ConversationActionDialog.EXTRA_DIALOG_TYPE,
                                            HomeDialogType.IgnoreRequest
                                        )
                                    }
                                    setListener(this@HomeFragment)
                                }
                                dialog.show(childFragmentManager, ConversationActionDialog.TAG)
                            },
                            openChat={
                                onConversationClick(it.threadId)
                            },
                            modifier=Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal=16.dp
                                )
                        )
                    }
                }

                if (threadDb.archivedConversationList.count != 0) {
                    binding.archiveChatCardView.visibility=View.VISIBLE
                    binding.archiveChatDivider.visibility=View.VISIBLE
                    binding.archiveChatCardView.setContent {
                        BChatTheme {
                            ArchiveChatView(
                                archiveChatViewModel=archiveChatViewModel!!,
                                threadDatabase=threadDb,
                                onRequestClick={
                                    showArchiveChats()
                                },
                                context=requireContext()
                            )
                        }
                    }
                } else {
                    binding.archiveChatCardView.visibility=View.GONE
                    binding.archiveChatDivider.visibility=View.GONE
                }
                homeAdapter.data=newData
                if (firstPos >= 0) {
                    manager.scrollToPositionWithOffset(firstPos, offsetTop)
                }
                //setupMessageRequestsBanner()
                updateEmptyState()
            }
        ApplicationContext.getInstance(requireActivity()).typingStatusRepository.typingThreads.observe(
            requireActivity()
        ) { threadIds ->
            homeAdapter.typingThreadIDs=(threadIds ?: setOf())
        }
        homeViewModel.tryUpdateChannel()
        // Set up new conversation button set
        val broadcastReceiver=object : BroadcastReceiver() {
            override fun onReceive(context : Context, intent : Intent) {
                binding.recyclerView.adapter!!.notifyDataSetChanged()
            }
        }
        this.broadcastReceiver=broadcastReceiver
        LocalBroadcastManager.getInstance(requireActivity().applicationContext)
            .registerReceiver(broadcastReceiver, IntentFilter("blockedContactsChanged"))
        //PathStatus
        registerObservers()
        activityCallback?.callLifeCycleScope(
            binding.recyclerView,
            mmsSmsDatabase,
            globalSearchAdapter,
            publicKey,
            binding.profileButton.root,
            binding.navigationMenu.drawerProfileName,
            binding.navigationMenu.drawerProfileIcon.root
        )
        binding.chatButtons.setContent {
            BChatTheme {
                NewChatButtons(
                    openNewConversationChat={
                        openNewConversationChat()
                    },
                )
            }
        }

        binding.navigationMenu.menuContainer.run {
            post {
                val params=layoutParams as DrawerLayout.LayoutParams
                params.width=(getScreenWidth() * 0.7).toInt()
                layoutParams=params
                translationX=-(requireContext().toPx(16))
            }
        }
        binding.navigationMenu.version.text=
            resources.getString(R.string.version_name).format(BuildConfig.VERSION_NAME)
        // binding.navigationMenu.uiMode.text = "Dark Mode"

    }

    private fun showArchiveChats() {
        Intent(requireContext(), MyAccountActivity::class.java).also {
            it.putExtra(
                MyAccountActivity.extraStartDestination,
                MyAccountScreens.ArchiveChatScreen.route
            )
            resultLauncher.launch(it)
        }
    }

    private fun callShowQrCode() {
        showQRCode()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }, 200)
    }

    private fun registerObservers() {
        val buildingPathsReceiver : BroadcastReceiver=object : BroadcastReceiver() {

            override fun onReceive(context : Context, intent : Intent) {
                handleBuildingPathsEvent()
            }
        }
        broadcastReceivers.add(buildingPathsReceiver)
        LocalBroadcastManager.getInstance(requireActivity().applicationContext)
            .registerReceiver(buildingPathsReceiver, IntentFilter("buildingPaths"))
        val pathsBuiltReceiver : BroadcastReceiver=object : BroadcastReceiver() {

            override fun onReceive(context : Context, intent : Intent) {
                handlePathsBuiltEvent()
            }
        }
        broadcastReceivers.add(pathsBuiltReceiver)
        LocalBroadcastManager.getInstance(requireActivity().applicationContext)
            .registerReceiver(pathsBuiltReceiver, IntentFilter("pathsBuilt"))
    }

    private fun handleBuildingPathsEvent() {
        update()
    }

    private fun handlePathsBuiltEvent() {
        update()
    }

    private fun update() {
        binding.hopsWarningLayout.visibility=when {
            OnionRequestAPI.paths.isNotEmpty() -> View.GONE
            OnionRequestAPI.paths.isEmpty() && CheckOnline.isOnline(requireActivity().applicationContext) -> View.GONE
            else -> View.VISIBLE
        }
    }

    override fun hideMessageRequests() {
        val dialog=AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_New)
            .setTitle("Hide message requests?")
            .setMessage("Once they are hidden, you can access them from Settings > Message Requests")
            .setPositiveButton(R.string.yes) { _, _ ->
                (activity as HomeActivity).textSecurePreferences.setHasHiddenMessageRequests()
                homeViewModel.tryUpdateChannel()
            }
            .setNegativeButton(R.string.no) { _, _ ->
                // Do nothing
            }.show()

        //SteveJosephh21
        val message : TextView=dialog.findViewById(android.R.id.message)
        val messageFace : Typeface=
            Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        message.typeface=messageFace
    }

    private fun setupCallActionBar() {

        val startTimeNew=viewModel!!.callStartTime
        if (startTimeNew == -1L) {
            binding.toolbarCall.isVisible=false
        } else {
            binding.toolbarCall.isVisible=true
            uiJob=lifecycleScope.launch {
                launch {
                    while (isActive) {
                        val startTime=viewModel!!.callStartTime
                        if (startTime == -1L) {
                            binding.toolbarCall.isVisible=false
                        } else {
                            binding.toolbarCall.isVisible=true
                            binding.callDurationCall.text=DurationFormatUtils.formatDuration(
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
            requireActivity().applicationContext.startService(
                WebRtcCallService.hangupIntent(
                    requireActivity().applicationContext
                )
            )
            binding.toolbarCall.isVisible=false
            Toast.makeText(requireActivity().applicationContext, "Call ended", Toast.LENGTH_SHORT)
                .show()
        }
        binding.toolbarCall.setOnClickListener {
            toolBarCall()
        }
    }

    //New Line
    private fun updateAdapter(highlightItemPos : Int) {
        adapter=NavigationRVAdapter(items, highlightItemPos)
        binding.navigationMenu.navigationRv.adapter=adapter
        adapter.notifyDataSetChanged()
    }


    private fun setupHeaderImage() {
        val isDayUiMode=UiModeUtilities.isDayUiMode(requireActivity())
        val headerTint=if (isDayUiMode) R.color.black else R.color.white
        binding.bchatHeaderImage.setTextColor(
            getColor(
                requireActivity().applicationContext,
                headerTint
            )
        )
    }

    override fun onInputFocusChanged(hasFocus : Boolean) {
//        if (hasFocus) {
//            setSearchShown(true)
//        } else {
//            setSearchShown(!binding.globalSearchInputLayout.query.value.isNullOrEmpty())
//        }
    }


    override fun onResume() {
        super.onResume()
        setupCallActionBar()
        if (TextSecurePreferences.isWalletActive(requireContext())) {
            if (TextSecurePreferences.getRefreshDynamicNodesStatus(requireContext())) {
                val async=
                    DownloadNodeListFileInHomeScreenAsyncTask(requireActivity().applicationContext)
                async.execute<String>(NodeListConstants.downloadNodeListUrl)
            } else {
                pingSelectedNode(false)
            }
        }
        ApplicationContext.getInstance(requireActivity().applicationContext).messageNotifier.setHomeScreenVisible(
            false
        )
        if (TextSecurePreferences.getLocalNumber(requireActivity().applicationContext) == null) {
            return; } // This can be the case after a secondary device is auto-cleared
        IdentityKeyUtil.checkUpdate(requireActivity().applicationContext)
        binding.profileButton.root.recycle() // clear cached image before update tje profilePictureView
        binding.profileButton.root.update(TextSecurePreferences.getProfileName(requireActivity().applicationContext))

        //New Line
        binding.navigationMenu.drawerProfileIcon.root.recycle()
        binding.navigationMenu.drawerProfileIcon.root.update(
            TextSecurePreferences.getProfileName(
                requireActivity().applicationContext
            )
        )

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
        if (arguments?.getBoolean(ConversationFragmentV2.SHORTCUT_LAUNCHER, false) == true) {
            arguments?.remove(ConversationFragmentV2.SHORTCUT_LAUNCHER)
            callConversationScreen(
                requireArguments().getLong(ConversationFragmentV2.THREAD_ID, -1L),
                requireArguments().parcelable(ConversationFragmentV2.ADDRESS),
                requireArguments().parcelable(ConversationFragmentV2.URI),
                requireArguments().getString(ConversationFragmentV2.TYPE),
                requireArguments().getCharSequence(Intent.EXTRA_TEXT)
            )
        }
        if (!TextSecurePreferences.isCopiedSeed(requireActivity().applicationContext)) {
            showSaveYourSeedDialog()
        }
    }

    private fun showSaveYourSeedDialog() {
        try {
            activityCallback?.let {
                SaveYourSeedDialogBox(
                    showSeed={
                        showSeed()
                    }
                ).show(requireActivity().supportFragmentManager, "")
            }
        } catch (exception : Exception) {
            Timber.tag("Beldex").d("Save your seed dialog box exception $exception")
        }
    }

    override fun onPause() {
        super.onPause()
        val dialog=childFragmentManager.findFragmentByTag(ConversationActionDialog.TAG)
        if (dialog is DialogFragment) {
            dialog.dismissAllowingStateLoss()
        }
        val bottomSheet=childFragmentManager.findFragmentByTag(UserDetailsBottomSheet.TAG)
        if (bottomSheet is UserDetailsBottomSheet) {
            bottomSheet.dismissAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        val broadcastReceiver=this.broadcastReceiver
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(requireActivity().applicationContext)
                .unregisterReceiver(broadcastReceiver)
        }
        //PathStatus
        for (receiver in broadcastReceivers) {
            LocalBroadcastManager.getInstance(requireActivity().applicationContext)
                .unregisterReceiver(receiver)
        }
        super.onDestroy()

    }

    private fun updateEmptyState() {
        val threadCount=(binding.recyclerView.adapter)!!.itemCount
        binding.emptyStateContainer.isVisible=
            threadCount == 0 && binding.recyclerView.isVisible && threadDb.archivedConversationList.count == 0
        binding.emptyStateContainerText.isVisible=
            threadCount == 0 && binding.recyclerView.isVisible && threadDb.archivedConversationList.count == 0
        val activity=activity
        if (isAdded && activity != null) {
            val isDayUiMode=UiModeUtilities.isDayUiMode(requireActivity())
            (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
                binding.emptyStateImageView.setImageResource(
                    it
                )
            }
        }
    }

    fun updateAdapter() {
        homeAdapter.notifyDataSetChanged()
    }

    fun updateProfileButton() {
        binding.profileButton.root.publicKey=publicKey
        binding.profileButton.root.displayName=
            TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.profileButton.root.recycle()
        binding.profileButton.root.update(TextSecurePreferences.getProfileName(requireActivity().applicationContext))

        //New Line
        binding.navigationMenu.drawerProfileName.text=
            TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.navigationMenu.drawerProfileIcon.root.publicKey=publicKey
        binding.navigationMenu.drawerProfileIcon.root.displayName=
            TextSecurePreferences.getProfileName(requireActivity().applicationContext)
        binding.navigationMenu.drawerProfileIcon.root.recycle()
        binding.navigationMenu.drawerProfileIcon.root.update(
            TextSecurePreferences.getProfileName(
                requireActivity().applicationContext
            )
        )
    }

    fun onBackPressed() {
//        if (binding.globalSearchRecycler.isVisible) {
//            binding.globalSearchInputLayout.clearSearch(true)
//        }
    }


    override fun onConversationClick(thread : ThreadRecord) {
        onConversationClick(thread.threadId)
    }

    override fun onLongConversationClick(thread : ThreadRecord, view : View, position : Int) {
        val recipient=thread.recipient
        val popupMenu=PopupMenu(requireContext(), view, R.style.PopupMenu)
        popupMenu.menuInflater.inflate(R.menu.menu_conversation_v2, popupMenu.menu)
        popupMenu.gravity=Gravity.END
        popupMenu.setForceShowIcon(true)
        val item : MenuItem=popupMenu.menu.findItem(R.id.menu_delete)
        val s=SpannableString("Delete")
        s.setSpan(ForegroundColorSpan(requireContext().getColor(R.color.red)), 0, s.length, 0)
        item.setTitle(s)
        with(popupMenu.menu) {
            if (recipient.isGroupRecipient && !recipient.isLocalNumber) {
                findItem(R.id.menu_details).setVisible(false)
                findItem(R.id.menu_unblock).setVisible(false)
                findItem(R.id.menu_block).setVisible(false)
            } else if (recipient.isLocalNumber) {
                findItem(R.id.menu_details).setVisible(false)
                findItem(R.id.menu_unblock).setVisible(false)
                findItem(R.id.menu_block).setVisible(false)
            } else {
                findItem(R.id.menu_details).setVisible(true)
                findItem(R.id.menu_unblock).setVisible(recipient.isBlocked)
                findItem(R.id.menu_block).setVisible(!recipient.isBlocked)
            }

            findItem(R.id.menu_unmute_notifications).setVisible(recipient.isMuted && !recipient.isLocalNumber)
            findItem(R.id.menu_mute_notifications).setVisible(!recipient.isMuted && !recipient.isLocalNumber)
            findItem(R.id.menu_notification_settings).setVisible(
                recipient.isGroupRecipient && !recipient.isMuted && isSecretGroupIsActive(
                    recipient
                )
            )
            findItem(R.id.menu_mark_read).setVisible(thread.unreadCount > 0)
            findItem(R.id.menu_pin).setVisible(!thread.isPinned)
            findItem(R.id.menu_unpin).setVisible(thread.isPinned)
            findItem(R.id.menu_archive_chat).setVisible(true)
        }
        popupMenu.setOnMenuItemClickListener {
            handlePopUpMenuClickListener(it, thread, position)
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

    private fun isSecretGroupIsActive(recipient : Recipient) : Boolean {
        return if (recipient.isClosedGroupRecipient) {
            val group=getGroup(recipient)
            val isActive=(group?.isActive == true)
            isActive
        } else {
            true
        }
    }

    fun getGroup(recipient : Recipient) : GroupRecord?=
        groupDb.getGroup(recipient.address.toGroupString()).orNull()

    private fun handlePopUpMenuClickListener(
        item : MenuItem,
        thread : ThreadRecord,
        position : Int
    ) {
        when (item.itemId) {
            R.id.menu_details -> {
                val userDetailsBottomSheet=UserDetailsBottomSheet()
                val bundle=bundleOf(
                    UserDetailsBottomSheet.ARGUMENT_PUBLIC_KEY to thread.recipient.address.toString(),
                    UserDetailsBottomSheet.ARGUMENT_THREAD_ID to thread.threadId
                )
                userDetailsBottomSheet.arguments=bundle
                userDetailsBottomSheet.show(childFragmentManager, UserDetailsBottomSheet.TAG)
            }

            R.id.menu_pin -> {
                setConversationPinned(thread.threadId, true)
            }

            R.id.menu_unpin -> {
                setConversationPinned(thread.threadId, false)
            }

            R.id.menu_block -> {
                if (!thread.recipient.isBlocked) {
                    blockConversation(thread, position)
                }
            }

            R.id.menu_unblock -> {
                if (thread.recipient.isBlocked) {
                    unblockConversation(thread, position)
                }
            }

            R.id.menu_mute_notifications -> {
                setConversationMuted(thread, true, position)
            }

            R.id.menu_unmute_notifications -> {
                setConversationMuted(thread, false, position)
            }

            R.id.menu_notification_settings -> {
                val dialog=ConversationActionDialog()
                dialog.apply {
                    arguments=Bundle().apply {
                        putInt(
                            ConversationActionDialog.EXTRA_ARGUMENT_3,
                            thread.recipient.notifyType
                        )
                        putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                        putSerializable(
                            ConversationActionDialog.EXTRA_DIALOG_TYPE,
                            HomeDialogType.NotificationSettings
                        )
                    }
                    setListener(this@HomeFragment)
                }
                dialog.show(childFragmentManager, ConversationActionDialog.TAG)
            }

            R.id.menu_mark_read -> {
                markAllAsRead(thread)
            }

            R.id.menu_delete -> {
                deleteConversation(thread)
            }

            R.id.menu_archive_chat -> {
                archiveChatViewModel?.let { archiveConversation(thread, it) }
            }

            else -> Unit
        }
    }

    private fun blockConversation(thread : ThreadRecord, position : Int) {
        val blockDialog=ConversationActionDialog()
        blockDialog.apply {
            arguments=Bundle().apply {
                putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.BlockUser
                )
                putInt(ConversationActionDialog.EXTRA_THREAD_POSITION, position)
            }
            setListener(this@HomeFragment)
        }
        blockDialog.show(childFragmentManager, ConversationActionDialog.TAG)
    }

    private fun unblockConversation(thread : ThreadRecord, position : Int) {
        val unBlockDialog=ConversationActionDialog()
        unBlockDialog.apply {
            arguments=Bundle().apply {
                putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.UnblockUser
                )
                putInt(ConversationActionDialog.EXTRA_THREAD_POSITION, position)
            }
            setListener(this@HomeFragment)
        }
        unBlockDialog.show(childFragmentManager, ConversationActionDialog.TAG)
    }

    private fun setConversationMuted(thread : ThreadRecord, isMuted : Boolean, position : Int) {
        if (!isMuted) {
            lifecycleScope.launch(Dispatchers.IO) {
                recipientDatabase.setMuted(thread.recipient, 0)
                withContext(Dispatchers.Main) {
                    binding.recyclerView.adapter!!.notifyItemChanged(position)
                }
            }
        } else {
            val dialog=ConversationActionDialog()
            dialog.apply {
                arguments=Bundle().apply {
                    putSerializable(
                        ConversationActionDialog.EXTRA_ARGUMENT_3,
                        thread.recipient.mutedUntil
                    )
                    putSerializable(
                        ConversationActionDialog.EXTRA_DIALOG_TYPE,
                        HomeDialogType.MuteChat
                    )
                    putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                    putInt(ConversationActionDialog.EXTRA_THREAD_POSITION, position)
                }
                setListener(this@HomeFragment)
            }
            dialog.show(childFragmentManager, ConversationActionDialog.TAG)
        }
    }

    private fun setNotifyType(thread : ThreadRecord, newNotifyType : Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            recipientDatabase.setNotifyType(thread.recipient, newNotifyType)
            withContext(Dispatchers.Main) {
                binding.recyclerView.adapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun setConversationPinned(
        threadId : Long,
        pinned : Boolean
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            threadDb.setPinned(threadId, pinned)
            homeViewModel.tryUpdateChannel()
        }
    }

    private fun markAllAsRead(thread : ThreadRecord) {
        ThreadUtils.queue {
            threadDb.markAllAsRead(thread.threadId, thread.recipient.isOpenGroupRecipient)
        }
    }

    private fun deleteConversation(thread : ThreadRecord) {
        val recipient=thread.recipient
        val message=if (recipient.isGroupRecipient) {
            val group=groupDb.getGroup(recipient.address.toString()).orNull()
            if (group != null && group.admins.map { it.toString() }
                    .contains(TextSecurePreferences.getLocalNumber(requireActivity()))) {
                "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
            } else {
                resources.getString(R.string.activity_home_leave_group_dialog_message)
            }
        } else {
            resources.getString(R.string.activity_home_delete_conversation_dialog_message)
        }
        val deleteConversation=ConversationActionDialog()
        deleteConversation.apply {
            arguments=Bundle().apply {
                putSerializable(ConversationActionDialog.EXTRA_THREAD_RECORD, thread)
                putString(ConversationActionDialog.EXTRA_ARGUMENT_1, message)
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.DeleteChat
                )
            }
            setListener(this@HomeFragment)
        }
        deleteConversation.show(childFragmentManager, ConversationActionDialog.TAG)
    }

    private fun archiveConversation(
        thread : ThreadRecord,
        archiveChatViewModel : ArchiveChatViewModel
    ) {
        val threadID=thread.threadId
        val context=requireActivity() as Context
        DatabaseComponent.get(context).bchatJobDatabase()
            .cancelPendingMessageSendJobs(threadID)
        lifecycleScope.launch(Dispatchers.IO) {
            threadDb.setThreadArchived(threadID)
            archiveChatViewModel.updateArchiveChatCount(threadDb.archivedConversationList.count)
        }
    }

    interface HomeFragmentListener {
        fun callLifeCycleScope(
            recyclerView : RecyclerView,
            mmsSmsDatabase : MmsSmsDatabase,
            globalSearchAdapter : GlobalSearchAdapter,
            publicKey : String,
            profileButton : ProfilePictureView,
            drawerProfileName : TextView,
            drawerProfileIcon : ProfilePictureView
        )

        fun sendMessageToSupport()

        //Node Connection
        fun getFavouriteNodes() : MutableSet<NodeInfo>
        fun getOrPopulateFavouritesRemoteNodeList(
            context : Context,
            storeNodes : Boolean
        ) : MutableSet<NodeInfo>

        fun getNode() : NodeInfo?
        fun setNode(node : NodeInfo?)

        //Wallet
        fun hasBoundService() : Boolean
        val connectionStatus : Wallet.ConnectionStatus?
    }

    private fun pingSelectedNode(storeNodes : Boolean) {
        val pingSelected=0
        val findBest=1
        AsyncFindBestNode(pingSelected, findBest, storeNodes).execute<Int>(pingSelected)
    }

    inner class AsyncFindBestNode(
        private val pingSelected : Int,
        private val findBest : Int,
        private val storeNodes : Boolean
    ) :
        AsyncTaskCoroutine<Int?, NodeInfo?>() {

        override fun doInBackground(vararg params : Int?) : NodeInfo? {
            if (isAdded) {
                val favourites : Set<NodeInfo?> =
                    activityCallback!!.getOrPopulateFavouritesRemoteNodeList(
                        requireActivity(),
                        storeNodes
                    )
                var selectedNode : NodeInfo?
                if (params[0] == findBest) {
                    selectedNode=autoselect(favourites)
                } else if (params[0] == pingSelected) {
                    selectedNode=activityCallback!!.getNode()
                    if (selectedNode == null) {
                        for (node in favourites) {
                            if (node!!.isSelected) {
                                selectedNode=node
                                break
                            }
                        }
                    }
                    if (selectedNode == null) { // autoselect
                        selectedNode=autoselect(favourites)
                    } else {
                        //Steve Josephh21
                        if (selectedNode != null) {
                            selectedNode.testRpcService()
                        }
                    }
                } else throw IllegalStateException()
                return if (selectedNode != null && selectedNode.isValid) {
                    activityCallback!!.setNode(selectedNode)
                    selectedNode
                } else {
                    selectedNode=autoselect(favourites)
                    activityCallback!!.setNode(selectedNode)
                    selectedNode
                }
            } else {
                activityCallback!!.setNode(null)
                return null
            }
        }

        override fun onPostExecute(result : NodeInfo?) {
            Log.d("Beldex", "daemon connected to  ${result?.host}")
        }

    }

    inner class DownloadNodeListFileInHomeScreenAsyncTask(private val mContext : Context) :
        AsyncTaskCoroutine<String?, String?>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg downloadUrl : String?) : String? {
            var input : InputStream?=null
            var output : OutputStream?=null
            var connection : HttpURLConnection?=null
            try {
                val url=URL(downloadUrl[0])
                connection=url.openConnection() as HttpURLConnection
                connection.connect()

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    android.util.Log.d(
                        "Error",
                        "Server returned HTTP  + ${connection.responseCode} \n +${connection.responseMessage}"
                    )
                    return ("Server returned HTTP " + connection.responseCode
                            + " " + connection.responseMessage)
                }

                // download the file
                input=connection.inputStream

                val file=File(mContext.filesDir, "/${NodeListConstants.downloadNodeListFileName}")
                if (file.exists()) {
                    file.delete()
                }
                output=
                    FileOutputStream(mContext.filesDir.toString() + "/${NodeListConstants.downloadNodeListFileName}")
                val data=ByteArray(4096)
                var total : Long=0
                var count : Int
                while (input.read(data).also { count=it } != -1) {
                    // allow canceling with back button
                    if (NonCancellable.isCancelled) {
                        input.close()
                        return null
                    }
                    total+=count.toLong()
                    output.write(data, 0, count)
                }
            } catch (e : Exception) {
                return e.toString()
            } finally {
                try {
                    output?.close()
                    input?.close()
                } catch (ignored : IOException) {
                }
                connection?.disconnect()
            }
            return null
        }

        override fun onPostExecute(result : String?) {
            super.onPostExecute(result)
            TextSecurePreferences.setRefreshDynamicNodesStatus(requireContext(), false)
            pingSelectedNode(true)
        }
    }

    fun autoselect(nodes : Set<NodeInfo?>) : NodeInfo? {
        if (nodes.isEmpty()) return null
        NodePinger.execute(nodes, null)
        val nodeList : ArrayList<NodeInfo?> =ArrayList<NodeInfo?>(nodes)
        Collections.sort(nodeList, NodeInfo.BestNodeComparator)
        val rnd=Random().nextInt(nodeList.size)
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

    private fun sendInvitation(hexEncodedPublicKey : String) {
        val intent=Intent()
        intent.action=Intent.ACTION_SEND
        val invitation=String.format(
            requireContext().resources.getString(R.string.invitation_msg),
            hexEncodedPublicKey
        )
        intent.putExtra(Intent.EXTRA_TEXT, invitation)
        intent.type="text/plain"
        val chooser=
            Intent.createChooser(intent, getString(R.string.activity_settings_invite_button_title))
        startActivity(chooser)
    }

    private fun help() {
        val intent=Intent(Intent.ACTION_SENDTO)
        intent.data=Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@beldex.io"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        startActivity(intent)
    }

    private fun showSeed() {
//        Intent(requireContext(), SeedPermissionActivity::class.java).also {
//            show(it)
//        }
        Intent(activity, MyAccountActivity::class.java).also {
            it.putExtra(
                MyAccountActivity.extraStartDestination,
                MyAccountScreens.RecoverySeedScreen.route
            )
            startActivity(it)
        }
    }

    private fun callAppPermission() {
        val intent=Intent()
        intent.action=Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri=Uri.fromParts("package", requireActivity().packageName, null)
        intent.data=uri
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

    private fun onConversationClick(threadId : Long) {
        val extras=Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID, threadId)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    private fun openSettings() {
        val activity=activity
        if (isAdded && activity != null) {
            Intent(activity, MyAccountActivity::class.java).also {
                it.putExtra(
                    MyAccountActivity.extraStartDestination,
                    MyAccountScreens.SettingsScreen.route
                )
                startActivity(it)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }, 200)
    }

    private var callSettingsActivityResultLauncher=registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras=Bundle()
            extras.putParcelable(
                ConversationFragmentV2.ADDRESS,
                result.data!!.parcelable(ConversationFragmentV2.ADDRESS)
            )
            extras.putLong(
                ConversationFragmentV2.THREAD_ID,
                result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID, -1)
            )
            extras.putParcelable(
                ConversationFragmentV2.URI,
                result.data!!.parcelable(ConversationFragmentV2.URI)
            )
            extras.putString(
                ConversationFragmentV2.TYPE,
                result.data!!.getStringExtra(ConversationFragmentV2.TYPE)
            )
            replaceFragment(ConversationFragmentV2(), null, extras)
        } else {
            homeAdapter.notifyDataSetChanged()
        }
    }

    private fun openMyWallet() {
        val walletName=TextSecurePreferences.getWalletName(requireContext())
        val walletPassword=TextSecurePreferences.getWalletPassword(requireContext())
        if (walletName != null && walletPassword != null) {
            //startWallet(walletName, walletPassword, fingerprintUsed = false, streetmode = false)
            val lockManager : LockManager<CustomPinActivity> =
                LockManager.getInstance() as LockManager<CustomPinActivity>
            lockManager.enableAppLock(requireContext(), CustomPinActivity::class.java)
            Intent(requireContext(), CustomPinActivity::class.java).also {
                if (TextSecurePreferences.getWalletEntryPassword(requireContext()) != null) {
                    it.putExtra(EXTRA_PIN_CODE_ACTION, PinCodeAction.VerifyWalletPin.action)
                    it.putExtra("send_authentication", false)
                    customPinActivityResultLauncher.launch(it)
                } else {
                    it.putExtra(EXTRA_PIN_CODE_ACTION, PinCodeAction.CreateWalletPin.action)
                    it.putExtra("send_authentication", false)
                    customPinActivityResultLauncher.launch(it)
                }
            }
        } else {
            Intent(requireContext(), WalletInfoActivity::class.java).also {
                push(it)
            }
        }
    }

    private fun openStartWalletInfo() {
        /*Intent(requireContext(), StartWalletInfo::class.java).also {
            push(it)
        }*/
        Intent(activity, MyAccountActivity::class.java).also {
            it.putExtra(
                MyAccountActivity.extraStartDestination,
                MyAccountScreens.StartWalletInfoScreen.route
            )
            startActivity(it)
        }
    }

    private var customPinActivityResultLauncher=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        val intent=Intent(activity, MyProfileActivity::class.java)
        intent.putExtra("profile_editable", true)
        startActivity(intent)
    }

    private var showQRCodeWithScanQRCodeActivityResultLauncher=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras=Bundle()
                extras.putParcelable(
                    ConversationFragmentV2.ADDRESS,
                    result.data!!.parcelable(ConversationFragmentV2.ADDRESS)
                )
                extras.putLong(
                    ConversationFragmentV2.THREAD_ID,
                    result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID, -1)
                )
                extras.putParcelable(
                    ConversationFragmentV2.URI,
                    result.data!!.parcelable(ConversationFragmentV2.URI)
                )
                extras.putString(
                    ConversationFragmentV2.TYPE,
                    result.data!!.getStringExtra(ConversationFragmentV2.TYPE)
                )
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
            it.putExtra(
                MyAccountActivity.extraStartDestination,
                MyAccountScreens.MessageRequestsScreen.route
            )
            resultLauncher.launch(it)
        }
    }

    private var resultLauncher=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras=Bundle()
                extras.putLong(
                    ConversationFragmentV2.THREAD_ID,
                    result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID, -1)
                )
                replaceFragment(ConversationFragmentV2(), null, extras)
            }
        }

    override fun openNewConversationChat() {
        val intent=Intent(requireContext(), NewChatConversationActivity::class.java)
        createNewPrivateChatResultLauncher.launch(intent)
    }

    private var createNewPrivateChatResultLauncher=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                when (result.data!!.getIntExtra(ConversationFragmentV2.ACTIVITY_TYPE, 1)) {
                    1 -> { //New Chat
                        val extras=Bundle()
                        extras.putParcelable(
                            ConversationFragmentV2.ADDRESS,
                            result.data!!.parcelable(ConversationFragmentV2.ADDRESS)
                        )
                        extras.putLong(
                            ConversationFragmentV2.THREAD_ID,
                            result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID, -1)
                        )
                        extras.putParcelable(
                            ConversationFragmentV2.URI,
                            result.data!!.parcelable(ConversationFragmentV2.URI)
                        )
                        extras.putString(
                            ConversationFragmentV2.TYPE,
                            result.data!!.getStringExtra(ConversationFragmentV2.TYPE)
                        )
                        extras.putString(
                            ConversationFragmentV2.BNS_NAME,
                            result.data!!.getStringExtra(ConversationFragmentV2.BNS_NAME)
                        )
                        replaceFragment(ConversationFragmentV2(), null, extras)
                    }

                    2 -> { // Secret Group
                        createNewSecretGroup()
                    }

                    3 -> { // Social Group
                        joinSocialGroup()
                    }

                    4 -> { // Note to Self
                        val recipient=Recipient.from(
                            requireContext(),
                            Address.fromSerialized(hexEncodedPublicKey),
                            false
                        )
                        passGlobalSearchAdapterModelContactValue(recipient.address)
                    }

                    5 -> { // Invite a Friend
                        sendInvitation(hexEncodedPublicKey)
                    }

                    6 -> { // Individual Conversation
                        val extras=Bundle()
                        extras.putParcelable(
                            ConversationFragmentV2.ADDRESS,
                            result.data!!.parcelable(ConversationFragmentV2.ADDRESS)
                        )
                        replaceFragment(ConversationFragmentV2(), null, extras)
                    }

                    else -> return@registerForActivityResult
                }
            }
        }

    override fun createNewSecretGroup() {
        val intent=Intent(requireContext(), NewGroupConversationActivity::class.java).apply {
            putExtra(
                NewGroupConversationActivity.EXTRA_DESTINATION,
                NewGroupConversationType.SecretGroup.destination
            )
        }
        createClosedGroupActivityResultLauncher.launch(intent)
    }

    private var createClosedGroupActivityResultLauncher=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras=Bundle()
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
            if (result.resultCode == CreateClosedGroupActivity.closedGroupCreatedResultCode) {
                openNewConversationChat()
            }
        }

    override fun joinSocialGroup() {
        val intent=Intent(requireContext(), NewGroupConversationActivity::class.java).apply {
            putExtra(
                NewGroupConversationActivity.EXTRA_DESTINATION,
                NewGroupConversationType.PublicGroup.destination
            )
        }
        joinPublicChatNewActivityResultLauncher.launch(intent)
    }

    private var joinPublicChatNewActivityResultLauncher=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras=Bundle()
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
        Intent(requireContext(), WebRTCComposeActivity::class.java).also {
            push(it)
        }
    }

    private fun passGlobalSearchAdapterModelSavedMessagesValue(address : Address) {
        val extras=Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS, address)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    private fun passGlobalSearchAdapterModelContactValue(address : Address) {
        val extras=Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS, address)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    private fun passGlobalSearchAdapterModelGroupConversationValue(threadId : Long) {
        val extras=Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID, threadId)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    private fun passGlobalSearchAdapterModelMessageValue(
        threadId : Long,
        timestamp : Long,
        author : Address
    ) {
        val extras=Bundle()
        extras.putLong(ConversationFragmentV2.THREAD_ID, threadId)
        extras.putLong(ConversationFragmentV2.SCROLL_MESSAGE_ID, timestamp)
        extras.putParcelable(ConversationFragmentV2.SCROLL_MESSAGE_AUTHOR, author)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    private fun callConversationScreen(
        threadId : Long,
        address : Address?,
        uri : Uri?,
        type : String?,
        extraText : CharSequence?
    ) {
        val extras=Bundle()
        extras.putParcelable(ConversationFragmentV2.ADDRESS, address)
        extras.putLong(ConversationFragmentV2.THREAD_ID, threadId)
        extras.putParcelable(ConversationFragmentV2.URI, uri)
        extras.putString(ConversationFragmentV2.TYPE, type)
        extras.putCharSequence(Intent.EXTRA_TEXT, extraText)
        replaceFragment(ConversationFragmentV2(), null, extras)
    }

    override fun onConfirm(
        dialogType : HomeDialogType,
        threadRecord : ThreadRecord?,
        position : Int
    ) {
        when (dialogType) {
            HomeDialogType.UnblockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, false)
                        withContext(Dispatchers.Main) {
                            homeAdapter.notifyItemChanged(position)
                        }
                    }
                }
            }

            HomeDialogType.BlockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, true)
                        withContext(Dispatchers.Main) {
                            homeAdapter.notifyItemChanged(position)
                        }
                    }
                }
            }

            HomeDialogType.DeleteChat -> {
                threadRecord?.let {
                    val threadID=it.threadId
                    val recipient=it.recipient
                    lifecycleScope.launch(Dispatchers.Main) {
                        val context=requireActivity() as Context
                        // Cancel any outstanding jobs
                        DatabaseComponent.get(context).bchatJobDatabase()
                            .cancelPendingMessageSendJobs(threadID)
                        // Send a leave group message if this is an active closed group
                        if (recipient.address.isClosedGroup && DatabaseComponent.get(context)
                                .groupDatabase().isActive(recipient.address.toGroupString())
                        ) {
                            var isClosedGroup : Boolean
                            var groupPublicKey : String?
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
                            if (isClosedGroup) {
                                MessageSender.explicitLeave(groupPublicKey!!, false)
                            }
                        }
                        // Delete the conversation
                        val v2OpenGroup=
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
                        val toastMessage=
                            if (recipient.isGroupRecipient) R.string.MessageRecord_left_group else R.string.activity_home_conversation_deleted_message
                        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            HomeDialogType.IgnoreRequest -> {
                threadRecord.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val recipient=threadRecord?.recipient
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
        dialogType : HomeDialogType,
        data : Any?,
        threadRecord : ThreadRecord?,
        position : Int
    ) {
        when (dialogType) {
            HomeDialogType.MuteChat -> {
                val index=data as Int
                val muteUntil=when (index) {
                    1 -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
                    2 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
                    3 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
                    4 -> Long.MAX_VALUE
                    else -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    threadRecord?.let {
                        DatabaseComponent.get(requireContext()).recipientDatabase()
                            .setMuted(it.recipient, muteUntil)
                        withContext(Dispatchers.Main) {
                            homeAdapter.notifyItemChanged(position)
                        }
                    }
                }
            }

            HomeDialogType.NotificationSettings ->                                                       {
                threadRecord?.let {
                    val index=data as Int
                    DatabaseComponent.get(requireActivity()).recipientDatabase()
                        .setNotifyType(it.recipient, index.toString().toInt())
                    homeAdapter.notifyItemChanged(position)
                }
            }

            HomeDialogType.BlockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, true)
                        withContext(Dispatchers.Main) {
                            homeAdapter.notifyItemChanged(position)
                        }
                    }
                }

            }

            HomeDialogType.UnblockUser -> {
                threadRecord?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipientDatabase.setBlocked(it.recipient, false)
                        withContext(Dispatchers.Main) {
                            homeAdapter.notifyItemChanged(position)
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}
//endregion
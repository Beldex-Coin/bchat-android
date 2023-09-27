package com.thoughtcrimes.securesms.conversation.v2

import android.Manifest
import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Pair
import android.util.TypedValue
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.annimon.stream.Stream
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.jobs.AttachmentDownloadJob
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.mentions.Mention
import com.beldex.libbchat.messaging.mentions.MentionsManager
import com.beldex.libbchat.messaging.messages.control.DataExtractionNotification
import com.beldex.libbchat.messaging.messages.control.ExpirationTimerUpdate
import com.beldex.libbchat.messaging.messages.signal.OutgoingMediaMessage
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview
import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.MediaTypes
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.concurrent.SimpleTask
import com.beldex.libbchat.utilities.isScrolledToBottom
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libbchat.utilities.recipients.RecipientModifiedListener
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.ListenableFuture
import com.beldex.libsignal.utilities.guava.Optional
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.ExpirationDialog
import com.thoughtcrimes.securesms.audio.AudioRecorder
import com.thoughtcrimes.securesms.calls.WebRtcCallActivity
import com.thoughtcrimes.securesms.contacts.SelectContactsActivity
import com.thoughtcrimes.securesms.contactshare.SimpleTextWatcher
import com.thoughtcrimes.securesms.conversation.v2.dialogs.BlockedDialog
import com.thoughtcrimes.securesms.conversation.v2.dialogs.LinkPreviewDialog
import com.thoughtcrimes.securesms.conversation.v2.dialogs.SendSeedDialog
import com.thoughtcrimes.securesms.conversation.v2.input_bar.InputBarButton
import com.thoughtcrimes.securesms.conversation.v2.input_bar.InputBarDelegate
import com.thoughtcrimes.securesms.conversation.v2.input_bar.InputBarRecordingViewDelegate
import com.thoughtcrimes.securesms.conversation.v2.input_bar.mentions.MentionCandidatesView
import com.thoughtcrimes.securesms.conversation.v2.menus.ConversationActionModeCallback
import com.thoughtcrimes.securesms.conversation.v2.menus.ConversationActionModeCallbackDelegate
import com.thoughtcrimes.securesms.conversation.v2.menus.ConversationMenuHelper
import com.thoughtcrimes.securesms.conversation.v2.messages.VisibleMessageContentViewDelegate
import com.thoughtcrimes.securesms.conversation.v2.messages.VisibleMessageView
import com.thoughtcrimes.securesms.conversation.v2.search.SearchBottomBar
import com.thoughtcrimes.securesms.conversation.v2.search.SearchViewModel
import com.thoughtcrimes.securesms.conversation.v2.utilities.AttachmentManager
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionManagerUtilities
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionUtilities
import com.thoughtcrimes.securesms.conversation.v2.utilities.ResendMessageUtilities
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.crypto.MnemonicUtilities
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.data.PendingTx
import com.thoughtcrimes.securesms.data.TxData
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.database.model.MmsMessageRecord
import com.thoughtcrimes.securesms.delegates.WalletDelegates
import com.thoughtcrimes.securesms.delegates.WalletDelegatesImpl
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.giph.ui.GiphyActivity
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.home.HomeFragment
import com.thoughtcrimes.securesms.linkpreview.LinkPreviewRepository
import com.thoughtcrimes.securesms.linkpreview.LinkPreviewUtil
import com.thoughtcrimes.securesms.linkpreview.LinkPreviewViewModel
import com.thoughtcrimes.securesms.mediasend.Media
import com.thoughtcrimes.securesms.mediasend.MediaSendActivity
import com.thoughtcrimes.securesms.mms.AudioSlide
import com.thoughtcrimes.securesms.mms.GifSlide
import com.thoughtcrimes.securesms.mms.GlideApp
import com.thoughtcrimes.securesms.mms.ImageSlide
import com.thoughtcrimes.securesms.mms.MediaConstraints
import com.thoughtcrimes.securesms.mms.Slide
import com.thoughtcrimes.securesms.mms.SlideDeck
import com.thoughtcrimes.securesms.mms.VideoSlide
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.permissions.Permissions
import com.thoughtcrimes.securesms.preferences.ChatSettingsActivity
import com.thoughtcrimes.securesms.preferences.PrivacySettingsActivity
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.util.ActivityDispatcher
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities
import com.thoughtcrimes.securesms.util.DateUtils
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.MediaUtil
import com.thoughtcrimes.securesms.util.SaveAttachmentTask
import com.thoughtcrimes.securesms.util.getColorWithID
import com.thoughtcrimes.securesms.util.isValidString
import com.thoughtcrimes.securesms.util.parcelable
import com.thoughtcrimes.securesms.util.slidetoact.SlideToActView
import com.thoughtcrimes.securesms.util.slidetoact.SlideToActView.OnSlideCompleteListener
import com.thoughtcrimes.securesms.util.toPx
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.send.interfaces.SendConfirm
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import com.thoughtcrimes.securesms.webrtc.CallViewModel
import com.thoughtcrimes.securesms.webrtc.NetworkChangeReceiver
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentConversationV2Binding
import io.beldex.bchat.databinding.ViewVisibleMessageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.komponents.kovenant.ui.successUi
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@AndroidEntryPoint
class ConversationFragmentV2 : Fragment(), InputBarDelegate,
    InputBarRecordingViewDelegate, AttachmentManager.AttachmentListener,
    ConversationActionModeCallbackDelegate, VisibleMessageContentViewDelegate,
    RecipientModifiedListener,
    SearchBottomBar.EventListener, LoaderManager.LoaderCallbacks<Cursor>,
    ConversationMenuHelper.ConversationMenuListener, OnBackPressedListener,SendConfirm, WalletDelegates by WalletDelegatesImpl() {

    private var param2: String? = null

    lateinit var binding: FragmentConversationV2Binding

    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val linkPreviewViewModel: LinkPreviewViewModel by lazy {
        ViewModelProvider(
            this, LinkPreviewViewModel.Factory(
                LinkPreviewRepository(requireActivity())
            )
        )[LinkPreviewViewModel::class.java]
    }

//    var threadId: Long? = -1L
    @Inject
    lateinit var threadDb: ThreadDatabase

    private val viewModel: ConversationViewModel by viewModels {
        var threadId = requireArguments().getLong(THREAD_ID,-1L)
        if (threadId == -1L) {
            requireArguments().getParcelable<Address>(ADDRESS)?.let { address ->
                val recipient = Recipient.from(requireActivity(), address, false)
                threadId = threadDb.getOrCreateThreadIdFor(recipient)
            }
        }
        listenerCallback!!.getConversationViewModel().create(threadId!!)
    }

    private fun callViewModel():Recipient?{
         val viewModels: ConversationViewModel by viewModels {
            var threadId = requireArguments().getLong(THREAD_ID,-1L)
            if (threadId == -1L) {
                requireArguments().parcelable<Address>(ADDRESS)?.let { address ->
                    val recipient = Recipient.from(requireActivity(), address, false)
                    threadId = threadDb.getOrCreateThreadIdFor(recipient)
                }
            }
            listenerCallback!!.getConversationViewModel().create(threadId!!)
        }
        return viewModels.recipient.value
    }

    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(requireContext())!!
        }

    private var actionMode: ActionMode? = null

    //Hales63
    private var selectedEvent: MotionEvent? = null
    private var selectedView: VisibleMessageView? = null
    private var selectedMessageRecord: MessageRecord? = null


    private var unreadCount = 0

    // Attachments
    private lateinit var audioRecorder: AudioRecorder
    private val stopAudioHandler = Handler(Looper.getMainLooper())
    private val stopVoiceMessageRecordingTask = Runnable { sendVoiceMessage() }
    private val attachmentManager by lazy {
        AttachmentManager(
            requireActivity(),
            this
        )
    }
    private var isLockViewExpanded = false
    private var isShowingAttachmentOptions = false

    // Mentions
    private val mentions = mutableListOf<Mention>()
    private var mentionCandidatesView: MentionCandidatesView? = null
    private var previousText: CharSequence = ""
    private var currentMentionStartIndex = -1
    private var isShowingMentionCandidatesView = false

    // Search
    var searchViewModel: SearchViewModel? = null
    var searchViewItem: MenuItem? = null


    private val isScrolledToBottom: Boolean
        get() = binding.conversationRecyclerView.isScrolledToBottom

    private val layoutManager: LinearLayoutManager?
        get() { return binding.conversationRecyclerView.layoutManager as LinearLayoutManager? }


    private val seed by lazy {
        var hexEncodedSeed =
            IdentityKeyUtil.retrieve(requireActivity(), IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed =
                IdentityKeyUtil.getIdentityKeyPair(requireActivity()).hexEncodedPrivateKey // Legacy account
        }
        val loadFileContents: (String) -> String = { fileName ->
            MnemonicUtilities.loadFileContents(requireActivity(), fileName)
        }
        MnemonicCodec(loadFileContents).encode(
            hexEncodedSeed!!,
            MnemonicCodec.Language.Configuration.english
        )
    }

    /*Hales63*/
    private val adapter by lazy {
        val cursor = viewModel.getConversationsCursor()
        val adapter = ConversationAdapter(
            requireActivity(),
            cursor,
            onItemPress = { message, position, view, event ->
                handlePress(message, position, view, event)
            },
            onItemSwipeToReply = { message, position ->
                handleSwipeToReply(message, position)
            },
            onItemLongPress = { message, position ->
                handleLongPress(message, position)
            },
            onDeselect = { message, position ->
                actionMode?.let {
                    onDeselect(message, position, it)
                }
            },
            onAttachmentNeedsDownload = { attachmentId, mmsId ->
                // Start download (on IO thread)
                lifecycleScope.launch(Dispatchers.IO) {
                    JobQueue.shared.add(AttachmentDownloadJob(attachmentId, mmsId))
                }
            },
            glide = glide,
            lifecycleCoroutineScope = lifecycleScope
        )
        adapter.visibleMessageContentViewDelegate = this
        adapter
    }

    private val glide by lazy { GlideApp.with(this) }
    private val lockViewHitMargin by lazy { toPx(40, resources) }
    private val gifButton by lazy {
        InputBarButton(
            requireActivity(),
            R.drawable.ic_gif,
            hasOpaqueBackground = false,
            isGIFButton = true
        )
    }
    private val documentButton by lazy {
        InputBarButton(
            requireActivity(),
            R.drawable.ic_document,
            hasOpaqueBackground = false
        )
    }
    private val libraryButton by lazy {
        InputBarButton(
            requireActivity(),
            R.drawable.ic_gallery,
            hasOpaqueBackground = false
        )
    }
    private val cameraButton by lazy {
        InputBarButton(
            requireActivity(),
            R.drawable.ic_camera,
            hasOpaqueBackground = false
        )
    }
    private val messageToScrollTimestamp = AtomicLong(-1)
    private val messageToScrollAuthor = AtomicReference<Address?>(null)

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ConversationFragmentV2().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        // Extras
        const val THREAD_ID = "thread_id"
        const val ADDRESS = "address"
        const val SCROLL_MESSAGE_ID = "scroll_message_id"
        const val SCROLL_MESSAGE_AUTHOR = "scroll_message_author"
        const val HEX_ENCODED_PUBLIC_KEY="hex_encode_public_key"
        //Shortcut launcher
        const val SHORTCUT_LAUNCHER ="shortcut_launcher"
        //SetDataAndType
        const val URI = "uri"
        const val TYPE = "type"

        // Request codes
        const val PICK_DOCUMENT = 2
        const val TAKE_PHOTO = 7
        const val PICK_GIF = 10
        const val PICK_FROM_LIBRARY = 12
        const val INVITE_CONTACTS = 124

        //flag
        const val IS_UNSEND_REQUESTS_ENABLED = true
    }

    private var listenerCallback: Listener? = null
    private var mContext: Context? = null

    var senderBeldexAddress: String? = null
    private var sendBDXAmount: String? = null

    private fun getTxData(): TxData {
        return txData
    }

    private var txData = TxData()

    var pendingTransaction: PendingTransaction? = null
    var pendingTx: PendingTx? = null
    private var totalFunds: Long = 0
    private val mixin = 0
    private var isResume: Boolean = false
    private val cleanFormat = "%." + Helper.BDX_DECIMALS.toString() + "f"
    private var committedTx: PendingTx? = null

    private var syncText: String? = null
    private var syncProgress = -1
    private var firstBlock: Long = 0
    private var balance: Long = 0
    private val formatter = NumberFormat.getInstance()
    private var walletAvailableBalance: String? = null
    private var unlockedBalance: Long = 0
    private var walletSynchronized: Boolean = false
    private var blockProgressBarVisible: Boolean = false
    var transactionInProgress = false
    private var valueOfBalance = "--"
    private var valueOfUnLockedBalance = "--"
    private var valueOfWallet = "--"
    private var tooltipIsVisible = false
    private var dispatchTouched = false
    private var networkChangedReceiver: NetworkChangeReceiver? = null
    private var isNetworkAvailable = true
    private var callViewModel : CallViewModel? =null


    interface Listener {
        fun getConversationViewModel(): ConversationViewModel.AssistedFactory
        fun gettextSecurePreferences(): TextSecurePreferences
        fun onDisposeRequest()
        val totalFunds: Long
        fun onPrepareSend(tag: String?, data: TxData?)
        fun onSend(notes: UserNotes?)
        fun onBackPressedFun()

        fun walletOnBackPressed() //-

        //Wallet
        fun hasBoundService(): Boolean
        val connectionStatus: Wallet.ConnectionStatus?
        fun forceUpdate(requireActivity: Context)

        //SetDataAndType
        fun passSharedMessageToConversationScreen(thread: Recipient)
        fun getNode(): NodeInfo?
        val isSynced: Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            threadId = it.getLong(THREAD_ID)
            param2 = it.getString(ARG_PARAM2)
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentConversationV2Binding.inflate(inflater, container, false)
        (activity as HomeActivity).setSupportActionBar(binding.conversationFragmentToolbar)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchViewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        audioRecorder = AudioRecorder(requireActivity().applicationContext)

//        val thread = threadDb.getRecipientForThreadId(viewModel.threadId)
        lifecycleScope.launch {
            viewModel.backToHome.collectLatest {
                if (it) {
                    Toast.makeText(requireActivity(), "This thread has been deleted.", Toast.LENGTH_LONG)
                        .show()
                    backToHome()
                }
            }
        }


        // messageIdToScroll
        messageToScrollTimestamp.set(requireArguments().getLong(SCROLL_MESSAGE_ID, -1))
        messageToScrollAuthor.set(requireArguments().parcelable(SCROLL_MESSAGE_AUTHOR))

        networkChangedReceiver = NetworkChangeReceiver(::networkChange)
        networkChangedReceiver!!.register(requireContext())
        if (isNetworkAvailable) {
            binding.networkStatusLayout.visibility = View.GONE
        }


        lifecycleScope.launch(Dispatchers.IO) {
            unreadCount = viewModel.getUnreadCount()
            withContext(Dispatchers.Main) {
                setUpRecyclerView()
                setUpTypingObserver(viewModel.recipient.value!!)
                setUpRecipientObserver()
                getLatestOpenGroupInfoIfNeeded()
                setUpSearchResultObserver()
                scrollToFirstUnreadMessageIfNeeded()
            }
        }
        setUpToolBar()
        setUpInputBar()
        setUpLinkPreviewObserver()
        restoreDraftIfNeeded()
        setUpUiStateObserver()
        setMediaControlForReportIssue()
        binding.scrollToBottomButton.setOnClickListener {

            val layoutManager = (binding.conversationRecyclerView.layoutManager as? LinearLayoutManager) ?: return@setOnClickListener

            if (layoutManager.isSmoothScrolling) {
                binding.conversationRecyclerView.scrollToPosition(0)
            } else {
                // It looks like 'smoothScrollToPosition' will actually load all intermediate items in
                // order to do the scroll, this can be very slow if there are a lot of messages so
                // instead we check the current position and if there are more than 10 items to scroll
                // we jump instantly to the 10th item and scroll from there (this should happen quick
                // enough to give a similar scroll effect without having to load everything)
                val position = layoutManager.findFirstVisibleItemPosition()
                if (position > 10) {
                    binding.conversationRecyclerView.scrollToPosition(10)
                }

                binding.conversationRecyclerView.post {
                    binding.conversationRecyclerView.smoothScrollToPosition(0)
                }
            }
        }


        updateUnreadCountIndicator()
        updateSubtitle()
        setUpBlockedBanner()
        binding.searchBottomBar.setEventListener(this)
        showOrHideInputIfNeeded()
        /*Hales63*/
        setUpMessageRequestsBar()


//        viewModel.recipient.value?.let { recipient ->
//            if (recipient.isOpenGroupRecipient) {
//                try {
//                    val openGroup = beldexThreadDb.getOpenGroupChat(viewModel.threadId)
//                    if (openGroup == null) {
//                        Toast.makeText(
//                            requireContext(),
//                            "This thread has been deleted.",
//                            Toast.LENGTH_LONG
//                        ).show()
//                        return backToHome()
//                    }
//                } catch (ex: NullPointerException) {
//                    Log.d("Exception ", ex.message.toString())
//                }
//            }
//        }
        viewModel.recipient.value?.let { thread ->
            if (!thread.isGroupRecipient && thread.hasApprovedMe() && !thread.isBlocked && HomeActivity.reportIssueBChatID != thread.address.toString() && !thread.isLocalNumber) {
                listenerCallback!!.forceUpdate(requireActivity())
            }
        }
        showBlockProgressBar(viewModel.recipient.value)

        callShowPayAsYouChatBDXIcon(viewModel.recipient.value)

        showBalance(Helper.getDisplayAmount(0), Helper.getDisplayAmount(0), walletSynchronized)

        if (listenerCallback!!.getNode() == null) {
            setProgress(getString(R.string.failed_to_connect_to_node))
            setProgress(101)
            binding.inputBar.setDrawableProgressBar(requireActivity().applicationContext,true,valueOfWallet)
        }


        binding.slideToPayButton.onSlideCompleteListener = object : OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                binding.slideToPayButton.setCompleted(completed = false, withAnimation = true)
                if (CheckOnline.isOnline(requireActivity())) {
                    if (blockProgressBarVisible) {
                        when {
                            syncText != getString(R.string.status_synchronized) -> {
                                Toast.makeText(
                                    requireActivity(),
                                    "Blocks are syncing wait until your wallet is fully synchronized",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            senderBeldexAddress == null || senderBeldexAddress!!.isEmpty() -> {
                                if (viewModel.recipient.value != null) {
                                    senderBeldexAddress = viewModel.getBeldexAddress(viewModel.recipient.value!!.address)
                                    if (senderBeldexAddress.isValidString()) {
                                        if (validateBELDEXAmount(binding.inputBar.text)) {
                                            sendBDX()
                                        } else {
                                            Toast.makeText(
                                                requireActivity(),
                                                R.string.beldex_amount_valid_error_message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            requireActivity(),
                                            R.string.invalid_destination_address,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        requireActivity(),
                                        R.string.invalid_destination_address,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            validateBELDEXAmount(binding.inputBar.text) -> {
                                sendBDX()
                            }
                            else -> {
                                Toast.makeText(
                                    requireActivity(),
                                    R.string.beldex_amount_valid_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireActivity(),
                        R.string.please_check_your_internet_connection,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        callViewModel = ViewModelProvider(requireActivity())[CallViewModel::class.java]
    }

    override fun onResume() {
        super.onResume()
        ApplicationContext.getInstance(requireActivity()).messageNotifier.setVisibleThread(viewModel.threadId)
        if (!viewModel.markAllRead())
            return

        viewModel.recipient.value?.let { thread ->
            showBlockProgressBar(thread)
            callShowPayAsYouChatBDXIcon(thread)

            if (TextSecurePreferences.isPayAsYouChat(requireActivity())) {
                if (binding.inputBar.text.isNotEmpty() && binding.inputBar.text.matches(Regex("^(([0-9]{0,9})?|[.][0-9]{0,5})?|([0-9]{0,9}+([.][0-9]{0,5}))\$"))) {
                    binding.inputBar.setTextColor(thread,HomeActivity.reportIssueBChatID,true)
                    showPayWithSlide(thread,true)
                } else {
                    binding.inputBar.setTextColor(thread,HomeActivity.reportIssueBChatID,false)
                    showPayWithSlide(thread,false)
                }
                if(syncText == getString(R.string.failed_to_connect_to_node) || syncText == getString(R.string.failed_connected_to_the_node)|| syncText == getString(R.string.no_node_connection)){
                    binding.inputBar.showDrawableProgressBar(true,valueOfWallet)
                }else{
                    binding.inputBar.showDrawableProgressBar(false,valueOfWallet)
                }
            } else {
                binding.inputBar.setTextColor(thread,HomeActivity.reportIssueBChatID,false)
                showPayWithSlide(thread,false)
            }
        }

        //Minimized app
        if (onTransactionProgress) {
            onTransactionProgress = false
            hideProgress()
            refreshTransactionDetails()
            //Continuously Transaction
            this.pendingTransaction = null
            this.pendingTx = null
        }
    }

    override fun onStop() {
        super.onStop()
        /*These 2 lines are introduced to handle the visibility of keyboard on home screen after we put the
        * app in background with this screen visible and keyboard opened. These lines can be removed if in future the issue is handled other
        * way.*/
        binding.inputBar.clearFocus()
        Helper.hideKeyboard(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelVoiceMessage()
        isNetworkAvailable = false
        networkChangedReceiver?.unregister(requireContext())
        networkChangedReceiver = null
    }

    private fun networkChange(networkAvailable: Boolean) {
        isNetworkAvailable = networkAvailable
        if (networkAvailable) {
            binding.connectedStatus.text = getString(R.string.connected)
            binding.networkStatusImage.setImageResource(R.drawable.ic_connected)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.networkStatusLayout.visibility = View.GONE
            }, 3000)
        } else {
            binding.networkStatusLayout.visibility = View.VISIBLE
            binding.connectedStatus.text = getString(R.string.try_to_connect)
            binding.networkStatusImage.setImageResource(R.drawable.ic_try_to_connect)
        }
    }

    private fun showPayWithSlide(thread: Recipient?, status: Boolean) {
        if (thread != null && !thread.isGroupRecipient && thread.hasApprovedMe() && !thread.isBlocked && HomeActivity.reportIssueBChatID!=thread.address.toString() && !thread.isLocalNumber && status) {
            binding.slideToPayButton.visibility = View.VISIBLE
        }else{
            binding.slideToPayButton.visibility = View.GONE
        }
    }

    private fun callShowPayAsYouChatBDXIcon(thread: Recipient?) {
        if (thread != null) {
            binding.inputBar.showPayAsYouChatBDXIcon(thread, HomeActivity.reportIssueBChatID)
        }
    }

    private fun showBlockProgressBar(thread: Recipient?) {
        try {
            if (thread != null) {
                blockProgressBarVisible = if (!thread.isGroupRecipient && thread.hasApprovedMe() && !thread.isBlocked && TextSecurePreferences.isPayAsYouChat(
                        requireActivity()
                    ) && thread.isApproved && HomeActivity.reportIssueBChatID != thread.address.toString() && !thread.isLocalNumber
                ) {
                    binding.inputBar.showProgressBar(true)
                    true
                } else {
                    binding.inputBar.showFailedProgressBar(false)
                    binding.inputBar.showProgressBar(false)
                    false
                }
            }
        } catch (ex: IllegalStateException) {
            Timber.tag("Exception").d(ex.toString())
        }
    }

    override fun onPause() {
        //Continuously loading progress bar
        if (inProgress) {
            hideProgress()
        }

        endActionMode()
        ApplicationContext.getInstance(requireActivity()).messageNotifier.setVisibleThread(-1)
        viewModel.saveDraft(binding.inputBar.text.trim())
        val recipient = viewModel.recipient.value ?: return super.onPause()
        /*Hales63*/ // New Line
        if (TextSecurePreferences.getPlayerStatus(requireActivity())) {
            TextSecurePreferences.setPlayerStatus(requireActivity(), false)
            val contactDB = DatabaseComponent.get(requireActivity()).bchatContactDatabase()
            val contact = contactDB.getContactWithBchatID(recipient.address.toString())
            val actionMode = this.actionMode
            if (contact?.isTrusted != null && contact.isTrusted && actionMode == null && selectedEvent != null && selectedView != null) {
                selectedEvent?.let {
                    selectedView?.onContentClick(it)
                }
                if (selectedMessageRecord?.isOutgoing != null) {
                    if (selectedMessageRecord?.isOutgoing!!) {
                        selectedEvent?.let { selectedView?.onContentClick(it) }
                    }
                }
            } else if (contact?.isTrusted == null && selectedMessageRecord?.isOutgoing == false && actionMode == null && selectedEvent != null && selectedView != null) {
                selectedEvent?.let {
                    selectedView?.onContentClick(it)
                }
            }
            if (selectedMessageRecord?.isOutgoing != null && selectedMessageRecord?.isOutgoing!! && actionMode == null && selectedEvent != null && selectedView != null) {
                selectedEvent?.let {
                    selectedView?.onContentClick(it)
                }
            }
        }
        super.onPause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        if (context is Listener) {
            listenerCallback = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        this.mContext = null
    }

    // `position` is the adapter position; not the visual position
    private fun handlePress(
        message: MessageRecord,
        position: Int,
        view: VisibleMessageView,
        event: MotionEvent
    ) {
        val actionMode = this.actionMode
        selectedEvent = event
        selectedView = view
        selectedMessageRecord = message
        if (actionMode != null) {
            onDeselect(message, position, actionMode)
        } else {
            // NOTE:
            // We have to use onContentClick (rather than a click listener directly on
            // the view) so as to not interfere with all the other gestures. Do not add
            // onClickListeners directly to message content views.
            view.onContentClick(event)
        }
    }

    private fun onDeselect(message: MessageRecord, position: Int, actionMode: ActionMode) {
        adapter.toggleSelection(message, position)
        val actionModeCallback =
            ConversationActionModeCallback(adapter, viewModel.threadId, requireActivity())
        actionModeCallback.delegate = this
        actionModeCallback.updateActionModeMenu(actionMode.menu)
        if (adapter.selectedItems.isEmpty()) {
            actionMode.finish()
            this.actionMode = null
        }
    }

    // `position` is the adapter position; not the visual position
    private fun handleSwipeToReply(message: MessageRecord, position: Int) {
        //New Line
        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 16
        val recipient = viewModel.recipient.value ?: return
        binding.inputBar.draftQuote(recipient, message, glide)
    }

    private fun handleLongPress(message: MessageRecord, position: Int) {
        val actionMode = this.actionMode
        val actionModeCallback =
            ConversationActionModeCallback(adapter, viewModel.threadId, requireActivity())
        actionModeCallback.delegate = this
        searchViewItem?.collapseActionView()
        if (actionMode == null) { // Nothing should be selected if this is the case
            adapter.toggleSelection(message, position)
            this.actionMode =
                this.activity?.startActionMode(actionModeCallback, ActionMode.TYPE_PRIMARY)
        } else {
            adapter.toggleSelection(message, position)
            actionModeCallback.updateActionModeMenu(actionMode.menu)
            if (adapter.selectedItems.isEmpty()) {
                actionMode.finish()
                this.actionMode = null
            }
        }
    }

    override fun inputBarHeightChanged(newValue: Int) {
    }

    override fun inputBarEditTextContentChanged(newContent: CharSequence) {
        val inputBarText = binding.inputBar.text
        if (listenerCallback!!.gettextSecurePreferences().isLinkPreviewsEnabled()) {
            linkPreviewViewModel.onTextChanged(requireActivity(), inputBarText, 0, 0)
        }
        val recipient = viewModel.recipient.value ?: return
        if (recipient.isGroupRecipient) {
            showOrHideMentionCandidatesIfNeeded(newContent)
        }
        if (LinkPreviewUtil.findWhitelistedUrls(newContent.toString()).isNotEmpty()
            && !listenerCallback!!.gettextSecurePreferences()
                .isLinkPreviewsEnabled() && !listenerCallback!!.gettextSecurePreferences()
                .hasSeenLinkPreviewSuggestionDialog()
        ) {
            LinkPreviewDialog {
                setUpLinkPreviewObserver()
                linkPreviewViewModel.onEnabled()
                linkPreviewViewModel.onTextChanged(requireContext(), inputBarText, 0, 0)
            }.show(requireActivity().supportFragmentManager, "Link Preview Dialog")
            listenerCallback!!.gettextSecurePreferences().setHasSeenLinkPreviewSuggestionDialog()
        }
    }

    override fun toggleAttachmentOptions() {
        val targetAlpha = if (isShowingAttachmentOptions) 0.0f else 1.0f
        val allButtonContainers = listOfNotNull(
            binding.cameraButtonContainer,
            binding.libraryButtonContainer,
            binding.documentButtonContainer,
            binding.gifButtonContainer
        )
        val isReversed = isShowingAttachmentOptions // Run the animation in reverse
        val count = allButtonContainers.size
        allButtonContainers.indices.forEach { index ->
            val view = allButtonContainers[index]
            val animation = ValueAnimator.ofObject(FloatEvaluator(), view.alpha, targetAlpha)
            animation.duration = 250L
            animation.startDelay =
                if (isReversed) 50L * (count - index.toLong()) else 50L * index.toLong()
            animation.addUpdateListener { animator ->
                view.alpha = animator.animatedValue as Float
            }
            animation.start()
        }
        isShowingAttachmentOptions = !isShowingAttachmentOptions
        val allButtons = listOf(cameraButton, libraryButton, documentButton, gifButton)
        allButtons.forEach { it.snIsEnabled = isShowingAttachmentOptions }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        val mediaPreppedListener = object : ListenableFuture.Listener<Boolean> {

            override fun onSuccess(result: Boolean?) {
                if(result == true) {
                    sendAttachments(attachmentManager.buildSlideDeck().asAttachments(), null)
                }else{
                    Toast.makeText(requireActivity().applicationContext, R.string.MediaSendActivity_an_item_was_removed_because_it_exceeded_the_size_limit, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(e: ExecutionException?) {
                Toast.makeText(
                    requireActivity(),
                    R.string.activity_conversation_attachment_prep_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        when (requestCode) {
            PICK_DOCUMENT -> {
                val uri = intent?.data ?: return
                prepMediaForSending(uri, AttachmentManager.MediaType.DOCUMENT).addListener(
                    mediaPreppedListener
                )
            }
            PICK_GIF -> {
                intent ?: return
                val uri = intent.data ?: return
                val type = AttachmentManager.MediaType.GIF
                val width = intent.getIntExtra(GiphyActivity.EXTRA_WIDTH, 0)
                val height = intent.getIntExtra(GiphyActivity.EXTRA_HEIGHT, 0)
                prepMediaForSending(uri, type, width, height).addListener(mediaPreppedListener)
            }
            PICK_FROM_LIBRARY,
            TAKE_PHOTO -> {
                intent ?: return
                val body = intent.getStringExtra(MediaSendActivity.EXTRA_MESSAGE)
                val media = intent.getParcelableArrayListExtra<Media>(
                    MediaSendActivity.EXTRA_MEDIA
                ) ?: return
                val slideDeck = SlideDeck()
                for (item in media) {
                    when {
                        MediaUtil.isVideoType(item.mimeType) -> {
                            slideDeck.addSlide(
                                VideoSlide(
                                    requireActivity(),
                                    item.uri,
                                    0,
                                    item.caption.orNull()
                                )
                            )
                        }
                        MediaUtil.isGif(item.mimeType) -> {
                            slideDeck.addSlide(
                                GifSlide(
                                    requireActivity(),
                                    item.uri,
                                    0,
                                    item.width,
                                    item.height,
                                    item.caption.orNull()
                                )
                            )
                        }
                        MediaUtil.isImageType(item.mimeType) -> {
                            slideDeck.addSlide(
                                ImageSlide(
                                    requireActivity(),
                                    item.uri,
                                    0,
                                    item.width,
                                    item.height,
                                    item.caption.orNull()
                                )
                            )
                        }
                        else -> {
                            Timber.tag("Beldex")
                                .d("Asked to send an unexpected media type: '" + item.mimeType + "'. Skipping.")
                        }
                    }
                }
                sendAttachments(slideDeck.asAttachments(), body)
            }
            INVITE_CONTACTS -> {
                if (viewModel.recipient.value?.isOpenGroupRecipient != true) {
                    return
                }
                val extras = intent?.extras ?: return
                if (!intent.hasExtra(SelectContactsActivity.selectedContactsKey)) {
                    return
                }
                val selectedContacts =
                    extras.getStringArray(SelectContactsActivity.selectedContactsKey)!!
                val recipients = selectedContacts.map { contact ->
                    Recipient.from(requireActivity(), Address.fromSerialized(contact), true)
                }
                viewModel.inviteContacts(recipients)
            }
        }
    }

    private fun prepMediaForSending(
        uri: Uri,
        type: AttachmentManager.MediaType
    ): ListenableFuture<Boolean> {
        return prepMediaForSending(uri, type, null, null)
    }

    private fun prepMediaForSending(
        uri: Uri,
        type: AttachmentManager.MediaType,
        width: Int?,
        height: Int?
    ): ListenableFuture<Boolean> {
        return attachmentManager.setMedia(
            glide,
            uri,
            type,
            MediaConstraints.getPushMediaConstraints(),
            width ?: 0,
            height ?: 0
        )
    }

    override fun startRecordingVoiceMessage() {
        if(callViewModel?.callStartTime == -1L) {
            if (Permissions.hasAll(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
                showVoiceMessageUI()
                this.activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                audioRecorder.startRecording()
                stopAudioHandler.postDelayed(
                    stopVoiceMessageRecordingTask,
                    60000
                ) // Limit voice messages to 1 minute each
            } else {
                Permissions.with(this)
                    .request(Manifest.permission.RECORD_AUDIO)
                    .withRationaleDialog(
                        getString(R.string.ConversationActivity_to_send_audio_messages_allow_signal_access_to_your_microphone),
                        R.drawable.ic_microphone_permission
                    )
                    .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_requires_the_microphone_permission_in_order_to_send_audio_messages))
                    .execute()
            }
        }else{
            Toast.makeText(requireActivity(),getString(R.string.record_voice_restriction),Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMicrophoneButtonMove(event: MotionEvent) {
        val rawX = event.rawX
        val chevronImageView = binding.inputBarRecordingView.chevronImageView
        val slideToCancelTextView = binding.inputBarRecordingView.slideToCancelTextView
        if (rawX < screenWidth / 2) {
            val translationX = rawX - screenWidth / 2
            val sign = -1.0f
            val chevronDamping = 4.0f
            val labelDamping = 3.0f
            val chevronX =
                (chevronDamping * (sqrt(abs(translationX)) / sqrt(chevronDamping))) * sign
            val labelX = (labelDamping * (sqrt(abs(translationX)) / sqrt(labelDamping))) * sign
            chevronImageView.translationX = chevronX
            slideToCancelTextView.translationX = labelX
        } else {
            chevronImageView.translationX = 0.0f
            slideToCancelTextView.translationX = 0.0f
        }
        if (isValidLockViewLocation(event.rawX.roundToInt(), event.rawY.roundToInt())) {
            if (!isLockViewExpanded) {
                expandVoiceMessageLockView()
                isLockViewExpanded = true
            }
        } else {
            if (isLockViewExpanded) {
                collapseVoiceMessageLockView()
                isLockViewExpanded = false
            }
        }
    }

    override fun onMicrophoneButtonCancel(event: MotionEvent) {
        hideVoiceMessageUI()
    }

    override fun onMicrophoneButtonUp(event: MotionEvent) {
        val x = event.rawX.roundToInt()
        val y = event.rawY.roundToInt()
        if (isValidLockViewLocation(x, y)) {
            binding.inputBarRecordingView.lock()
        } else {
            val recordButtonOverlay = binding.inputBarRecordingView.recordButtonOverlay
            val location = IntArray(2) { 0 }
            recordButtonOverlay.getLocationOnScreen(location)
            val hitRect = Rect(
                location[0],
                location[1],
                location[0] + recordButtonOverlay.width,
                location[1] + recordButtonOverlay.height
            )
            if (hitRect.contains(x, y)) {
                sendVoiceMessage()
            } else {
                cancelVoiceMessage()
            }
        }
    }

    override fun sendMessage() {
        val recipient = viewModel.recipient.value ?: callViewModel() ?: return
        if (recipient.isContactRecipient && recipient.isBlocked) {
            BlockedDialog(recipient).show(
                requireActivity().supportFragmentManager,
                "Blocked Dialog"
            )
            return
        }

        val binding = binding
        if (binding.inputBar.text.trim().isEmpty()) {
            Toast.makeText(
                requireActivity(),
                R.string.empty_message_toast,
                Toast.LENGTH_SHORT
            ).show()
        }else if (binding.inputBar.linkPreview != null || binding.inputBar.quote != null) {
            sendAttachments(
                listOf(),
                getMessageBody(),
                binding.inputBar.quote,
                binding.inputBar.linkPreview
            )
        }else {
            Timber.tag("SendMessage ").d("5")
            callSendTextOnlyMessage()
        }
    }

    private fun validateBELDEXAmount(amount: String): Boolean {
        val maxValue = 150000000.00000
        val value = amount.replace(',', '.')
        val regExp = "^(([0-9]{0,9})?|[.][0-9]{0,5})?|([0-9]{0,9}+([.][0-9]{0,5}))\$"

        val isValid: Boolean = if (value.matches(Regex(regExp))) {
            if (value == ".") {
                false
            } else {
                try {
                    val dValue = value.toDouble()
                    (dValue <= maxValue && dValue > 0)
                } catch (e: java.lang.Exception) {
                    false
                }
            }
        } else {
            false
        }
        return isValid
    }

    private fun callSendTextOnlyMessage() {
        if (binding.inputBar.text.length > 4096) {
            Toast.makeText(
                requireActivity(),
                "Text limit exceed: Maximum limit of messages is 4096 characters",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            sendTextOnlyMessage()
        }
    }

    override fun inChatBDXOptions() {
        try {
                val dialog = android.app.AlertDialog.Builder(requireActivity())
                val inflater = layoutInflater
                val dialogView = inflater.inflate(R.layout.pay_as_you_chat, null)
                dialog.setView(dialogView)

                val okButton = dialogView.findViewById<Button>(R.id.okButton)
                val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
                val enableInstruction =
                    dialogView.findViewById<TextView>(R.id.payAsYouChatEnable_Instruction)
                val alert = dialog.create()
                alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alert.setCanceledOnTouchOutside(false)
                alert.show()
                if (TextSecurePreferences.isPayAsYouChat(requireActivity())) {
                    enableInstruction.text =
                        fromHtml("To disable pay as you chat, go to <b>My Account -> Chat Settings -> Pay As You Chat</b> to use this option")
                } else {
                    enableInstruction.text =
                        fromHtml("Enable pay as you chat from <b>My Account -> Chat Settings -> Pay As You Chat</b> to use this option")
                }
                okButton.setOnClickListener {
                    val intent = Intent(requireActivity(), ChatSettingsActivity::class.java)
                    this.activity?.startActivity(intent)
                    alert.dismiss()
                }
                cancelButton.setOnClickListener {
                    alert.dismiss()
                }
        } catch (exception: Exception) {
            Timber.tag("Beldex").d("PayAsYouChat exception $exception")
        }
    }

    override fun walletDetailsUI() {
        when {
            binding.tooltip.isVisible -> {
                binding.tooltip.visibility = View.GONE
                tooltipIsVisible = false
            }
            dispatchTouched -> {
                dispatchTouched = false
            }
            else -> {
                binding.tooltip.visibility = View.VISIBLE
                tooltipIsVisible = true
            }
        }
        toolTip()
    }

    fun dispatchTouchEvent() {
        if (tooltipIsVisible) {
            binding.tooltip.visibility = View.GONE
            tooltipIsVisible = false
            dispatchTouched = true
        } else {
            dispatchTouched = false
        }
    }

    private fun toolTip() {
        checkIfFragmentAttached {
            if (TextSecurePreferences.isPayAsYouChat(requireContext())) {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                        when {
                            valueOfWallet == "--" -> {
                                binding.tooltip.text = getString(R.string.failed_to_connect)
                                failedToConnectToolTipStyle()
                            }
                            valueOfWallet != "100%" -> {
                                binding.tooltip.text = Html.fromHtml("<p>Wallet Synchronizing <b> $valueOfWallet </b> </p>", Html.FROM_HTML_MODE_COMPACT)
                                tooltipStyle()
                            }
                            else -> {
                                binding.tooltip.text = Html.fromHtml("<p> <b>Balance : $valueOfBalance </b> </p> <br /> <p><b>Unlocked Balance : $valueOfUnLockedBalance</b> </p> <br /> <p>Wallet :  $valueOfWallet </p>", Html.FROM_HTML_MODE_COMPACT)
                                tooltipStyle()
                            }
                        }
                    }
                    else -> {
                        when {
                            valueOfWallet == "--" -> {
                                binding.tooltip.text = getString(R.string.failed_to_connect)
                                failedToConnectToolTipStyle()
                            }
                            valueOfWallet != "100%" -> {
                                binding.tooltip.text = Html.fromHtml("<p> Wallet Synchronizing <b> $valueOfWallet</b> </p>")
                                tooltipStyle()
                            }
                            else -> {
                                binding.tooltip.text = Html.fromHtml("<p> <b>Balance : $valueOfBalance </b> </p> <br /> <p><b>Unlocked Balance : $valueOfUnLockedBalance</b> </p> <br /> <p>Wallet :  $valueOfWallet </p>")
                                tooltipStyle()
                            }
                        }

                    }
                }
            } else {
                val explanation = resources.getString(R.string.hold_to_enable_option)
                val spannable = SpannableStringBuilder(explanation)
                val face =
                    Typeface.createFromAsset(requireContext().assets, "fonts/open_sans_bold.ttf")
                val color = ResourcesCompat.getColor(requireContext().resources,
                    R.color.node_status, requireContext().theme)
                spannable.setSpan(ForegroundColorSpan(color),
                    14,
                    30,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.tooltip.typeface = face
                binding.tooltip.text = spannable
            }
        }
    }

    private fun tooltipStyle() {
        val face =
            Typeface.createFromAsset(requireContext().assets,
                "fonts/open_sans_medium.ttf")
        @ColorInt val color =
            requireContext().resources.getColorWithID(R.color.text,
                requireContext().theme)
        binding.tooltip.setTextColor(color)
        binding.tooltip.typeface = face
    }

    private fun failedToConnectToolTipStyle() {
        val face =
            Typeface.createFromAsset(requireContext().assets,
                "fonts/open_sans_medium.ttf")
        @ColorInt val color =
            requireContext().resources.getColorWithID(R.color.red,
                requireContext().theme)
        binding.tooltip.setTextColor(color)
        binding.tooltip.typeface = face
    }

    private fun fromHtml(source: String?): Spanned? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(source)
        }
    }

    override fun commitInputContent(contentUri: Uri) {
        val recipient = viewModel.recipient.value ?: return
        val media = Media(
            contentUri,
            MediaUtil.getMimeType(
                requireActivity(),
                contentUri
            )!!,
            0,
            0,
            0,
            0,
            Optional.absent(),
            Optional.absent()
        )
        startActivityForResult(
            MediaSendActivity.buildEditorIntent(
                requireActivity(),
                listOf(media),
                recipient,
                getMessageBody()
            ),
            PICK_FROM_LIBRARY
        )

    }

    override fun handleVoiceMessageUIHidden() {
        val inputBar = binding.inputBar
        val inputBarCard = binding.inputBarCard
        //New Line
        inputBar.visibility = View.VISIBLE

        inputBar.alpha = 1.0f
        inputBarCard.alpha = 1.0f
        val animation = ValueAnimator.ofObject(FloatEvaluator(), 0.0f, 1.0f)
        animation.duration = 250L
        animation.addUpdateListener { animator ->
            inputBar.alpha = animator.animatedValue as Float
            inputBarCard.alpha = animator.animatedValue as Float
        }
        animation.start()
    }

    override fun sendVoiceMessage() {
            hideVoiceMessageUI()
            this.activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val future = audioRecorder.stopRecording()
            stopAudioHandler.removeCallbacks(stopVoiceMessageRecordingTask)
            future.addListener(object : ListenableFuture.Listener<Pair<Uri, Long>> {

                override fun onSuccess(result: Pair<Uri, Long>) {
                    val audioSlide = AudioSlide(
                        requireActivity(),
                        result.first,
                        result.second,
                        MediaTypes.AUDIO_AAC,
                        true
                    )
                    val slideDeck = SlideDeck()
                    slideDeck.addSlide(audioSlide)
                    sendAttachments(slideDeck.asAttachments(), null)
                }

                override fun onFailure(e: ExecutionException) {
                    Toast.makeText(
                        requireActivity(),
                        R.string.ConversationActivity_unable_to_record_audio,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    override fun cancelVoiceMessage() {
        hideVoiceMessageUI()
        this.activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        audioRecorder.stopRecording()
        stopAudioHandler.removeCallbacks(stopVoiceMessageRecordingTask)
    }

    override fun deleteMessages(messages: Set<MessageRecord>) {
        val recipient = viewModel.recipient.value ?: return
        if (!IS_UNSEND_REQUESTS_ENABLED) {
            deleteMessagesWithoutUnsendRequest(messages)
            return
        }
        val allSentByCurrentUser = messages.all { it.isOutgoing }
        val allHasHash =
            messages.all { viewModel.getMessageServerHash(it.id) != null }
        if (recipient.isOpenGroupRecipient) {
            val messageCount = messages.size
            val builder = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            builder.setTitle(
                resources.getQuantityString(
                    R.plurals.ConversationFragment_delete_selected_messages,
                    messageCount,
                    messageCount
                )
            )
            builder.setMessage(
                resources.getQuantityString(
                    R.plurals.ConversationFragment_this_will_permanently_delete_all_n_selected_messages,
                    messageCount,
                    messageCount
                )
            )
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.delete) { _, _ ->
                for (message in messages) {
                    viewModel.deleteForEveryone(message)
                }
                endActionMode()
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                endActionMode()
            }
            builder.show()
        } else if (allSentByCurrentUser && allHasHash) {
            val bottomSheet = DeleteOptionsBottomSheet()
            bottomSheet.recipient = recipient
            bottomSheet.onDeleteForMeTapped = {
                for (message in messages) {
                    viewModel.deleteLocally(message)
                }
                bottomSheet.dismiss()
                endActionMode()
            }
            bottomSheet.onDeleteForEveryoneTapped = {
                for (message in messages) {
                    viewModel.deleteForEveryone(message)
                }
                bottomSheet.dismiss()
                endActionMode()
            }
            bottomSheet.onCancelTapped = {
                bottomSheet.dismiss()
                endActionMode()
            }
            bottomSheet.show(requireActivity().supportFragmentManager, bottomSheet.tag)
        } else {
            val messageCount = messages.size
            val builder = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            builder.setTitle(
                resources.getQuantityString(
                    R.plurals.ConversationFragment_delete_selected_messages,
                    messageCount,
                    messageCount
                )
            )
            builder.setMessage(
                resources.getQuantityString(
                    R.plurals.ConversationFragment_this_will_permanently_delete_all_n_selected_messages,
                    messageCount,
                    messageCount
                )
            )
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.delete) { _, _ ->
                for (message in messages) {
                    viewModel.deleteLocally(message)
                }
                endActionMode()
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                endActionMode()
            }
            builder.show()
        }
    }

    override fun banUser(messages: Set<MessageRecord>) {
        val builder = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_ForBan)
        builder.setTitle(R.string.ConversationFragment_ban_selected_user)
        builder.setMessage("This will ban the selected user from this room. It won't ban them from other rooms.")
        builder.setCancelable(true)
        builder.setPositiveButton(R.string.ban) { _, _ ->
            viewModel.banUser(messages.first().individualRecipient)
            endActionMode()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            endActionMode()
        }
        builder.show()
    }

    override fun banAndDeleteAll(messages: Set<MessageRecord>) {
        val builder = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_ForBan)
        builder.setTitle(R.string.ConversationFragment_ban_selected_user)
        builder.setMessage("This will ban the selected user from this room and delete all messages sent by them. It won't ban them from other rooms or delete the messages they sent there.")
        builder.setCancelable(true)
        builder.setPositiveButton(R.string.ban) { _, _ ->
            viewModel.banAndDeleteAll(messages.first().individualRecipient)
            endActionMode()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            endActionMode()
        }
        builder.show()
    }

    override fun copyMessages(messages: Set<MessageRecord>) {
        val sortedMessages = messages.sortedBy { it.dateSent }
        val messageSize = sortedMessages.size
        val builder = StringBuilder()
        val messageIterator = sortedMessages.iterator()
        while (messageIterator.hasNext()) {
            val message = messageIterator.next()
            var body = MentionUtilities.highlightMentions(
                message.body,
                viewModel.threadId,
                requireActivity()
            )
            if (message.isPayment) {
                //Payment Tag
                var amount = ""
                var direction = ""
                try {
                    val mainObject = JSONObject(message.body)
                    val uniObject = mainObject.getJSONObject("kind")
                    amount = uniObject.getString("amount")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                direction = if (message.isOutgoing) {
                    resources.getString(R.string.payment_sent)
                } else {
                    resources.getString(R.string.payment_received)
                }
                body = resources.getString(R.string.reply_payment_card_message, direction, amount)
            } else if (message.isOpenGroupInvitation) {
                body = resources.getString(R.string.ThreadRecord_open_group_invitation)
            }

            if (TextUtils.isEmpty(body)) {
                continue
            }
            if (messageSize > 1) {
                val formattedTimestamp = DateUtils.getDisplayFormattedTimeSpanString(
                    requireActivity(),
                    Locale.getDefault(),
                    message.timestamp
                )
                builder.append("$formattedTimestamp: ")
            }
            builder.append(body)
            if (messageIterator.hasNext()) {
                builder.append('\n')
            }
        }
        if (builder.isNotEmpty() && builder[builder.length - 1] == '\n') {
            builder.deleteCharAt(builder.length - 1)
        }
        val result = builder.toString()
        if (TextUtils.isEmpty(result)) {
            return
        }

        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Message Content", result))
        Toast.makeText(requireActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        endActionMode()
    }

    override fun copyBchatID(messages: Set<MessageRecord>) {
        val bchatID = if(messages.first().isOutgoing){
            hexEncodedPublicKey
        }else{
            messages.first().individualRecipient.address.toString()
        }
        val clip = ClipData.newPlainText("BChat ID", bchatID)
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(clip)
        Toast.makeText(requireActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        endActionMode()
    }

    override fun resendMessage(messages: Set<MessageRecord>) {
        messages.iterator().forEach { messageRecord ->
            ResendMessageUtilities.resend(messageRecord)
        }
        endActionMode()
    }

    override fun showMessageDetail(messages: Set<MessageRecord>) {
        val message = messages.first()
        val intent = Intent(requireActivity(), MessageDetailActivity::class.java)
        intent.putExtra(MessageDetailActivity.MESSAGE_TIMESTAMP, message.timestamp)
        startActivity(intent)
        endActionMode()
    }

    override fun saveAttachment(messages: Set<MessageRecord>) {
        val message = messages.first() as MmsMessageRecord
        SaveAttachmentTask.showWarningDialog(requireActivity(), { _, _ ->
            Permissions.with(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .maxSdkVersion(Build.VERSION_CODES.P)
                .withPermanentDenialDialog(getString(R.string.MediaPreviewActivity_signal_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
                .onAnyDenied {
                    endActionMode()
                    Toast.makeText(
                        requireActivity(),
                        R.string.MediaPreviewActivity_unable_to_write_to_external_storage_without_permission,
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onAllGranted {
                    endActionMode()
                    val attachments: List<SaveAttachmentTask.Attachment?> =
                        Stream.of(message.slideDeck.slides)
                            .filter { s: Slide -> s.uri != null && (s.hasImage() || s.hasVideo() || s.hasAudio() || s.hasDocument()) }
                            .map { s: Slide ->
                                SaveAttachmentTask.Attachment(
                                    s.uri!!,
                                    s.contentType,
                                    message.dateReceived,
                                    s.fileName.orNull()
                                )
                            }
                            .toList()
                    if (attachments.isNotEmpty()) {
                        val saveTask = SaveAttachmentTask(requireActivity())
                        saveTask.executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR,
                            *attachments.toTypedArray()
                        )
                        if (!message.isOutgoing) {
                            sendMediaSavedNotification()
                        }
                        return@onAllGranted
                    }
                    Toast.makeText(
                        requireActivity(),
                        resources.getQuantityString(
                            R.plurals.ConversationFragment_error_while_saving_attachments_to_sd_card,
                            1
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .execute()
        })
    }

    override fun reply(messages: Set<MessageRecord>) {
        val recipient = viewModel.recipient.value ?: return
        //New Line
        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 16

        binding.inputBar.draftQuote(recipient, messages.first(), glide)
        endActionMode()
    }

    override fun destroyActionMode() {
        this.actionMode = null
    }

    //SteveJosephh21 - 08
    override fun block(deleteThread: Boolean) {
        val title = R.string.RecipientPreferenceActivity_block_this_contact_question
        val message =
            R.string.RecipientPreferenceActivity_you_will_no_longer_receive_messages_and_calls_from_this_contact
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_Clear_All)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.RecipientPreferenceActivity_block) { _, _ ->
                viewModel.block()
                viewModel.recipient.value?.let { thread ->
                    showBlockProgressBar(thread)
                }
                if (deleteThread) {
                    viewModel.deleteThread()
                }
            }.show()
        //New Line
        val textView: TextView? = dialog.findViewById(android.R.id.message)
        val face: Typeface =
            Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    override fun unblock() {
        val title = R.string.ConversationActivity_unblock_this_contact_question
        val message =
            R.string.ConversationActivity_you_will_once_again_be_able_to_receive_messages_and_calls_from_this_contact
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_Clear_All)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.ConversationActivity_unblock) { _, _ ->
                viewModel.unblock()
                viewModel.recipient.value?.let { thread ->
                    showBlockProgressBar(thread)
                }
            }.show()

        //New Line
        val textView: TextView? = dialog.findViewById(android.R.id.message)
        val face: Typeface =
            Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    fun getSystemService(name: String): Any? {
        if (name == ActivityDispatcher.SERVICE) {
            return this
        }
        return super.requireActivity().getSystemService(name)
    }

    override fun copyBchatID(bchatId: String) {
        val clip = ClipData.newPlainText("BChat ID", bchatId)
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(clip)
        Toast.makeText(requireActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    override fun showExpiringMessagesDialog(thread: Recipient) {
        if (thread.isClosedGroupRecipient) {
            val group = viewModel.getGroup(thread)
            if (group?.isActive == false) {
                return
            }
        }
        ExpirationDialog.show(requireActivity(), thread.expireMessages) { expirationTime: Int ->
            viewModel.setExpireMessages(thread, expirationTime)
            val message = ExpirationTimerUpdate(expirationTime)
            message.recipient = thread.address.serialize()
            message.sentTimestamp = MnodeAPI.nowWithOffset
            val expiringMessageManager =
                ApplicationContext.getInstance(requireActivity()).expiringMessageManager
            expiringMessageManager.setExpirationTimer(message)
            MessageSender.send(message, thread.address)
            this.activity?.invalidateOptionsMenu()
        }
    }

    override fun scrollToMessageIfPossible(timestamp: Long) {
        val lastSeenItemPosition = adapter.getItemPositionForTimestamp(timestamp) ?: return
        binding.conversationRecyclerView.scrollToPosition(lastSeenItemPosition)
    }

    fun playVoiceMessageAtIndexIfPossible(indexInAdapter: Int) {
        if (indexInAdapter < 0 || indexInAdapter >= adapter.itemCount) {
            return
        }
        val viewHolder =
            binding.conversationRecyclerView.findViewHolderForAdapterPosition(indexInAdapter) as? ConversationAdapter.VisibleMessageViewHolder ?: return
        val visibleMessageView = ViewVisibleMessageBinding.bind(viewHolder.view).visibleMessageView
        visibleMessageView.playVoiceMessage()
    }

    fun onSearchOpened() {
        searchViewModel!!.onSearchOpened()
        binding.searchBottomBar.visibility = View.VISIBLE
        binding.searchBottomBar.setData(0, 0)
        binding.inputBar.visibility = View.GONE
    }

    fun onSearchClosed() {
        searchViewModel!!.onSearchClosed()
        binding.searchBottomBar.visibility = View.GONE
        binding.inputBar.visibility = View.VISIBLE
        adapter.onSearchQueryUpdated(null)
        this.activity?.invalidateOptionsMenu()
    }

    fun onSearchQueryUpdated(query: String) {
        searchViewModel!!.onQueryUpdated(query, viewModel.threadId)
        binding.searchBottomBar.showLoading()
        adapter.onSearchQueryUpdated(query)
    }

    override fun onSearchMoveUpPressed() {
        this.searchViewModel!!.onMoveUp()
    }

    override fun onSearchMoveDownPressed() {
        this.searchViewModel!!.onMoveDown()
    }

    override fun onAttachmentChanged() {
    }


    // region Animation & Updating
    override fun onModified(recipient: Recipient) {
        this.activity?.runOnUiThread {
            val threadRecipient = viewModel.recipient.value ?: return@runOnUiThread
            if (threadRecipient.isContactRecipient) {
                binding.blockedBanner.isVisible = threadRecipient.isBlocked
                callShowPayAsYouChatBDXIcon(threadRecipient)
                showBlockProgressBar(threadRecipient)
            }
            //New Line v32
            setUpMessageRequestsBar()
            this.activity?.invalidateOptionsMenu()
            updateSubtitle()
            showOrHideInputIfNeeded()
            binding?.profilePictureView?.root?.update(threadRecipient)
            //New Line v32
            binding?.conversationTitleView?.text = when {
                threadRecipient.isLocalNumber -> getString(R.string.note_to_self)
                else -> threadRecipient.toShortString()
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return ConversationLoader(
            viewModel.threadId,
            !viewModel.isIncomingMessageRequestThread(),
            requireActivity()
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        adapter.changeCursor(cursor)
        if (cursor != null) {
            val messageTimestamp = messageToScrollTimestamp.getAndSet(-1)
            val author = messageToScrollAuthor.getAndSet(null)
            if (author != null && messageTimestamp >= 0) {
                jumpToMessage(author, messageTimestamp, null)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.changeCursor(null)
    }

    /*Hales63*/
    private fun setUpRecyclerView() {
        binding.conversationRecyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.VERTICAL,
            !viewModel.isIncomingMessageRequestThread()
        )
        binding.conversationRecyclerView.layoutManager = layoutManager
        // Workaround for the fact that CursorRecyclerViewAdapter doesn't auto-update automatically (even though it says it will)
        LoaderManager.getInstance(this).restartLoader(0, null, this)
        binding.conversationRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                handleRecyclerViewScrolled()
            }
        })

        binding.conversationRecyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            showScrollToBottomButtonIfApplicable()
        }
    }

    private fun setUpToolBar() {
        val recipient = viewModel.recipient.value ?: return
        binding.conversationTitleView.text = when {
            recipient.isLocalNumber -> getString(R.string.note_to_self)
            else -> recipient.toShortString()
        }
        @DimenRes val sizeID: Int = if (recipient.isClosedGroupRecipient) {
            R.dimen.medium_profile_picture_size
        } else {
            R.dimen.small_profile_picture_size
        }
        val size = resources.getDimension(sizeID).roundToInt()
        binding.profilePictureView.root.layoutParams = LinearLayout.LayoutParams(size, size)
        binding.profilePictureView.root.glide = glide
        MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(
            viewModel.threadId,
            requireActivity()
        )
        binding.profilePictureView.root.update(recipient)
        binding.layoutConversation.setOnClickListener()
        {
            ConversationMenuHelper.showAllMedia(recipient, listenerCallback)
        }
        binding.backToHomeBtn.setOnClickListener {
            listenerCallback?.walletOnBackPressed()
        }

    }

    private fun backToHome() {
        val homeFragment: Fragment = HomeFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(
                R.id.activity_home_frame_layout_container,
                homeFragment,
                HomeFragment::class.java.name
            ).commit()
    }

    private fun setUpInputBar() {
        binding.inputBar.delegate = this
        binding.inputBarRecordingView.delegate = this
        // GIF button
        binding.gifButtonContainer.addView(gifButton)
        gifButton.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        gifButton.onUp = { showGIFPicker() }
        gifButton.snIsEnabled = false
        // Document button
        binding.documentButtonContainer.addView(documentButton)
        documentButton.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        documentButton.onUp = { showDocumentPicker() }
        documentButton.snIsEnabled = false
        // Library button
        binding.libraryButtonContainer.addView(libraryButton)
        libraryButton.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        libraryButton.onUp = { pickFromLibrary() }
        libraryButton.snIsEnabled = false
        // Camera button
        binding.cameraButtonContainer.addView(cameraButton)
        cameraButton.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        cameraButton.onUp = { showCamera() }
        cameraButton.snIsEnabled = false
    }

    private fun setUpLinkPreviewObserver() {
        if (!listenerCallback!!.gettextSecurePreferences().isLinkPreviewsEnabled()) {
            linkPreviewViewModel.onUserCancel(); return
        }
        linkPreviewViewModel.linkPreviewState.observe(requireActivity()) { previewState: LinkPreviewViewModel.LinkPreviewState? ->
            if (previewState == null) return@observe
            when {
                previewState.isLoading -> {
                    //New Line
                    val params =
                        binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin = 20

                    binding.inputBar.draftLinkPreview()
                }
                previewState.linkPreview.isPresent -> {
                    //New Line
                    val params =
                        binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin = 20

                    binding.inputBar.updateLinkPreviewDraft(glide, previewState.linkPreview.get())
                }
                else -> {
                    //New Line
                    val params =
                        binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin = 16

                    binding.inputBar.cancelLinkPreviewDraft(2)
                }
            }
        }
    }

    private fun restoreDraftIfNeeded() {
        //SetDataAndType
        val mediaURI = requireArguments().parcelable<Uri>(URI)
        val mediaType = AttachmentManager.MediaType.from(requireArguments().getString(TYPE))
        if (mediaURI != null && mediaType != null) {
            if (AttachmentManager.MediaType.IMAGE == mediaType || AttachmentManager.MediaType.GIF == mediaType || AttachmentManager.MediaType.VIDEO == mediaType) {
                val media = Media(
                    mediaURI,
                    MediaUtil.getMimeType(
                        requireActivity(),
                        mediaURI
                    )!!,
                    0,
                    0,
                    0,
                    0,
                    Optional.absent(),
                    Optional.absent()
                )
                startActivityForResult(
                    MediaSendActivity.buildEditorIntent(
                        requireActivity(),
                        listOf(media),
                        viewModel.recipient.value!!,
                        ""
                    ), PICK_FROM_LIBRARY
                )
                return
            } else {
                prepMediaForSending(mediaURI, mediaType).addListener(object :
                    ListenableFuture.Listener<Boolean> {

                    override fun onSuccess(result: Boolean?) {
                        sendAttachments(attachmentManager.buildSlideDeck().asAttachments(), null)
                    }

                    override fun onFailure(e: ExecutionException?) {
                        Toast.makeText(
                            requireActivity(),
                            R.string.activity_conversation_attachment_prep_failed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
                return
            }
        } else if (!requireArguments().getCharSequence(Intent.EXTRA_TEXT).isNullOrEmpty()) {
            val dataTextExtra =
                requireArguments().getCharSequence(Intent.EXTRA_TEXT) ?: ""
            binding.inputBar.text = dataTextExtra.toString()
        } else {
            viewModel.getDraft()?.let { text ->
                binding.inputBar.text = text
            }
        }
    }

    private fun setUpUiStateObserver() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { uiState ->
                uiState.uiMessages.firstOrNull()?.let {
                    Toast.makeText(requireActivity(), it.message, Toast.LENGTH_LONG).show()
                    viewModel.messageShown(it.id)
                }
                addOpenGroupGuidelinesIfNeeded(uiState.isBeldexHostedOpenGroup)
                if (uiState.isMessageRequestAccepted == true) {
                    binding.messageRequestBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setMediaControlForReportIssue() {
        val recipient = viewModel.recipient.value ?: return
        if (recipient.address.toString() == HomeActivity.reportIssueBChatID) {
            binding.inputBar.showMediaControls = true
        }
    }

    private fun updateUnreadCountIndicator() {
        val formattedUnreadCount = if (unreadCount < 10000) unreadCount.toString() else "9999+"
        binding.unreadCountTextView.text = formattedUnreadCount
        val textSize = if (unreadCount < 10000) 12.0f else 9.0f
        binding.unreadCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
        binding.unreadCountTextView.setTypeface(
            Typeface.DEFAULT,
            if (unreadCount < 100) Typeface.BOLD else Typeface.NORMAL
        )
        binding.unreadCountIndicator.isVisible = (unreadCount != 0)
    }

    private fun setUpTypingObserver(thread: Recipient) {
        ApplicationContext.getInstance(requireActivity()).typingStatusRepository.getTypists(
            viewModel.threadId
        ).observe(requireActivity()) { state ->
            val recipients = if (state != null) state.typists else listOf()
            val viewContainer = binding.typingIndicatorViewContainer
            viewContainer.isVisible = recipients.isNotEmpty() && isScrolledToBottom
            viewContainer.setTypists(recipients)
            inputBarHeightChanged(binding.inputBar.height)
        }
        if (listenerCallback!!.gettextSecurePreferences().isTypingIndicatorsEnabled()) {
            binding.inputBar.addTextChangedListener(object : SimpleTextWatcher() {

                override fun onTextChanged(text: String?) {
                    ApplicationContext.getInstance(requireActivity()).typingStatusSender.onTypingStarted(
                        viewModel.threadId
                    )
                    checkInputBarTextOnTextChanged(text,thread)
                }
            })
        } else {
            binding.inputBar.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(text: String?) {
                   checkInputBarTextOnTextChanged(text,thread)
                }
            })
        }
    }

    private fun checkInputBarTextOnTextChanged(text: String?,thread: Recipient){
        if (TextSecurePreferences.isPayAsYouChat(requireActivity())) {
            if (text!!.isNotEmpty() && text.matches(Regex("^(([0-9]{0,9})?|[.][0-9]{0,5})?|([0-9]{0,9}+([.][0-9]{0,5}))\$")) && binding.inputBar.quote == null) {
                binding.inputBar.setTextColor(thread,HomeActivity.reportIssueBChatID,true)
                showPayWithSlide(thread,true)
            } else {
                binding.inputBar.setTextColor(thread,HomeActivity.reportIssueBChatID,false)
                showPayWithSlide(thread,false)
            }
        }
    }

    private fun setUpRecipientObserver() {
        viewModel.recipient.value?.addListener(this)
    }

    private fun updateSubtitle() {
        val recipient = viewModel.recipient.value ?: return
        binding.muteIconImageView.isVisible = recipient.isMuted
        binding.conversationSubtitleView.isVisible = true
        if (recipient.isMuted) {
            if (recipient.mutedUntil != Long.MAX_VALUE) {
                binding.conversationSubtitleView.text = getString(
                    R.string.ConversationActivity_muted_until_date,
                    DateUtils.getFormattedDateTime(
                        recipient.mutedUntil,
                        "EEE, MMM d, yyyy HH:mm",
                        Locale.getDefault()
                    )
                )
            } else {
                binding.conversationSubtitleView.text =
                    getString(R.string.ConversationActivity_muted_forever)
            }
        } else if (recipient.isGroupRecipient) {
            try {
                val openGroup = viewModel.getOpenGroupChat()
                if (openGroup != null) {
                    val userCount = viewModel.getUserCount(openGroup)
                    try {
                        if (userCount != null) {
                            binding.conversationSubtitleView.text =
                                    getString(R.string.ConversationActivity_member_count, userCount)
                        } else {
                            binding.conversationSubtitleView.isVisible = false
                        }
                    } catch (ex: IllegalStateException) {
                        Timber.w(ex.message)
                    }
                } else {
                    binding.conversationSubtitleView.isVisible = false
                }
            } catch (ex: NullPointerException) {
                Timber.tag("Exception ").d(ex.message.toString())
            }
        } else {
            binding.conversationSubtitleView.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            return false
        } else if (item.itemId == R.id.menu_call) {
            val recipient = viewModel.recipient.value ?: return false
            if (recipient.isContactRecipient && recipient.isBlocked) {
                BlockedDialog(recipient).show(
                    requireActivity().supportFragmentManager,
                    "Blocked Dialog"
                )
            } else {
                if (Helper.getPhoneStatePermission(requireActivity())) {
                    isMenuCall()
                } else {
                    Timber.tag("Beldex").d("Permission not granted")
                }
            }
        }
        return viewModel.recipient.value?.let { recipient ->
            ConversationMenuHelper.onOptionItemSelected(
                requireActivity(),
                this,
                item,
                recipient,
                listenerCallback
            )
        } ?: false
    }

    private fun isMenuCall() {
        if (CheckOnline.isOnline(requireActivity().applicationContext)) {
            val tm = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (ContextCompat.checkSelfPermission(
                    requireActivity().applicationContext,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    tm.registerTelephonyCallback(
                        requireActivity().applicationContext.mainExecutor,
                        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                            override fun onCallStateChanged(state: Int) {
                                when (state) {
                                    TelephonyManager.CALL_STATE_RINGING -> {
                                        Toast.makeText(
                                            requireActivity().applicationContext,
                                            getString(R.string.call_alert),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                                        Toast.makeText(
                                            requireActivity().applicationContext,
                                            getString(R.string.call_alert),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    }
                                    TelephonyManager.CALL_STATE_IDLE -> {
                                        viewModel.recipient.value?.let { recipient ->
                                            call(requireActivity().applicationContext, recipient)
                                        }
                                    }
                                }
                            }
                        })

                } else {
                    tm.listen(object : PhoneStateListener() {
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            super.onCallStateChanged(state, phoneNumber)
                            when (state) {
                                TelephonyManager.CALL_STATE_RINGING -> {
                                    Toast.makeText(
                                        requireActivity().applicationContext,
                                        getString(R.string.call_alert),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                TelephonyManager.CALL_STATE_OFFHOOK -> {
                                    Toast.makeText(
                                        requireActivity().applicationContext,
                                        getString(R.string.call_alert),
                                        Toast.LENGTH_SHORT
                                    ).show()

                                }
                                TelephonyManager.CALL_STATE_IDLE -> {
                                    viewModel.recipient.value?.let { recipient ->
                                        call(requireActivity(), recipient)
                                    }
                                }
                            }
                        }
                    }, PhoneStateListener.LISTEN_CALL_STATE)
                }
            } else {
                Timber.tag("Beldex").d("Call state issue called else")
            }

        } else {
            Toast.makeText(
                requireActivity().applicationContext,
                getString(R.string.please_check_your_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun call(context: Context, thread: Recipient) {

        if (!TextSecurePreferences.isCallNotificationsEnabled(context)) {
            //SteveJosephh22
            val factory = LayoutInflater.from(context)
            val callPermissionDialogView: View =
                factory.inflate(R.layout.call_permissions_dialog_box, null)
            val callPermissionDialog = AlertDialog.Builder(requireActivity()).create()
            callPermissionDialog.setView(callPermissionDialogView)
            callPermissionDialogView.findViewById<TextView>(R.id.settingsDialogBoxButton)
                .setOnClickListener {
                    val intent = Intent(requireActivity(), PrivacySettingsActivity::class.java)
                    this.activity?.startActivity(intent)
                    callPermissionDialog.dismiss()
                }
            callPermissionDialogView.findViewById<TextView>(R.id.cancelDialogBoxButton)
                .setOnClickListener {
                    callPermissionDialog.dismiss()
                }
            callPermissionDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            callPermissionDialog.show()
            return
        }

        val service = WebRtcCallService.createCall(context, thread)
        context.startService(service)

        val activity = Intent(context, WebRtcCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(activity)

    }

    private fun getLatestOpenGroupInfoIfNeeded() {
        try {
            val openGroup = viewModel.getOpenGroupChat()
                    ?: return
            OpenGroupAPIV2.getMemberCount(openGroup.room, openGroup.server)
                .successUi { updateSubtitle() }
        } catch (ex: NullPointerException) {
            Timber.tag("Exception ").d(ex.message.toString())
        }
    }

    private fun setUpBlockedBanner() {
        val recipient = viewModel.recipient.value ?: return
        if (recipient.isGroupRecipient) {
            return
        }
        val contact = viewModel.getContactWithBChatId()
        val name = contact?.displayName(Contact.ContactContext.REGULAR) ?: recipient.address.toString()
        binding.blockedBannerTextView.text =
            resources.getString(R.string.activity_conversation_blocked_banner_text, name)
        binding.blockedBanner.isVisible = recipient.isBlocked
        callShowPayAsYouChatBDXIcon(recipient)
        showBlockProgressBar(recipient)
        binding.blockedBanner.setOnClickListener { viewModel.unblock() }
        binding.unblockButton.setOnClickListener {
            viewModel.unblock()
            viewModel.recipient.value?.let { thread ->
                showBlockProgressBar(thread)
            }
        }
    }

    // region Search
    private fun setUpSearchResultObserver() {
        searchViewModel!!.searchResults.observe(
            requireActivity(),
            Observer { result: SearchViewModel.SearchResult? ->
                if (result == null) return@Observer
                if (result.getResults().isNotEmpty()) {
                    result.getResults()[result.position]?.let {
                        jumpToMessage(
                            it.messageRecipient.address,
                            it.sentTimestampMs,
                            Runnable { searchViewModel!!.onMissingResult() })
                    }
                }
                binding.searchBottomBar.setData(result.position, result.getResults().size)
            })
    }


    private fun scrollToFirstUnreadMessageIfNeeded() {
        val lastSeenTimestamp = viewModel.getLastSeenAndHasSent().first()
        val lastSeenItemPosition = adapter.findLastSeenItemPosition(lastSeenTimestamp) ?: return
        if (lastSeenItemPosition <= 3) {
            return
        }
        binding.conversationRecyclerView.scrollToPosition(lastSeenItemPosition)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val recipient = viewModel.recipient.value ?: return
        //New Line
        if (!isMessageRequestThread()) {
            ConversationMenuHelper.onPrepareOptionsMenu(
                menu,
                requireActivity().menuInflater,
                recipient,
                viewModel.threadId,
                requireActivity().applicationContext,
                this
            ) { onOptionsItemSelected(it) }
        }
        super.onPrepareOptionsMenu(menu)
    }

    private fun showOrHideInputIfNeeded() {
        val recipient = viewModel.recipient.value
        if (recipient != null && recipient.isClosedGroupRecipient && mContext != null) {
            val group = viewModel.getGroup(recipient)
            val isActive = (group?.isActive == true)
            binding.inputBar.showInput = isActive
        } else {
            binding.inputBar.showInput = true
        }
    }

    /*Hales63*/
    private fun setUpMessageRequestsBar() {
        val recipient = viewModel.recipient.value ?: return
        if (recipient.address.toString() != HomeActivity.reportIssueBChatID) {
            binding.inputBar.showMediaControls = !isOutgoingMessageRequestThread()
        }
        binding.messageRequestBar.isVisible = viewModel.isIncomingMessageRequestThread()
        binding.acceptMessageRequestButton.setOnClickListener {
            acceptAlertDialog()
        }
        binding.messageRequestBlock.setOnClickListener {
            block(deleteThread = true)
        }
        binding.declineMessageRequestButton.setOnClickListener {
            declineAlertDialog()
        }
    }

    private fun hideVoiceMessageUI() {
        val chevronImageView = binding.inputBarRecordingView.chevronImageView
        val slideToCancelTextView = binding.inputBarRecordingView.slideToCancelTextView
        listOf(chevronImageView, slideToCancelTextView).forEach { view ->
            val animation = ValueAnimator.ofObject(FloatEvaluator(), view.translationX, 0.0f)
            animation.duration = 250L
            animation.addUpdateListener { animator ->
                view.translationX = animator.animatedValue as Float
            }
            animation.start()
        }
        binding.inputBarRecordingView.hide()
    }

    private fun isOutgoingMessageRequestThread(): Boolean {
        val recipient = viewModel.recipient.value ?: return false
        return !recipient.isGroupRecipient &&
                !recipient.isLocalNumber &&
                !(recipient.hasApprovedMe() || viewModel.hasReceived())
    }

    private fun showOrHideMentionCandidatesIfNeeded(text: CharSequence) {
        if (text.length < previousText.length) {
            currentMentionStartIndex = -1
            hideMentionCandidates()
            val mentionsToRemove = mentions.filter { !text.contains(it.displayName) }
            mentions.removeAll(mentionsToRemove)
        }
        if (text.isNotEmpty()) {
            val lastCharIndex = text.lastIndex
            val lastChar = text[lastCharIndex]
            // Check if there is whitespace before the '@' or the '@' is the first character
            val isCharacterBeforeLastWhiteSpaceOrStartOfLine: Boolean = if (text.length == 1) {
                true // Start of line
            } else {
                val charBeforeLast = text[lastCharIndex - 1]
                Character.isWhitespace(charBeforeLast)
            }
            if (lastChar == '@' && isCharacterBeforeLastWhiteSpaceOrStartOfLine) {
                currentMentionStartIndex = lastCharIndex
                showOrUpdateMentionCandidatesIfNeeded()
            } else if (Character.isWhitespace(lastChar) || lastChar == '@') { // the lastCharacter == "@" is to check for @@
                currentMentionStartIndex = -1
                hideMentionCandidates()
            } else if (currentMentionStartIndex != -1) {
                val query =
                    text.substring(currentMentionStartIndex + 1) // + 1 to get rid of the "@"
                showOrUpdateMentionCandidatesIfNeeded(query)
            }
        } else {
            currentMentionStartIndex = -1
            hideMentionCandidates()
        }
        previousText = text
    }

    private fun isValidLockViewLocation(x: Int, y: Int): Boolean {
        // We can be anywhere above the lock view and a bit to the side of it (at most `lockViewHitMargin`
        // to the side)
        val binding = binding
        val lockViewLocation = IntArray(2) { 0 }
        binding.inputBarRecordingView.lockView.getLocationOnScreen(lockViewLocation)
        val hitRect = Rect(
            lockViewLocation[0] - lockViewHitMargin,
            0,
            lockViewLocation[0] + binding.inputBarRecordingView.lockView.width + lockViewHitMargin,
            lockViewLocation[1] + binding.inputBarRecordingView.lockView.height
        )
        return hitRect.contains(x, y)
    }

    override fun showVoiceMessageUI() {
        //New Line
        binding.inputBar.visibility = View.GONE

        binding.inputBarRecordingView.show()
        binding.inputBarCard.alpha = 0.0f
        binding.inputBar.alpha = 0.0f
        val animation = ValueAnimator.ofObject(FloatEvaluator(), 1.0f, 0.0f)
        animation.duration = 250L
        animation.addUpdateListener { animator ->
            binding.inputBar.alpha = animator.animatedValue as Float
        }
        animation.start()
    }

    private fun expandVoiceMessageLockView() {
        val lockView = binding.inputBarRecordingView.lockView
        val animation = ValueAnimator.ofObject(FloatEvaluator(), lockView.scaleX, 1.10f)
        animation.duration = 250L
        animation.addUpdateListener { animator ->
            lockView.scaleX = animator.animatedValue as Float
            lockView.scaleY = animator.animatedValue as Float
        }
        animation.start()
    }

    private fun collapseVoiceMessageLockView() {
        val lockView = binding.inputBarRecordingView.lockView
        val animation = ValueAnimator.ofObject(FloatEvaluator(), lockView.scaleX, 1.0f)
        animation.duration = 250L
        animation.addUpdateListener { animator ->
            lockView.scaleX = animator.animatedValue as Float
            lockView.scaleY = animator.animatedValue as Float
        }
        animation.start()
    }

    private fun sendAttachments(
        attachments: List<Attachment>,
        body: String?,
        quotedMessage: MessageRecord? = null,
        linkPreview: LinkPreview? = null
    ) {
        val recipient = viewModel.recipient.value ?: return
        //New Line v32
        processMessageRequestApproval()

        // Create the message
        val message = VisibleMessage()
        message.sentTimestamp = MnodeAPI.nowWithOffset
        message.text = body
        val quote = quotedMessage?.let {
            val quotedAttachments =
                (it as? MmsMessageRecord)?.slideDeck?.asAttachments() ?: listOf()
            val sender =
                if (it.isOutgoing) Address.fromSerialized(
                    listenerCallback!!.gettextSecurePreferences().getLocalNumber()!!
                ) else it.individualRecipient.address
            //Payment Tag
            var quoteBody = it.body
            if (it.isPayment) {
                //Payment Tag
                var amount = ""
                try {
                    val mainObject = JSONObject(it.body)
                    val uniObject = mainObject.getJSONObject("kind")
                    amount = uniObject.getString("amount")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                val direction: String = if (it.isOutgoing) {
                    resources.getString(R.string.payment_sent)
                } else {
                    resources.getString(R.string.payment_received)
                }
                quoteBody =
                    resources.getString(R.string.reply_payment_card_message, direction, amount)
            } else if (it.isOpenGroupInvitation) {
                quoteBody = resources.getString(R.string.ThreadRecord_open_group_invitation)
            }
            QuoteModel(it.dateSent, sender, quoteBody, false, quotedAttachments)
        }
        val outgoingTextMessage =
            OutgoingMediaMessage.from(message, recipient, attachments, quote, linkPreview)
        // Clear the input bar
        binding.inputBar.text = ""
        //New Line
        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 16

        binding.inputBar.cancelQuoteDraft(2)
        binding.inputBar.cancelLinkPreviewDraft(2)
        // Clear mentions
        previousText = ""
        currentMentionStartIndex = -1
        mentions.clear()
        // Reset the attachment manager
        attachmentManager.clear()
        // Reset attachments button if needed
        if (isShowingAttachmentOptions) {
            toggleAttachmentOptions()
        }
        // Put the message in the database
        message.id = viewModel.insertMessageOutBox(outgoingTextMessage)
        // Send it
        MessageSender.send(message, recipient.address, attachments, quote, linkPreview)
        // Send a typing stopped message
        ApplicationContext.getInstance(requireActivity()).typingStatusSender.onTypingStopped(
            viewModel.threadId
        )
    }

    private fun sendTextOnlyMessage(hasPermissionToSendSeed: Boolean = false) {
        val recipient = viewModel.recipient.value ?: return
        //New Line v32
        processMessageRequestApproval()

        val text = getMessageBody()
        val userPublicKey = listenerCallback!!.gettextSecurePreferences().getLocalNumber()
        val isNoteToSelf =
            (recipient.isContactRecipient && recipient.address.toString() == userPublicKey)
        if (text.contains(seed) && !isNoteToSelf && !hasPermissionToSendSeed) {
            val dialog = SendSeedDialog { sendTextOnlyMessage(true) }
            return dialog.show(requireActivity().supportFragmentManager, "Send Seed Dialog")
        }
        // Create the message
        val message = VisibleMessage()
        message.sentTimestamp = MnodeAPI.nowWithOffset
        message.text = text
        val outgoingTextMessage = OutgoingTextMessage.from(message, viewModel.recipient.value)
        // Clear the input bar
        binding.inputBar.text = ""
        //New Line
        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 16

        binding.inputBar.cancelQuoteDraft(2)
        binding.inputBar.cancelLinkPreviewDraft(2)
        // Clear mentions
        previousText = ""
        currentMentionStartIndex = -1
        mentions.clear()
        // Put the message in the database
        message.id = viewModel.insertMessageOutBoxSMS(outgoingTextMessage, message.sentTimestamp)

        // Send it
        MessageSender.send(message, recipient.address)
        // Send a typing stopped message
        ApplicationContext.getInstance(requireActivity()).typingStatusSender.onTypingStopped(
            viewModel.threadId
        )
    }

    //New Line v32
    private fun processMessageRequestApproval() {
        if (viewModel.isIncomingMessageRequestThread()) {
            acceptMessageRequest()
        } else if (viewModel.recipient.value?.isApproved == false) {
            // edge case for new outgoing thread on new recipient without sending approval messages
            viewModel.setRecipientApproved()
        }
    }

    // region General
    private fun getMessageBody(): String {
        var result = binding.inputBar.text.trim()
        for (mention in mentions) {
            try {
                val startIndex = result.indexOf("@" + mention.displayName)
                val endIndex =
                    startIndex + mention.displayName.count() + 1 // + 1 to include the "@"
                result =
                    result.substring(0, startIndex) + "@" + mention.publicKey + result.substring(
                        endIndex
                    )
            } catch (exception: Exception) {
                Timber.tag("Beldex").d("Failed to process mention due to error: $exception")
            }
        }
        return result
    }

    private fun acceptMessageRequest() {
        binding.messageRequestBar.isVisible = false
        binding.conversationRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, true)
        //New Line 1
        adapter.notifyDataSetChanged()
        viewModel.acceptMessageRequest()
        //New Line 1
        LoaderManager.getInstance(this).restartLoader(0, null, this)
        lifecycleScope.launch(Dispatchers.IO) {
            ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(requireActivity())
        }
    }

    // Remove this after the unsend request is enabled
    private fun deleteMessagesWithoutUnsendRequest(messages: Set<MessageRecord>) {
        val messageCount = messages.size
        val builder = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
        builder.setTitle(
            resources.getQuantityString(
                R.plurals.ConversationFragment_delete_selected_messages,
                messageCount,
                messageCount
            )
        )
        builder.setMessage(
            resources.getQuantityString(
                R.plurals.ConversationFragment_this_will_permanently_delete_all_n_selected_messages,
                messageCount,
                messageCount
            )
        )
        builder.setCancelable(true)
        builder.setPositiveButton(R.string.delete) { _, _ ->
            viewModel.deleteMessagesWithoutUnsendRequest(messages)
            endActionMode()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            endActionMode()
        }
        builder.show()
    }

    private fun endActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    private fun jumpToMessage(author: Address, timestamp: Long, onMessageNotFound: Runnable?) {
        SimpleTask.run(lifecycle, {
            viewModel.getMessagePositionInConversation(timestamp, author)
        }) { p: Int -> moveToMessagePosition(p, onMessageNotFound) }
    }

    private fun handleRecyclerViewScrolled() {
        val binding = binding
        val wasTypingIndicatorVisibleBefore = binding.typingIndicatorViewContainer.isVisible
        binding.typingIndicatorViewContainer.isVisible =
            wasTypingIndicatorVisibleBefore && isScrolledToBottom
        val isTypingIndicatorVisibleAfter = binding.typingIndicatorViewContainer.isVisible
        if (isTypingIndicatorVisibleAfter != wasTypingIndicatorVisibleBefore) {
            inputBarHeightChanged(binding.inputBar.height)
        }
        showScrollToBottomButtonIfApplicable()
        val firstVisiblePosition = layoutManager?.findFirstVisibleItemPosition() ?: -1
        unreadCount = min(unreadCount, firstVisiblePosition).coerceAtLeast(0)
        updateUnreadCountIndicator()
    }

    private fun showScrollToBottomButtonIfApplicable() {
        binding.scrollToBottomButton.isVisible = !isScrolledToBottom && adapter.itemCount > 0
    }

    private fun moveToMessagePosition(position: Int, onMessageNotFound: Runnable?) {
        if (position >= 0) {
            binding.conversationRecyclerView.scrollToPosition(position)
        } else {
            onMessageNotFound?.run()
        }
    }

    private fun sendMediaSavedNotification() {
        val recipient = viewModel.recipient.value ?: return
        if (recipient.isGroupRecipient) {
            return
        }
        val timestamp = MnodeAPI.nowWithOffset
        val kind = DataExtractionNotification.Kind.MediaSaved(timestamp)
        val message = DataExtractionNotification(kind)
        MessageSender.send(message, recipient.address)
    }

    private fun showGIFPicker() {
        val hasSeenGIFMetaDataWarning: Boolean =
            listenerCallback!!.gettextSecurePreferences().hasSeenGIFMetaDataWarning()
        if (!hasSeenGIFMetaDataWarning) {
            val builder = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            builder.setTitle("Search GIFs?")
            builder.setMessage("You will not have full metadata protection when sending GIFs.")
            builder.setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                listenerCallback!!.gettextSecurePreferences().setHasSeenGIFMetaDataWarning()
                AttachmentManager.selectGif(requireActivity(), PICK_GIF)
                dialog.dismiss()
            }
            builder.setNegativeButton(
                "Cancel"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            builder.create().show()
        } else {
            AttachmentManager.selectGif(requireActivity(), PICK_GIF)
        }
    }

    private fun showDocumentPicker() {
        AttachmentManager.selectDocument(requireActivity(), PICK_DOCUMENT)
    }

    private fun pickFromLibrary() {
        val recipient = viewModel.recipient.value ?: return
        binding.inputBar.text.trim().let { text ->
            AttachmentManager.selectGallery(
                requireActivity(),
                PICK_FROM_LIBRARY, recipient, text
            )
        }
    }

    private fun showCamera() {
        attachmentManager.capturePhoto(requireActivity(), TAKE_PHOTO, viewModel.recipient.value)
    }

    private fun hideMentionCandidates() {
        if (isShowingMentionCandidatesView) {
            val mentionCandidatesView = mentionCandidatesView ?: return
            val animation =
                ValueAnimator.ofObject(FloatEvaluator(), mentionCandidatesView.alpha, 0.0f)
            animation.duration = 250L
            animation.addUpdateListener { animator ->
                mentionCandidatesView.alpha = animator.animatedValue as Float
                if (animator.animatedFraction == 1.0f) {
                    binding.additionalContentContainer.removeAllViews()
                }
            }
            animation.start()
        }
        isShowingMentionCandidatesView = false
    }

    private fun showOrUpdateMentionCandidatesIfNeeded(query: String = "") {
        val additionalContentContainer = binding.additionalContentContainer
        val recipient = viewModel.recipient.value ?: return
        if (!isShowingMentionCandidatesView) {
            additionalContentContainer.removeAllViews()
            val view = MentionCandidatesView(requireActivity())
            view.glide = glide
            view.onCandidateSelected = { handleMentionSelected(it) }
            additionalContentContainer.addView(view)
            val candidates = MentionsManager.getMentionCandidates(
                query,
                viewModel.threadId,
                recipient.isOpenGroupRecipient
            )
            this.mentionCandidatesView = view
            view.show(candidates, viewModel.threadId)
        } else {
            val candidates = MentionsManager.getMentionCandidates(
                query,
                viewModel.threadId,
                recipient.isOpenGroupRecipient
            )
            this.mentionCandidatesView!!.setMentionCandidates(candidates)
        }
        isShowingMentionCandidatesView = true
    }

    /*Hales63*/
    private fun acceptAlertDialog() {
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog)
            .setMessage(resources.getString(R.string.message_requests_accept_message))
            .setPositiveButton(R.string.accept) { _, _ ->
                acceptMessageRequest()
                viewModel.recipient.value?.let {
                    showBlockProgressBar(it)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                // Do nothing
            }.show()

        //SteveJosephh21
        val textView: TextView? = dialog.findViewById(android.R.id.message)
        val face: Typeface =
            Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    private fun declineAlertDialog() {
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_Clear_All)
            .setMessage(resources.getString(R.string.message_requests_decline_message))
            .setPositiveButton(R.string.decline) { _, _ ->
                viewModel.declineMessageRequest()
                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(requireActivity())
                }
                //finish()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                // Do nothing
            }.show()

        //SteveJosephh21
        val textView: TextView? = dialog.findViewById(android.R.id.message)
        val face: Typeface =
            Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    private fun handleMentionSelected(mention: Mention) {
        val binding = binding
        if (currentMentionStartIndex == -1) {
            return
        }
        mentions.add(mention)
        val previousText = binding.inputBar.text
        val newText =
            previousText.substring(0, currentMentionStartIndex) + "@" + mention.displayName + " "
        binding.inputBar.text = newText
        binding.inputBar.setSelection(newText.length)
        currentMentionStartIndex = -1
        hideMentionCandidates()
        this.previousText = newText
    }

    private fun addOpenGroupGuidelinesIfNeeded(isBeldexHostedOpenGroup: Boolean) {
        if (!isBeldexHostedOpenGroup) {
            return
        }
        binding.openGroupGuidelinesView.visibility = View.VISIBLE
        val recyclerViewLayoutParams =
            binding.conversationRecyclerView.layoutParams as RelativeLayout.LayoutParams
        recyclerViewLayoutParams.topMargin = toPx(
            5,
            resources
        ) // The height of the social group guidelines view is hardcoded to this
        binding.conversationRecyclerView.layoutParams = recyclerViewLayoutParams
    }

    private fun isMessageRequestThread(): Boolean {
        //New Line v32
        val recipient = viewModel.recipient.value ?: return false
        return !recipient.isGroupRecipient && !recipient.isApproved
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    //Payment Tag
    override fun sendBDX() {
        val txData: TxData = getTxData()
        txData.destinationAddress = senderBeldexAddress
        txData.destinationAddress?.let { Timber.tag("SenderBeldexAddress txData->").d(it) }
        if (getCleanAmountString(getBDXAmount()).equals(
                Wallet.getDisplayAmount(totalFunds)
            )
        ) {
            val amount = (totalFunds - 10485760)// 10485760 == 050000000
            val bdx = getCleanAmountString(getBDXAmount())
            if (bdx != null) {
                txData.amount = amount
            } else {
                txData.amount = 0L
            }

        } else {
            val bdx =
                getCleanAmountString(getBDXAmount())
            if (bdx != null) {
                txData.amount = Wallet.getAmountFromString(bdx)
            } else {
                txData.amount = 0L
            }
        }
        txData.userNotes = UserNotes("-")
        if (TextSecurePreferences.getFeePriority(requireActivity()) == 0) {
            txData.priority = PendingTransaction.Priority.Priority_Slow
        } else {
            txData.priority = PendingTransaction.Priority.Priority_Flash
        }
        txData.mixin = mixin
        //Important
        val lockManager: LockManager<CustomPinActivity> =
            LockManager.getInstance() as LockManager<CustomPinActivity>
        lockManager.enableAppLock(requireActivity(), CustomPinActivity::class.java)
        val intent = Intent(requireActivity(), CustomPinActivity::class.java)
        intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN)
        intent.putExtra("change_pin", false)
        intent.putExtra("send_authentication", true)
        resultLaunchers.launch(intent)
        // Clear the input bar
        binding.inputBar.text = ""
    }

    override fun sendFailed(errorText: String?) {
        val transactionLoadingBar: Fragment? =
            requireActivity().supportFragmentManager.findFragmentByTag("transaction_progressbar_tag")
        if (transactionLoadingBar != null) {
            val df: DialogFragment = transactionLoadingBar as DialogFragment
            try {
                df.dismiss()
            } catch (e: IllegalStateException) {
                return
            }
        }
        //sendButtonEnabled()
        showAlert(getString(R.string.send_create_tx_error_title), errorText!!)
    }

    override fun createTransactionFailed(errorText: String?) {
        hideProgress()
        if(getString(R.string.invalid_destination_address) == errorText!!){
            showAlert(getString(R.string.send_create_tx_error_title), getString(R.string.receiver_address_is_not_available))
        }else{
            showAlert(getString(R.string.send_create_tx_error_title), errorText)
        }
    }

    override fun transactionCreated(txTag: String?, pendingTransaction: PendingTransaction?) {
        // ignore txTag - the app flow ensures this is the correct tx
        hideProgress()
        if (isResume) {
            this.pendingTransaction = pendingTransaction
            refreshTransactionDetails()
        } else {
            this.disposeTransaction()
        }
    }

    // callbacks from send service
    fun onTransactionCreated(txTag: String?, pendingTransaction: PendingTransaction?) {
        pendingTx = PendingTx(pendingTransaction)
        transactionCreated(txTag, pendingTransaction)
    }

    fun onCreateTransactionFailed(errorText: String?) {
        createTransactionFailed(errorText)
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(
            requireActivity(), R.style.backgroundColor
        )
        builder.setCancelable(true).setTitle(title).setMessage(message).create().show()
        transactionInProgress = false
    }

    private fun disposeTransaction() {
        pendingTx = null
        listenerCallback!!.onDisposeRequest()
    }

    private var inProgress = false

    //Minimized app
    private var onTransactionProgress = false

    private fun hideProgress() {
        val transactionLoadingBar: Fragment? =
            requireActivity().supportFragmentManager.findFragmentByTag("transaction_progressbar_tag")
        if (transactionLoadingBar != null) {
            val df: DialogFragment = transactionLoadingBar as DialogFragment
            try {
                df.dismiss()
            } catch (e: IllegalStateException) {
                //Minimized app
                onTransactionProgress = true
                return
            }
        }
        inProgress = false
    }

    private fun showProgress() {
        TransactionLoadingBar().show(
            requireActivity().supportFragmentManager,
            "transaction_progressbar_tag"
        )
        inProgress = true
    }

    private fun refreshTransactionDetails() {
        if (pendingTransaction != null) {
            val txData: TxData = getTxData()
            try {
                if (pendingTransaction!!.firstTxId != null) {
                    InChatSend(
                        pendingTransaction!!,
                        txData,
                        this
                    ).show(requireActivity().supportFragmentManager, "")
                }
            } catch (e: IllegalStateException) {
                //Minimized app
                onTransactionProgress = true
                return
            } catch (e: IndexOutOfBoundsException) {
                //Minimized app
                hideProgress()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_try_again_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getCleanAmountString(enteredAmount: String): String? {
        return try {
            val amount = enteredAmount.toDouble()
            if (amount >= 0) {
                String.format(Locale.US, cleanFormat, amount)
            } else {
                null
            }
        } catch (ex: NumberFormatException) {
            null
        }
    }

    private val resultLaunchers =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onResumeFragment()
            }
        }

    private fun onResumeFragment() {
        Helper.hideKeyboard(activity)
        isResume = true
        transactionInProgress = true
        refreshTransactionDetails()
        if (pendingTransaction == null && !inProgress) {
            showProgress()
            prepareSend(txData)
        }
    }

    private fun prepareSend(txData: TxData?) {
        listenerCallback!!.onPrepareSend(null, txData)
    }

    fun send() {
        commitTransaction()
        //Insert Recipient Address
        if (TextSecurePreferences.getSaveRecipientAddress(requireActivity())) {
            val insertRecipientAddress =
                DatabaseComponent.get(requireActivity()).bchatRecipientAddressDatabase()
            try {
                if (pendingTransaction!!.firstTxId != null) {
                    insertRecipientAddress.insertRecipientAddress(
                        pendingTransaction!!.firstTxId,
                        txData.destinationAddress
                    )
                }
            } catch (e: IndexOutOfBoundsException) {
                e.message?.let { Timber.tag("ConversationFragmentV2->").d(it) }
            }
        }
        showProgress()
    }

    private fun commitTransaction() {
        listenerCallback!!.onSend(txData.userNotes)
        committedTx = pendingTx
    }


    private fun getBDXAmount(): String {
        sendBDXAmount = binding.inputBar.text.trim()
        return sendBDXAmount as String
    }

    //If Transaction successfully completed after call this function
    fun onTransactionSent(txId: String?) {
        hideProgress()
        //Payment Tag
        viewModel.sentPayment(sendBDXAmount.toString(), txId, viewModel.recipient.value)
        processMessageRequestApproval()
        InChatSendSuccess(this).show(requireActivity().supportFragmentManager, "")
    }

    fun setProgress(text: String?) {
        //WalletFragment Functionality
        try {
            if (text == getString(R.string.reconnecting) || text == getString(R.string.status_wallet_loading) || text == getString(
                    R.string.status_wallet_connecting
                )
            ) {
                binding.inputBar.setDrawableProgressBar(requireActivity().applicationContext, false,valueOfWallet)
            }
            syncText = text
        } catch (ex: IllegalStateException) {
            Timber.tag("Exception").d(ex.toString())
        }
    }

    fun setProgress(n: Int) {
        syncProgress = n
        when {
            n > 100 -> {
                //binding.blockProgressBar.isIndeterminate = true
                binding.inputBar.showProgressBar(blockProgressBarVisible)
            }
            n >= 0 -> {
                //binding.blockProgressBar.isIndeterminate = false
                binding.inputBar.setProgress(n)//binding.blockProgressBar.progress = n
                binding.inputBar.showProgressBar(blockProgressBarVisible)
            }
            n == -2 -> {
                binding.inputBar.showProgressBar(blockProgressBarVisible)
                //binding.blockProgressBar.isIndeterminate = false
                binding.inputBar.setProgress(100)
            }
            else -> { // <0
                binding.inputBar.showProgressBar(false)
            }
        }
    }

    fun onRefreshed(wallet: Wallet, full: Boolean) {
        val recipient = viewModel.recipient.value ?: return
        if (!recipient.isGroupRecipient && recipient.hasApprovedMe() && !recipient.isBlocked && HomeActivity.reportIssueBChatID != recipient.address.toString() && !recipient.isLocalNumber) {
            if (full && listenerCallback!!.isSynced) {
                if (CheckOnline.isOnline(requireContext())) {
                    check(listenerCallback!!.hasBoundService()) { "WalletService not bound." }
                    val daemonConnected: Wallet.ConnectionStatus = listenerCallback!!.connectionStatus!!
                    if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
//                        AsyncGetUnlockedBalance(wallet).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
                        getUnlockedBalance(wallet)
                    }
                }
            }
            updateStatus(wallet)
        }
    }

    private fun updateStatus(wallet: Wallet) {
            if (!isAdded) return
            if (CheckOnline.isOnline(requireContext())) {
                val sync: String
                check(listenerCallback!!.hasBoundService()) { "WalletService not bound." }
                val daemonConnected: Wallet.ConnectionStatus = listenerCallback!!.connectionStatus!!
                if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                    val daemonHeight: Long = wallet.daemonBlockChainHeight
                    val walletHeight: Long = wallet.blockChainHeight
                    val df = DecimalFormat("#.##")
                    val walletSyncPercentage = ((100.00 * walletHeight.toDouble()) / daemonHeight)
                    if (!wallet.isSynchronized) {
                        ApplicationContext.getInstance(context).messageNotifier.setHomeScreenVisible(
                                true
                        )
                        val n = daemonHeight - walletHeight
                        sync = formatter.format(n) + " " + getString(R.string.status_remaining)
                        if (firstBlock == 0L) {
                            firstBlock = walletHeight
                        }
                        var x = (100 - Math.round(100f * n / (1f * daemonHeight - firstBlock))).toInt()
                        if (x == 0) x = 101 // indeterminate
                        setProgress(x)
                        valueOfWallet = "${df.format(walletSyncPercentage)}%"
                        binding.inputBar.setDrawableProgressBar(requireActivity().applicationContext, false, valueOfWallet)
                    } else {
                        balance = wallet.balance
                        ApplicationContext.getInstance(context).messageNotifier.setHomeScreenVisible(
                                false
                        )
                        sync = getString(R.string.status_synchronized)
                        valueOfWallet = "${df.format(walletSyncPercentage)}%"
                        binding.inputBar.setDrawableProgressBar(requireActivity().applicationContext, false, valueOfWallet)
                        //SteveJosephh21
                        setProgress(-2)
                    }
                } else {
                    sync = getString(R.string.failed_connected_to_the_node)
                    setProgress(-1)
                    valueOfWallet = "--"
                    binding.inputBar.setDrawableProgressBar(requireActivity().applicationContext, true, valueOfWallet)
                }
                setProgress(sync)
            } else {
                setProgress(getString(R.string.no_node_connection))
                valueOfWallet = "--"
                binding.inputBar.setDrawableProgressBar(requireActivity().applicationContext, true, valueOfWallet)
            }
            toolTip()
        }

    private fun refreshBalance(synchronized: Boolean) {
        refreshBalance(
            synchronized = synchronized,
            unlockedBalance = unlockedBalance,
            balance = balance,
        ) { bal, unlockedBal, sync ->
            showBalance(bal, unlockedBal, sync)
        }
    }

    private fun showBalance(
        walletBalance: String?,
        walletUnlockedBalance: String?,
        synchronized: Boolean
    ) {
        showBalance(
            walletBalance = walletBalance,
            walletUnlockedBalance = walletUnlockedBalance,
            synchronized = synchronized,
            activity = requireActivity(),
            mContext = mContext,
        ) { bal, unlockedBal ->
            valueOfBalance = bal
            unlockedBal?.let {
                valueOfUnLockedBalance = it
            }
        }
        toolTip()
    }

    private fun getUnlockedBalance(wallet: Wallet) {
        if (mContext != null && walletAvailableBalance != null) {
            if (walletAvailableBalance!!.replace(",", "").toDouble() > 0.0) {
                showBalance(walletAvailableBalance!!, unlockedBalance.toString(), true)
            }
        } else {
            refreshBalance(false)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                unlockedBalance = wallet.unlockedBalance
                delay(100)
                refreshBalance(wallet.isSynchronized)
            } catch (e: Exception) {
                Timber.tag("WalletFragment").d(e.toString())
            }
        }
    }

//    inner class AsyncGetUnlockedBalance(val wallet: Wallet) :
//        AsyncTaskCoroutine<Executor?, Boolean?>() {
//        override fun onPreExecute() {
//            super.onPreExecute()
//            if (mContext != null && walletAvailableBalance != null) {
//                if (walletAvailableBalance!!.replace(",", "").toDouble() > 0.0) {
//                    showBalance(walletAvailableBalance!!, unlockedBalance.toString(), true)
//                }
//            } else {
//                refreshBalance(false)
//            }
//        }
//
//        override fun doInBackground(vararg params: Executor?): Boolean {
//            try {
//                unlockedBalance = wallet.unlockedBalance
//            } catch (e: Exception) {
//                Timber.tag("WalletFragment").d(e.toString())
//            }
//            return true
//        }
//
//        override fun onPostExecute(result: Boolean?) {
//            refreshBalance(wallet.isSynchronized)
//        }
//    }

    private fun checkIfFragmentAttached(operation: Context.() -> Unit) {
        if (isAdded && context != null) {
            operation(requireContext())
        }
    }
}
//endregion
package com.thoughtcrimes.securesms.conversation.v2

import android.Manifest
import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DimenRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.annimon.stream.Stream
import com.beldex.libbchat.messaging.contacts.Contact
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
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.MediaTypes
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.concurrent.SimpleTask
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libbchat.utilities.recipients.RecipientModifiedListener
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.ListenableFuture
import com.beldex.libsignal.utilities.guava.Optional
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.ExpirationDialog
import com.thoughtcrimes.securesms.audio.AudioRecorder
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
import com.thoughtcrimes.securesms.conversation.v2.utilities.*
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.crypto.MnemonicUtilities
import com.thoughtcrimes.securesms.database.*
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.database.model.MmsMessageRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.linkpreview.LinkPreviewRepository
import com.thoughtcrimes.securesms.linkpreview.LinkPreviewUtil
import com.thoughtcrimes.securesms.linkpreview.LinkPreviewViewModel
import com.thoughtcrimes.securesms.mediasend.Media
import com.thoughtcrimes.securesms.mediasend.MediaSendActivity
import com.thoughtcrimes.securesms.mms.*
import com.thoughtcrimes.securesms.permissions.Permissions
import com.thoughtcrimes.securesms.util.*
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentConversationV2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.komponents.kovenant.ui.successUi
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.lifecycle.Observer
import com.thoughtcrimes.securesms.calls.WebRtcCallActivity
import com.thoughtcrimes.securesms.contacts.SelectContactsActivity
import com.thoughtcrimes.securesms.data.PendingTx
import com.thoughtcrimes.securesms.data.TxData
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.giph.ui.GiphyActivity
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.home.HomeFragment
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.model.PendingTransaction
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.preferences.ChatSettingsActivity
import com.thoughtcrimes.securesms.preferences.PrivacySettingsActivity
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.wallet.CheckOnline
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.send.interfaces.SendConfirm
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.CustomPinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.LockManager
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.lang.ClassCastException
import java.lang.NumberFormatException
import java.text.NumberFormat
import java.util.concurrent.Executor

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ConversationFragmentV2.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConversationFragmentV2 : Fragment(), InputBarDelegate,
    InputBarRecordingViewDelegate, AttachmentManager.AttachmentListener,
    ConversationActionModeCallbackDelegate, VisibleMessageContentViewDelegate,
    RecipientModifiedListener,
    SearchBottomBar.EventListener, LoaderManager.LoaderCallbacks<Cursor>,
    ConversationMenuHelper.ConversationMenuListener, OnBackPressedListener,SendConfirm {
    // TODO: Rename and change types of parameters

    private var param1: String? = null
    private var param2: String? = null

    lateinit var binding: FragmentConversationV2Binding

    @Inject
    lateinit var threadDb: ThreadDatabase

    @Inject
    lateinit var mmsSmsDb: MmsSmsDatabase

    @Inject
    lateinit var beldexThreadDb: BeldexThreadDatabase

    @Inject
    lateinit var bchatContactDb: BchatContactDatabase

    @Inject
    lateinit var groupDb: GroupDatabase

    @Inject
    lateinit var beldexApiDb: BeldexAPIDatabase

    @Inject
    lateinit var smsDb: SmsDatabase

    @Inject
    lateinit var mmsDb: MmsDatabase

    @Inject
    lateinit var beldexMessageDb: BeldexMessageDatabase


    @Inject
    lateinit var recipientDatabase: RecipientDatabase

    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val linkPreviewViewModel: LinkPreviewViewModel by lazy {
        ViewModelProvider(
            this, LinkPreviewViewModel.Factory(
                LinkPreviewRepository(requireActivity())
            )
        )
            .get(LinkPreviewViewModel::class.java)
    }

    var threadId: Long? = -1L

    private val viewModel: ConversationViewModel by viewModels {
        threadId = requireArguments().getLong(THREAD_ID,-1L)
        if (threadId == -1L) {
            requireArguments().getParcelable<Address>(ADDRESS)?.let { address ->
                val recipient = Recipient.from(requireActivity(), address, false)
                threadId = threadDb.getOrCreateThreadIdFor(recipient)
            }
        }
        listenerCallback!!.getConversationViewModel().create(threadId!!)
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
    /*private val searchViewModel: SearchViewModel by viewModels()*/
    var searchViewModel: SearchViewModel? = null
    var searchViewItem: MenuItem? = null


    private val isScrolledToBottom: Boolean
        get() {
            val position = layoutManager.findFirstCompletelyVisibleItemPosition()
            return position == 0
        }

    private val layoutManager: LinearLayoutManager
        get() {
            return binding.conversationRecyclerView.layoutManager as LinearLayoutManager
        }


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
        val cursor = mmsSmsDb.getConversation(viewModel.threadId, !isIncomingMessageRequestThread())
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
            glide,
            onDeselect = { message, position ->
                actionMode?.let {
                    onDeselect(message, position, it)
                }
            }
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ConversationFragmentV2.
         */
        // TODO: Rename and change types and number of parameters
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

        // Request codes
        const val PICK_DOCUMENT = 2
        const val TAKE_PHOTO = 7
        const val PICK_GIF = 10
        const val PICK_FROM_LIBRARY = 12
        const val INVITE_CONTACTS = 124

        //flag
        const val IS_UNSEND_REQUESTS_ENABLED = true
    }

    var listenerCallback: Listener? = null
    private var mContext: Context? = null

    var senderBeldexAddress: String? = null
    var sendBDXAmount: String? = null

    private fun getTxData(): TxData {
        return txData
    }

    private var txData = TxData()

    var pendingTransaction: PendingTransaction? = null
    var pendingTx: PendingTx? = null
    private var totalFunds: Long = 0
    val MIXIN = 0
    private var isResume:Boolean = false
    private val CLEAN_FORMAT = "%." + Helper.BDX_DECIMALS.toString() + "f"
    var committedTx: PendingTx? = null

    private var syncText: String? = null
    private var syncProgress = -1
    private var firstBlock: Long = 0
    private var balance: Long = 0
    private val formatter = NumberFormat.getInstance()
    private var walletAvailableBalance: String? =null
    private var unlockedBalance: Long = 0
    private var walletSynchronized:Boolean = false
    private var blockProgressBarVisible:Boolean = false

    
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            threadId = it.getLong(THREAD_ID)
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

        searchViewModel = ViewModelProvider(requireActivity()).get(SearchViewModel::class.java)
        val databaseHelper =
            DatabaseComponent.get(requireActivity().applicationContext).openHelper()
        audioRecorder = AudioRecorder(requireActivity().applicationContext)
        threadDb = ThreadDatabase(requireActivity().applicationContext, databaseHelper)
        mmsSmsDb = MmsSmsDatabase(requireActivity().applicationContext, databaseHelper)
        beldexThreadDb = BeldexThreadDatabase(requireActivity().applicationContext, databaseHelper)
        bchatContactDb = BchatContactDatabase(requireActivity().applicationContext, databaseHelper)
        groupDb = GroupDatabase(requireActivity().applicationContext, databaseHelper)
        beldexApiDb = BeldexAPIDatabase(requireActivity().applicationContext, databaseHelper)
        bchatContactDb = BchatContactDatabase(requireActivity().applicationContext, databaseHelper)
        smsDb = SmsDatabase(requireActivity().applicationContext, databaseHelper)
        mmsDb = MmsDatabase(requireActivity().applicationContext, databaseHelper)
        beldexMessageDb =
            BeldexMessageDatabase(requireActivity().applicationContext, databaseHelper)
        recipientDatabase = RecipientDatabase(requireContext().applicationContext, databaseHelper)

        val thread = threadDb.getRecipientForThreadId(viewModel.threadId)
        if (thread == null) {
            Toast.makeText(requireActivity(), "This thread has been deleted.", Toast.LENGTH_LONG)
                .show()
            return backToHome()
        }

        // messageIdToScroll
        messageToScrollTimestamp.set(requireArguments().getLong(SCROLL_MESSAGE_ID, -1))
        messageToScrollAuthor.set(requireArguments().getParcelable<Address>(SCROLL_MESSAGE_AUTHOR))

        if (!thread.isGroupRecipient && thread.hasApprovedMe()) {
           senderBeldexAddress =  getBeldexAddress(thread.address)
        }
        //AsyncGetUnlockedBalance(listenerCallback).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
        setUpRecyclerView()
        setUpToolBar()
        setUpInputBar()
        setUpLinkPreviewObserver()
        restoreDraftIfNeeded()
        setUpUiStateObserver()

        binding.scrollToBottomButton.setOnClickListener {

            val layoutManager =
                binding.conversationRecyclerView.layoutManager ?: return@setOnClickListener

            if (layoutManager.isSmoothScrolling) {
                binding.conversationRecyclerView.scrollToPosition(0)
            } else {
                binding.conversationRecyclerView.smoothScrollToPosition(0)
            }
        }

        unreadCount = mmsSmsDb.getUnreadCount(viewModel.threadId)
        updateUnreadCountIndicator()
        setUpTypingObserver()
        setUpRecipientObserver()
        updateSubtitle()
        getLatestOpenGroupInfoIfNeeded()
        setUpBlockedBanner()
        binding.searchBottomBar.setEventListener(this)
        setUpSearchResultObserver()
        scrollToFirstUnreadMessageIfNeeded()
        showOrHideInputIfNeeded()
        /*Hales63*/
        setUpMessageRequestsBar()

        viewModel.recipient?.let { recipient ->
            if (recipient.isOpenGroupRecipient) {
                val openGroup = beldexThreadDb.getOpenGroupChat(viewModel.threadId)
                if (openGroup == null) {
                    Log.d("Beldex", "Thread deleted toast called 1")
                    Toast.makeText(
                        requireContext(),
                        "This thread has been deleted.",
                        Toast.LENGTH_LONG
                    ).show()
                    //return finish()
                }
            }
        }
        showBlockProgressBar(thread)

        binding.conversationExpandableArrow.setOnClickListener {
            if (!binding.inChatWalletDetails.isVisible) {
                binding.inChatWalletDetails.visibility = View.VISIBLE
                binding.conversationExpandableArrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)
            } else if (binding.inChatWalletDetails.isVisible) {
                binding.inChatWalletDetails.visibility = View.GONE
                binding.conversationExpandableArrow.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
            }
        }

        showBalance(Helper.getDisplayAmount(0),Helper.getDisplayAmount(0),walletSynchronized)

    }

    override fun onResume() {
        super.onResume()
        ApplicationContext.getInstance(requireActivity()).messageNotifier.setVisibleThread(viewModel.threadId)
        val recipient = viewModel.recipient ?: return
        threadDb.markAllAsRead(viewModel.threadId, recipient.isOpenGroupRecipient)

        val thread = threadDb.getRecipientForThreadId(viewModel.threadId)
        showBlockProgressBar(thread)
    }

    private fun showBlockProgressBar(thread: Recipient?) {
        if (thread != null) {
            if (!thread.isGroupRecipient && thread.hasApprovedMe() && !thread.isBlocked && TextSecurePreferences.isPayAsYouChat(requireActivity())) {
                binding.blockProgressBar.visibility = View.VISIBLE
                binding.syncStatusLayout.visibility = View.VISIBLE
                blockProgressBarVisible = true
            } else{
                binding.blockProgressBar.visibility = View.GONE
                binding.syncStatusLayout.visibility = View.GONE
                blockProgressBarVisible = false
            }
        }
    }

    override fun onPause() {
        endActionMode()
        Log.d("Beldex", "OnPause called")
        ApplicationContext.getInstance(requireActivity()).messageNotifier.setVisibleThread(-1)
        viewModel.saveDraft(binding.inputBar.text.trim())
        val recipient = viewModel.recipient ?: return  super.onPause()
        /*Hales63*/ // New Line
        if (TextSecurePreferences.getPlayerStatus(requireActivity())) {
            TextSecurePreferences.setPlayerStatus(requireActivity(), false)
            val contactDB = DatabaseComponent.get(requireActivity()).bchatContactDatabase()
            val contact = contactDB.getContactWithBchatID(recipient.address.toString())
            if (contact?.isTrusted != null) {
                if (contact.isTrusted) {
                    val actionMode = this.actionMode
                    if (actionMode == null) {
                        if (selectedEvent != null && selectedView != null) {
                            selectedEvent?.let { selectedView?.onContentClick(it) }
                            if (selectedMessageRecord?.isOutgoing != null) {
                                if (selectedMessageRecord?.isOutgoing!!) {
                                    selectedEvent?.let { selectedView?.onContentClick(it) }
                                }
                            }
                        }
                    }
                }

            } else if (contact?.isTrusted == null && selectedMessageRecord?.isOutgoing == false) {
                val actionMode = this.actionMode
                if (actionMode == null) {
                    if (selectedEvent != null && selectedView != null) {
                        selectedEvent?.let { selectedView?.onContentClick(it) }
                    }
                }
            } // New Line Social Group Receiver Voice Message
            else if (contact?.isTrusted == null && selectedMessageRecord?.isOutgoing == false) {
                val actionMode = this.actionMode
                if (actionMode == null) {
                    if (selectedEvent != null && selectedView != null) {
                        selectedEvent?.let { selectedView?.onContentClick(it) }
                    }
                }

            }
            if (selectedMessageRecord?.isOutgoing != null) {
                if (selectedMessageRecord?.isOutgoing!!) {
                    val actionMode = this.actionMode
                    if (actionMode == null) {
                        if (selectedEvent != null && selectedView != null) {
                            selectedEvent?.let { selectedView?.onContentClick(it) }
                        }
                    }
                }

            }
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
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
    // endregion

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
            Log.d("Beldex", "handlePress if")
            onDeselect(message, position, actionMode)
        } else {
            Log.d("Beldex", "handlePress else")
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
        params.bottomMargin = 400
        val recipient = viewModel.recipient ?: return
        binding.inputBar.draftQuote(recipient, message, glide)
    }

    // `position` is the adapter position; not the visual position
    private fun handleLongPress(message: MessageRecord, position: Int) {
        val actionMode = this.actionMode
        val actionModeCallback =
            ConversationActionModeCallback(adapter, viewModel.threadId, requireActivity())
        actionModeCallback.delegate = this
        searchViewItem?.collapseActionView()
        if (actionMode == null) { // Nothing should be selected if this is the case
            adapter.toggleSelection(message, position)
            this.actionMode =
                requireActivity().startActionMode(actionModeCallback, ActionMode.TYPE_PRIMARY)
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
        val inputBarText =binding.inputBar?.text ?: return
        if (listenerCallback!!.gettextSecurePreferences().isLinkPreviewsEnabled()) {
            linkPreviewViewModel.onTextChanged(requireActivity(), inputBarText, 0, 0)
        }
        showOrHideMentionCandidatesIfNeeded(newContent)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        //-Log.d("-->onActivityResult boolean", "Done result"+resultCode.toString()+" requestcode "+requestCode.toString()+"")
        val mediaPreppedListener = object : ListenableFuture.Listener<Boolean> {

            override fun onSuccess(result: Boolean?) {
                //-Log.d("-->onActivityResult boolean", result.toString())
                sendAttachments(attachmentManager.buildSlideDeck().asAttachments(), null)
            }

            override fun onFailure(e: ExecutionException?) {
                Log.d("-->onActivityResult exception", e.toString())
                Toast.makeText(requireActivity(), R.string.activity_conversation_attachment_prep_failed, Toast.LENGTH_LONG).show()
            }
        }
        when (requestCode) {
            ConversationFragmentV2.PICK_DOCUMENT -> {
                Log.d("-->onActivityResult boolean", "PICK_DOCUMENT")
                val uri = intent?.data ?: return
                /*getImagePath(this,uri)?.let { Log.d("@--> uri get Image path", it) }
                val file = File(uri.path)
                Log.d("@--> uri ",file.absolutePath.toString())
                val file_size: Int = java.lang.String.valueOf(file.length() / 1024).toInt()
                Log.d("@--> uri ",file_size.toString())*/
                prepMediaForSending(uri, AttachmentManager.MediaType.DOCUMENT).addListener(mediaPreppedListener)
            }
            ConversationFragmentV2.PICK_GIF -> {
                intent ?: return
                val uri = intent.data ?: return
                val type = AttachmentManager.MediaType.GIF
                val width = intent.getIntExtra(GiphyActivity.EXTRA_WIDTH, 0)
                val height = intent.getIntExtra(GiphyActivity.EXTRA_HEIGHT, 0)
                prepMediaForSending(uri, type, width, height).addListener(mediaPreppedListener)
            }
            ConversationFragmentV2.PICK_FROM_LIBRARY,
            ConversationFragmentV2.TAKE_PHOTO -> {
                Log.d("TAKE_PHOTO","1")
                intent ?: return
                Log.d("TAKE_PHOTO","2")
                val body = intent.getStringExtra(MediaSendActivity.EXTRA_MESSAGE)
                val media = intent.getParcelableArrayListExtra<Media>(
                    MediaSendActivity.EXTRA_MEDIA) ?: return
                Log.d("TAKE_PHOTO","3")
                val slideDeck = SlideDeck()
                Log.d("TAKE_PHOTO","4")
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
                            Log.d("Beldex", "Asked to send an unexpected media type: '" + item.mimeType + "'. Skipping.")
                        }
                    }
                }
                sendAttachments(slideDeck.asAttachments(), body)
            }
            ConversationFragmentV2.INVITE_CONTACTS -> {
                if (viewModel.recipient?.isOpenGroupRecipient != true) { return }
                val extras = intent?.extras ?: return
                if (!intent.hasExtra(SelectContactsActivity.selectedContactsKey)) { return }
                val selectedContacts = extras.getStringArray(SelectContactsActivity.selectedContactsKey)!!
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
        Log.d("-->Doc 1", "true")
        return prepMediaForSending(uri, type, null, null)
    }

    private fun prepMediaForSending(
        uri: Uri,
        type: AttachmentManager.MediaType,
        width: Int?,
        height: Int?
    ): ListenableFuture<Boolean> {
        Log.d("-->Doc 2", "true")
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
        if (Permissions.hasAll(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
            showVoiceMessageUI()
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
    }

    override fun onMicrophoneButtonMove(event: MotionEvent) {
        val rawX = event.rawX
        val chevronImageView = binding.inputBarRecordingView.chevronImageView ?: return
        val slideToCancelTextView = binding.inputBarRecordingView.slideToCancelTextView ?: return
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
            val recordButtonOverlay = binding.inputBarRecordingView.recordButtonOverlay ?: return
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
        //New Line v32
        /* if (isIncomingMessageRequestThread()) {
             acceptMessageRequest()
         }*/
        val recipient = viewModel.recipient ?: return
        if (recipient.isContactRecipient && recipient.isBlocked) {
            BlockedDialog(recipient).show(
                requireActivity().supportFragmentManager,
                "Blocked Dialog"
            )
            return
        }
        val binding = binding ?: return
        if (binding.inputBar.linkPreview != null || binding.inputBar.quote != null) {
            sendAttachments(
                listOf(),
                getMessageBody(),
                binding.inputBar.quote,
                binding.inputBar.linkPreview
            )
        } else {
           /* if (binding.inputBar.text.length > 4096) {
                Toast.makeText(
                    requireActivity(),
                    "Text limit exceed: Maximum limit of messages is 4096 characters",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sendTextOnlyMessage()
            }*/
                if(TextSecurePreferences.isPayAsYouChat(requireActivity())) {
                    if (binding.inputBar.text.matches(Regex("^[0-9]*\\.?[0-9]*\$"))) {
                        if (binding.syncStatus.text == getString(R.string.status_synchronized)) {
                            sendBDX()
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "Blocks are syncing wait until finished",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        callSendTextOnlyMessage()
                    }
                }else{
                   callSendTextOnlyMessage()
                }
        }
    }

    fun callSendTextOnlyMessage(){
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
        val dialog = android.app.AlertDialog.Builder(requireActivity())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.pay_as_you_chat, null)
        dialog.setView(dialogView)

        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val enableInstruction = dialogView.findViewById<TextView>(R.id.payAsYouChatEnable_Instruction)
        val alert = dialog.create()
        alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alert.setCanceledOnTouchOutside(false)
        alert.show()
        val sourceString = "Enable pay as you chat from <b>My Account -> Chat Settings -> Pay As You Chat</b> to use this option"
        enableInstruction.text = Html.fromHtml(sourceString)
        okButton.setOnClickListener {
            val intent = Intent(requireActivity(), ChatSettingsActivity::class.java)
            requireActivity().startActivity(intent)
            alert.dismiss()
        }
        cancelButton.setOnClickListener {
            alert.dismiss()
        }
    }

    override fun commitInputContent(contentUri: Uri) {
        val recipient = viewModel.recipient ?: return
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
        val inputBar =binding.inputBar ?: return
        val inputBarCard =binding.inputBarCard ?: return
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
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        audioRecorder.stopRecording()
        stopAudioHandler.removeCallbacks(stopVoiceMessageRecordingTask)
    }

    override fun deleteMessages(messages: Set<MessageRecord>) {
        val recipient = viewModel.recipient ?: return
        if (!IS_UNSEND_REQUESTS_ENABLED) {
            deleteMessagesWithoutUnsendRequest(messages)
            return
        }
        val allSentByCurrentUser = messages.all { it.isOutgoing }
        val allHasHash = messages.all { beldexMessageDb.getMessageServerHash(it.id) != null }
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
            val body = MentionUtilities.highlightMentions(
                message.body,
                viewModel.threadId,
                requireActivity()
            )
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
        val bchatID = messages.first().individualRecipient.address.toString()
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
        val recipient = viewModel.recipient ?: return
        //New Line
        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 400

        binding.inputBar.draftQuote(recipient, messages.first(), glide)
        endActionMode()
    }

    //SteveJosephh21 - 08
    override fun block(deleteThread: Boolean) {
        Log.d("test-","true")
        val title = R.string.RecipientPreferenceActivity_block_this_contact_question
        val message =
            R.string.RecipientPreferenceActivity_you_will_no_longer_receive_messages_and_calls_from_this_contact
        val dialog = AlertDialog.Builder(requireActivity(), R.style.BChatAlertDialog_Clear_All)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.RecipientPreferenceActivity_block) { _, _ ->
                viewModel.block()
                if (deleteThread) {
                    viewModel.deleteThread()
                    //finish()
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
            val group = groupDb.getGroup(thread.address.toGroupString()).orNull()
            if (group?.isActive == false) {
                return
            }
        }
        ExpirationDialog.show(requireActivity(), thread.expireMessages) { expirationTime: Int ->
            recipientDatabase.setExpireMessages(thread, expirationTime)
            val message = ExpirationTimerUpdate(expirationTime)
            message.recipient = thread.address.serialize()
            message.sentTimestamp = System.currentTimeMillis()
            val expiringMessageManager =
                ApplicationContext.getInstance(requireActivity()).expiringMessageManager
            expiringMessageManager.setExpirationTimer(message)
            MessageSender.send(message, thread.address)
            requireActivity().invalidateOptionsMenu()
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
            binding.conversationRecyclerView.findViewHolderForAdapterPosition(indexInAdapter) as? ConversationAdapter.VisibleMessageViewHolder
        viewHolder?.view?.playVoiceMessage()
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
        requireActivity().invalidateOptionsMenu()
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
            val threadRecipient = viewModel.recipient ?: return@runOnUiThread
            if (threadRecipient.isContactRecipient) {
                binding.blockedBanner.isVisible = threadRecipient.isBlocked
            }
            //New Line v32
            setUpMessageRequestsBar()
            requireActivity().invalidateOptionsMenu()
            updateSubtitle()
            showOrHideInputIfNeeded()
            binding.profilePictureView.update(recipient)
            //New Line v32
            binding.conversationTitleView.text = recipient.toShortString()
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return ConversationLoader(
            viewModel.threadId,
            !isIncomingMessageRequestThread(),
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
            !isIncomingMessageRequestThread()
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
    }

    private fun setUpToolBar() {
        //test
        val recipient = viewModel.recipient ?: return
        binding.conversationTitleView.text = recipient.toShortString()
        @DimenRes val sizeID: Int = if (recipient.isClosedGroupRecipient) {
            R.dimen.medium_profile_picture_size
        } else {
            R.dimen.small_profile_picture_size
        }
        val size = resources.getDimension(sizeID).roundToInt()
        binding.profilePictureView.layoutParams = LinearLayout.LayoutParams(size, size)
        binding.profilePictureView.glide = glide
        MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(
            viewModel.threadId,
            requireActivity()
        )
        binding.profilePictureView.update(recipient)
        binding.layoutConversation.setOnClickListener()
        {
            ConversationMenuHelper.showAllMedia(requireActivity(), recipient)
        }
        binding.backToHomeBtn.setOnClickListener{
            listenerCallback?.walletOnBackPressed()
        }

    }

    private fun backToHome(){
        val homeFragment: Fragment = HomeFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.activity_home_frame_layout_container, homeFragment, HomeFragment::class.java.name).commit()
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
                    params.bottomMargin = 440

                    binding.inputBar.draftLinkPreview()
                }
                previewState.linkPreview.isPresent -> {
                    //New Line
                    val params =
                        binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin = 440

                    binding.inputBar.updateLinkPreviewDraft(glide, previewState.linkPreview.get())
                }
                else -> {
                    //New Line
                    val params =
                        binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin = 220

                    binding.inputBar.cancelLinkPreviewDraft(2)
                }
            }
        }
    }

    private fun restoreDraftIfNeeded() {
        val mediaURI = requireActivity().intent.data
        val mediaType = AttachmentManager.MediaType.from(requireActivity().intent.type)
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
                        viewModel.recipient!!,
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
        } else if (requireActivity().intent.hasExtra(Intent.EXTRA_TEXT)) {
            val dataTextExtra =
                requireActivity().intent.getCharSequenceExtra(Intent.EXTRA_TEXT) ?: ""
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

    private fun updateUnreadCountIndicator() {
        val binding = binding ?: return
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

    private fun setUpTypingObserver() {
        ApplicationContext.getInstance(requireActivity()).typingStatusRepository.getTypists(
            viewModel.threadId
        ).observe(requireActivity()) { state ->
            val recipients = if (state != null) state.typists else listOf()
            // FIXME: Also checking isScrolledToBottom is a quick fix for an issue where the
            //        typing indicator overlays the recycler view when scrolled up
            val viewContainer = binding.typingIndicatorViewContainer ?: return@observe
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
                }
            })
        }else{
            binding.inputBar.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(text: String?) {
                    if(TextSecurePreferences.isPayAsYouChat(requireActivity())){
                        if(text!!.isNotEmpty() && text.matches(Regex("^[0-9]*\\.?[0-9]*\$"))){
                            binding.inputBar.showPayAsYouChatBDXIcon(true)
                        }else{
                            binding.inputBar.showPayAsYouChatBDXIcon(false)
                        }
                    }
                }
            })
        }
    }

    private fun setUpRecipientObserver() {
        viewModel.recipient?.addListener(this)
    }

    private fun updateSubtitle() {
        val recipient = viewModel.recipient ?: return
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
                binding.conversationSubtitleView.text = requireActivity().getString(R.string.ConversationActivity_muted_forever)
            }
        } else if (recipient.isGroupRecipient) {
            val openGroup = beldexThreadDb.getOpenGroupChat(viewModel.threadId)
            if (openGroup != null) {
                val userCount = beldexApiDb.getUserCount(openGroup.room, openGroup.server) ?: 0
                binding.conversationSubtitleView.text = requireActivity().getString(R.string.ConversationActivity_member_count, userCount)
            } else {
                binding.conversationSubtitleView.isVisible = false
            }
        } else {
            binding.conversationSubtitleView.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            return false
        } else if (item.itemId == R.id.menu_call) {
            val recipient = viewModel.recipient ?: return false
            if (recipient.isContactRecipient && recipient.isBlocked) {
                BlockedDialog(recipient).show(requireActivity().supportFragmentManager, "Blocked Dialog")
            } else {
                if (Helper.getPhoneStatePermission(requireActivity())) {
                    isMenuCall()
                } else {
                    Log.d("Beldex", "Permission not granted")
                }
            }
        }
        return  viewModel.recipient?.let { recipient ->
            ConversationMenuHelper.onOptionItemSelected(requireActivity(),this, item, recipient)
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
                                            getString(R.string.call_alert_while_ringing),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                                        Toast.makeText(
                                            requireActivity().applicationContext,
                                            getString(R.string.call_alert_while_on_going),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    }
                                    TelephonyManager.CALL_STATE_IDLE -> {
                                        viewModel.recipient?.let { recipient ->
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
                                        getString(R.string.call_alert_while_ringing),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                TelephonyManager.CALL_STATE_OFFHOOK -> {
                                    Toast.makeText(
                                        requireActivity().applicationContext,
                                        getString(R.string.call_alert_while_on_going),
                                        Toast.LENGTH_SHORT
                                    ).show()

                                }
                                TelephonyManager.CALL_STATE_IDLE -> {
                                    viewModel.recipient?.let { recipient ->
                                        call(requireActivity(), recipient)
                                    }
                                }
                            }
                        }
                    }, PhoneStateListener.LISTEN_CALL_STATE)
                }
            } else {
                Log.d("Beldex", "Call state issue called else")
            }

        } else {
            Toast.makeText(requireActivity().applicationContext, "Check your Internet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun call(context: Context, thread: Recipient) {

        if (!TextSecurePreferences.isCallNotificationsEnabled(context)) {
            /* AlertDialog.Builder(context)
                 .setTitle(R.string.ConversationActivity_call_title)
                 .setMessage(R.string.ConversationActivity_call_prompt)
                 .setPositiveButton(R.string.activity_settings_title) { _, _ ->
                     val intent = Intent(context, PrivacySettingsActivity::class.java)
                     context.startActivity(intent)
                 }
                 .setNeutralButton(R.string.cancel) { d, _ ->
                     d.dismiss()
                 }.show()*/
            //SteveJosephh22
            val factory = LayoutInflater.from(context)
            val callPermissionDialogView: View = factory.inflate(R.layout.call_permissions_dialog_box, null)
            val callPermissionDialog = AlertDialog.Builder(context).create()
            callPermissionDialog.setView(callPermissionDialogView)
            callPermissionDialogView.findViewById<TextView>(R.id.settingsDialogBoxButton).setOnClickListener{
                val intent = Intent(context, PrivacySettingsActivity::class.java)
                context.startActivity(intent)
                callPermissionDialog.dismiss()
            }
            callPermissionDialogView.findViewById<TextView>(R.id.cancelDialogBoxButton).setOnClickListener{
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
        val openGroup = beldexThreadDb.getOpenGroupChat(viewModel.threadId) ?: return
        OpenGroupAPIV2.getMemberCount(openGroup.room, openGroup.server)
            .successUi { updateSubtitle() }
    }

    private fun setUpBlockedBanner() {
        val recipient = viewModel.recipient ?: return
        if (recipient.isGroupRecipient) {
            return
        }
        val bchatID = recipient.address.toString()
        val contact = bchatContactDb.getContactWithBchatID(bchatID)
        val name = contact?.displayName(Contact.ContactContext.REGULAR) ?: bchatID
        binding.blockedBannerTextView.text =
            resources.getString(R.string.activity_conversation_blocked_banner_text, name)
        binding.blockedBanner.isVisible = recipient.isBlocked
        binding.blockedBanner.setOnClickListener { viewModel.unblock() }
        binding.unblockButton.setOnClickListener {
            val thread = threadDb.getRecipientForThreadId(viewModel.threadId)
            showBlockProgressBar(thread)
            viewModel.unblock()
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
                            it.receivedTimestampMs,
                            Runnable { searchViewModel!!.onMissingResult() })
                    }
                }
                binding.searchBottomBar.setData(result.position, result.getResults().size)
            })
    }


    private fun scrollToFirstUnreadMessageIfNeeded() {
        val lastSeenTimestamp = threadDb.getLastSeenAndHasSent(viewModel.threadId).first()
        val lastSeenItemPosition = adapter.findLastSeenItemPosition(lastSeenTimestamp) ?: return
        if (lastSeenItemPosition <= 3) {
            return
        }
        binding.conversationRecyclerView.scrollToPosition(lastSeenItemPosition)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val recipient = viewModel.recipient ?: return
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
        val recipient = viewModel.recipient
        if (recipient != null && recipient.isClosedGroupRecipient) {
            val group = groupDb.getGroup(recipient.address.toGroupString()).orNull()
            val isActive = (group?.isActive == true)
            binding.inputBar.showInput = isActive
        } else {
            binding.inputBar.showInput = true
        }
    }

    /*Hales63*/
    private fun setUpMessageRequestsBar() {
        binding.inputBar.showMediaControls = !isOutgoingMessageRequestThread()
        binding.messageRequestBar.isVisible = isIncomingMessageRequestThread()
        binding.acceptMessageRequestButton.setOnClickListener {
            acceptAlertDialog()
        }
        binding.messageRequestBlock.setOnClickListener {
            block(deleteThread = true)
        }
        binding.declineMessageRequestButton.setOnClickListener {
            /*viewModel.declineMessageRequest()
                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@ConversationActivityV2)
                }
                finish()*/
            declineAlertDialog()
        }
    }

    private fun hideVoiceMessageUI() {
        val chevronImageView =binding.inputBarRecordingView?.chevronImageView ?: return
        val slideToCancelTextView =binding.inputBarRecordingView?.slideToCancelTextView ?: return
        listOf(chevronImageView, slideToCancelTextView).forEach { view ->
            val animation = ValueAnimator.ofObject(FloatEvaluator(), view.translationX, 0.0f)
            animation.duration = 250L
            animation.addUpdateListener { animator ->
                view.translationX = animator.animatedValue as Float
            }
            animation.start()
        }
       binding.inputBarRecordingView?.hide()
    }

    private fun isIncomingMessageRequestThread(): Boolean {
        val recipient = viewModel.recipient ?: return false
        return !recipient.isGroupRecipient &&
                !recipient.isApproved &&
                !recipient.isLocalNumber &&
                !threadDb.getLastSeenAndHasSent(viewModel.threadId).second() &&
                threadDb.getMessageCount(viewModel.threadId) > 0
    }

    private fun isOutgoingMessageRequestThread(): Boolean {
        val recipient = viewModel.recipient ?: return false
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
            val isCharacterBeforeLastWhiteSpaceOrStartOfLine: Boolean
            if (text.length == 1) {
                isCharacterBeforeLastWhiteSpaceOrStartOfLine = true // Start of line
            } else {
                val charBeforeLast = text[lastCharIndex - 1]
                isCharacterBeforeLastWhiteSpaceOrStartOfLine =
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
        val binding = binding ?: return false
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
        val lockView =binding.inputBarRecordingView?.lockView ?: return
        val animation = ValueAnimator.ofObject(FloatEvaluator(), lockView.scaleX, 1.10f)
        animation.duration = 250L
        animation.addUpdateListener { animator ->
            lockView.scaleX = animator.animatedValue as Float
            lockView.scaleY = animator.animatedValue as Float
        }
        animation.start()
    }

    private fun collapseVoiceMessageLockView() {
        val lockView =binding.inputBarRecordingView?.lockView ?: return
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
        val recipient = viewModel.recipient ?: return
        //New Line v32
        processMessageRequestApproval()

        // Create the message
        val message = VisibleMessage()
        message.sentTimestamp = System.currentTimeMillis()
        message.text = body
        val quote = quotedMessage?.let {
            val quotedAttachments =
                (it as? MmsMessageRecord)?.slideDeck?.asAttachments() ?: listOf()
            val sender =
                if (it.isOutgoing) Address.fromSerialized(
                    listenerCallback!!.gettextSecurePreferences().getLocalNumber()!!
                ) else it.individualRecipient.address
            var quoteBody = it.body
            if(it.isPayment){
                Log.d("QuoteModel->1","${it.body}")
                //Payment Tag
                var amount = ""
                var direction = ""
                try {
                    val mainObject: JSONObject = JSONObject(it.body)
                    val uniObject = mainObject.getJSONObject("kind")
                    amount = uniObject.getString("amount")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                direction = if (it.isOutgoing) {
                    resources.getString(R.string.payment_sent)
                } else {
                    resources.getString(R.string.payment_received)
                }
                quoteBody = resources.getString(R.string.reply_payment_card_message,direction,amount)
            }
            QuoteModel(it.dateSent, sender, quoteBody, false, quotedAttachments)
        }
        val outgoingTextMessage =
            OutgoingMediaMessage.from(message, recipient, attachments, quote, linkPreview)
        // Clear the input bar
        binding.inputBar.text = ""
        //New Line
        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 220

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
        message.id = mmsDb.insertMessageOutbox(outgoingTextMessage, viewModel.threadId, false) { }
        // Send it
        MessageSender.send(message, recipient.address, attachments, quote, linkPreview)
        // Send a typing stopped message
        ApplicationContext.getInstance(requireActivity()).typingStatusSender.onTypingStopped(
            viewModel.threadId
        )
    }

    private fun sendTextOnlyMessage(hasPermissionToSendSeed: Boolean = false) {
        val recipient = viewModel.recipient ?: return
        //New Line v32
        processMessageRequestApproval()

        val text = getMessageBody()
        Log.d("Beldex", "bchat id validation -- get bchat id")
        val userPublicKey = listenerCallback!!.gettextSecurePreferences().getLocalNumber()
        val isNoteToSelf =
            (recipient.isContactRecipient && recipient.address.toString() == userPublicKey)
        if (text.contains(seed) && !isNoteToSelf && !hasPermissionToSendSeed) {
            val dialog = SendSeedDialog { sendTextOnlyMessage(true) }
            return dialog.show(requireActivity().supportFragmentManager, "Send Seed Dialog")
        }
        // Create the message
        val message = VisibleMessage()
        message.sentTimestamp = System.currentTimeMillis()
        message.text = text
        val outgoingTextMessage = OutgoingTextMessage.from(message, viewModel.recipient)
        // Clear the input bar
        binding.inputBar.text = ""
        //New Line
        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = 220

        binding.inputBar.cancelQuoteDraft(2)
        binding.inputBar.cancelLinkPreviewDraft(2)
        // Clear mentions
        previousText = ""
        currentMentionStartIndex = -1
        mentions.clear()
        Log.d("Beldex", "thread ID value ${viewModel.threadId}")
        //-Log.d("Beldex","recipient ID value ${viewModel.recipient.address}")
        // Put the message in the database
        message.id = smsDb.insertMessageOutbox(
            viewModel.threadId,
            outgoingTextMessage,
            false,
            message.sentTimestamp!!,
            null
        )

        // Send it
        Log.d("Beldex", "bchat id validation -- message send using bchat id")
        MessageSender.send(message, recipient.address)
        // Send a typing stopped message
        ApplicationContext.getInstance(requireActivity()).typingStatusSender.onTypingStopped(
            viewModel.threadId
        )
    }

    //New Line v32
    private fun processMessageRequestApproval() {
        if (isIncomingMessageRequestThread()) {
            acceptMessageRequest()
        } else if (viewModel.recipient?.isApproved == false) {
            // edge case for new outgoing thread on new recipient without sending approval messages
            viewModel.setRecipientApproved()
        }
    }

    // region General
    private fun getMessageBody(): String {
        var result = binding.inputBar.text.trim() ?: return ""
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
                Log.d("Beldex", "Failed to process mention due to error: $exception")
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
    fun deleteMessagesWithoutUnsendRequest(messages: Set<MessageRecord>) {
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
            mmsSmsDb.getMessagePositionInConversation(viewModel.threadId, timestamp, author)
        }) { p: Int -> moveToMessagePosition(p, onMessageNotFound) }
    }

    private fun handleRecyclerViewScrolled() {
        // FIXME: Checking isScrolledToBottom is a quick fix for an issue where the
        //        typing indicator overlays the recycler view when scrolled up
        val binding = binding ?: return
        val wasTypingIndicatorVisibleBefore = binding.typingIndicatorViewContainer.isVisible
        binding.typingIndicatorViewContainer.isVisible =
            wasTypingIndicatorVisibleBefore && isScrolledToBottom
        val isTypingIndicatorVisibleAfter = binding.typingIndicatorViewContainer.isVisible
        if (isTypingIndicatorVisibleAfter != wasTypingIndicatorVisibleBefore) {
            inputBarHeightChanged(binding.inputBar.height)
        }
        binding.scrollToBottomButton.isVisible = !isScrolledToBottom
        unreadCount = min(unreadCount, layoutManager.findFirstVisibleItemPosition())
        updateUnreadCountIndicator()
    }

    private fun moveToMessagePosition(position: Int, onMessageNotFound: Runnable?) {
        if (position >= 0) {
            binding.conversationRecyclerView.scrollToPosition(position)
        } else {
            onMessageNotFound?.run()
        }
    }

    private fun sendMediaSavedNotification() {
        val recipient = viewModel.recipient ?: return
        if (recipient.isGroupRecipient) {
            return
        }
        val timestamp = System.currentTimeMillis()
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
        val recipient = viewModel.recipient ?: return
        binding.inputBar.text.trim().let { text ->
            AttachmentManager.selectGallery(
                requireActivity(),
                PICK_FROM_LIBRARY, recipient, text
            )
        }
    }

    private fun showCamera() {
        attachmentManager.capturePhoto(requireActivity(), TAKE_PHOTO, viewModel.recipient);
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
                   binding.additionalContentContainer?.removeAllViews()
                }
            }
            animation.start()
        }
        isShowingMentionCandidatesView = false
    }

    private fun showOrUpdateMentionCandidatesIfNeeded(query: String = "") {
        val additionalContentContainer =binding.additionalContentContainer ?: return
        val recipient = viewModel.recipient ?: return
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
        val binding = binding ?: return
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
            57,
            resources
        ) // The height of the social group guidelines view is hardcoded to this
        binding.conversationRecyclerView.layoutParams = recyclerViewLayoutParams
    }

    private fun isMessageRequestThread(): Boolean {
        /*val hasSent = threadDb.getLastSeenAndHasSent(viewModel.threadId).second()
        return (!viewModel.recipient.isGroupRecipient && !hasSent) ||
                (!viewModel.recipient.isGroupRecipient && hasSent && !(viewModel.recipient.hasApprovedMe() || viewModel.hasReceived()))*/
        //New Line v32
        val recipient = viewModel.recipient ?: return false
        return !recipient.isGroupRecipient && !recipient.isApproved
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    private fun getBeldexAddress(address: Address): String {
        val contact = bchatContactDb.getContactWithBchatID(address.toString())
        val beldexAddress =
            contact?.displayBeldexAddress(Contact.ContactContext.REGULAR) ?: address.toString()
        Log.d("Beldex", "value of Beldex address $beldexAddress")
        return beldexAddress
    }

    //Payment Tag
    override fun sendBDX() {
        val txData: TxData = getTxData()
        txData.destinationAddress = senderBeldexAddress
        if (getCleanAmountString(getBDXAmount()).equals(
                Wallet.getDisplayAmount(totalFunds)))
                {
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
        if(TextSecurePreferences.getFeePriority(requireActivity())==0){
            txData.priority = PendingTransaction.Priority.Priority_Slow
        }else{
            txData.priority = PendingTransaction.Priority.Priority_Flash
        }
         Log.d("Beldex","Value of txData amount ${txData.amount}")
         Log.d("Beldex","Value of txData destination address ${txData.destinationAddress}")
         Log.d("Beldex","Value of txData priority ${txData.priority}")
        txData.mixin = MIXIN
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
        binding.progressBar.visibility = View.INVISIBLE
        //  sendButtonEnabled()
        showAlert(getString(R.string.send_create_tx_error_title), errorText!!)
    }

    override fun createTransactionFailed(errorText: String?) {
         hideProgress()
        //sendButtonEnabled()
        showAlert(getString(R.string.send_create_tx_error_title), errorText!!)
    }

    override fun transactionCreated(txTag: String?, pendingTransaction: PendingTransaction?) {
        // ignore txTag - the app flow ensures this is the correct tx
        Log.d("onTransactionCreated Status_Ok", "----")
        Log.d("Beldex","pending transaction called 1")
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
        //Important
        /*val confirm: SendConfirm? = getSendConfirm()
        if (confirm != null) {
            confirm.createTransactionFailed(errorText)
        }*/
        createTransactionFailed(errorText)
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(
            requireActivity(), R.style.backgroundColor
        )
        builder.setCancelable(true).setTitle(title).setMessage(message).create().show()
    }

    fun disposeTransaction() {
        pendingTx = null
        listenerCallback!!.onDisposeRequest()
    }

    var inProgress = false

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE
        inProgress = false
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
        inProgress = true
    }

    private fun refreshTransactionDetails() {
        Timber.d("refreshTransactionDetails()")
        Log.d("Beldex","refreshTransactionDetails called value of pending transaction $pendingTransaction")
        if (pendingTransaction != null) {
            Log.d("Beldex","refreshTransactionDetails called 1")
            val txData: TxData = getTxData()
            //Insert Recipient Address
            if(TextSecurePreferences.getSaveRecipientAddress(requireActivity())) {
                val insertRecipientAddress =
                    DatabaseComponent.get(requireActivity()).bchatRecipientAddressDatabase()
                try {
                    insertRecipientAddress.insertRecipientAddress(
                        pendingTransaction!!.firstTxId,
                        txData.destinationAddress
                    )
                }catch(e: IndexOutOfBoundsException){
                    Toast.makeText(requireContext(),getString(R.string.please_try_again_later),Toast.LENGTH_SHORT).show()
                }
            }
            InChatSend(pendingTransaction!!,txData, this).show(requireActivity().supportFragmentManager,"")
        }
    }



   /* inner class AsyncGetUnlockedBalance(val wallet: Listener?) :
        AsyncTaskCoroutine<Executor?, Boolean?>() {
        override fun onPreExecute() {
            super.onPreExecute()

        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            totalFunds = wallet!!.totalFunds
            return true
        }

        override fun onPostExecute(result: Boolean?) {

        }
    }*/

    private fun getCleanAmountString(enteredAmount: String): String? {
        return try {
            val amount = enteredAmount.toDouble()
            if (amount >= 0) {
                String.format(Locale.US, CLEAN_FORMAT, amount)
            } else {
                null
            }
        } catch (ex: NumberFormatException) {
            null
        }
    }

    private val resultLaunchers = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("Beldex","resultLaunchers called")
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("Beldex","resultLaunchers called 1")
            onResumeFragment()
        }
    }


    private fun onResumeFragment(){
        Timber.d("onResumeFragment()")
        Helper.hideKeyboard(activity)
        isResume = true

        //val txData: TxData = getTxData()
        //tvTxAddress.setText(txData.destinationAddress)
        //val notes: UserNotes = getTxData().userNotes
        /*if (notes != null && notes.note.isNotEmpty()) {
            //tvTxNotes.setText(notes.note)
            //fragmentSendConfirmNotesLinearLayout.setVisibility(View.VISIBLE)
        } else {
            //fragmentSendConfirmNotesLinearLayout.setVisibility(View.GONE)
        }*/
        refreshTransactionDetails()
        if (pendingTransaction == null && !inProgress) {
            Log.d("Beldex","pending transaction called 2")
           /* binding.sendButton.isEnabled=false
            binding.sendButton.isClickable=false*/
            showProgress()
            Log.d("Beldex","value of txData $txData")
            prepareSend(txData)
        }
    }

    private fun prepareSend(txData: TxData?) {
        Log.d("Beldex","pending transaction called 3")
        listenerCallback!!.onPrepareSend(null, txData)
    }

    fun send() {
        commitTransaction()
        requireActivity().runOnUiThread { binding.progressBar.visibility = View.VISIBLE }
    }

    private fun commitTransaction() {
        Timber.d("REALLY SEND")
        //disableNavigation() // committed - disable all navigation
        listenerCallback!!.onSend(txData.userNotes)
        committedTx = pendingTx
    }


    private fun getBDXAmount(): String {
        sendBDXAmount = binding.inputBar.text.trim() ?: return ""
        return sendBDXAmount as String
    }

    //If Transaction successfully completed after call this function
    fun onTransactionSent(txId: String?) {
        hideProgress()
        //Important
        Timber.d("txid=%s", txId)
        //activityCallback!!.setToolbarButton(Toolbar.BUTTON_BACK)
        Log.d("Beldex","Transaction Completed")
        //Payment Tag
        viewModel.sentPayment(sendBDXAmount.toString(),txId,viewModel.recipient)
        InChatSendSuccess(this).show(requireActivity().supportFragmentManager,"")
    }

    fun transactionFinished(){
       //sendButtonEnabled()
        listenerCallback!!.onBackPressedFun()
    }

    @SuppressLint("ResourceType")
    fun onSynced() {
        //WalletFragment Functionality-
        /* if (!activityCallback?.isWatchOnly!!) {
             binding.sendCardViewButton.isEnabled = true
             binding.sendCardViewButton.setBackgroundResource(R.drawable.send_card_enabled_background)
             binding.sendCardViewButtonText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.white))
             binding.scanQrCodeImg.isEnabled = true
             binding.scanQrCodeImg.setImageResource(R.drawable.ic_scan_qr)
         }*/
    }

    fun setProgress(text: String?) {
        //WalletFragment Functionality
        /*if(text==getString(R.string.reconnecting) || text==getString(R.string.status_wallet_connecting)){
           binding.syncStatusIcon.visibility=View.GONE
        }*/
        syncText = text
        binding.syncStatus.text = text
    }

    fun setProgress(n: Int) {
        Log.d("Beldex","mConnection value of n $n")
        syncProgress = n
        when {
            n > 100 -> {
                binding.blockProgressBar.isIndeterminate = true
                binding.blockProgressBar.isVisible = blockProgressBarVisible
            }
            n >= 0 -> {
                binding.blockProgressBar.isIndeterminate = false
                binding.blockProgressBar.progress = n
                binding.blockProgressBar.isVisible = blockProgressBarVisible
            }
            n==-2 -> {
                binding.blockProgressBar.isVisible = blockProgressBarVisible
                binding.blockProgressBar.isIndeterminate = false
                binding.blockProgressBar.progress=100
            }
            else -> { // <0
                binding.blockProgressBar.visibility = View.GONE
            }
        }
    }

    fun onRefreshed(wallet: Wallet, full: Boolean) {
        if (full) {
            if (CheckOnline.isOnline(requireContext())) {
                check(listenerCallback!!.hasBoundService()) { "WalletService not bound." }
                val daemonConnected: Wallet.ConnectionStatus = listenerCallback!!.connectionStatus!!
                Log.d("Beldex", "Value of daemon connection 1 $daemonConnected")
                if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                    AsyncGetUnlockedBalance(wallet).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)

                }
            }
        }
        binding.valueOfRemote.text = wallet.daemonBlockChainHeight.toString()
        //WalletFragment Functionality
        /*if (adapter!!.needsTransactionUpdateOnNewBlock()) {
            *//* wallet.refreshHistory()*//*
            full = true
            Log.d("TransactionList","full = true 1")
        }
        if (full) {
            Log.d("TransactionList","full = true 2")
            val list: MutableList<TransactionInfo> = ArrayList()
            val streetHeight: Long = activityCallback!!.streetModeHeight
            wallet.refreshHistory()
            for (info in wallet.history.all) {
                //Log.d("TxHeight=%d, Label=%s", info.blockheight.toString(), info.subaddressLabel)
                if ((info.isPending || info.blockheight >= streetHeight)
                    && !dismissedTransactions.contains(info.hash)
                ) list.add(info)
            }
            adapter!!.setInfos(list)
            adapterItems.clear()
            adapterItems.addAll(adapter!!.infoItems!!)
            if (accountIndex != wallet.accountIndex) {
                accountIndex = wallet.accountIndex
                binding.transactionList.scrollToPosition(0)
            }

            //SteveJosephh21
            if (adapter!!.itemCount > 0) {
                binding.transactionList.visibility = View.VISIBLE
                binding.emptyContainerLayout.visibility = View.GONE
            } else {
                binding.filterTransactionsIcon.isClickable = true // default = false
                binding.transactionList.visibility = View.GONE
                binding.emptyContainerLayout.visibility = View.VISIBLE
            }*/
        //Steve Josephh21 ANRS
        /*if (CheckOnline.isOnline(requireContext())) {
            check(activityCallback!!.hasBoundService()) { "WalletService not bound." }
            val daemonConnected: Wallet.ConnectionStatus = activityCallback!!.connectionStatus!!
            Log.d("Beldex", "Value of daemon connection 1 $daemonConnected")
            if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                Log.d("Beldex", "onRefreshed Called unlocked balance updated")
                AsyncGetUnlockedBalance(wallet).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
            }
        }
    }*/
        updateStatus(wallet)
    }

    private fun updateStatus(wallet: Wallet) {
        if (!isAdded) return
        Log.d("Beldex", "updateStatus()")
        //WalletFragment Functionality
        /*if (walletTitle == null || accountIdx != wallet.accountIndex) {
            accountIdx = wallet.accountIndex
            setActivityTitle(wallet)
        }*/
        val daemonHeight: Long = wallet.daemonBlockChainHeight
        val walletHeight: Long = wallet.blockChainHeight
        val walletSyncPercentage: Long = ((100 * walletHeight) / daemonHeight)

        Log.d("Beldex", "isOnline 0  ${CheckOnline.isOnline(requireContext())}")
        if(CheckOnline.isOnline(requireContext())) {
            Log.d("Beldex", "isOnline 1  ${CheckOnline.isOnline(requireContext())}")
            balance = wallet.balance
            Log.d("Beldex", "value of balance $balance")
            //unlockedBalance = wallet.unlockedBalance
            //refreshBalance(wallet.isSynchronized)
            val sync: String
            check(listenerCallback!!.hasBoundService()) { "WalletService not bound." }
            val daemonConnected: Wallet.ConnectionStatus = listenerCallback!!.connectionStatus!!
            Log.d("Beldex","Value of daemon connection $daemonConnected")
            if (daemonConnected === Wallet.ConnectionStatus.ConnectionStatus_Connected) {
                if (!wallet.isSynchronized) {
                    ApplicationContext.getInstance(context).messageNotifier.setHomeScreenVisible(true)
                    Log.d("Beldex","Height value of daemonHeight ${wallet.daemonBlockChainHeight}")
                    //Log.d("Beldex","Height value of daemonHeight one  ${activityCallback!!.daemonHeight}")
                    Log.d("Beldex","Height value of blockChainHeight ${wallet.blockChainHeight}")
                    Log.d("Beldex","Height value of approximateBlockChainHeight ${wallet.approximateBlockChainHeight}")
                    Log.d("Beldex","Height value of restoreHeight ${wallet.restoreHeight}")
                    Log.d("Beldex","Height value of daemonBlockChainTargetHeight ${wallet.daemonBlockChainTargetHeight}")


                    val n = daemonHeight - walletHeight
                    sync = formatter.format(n) + " " + getString(R.string.status_remaining)
                    if (firstBlock == 0L) {
                        firstBlock = walletHeight
                    }
                    var x = (100 - Math.round(100f * n / (1f * daemonHeight  - firstBlock))).toInt()
                    if (x == 0) x = 101 // indeterminate
                    Log.d("Beldex","App crash issue value of height daemon height $daemonHeight")
                    Log.d("Beldex","App crash issue value of height walletHeight height $walletHeight")
                    Log.d("Beldex","App crash issue value of height x height $x")
                    Log.d("Beldex","App crash issue value of height n height $n")
                    Log.d("Beldex","App crash issue value of height n firstBlock $firstBlock")
                    setProgress(x)
                    binding.valueOfWallet.text = "$walletHeight/$daemonHeight ($walletSyncPercentage%)"
                    //WalletFragment Functionality
                    /*ivSynced.setVisibility(View.GONE);
                    binding.filterTransactionsIcon.isClickable = false
                    activityCallback!!.hiddenRescan(false)
                    binding.syncStatusIcon.visibility=View.GONE*/
                    binding.syncStatus.setTextColor(
                        ContextCompat.getColor(
                            requireActivity().applicationContext,
                            R.color.green_color
                        )
                    )
                    binding.valueOfRemote.text = wallet.daemonBlockChainHeight.toString()
                } else {
                    ApplicationContext.getInstance(context).messageNotifier.setHomeScreenVisible(false)
                    binding.valueOfRemote.text = wallet.daemonBlockChainHeight.toString()
                    //Steve Josephh21 ANRS
                    // AsyncGetUnlockedBalance(wallet).execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
                    Log.d("showBalance->","Synchronized")
                    sync =
                        getString(R.string.status_synchronized)
                    binding.syncStatus.setTextColor(
                        ContextCompat.getColor(
                            requireActivity().applicationContext,
                            R.color.green_color
                        )
                    )
                    binding.valueOfWallet.text = "$walletHeight/$daemonHeight ($walletSyncPercentage%)"
                    //SteveJosephh21
                    setProgress(-2)
                    //WalletFragment Functionality
                    /*ivSynced.setVisibility(View.VISIBLE);
                    binding.filterTransactionsIcon.isClickable = true //default = adapter!!.itemCount > 0
                    activityCallback!!.hiddenRescan(true)
                    binding.syncStatusIcon.visibility = View.VISIBLE
                    binding.syncStatusIcon.setOnClickListener {
                        if (CheckOnline.isOnline(requireActivity())) {
                            if (wallet != null) {
                                checkSyncInfo(requireActivity(), wallet.restoreHeight)
                            }
                        }
                    }*/
                }
            } else {
                sync = getString(R.string.failed_connected_to_the_node)
                setProgress(-1)
                //WalletFragment Functionality
                //binding.syncStatusIcon.visibility=View.GONE
                //SteveJosephh21
                //binding.transactionTitle.visibility = View.INVISIBLE
                //binding.transactionLayoutCardView.visibility = View.GONE
                //anchorBehavior.setHideable(true)
                binding.syncStatus.setTextColor(
                    ContextCompat.getColor(
                        requireActivity().applicationContext,
                        R.color.red
                    )
                )
            }
            setProgress(sync)
        }
        else
        {
            Log.d("Beldex","isOnline else 2")
            setProgress(getString(R.string.no_node_connection))
            binding.syncStatus.setTextColor(
                ContextCompat.getColor(
                    requireActivity().applicationContext,
                    R.color.red
                )
            )
            //WalletFragment Functionality
            //binding.syncStatusIcon.visibility=View.GONE
        }
    }

    var walletLoaded = false

    fun onLoaded() {
        walletLoaded = true
        showReceive()
    }

    private fun showReceive() {
        /*if (walletLoaded) {
            binding.receiveCardViewButton.isEnabled = true
        }*/
    }

    private fun refreshBalance(synchronized: Boolean) {
        val unlockedBalance: Double = Helper.getDecimalAmount(unlockedBalance).toDouble()
        val balance: Double = Helper.getDecimalAmount(balance).toDouble()
        Log.d("Beldex", "value of balance $balance")
        Log.d("Beldex", "value of unlockedBalance $unlockedBalance")
        showBalance(
            Helper.getFormattedAmount(balance, true),
            Helper.getFormattedAmount(unlockedBalance, true),
            synchronized
        )
    }

    private fun showBalance(walletBalance: String?, walletUnlockedBalance: String?, synchronized:Boolean){
        Log.d("Beldex","value of show balance called 1")
        if(mContext!=null) {
            TextSecurePreferences.getDecimals(requireActivity())?.let { Log.d("Decimal", it) }
            if (!synchronized) {
                Log.d("Beldex", "value of show balance called 2")
                when {
                    TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                        binding.valueOfBalance.text = "-.--"
                        binding.valueOfUnlockedBalance.text = "-.--"
                    }
                    TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                        binding.valueOfBalance.text = "-.---"
                        binding.valueOfUnlockedBalance.text = "-.---"
                    }
                    TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (000)" -> {
                        binding.valueOfBalance.text = "-"
                        binding.valueOfUnlockedBalance.text = "-"
                    }
                    else -> {
                        binding.valueOfBalance.text = "-.----"
                        binding.valueOfUnlockedBalance.text = "-.----"
                    }
                }
            } else {
                Log.d("Beldex", "value of show balance called 3")
                when {
                    TextSecurePreferences.getDecimals(requireActivity()) == "2 - Two (0.00)" -> {
                        binding.valueOfBalance.text =
                            String.format("%.2f", walletBalance!!.replace(",", "").toDouble())
                        binding.valueOfUnlockedBalance.text = String.format(
                            "%.2f",
                            walletUnlockedBalance!!.replace(",", "").toDouble()
                        )
                    }
                    TextSecurePreferences.getDecimals(requireActivity()) == "3 - Three (0.000)" -> {
                        binding.valueOfBalance.text =
                            String.format("%.3f", walletBalance!!.replace(",", "").toDouble())
                        binding.valueOfUnlockedBalance.text = String.format(
                            "%.3f",
                            walletUnlockedBalance!!.replace(",", "").toDouble()
                        )
                    }
                    TextSecurePreferences.getDecimals(requireActivity()) == "0 - Zero (000)" -> {
                        binding.valueOfBalance.text =
                            String.format("%.0f", walletBalance!!.replace(",", "").toDouble())
                        binding.valueOfUnlockedBalance.text = String.format(
                            "%.0f",
                            walletUnlockedBalance!!.replace(",", "").toDouble()
                        )
                    }
                    else -> {
                        binding.valueOfBalance.text = walletBalance
                    }
                }
            }
        }
    }

    inner class AsyncGetUnlockedBalance(val wallet: Wallet) :
        AsyncTaskCoroutine<Executor?, Boolean?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            if (mContext != null && walletAvailableBalance != null) {
                if (walletAvailableBalance!!.replace(",", "").toDouble() > 0.0) {
                    showBalance(walletAvailableBalance!!, unlockedBalance.toString(), true)
                }
            } else {
                refreshBalance(false)
            }
        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            try {
                unlockedBalance = wallet.unlockedBalance
            }catch (e: Exception){
                Log.d("WalletFragment",e.toString())
            }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            refreshBalance(wallet.isSynchronized)
        }
    }
}

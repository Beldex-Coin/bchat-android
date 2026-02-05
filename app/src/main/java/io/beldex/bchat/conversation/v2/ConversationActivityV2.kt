package io.beldex.bchat.conversation.v2

import android.Manifest
import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.util.Log
import android.util.TypedValue
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DimenRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.jobs.AttachmentDownloadJob
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.mentions.Mention
import com.beldex.libbchat.messaging.mentions.MentionsManager
import com.beldex.libbchat.messaging.messages.control.DataExtractionNotification
import com.beldex.libbchat.messaging.messages.control.ExpirationTimerUpdate
import com.beldex.libbchat.messaging.messages.signal.OutgoingMediaMessage
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage
import com.beldex.libbchat.messaging.messages.visible.Reaction
import com.beldex.libbchat.messaging.messages.visible.SharedContact
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview
import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel
import com.beldex.libbchat.messaging.utilities.UpdateMessageBuilder.capitalizeFirstLetter
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.MediaTypes
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.Stub
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getIsReactionOverlayVisible
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setIsReactionOverlayVisible
import com.beldex.libbchat.utilities.concurrent.SimpleTask
import com.beldex.libbchat.utilities.isScrolledToBottom
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libbchat.utilities.recipients.RecipientModifiedListener
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.ListenableFuture
import com.beldex.libsignal.utilities.guava.Optional
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.CheckOnline
import io.beldex.bchat.MediaOverviewActivity
import io.beldex.bchat.R
import io.beldex.bchat.audio.AudioRecorder
import io.beldex.bchat.compose_utils.ComposeDialogContainer
import io.beldex.bchat.compose_utils.DialogType
import io.beldex.bchat.contacts.SelectContactsActivity
import io.beldex.bchat.contactshare.SimpleTextWatcher
import io.beldex.bchat.conversation.v2.contact_sharing.ContactModel
import io.beldex.bchat.conversation.v2.contact_sharing.ContactSharingActivity
import io.beldex.bchat.conversation.v2.contact_sharing.ViewAllContactsActivity
import io.beldex.bchat.conversation.v2.contact_sharing.flattenData
import io.beldex.bchat.conversation.v2.dialogs.LinkPreviewDialog
import io.beldex.bchat.conversation.v2.dialogs.SendSeedDialog
import io.beldex.bchat.conversation.v2.input_bar.InputBarDelegate
import io.beldex.bchat.conversation.v2.input_bar.InputBarRecordingViewDelegate
import io.beldex.bchat.conversation.v2.input_bar.mentions.MentionCandidatesView
import io.beldex.bchat.conversation.v2.menus.ConversationActionModeCallback
import io.beldex.bchat.conversation.v2.menus.ConversationActionModeCallbackDelegate
import io.beldex.bchat.conversation.v2.menus.ConversationMenuHelper
import io.beldex.bchat.conversation.v2.messages.ControlMessageView
import io.beldex.bchat.conversation.v2.messages.VisibleMessageContentView
import io.beldex.bchat.conversation.v2.messages.VisibleMessageView
import io.beldex.bchat.conversation.v2.messages.VisibleMessageViewDelegate
import io.beldex.bchat.conversation.v2.search.SearchBottomBar
import io.beldex.bchat.conversation.v2.search.SearchViewModel
import io.beldex.bchat.conversation.v2.utilities.AttachmentManager
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.conversation.v2.utilities.MentionManagerUtilities
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities
import io.beldex.bchat.conversation.v2.utilities.ResendMessageUtilities
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.database.BeldexMessageDatabase
import io.beldex.bchat.database.MmsDatabase
import io.beldex.bchat.database.ReactionDatabase
import io.beldex.bchat.database.SmsDatabase
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.model.MessageId
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.database.model.MmsMessageRecord
import io.beldex.bchat.database.model.ReactionRecord
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.databinding.ActivityConversationV2Binding
import io.beldex.bchat.databinding.ViewVisibleMessageBinding
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.giph.ui.GiphyActivity
import io.beldex.bchat.groups.SecretGroupInfoComposeActivity
import io.beldex.bchat.groups.SecretGroupInfoRepository
import io.beldex.bchat.home.ConversationActionDialog
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.home.HomeDialogType
import io.beldex.bchat.home.UserDetailsBottomSheet
import io.beldex.bchat.linkpreview.LinkPreviewRepository
import io.beldex.bchat.linkpreview.LinkPreviewUtil
import io.beldex.bchat.linkpreview.LinkPreviewViewModel
import io.beldex.bchat.mediasend.Media
import io.beldex.bchat.mediasend.MediaSendActivity
import io.beldex.bchat.mms.AudioSlide
import io.beldex.bchat.mms.GifSlide
import io.beldex.bchat.mms.ImageSlide
import io.beldex.bchat.mms.MediaConstraints
import io.beldex.bchat.mms.Slide
import io.beldex.bchat.mms.SlideDeck
import io.beldex.bchat.mms.VideoSlide
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.preferences.PrivacySettingsActivity
import io.beldex.bchat.reactions.ReactionsDialogFragment
import io.beldex.bchat.reactions.any.ReactWithAnyEmojiDialogFragment
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.MediaUtil
import io.beldex.bchat.util.SaveAttachmentTask
import io.beldex.bchat.util.drawToBitmap
import io.beldex.bchat.util.parcelable
import io.beldex.bchat.util.push
import io.beldex.bchat.util.serializable
import io.beldex.bchat.util.shortNameAndAddress
import io.beldex.bchat.util.toPx
import io.beldex.bchat.webrtc.CallViewModel
import io.beldex.bchat.webrtc.NetworkChangeReceiver
import io.beldex.bchat.webrtc.WebRTCComposeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.komponents.kovenant.ui.successUi
import org.apache.commons.lang3.time.DurationFormatUtils
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@AndroidEntryPoint
class ConversationActivityV2 : AppCompatActivity(), InputBarDelegate,
    InputBarRecordingViewDelegate, AttachmentManager.AttachmentListener,
    ConversationActionModeCallbackDelegate,
    RecipientModifiedListener,
    SearchBottomBar.EventListener, LoaderManager.LoaderCallbacks<Cursor>,
    ConversationMenuHelper.ConversationMenuListener, ConversationActionDialog.ConversationActionDialogListener, VisibleMessageViewDelegate,
    ConversationReactionOverlay.OnReactionSelectedListener,
    ReactWithAnyEmojiDialogFragment.Callback, ReactionsDialogFragment.Callback,
    SecretGroupInfoComposeActivity.SocialGroupInfoInterface,
    VisibleMessageContentView.VisibleMessageContentViewDelegate,
    ScreenshotDetector.ScreenshotDetectionListeners, ActivityDispatcher, UserDetailsBottomSheet.UserDetailsBottomSheetListener  {

    // -------------------- ViewBinding --------------------
    lateinit var binding: ActivityConversationV2Binding

    // -------------------- Simple fields ------------------
    private var param2: String? = null
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels

    private var actionMode: ActionMode? = null
    private var selectedEvent: MotionEvent? = null
    private var selectedView: VisibleMessageView? = null
    private var selectedMessageRecord: MessageRecord? = null

    private var unreadCount = 0
    private var isLockViewExpanded = false
    private var isShowingAttachmentOptions = false
    private var emojiLastClickTime: Long = 0

    private var isResume = false
    private var blockProgressBarVisible = false
    private var dispatchTouched = false
    private var isNetworkAvailable = true
    private var isAudioPlaying = false
    private var audioPlayingIndexInAdapter = -1

    private var menuItemLastClickTime = 0L
    private var unblockButtonLastClickTime = 0L
    private var clearChatButtonLastClickTime = 0L
    private var networkChangedReceiver : NetworkChangeReceiver?=null
    private val layoutManager : LinearLayoutManager?
        get() {
            return binding.conversationRecyclerView.layoutManager as LinearLayoutManager?
        }

    // -------------------- Intent extras -------------------
    private val threadId: Long by lazy {
        intent.getLongExtra(THREAD_ID, -1L)
    }

    private val address: Address? by lazy {
        intent.getParcelableExtra(ADDRESS)
    }

    private var uiJob : Job?=null
    private val callDurationFormat="HH:mm:ss"

    // -------------------- Hilt injections ----------------
    @Inject
    lateinit var threadDb: ThreadDatabase
    @Inject lateinit var reactionDb: ReactionDatabase
    @Inject lateinit var textSecurePreferences: TextSecurePreferences
    @Inject lateinit var beldexMessageDb: BeldexMessageDatabase
    @Inject lateinit var smsDb: SmsDatabase
    @Inject lateinit var mmsDb: MmsDatabase
    @Inject lateinit var conversationViewModelFactory: ConversationViewModel.AssistedFactory

    // -------------------- ViewModels ---------------------
    private val viewModel : ConversationViewModel by viewModels {
        val extras = intent.extras
        var threadId=extras?.getLong(THREAD_ID, -1L)
        if (threadId == -1L) {
            extras?.getParcelable<Address>(ADDRESS)?.let { address ->
                val recipient=Recipient.from(this, address, false)
                threadId=threadDb.getOrCreateThreadIdFor(recipient)
            }
        }
        conversationViewModelFactory.create(threadId!!)
    }


    private val linkPreviewViewModel: LinkPreviewViewModel by lazy {
        ViewModelProvider(
            this,
            LinkPreviewViewModel.Factory(LinkPreviewRepository(this))
        )[LinkPreviewViewModel::class.java]
    }
    private val searchViewModel by viewModels<SearchViewModel>()
    private val callViewModel by viewModels<CallViewModel>()
    var searchViewItem : MenuItem?=null

    // -------------------- Media / Attachments ------------
    private lateinit var audioRecorder: AudioRecorder
    private val stopAudioHandler = Handler(Looper.getMainLooper())
    private val stopVoiceMessageRecordingTask = Runnable { sendVoiceMessage() }
    private var groupRepository : SecretGroupInfoRepository?=null
    private lateinit var reactionDelegate : ConversationReactionDelegate
    private val reactWithAnyEmojiStartPage=-1

    private val attachmentManager by lazy {
        AttachmentManager(this, this)
    }
    private val isScrolledToBottom : Boolean
        get()=binding.conversationRecyclerView.isScrolledToBottom

    // -------------------- Mentions -----------------------
    private val mentions = mutableListOf<Mention>()
    private var mentionCandidatesView: MentionCandidatesView? = null
    private var previousText: CharSequence = ""
    private var currentMentionStartIndex = -1
    private var isShowingMentionCandidatesView = false
    private var clearChatButtonLastClickTiem: Long = 0

    // -------------------- RecyclerView -------------------
    private val glide by lazy { Glide.with(this) }

    private val adapter by lazy {
        val cursor=viewModel.getConversationsCursor()
        val adapter=ConversationAdapter(
            this,
            cursor,
            searchViewModel,
            onItemPress={ message, position, view, event ->
                if (!TextSecurePreferences.getIsReactionOverlayVisible(this)) {
                    handlePress(message, position, view, event)
                }
            },
            onItemSwipeToReply={ message, _ ->
                if (isSecretGroupIsActive()) {
                    handleSwipeToReply(message)
                }
            },
            onItemLongPress={ message, position, view ->
                if (isSecretGroupIsActive()) {
                    if (message.isSent && !isMessageRequestThread() && !viewModel.recipient.value?.isOpenGroupRecipient!!) {
                        if (selectedItem(message)) {
                            actionMode?.let { onDeselect(message, position, it) }
                        } else {
                            showConversationReaction(message, view, position)
                        }
                    } else {
                        selectMessage(message, position)
                    }
                }
            },
            onDeselect={ message, position ->
                actionMode?.let {
                    onDeselect(message, position, it)
                }
            },
            onAttachmentNeedsDownload={ attachmentId, mmsId ->
                // Start download (on IO thread)
                lifecycleScope.launch(Dispatchers.IO) {
                    JobQueue.shared.add(AttachmentDownloadJob(attachmentId, mmsId))
                }
            },
            glide=glide,
            lifecycleCoroutineScope=lifecycleScope
        )
        adapter.visibleMessageViewDelegate=this
        adapter
    }

    // -------------------- Other --------------------------
    private lateinit var screenshotDetector: ScreenshotDetector
    private var amplitudeJob: Job? = null
    private var conversationApprovalJob: Job? = null
    private val messageToScrollTimestamp=AtomicLong(-1)
    private val messageToScrollAuthor=AtomicReference<Address?>(null)
    private val lockViewHitMargin by lazy { toPx(40, resources) }
    private val hexEncodedPublicKey : String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    private val seed by lazy {
        var hexEncodedSeed = IdentityKeyUtil.retrieve(this, IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed = IdentityKeyUtil.getIdentityKeyPair(this).hexEncodedPrivateKey
        }
        val loadFileContents: (String) -> String = { fileName ->
            MnemonicUtilities.loadFileContents(this, fileName)
        }
        MnemonicCodec(loadFileContents).encode(
            hexEncodedSeed!!,
            MnemonicCodec.Language.Configuration.english
        )
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConversationV2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.conversationActivityToolbar)

        if (threadId == -1L) {
            val extras = intent.extras
            var threadId= extras?.getLong(THREAD_ID, -1L)
            if (threadId == -1L) {
                extras?.getParcelable<Address>(ADDRESS)?.let { address ->
                    val recipient=Recipient.from(this, address, false)
                    threadId=threadDb.getOrCreateThreadIdFor(recipient)
                }
            }
           conversationViewModelFactory.create(threadId!!)
        }
        // ---------- Network monitoring ----------
        networkChangedReceiver = NetworkChangeReceiver(::networkChange)
        networkChangedReceiver?.register(this)

        initConversationScreen()
    }

    override fun onResume() {
        super.onResume()
        setupCallActionBar()
        ApplicationContext.getInstance(this).messageNotifier.setVisibleThread(viewModel.threadId)
        if (!viewModel.markAllRead())
            return

        viewModel.recipient.value?.let { thread ->
            showBlockProgressBar(thread)
        }

    }

    override fun onStart() {
        super.onStart()
        screenshotDetector = ScreenshotDetector(this, this)
        screenshotDetector.register()
    }

    override fun onStop() {
        super.onStop()
        screenshotDetector.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelVoiceMessage()
        isNetworkAvailable=false
        networkChangedReceiver?.unregister(this)
        networkChangedReceiver=null
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPrepareOptionsMenu(menu : Menu) : Boolean {
        val recipient=viewModel.recipient.value
        recipient?.let {
            if (!isMessageRequestThread()) {
                callOnPrepareOptionsMenu(menu, recipient)
            } else if (recipient.isLocalNumber) {
                callOnPrepareOptionsMenu(menu, recipient)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun callOnPrepareOptionsMenu(menu : Menu, recipient : Recipient) {
        hideAttachmentContainer()
        ConversationMenuHelper.onPrepareOptionsMenu(
            menu,
            this.menuInflater,
            recipient,
            viewModel.threadId,
           this,
            this@ConversationActivityV2
        ) {
            onOptionsItemSelected(it)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        if (SystemClock.elapsedRealtime() - menuItemLastClickTime >= 1000) {
            if (!binding.inputBarRecordingView.isTimerRunning) {
                menuItemLastClickTime = SystemClock.elapsedRealtime()
                if (item.itemId == android.R.id.home) {
                    hideAttachmentContainer()
                    return false
                } else if (item.itemId == R.id.menu_call) {
                    hideAttachmentContainer()
                    val recipient=viewModel.recipient.value ?: return false
                    if (recipient.isContactRecipient && recipient.isBlocked) {
                        unblock()
                    } else {
                        viewModel.recipient.value?.let { recipients ->
                            call(this, recipients)
                        }
                    }
                }
                return viewModel.recipient.value?.let { recipient ->
                    ConversationMenuHelper.onOptionItemSelected(
                        this,
                        this,
                        item,
                        recipient,
                        supportFragmentManager
                    )
                } ?: false
            } else {
                return false
            }
        }
        return false
    }

    private fun call(context : Context, thread : Recipient) {

        if (!TextSecurePreferences.isCallNotificationsEnabled(context)) {
            //SteveJosephh22
            val factory=LayoutInflater.from(this)
            val callPermissionDialogView : View=
                factory.inflate(R.layout.call_permissions_dialog_box, null)
            val callPermissionDialog=AlertDialog.Builder(this).create()
            callPermissionDialog.setView(callPermissionDialogView)
            callPermissionDialogView.findViewById<Button>(R.id.settingsDialogBoxButton)
                .setOnClickListener {
                    val intent=Intent(this, PrivacySettingsActivity::class.java)
                    startActivity(intent)
                    callPermissionDialog.dismiss()
                }
            callPermissionDialogView.findViewById<Button>(R.id.cancelDialogBoxButton)
                .setOnClickListener {
                    callPermissionDialog.dismiss()
                }
            callPermissionDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            callPermissionDialog.show()
            return
        }

        val service=WebRtcCallService.createCall(context, thread)
        context.startService(service)
        val activity=Intent(context, WebRTCComposeActivity::class.java).apply {
            flags=Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(activity)

    }

    private fun initConversationScreen() {
        // ---------- Audio ----------
        audioRecorder = AudioRecorder(applicationContext)

        // ---------- Permissions ----------
        checkReadExternalStoragePermission()

        // ---------- Back-to-home observer ----------
        lifecycleScope.launch {
            viewModel.backToHome.collectLatest { shouldGoBack ->
                if (shouldGoBack) {
                    Toast.makeText(
                        this@ConversationActivityV2,
                        getString(R.string.conversationsDeleted),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }

        // ---------- Group repository (NO DatabaseComponent lookup on UI thread) ----------
        groupRepository = SecretGroupInfoRepository(
            DatabaseComponent.get(applicationContext).groupDatabase()
        )

        // ---------- Scroll-to-message extras ----------
        messageToScrollTimestamp.set(
            intent.getLongExtra(SCROLL_MESSAGE_ID, -1L)
        )
        messageToScrollAuthor.set(
            intent.getParcelableExtra(SCROLL_MESSAGE_AUTHOR)
        )

        binding.networkStatusLayout.visibility =
            if (isNetworkAvailable) View.GONE else View.VISIBLE

        // ---------- Heavy DB / IO work OFF main thread ----------
        lifecycleScope.launch(Dispatchers.IO) {

            unreadCount = viewModel.getUnreadCount()

            withContext(Dispatchers.Main) {

                setUpRecyclerView()

                viewModel.recipient.value?.let { recipient ->
                    setUpTypingObserver(recipient)
                }

                setUpRecipientObserver()
                getLatestOpenGroupInfoIfNeeded()
                setUpSearchResultObserver()
                scrollToFirstUnreadMessageIfNeeded()
            }
        }

        // ---------- UI setup ----------
        setUpToolBar()
        setupInputBar()
        setUpLinkPreviewObserver()
        restoreDraftIfNeeded()
        setUpUiStateObserver()
        setMediaControlForReportIssue()
        updateUnreadCountIndicator()
        updateSubtitle()
        setUpBlockedBanner()
        setUpMessageRequestsBar()
        setSearchView()
        showOrHideInputIfNeeded()

        // ---------- Scroll to bottom ----------
        binding.scrollToBottomButton.setOnClickListener {

            val layoutManager =
                binding.conversationRecyclerView.layoutManager as? LinearLayoutManager
                    ?: return@setOnClickListener

            if (layoutManager.isSmoothScrolling) {
                binding.conversationRecyclerView.scrollToPosition(0)
            } else {
                val position = layoutManager.findFirstVisibleItemPosition()
                if (position > 10) {
                    binding.conversationRecyclerView.scrollToPosition(10)
                }
                binding.conversationRecyclerView.post {
                    binding.conversationRecyclerView.smoothScrollToPosition(0)
                }
            }
        }

        // ---------- Reaction overlay ----------
        val reactionOverlayStub: Stub<ConversationReactionOverlay> =
            ViewUtil.findStubById(
                this,
                R.id.conversation_reaction_scrubber_stub
            )

        reactionDelegate = ConversationReactionDelegate(reactionOverlayStub)
        reactionDelegate.setOnReactionSelectedListener(this)
    }

    private fun setupCallActionBar() {
        val startTimeNew=callViewModel.callStartTime
        if (startTimeNew == -1L) {
            binding.callActionBarView.isVisible=false
        } else {
            binding.callActionBarView.isVisible=true
            uiJob=lifecycleScope.launch {
                launch {
                    while (isActive) {
                        val startTime=callViewModel!!.callStartTime
                        if (startTime == -1L) {
                            binding.callActionBarView.isVisible=false
                        } else {
                            binding.callActionBarView.isVisible=true
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
            this.startService(
                WebRtcCallService.hangupIntent(
                    this
                )
            )
            binding.callActionBarView.isVisible=false
            Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT)
                .show()
        }
        binding.callActionBarView.setOnClickListener {
            callWebRTCCallScreen()
        }
    }

    private fun callWebRTCCallScreen() {
        Intent(this, WebRTCComposeActivity::class.java).also {
            startActivity(it)
        }
    }

    private fun checkReadExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestReadExternalStoragePermissionForTiramisu()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestReadExternalStoragePermission()
            }
        }
    }

    private fun requestReadExternalStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PICK_FROM_LIBRARY
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestReadExternalStoragePermissionForTiramisu() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            ), PICK_FROM_LIBRARY
        )
    }

    private fun isMessageRequestThread() : Boolean {
        val recipient = viewModel.recipient.value ?: return false
        return !recipient.isGroupRecipient && !recipient.isApproved
    }

    private fun selectedItem(message : MessageRecord) : Boolean {
        return adapter.selectedItems.contains(message)
    }

    private fun setUpRecyclerView() {

        binding.conversationRecyclerView.adapter = adapter

        // LayoutManager
        val layoutManager = LinearLayoutManager(
            this, // ✅ Activity context
            LinearLayoutManager.VERTICAL,
            !viewModel.isIncomingMessageRequestThread()
        )

        binding.conversationRecyclerView.layoutManager = layoutManager
        binding.conversationRecyclerView.setHasFixedSize(true)

        // Cursor loader (Activity-safe)
        LoaderManager.getInstance(this@ConversationActivityV2)
            .restartLoader(0, null, this)

        // Scroll listener (lightweight, no allocations)
        binding.conversationRecyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    handleRecyclerViewScrolled()
                }
            }
        )

        // Layout change listener (used for FAB / scroll-to-bottom visibility)
        binding.conversationRecyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            showScrollToBottomButtonIfApplicable()
        }
    }

    private fun setUpTypingObserver(thread: Recipient) {

        // ---------- Observe typing status (Activity lifecycle) ----------
        ApplicationContext
            .getInstance(applicationContext)
            .typingStatusRepository
            .getTypists(viewModel.threadId)
            .observe(this@ConversationActivityV2) { state ->

                val recipients = state?.typists.orEmpty()

                val container = binding.typingIndicatorViewContainer
                container.isVisible = recipients.isNotEmpty() && isScrolledToBottom
                container.setTypists(recipients)

                inputBarHeightChanged(binding.inputBar.height)
            }

        // ---------- Text watcher ----------

        val typingEnabled = textSecurePreferences.isTypingIndicatorsEnabled()

        binding.inputBar.addTextChangedListener(object : SimpleTextWatcher() {

            override fun onTextChanged(text: String?) {

                if (typingEnabled) {
                    ApplicationContext
                        .getInstance(applicationContext)
                        .typingStatusSender
                        .onTypingStarted(viewModel.threadId)
                }

                //Need to check this
                //checkInputBarTextOnTextChanged(text, thread)
            }

            override fun afterTextChanged(s: Editable?) {
                // Keep empty to avoid layout thrash
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.backToHome.collectLatest {
                if (it) finish()
            }
        }
    }
    private fun setUpRecipientObserver() {
        viewModel.recipient.value?.addListener(this)
    }

    private fun getLatestOpenGroupInfoIfNeeded() {
        try {
            val openGroup=viewModel.getOpenGroupChat()
                ?: return
            OpenGroupAPIV2.getMemberCount(openGroup.room, openGroup.server)
                .successUi { updateSubtitle() }
        } catch (ex : NullPointerException) {
            Timber.tag("Exception ").d(ex.message.toString())
        }
    }

    private fun showConversationReaction(
        message: MessageRecord,
        messageView: View,
        position: Int
    ) {
        val messageContentView = when (messageView) {
            is VisibleMessageView -> messageView.messageContentView
            is ControlMessageView -> messageView.controlContentView
            else -> {
                Timber.tag("Beldex")
                    .w("Failed to show reaction because the messageView is not of a known type")
                return
            }
        }

        val messageContentBitmap = try {
            messageContentView.drawToBitmap()
        } catch (e: Exception) {
            Log.e("Beldex", "Failed to show emoji picker", e)
            return
        }

        // Activity uses `this` instead of requireContext()
        setIsReactionOverlayVisible(this, true)

        // Hide keyboard
        ViewUtil.hideKeyboard(this, messageView)

        binding.reactionsShade.isVisible = true
        showOrHidScrollToBottomButton(false)

        binding.conversationRecyclerView.suppressLayout(true)

        reactionDelegate.setOnActionSelectedListener(
            ReactionsToolbarListener(message, position)
        )

        reactionDelegate.setOnHideListener(object :
            ConversationReactionOverlay.OnHideListener {

            override fun startHide() {
                ViewUtil.fadeOut(
                    binding.reactionsShade,
                    resources.getInteger(R.integer.reaction_scrubber_hide_duration),
                    View.GONE
                )
                showOrHidScrollToBottomButton(true)
            }

            override fun onHide() {
                binding.conversationRecyclerView.suppressLayout(false)
                setIsReactionOverlayVisible(this@ConversationActivityV2, false)
            }
        })

        val topLeft = IntArray(2).also {
            messageContentView.getLocationInWindow(it)
        }

        val selectedConversationModel = SelectedConversationModel(
            bitmap = messageContentBitmap,
            topLeft[0].toFloat(),
            topLeft[1].toFloat(),
            messageContentView.width,
            isOutgoing = message.isOutgoing,
            messageContentView
        )

        // Activity call
        reactionDelegate.show(this, message, selectedConversationModel)
    }


    private fun setUpSearchResultObserver() {

        val vm = searchViewModel ?: return

        vm.searchResults.observe(this@ConversationActivityV2) { result ->
            result ?: return@observe

            val hasQuery = !binding.searchQuery.text.isNullOrEmpty()
            val results = result.getResults()

            if (results.isNotEmpty()) {

                vm.updateSearchResult(true)

                results.getOrNull(result.position)?.let { item ->
                    jumpToMessage(
                        item.messageRecipient.address,
                        item.sentTimestampMs,
                        Runnable { vm.onMissingResult() }
                    )
                }

                updateSearchUi(
                    showNavigation = true,
                    showProgress = false,
                    showNoMatches = false,
                    showClose = hasQuery
                )

            } else {

                vm.updateSearchResult(false)

                updateSearchUi(
                    showNavigation = false,
                    showProgress = false,
                    showNoMatches = hasQuery,
                    showClose = hasQuery
                )
            }
        }
    }

    private fun updateSearchUi(
        showNavigation: Boolean,
        showProgress: Boolean,
        showNoMatches: Boolean,
        showClose: Boolean
    ) {
        binding.searchUp.isVisible = showNavigation
        binding.searchDown.isVisible = showNavigation
        binding.searchProgress.isVisible = showProgress

        binding.noMatchesFoundTextview.isVisible = showNoMatches
        binding.closeSearch.isVisible = showClose

        binding.search.isVisible = !showClose
        binding.searchClose.isVisible = !showClose
    }

    private fun jumpToMessage(
        author : Address,
        timestamp : Long,
        onMessageNotFound : Runnable?
    ) {
        SimpleTask.run(lifecycle, {
            viewModel.getMessagePositionInConversation(timestamp, author)
        }) { p : Int -> moveToMessagePosition(p, onMessageNotFound) }
    }

    private fun scrollToFirstUnreadMessageIfNeeded() {
        val lastSeenTimestamp=viewModel.getLastSeenAndHasSent().first()
        val lastSeenItemPosition=adapter.findLastSeenItemPosition(lastSeenTimestamp) ?: return
        if (lastSeenItemPosition <= 3) {
            return
        }
        binding.conversationRecyclerView.scrollToPosition(lastSeenItemPosition)
    }

    private fun setUpToolBar() {

        val recipient = viewModel.recipient.value ?: return
        val profileManager = SSKEnvironment.shared.profileManager

        // ---------- Optional BNS name (from Intent extras) ----------
        val bnsName = intent.getStringExtra(BNS_NAME)
        if (!bnsName.isNullOrEmpty() && adapter.cursor?.count == 0) {
            profileManager.setName(applicationContext, recipient, bnsName)
        }

        // ---------- Title ----------
        val title = when {
            recipient.isLocalNumber ->
                getString(R.string.note_to_self).capitalizeFirstLetter()
            else ->
                recipient.toShortString().capitalizeFirstLetter()
        }

        binding.conversationTitleView.text = title

        // ---------- Profile picture size ----------
        @DimenRes val sizeRes = if (recipient.isClosedGroupRecipient) {
            R.dimen.medium_profile_picture_size
        } else {
            R.dimen.small_profile_picture_size
        }

        val sizePx = resources.getDimension(sizeRes).roundToInt()

        binding.profilePictureView.root.layoutParams =
            LinearLayout.LayoutParams(sizePx, sizePx)

        // ---------- Profile picture binding ----------
        binding.profilePictureView.root.glide = glide
        binding.profilePictureView.root.update(recipient)

        // ---------- Populate mentions cache ----------
        MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(
            viewModel.threadId,
            applicationContext
        )

        // ---------- Toolbar click ----------
        binding.layoutConversation.setOnClickListener {
            cancelVoiceMessage()
            hideAttachmentContainer()

            if (recipient.isClosedGroupRecipient) {
                callSecretGroupInfo(recipient)
            } else {
                val intent = Intent(this, MediaOverviewActivity::class.java)
                intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, recipient.address)
                passSharedMessageToConversationScreen.launch(intent)
            }
        }

        // ---------- Back button ----------
        binding.backToHomeBtn.setOnClickListener {
            handleBackPressed()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupInputBar() {
        binding.inputBar.delegate=this
        binding.inputBarRecordingView.delegate=this
        binding.cameraButton.setOnClickListener {
            showCamera()
            toggleAttachmentOptions()
        }
        binding.imageButton.setOnClickListener {
            pickFromLibrary()
            toggleAttachmentOptions()
        }
        binding.documentButton.setOnClickListener {
            showDocumentPicker()
            toggleAttachmentOptions()
        }
        binding.gifButton.setOnClickListener {
            if (CheckOnline.isOnline(this)) {
                showGIFPicker()
                toggleAttachmentOptions()
            } else {
                Toast.makeText(
                    this,
                    R.string.please_check_your_internet_connection,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.contactButton.setOnClickListener {
            toggleAttachmentOptions()
            val intent=Intent(this, ContactSharingActivity::class.java)
            contactActivityLauncher.launch(intent)
        }
    }

    private fun setUpLinkPreviewObserver() {

        // ---------- Feature toggle ----------
        if (!textSecurePreferences.isLinkPreviewsEnabled()) {
            linkPreviewViewModel.onUserCancel()
            return
        }

        // ---------- Observe link preview state ----------
        linkPreviewViewModel.linkPreviewState.observe(
            this@ConversationActivityV2
        ) { previewState ->

            previewState ?: return@observe

            when {
                previewState.isLoading -> {
                    binding.inputBar.draftLinkPreview()
                }

                previewState.linkPreview.isPresent -> {
                    binding.inputBar.updateLinkPreviewDraft(
                        glide,
                        previewState.linkPreview.get()
                    )
                }

                else -> {
                    binding.inputBar.cancelLinkPreviewDraft(2)
                }
            }
        }
    }

    private fun restoreDraftIfNeeded() {

        // ---------- Intent extras ----------
        val mediaUri: Uri? = intent.getParcelableExtra(URI)
        val mediaType = AttachmentManager.MediaType.from(
            intent.getStringExtra(TYPE)
        )
        val isInChatShare = intent.getBooleanExtra(IN_CHAT_SHARE, false)

        val mimeType = mediaUri?.let {
            MediaUtil.getMimeType(this, it)
        }

        // ---------- Media shared into chat ----------
        if (mediaUri != null && mediaType != null) {

            val isVisualMedia =
                mimeType != null &&
                        (mediaType == AttachmentManager.MediaType.IMAGE ||
                                mediaType == AttachmentManager.MediaType.GIF ||
                                mediaType == AttachmentManager.MediaType.VIDEO)

            if (isVisualMedia && isInChatShare) {

                prepMediaForSending(mediaUri, mediaType)
                    .addListener(object : ListenableFuture.Listener<Boolean> {

                        override fun onSuccess(result: Boolean?) {
                            sendAttachments(
                                attachmentManager.buildSlideDeck().asAttachments(),
                                null
                            )
                        }

                        override fun onFailure(e: ExecutionException?) {
                            Toast.makeText(
                                this@ConversationActivityV2,
                                R.string.activity_conversation_attachment_prep_failed,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })

                return
            }

            if (isVisualMedia) {

                val media =mimeType?.let {
                    Media(
                        mediaUri,
                        it,
                        0, 0, 0, 0,
                        Optional.absent(),
                        Optional.absent()
                    )
                }

                startActivityForResult(
                    MediaSendActivity.buildEditorIntent(
                        this,
                        listOf(media),
                        viewModel.recipient.value ?: return,
                        ""
                    ),
                    PICK_FROM_LIBRARY
                )

                return
            }

            // ---------- Non-visual media ----------
            prepMediaForSending(mediaUri, mediaType)
                .addListener(object : ListenableFuture.Listener<Boolean> {

                    override fun onSuccess(result: Boolean?) {
                        sendAttachments(
                            attachmentManager.buildSlideDeck().asAttachments(),
                            null
                        )
                    }

                    override fun onFailure(e: ExecutionException?) {
                        Toast.makeText(
                            this@ConversationActivityV2,
                            R.string.activity_conversation_attachment_prep_failed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })

            return
        }

        // ---------- Text shared into chat ----------
        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrEmpty()) {
            binding.inputBar.text = sharedText.toString()
            return
        }

        // ---------- Restore draft ----------
        viewModel.getDraft()?.let { draft ->
            binding.inputBar.text = draft
        }
    }

    private fun setUpUiStateObserver() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.uiState.collect { uiState ->

                    // ---------- UI messages ----------
                    uiState.uiMessages.firstOrNull()?.let { uiMessage ->
                        Toast.makeText(
                            this@ConversationActivityV2,
                            uiMessage.message,
                            Toast.LENGTH_LONG
                        ).show()

                        viewModel.messageShown(uiMessage.id)
                    }

                    // ---------- Open group guidelines ----------
                    addOpenGroupGuidelinesIfNeeded(
                        uiState.isBeldexHostedOpenGroup
                    )

                    // ---------- Message request accepted ----------
                    if (uiState.isMessageRequestAccepted == true) {
                        binding.messageRequestBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun addOpenGroupGuidelinesIfNeeded(isBeldexHostedOpenGroup : Boolean) {
        if (!isBeldexHostedOpenGroup) {
            return
        }
        binding.openGroupGuidelinesView.visibility=View.VISIBLE
        val recyclerViewLayoutParams=
            binding.conversationRecyclerView.layoutParams as RelativeLayout.LayoutParams
        recyclerViewLayoutParams.topMargin=toPx(
            5,
            resources
        ) // The height of the social group guidelines view is hardcoded to this
        binding.conversationRecyclerView.layoutParams=recyclerViewLayoutParams
    }

    private fun setMediaControlForReportIssue() {
        val recipient=viewModel.recipient.value ?: return
        if (recipient.address.toString() == HomeActivity.reportIssueBChatID) {
            binding.inputBar.showMediaControls=true
        }
    }

    private fun updateUnreadCountIndicator() {

        val count = unreadCount
        if (count <= 0) {
            binding.unreadCountIndicator.isVisible = false
            return
        }

        val displayText = if (count < 10_000) {
            count.toString()
        } else {
            "9999+"
        }

        binding.unreadCountTextView.apply {
            text = displayText
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                if (count < 10_000) 12f else 9f
            )
            setTypeface(
                Typeface.DEFAULT,
                if (count < 100) Typeface.BOLD else Typeface.NORMAL
            )
        }

        binding.unreadCountIndicator.isVisible = true
    }

    private fun updateSubtitle() {

        val recipient = viewModel.recipient.value ?: run {
            binding.conversationSubtitleView.isVisible = false
            binding.muteIconImageView.isVisible = false
            return
        }

        binding.muteIconImageView.isVisible = recipient.isMuted

        when {
            // ---------- Muted ----------
            recipient.isMuted -> {
                binding.conversationSubtitleView.isVisible = true
                binding.conversationSubtitleView.text =
                    if (recipient.mutedUntil != Long.MAX_VALUE) {
                        getString(
                            R.string.ConversationActivity_muted_until_date,
                            DateUtils.getFormattedDateTime(
                                recipient.mutedUntil,
                                "EEE, MMM d, yyyy HH:mm",
                                Locale.getDefault()
                            )
                        )
                    } else {
                        getString(R.string.ConversationActivity_muted_forever)
                    }
            }

            // ---------- Group ----------
            recipient.isGroupRecipient -> {
                updateGroupSubtitle(recipient)
            }

            // ---------- Default ----------
            else -> {
                binding.conversationSubtitleView.isVisible = false
            }
        }
    }

    private fun updateGroupSubtitle(recipient : Recipient) {
        val groupID : String=recipient.address.toGroupString()
        val members=groupRepository?.getGroupMembers(groupID)
        val memberCount=members?.members?.size ?: 0
        binding.conversationSubtitleView.isVisible=true
        binding.conversationSubtitleView.text=
            if (memberCount > 1) "$memberCount members" else "$memberCount member"
    }

    private fun setUpBlockedBanner() {

        val recipient = viewModel.recipient.value ?: run {
            binding.blockedBanner.isVisible = false
            return
        }

        // Groups never show block banner
        if (recipient.isGroupRecipient) {
            binding.blockedBanner.isVisible = false
            return
        }

        binding.blockedBanner.isVisible = recipient.isBlocked

        // Adjust UI based on block state
        setConversationRecyclerViewLayout(recipient.isBlocked)
        showBlockProgressBar(recipient)

        // Consume click to avoid click-through (DO NOT REMOVE)
        binding.blockedBanner.setOnClickListener { /* no-op */ }

        setUpBlockedBannerActions()
    }

    private fun setUpBlockedBannerActions() {
        binding.clearChat.setOnClickListener {
            if(SystemClock.elapsedRealtime() - clearChatButtonLastClickTiem >= 1000) {
                clearChatButtonLastClickTiem = SystemClock.elapsedRealtime()
                clearChatDialog()
            }
        }
        binding.unblockButton.setOnClickListener {
            if(SystemClock.elapsedRealtime() - unblockButtonLastClickTime >= 1000) {
                unblockButtonLastClickTime = SystemClock.elapsedRealtime()
                unblockContactDialog()
            }
        }
    }

    private fun showBlockProgressBar(recipient: Recipient?) {

        if (recipient == null || recipient.isGroupRecipient) {
            hideBlockProgress()
            return
        }

        val shouldShowProgress =
            recipient.hasApprovedMe() &&
                    recipient.isApproved &&
                    !recipient.isBlocked &&
                    !recipient.isLocalNumber &&
                    TextSecurePreferences.isPayAsYouChat(this) &&
                    HomeActivity.reportIssueBChatID != recipient.address.toString()

        blockProgressBarVisible = shouldShowProgress

        if (shouldShowProgress) {
            binding.inputBar.showProgressBar(true)
        } else {
            hideBlockProgress()
        }
    }

    private fun hideBlockProgress() {
        binding.inputBar.showFailedProgressBar(false)
        binding.inputBar.showProgressBar(false)
        blockProgressBarVisible = false
    }

    private fun isOutgoingMessageRequestThread() : Boolean {
        val recipient=viewModel.recipient.value ?: return false
        return !recipient.isGroupRecipient &&
                !recipient.isLocalNumber &&
                !(recipient.hasApprovedMe() || viewModel.hasReceived())
    }

    private fun isIncomingMessageRequestThread() : Boolean {
        val recipient=viewModel.recipient.value ?: return false
        return !recipient.isGroupRecipient &&
                !recipient.isApproved &&
                !recipient.isLocalNumber &&
                !threadDb.getLastSeenAndHasSent(viewModel.threadId).second() &&
                threadDb.getMessageCount(viewModel.threadId) > 0
    }

    private fun setUpMessageRequestsBar() {
        val recipient=viewModel.recipient.value ?: return
        if (recipient.address.toString() != HomeActivity.reportIssueBChatID) {
            binding.inputBar.showMediaControls=!isOutgoingMessageRequestThread()
        }
        binding.messageRequestBar.isVisible=isIncomingMessageRequestThread()
        binding.acceptMessageRequestButton.setOnClickListener {
            acceptAlertDialog()
        }
        binding.messageRequestBlock.setOnClickListener {
            block(deleteThread=true)
        }
        binding.declineMessageRequestButton.setOnClickListener {
            declineAlertDialog()
        }
    }

    private fun setSearchView() {

        binding.searchUp.setOnClickListener { onSearchMoveUpPressed() }
        binding.searchDown.setOnClickListener { onSearchMoveDownPressed() }

        binding.closeSearch.setOnClickListener { onSearchClosed() }
        binding.searchClose.setOnClickListener { onSearchClosed() }

        binding.searchQuery.addTextChangedListener(
            object : SimpleTextWatcher() {

                override fun onTextChanged(text: String?) {
                    handleSearchTextChanged(text)
                }
            }
        )
    }

    private fun showOrHideInputIfNeeded() {
        binding.inputBar.showInput=isSecretGroupIsActive()
    }

    private fun handleSearchTextChanged(text: String?) {

        val query = text?.trim().orEmpty()

        if (query.isNotEmpty()) {
            onSearchQueryUpdated(query)

            binding.searchProgress.isVisible = true
            binding.closeSearch.isVisible = true
            binding.search.isVisible = false
            binding.searchClose.isVisible = false

        } else {
            resetSearchUi()
            adapter.onSearchQueryUpdated("")
        }
    }

    private fun resetSearchUi() {
        binding.closeSearch.isVisible = false
        binding.search.isVisible = true
        binding.searchUp.isVisible = false
        binding.searchDown.isVisible = false
        binding.searchClose.isVisible = true
        binding.noMatchesFoundTextview.isVisible = false
        binding.searchProgress.isVisible = false
    }

    // `position` is the adapter position; not the visual position
    private fun handlePress(
        message : MessageRecord,
        position : Int,
        view : VisibleMessageView,
        event : MotionEvent
    ) {
        val actionMode=this.actionMode
        selectedEvent=event
        selectedView=view
        selectedMessageRecord=message
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

    private fun onDeselect(message : MessageRecord, position : Int, actionMode : ActionMode) {
        adapter.toggleSelection(message, position)
        val actionModeCallback=
            ConversationActionModeCallback(adapter, viewModel.threadId, this)
        actionModeCallback.delegate=this
        actionModeCallback.updateActionModeMenu(actionMode.menu)
        if (adapter.selectedItems.isEmpty()) {
            actionMode.finish()
            this.actionMode=null
        }
    }

    // `position` is the adapter position; not the visual position
    private fun handleSwipeToReply(message : MessageRecord) {
        //New Line
//        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
//        params.bottomMargin = 16
        val recipient=viewModel.recipient.value ?: return
        binding.slideToPayButton.visibility=View.GONE
        binding.inputBar.draftQuote(recipient, message, glide)
        setConversationRecyclerViewLayout(true)
    }

    private fun showOrHidScrollToBottomButton(show : Boolean=true) {
        binding.scrollToBottomButton.isVisible=show && !isScrolledToBottom && adapter.itemCount > 0
    }

    fun isSecretGroupIsActive() : Boolean {
        val recipient=viewModel.recipient.value
        return if (recipient != null && recipient.isClosedGroupRecipient) {
            val group=viewModel.getGroup(recipient)
            val isActive=(group?.isActive == true)
            isActive
        } else {
            true
        }
    }

    private fun selectMessage(message: MessageRecord, position: Int) {
        val actionMode=this.actionMode
        val actionModeCallback=
            ConversationActionModeCallback(adapter, viewModel.threadId, this)
        actionModeCallback.delegate=this
        searchViewItem?.collapseActionView()
        if (actionMode == null) { // Nothing should be selected if this is the case
            adapter.toggleSelection(message, position)
            this.actionMode=
                this.startActionMode(actionModeCallback, ActionMode.TYPE_PRIMARY)
        } else {
            adapter.toggleSelection(message, position)
            actionModeCallback.updateActionModeMenu(actionMode.menu)
            if (adapter.selectedItems.isEmpty()) {
                actionMode.finish()
                this.actionMode=null
            }
        }
    }

    private fun handleRecyclerViewScrolled() {
        val binding =this.binding

        val wasTypingVisible = binding.typingIndicatorViewContainer.isVisible

        binding.typingIndicatorViewContainer.isVisible =
            wasTypingVisible && isScrolledToBottom

        val isTypingVisibleNow = binding.typingIndicatorViewContainer.isVisible
        if (wasTypingVisible != isTypingVisibleNow) {
            inputBarHeightChanged(binding.inputBar.height)
        }

        showScrollToBottomButtonIfApplicable()

        val firstVisiblePosition =
            layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION

        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            unreadCount = min(unreadCount, firstVisiblePosition).coerceAtLeast(0)
            updateUnreadCountIndicator()
        }
    }

    private fun showScrollToBottomButtonIfApplicable() {
        binding.scrollToBottomButton.isVisible=!isScrolledToBottom && adapter.itemCount > 0
    }

    private fun moveToMessagePosition(position : Int, onMessageNotFound : Runnable?) {
        if (position >= 0) {
            binding.conversationRecyclerView.scrollToPosition(position)
        } else {
            onMessageNotFound?.run()
        }
    }

    private fun hideAttachmentContainer() {
        isShowingAttachmentOptions=false
        binding.attachmentContainer.isVisible=isShowingAttachmentOptions
    }

    private fun callSecretGroupInfo(recipient : Recipient) {
        SecretGroupInfoComposeActivity.setOnActionSelectedListener(this)
        val intent=Intent(this, SecretGroupInfoComposeActivity::class.java).apply {
            putExtra(
                SecretGroupInfoComposeActivity.secretGroupID,
                recipient.address.toGroupString()
            )
        }
        startActivity(intent)
    }

    private fun gifInfoDialog() {
        val dialog = ConversationActionDialog().apply {
            arguments = Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.GifSetting
                )
            }
            setListener(this@ConversationActivityV2)
        }

        dialog.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    private fun showGIFPicker() {
        val hasSeenGIFMetaDataWarning : Boolean=textSecurePreferences.hasSeenGIFMetaDataWarning()

        if (!hasSeenGIFMetaDataWarning) {
            gifInfoDialog()
        } else {
            AttachmentManager.selectGif(this, PICK_GIF)
        }
    }

    private fun showDocumentPicker() {
        AttachmentManager.selectDocument(this, PICK_DOCUMENT)
    }

    private fun pickFromLibrary() {
        val recipient = viewModel.recipient.value ?: return
        val text = binding.inputBar.text.trim()

        AttachmentManager.selectGallery(
            this,
            PICK_FROM_LIBRARY,
            recipient,
            text
        )
    }

    private fun showCamera() {
        attachmentManager.capturePhoto(
            this,
            TAKE_PHOTO,
            viewModel.recipient.value
        )
    }

    private val contactActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

            val contacts: List<ContactModel> =
                result.data
                    ?.serializable<ArrayList<ContactModel>>(
                        ContactSharingActivity.RESULT_CONTACT_TO_SHARE
                    )
                    ?.toList()
                    ?: emptyList()

            val quote = binding.inputBar.quote

            if (quote != null) {
                sendAttachments(
                    attachments = emptyList(),
                    body = null,
                    quotedMessage = quote,
                    linkPreview = binding.inputBar.linkPreview,
                    contacts = contacts
                )
            } else {
                binding.conversationRecyclerView.scrollToPosition(0)
                shareContact(contacts)
            }
        }

    private fun prepMediaForSending(
        uri : Uri,
        type : AttachmentManager.MediaType
    ) : ListenableFuture<Boolean> {
        return prepMediaForSending(uri, type, null, null)
    }

    private fun prepMediaForSending(
        uri : Uri,
        type : AttachmentManager.MediaType,
        width : Int?,
        height : Int?
    ) : ListenableFuture<Boolean> {
        return attachmentManager.setMedia(
            glide,
            uri,
            type,
            MediaConstraints.getPushMediaConstraints(),
            width ?: 0,
            height ?: 0
        )
    }

    private fun sendAttachments(
        attachments: List<Attachment>,
        body: String?,
        quotedMessage: MessageRecord? = binding.inputBar.quote,
        linkPreview: LinkPreview? = null,
        contacts: List<ContactModel> = emptyList()
    ) {
        val recipient = viewModel.recipient.value ?: return

        binding.conversationRecyclerView.scrollToPosition(0)

        // Request approval if needed
        processMessageRequestApproval()
            ?.let { conversationApprovalJob = it }

        // Create base message
        val message = VisibleMessage().apply {
            sentTimestamp = MnodeAPI.nowWithOffset
            text = body
        }

        val quote=quotedMessage?.let {
            val quotedAttachments=
                (it as? MmsMessageRecord)?.slideDeck?.asAttachments() ?: listOf()
            val sender=
                if (it.isOutgoing) Address.fromSerialized(
                    textSecurePreferences.getLocalNumber()!!
                ) else it.individualRecipient.address
            //Payment Tag
            var quoteBody=it.body
            if (it.isPayment) {
                //Payment Tag
                var amount=""
                try {
                    val mainObject=JSONObject(it.body)
                    val uniObject=mainObject.getJSONObject("kind")
                    amount=uniObject.getString("amount")
                } catch (e : JSONException) {
                    e.printStackTrace()
                }
                val direction : String=if (it.isOutgoing) {
                    resources.getString(R.string.payment_sent)
                } else {
                    resources.getString(R.string.payment_received)
                }
                quoteBody=
                    resources.getString(R.string.reply_payment_card_message, direction, amount)
            } else if (it.isOpenGroupInvitation) {
                quoteBody=resources.getString(R.string.ThreadRecord_open_group_invitation)
            }
            QuoteModel(it.dateSent, sender, quoteBody, false, quotedAttachments)
        }

        // Reset input state
        binding.inputBar.text = ""
        binding.inputBar.cancelQuoteDraft(2)
        binding.inputBar.cancelLinkPreviewDraft(2)

        previousText = ""
        currentMentionStartIndex = -1
        mentions.clear()

        attachmentManager.clear()

        if (isShowingAttachmentOptions) {
            toggleAttachmentOptions()
        }

        val outgoingMediaMessage =
            if (contacts.isNotEmpty()) {
                val sharedContact = createSharedContact(contacts)
                message.sharedContact = sharedContact

                OutgoingMediaMessage.fromSharedContact(
                    message,
                    recipient,
                    attachments,
                    quote,
                    linkPreview
                )
            } else {
                OutgoingMediaMessage.from(
                    message,
                    recipient,
                    attachments,
                    quote,
                    linkPreview
                )
            }

        lifecycleScope.launch(Dispatchers.Default) {
            message.id = viewModel.insertMessageOutBox(outgoingMediaMessage)

            waitForApprovalJobToBeSubmitted()

            MessageSender.send(
                message,
                recipient.address,
                attachments,
                quote,
                linkPreview
            )
        }

        ApplicationContext
            .getInstance(this)
            .typingStatusSender
            .onTypingStopped(viewModel.threadId)
    }

    private fun processMessageRequestApproval(): Job? {
        return when {
            binding.messageRequestBar.isVisible -> {
                acceptMessageRequest()
            }

            viewModel.recipient.value?.isApproved == false -> {
                // Edge case: new outgoing thread for a new recipient
                // without having sent approval messages yet
                viewModel.setRecipientApproved()
                null
            }

            else -> null
        }
    }

    private fun buildPaymentQuote(record: MessageRecord): String {
        var amount = ""

        try {
            val json = JSONObject(record.body)
            val kind = json.getJSONObject("kind")
            amount = kind.getString("amount")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val direction = if (record.isOutgoing) {
            getString(R.string.payment_sent)
        } else {
            getString(R.string.payment_received)
        }

        return getString(
            R.string.reply_payment_card_message,
            direction,
            amount
        )
    }

    private fun createSharedContact(
        contacts: List<ContactModel>
    ): SharedContact {
        val addresses = contacts.map { it.address.serialize() }
        val names = contacts.map { it.name }

        return SharedContact(
            address = Json.encodeToString(addresses),
            name = Json.encodeToString(names)
        )
    }

    private fun shareContact(contacts: List<ContactModel>) {
        val recipient = viewModel.recipient.value ?: return
        processMessageRequestApproval()

        // Create the message
        val message = VisibleMessage()
        message.sentTimestamp = MnodeAPI.nowWithOffset

        // Collect all addresses and names
        val addresses = contacts.map { it.address.serialize()}
        val names = contacts.map { it.name }

        val contact = SharedContact(
            address = Json.encodeToString(addresses),
            name = Json.encodeToString(names)
        )
        message.sharedContact = contact

        val outgoingTextMessage = OutgoingTextMessage.fromSharedContact(
            contact,
            recipient,
            message.sentTimestamp
        )

        // Put the message in the database
        message.id = viewModel.insertMessageOutBoxSMS(outgoingTextMessage, message.sentTimestamp)

        // Send it
        MessageSender.send(message, recipient.address)

        // Send a typing stopped message
        ApplicationContext.getInstance(this).typingStatusSender.onTypingStopped(
            viewModel.threadId
        )
    }

    private suspend fun waitForApprovalJobToBeSubmitted() {
        conversationApprovalJob?.join()
        conversationApprovalJob = null
    }

    private fun acceptMessageRequest(): Job {
        binding.messageRequestBar.isVisible = false

        binding.conversationRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                true
            )

        adapter.notifyDataSetChanged()

        viewModel.acceptMessageRequest()

        LoaderManager
            .getInstance(this)
            .restartLoader(0, null, this)

        // Launch background sync and return the Job
        return lifecycleScope.launch(Dispatchers.IO) {
            ConfigurationMessageUtilities
                .forceSyncConfigurationNowIfNeeded(this@ConversationActivityV2)
        }
    }

    private fun clearChatDialog() {
        val dialog = ConversationActionDialog().apply {
            arguments = Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.ClearChat
                )
            }
            setListener(this@ConversationActivityV2)
        }

        dialog.show(
            supportFragmentManager,
            ConversationActionDialog.TAG
        )
    }

    private fun unblockContactDialog() {
        val dialog=ConversationActionDialog()
        dialog.apply {
            arguments=Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.UnblockUser
                )
            }
            setListener(this@ConversationActivityV2)
        }
        dialog.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    private fun acceptAlertDialog() {
        val acceptRequest=ConversationActionDialog()
        acceptRequest.apply {
            arguments=Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.AcceptRequest
                )
            }
            setListener(this@ConversationActivityV2)
        }
        acceptRequest.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    private fun declineAlertDialog() {
        val declineRequest=ConversationActionDialog()
        declineRequest.apply {
            arguments=Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.DeclineRequest
                )
            }
            setListener(this@ConversationActivityV2)
        }
        declineRequest.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    private fun sendScreenShotTakenNotification() {
        val recipient = viewModel.recipient.value ?: return

        if (!recipient.hasApprovedMe() || !recipient.isApproved) return

        val localNumber = textSecurePreferences.getLocalNumber()
        val isNoteToSelf = recipient.address.toString() == localNumber

        if (recipient.isGroupRecipient || recipient.isBlocked || isNoteToSelf) {
            return
        }

        val kind = DataExtractionNotification.Kind.Screenshot()
        val message = DataExtractionNotification(kind).apply {
            this.recipient = recipient.address.serialize()
            sentTimestamp = MnodeAPI.nowWithOffset
        }

        val screenshotMessageManager =
            ApplicationContext
                .getInstance(this)
                .expiringMessageManager

        screenshotMessageManager.setScreenShotMessage(message)

        MessageSender.send(message, recipient.address)
    }

    private fun showOrHideMentionCandidatesIfNeeded(text: CharSequence) {

        // Handle text deletion
        if (text.length < previousText.length) {
            currentMentionStartIndex = -1
            hideMentionCandidates()

            val mentionsToRemove =
                mentions.filterNot { text.contains(it.displayName) }

            mentions.removeAll(mentionsToRemove)
        }

        if (text.isEmpty()) {
            currentMentionStartIndex = -1
            hideMentionCandidates()
            previousText = text
            return
        }

        val lastIndex = text.lastIndex
        val lastChar = text[lastIndex]

        val isStartOrWhitespaceBefore =
            lastIndex == 0 || Character.isWhitespace(text[lastIndex - 1])

        when {
            lastChar == '@' && isStartOrWhitespaceBefore -> {
                currentMentionStartIndex = lastIndex
                showOrUpdateMentionCandidatesIfNeeded()
            }

            Character.isWhitespace(lastChar) || lastChar == '@' -> {
                // '@@' or space ends mention
                currentMentionStartIndex = -1
                hideMentionCandidates()
            }

            currentMentionStartIndex != -1 -> {
                val query = text.substring(currentMentionStartIndex + 1)
                showOrUpdateMentionCandidatesIfNeeded(query)
            }
        }

        previousText = text
    }

    private fun hideMentionCandidates() {
        if (isShowingMentionCandidatesView) {
            val mentionCandidatesView=mentionCandidatesView ?: return
            val animation=
                ValueAnimator.ofObject(FloatEvaluator(), mentionCandidatesView.alpha, 0.0f)
            animation.duration=250L
            animation.addUpdateListener { animator ->
                mentionCandidatesView.alpha=animator.animatedValue as Float
                if (animator.animatedFraction == 1.0f) {
                    binding.additionalContentContainer.removeAllViews()
                }
            }
            animation.start()
        }
        isShowingMentionCandidatesView=false
    }

    private fun showOrUpdateMentionCandidatesIfNeeded(query: String = "") {
        val recipient = viewModel.recipient.value ?: return
        val container = binding.additionalContentContainer

        val candidates = MentionsManager.getMentionCandidates(
            query,
            viewModel.threadId,
            recipient.isOpenGroupRecipient
        )

        if (!isShowingMentionCandidatesView) {
            container.removeAllViews()

            val view = MentionCandidatesView(this).apply {
                glide = this@ConversationActivityV2.glide
                onCandidateSelected = { handleMentionSelected(it) }
            }

            container.addView(view)
            mentionCandidatesView = view
            view.show(candidates, viewModel.threadId)
        } else {
            mentionCandidatesView?.setMentionCandidates(candidates)
        }

        isShowingMentionCandidatesView = true
    }

    private fun handleMentionSelected(mention : Mention) {
        val binding=binding
        if (currentMentionStartIndex == -1) {
            return
        }
        mentions.add(mention)
        val previousText=binding.inputBar.text
        val newText=
            previousText.substring(
                0,
                currentMentionStartIndex
            ) + "@" + mention.displayName + " "
        binding.inputBar.text=newText
        binding.inputBar.setSelection(newText.length)
        currentMentionStartIndex=-1
        hideMentionCandidates()
        this.previousText=newText
    }

    private fun checkUnBlock() {
        val recipient=viewModel.recipient.value ?: return
        if (recipient.isBlocked) {
            unblock()
            return
        }
    }

    private fun handleAttachment(isHidden: Boolean) {
        val visibility = if (isHidden) View.GONE else View.VISIBLE
        binding.inputBar.containerCardView.visibility = visibility
    }

    private fun calculateDampedTranslation(
        translation: Float,
        damping: Float
    ): Float {
        val sign = -1f
        return damping * (sqrt(abs(translation)) / sqrt(damping)) * sign
    }

    private fun endActionMode() {
        this.actionMode?.finish()
        this.actionMode=null
    }

    private fun getRemainingTime(startedAt: Long, expiresIn: Long) : Long {
        val progressed: Long = System.currentTimeMillis() - startedAt
        val remaining: Long = expiresIn - progressed
        return remaining
    }

    fun sendEmojiReaction(emoji : String, originalMessage : MessageRecord) {
        // Create the message
        val recipient=viewModel.recipient.value ?: return
        if (recipient.isBlocked) {
            unblock()
            return
        }
        val reactionMessage=VisibleMessage()
        val emojiTimestamp=System.currentTimeMillis()
        reactionMessage.sentTimestamp=emojiTimestamp
        val author=textSecurePreferences.getLocalNumber()!!
        // Put the message in the database
        val reaction=ReactionRecord(
            messageId=originalMessage.id,
            isMms=originalMessage.isMms,
            author=author,
            emoji=emoji,
            count=1,
            dateSent=emojiTimestamp,
            dateReceived=emojiTimestamp
        )
        reactionDb.addReaction(
            MessageId(originalMessage.id, originalMessage.isMms),
            reaction,
            false
        )
        val originalAuthor=if (originalMessage.isOutgoing) {
            textSecurePreferences.getLocalNumber()!!
        } else originalMessage.individualRecipient.address.serialize()
        // Send it
        reactionMessage.reaction=
            Reaction.from(originalMessage.timestamp, originalAuthor, emoji, true)
        if (recipient.isOpenGroupRecipient) {
            val messageServerId=
                beldexMessageDb.getServerID(originalMessage.id, !originalMessage.isMms) ?: return
            viewModel.openGroup?.let {
                OpenGroupAPIV2.addReaction(it.room, it.server, messageServerId, emoji)
            }
        } else {
            MessageSender.send(reactionMessage, recipient.address)
        }
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

    fun sendEmojiRemoval(emoji : String, originalMessage : MessageRecord) {
        val recipient=viewModel.recipient.value ?: return
        if (recipient.isBlocked) {
            unblock()
            return
        }
        val author=textSecurePreferences.getLocalNumber()!!
        reactionDb.deleteReaction(
            emoji,
            MessageId(originalMessage.id, originalMessage.isMms),
            author,
            false
        )
        val message=VisibleMessage()
        val emojiTimestamp=System.currentTimeMillis()
        message.sentTimestamp=emojiTimestamp
        message.reaction=Reaction.from(originalMessage.timestamp, author, emoji, false)
        if (recipient.isOpenGroupRecipient) {
            val messageServerId=
                beldexMessageDb.getServerID(originalMessage.id, !originalMessage.isMms) ?: return
            viewModel.openGroup?.let {
                OpenGroupAPIV2.deleteReaction(it.room, it.server, messageServerId, emoji)
            }
        } else {
            MessageSender.send(message, recipient.address)
        }
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

    fun reactionDelegateDismiss() {
        if (reactionDelegate.isShowing) {
            reactionDelegate.hide()
        }
    }

    private fun networkChange(networkAvailable: Boolean) {

        isNetworkAvailable = networkAvailable

        if (networkAvailable) {
            binding.connectedStatus.text = getString(R.string.connected)
            binding.networkStatusImage.setImageResource(R.drawable.ic_connected)

            Handler(Looper.getMainLooper()).postDelayed({
                binding.networkStatusLayout.visibility = View.GONE
            }, 3000L)

        } else {
            binding.networkStatusLayout.visibility = View.VISIBLE
            binding.connectedStatus.text = getString(R.string.no_connection)
            binding.networkStatusImage.setImageResource(R.drawable.ic_try_to_connect)
        }
    }

    private fun getMessageBody() : String {
        var result=binding.inputBar.text.trim()
        for (mention in mentions) {
            try {
                val startIndex=result.indexOf("@" + mention.displayName)
                val endIndex=
                    startIndex + mention.displayName.count() + 1 // + 1 to include the "@"
                result=
                    result.substring(
                        0,
                        startIndex
                    ) + "@" + mention.publicKey + result.substring(
                        endIndex
                    )
            } catch (exception : Exception) {
                Timber.tag("Beldex").d("Failed to process mention due to error: $exception")
            }
        }
        return result
    }

    private fun callSendTextOnlyMessage() {
        if (binding.inputBar.text.length > 4096) {
            Toast.makeText(
                this,
                "Text limit exceed: Maximum limit of messages is 4096 characters",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            binding.conversationRecyclerView.scrollToPosition(0)
            sendTextOnlyMessage()
        }
    }

    fun showAllMediaScreen() {
        val recipient=viewModel.recipient.value ?: return
        if (recipient.isClosedGroupRecipient) {
            callSecretGroupInfo(recipient)
        } else {
            val intent = Intent(this, MediaOverviewActivity::class.java)
            intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, recipient.address)
            passSharedMessageToConversationScreen.launch(intent)
        }
    }

    fun showAllMediaView(thread : Recipient){
        val intent = Intent(this, MediaOverviewActivity::class.java)
        intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, thread.address)
        passSharedMessageToConversationScreen.launch(intent)
    }

    val passSharedMessageToConversationScreen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if(result.data!=null){
                val extras = Bundle()
                val address = intent.parcelable<Address>(ADDRESS)
                extras.putParcelable(ADDRESS, address)
                extras.putLong(THREAD_ID, result.data!!.getLongExtra(THREAD_ID,-1))
                val uri = intent.parcelable<Uri>(URI)
                extras.putParcelable(URI, uri)
                extras.putString(TYPE,result.data!!.getStringExtra(TYPE))
                extras.putCharSequence(Intent.EXTRA_TEXT,result.data!!.getCharSequenceExtra(Intent.EXTRA_TEXT))
                //Shortcut launcher
                extras.putBoolean(SHORTCUT_LAUNCHER,true)
                val intent = Intent(this, ConversationActivityV2::class.java)
                intent.putExtras(extras)
                startActivity(intent)
            }
        }
    }

    private fun onSearchOpened() {
        searchViewModel.onSearchOpened()
        binding.searchBar.visibility=View.VISIBLE
        binding.noMatchesFoundTextview.visibility=View.GONE
        binding.searchProgress.visibility = View.GONE
        binding.searchQuery.setText("")
        binding.searchQuery.requestFocus()
    }

    private fun onSearchClosed() {
        searchViewModel.onSearchClosed()
        binding.searchProgress.visibility = View.GONE
        binding.searchBar.visibility=View.GONE
        binding.noMatchesFoundTextview.visibility=View.GONE
        adapter.onSearchQueryUpdated(null)
        binding.searchQuery.clearFocus()
        this.invalidateOptionsMenu()
        hideKeyboard()
    }

    fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchQuery.windowToken, 0)
    }

    fun onSearchQueryUpdated(query : String) {
        if (query.trim().isNotEmpty()) {
            searchViewModel.onQueryUpdated(query, viewModel.threadId)
            binding.searchProgress.visibility=View.VISIBLE
            adapter.onSearchQueryUpdated(query)
        }
    }

    private fun sendTextOnlyMessage(hasPermissionToSendSeed : Boolean=false) {
        val recipient=viewModel.recipient.value ?: return
        //New Line v32
        processMessageRequestApproval().let { conversationApprovalJob = it }

        val text=getMessageBody()
        val userPublicKey= textSecurePreferences.getLocalNumber()
        val isNoteToSelf=
            (recipient.isContactRecipient && recipient.address.toString() == userPublicKey)
        if (text.contains(seed) && !isNoteToSelf && !hasPermissionToSendSeed) {
            val dialog=SendSeedDialog { sendTextOnlyMessage(true) }
            return dialog.show(supportFragmentManager, "Send Seed Dialog")
        }
        // Create the message
        val message=VisibleMessage()
        message.sentTimestamp=MnodeAPI.nowWithOffset
        message.text=text
        val outgoingTextMessage=OutgoingTextMessage.from(message, viewModel.recipient.value)
        // Clear the input bar
        binding.inputBar.text=""
        //New Line
//        val params = binding.attachmentOptionsContainer.layoutParams as ViewGroup.MarginLayoutParams
//        params.bottomMargin = 16

        binding.inputBar.cancelQuoteDraft(2)
        binding.inputBar.cancelLinkPreviewDraft(2)
        // Clear mentions
        previousText=""
        currentMentionStartIndex=-1
        mentions.clear()
        lifecycleScope.launch(Dispatchers.Default) {
            // Put the message in the database
            message.id=viewModel.insertMessageOutBoxSMS(outgoingTextMessage, message.sentTimestamp)

            waitForApprovalJobToBeSubmitted()
            // Send it
            MessageSender.send(message, recipient.address)
        }
        // Send a typing stopped message
        ApplicationContext.getInstance(this).typingStatusSender.onTypingStopped(
            viewModel.threadId
        )
    }

    private fun isValidLockViewLocation(x : Int, y : Int) : Boolean {
        // We can be anywhere above the lock view and a bit to the side of it (at most `lockViewHitMargin`
        // to the side)
        val binding=binding
        val lockViewLocation=IntArray(2) { 0 }
        binding.inputBarRecordingView.lockView.getLocationOnScreen(lockViewLocation)
        val hitRect=Rect(
            lockViewLocation[0] - lockViewHitMargin,
            0,
            lockViewLocation[0] + binding.inputBarRecordingView.lockView.width + lockViewHitMargin,
            lockViewLocation[1] + binding.inputBarRecordingView.lockView.height
        )
        return hitRect.contains(x, y)
    }

    private fun expandVoiceMessageLockView() {
        val lockView=binding.inputBarRecordingView.lockView
        val animation=ValueAnimator.ofObject(FloatEvaluator(), lockView.scaleX, 1.10f)
        animation.duration=250L
        animation.addUpdateListener { animator ->
            lockView.scaleX=animator.animatedValue as Float
            lockView.scaleY=animator.animatedValue as Float
        }
        animation.start()
    }

    private fun collapseVoiceMessageLockView() {
        val lockView=binding.inputBarRecordingView.lockView
        val animation=ValueAnimator.ofObject(FloatEvaluator(), lockView.scaleX, 1.0f)
        animation.duration=250L
        animation.addUpdateListener { animator ->
            lockView.scaleX=animator.animatedValue as Float
            lockView.scaleY=animator.animatedValue as Float
        }
        animation.start()
    }

    private fun hideVoiceMessageUI() {
        try {
            handleAttachment(false)
            val chevronImageView=binding.inputBarRecordingView.chevronImageView
            val slideToCancelTextView=binding.inputBarRecordingView.slideToCancelTextView
            listOf(chevronImageView, slideToCancelTextView).forEach { view ->
                val animation=ValueAnimator.ofObject(FloatEvaluator(), view.translationX, 0.0f)
                animation.duration=250L
                animation.addUpdateListener { animator ->
                    view.translationX=animator.animatedValue as Float
                }
                animation.start()
            }
            binding.inputBarRecordingView.hide()
        } catch (e : UninitializedPropertyAccessException) {
            println("Hide voice message -> ${e.localizedMessage}")
        }
    }

    private fun sendMediaSavedNotification() {
        val recipient=viewModel.recipient.value ?: return
        if (recipient.isGroupRecipient) {
            return
        }
        val timestamp=MnodeAPI.nowWithOffset
        val kind=DataExtractionNotification.Kind.MediaSaved(timestamp)
        val message=DataExtractionNotification(kind)
        MessageSender.send(message, recipient.address)
    }

    private fun deleteBlockedConversation() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Cancel any outstanding jobs
            DatabaseComponent.get(this@ConversationActivityV2).bchatJobDatabase()
                .cancelPendingMessageSendJobs(viewModel.threadId)
            // Delete the conversation
            lifecycleScope.launch(Dispatchers.IO) {
                threadDb.deleteConversation(viewModel.threadId)
            }
            // Update the badge count
            ApplicationContext.getInstance(this@ConversationActivityV2).messageNotifier.updateNotification(
                this@ConversationActivityV2
            )
            // Notify the user
            val toastMessage=R.string.activity_home_conversation_deleted_message
            Toast.makeText(this@ConversationActivityV2, toastMessage, Toast.LENGTH_LONG).show()
            walletOnBackPressed()
        }
    }

    private fun walletOnBackPressed() {
        reactionDelegateDismiss()
        if (getIsReactionOverlayVisible(this)) {
            setIsReactionOverlayVisible(this, false)
        }
        try {
            onBackPressedDispatcher.onBackPressed()
        } catch (e : IllegalStateException) {
            //Need to check
            //replaceHomeFragment()
        }
    }

    fun backToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode : Int,
        permissions : Array<out String>,
        grantResults : IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode : Int, resultCode : Int, intent : Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        val mediaPreppedListener=object : ListenableFuture.Listener<Boolean> {

            override fun onSuccess(result : Boolean?) {
                if (result == true) {
                    sendAttachments(attachmentManager.buildSlideDeck().asAttachments(), null)
                }
            }

            override fun onFailure(e : ExecutionException?) {
                    Toast.makeText(
                        this@ConversationActivityV2,
                        R.string.activity_conversation_attachment_prep_failed,
                        Toast.LENGTH_LONG
                    ).show()
            }
        }
        when (requestCode) {
            PICK_DOCUMENT -> {
                val uri=intent?.data ?: return
                prepMediaForSending(uri, AttachmentManager.MediaType.DOCUMENT).addListener(
                    mediaPreppedListener
                )
            }

            PICK_GIF -> {
                intent ?: return
                val uri=intent.data ?: return
                val type=AttachmentManager.MediaType.GIF
                val width=intent.getIntExtra(GiphyActivity.EXTRA_WIDTH, 0)
                val height=intent.getIntExtra(GiphyActivity.EXTRA_HEIGHT, 0)
                prepMediaForSending(uri, type, width, height).addListener(mediaPreppedListener)
            }

            PICK_FROM_LIBRARY,
            TAKE_PHOTO -> {
                intent ?: return
                val body=intent.getStringExtra(MediaSendActivity.EXTRA_MESSAGE)
                val media=intent.getParcelableArrayListExtra<Media>(
                    MediaSendActivity.EXTRA_MEDIA
                ) ?: return
                val slideDeck=SlideDeck()
                for (item in media) {
                    when {
                        MediaUtil.isVideoType(item.mimeType) -> {
                            slideDeck.addSlide(
                                VideoSlide(
                                    this,
                                    item.uri,
                                    0,
                                    item.caption.orNull()
                                )
                            )
                        }

                        MediaUtil.isGif(item.mimeType) -> {
                            slideDeck.addSlide(
                                GifSlide(
                                    this,
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
                                    this,
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
                val extras=intent?.extras ?: return
                if (!intent.hasExtra(SelectContactsActivity.selectedContactsKey)) {
                    return
                }
                val selectedContacts=
                    extras.getStringArray(SelectContactsActivity.selectedContactsKey)!!
                val recipients=selectedContacts.map { contact ->
                    Recipient.from(this, Address.fromSerialized(contact), true)
                }
                viewModel.inviteContacts(recipients)
            }
        }
    }

    override fun getSystemService(name: String): Any? {
        if (name == ActivityDispatcher.SERVICE) {
            return this
        }
        return super.getSystemService(name)
    }

    fun replaceFragment(newFragment: Fragment, stackName: String?, extras: Bundle?) {
        if (extras != null) {
            newFragment.arguments = extras
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.additionalContentContainer, newFragment)
            .addToBackStack(stackName)
            .commit()
    }

















    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onScreenCaptured() {
        sendScreenShotTakenNotification()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        handleBackPressed()
    }

    private fun handleBackPressed() {
        reactionDelegateDismiss()
        if(getIsReactionOverlayVisible(this)){
            setIsReactionOverlayVisible(this,false)
        }
    }

    override fun inputBarHeightChanged(newValue : Int) {
    }

    override fun inputBarEditTextContentChanged(newContent: CharSequence) {

        if (isShowingAttachmentOptions) {
            binding.attachmentContainer.visibility = View.GONE
            isShowingAttachmentOptions = false
        }

        val inputBarText = binding.inputBar.text
        val preferences = textSecurePreferences

        if (preferences.isLinkPreviewsEnabled()) {
            linkPreviewViewModel.onTextChanged(
                this,
                inputBarText,
                0,
                0
            )
        }

        val recipient = viewModel.recipient.value ?: return

        if (recipient.isGroupRecipient) {
            showOrHideMentionCandidatesIfNeeded(newContent)
        }

        val containsWhitelistedLink =
            LinkPreviewUtil.findWhitelistedUrls(newContent.toString()).isNotEmpty()

        if (
            containsWhitelistedLink &&
            !preferences.isLinkPreviewsEnabled() &&
            !preferences.hasSeenLinkPreviewSuggestionDialog()
        ) {
            LinkPreviewDialog {
                setUpLinkPreviewObserver()
                linkPreviewViewModel.onEnabled()
                linkPreviewViewModel.onTextChanged(
                    this,
                    inputBarText,
                    0,
                    0
                )
            }.show(
                supportFragmentManager,
                "Link Preview Dialog"
            )

            preferences.setHasSeenLinkPreviewSuggestionDialog()
        }
    }

    override fun toggleAttachmentOptions() {
        checkUnBlock()
        isShowingAttachmentOptions=!isShowingAttachmentOptions
        binding.attachmentContainer.isVisible=isShowingAttachmentOptions
    }

    override fun showVoiceMessageUI() {
        Helper.hideKeyboard(this)
        handleAttachment(true)
        binding.inputBar.visibility = View.INVISIBLE
        binding.inputBarRecordingView.show()
        val animation = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 250L
            addUpdateListener { animator ->
                binding.inputBar.alpha = animator.animatedValue as Float
            }
        }

        animation.start()
    }

    override fun startRecordingVoiceMessage() {
        if (isAudioPlaying) {
            stopVoiceMessages(audioPlayingIndexInAdapter)
        }

        val callStartTime = callViewModel?.callStartTime ?: -1L
        if (callStartTime != -1L) {
            Toast.makeText(
                this,
                getString(R.string.warning_message_of_voice_recording),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        hideAttachmentContainer()
        checkUnBlock()

        val recipient = viewModel.recipient.value ?: return
        if (recipient.isBlocked) {
            unblock()
            return
        }

        if (Permissions.hasAll(this, Manifest.permission.RECORD_AUDIO)) {

            showVoiceMessageUI()

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            audioRecorder.startRecording()

            // Limit voice messages to 5 minutes
            stopAudioHandler.postDelayed(
                stopVoiceMessageRecordingTask,
                300_000L
            )

        } else {
            Permissions.with(this)
                .request(Manifest.permission.RECORD_AUDIO)
                .withRationaleDialog(
                    getString(
                        R.string.ConversationActivity_to_send_audio_messages_allow_signal_access_to_your_microphone
                    ),
                    getString(R.string.Permissions_record_permission_required),
                    R.drawable.ic_microphone
                )
                .withPermanentDenialDialog(
                    getString(
                        R.string.ConversationActivity_signal_requires_the_microphone_permission_in_order_to_send_audio_messages
                    )
                )
                .execute()
        }
    }

    override fun onMicrophoneButtonMove(event: MotionEvent) {

        val rawX = event.rawX
        val rawY = event.rawY

        val chevronView = binding.inputBarRecordingView.chevronImageView
        val labelView = binding.inputBarRecordingView.slideToCancelTextView

        if (rawX < screenWidth / 2f) {
            val translationX = rawX - screenWidth / 2f

            val chevronX = calculateDampedTranslation(
                translationX,
                damping = 4f
            )

            val labelX = calculateDampedTranslation(
                translationX,
                damping = 3f
            )

            chevronView.translationX = chevronX
            labelView.translationX = labelX
        } else {
            chevronView.translationX = 0f
            labelView.translationX = 0f
        }

        val isInLockArea =
            isValidLockViewLocation(rawX.roundToInt(), rawY.roundToInt())

        if (isInLockArea && !isLockViewExpanded) {
            expandVoiceMessageLockView()
            isLockViewExpanded = true
        } else if (!isInLockArea && isLockViewExpanded) {
            collapseVoiceMessageLockView()
            isLockViewExpanded = false
        }
    }

    override fun onMicrophoneButtonCancel(event : MotionEvent) {
        hideVoiceMessageUI()
    }

    override fun onMicrophoneButtonUp(event : MotionEvent) {
        val x=event.rawX.roundToInt()
        val y=event.rawY.roundToInt()
        if (isValidLockViewLocation(x, y)) {
            binding.inputBarRecordingView.lock()
        } else {
            val recordButtonOverlay=binding.inputBarRecordingView.recordButtonOverlay
            val location=IntArray(2) { 0 }
            recordButtonOverlay.getLocationOnScreen(location)
            val hitRect=Rect(
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
        checkUnBlock()
        val recipient=viewModel.recipient.value
        if (recipient?.isGroupRecipient == true && recipient.isBlocked) {
            unblock()
            return
        }

        val messageText = binding.inputBar.text.trim()

        when {
            messageText.isEmpty() -> {
                Toast.makeText(
                    this,
                    R.string.empty_message_toast,
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.inputBar.linkPreview != null ||
                    binding.inputBar.quote != null -> {

                sendAttachments(
                    attachments = emptyList(),
                    body = getMessageBody(),
                    quotedMessage = binding.inputBar.quote,
                    linkPreview = binding.inputBar.linkPreview
                )
            }

            else -> {
                Timber.tag("SendMessage").d("Sending text-only message")
                callSendTextOnlyMessage()
            }
        }
    }

    override fun commitInputContent(contentUri : Uri) {
        val recipient=viewModel.recipient.value ?: return
        val media=Media(
            contentUri,
            MediaUtil.getMimeType(
                this,
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
                this,
                listOf(media),
                recipient,
                getMessageBody()
            ),
            PICK_FROM_LIBRARY
        )
    }

    override fun setConversationRecyclerViewLayout(status : Boolean) {
        val layoutParams : RelativeLayout.LayoutParams=
            binding.conversationRecyclerView.layoutParams as RelativeLayout.LayoutParams
        if (status) {
            layoutParams.addRule(RelativeLayout.ABOVE, R.id.inputBar)
        } else {
            layoutParams.addRule(RelativeLayout.ABOVE, R.id.typingIndicatorViewContainer)
        }
    }

    override fun handleVoiceMessageUIHidden() {
        val inputBar = binding.inputBar

        inputBar.visibility = View.VISIBLE
        inputBar.alpha = 0f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250L
            addUpdateListener { animator ->
                inputBar.alpha = animator.animatedValue as Float
            }
            start()
        }
    }

    override fun sendVoiceMessage() {
        hideVoiceMessageUI()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val future = audioRecorder.stopRecording()

        stopAudioHandler.removeCallbacks(stopVoiceMessageRecordingTask)

        future.addListener(object : ListenableFuture.Listener<android.util.Pair<Uri, Long>> {
            override fun onSuccess(result : android.util.Pair<Uri, Long>?) {
                if (!isFinishing && !isDestroyed) {

                    val audioSlide =result?.let {
                        AudioSlide(
                            this@ConversationActivityV2,
                            result.first,
                            it.second,
                            MediaTypes.AUDIO_AAC,
                            true
                        )
                    }

                    val slideDeck = SlideDeck()
                    slideDeck.addSlide(audioSlide)

                    sendAttachments(slideDeck.asAttachments(), null)
                }
            }

            override fun onFailure(e: ExecutionException) {
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this@ConversationActivityV2,
                        R.string.ConversationActivity_unable_to_record_audio,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }


    override fun cancelVoiceMessage() {
        hideVoiceMessageUI()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (::audioRecorder.isInitialized) {
            try {
                audioRecorder.stopRecording()
            } catch (e: Exception) {
                Log.d("AudioRecorder", e.message.orEmpty())
            }
        }
        stopAudioHandler.removeCallbacks(stopVoiceMessageRecordingTask)

    }

    override fun onAttachmentChanged() {
    }

    override fun selectMessages(messages : Set<MessageRecord>, position : Int) {
        selectMessage(messages.first(), position)
    }

    override fun deleteMessages(messages: Set<MessageRecord>) {

        val recipient = viewModel.recipient.value ?: return

        val allSentByCurrentUser = messages.all { it.isOutgoing }
        val allHaveServerHash =
            messages.all {
                viewModel.getMessageServerHash(it.id, it.isMms) != null
            }

        when {

            recipient.isOpenGroupRecipient -> {
                val messageCount = messages.size

                AlertDialog.Builder(this, R.style.BChatAlertDialog)
                    .setTitle(
                        resources.getQuantityString(
                            R.plurals.ConversationFragment_delete_selected_messages,
                            messageCount,
                            messageCount
                        )
                    )
                    .setMessage(
                        resources.getQuantityString(
                            R.plurals.ConversationFragment_this_will_permanently_delete_all_n_selected_messages,
                            messageCount,
                            messageCount
                        )
                    )
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        messages.forEach {
                            viewModel.deleteForEveryone(it)
                        }
                        endActionMode()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                        endActionMode()
                    }
                    .show()
            }

            allSentByCurrentUser && allHaveServerHash -> {
                val bottomSheet = DeleteOptionsBottomSheet().apply {
                    this.recipient = recipient

                    onDeleteForMeTapped = {
                        messages.forEach {
                            viewModel.deleteLocally(it)
                        }
                        dismiss()
                        endActionMode()
                    }

                    onDeleteForEveryoneTapped = {
                        messages.forEach {
                            viewModel.deleteForEveryone(it)
                        }
                        dismiss()
                        endActionMode()
                    }

                    onCancelTapped = {
                        dismiss()
                        endActionMode()
                    }
                }

                bottomSheet.show(
                    supportFragmentManager,
                    bottomSheet.tag
                )
            }

            else -> {
                viewModel.setMessagesToDelete(messages)

                val dialog = ConversationActionDialog().apply {
                    arguments = Bundle().apply {
                        putInt(
                            ConversationActionDialog.EXTRA_ARGUMENT_3,
                            messages.size
                        )
                        putSerializable(
                            ConversationActionDialog.EXTRA_DIALOG_TYPE,
                            HomeDialogType.SelectedMessageDelete
                        )
                    }
                    setListener(this@ConversationActivityV2)
                }

                dialog.show(
                    supportFragmentManager,
                    ConversationActionDialog.TAG
                )
            }
        }
    }

    override fun banUser(messages: Set<MessageRecord>) {

        AlertDialog.Builder(this, R.style.BChatAlertDialog_ForBan)
            .setTitle(R.string.ConversationFragment_ban_selected_user)
            .setMessage(
                "This will ban the selected user from this room. It won't ban them from other rooms."
            )
            .setCancelable(true)
            .setPositiveButton(R.string.ban) { _, _ ->
                messages.firstOrNull()?.let {
                    viewModel.banUser(it.individualRecipient)
                }
                endActionMode()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                endActionMode()
            }
            .show()
    }

    override fun banAndDeleteAll(messages : Set<MessageRecord>) {
        val builder=AlertDialog.Builder(this, R.style.BChatAlertDialog_ForBan)
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
        val messageCount = sortedMessages.size
        val builder = StringBuilder()

        for ((index, message) in sortedMessages.withIndex()) {

            var body = MentionUtilities.highlightMentions(
                message.body,
                viewModel.threadId,
                this
            )

            when {
                message.isPayment -> {
                    var amount = ""
                    val direction = if (message.isOutgoing) {
                        getString(R.string.payment_sent)
                    } else {
                        getString(R.string.payment_received)
                    }

                    try {
                        val json = JSONObject(message.body)
                        val kind = json.getJSONObject("kind")
                        amount = kind.getString("amount")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    body = getString(
                        R.string.reply_payment_card_message,
                        direction,
                        amount
                    )
                }

                message.isOpenGroupInvitation -> {
                    try {
                        val json = JSONObject(message.body)
                        val kind = json.getJSONObject("kind")
                        body = kind.getString("groupUrl")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            if (body.isNullOrBlank()) continue

            if (messageCount > 1) {
                val timestamp = DateUtils.getDisplayFormattedTimeSpanString(
                    this,
                    Locale.getDefault(),
                    message.timestamp
                )
                builder.append("$timestamp: ")
            }

            builder.append(body)

            if (index < sortedMessages.lastIndex) {
                builder.append('\n')
            }
        }

        if (builder.isBlank()) return

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Message Content", builder.toString())
        )

        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        endActionMode()
    }

    override fun copyBchatID(messages: Set<MessageRecord>) {

        if (messages.isEmpty()) return

        val message = messages.first()

        val bchatID = if (message.isOutgoing) {
            hexEncodedPublicKey
        } else {
            message.individualRecipient.address.toString()
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BChat ID", bchatID)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()

        endActionMode()
    }

    override fun resendMessage(messages : Set<MessageRecord>) {
        messages.iterator().forEach { messageRecord ->
            ResendMessageUtilities.resend(messageRecord)
        }
        endActionMode()
    }

    override fun showMessageDetail(messages: Set<MessageRecord>) {
        if (messages.isEmpty()) return
        val message = messages.first()
        val intent = Intent(this, MessageDetailActivity::class.java).apply {
            putExtra(
                MessageDetailActivity.MESSAGE_TIMESTAMP,
                message.timestamp
            )
        }
        startActivity(intent)
        endActionMode()
    }

    override fun saveAttachment(messages: Set<MessageRecord>) {

        val message = messages.firstOrNull() as? MmsMessageRecord ?: return

        // Do not allow saving before media finishes downloading
        if (message.isMediaPending) {
            Toast.makeText(
                this,
                getString(R.string.conversation_activity__wait_until_attachment_has_finished_downloading),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        SaveAttachmentTask.showWarningDialog(this) {

            Permissions.with(this@ConversationActivityV2)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .maxSdkVersion(Build.VERSION_CODES.P)
                .withPermanentDenialDialog(
                    getString(
                        R.string.MediaPreviewActivity_signal_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied
                    )
                )
                .onAnyDenied {
                    endActionMode()
                    Toast.makeText(
                        this@ConversationActivityV2,
                        R.string.MediaPreviewActivity_unable_to_write_to_external_storage_without_permission,
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onAllGranted {
                    endActionMode()

                    val attachments : List<SaveAttachmentTask.Attachment?> =
                        com.annimon.stream.Stream.of(message.slideDeck.slides)
                            .filter { s : Slide -> s.uri != null && (s.hasImage() || s.hasVideo() || s.hasAudio() || s.hasDocument()) }
                            .map { s : Slide ->
                                SaveAttachmentTask.Attachment(
                                    s.uri!!,
                                    s.contentType,
                                    message.dateReceived,
                                    s.fileName.orNull()
                                )
                            }
                            .toList()

                    if (attachments.isNotEmpty()) {
                        SaveAttachmentTask(this@ConversationActivityV2)
                            .executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR,
                                *attachments.toTypedArray()
                            )

                        if (!message.isOutgoing) {
                            sendMediaSavedNotification()
                        }
                        return@onAllGranted
                    }

                    Toast.makeText(
                        this@ConversationActivityV2,
                        resources.getQuantityString(
                            R.plurals.ConversationFragment_error_while_saving_attachments_to_sd_card,
                            1
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .execute()
        }
    }

    override fun reply(messages : Set<MessageRecord>) {
        val recipient=viewModel.recipient.value ?: return
        if (messages.isNotEmpty()) {
            binding.slideToPayButton.visibility=View.GONE
            binding.inputBar.draftQuote(recipient, messages.first(), glide)
            setConversationRecyclerViewLayout(true)
        }
        endActionMode()
    }

    override fun destroyActionMode() {
        this.actionMode=null
    }

    override fun block(deleteThread: Boolean) {

        val blockDialog = ConversationActionDialog().apply {
            arguments = Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.BlockUser
                )
                putInt(
                    ConversationActionDialog.EXTRA_ARGUMENT_3,
                    if (deleteThread) 1 else 0
                )
            }

            // Activity implements the dialog listener
            setListener(this@ConversationActivityV2)
        }

        blockDialog.show(
            supportFragmentManager,
            ConversationActionDialog.TAG
        )
    }


    override fun unblock() {
        val blockDialog=ConversationActionDialog()
        blockDialog.apply {
            arguments=Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.UnblockUser
                )
            }
            setListener(this@ConversationActivityV2)
        }
        blockDialog.show(supportFragmentManager, ConversationActionDialog.TAG)
    }

    override fun copyBchatID(bchatId : String) {
        val clip=ClipData.newPlainText("BChat ID", bchatId)
        val manager=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    override fun showExpiringMessagesDialog(thread: Recipient) {

        if (thread.isClosedGroupRecipient) {
            val group = viewModel.getGroup(thread)
            if (group?.isActive == false) {
                return
            }
        }

        val dialog = ConversationActionDialog().apply {
            arguments = Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.DisappearingTimer
                )
                putInt(
                    ConversationActionDialog.EXTRA_ARGUMENT_3,
                    thread.expireMessages
                )
            }

            // Activity implements the dialog listener
            setListener(this@ConversationActivityV2)
        }

        dialog.show(
            supportFragmentManager,
            ConversationActionDialog.TAG
        )
    }

    override fun showMuteOptionDialog(thread: Recipient) {

        val dialog = ConversationActionDialog().apply {
            arguments = Bundle().apply {
                putSerializable(
                    ConversationActionDialog.EXTRA_DIALOG_TYPE,
                    HomeDialogType.MuteChat
                )
                putSerializable(
                    ConversationActionDialog.EXTRA_ARGUMENT_3,
                    thread.mutedUntil
                )
            }

            // Activity implements the dialog listener
            setListener(this@ConversationActivityV2)
        }

        dialog.show(
            supportFragmentManager,
            ConversationActionDialog.TAG
        )
    }

    override fun openSearch() {
        onSearchOpened()
    }

    override fun onModified(recipient: Recipient) {

        runOnUiThread {
            val threadRecipient = viewModel.recipient.value ?: return@runOnUiThread

            if (threadRecipient.isContactRecipient) {
                binding.blockedBanner.isVisible = threadRecipient.isBlocked
                setConversationRecyclerViewLayout(threadRecipient.isBlocked)
                //Need to check
                //callShowPayAsYouChatBDXIcon(threadRecipient)
                showBlockProgressBar(threadRecipient)
            }

            // Message request bar
            setUpMessageRequestsBar()

            // Refresh menu
            invalidateOptionsMenu()

            updateSubtitle()
            showOrHideInputIfNeeded()

            // Update profile picture
            binding.profilePictureView.root.recycle()
            binding.profilePictureView.root.update(threadRecipient)

            // Update conversation title
            binding.conversationTitleView.text = when {
                threadRecipient.isLocalNumber ->
                    getString(R.string.note_to_self).capitalizeFirstLetter()
                else ->
                    threadRecipient.toShortString().capitalizeFirstLetter()
            }
        }
    }


    override fun onSearchMoveUpPressed() {
        this.searchViewModel.onMoveUp()
    }

    override fun onSearchMoveDownPressed() {
        this.searchViewModel.onMoveDown()
    }

    override fun onCreateLoader(id : Int, args : Bundle?) : Loader<Cursor> {
        return ConversationLoader(
            viewModel.threadId,
            !viewModel.isIncomingMessageRequestThread(),
            this
        )
    }

    override fun onLoaderReset(loader : Loader<Cursor>) {
        adapter.changeCursor(null)
    }

    override fun onLoadFinished(loader : Loader<Cursor>, cursor : Cursor?) {
        adapter.changeCursor(cursor)
        if (cursor != null) {
            val messageTimestamp=messageToScrollTimestamp.getAndSet(-1)
            val author=messageToScrollAuthor.getAndSet(null)
            if (author != null && messageTimestamp >= 0) {
                jumpToMessage(author, messageTimestamp, null)
            }
        }
    }

    override fun onConfirm(
        dialogType: HomeDialogType,
        threadRecord: ThreadRecord?
    ) {
        when (dialogType) {

            HomeDialogType.UnblockUser -> {
                viewModel.unblock()
                viewModel.recipient.value?.let { thread ->
                    showBlockProgressBar(thread)
                }
            }

            HomeDialogType.ClearChat -> {
                deleteBlockedConversation()
            }

            HomeDialogType.AcceptRequest -> {
                conversationApprovalJob = acceptMessageRequest()
                viewModel.recipient.value?.let {
                    showBlockProgressBar(it)
                }
            }

            HomeDialogType.DeclineRequest -> {
                viewModel.declineMessageRequest()

                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigurationMessageUtilities
                        .forceSyncConfigurationNowIfNeeded(this@ConversationActivityV2)
                }

                backToHome()
            }

            HomeDialogType.SelectedMessageDelete -> {
                viewModel.deleteMessages?.forEach { message ->
                    viewModel.deleteLocally(message)
                }
                viewModel.setMessagesToDelete(null)
                endActionMode()
            }

            HomeDialogType.GifSetting -> {
                textSecurePreferences.setHasSeenGIFMetaDataWarning()
                AttachmentManager.selectGif(
                    this@ConversationActivityV2,
                    PICK_GIF
                )
            }

            else -> Unit
        }
    }

    override fun onCancel(
        dialogType: HomeDialogType,
        threadRecord: ThreadRecord?
    ) {
        when (dialogType) {
            HomeDialogType.SelectedMessageDelete -> {
                endActionMode()
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
                val muteUntil = when (data as? Int) {
                    1 -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)
                    2 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
                    3 -> System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
                    4 -> Long.MAX_VALUE
                    else -> System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                }

                viewModel.recipient.value?.let { recipient ->
                    DatabaseComponent.get(this@ConversationActivityV2)
                        .recipientDatabase()
                        .setMuted(recipient, muteUntil)
                }
            }

            HomeDialogType.BlockUser -> {
                val deleteThread = (data as? Int) == 1

                viewModel.block()

                viewModel.recipient.value?.let { thread ->
                    showBlockProgressBar(thread)
                }

                if (deleteThread) {
                    viewModel.deleteThread()
                }

                cancelVoiceMessage()
            }

            HomeDialogType.DisappearingTimer -> {
                val expirationTime = data as? Int ?: return

                viewModel.recipient.value?.let { thread ->
                    viewModel.setExpireMessages(thread, expirationTime)

                    val message = ExpirationTimerUpdate(expirationTime).apply {
                        recipient = thread.address.serialize()
                        sentTimestamp = MnodeAPI.nowWithOffset
                    }

                    val expiringMessageManager =
                        ApplicationContext
                            .getInstance(this@ConversationActivityV2)
                            .expiringMessageManager

                    expiringMessageManager.setExpirationTimer(message)
                    MessageSender.send(message, thread.address)

                    invalidateOptionsMenu()
                }
            }

            else -> Unit
        }
    }


    override fun playVoiceMessageAtIndexIfPossible(indexInAdapter : Int) {
        if (indexInAdapter < 0 || indexInAdapter >= adapter.itemCount) {
            return
        }
        val viewHolder =
            binding.conversationRecyclerView.findViewHolderForAdapterPosition(indexInAdapter) as? ConversationAdapter.VisibleMessageViewHolder ?: return
        val visibleMessageView = ViewVisibleMessageBinding.bind(viewHolder.view).visibleMessageView
        visibleMessageView.playVoiceMessage()
    }

    override fun isAudioPlaying(isPlaying : Boolean, audioPlayingIndex : Int) {
        isAudioPlaying=isPlaying
        audioPlayingIndexInAdapter=audioPlayingIndex
    }

    override fun stopVoiceMessages(indexInAdapter : Int) {
        if (indexInAdapter < 0 || indexInAdapter >= adapter.itemCount) {
            return
        }
        val viewHolder=
            binding.conversationRecyclerView.findViewHolderForAdapterPosition(indexInAdapter) as? ConversationAdapter.VisibleMessageViewHolder
                ?: return
        val visibleMessageView=
            ViewVisibleMessageBinding.bind(viewHolder.view).visibleMessageView
        visibleMessageView.stoppedVoiceMessage()
    }

    override fun scrollToMessageIfPossible(timestamp : Long) {
        val lastSeenItemPosition=adapter.getItemPositionForTimestamp(timestamp) ?: return
        binding.conversationRecyclerView.scrollToPosition(lastSeenItemPosition)
    }

    override fun onReactionClicked(emoji : String, messageId : MessageId, userWasSender : Boolean) {
        val message=if (messageId.mms) {
            mmsDb.getMessageRecord(messageId.id)
        } else {
            smsDb.getMessageRecord(messageId.id)
        }
        if (userWasSender) {
            sendEmojiRemoval(emoji, message)
        } else {
            sendEmojiReaction(emoji, message)
        }
    }

    override fun onReactionLongClicked(messageId : MessageId, emoji : String?) {
        if (SystemClock.elapsedRealtime() - emojiLastClickTime >= 1000) {
            emojiLastClickTime=SystemClock.elapsedRealtime()
            val fragment=ReactionsDialogFragment.create(messageId, emoji)
            fragment.show(supportFragmentManager, null)
        }
    }

    override fun onItemLongPress(
        messageRecord: MessageRecord,
        visibleMessageView: VisibleMessageView,
        position: Int
    ) {
        if (!isSecretGroupIsActive()) return

        val recipient = viewModel.recipient.value ?: return
        val isOpenGroup = recipient.isOpenGroupRecipient

        if (
            messageRecord.isSent &&
            !isMessageRequestThread() &&
            !isOpenGroup
        ) {
            if (selectedItem(messageRecord)) {
                actionMode?.let {
                    onDeselect(messageRecord, position, it)
                }
            } else {
                showConversationReaction(
                    messageRecord,
                    visibleMessageView,
                    position
                )
            }
        } else {
            selectMessage(messageRecord, position)
        }
    }


    override suspend fun chatWithContact(
        contact: ContactModel,
        message: MessageRecord
    ) {
        // Disable popup if audio recording is in progress
        if (binding.inputBarRecordingView.isTimerRunning) return

        val addresses = flattenData(contact.address.serialize())

        // -----------------------------
        // CASE 1: Multiple addresses
        // -----------------------------
        if (addresses.size > 1) {
            val intent = Intent(this, ViewAllContactsActivity::class.java).apply {
                putExtra(ViewAllContactsActivity.CONTACTMODEL, contact)
            }
            startActivity(intent)
            return
        }

        // -----------------------------
        // CASE 2: Single address
        // -----------------------------
        val existingDialog =
            supportFragmentManager.findFragmentByTag(
                ComposeDialogContainer.TAG
            )
        if (existingDialog != null) return

        val chatConfirmationDialog = ComposeDialogContainer(
            dialogType = DialogType.ChatWithContactConfirmation,
            onConfirm = {
                val addressForThread =
                    Address.fromSerialized(addresses.first())

                val recipient =
                    Recipient.from(
                        this@ConversationActivityV2,
                        addressForThread,
                        true
                    )

                val threadId =
                    viewModel.getOrCreateThreadIdForContact(recipient)

                val intent = Intent(
                    this@ConversationActivityV2,
                    ConversationActivityV2::class.java
                ).apply {
                    putExtra(THREAD_ID, threadId)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                supportFragmentManager.popBackStack()
                finish()
                startActivity(intent)
            },
            onCancel = {}
        ).apply {

            val names = flattenData(contact.name).ifEmpty { addresses }

            val displayName = when (names.size) {
                0 -> "No Name"
                1 -> names.first().capitalizeFirstLetter()
                2 -> "${shortNameAndAddress(names[0], addresses[0])} and 1 other"
                else ->
                    "${shortNameAndAddress(names.first(), addresses.first())} and ${names.size - 1} others"
            }

            arguments = Bundle().apply {
                putString(
                    ComposeDialogContainer.EXTRA_ARGUMENT_1,
                    displayName
                )
            }
        }

        chatConfirmationDialog.show(
            supportFragmentManager,
            ComposeDialogContainer.TAG
        )

        // Auto-dismiss dialog if message expires
        if (message.expiresIn > 0) {
            delay(
                getRemainingTime(
                    message.expireStarted,
                    message.expiresIn
                )
            )
            chatConfirmationDialog.dismiss()
        }
    }

    override fun onReactionSelected(messageRecord : MessageRecord, emoji : String) {
        reactionDelegate.hide()
        val localUser=textSecurePreferences.getLocalNumber()
        val userReactions=messageRecord.reactions.filter { it.author == localUser }
        val isAlreadyReacted=userReactions.isNotEmpty()

        if (isAlreadyReacted && userReactions.any { it.emoji == emoji }) {
            userReactions.forEach {
                sendEmojiRemoval(it.emoji, messageRecord)
            }
        } else {
            if (isAlreadyReacted) {
                userReactions.forEach {
                    sendEmojiRemoval(it.emoji, messageRecord)
                }
            }
            sendEmojiReaction(emoji, messageRecord)
        }
    }

    override fun onCustomReactionSelected(
        messageRecord : MessageRecord,
        hasAddedCustomEmoji : Boolean
    ) {
        val oldRecord=
            messageRecord.reactions.find { record -> record.author == textSecurePreferences.getLocalNumber() }
        if (oldRecord != null && hasAddedCustomEmoji) {
            reactionDelegate.hide()
            sendEmojiRemoval(oldRecord.emoji, messageRecord)
        } else {
            reactionDelegate.hideForReactWithAny()
            ReactWithAnyEmojiDialogFragment
                .createForMessageRecord(messageRecord, reactWithAnyEmojiStartPage)
                .show(supportFragmentManager, "BOTTOM");
        }
    }

    override fun onReactWithAnyEmojiDialogDismissed() {
        reactionDelegate.hide()
    }

    override fun onReactWithAnyEmojiSelected(emoji : String, messageId : MessageId) {
        reactionDelegate.hide()
        val message=if (messageId.mms) {
            mmsDb.getMessageRecord(messageId.id)
        } else {
            smsDb.getMessageRecord(messageId.id)
        }
        val oldRecord=reactionDb.getReactions(messageId)
            .find { it.author == textSecurePreferences.getLocalNumber() }
        if (oldRecord?.emoji == emoji) {
            sendEmojiRemoval(emoji, message)
        } else {
            sendEmojiReaction(emoji, message)
        }
    }

    override fun onRemoveReaction(emoji : String, messageId : MessageId) {
        val message=if (messageId.mms) {
            mmsDb.getMessageRecords(messageId.id)
        } else {
            smsDb.getMessageRecords(messageId.id)
        }
        if (message != null) {
            sendEmojiRemoval(emoji, message)
        }
    }

    override fun onClearAll(emoji : String, messageId : MessageId) {
        reactionDb.deleteEmojiReactions(emoji, messageId)
        viewModel.openGroup?.let { openGroup ->
            beldexMessageDb.getServerID(messageId.id, !messageId.mms)?.let { serverId ->
                OpenGroupAPIV2.deleteAllReactions(openGroup.room, openGroup.server, serverId, emoji)
            }
        }
        threadDb.notifyThreadUpdated(viewModel.threadId)
    }

    override fun showAllMedia(recipient : Recipient) {
        showAllMediaView(recipient)
    }

    inner class ReactionsToolbarListener(val message : MessageRecord, val position : Int) :
        ConversationReactionOverlay.OnActionSelectedListener {
        override fun onActionSelected(action : ConversationReactionOverlay.Action) {
            val selectedItems=setOf(message)
            when (action) {
                ConversationReactionOverlay.Action.REPLY -> reply(selectedItems)
                ConversationReactionOverlay.Action.RESEND -> resendMessage(selectedItems)
                ConversationReactionOverlay.Action.DOWNLOAD -> saveAttachment(selectedItems)
                ConversationReactionOverlay.Action.COPY_MESSAGE -> copyMessages(selectedItems)
                ConversationReactionOverlay.Action.VIEW_INFO -> showMessageDetail(selectedItems)
                ConversationReactionOverlay.Action.SELECT -> selectMessages(selectedItems, position)
                ConversationReactionOverlay.Action.DELETE -> deleteMessages(selectedItems)
                ConversationReactionOverlay.Action.COPY_BCHAT_ID -> copyBchatID(selectedItems)
                else -> {}
            }
        }
    }

    override fun dispatchIntent(body : (Context) -> Intent?) {
        val intent = body(this) ?: return
        push(intent, false)
    }

    override fun showDialog(baseDialog : BaseDialog, tag : String?) {
        baseDialog.show(supportFragmentManager, tag)
    }

    override fun showBottomSheetDialog(
        bottomSheetDialogFragment : BottomSheetDialogFragment,
        tag : String?
    ) {
        bottomSheetDialogFragment.show(supportFragmentManager,tag)
    }

    override fun showBottomSheetDialogWithBundle(
        bottomSheetDialogFragment : BottomSheetDialogFragment,
        tag : String?,
        bundle : Bundle
    ) {
        bottomSheetDialogFragment.arguments = bundle
        bottomSheetDialogFragment.show(supportFragmentManager,tag)
    }

    override fun dispatchTouchEvent(event : MotionEvent?) : Boolean {
        if (event != null) {
            dispatchTouchEvents(event)
        }
        return super.dispatchTouchEvent(event)
        }

    private fun dispatchTouchEvents(event : MotionEvent) {
        reactionDelegate.applyTouchEvent(event)
    }

    override fun callConversationFragmentV2(address : Address, threadId : Long) {
        val extras = Bundle()
        extras.putParcelable(ADDRESS,address)
        extras.putLong(THREAD_ID,threadId)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtras(extras)
        finish()
        startActivity(intent)
    }

    companion object {

        // Extras
        const val THREAD_ID="thread_id"
        const val ADDRESS="address"
        const val SCROLL_MESSAGE_ID="scroll_message_id"
        const val SCROLL_MESSAGE_AUTHOR="scroll_message_author"
        const val HEX_ENCODED_PUBLIC_KEY="hex_encode_public_key"
        const val BNS_NAME="bns_name"
        const val ACTIVITY_TYPE="activity_type"

        //Shortcut launcher
        const val SHORTCUT_LAUNCHER="shortcut_launcher"

        //SetDataAndType
        const val URI="uri"
        const val TYPE="type"
        const val IN_CHAT_SHARE="share_into_chat"

        // Request codes
        const val PICK_DOCUMENT=2
        const val TAKE_PHOTO=7
        const val PICK_GIF=10
        const val PICK_FROM_LIBRARY=12
        const val INVITE_CONTACTS=124
    }
}


package com.thoughtcrimes.securesms.home

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.AppTextSecurePreferences
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils
import com.beldex.libsignal.utilities.toHexString
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.MuteDialog
import com.thoughtcrimes.securesms.conversation.v2.utilities.NotificationUtils
import com.thoughtcrimes.securesms.database.GroupDatabase
import com.thoughtcrimes.securesms.database.MmsSmsDatabase
import com.thoughtcrimes.securesms.database.RecipientDatabase
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent.Companion.get
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import com.thoughtcrimes.securesms.home.search.GlobalSearchAdapter
import com.thoughtcrimes.securesms.home.search.GlobalSearchInputLayout
import com.thoughtcrimes.securesms.mms.GlideApp
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.util.*
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentHomeBinding
import io.beldex.bchat.databinding.ViewMessageRequestBannerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject

class HomeFragment : Fragment(),ConversationClickListener,LoaderManager.LoaderCallbacks<Cursor>, GlobalSearchInputLayout.GlobalSearchInputLayoutListener,
    NewConversationButtonSetViewDelegate {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var glide: GlideRequests

    @Inject
    lateinit var threadDb: ThreadDatabase

    @Inject
    lateinit var recipientDatabase: RecipientDatabase

    @Inject
    lateinit var groupDatabase: GroupDatabase

    @Inject
    lateinit var mmsSmsDatabase: MmsSmsDatabase

    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences

    private var broadcastReceiver: BroadcastReceiver? = null

    private val publicKey: String
        get() = TextSecurePreferences.getLocalNumber(requireActivity().applicationContext)!!

    /*Hales63*/
    private val homeAdapter: HomeAdapter by lazy {
        HomeAdapter(context = requireActivity().applicationContext, cursor = threadDb.approvedConversationList, listener = this)
    }

    private val globalSearchAdapter = GlobalSearchAdapter { model ->
        when (model) {
            is GlobalSearchAdapter.Model.Message -> {
                val threadId = model.messageResult.threadId
                val timestamp = model.messageResult.receivedTimestampMs
                val author = model.messageResult.messageRecipient.address

                activityCallback?.passGlobalSearchAdapterModelMessageValues(threadId,timestamp,author)//- Important
            }
            is GlobalSearchAdapter.Model.SavedMessages -> {
                activityCallback?.passGlobalSearchAdapterModelSavedMessagesValues(model) //- Important
            }
            is GlobalSearchAdapter.Model.Contact -> {
                val address = model.contact.bchatID

                activityCallback?.passGlobalSearchAdapterModelContactValues(address)//- Important
            }
            is GlobalSearchAdapter.Model.GroupConversation -> {
                val groupAddress = Address.fromSerialized(model.groupRecord.encodedId)
                val threadId =
                    threadDb.getThreadIdIfExistsFor(Recipient.from(requireActivity().applicationContext, groupAddress, false))
                if (threadId >= 0) {
                    activityCallback?.passGlobalSearchAdapterModelGroupConversationValues(threadId)//- Important
                }
            }
            else -> {
                Log.d("Beldex", "callback with model: $model")
            }
        }
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
        this.mContext = null
    }

    interface HomeFragmentListener{
        fun updateProfileButton()
        fun passGlobalSearchAdapterModelMessageValues(
            threadId: Long,
            timestamp: Long,
            author: Address
        )
        fun passGlobalSearchAdapterModelSavedMessagesValues(model: GlobalSearchAdapter.Model.SavedMessages)
        fun passGlobalSearchAdapterModelContactValues(address: String)
        fun passGlobalSearchAdapterModelGroupConversationValues(threadId: Long)
        fun onConversationClick(threadId: Long)
        fun callJoinSocialGroup()
        fun callCreateNewPrivateChat()
        fun callCreateNewSecretGroup()
        fun callLifeCycleScope(
            recyclerView: RecyclerView,
            globalSearchInputLayout: GlobalSearchInputLayout,
            mmsSmsDatabase: MmsSmsDatabase,
            globalSearchAdapter: GlobalSearchAdapter,
            publicKey: String
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater,container,false)
        binding.searchViewContainer.setOnClickListener {
            binding.globalSearchInputLayout.requestFocus()
        }

        glide = GlideApp.with(this)
        val databaseHelper = get(requireActivity().applicationContext).openHelper()

        threadDb = ThreadDatabase(requireActivity().applicationContext,databaseHelper)
        recipientDatabase = RecipientDatabase(requireActivity().applicationContext,databaseHelper)
        groupDatabase = GroupDatabase(requireActivity().applicationContext,databaseHelper)
        textSecurePreferences = AppTextSecurePreferences(requireActivity().applicationContext)
        mmsSmsDatabase = MmsSmsDatabase(requireActivity().applicationContext,databaseHelper)

        setupMessageRequestsBanner()
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
        LocalBroadcastManager.getInstance(requireActivity().applicationContext).registerReceiver(broadcastReceiver, IntentFilter("blockedContactsChanged"))

        activityCallback?.callLifeCycleScope(binding.recyclerView,binding.globalSearchInputLayout,mmsSmsDatabase,globalSearchAdapter,publicKey)


        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {

            }
    }

    /*Hales63*/
    private fun setupMessageRequestsBanner() {
        val messageRequestCount = threadDb.unapprovedConversationCount
        // Set up message requests
        if (messageRequestCount > 0 && !textSecurePreferences.hasHiddenMessageRequests()) {
            with(ViewMessageRequestBannerBinding.inflate(layoutInflater)) {
                unreadCountTextView.text = messageRequestCount.toString()
                timestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(
                    requireActivity().applicationContext,
                    Locale.getDefault(),
                    threadDb.latestUnapprovedConversationTimestamp
                )
                root.setOnClickListener { showMessageRequests() }
                expandMessageRequest.setOnClickListener { showMessageRequests() }
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

    override fun onConversationClick(thread: ThreadRecord) {
         activityCallback?.onConversationClick(thread.threadId)//- Important
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
            NotificationUtils.showNotifyDialog(requireActivity().applicationContext, thread.recipient) { notifyType ->
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

    override fun joinSocialGroup() {
        activityCallback?.callJoinSocialGroup() //- Important
    }

    override fun createNewPrivateChat() {
        activityCallback?.callCreateNewPrivateChat() //- Important
    }

    override fun createNewSecretGroup() {
        activityCallback?.callCreateNewSecretGroup() //- Important
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return HomeLoader(requireActivity().applicationContext)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        homeAdapter.changeCursor(cursor)
        setupMessageRequestsBanner()
        updateEmptyState()
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        homeAdapter.changeCursor(null)
    }

    override fun onInputFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            Log.d("test1",true.toString())
            setSearchShown(true)
        } else {
            Log.d("test1",false.toString())
            setSearchShown(!binding.globalSearchInputLayout.query.value.isNullOrEmpty())
        }
    }

    private fun updateEmptyState() {
        val threadCount = (binding.recyclerView.adapter as HomeAdapter).itemCount
        binding.emptyStateContainer.isVisible = threadCount == 0 && binding.recyclerView.isVisible
        binding.emptyStateContainerText.isVisible =
            threadCount == 0 && binding.recyclerView.isVisible
        val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity().applicationContext)
        (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
            binding.emptyStateImageView.setImageResource(
                it
            )
        }
    }

    private fun setSearchShown(isShown: Boolean) {
        //New Line
        /*binding.searchBarLayout.isVisible = isShown
        binding.searchBarLayout.setOnClickListener {
            onBackPressed()
        }*/ //- Important
        binding.searchToolBar.isVisible = isShown
        binding.searchViewCard.isVisible = !isShown
        //binding.bchatToolbar.isVisible = !isShown //- Important
        binding.recyclerView.isVisible = !isShown
        binding.emptyStateContainer.isVisible =
            (binding.recyclerView.adapter as HomeAdapter).itemCount == 0 && binding.recyclerView.isVisible
        binding.emptyStateContainerText.isVisible =
            (binding.recyclerView.adapter as HomeAdapter).itemCount == 0 && binding.recyclerView.isVisible
        if (activity != null) {
            val isDayUiMode = UiModeUtilities.isDayUiMode(requireActivity())
            (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
                binding.emptyStateImageView.setImageResource(
                    it
                )
            }
        }
        //Comment-->
        /*binding.seedReminderView.isVisible =
            !TextSecurePreferences.getHasViewedSeed(this) && !isShown*/

        binding.gradientView.isVisible = !isShown
        binding.globalSearchRecycler.isVisible = isShown
        binding.newConversationButtonSet.isVisible = !isShown
    }

    private fun hideMessageRequests() {
        val dialog = AlertDialog.Builder(requireActivity().applicationContext, R.style.BChatAlertDialog_New)
            .setTitle("Hide message requests?")
            .setMessage("Once they are hidden, you can access them from Settings > Message Requests")
            .setPositiveButton(R.string.yes) { _, _ ->
                textSecurePreferences.setHasHiddenMessageRequests()
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

    private fun setConversationMuted(thread: ThreadRecord, isMuted: Boolean) {
        if (!isMuted) {
            lifecycleScope.launch(Dispatchers.IO) {
                recipientDatabase.setMuted(thread.recipient, 0)
                withContext(Dispatchers.Main) {
                    binding.recyclerView.adapter!!.notifyDataSetChanged()
                }
            }
        } else {
            MuteDialog.show(requireActivity().applicationContext) { until: Long ->
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
        pinned: Boolean,
        homeFragment: HomeFragment
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            threadDb.setPinned(threadId, pinned)
            withContext(Dispatchers.Main) {
                LoaderManager.getInstance(homeFragment).restartLoader(0, null, homeFragment)
            }
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
            val group = groupDatabase.getGroup(recipient.address.toString()).orNull()
            if (group != null && group.admins.map { it.toString() }
                    .contains(TextSecurePreferences.getLocalNumber(requireActivity().applicationContext))) {
                "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
            } else {
                resources.getString(R.string.activity_home_leave_group_dialog_message)
            }
        } else {
            resources.getString(R.string.activity_home_delete_conversation_dialog_message)
        }
        val dialog = AlertDialog.Builder(requireActivity().applicationContext, R.style.BChatAlertDialog)
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch(Dispatchers.Main) {
                    val context = requireActivity().applicationContext as Context
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
                        DatabaseComponent.get(requireActivity().applicationContext).beldexThreadDatabase()
                            .getOpenGroupChat(threadID)
                    if (v2OpenGroup != null) {
                        OpenGroupManager.delete(
                            v2OpenGroup.server,
                            v2OpenGroup.room,
                            requireActivity().applicationContext
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
        val face: Typeface = Typeface.createFromAsset(requireActivity().assets, "fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    private fun unblockConversation(thread: ThreadRecord) {
        val dialog = AlertDialog.Builder(requireActivity().applicationContext, R.style.BChatAlertDialog)
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

    private fun blockConversation(thread: ThreadRecord) {
        val dialog = AlertDialog.Builder(requireActivity().applicationContext, R.style.BChatAlertDialog)
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

    /*Hales63*/
    private fun showMessageRequests() {
        /*val intent = Intent(this, MessageRequestsActivity::class.java)
        push(intent)*/ //- Important
    }

    override fun onResume() {
        super.onResume()
        /*Hales63*/
        if (TextSecurePreferences.isUnBlocked(requireActivity().applicationContext)) {
            homeAdapter.notifyDataSetChanged()
            TextSecurePreferences.setUnBlockStatus(requireActivity().applicationContext, false)
        }
    }

    fun dispatchTouchEvent(){
        if (binding.newConversationButtonSet.isExpanded) {
            binding.newConversationButtonSet.collapse()
        } else {
            //binding.newConversationButtonSet.collapse()
        }
    }

    fun onBackPressed() {
        if (binding.globalSearchRecycler.isVisible) {
            binding.globalSearchInputLayout.clearSearch(true)
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val broadcastReceiver = this.broadcastReceiver
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(requireActivity().applicationContext).unregisterReceiver(broadcastReceiver)
        }
    }

}
package io.beldex.bchat.messagerequests

import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.beldex.bchat.compose_utils.ui.ComposeMessageDialogFragment
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.model.ThreadRecord
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityMessageRequestsBinding
import javax.inject.Inject

/*Hales63*/
@AndroidEntryPoint
class MessageRequestsActivity : PassphraseRequiredActionBarActivity(), ConversationClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var binding: ActivityMessageRequestsBinding
    private lateinit var glide: RequestManager

    @Inject lateinit var threadDb: ThreadDatabase

    private val viewModel: MessageRequestsViewModel by viewModels()

    private val adapter: MessageRequestsAdapter by lazy {
        MessageRequestsAdapter(context = this, cursor = threadDb.unapprovedConversationList, listener = this)
    }

    override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
        super.onCreate(savedInstanceState, ready)
        binding = ActivityMessageRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        glide = Glide.with(this)

        adapter.setHasStableIds(true)
        adapter.glide = glide
        binding.recyclerView.adapter = adapter

        binding.clearAllMessageRequestsButton.setOnClickListener { deleteAllAndBlock() }
        //binding.acceptAllMessageRequestsButton.setOnClickListener{acceptAllMessageRequest()}
    }


    override fun onResume() {
        super.onResume()
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor> {
        return MessageRequestsLoader(this@MessageRequestsActivity)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        adapter.changeCursor(cursor)
        updateEmptyState()
    }

    override fun onLoaderReset(cursor: Loader<Cursor>) {
        adapter.changeCursor(null)
    }

    override fun onConversationClick(thread: ThreadRecord) {
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationActivityV2.THREAD_ID,thread.threadId)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    override fun onBlockConversationClick(thread: ThreadRecord) {
        ComposeMessageDialogFragment.show(
            supportFragmentManager,
            title = getString(R.string.RecipientPreferenceActivity_block_this_contact_question),
            message = getString(R.string.message_requests_block_message),
            positiveText = getString(R.string.RecipientPreferenceActivity_block),
            negativeText = getString(R.string.no),
            destructive = true,
            onPositive = {
                viewModel.blockMessageRequest(thread)
                LoaderManager.getInstance(this).restartLoader(0, null, this)
            }
        )
    }

    override fun onDeleteConversationClick(thread: ThreadRecord) {
        ComposeMessageDialogFragment.show(
            supportFragmentManager,
            title = getString(R.string.delete),
            message = getString(R.string.message_requests_delete_message),
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            destructive = true,
            onPositive = {
                viewModel.deleteMessageRequest(thread)
                LoaderManager.getInstance(this).restartLoader(0, null, this)
                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@MessageRequestsActivity)
                }
            }
        )
    }

    private fun updateEmptyState() {
        val threadCount = (binding.recyclerView.adapter as MessageRequestsAdapter).itemCount
        binding.emptyStateContainer.isVisible = threadCount == 0
        binding.clearAllMessageRequestsButton.isVisible = threadCount != 0
       /* binding.acceptAllMessageRequestsButton.isVisible = threadCount !=0*/
        binding.messageRequestCardView.isVisible = threadCount !=0
    }

    private fun deleteAllAndBlock() {
        ComposeMessageDialogFragment.show(
            supportFragmentManager,
            title = getString(R.string.clear),
            message = getString(R.string.message_requests_clear_all_message),
            positiveText = getString(R.string.clear),
            negativeText = getString(R.string.cancel),
            destructive = true,
            onPositive = {
                viewModel.clearAllMessageRequests()
                LoaderManager.getInstance(this).restartLoader(0, null, this)
                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@MessageRequestsActivity)
                }
            }
        )
    }

    private fun acceptAllMessageRequest() {
        ComposeMessageDialogFragment.show(
            supportFragmentManager,
            title = getString(R.string.accept),
            message = getString(R.string.message_requests_clear_all_message),
            positiveText = getString(R.string.accept),
            negativeText = getString(R.string.cancel),
            onPositive = {
                viewModel.acceptAllMessageRequests()
                LoaderManager.getInstance(this).restartLoader(0, null, this)
                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@MessageRequestsActivity)
                }
            }
        )
    }
}
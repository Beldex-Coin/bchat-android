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
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
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
        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,thread.threadId)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    override fun onBlockConversationClick(thread: ThreadRecord) {
        val dialog = AlertDialog.Builder(this,R.style.BChatAlertDialog_Clear_All)
        dialog.setTitle(R.string.RecipientPreferenceActivity_block_this_contact_question)
            .setMessage(R.string.message_requests_block_message)
            .setPositiveButton(R.string.recipient_preferences__block) { _, _ ->
                viewModel.blockMessageRequest(thread)
                LoaderManager.getInstance(this).restartLoader(0, null, this)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                // Do nothing
            }
        dialog.create().show()
    }

    override fun onDeleteConversationClick(thread: ThreadRecord) {
        val dialog = AlertDialog.Builder(this,R.style.BChatAlertDialog_Clear_All)
        dialog.setMessage(resources.getString(R.string.message_requests_delete_message))
        dialog.setPositiveButton(R.string.yes) { _, _ ->
            viewModel.deleteMessageRequest(thread)
            LoaderManager.getInstance(this).restartLoader(0, null, this)
            lifecycleScope.launch(Dispatchers.IO) {
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@MessageRequestsActivity)
            }
        }
        dialog.setNegativeButton(R.string.no) { _, _ ->
            // Do nothing
        }
        dialog.create().show()
    }

    private fun updateEmptyState() {
        val threadCount = (binding.recyclerView.adapter as MessageRequestsAdapter).itemCount
        binding.emptyStateContainer.isVisible = threadCount == 0
        binding.clearAllMessageRequestsButton.isVisible = threadCount != 0
       /* binding.acceptAllMessageRequestsButton.isVisible = threadCount !=0*/
        binding.messageRequestCardView.isVisible = threadCount !=0
    }

    private fun deleteAllAndBlock() {
        val dialog = AlertDialog.Builder(this,R.style.BChatAlertDialog_Clear_All)
           dialog.setMessage(resources.getString(R.string.message_requests_clear_all_message))
           dialog.setPositiveButton(R.string.message_requests_clear) { _, _ ->
            viewModel.clearAllMessageRequests()
            LoaderManager.getInstance(this).restartLoader(0, null, this)
            lifecycleScope.launch(Dispatchers.IO) {
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@MessageRequestsActivity)
            }
        }
        dialog.setNegativeButton(R.string.cancel) { _, _ ->
            // Do nothing
        }.create().show()
    }

    private fun acceptAllMessageRequest() {
        val dialog = AlertDialog.Builder(this,R.style.BChatAlertDialog)
           .setMessage(resources.getString(R.string.message_requests_clear_all_message))
           .setPositiveButton(R.string.accept) { _, _ ->
           viewModel.acceptAllMessageRequests()
           LoaderManager.getInstance(this).restartLoader(0, null, this)
           lifecycleScope.launch(Dispatchers.IO) {
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@MessageRequestsActivity)
            }
        }.setNegativeButton(R.string.cancel) { _, _ ->
            // Do nothing
        }.create().show()
    }
}
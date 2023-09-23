package com.thoughtcrimes.securesms.groups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityCreateClosedGroupBinding
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.groupSizeLimit
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.contacts.SelectContactsAdapter
import com.thoughtcrimes.securesms.contacts.SelectContactsLoader
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.util.fadeIn
import com.thoughtcrimes.securesms.util.fadeOut
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import com.thoughtcrimes.securesms.mms.GlideApp

class CreateClosedGroupActivity : PassphraseRequiredActionBarActivity(), LoaderManager.LoaderCallbacks<List<String>> {
    private lateinit var binding: ActivityCreateClosedGroupBinding
    private var isLoading = false
        set(newValue) { field = newValue; invalidateOptionsMenu() }
    private var members = listOf<String>()
        set(value) { field = value; selectContactsAdapter.members = value }
    private val publicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    private val selectContactsAdapter by lazy {
        SelectContactsAdapter(this, GlideApp.with(this))
    }

    companion object {
        const val closedGroupCreatedResultCode = 100
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityCreateClosedGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_create_closed_group_title)

        with(binding){
            recyclerView.adapter = this@CreateClosedGroupActivity.selectContactsAdapter
            recyclerView.layoutManager = LinearLayoutManager(this@CreateClosedGroupActivity)
            createNewPrivateChatButton.setOnClickListener { createNewPrivateChat() }
            createNewPrivateButton.setOnClickListener {
                if (!isLoading) {
                    createClosedGroup()
                }
                LoaderManager.getInstance(this@CreateClosedGroupActivity).initLoader(0, null, this@CreateClosedGroupActivity)
            }
            nameEditText.imeOptions =
                EditorInfo.IME_ACTION_DONE or 16777216 // Always use incognito keyboard
            nameEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
            nameEditText.setOnEditorActionListener { v, actionID, _ ->
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    val imm =
                        v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    createClosedGroup()
                    true
                } else {
                    false
                }
            }
        }
        LoaderManager.getInstance(this).initLoader(0, null, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideSoftKeyboard()
        binding.nameEditText.isFocusable = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_done, menu)
        return members.isNotEmpty() && !isLoading
    }
    // endregion

    // region Updating
    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<List<String>> {
        return SelectContactsLoader(this, setOf())
    }

    override fun onLoadFinished(loader: Loader<List<String>>, members: List<String>) {
        update(members)
    }

    override fun onLoaderReset(loader: Loader<List<String>>) {
        update(listOf())
    }

    private fun update(members: List<String>) {
        //if there is a Note to self conversation, it loads self in the list, so we need to remove it here
        this.members = members.minus(publicKey)
        binding.mainContentContainer.visibility = if (members.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyStateContainer.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
        val isDayUiMode = UiModeUtilities.isDayUiMode(this)
        (if (isDayUiMode) R.drawable.ic_doodle_3_2 else R.drawable.ic_doodle_3_1).also {
            binding.emptyStateImageView.setImageResource(
                it
            )
        }
        //binding.emptyStateImageView.alpha = if (isDayUiMode) 0.08F else 0.05F
        invalidateOptionsMenu()
    }
    // endregion

    // region Interaction
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.doneButton -> if (!isLoading) { createClosedGroup() }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createNewPrivateChat() {
        setResult(closedGroupCreatedResultCode)
        finish()
    }

    private fun createClosedGroup() {
        val name = binding.nameEditText.text.trim()
        if (name.isEmpty()) {
            return Toast.makeText(this, R.string.activity_create_closed_group_group_name_missing_error, Toast.LENGTH_LONG).show()
        }
        if (name.length >= 64) {
            return Toast.makeText(this, R.string.activity_create_closed_group_group_name_too_long_error, Toast.LENGTH_LONG).show()
        }
        val selectedMembers = this.selectContactsAdapter.selectedMembers
        if (selectedMembers.count() < 1) {
            return Toast.makeText(this, R.string.activity_create_closed_group_not_enough_group_members_error, Toast.LENGTH_LONG).show()
        }
        if (selectedMembers.count() >= groupSizeLimit) { // Minus one because we're going to include self later
            return Toast.makeText(this, R.string.activity_create_closed_group_too_many_group_members_error, Toast.LENGTH_LONG).show()
        }
        val userPublicKey = TextSecurePreferences.getLocalNumber(this)!!
        isLoading = true
        binding.loaderContainer.fadeIn()
        binding.nameEditText.text.clear()
        binding.nameEditText.isFocusable = false
        MessageSender.createClosedGroup(name.toString(), selectedMembers + setOf( userPublicKey )).successUi { groupID ->
            binding.nameEditText.isFocusable = true
            binding.loaderContainer.fadeOut()
            isLoading = false
            val threadID = DatabaseComponent.get(this).threadDatabase().getOrCreateThreadIdFor(
                Recipient.from(this, Address.fromSerialized(groupID), false))
             if (!isFinishing) {
                openConversationActivity(threadID, Recipient.from(this, Address.fromSerialized(groupID), false))
                finish()
            }
        }.failUi {
            binding.nameEditText.isFocusable = true
            binding.loaderContainer.fadeOut()
            isLoading = false
            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun openConversationActivity(threadId: Long, recipient: Recipient) {
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,threadId)
        returnIntent.putExtra(ConversationFragmentV2.ADDRESS,recipient.address)
        setResult(RESULT_OK, returnIntent)
    }
    private fun hideSoftKeyboard() {
        if (currentFocus != null) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }
    // endregion
}
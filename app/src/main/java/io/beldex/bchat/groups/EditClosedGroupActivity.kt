package io.beldex.bchat.groups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.groupSizeLimit
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.ThemeUtil
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.toHexString
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.contacts.SelectContactsActivity
import io.beldex.bchat.dependencies.DatabaseComponent
import com.bumptech.glide.Glide
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.fadeIn
import io.beldex.bchat.util.fadeOut
import io.beldex.bchat.wallet.CheckOnline
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityEditClosedGroupBinding
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import java.io.IOException

class EditClosedGroupActivity : PassphraseRequiredActionBarActivity() {
    private val originalMembers = HashSet<String>()
    private val zombies = HashSet<String>()
    private val members = HashSet<String>()
    private val allMembers: Set<String>
        get() {
            return members + zombies
        }
    private var hasNameChanged = false
    private var isSelfAdmin = false
    private var isLoading = false
        set(newValue) { field = newValue; invalidateOptionsMenu() }

    private lateinit var groupID: String
    private lateinit var originalName: String
    private lateinit var name: String
    private lateinit var groupRecipientID: String
    private val glide by lazy { Glide.with(this) }

    private var isEditingName = false
        set(value) {
            if (field == value) return
            field = value
            handleIsEditingNameChanged()
        }

    private val memberListAdapter by lazy {
        if (isSelfAdmin)
            EditClosedGroupMembersAdapter(this, Glide.with(this), isSelfAdmin, this::onMemberClick)
        else
            EditClosedGroupMembersAdapter(this, Glide.with(this), isSelfAdmin)
    }

    private lateinit var binding: ActivityEditClosedGroupBinding

    companion object {
        @JvmStatic val groupIDKey = "groupIDKey"
        @JvmStatic val recipient = "recipient"
        private val loaderID = 0
        val addUsersRequestCode = 124
        val legacyGroupSizeLimit = 10
    }
    var applyChangesButtonLastClickTime: Long = 0

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityEditClosedGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.setHomeAsUpIndicator(
                ThemeUtil.getThemedDrawableResId(this, R.attr.actionModeCloseDrawable))

        groupID = intent.getStringExtra(groupIDKey)!!
        val groupInfo = DatabaseComponent.get(this).groupDatabase().getGroup(groupID).get()
        originalName = groupInfo.title
        isSelfAdmin = groupInfo.admins.any{ it.serialize() == TextSecurePreferences.getLocalNumber(this) }

        name = originalName




        binding.addMembersClosedGroupButton.setOnClickListener {
            onAddMembersClick()
        }
        binding.rvUserList.apply {
            adapter = memberListAdapter
            layoutManager = LinearLayoutManager(this@EditClosedGroupActivity)
        }

       val recipient =  Recipient.from(
                this,
                Address.fromSerialized(groupID), false)
        binding.profilePictureView.root.glide = glide
        binding.profilePictureView.root.update(recipient, fromEditGroup = true)


        binding.lblGroupNameDisplay.text = originalName
        binding.lblGroupNameDisplay.setOnClickListener {
            if (checkIsOnline()) {
                isEditingName = true
            }
        }
        binding.btnCancelGroupNameEdit.setOnClickListener{
            isEditingName = false
        }
        binding.btnSaveGroupNameEdit.setOnClickListener{
            if (checkIsOnline()) {
                saveName()
            }
        }
        binding.applyChangesBtn.setOnClickListener {
            if (checkIsOnline()) {
                if (SystemClock.elapsedRealtime() - applyChangesButtonLastClickTime >= 1000) {
                    applyChangesButtonLastClickTime = SystemClock.elapsedRealtime()
                    commitChanges()
                }
            }
        }

        binding.edtGroupName.setImeActionLabel(getString(R.string.save), EditorInfo.IME_ACTION_DONE)
        binding.edtGroupName.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (checkIsOnline()) {
                        saveName()
                    }
                    return@setOnEditorActionListener true
                }
                else -> return@setOnEditorActionListener false
            }
        }

        LoaderManager.getInstance(this).initLoader(loaderID, null, object : LoaderManager.LoaderCallbacks<GroupMembers> {

            override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<GroupMembers> {
                return EditClosedGroupLoader(this@EditClosedGroupActivity, groupID)
            }

            override fun onLoadFinished(loader: Loader<GroupMembers>, groupMembers: GroupMembers) {
                // We no longer need any subsequent loading events
                // (they will occur on every activity resume).
                LoaderManager.getInstance(this@EditClosedGroupActivity).destroyLoader(loaderID)

                members.clear()
                members.addAll(groupMembers.members.toHashSet())
                zombies.clear()
                zombies.addAll(groupMembers.zombieMembers.toHashSet())
                originalMembers.clear()
                originalMembers.addAll(members + zombies)
                updateMembers()
            }

            override fun onLoaderReset(loader: Loader<GroupMembers>) {
                updateMembers()
            }
        })
    }

    private fun checkIsOnline():Boolean{
        return if(CheckOnline.isOnline(this)){
            true
        }else{
            Toast.makeText(
                this,
                R.string.please_check_your_internet_connection,
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    override fun onPause() {
        super.onPause()
        Helper.hideKeyboard(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.edtGroupName.isFocusable = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_closed_group, menu)
        return allMembers.isNotEmpty() && !isLoading
    }
    // endregion

    // region Updating
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            addUsersRequestCode -> {
                if (resultCode != RESULT_OK) return
                if (data == null || data.extras == null || !data.hasExtra(SelectContactsActivity.selectedContactsKey)) return

                val selectedContacts = data.extras!!.getStringArray(SelectContactsActivity.selectedContactsKey)!!.toSet()
                members.addAll(selectedContacts)
                updateMembers()
            }
        }
    }

    private fun handleIsEditingNameChanged() {
        binding.cntGroupNameEdit.visibility = if (isEditingName) View.VISIBLE else View.INVISIBLE
        binding.cntGroupNameDisplay.visibility = if (isEditingName) View.INVISIBLE else View.VISIBLE
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (isEditingName) {
            binding.edtGroupName.setText(name)
            binding.edtGroupName.selectAll()
            binding.edtGroupName.requestFocus()
            inputMethodManager.showSoftInput(binding.edtGroupName, 0)
        } else {
            inputMethodManager.hideSoftInputFromWindow(binding.edtGroupName.windowToken, 0)
        }
    }

    private fun updateMembers() {
        memberListAdapter.setMembers(allMembers)
        memberListAdapter.setZombieMembers(zombies)

        binding.mainContentContainer.visibility = if (allMembers.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyStateContainer.visibility = if (allMembers.isEmpty()) View.VISIBLE else View.GONE

        invalidateOptionsMenu()
    }
    // endregion

    // region Interaction
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_apply -> if (!isLoading) {
                onAddMembersClick()
                //commitChanges()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onMemberClick(member: String) {
//        val title = R.string.remove_this_contact
//        val message = R.string.remove_message
//        AlertDialog.Builder(context,R.style.BChatAlertDialog)
//            .setTitle(title)
//            .setMessage(message)
//            .setNegativeButton(android.R.string.no, null)
//            .setPositiveButton(R.string.RecipientPreferenceActivity_block) { _, _ ->
//                if (zombies.contains(member)) zombies.remove(member)
//                else members.remove(member)
//                updateMembers()
//            }.show()
        val bottomSheet = ClosedGroupEditingOptionsBottomSheet()
        bottomSheet.onRemoveTapped = {
            if (checkIsOnline()) {
                if (zombies.contains(member)) zombies.remove(member)
                else members.remove(member)
                updateMembers()
                bottomSheet.dismiss()
            }
        }
        bottomSheet.show(supportFragmentManager, "GroupEditingOptionsBottomSheet")
    }

    private fun onAddMembersClick() {
        val intent = Intent(this@EditClosedGroupActivity, SelectContactsActivity::class.java)
        intent.putExtra(SelectContactsActivity.usersToExcludeKey, allMembers.toTypedArray())
        intent.putExtra(SelectContactsActivity.emptyStateTextKey, "No contacts to add")
        startActivityForResult(intent, addUsersRequestCode)
    }

    private fun saveName() {
        val name = binding.edtGroupName.text.toString().trim()
        if (name.isEmpty()) {
            return Toast.makeText(this, R.string.activity_edit_closed_group_group_name_missing_error, Toast.LENGTH_SHORT).show()
        }
        if (name.length >= 26) {
            return Toast.makeText(this, R.string.activity_edit_closed_group_group_name_too_long_error, Toast.LENGTH_SHORT).show()
        }
        if(name == originalName){
            return Toast.makeText(this, R.string.activity_edit_closed_group_group_name_same_name_error,Toast.LENGTH_SHORT).show()
        }
        this.name = name
        binding.lblGroupNameDisplay.text = name
        hasNameChanged = true
        isEditingName = false
    }

    private fun commitChanges() {
        val hasMemberListChanges = (allMembers != originalMembers)

        if (!hasNameChanged && !hasMemberListChanges) {
            return finish()
        }

        val name = if (hasNameChanged) this.name else originalName

        val members = this.allMembers.map {
            Recipient.from(this, Address.fromSerialized(it), false)
        }.toSet()
        val originalMembers = this.originalMembers.map {
            Recipient.from(this, Address.fromSerialized(it), false)
        }.toSet()

        var isClosedGroup: Boolean
        var groupPublicKey: String?
        try {
            groupPublicKey = GroupUtil.doubleDecodeGroupID(groupID).toHexString()
            isClosedGroup = DatabaseComponent.get(this).beldexAPIDatabase().isClosedGroup(groupPublicKey)
        } catch (e: IOException) {
            groupPublicKey = null
            isClosedGroup = false
        }

        if (members.isEmpty()) {
            return Toast.makeText(this, R.string.activity_edit_closed_group_not_enough_group_members_error, Toast.LENGTH_LONG).show()
        }

        val maxGroupMembers = if (isClosedGroup) groupSizeLimit else legacyGroupSizeLimit
        if (members.size >= maxGroupMembers) {
            return Toast.makeText(this, R.string.activity_create_closed_group_too_many_group_members_error, Toast.LENGTH_LONG).show()
        }

        val userPublicKey = TextSecurePreferences.getLocalNumber(this)!!
        val userAsRecipient = Recipient.from(this, Address.fromSerialized(userPublicKey), false)

        if (!members.contains(userAsRecipient) && !members.map { it.address.toString() }.containsAll(originalMembers.minus(userPublicKey))) {
            val message = "Can't leave while adding or removing other members."
            return Toast.makeText(this@EditClosedGroupActivity, message, Toast.LENGTH_LONG).show()
        }

        if (isClosedGroup) {
            isLoading = true
            binding.loaderContainer.fadeIn()
            val promise: Promise<Any, Exception> = if (!members.contains(Recipient.from(this, Address.fromSerialized(userPublicKey), false))) {
                MessageSender.explicitLeave(groupPublicKey!!, true)
            } else {
                task {
                    if (hasNameChanged) {
                        MessageSender.explicitNameChange(groupPublicKey!!, name)
                    }
                    members.filterNot { it in originalMembers }.let { adds ->
                        if (adds.isNotEmpty()) MessageSender.explicitAddMembers(groupPublicKey!!, adds.map { it.address.serialize() })
                    }
                    originalMembers.filterNot { it in members }.let { removes ->
                        if (removes.isNotEmpty()) MessageSender.explicitRemoveMembers(groupPublicKey!!, removes.map { it.address.serialize() })
                    }
                }
            }
            promise.successUi {
                binding.loaderContainer.fadeOut()
                isLoading = false
                finish()
            }.failUi { exception ->
                val message = if (exception is MessageSender.Error) exception.description else "An error occurred"
                Toast.makeText(this@EditClosedGroupActivity, message, Toast.LENGTH_LONG).show()
                binding.loaderContainer.fadeOut()
                isLoading = false
            }
        }
    }

    class GroupMembers(val members: List<String>, val zombieMembers: List<String>) { }
}
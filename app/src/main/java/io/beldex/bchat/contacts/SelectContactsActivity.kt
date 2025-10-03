package io.beldex.bchat.contacts

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.beldex.libbchat.messaging.contacts.Contact
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.dependencies.DatabaseComponent
import com.bumptech.glide.Glide
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySelectContactsBinding


class SelectContactsActivity : PassphraseRequiredActionBarActivity(), LoaderManager.LoaderCallbacks<List<String>> {
    private lateinit var binding: ActivitySelectContactsBinding
    private var members = listOf<String>()
        set(value) { field = value; selectContactsAdapter.members = value }
    private lateinit var usersToExclude: Set<String>

    private val selectContactsAdapter by lazy {
        SelectContactsAdapter(this, Glide.with(this))
    }

    companion object {
        val usersToExcludeKey = "usersToExcludeKey"
        val emptyStateTextKey = "emptyStateTextKey"
        val selectedContactsKey = "selectedContactsKey"
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding=ActivitySelectContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title=resources.getString(R.string.activity_select_contacts_title)

        usersToExclude=intent.getStringArrayExtra(usersToExcludeKey)?.toSet() ?: setOf()
        val emptyStateText=intent.getStringExtra(emptyStateTextKey)
        if (emptyStateText != null) {
            binding.emptyStateMessageTextView.text=emptyStateText
        }

        binding.recyclerView.adapter=selectContactsAdapter
        binding.recyclerView.layoutManager=LinearLayoutManager(this)

        LoaderManager.getInstance(this).initLoader(0, null, this)

        binding.addButton.setOnClickListener {
            closeAndReturnSelected()
        }

        binding.searchContact.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s : Editable) {
                filter(s.toString().lowercase(), members as ArrayList<String>)
            }

            override fun beforeTextChanged(
                    s : CharSequence, start : Int,
                    count : Int, after : Int
            ) {
            }

            override fun onTextChanged(
                    s : CharSequence, start : Int,
                    before : Int, count : Int
            ) {
                if(s.toString().isNotEmpty()){
                    binding.searchAndClearImageview.setImageResource(R.drawable.ic_close)
                }else{
                    binding.searchAndClearImageview.setImageResource(R.drawable.ic_baseline_search_24)
                }
            }
        })

        binding.searchAndClearImageview.setOnClickListener {
            if(binding.searchContact.text.isNotEmpty()){
                binding.searchContact.text.clear()
            }
        }
        selectContactsAdapter.selectionChangedListener=
            object : SelectContactsAdapter.OnSelectionChangedListener {
                override fun onSelectionChanged(selectedCount : Int) {
                    val context=this@SelectContactsActivity
                    val resources=context.resources

                    if (selectedCount > 0) {
                        val enabledColor=
                            ResourcesCompat.getColor(resources, R.color.button_green, context.theme)
                        binding.addButton.apply {
                            isEnabled=true
                            backgroundTintList=ColorStateList.valueOf(enabledColor)
                            setTextColor(ContextCompat.getColor(context, R.color.white))
                        }
                    } else {
                        val disabledColor=ResourcesCompat.getColor(
                            resources, R.color.disable_button_background_color, context.theme
                        )
                        val disabledTextColor=
                            ContextCompat.getColor(context, R.color.disable_button_text_color)

                        binding.addButton.apply {
                            isEnabled=false
                            backgroundTintList=ColorStateList.valueOf(disabledColor)
                            setTextColor(disabledTextColor)
                        }
                    }
                }
            }
    }
    fun filter(text: String?, arrayList: ArrayList<String>) {
        val temp: MutableList<String> = ArrayList()

        for (d in arrayList) {
            if (getUserDisplayName(d).lowercase().contains(text!!)) {
                temp.add(d)
            }

        }
        //update recyclerview
        selectContactsAdapter.updateList(temp)
    }
    private fun getUserDisplayName(publicKey: String): String {
        val contact = DatabaseComponent.get(this).bchatContactDatabase()
                .getContactWithBchatID(publicKey)
        return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
    }


   /* override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_done_open_group, menu)
        return members.isNotEmpty()
    }*/
    // endregion

    // region Updating
    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<List<String>> {
        return SelectContactsLoader(this, usersToExclude)
    }

    override fun onLoadFinished(loader: Loader<List<String>>, members: List<String>) {
        update(members)
    }

    override fun onLoaderReset(loader: Loader<List<String>>) {
        update(listOf())
    }

    private fun update(members: List<String>) {
        println("filter function called 5 $members")
        this.members = members
        binding.recyclerView.visibility = if (members.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyStateContainer.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
        invalidateOptionsMenu()
    }
    // endregion

    // region Interaction
    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.doneButton -> closeAndReturnSelected()
        }
        return super.onOptionsItemSelected(item)
    }
*/
    private fun closeAndReturnSelected() {
        val selectedMembers = selectContactsAdapter.selectedMembers
        val selectedContacts = selectedMembers.toTypedArray()
        val intent = Intent()
        intent.putExtra(selectedContactsKey, selectedContacts)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
    // endregion
}
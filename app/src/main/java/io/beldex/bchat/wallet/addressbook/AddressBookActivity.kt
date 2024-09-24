package io.beldex.bchat.wallet.addressbook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.mms.GlideApp
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityAddressBookBinding




class AddressBookActivity(
) : PassphraseRequiredActionBarActivity(),AddressBookClickListener,
    LoaderManager.LoaderCallbacks<List<String>> {
    private lateinit var binding: ActivityAddressBookBinding
    private var members = listOf<String>()
        set(value) {
            field = value; addressbooktadapter.members = value
        }
    private lateinit var usersToExclude: Set<String>
    private val addressbooktadapter by lazy {
        AddressBookAdapter(this, GlideApp.with(this), this)
    }

    companion object {
        val usersToExcludeKey = "usersToExcludeKey"
        val emptyStateTextKey = "emptyStateTextKey"
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityAddressBookBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_address_book_page_title)

        usersToExclude = intent.getStringArrayExtra(usersToExcludeKey)?.toSet() ?: setOf()
        val emptyStateText = intent.getStringExtra(emptyStateTextKey)
        if (emptyStateText != null) {
            binding.emptyStateMessageTextView.text = emptyStateText
        }

        binding.recyclerView.adapter = addressbooktadapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        LoaderManager.getInstance(this).initLoader(0, null, this)
    }

    // region Updating
    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<List<String>> {
        return AddressBookLoader(this, usersToExclude)
    }

    override fun onLoadFinished(loader: Loader<List<String>>, members: List<String>) {
        update(members)
    }

    override fun onLoaderReset(loader: Loader<List<String>>) {
        update(listOf())
    }

    private fun update(members: List<String>) {
        val temp: MutableList<String> = ArrayList()
        for (d in members) {
            if (getBeldexAddress(d) != "") {
                temp.add(d)
            }
        }
        this.members = temp
        binding.mainContentContainer.visibility = if (members.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyStateContainer.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
        invalidateOptionsMenu()
    }

    private fun getBeldexAddress(d: String): String? {
        val contact = DatabaseComponent.get(this).bchatContactDatabase()
            .getContactWithBchatID(d)
        return contact?.displayBeldexAddress(Contact.ContactContext.REGULAR) ?: ""
    }

    override fun onAddressBookClick(position: Int,address: String) {
        Log.d("Beldex","beldex address value $address")
        val returnIntent = Intent()
        returnIntent.putExtra("address_value", address)
        setResult(RESULT_OK, returnIntent)
        finish()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.search_menu, menu)
        val searchItem: MenuItem = menu.findItem(R.id.actionSearch)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.queryHint=getString(R.string.search_by_name_hint)

        // below line is to call set on query text listener method.
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
              /*  filter(p0!!.lowercase(), members as ArrayList<String>)*/
                return false
            }

            override fun onQueryTextChange(msg: String): Boolean {
                filter(msg.lowercase(), members as ArrayList<String>)
                return false
            }
        })
        return true
    }

    fun filter(text: String?, arrayList: ArrayList<String>) {
        val temp: MutableList<String> = ArrayList()

        for (d in arrayList) {
            if (getUserDisplayName(d).lowercase().contains(text!!)) {
                temp.add(d)
            }
            if (temp.count() == 0) {
                binding.noRecordFoundStateContainer.visibility = View.VISIBLE
            } else {
                binding.noRecordFoundStateContainer.visibility = View.GONE
            }

        }
        //update recyclerview
        addressbooktadapter.updateList(temp)
    }
    private fun getUserDisplayName(publicKey: String): String {
        val contact = DatabaseComponent.get(this).bchatContactDatabase()
            .getContactWithBchatID(publicKey)
        return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
    }
}

// endregion
package com.thoughtcrimes.securesms.wallet.addressbook

import android.os.Bundle
import android.view.View
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.mms.GlideApp
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityAddressBookBinding
import timber.log.Timber

class AddressBookActivity : PassphraseRequiredActionBarActivity(),AddressBookClickListener,
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
        this.members = members
        binding.mainContentContainer.visibility = if (members.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyStateContainer.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
        invalidateOptionsMenu()
    }

    override fun onAddressBookClick(position: Int) {
        Timber.d("Address book position $position")
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

}
// endregion
package io.beldex.bchat.contacts

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityBlockedContactBinding

class BlockedContactActivity : io.beldex.bchat.PassphraseRequiredActionBarActivity(), BlockedContactClickListener,
    LoaderManager.LoaderCallbacks<List<String>> {
    private lateinit var binding: ActivityBlockedContactBinding
    private var members = listOf<String>()
        set(value) {
            field = value; blockedcontactadapter.members = value
        }
    private lateinit var usersToExclude: Set<String>

    private val blockedcontactadapter by lazy {
        BlockedContactAdapter(this, Glide.with(this), this)
    }

    companion object {
        const val usersToExcludeKey = "usersToExcludeKey"
        const val emptyStateTextKey = "emptyStateTextKey"
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityBlockedContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.blocked_contacts)

        usersToExclude = intent.getStringArrayExtra(usersToExcludeKey)?.toSet() ?: setOf()
        val emptyStateText = intent.getStringExtra(emptyStateTextKey)
        if (emptyStateText != null) {
            binding.emptyStateMessageTextView.text = emptyStateText
        }

        binding.recyclerView.adapter = blockedcontactadapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)


        LoaderManager.getInstance(this).initLoader(0, null, this)

    }

    // region Updating
    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<List<String>> {
        return BlockedContactLoader(this, usersToExclude)
    }

    override fun onLoadFinished(loader: Loader<List<String>>, members: List<String>) {
        Log.d("BlockedContactLoader1",members.size.toString())
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

    override fun onBlockedContactClick(position: Int) {
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

}
// endregion
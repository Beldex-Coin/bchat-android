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
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities


class SelectContactsActivity : PassphraseRequiredActionBarActivity(), LoaderManager.LoaderCallbacks<List<String>> {

    private lateinit var binding: ActivitySelectContactsBinding
    private var members = listOf<String>()
        set(value) {
            field = value
            selectContactsAdapter.updateList(value)
        }

    private lateinit var usersToExclude: Set<String>

    private val selectContactsAdapter by lazy {
        SelectContactsAdapter(this, Glide.with(this))
    }

    var isDarkTheme = true

    companion object {
        val usersToExcludeKey = "usersToExcludeKey"
        val selectedContactsKey = "selectedContactsKey"
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivitySelectContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_select_contacts_title)

        isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
        usersToExclude = intent.getStringArrayExtra(usersToExcludeKey)?.toSet() ?: setOf()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = selectContactsAdapter

        binding.recyclerView.visibility = View.GONE
        binding.noContactFoundContainer.visibility = View.VISIBLE

        LoaderManager.getInstance(this).initLoader(0, null, this)

        binding.addButton.setOnClickListener { closeAndReturnSelected() }

        binding.searchContact.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                filter(s.toString(), members)
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.searchAndClearImageview.setImageResource(
                    if (s.isNotEmpty()) R.drawable.ic_close else R.drawable.ic_baseline_search_24
                )
            }
        })

        binding.searchAndClearImageview.setOnClickListener {
            if (binding.searchContact.text.isNotEmpty()) binding.searchContact.text.clear()
        }

        selectContactsAdapter.selectionChangedListener =
            object : SelectContactsAdapter.OnSelectionChangedListener {
                override fun onSelectionChanged(selectedCount: Int) {
                    val enabledColor = ResourcesCompat.getColor(
                        resources,
                        if (selectedCount > 0) R.color.button_green else R.color.cancel_background,
                        theme
                    )
                    binding.addButton.apply {
                        isEnabled = selectedCount > 0
                        backgroundTintList = ColorStateList.valueOf(enabledColor)
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                    }
                }
            }
    }
    fun filter(text: String?, arrayList: List<String>) {
        val query = text?.lowercase()?.trim().orEmpty()

        val filteredList = arrayList.filter { d ->
            getUserDisplayName(d).lowercase().contains(query)
        }

        selectContactsAdapter.updateList(filteredList)

        showNoContactFoundContainer(if (filteredList.isEmpty()) View.VISIBLE else View.GONE)
    }

    private fun getUserDisplayName(publicKey: String): String {
        val contact = DatabaseComponent.get(this).bchatContactDatabase()
            .getContactWithBchatID(publicKey)
        return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
    }

    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<List<String>> {
        return SelectContactsLoader(this, usersToExclude) // AsyncTaskLoader
    }

    override fun onLoadFinished(loader: Loader<List<String>>, data: List<String>) {
        members = data // will auto-update adapter via setter
        binding.recyclerView.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
        showNoContactFoundContainer(if (data.isEmpty()) View.VISIBLE else View.GONE)
    }

    override fun onLoaderReset(loader: Loader<List<String>>) {
        members = listOf()
        binding.recyclerView.visibility = View.GONE
        showNoContactFoundContainer(View.VISIBLE)
    }

    private fun showNoContactFoundContainer(isVisible: Int) {
        binding.noContactFoundContainer.visibility = isVisible
        if (isVisible == View.VISIBLE) {
            binding.icNoContactFound.setImageResource(
                if (isDarkTheme) R.drawable.ic_no_contact_found
                else R.drawable.ic_no_contact_found_white
            )
        }
    }
    private fun closeAndReturnSelected() {
        val selectedContacts = selectContactsAdapter.selectedMembers.toTypedArray()
        val intent = Intent().apply { putExtra(selectedContactsKey, selectedContacts) }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
    // endregion
}

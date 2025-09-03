package io.beldex.bchat.conversation.v2.contact_sharing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.compose_utils.BChatTheme

@AndroidEntryPoint
class ContactSharingActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            BChatTheme {
                val viewModel: ContactsViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                val sendResult: () -> Unit = {
                    val resultIntent = Intent()
                    resultIntent.putExtra(RESULT_CONTACT_TO_SHARE, ArrayList(state.selectedContacts.map { contact ->
                        ContactModel(
                            address = contact.recipient.address,
                            name = contact.recipient.name ?: ""
                        )
                    }))
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }

                ContactsScreen(
                    searchQuery = state.searchQuery,
                    contacts = state.filteredContacts,
                    selectedContacts = state.selectedContacts,
                    onQueryChanged = viewModel::postQuery,
                    onSend = { _ ->
                        sendResult()
                    },
                    onBack = {
                        finish()
                    },
                    contactChanged = viewModel::onContactSelected,
                    modifier=Modifier.padding(WindowInsets.systemBars.asPaddingValues()))
            }
        }
    }

    companion object {
        const val RESULT_CONTACT_TO_SHARE = "io.beldex.bchat.CONTACTS_TO_SHARE"
    }

}
package io.beldex.bchat.conversation.v2.contact_sharing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.compose_utils.BChatTheme

@AndroidEntryPoint
class ContactSharingActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme {
                val viewModel: ContactsViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                val sendResult: () -> Unit = {
                    val resultIntent = Intent()
                    resultIntent.putExtra(RESULT_CONTACT_TO_SHARE, ArrayList(viewModel.selectedContacts.map { contact ->
                        ContactModel(
                            threadId = contact.threadId,
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
                    selectedContacts = state.selectedContactsCount,
                    onQueryChanged = viewModel::postQuery,
                    onSend = { records ->
                        sendResult()
                    },
                    onBack = {
                        finish()
                    },
                    contactChanged = viewModel::onContactSelected
                )
            }
        }
    }

    companion object {
        const val RESULT_CONTACT_TO_SHARE = "io.beldex.bchat.CONTACTS_TO_SHARE"
    }

}
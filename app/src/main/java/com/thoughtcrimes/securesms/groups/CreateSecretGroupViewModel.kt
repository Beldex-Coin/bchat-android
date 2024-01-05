package com.thoughtcrimes.securesms.groups

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.contacts.blocked.BlockedContactsViewModel
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.my_account.domain.PathNodeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreateSecretGroupViewModel@Inject constructor(
    private val threadDb: ThreadDatabase,
    private val textSecurePreferences: TextSecurePreferences
) : ViewModel() {

    private val _recipients = MutableLiveData<List<Recipient>>()
    val recipients: LiveData<List<Recipient>> = _recipients

    val selectedContacts = MutableLiveData(SelectedContact(emptyList()))


    fun subscribe(context: Context): LiveData<SelectedContact> {
        return selectedContacts
    }

    init {
        viewModelScope.launch {
            threadDb.approvedConversationList.use { openCursor ->
                val reader = threadDb.readerFor(openCursor)
                val recipients = mutableListOf<Recipient>()
                while (true) {
                    recipients += reader.next?.recipient ?: break
                }
                withContext(Dispatchers.Main) {
                    _recipients.value = recipients
                        .filter { !it.isGroupRecipient && it.hasApprovedMe() && it.address.serialize() != textSecurePreferences.getLocalNumber() }
                }
            }
        }
    }


    fun filter(query: String): List<Recipient> {
        return _recipients.value?.filter {
            it.address.serialize().contains(query, ignoreCase = true) || it.name?.contains(query, ignoreCase = true) == true
        } ?: emptyList()
    }
}



data class SelectedContact(
    val blockedContacts: List<String>)


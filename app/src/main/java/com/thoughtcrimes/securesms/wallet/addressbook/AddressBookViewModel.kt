package com.thoughtcrimes.securesms.wallet.addressbook

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.database.DatabaseContentProviders
import com.thoughtcrimes.securesms.database.Storage
import com.thoughtcrimes.securesms.my_account.ui.AddressBookEvents
import com.thoughtcrimes.securesms.util.ContactUtilities
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddressBookViewModel @Inject constructor(private val storage: Storage): ViewModel() {

        data class UiState(
                val multiSelectedActivated: Boolean = false,
                val selectedList: List<Recipient> = emptyList()
        )

        private val _uiState = MutableStateFlow(UiState())
        val uiState = _uiState.asStateFlow()

    private val _searchQuery=MutableStateFlow("")
    val searchQuery: StateFlow<String> =_searchQuery.asStateFlow()

    private val executor=viewModelScope + SupervisorJob()

    private val listUpdateChannel=Channel<Unit>(capacity=Channel.CONFLATED)
    private val _contactsList=MutableLiveData(AddressBookContactsValueViewState(emptyList()))

    fun subscribeList(context: Context): LiveData<AddressBookContactsValueViewState> {
        executor.launch(Dispatchers.IO) {
            context.contentResolver
                    .observeQuery(DatabaseContentProviders.Recipient.CONTENT_URI)
                    .onStart {
                        listUpdateChannel.trySend(Unit)
                    }
                    .onEach {
                        listUpdateChannel.trySend(Unit)
                    }
                    .collect()
        }
        executor.launch(Dispatchers.IO) {
            for (update in listUpdateChannel) {
                val addressBookContactsList = AddressBookContactsValueViewState(listOfContacts(context))
                withContext(Dispatchers.Main) {
                    _contactsList.value = addressBookContactsList
                }
            }
        }
        return _contactsList
    }


    fun onEvent(event: AddressBookEvents) {
        when (event) {
            is AddressBookEvents.SearchQueryChanged -> {
                _contactsList.value = AddressBookContactsValueViewState(listOf(_searchQuery.value))
            }
        }
    }


    data class AddressBookContactsValueViewState(
            val addressBookContactsList: List<String>
    )

    private fun listOfContacts(context: Context): List<String> {
        val contacts=ContactUtilities.getAllContacts(context)
        return contacts.filter {
            it.isApproved
        }.map {
            it.address.toString()
        }
    }

}
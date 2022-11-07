package com.thoughtcrimes.securesms.wallet.addressbook

import android.content.Context
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.AsyncLoader

class AddressBookLoader(context: Context, private val usersToExclude: Set<String>) :
    AsyncLoader<List<String>>(context) {

    /*Hales63*/
    override fun loadInBackground(): List<String> {
        val contacts = DatabaseComponent.get(context).bchatContactDatabase().getAllContacts()
        return contacts.filter {
            it.beldexAddress != null && it.beldexAddress!!.isNotEmpty() && it.beldexAddress != ""
        }.map {
            it.bchatID
        }
    }
}
// endregion
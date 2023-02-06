package com.thoughtcrimes.securesms.wallet.addressbook

import android.content.Context
import com.thoughtcrimes.securesms.util.AsyncLoader
import com.thoughtcrimes.securesms.util.ContactUtilities

class AddressBookLoader(context: Context, private val usersToExclude: Set<String>) :
    AsyncLoader<List<String>>(context) {

    /*Hales63*/
    override fun loadInBackground(): List<String> {
        val contacts = ContactUtilities.getAllContacts(context)
        return contacts.filter {
            it.isApproved
        }.map {
            it.address.toString()
        }
    }
}
// endregion
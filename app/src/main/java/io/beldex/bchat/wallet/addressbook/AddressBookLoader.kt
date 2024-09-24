package io.beldex.bchat.wallet.addressbook

import android.content.Context
import io.beldex.bchat.util.AsyncLoader
import io.beldex.bchat.util.ContactUtilities

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
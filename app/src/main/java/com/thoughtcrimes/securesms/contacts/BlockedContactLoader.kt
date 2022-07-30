package com.thoughtcrimes.securesms.contacts

import android.content.Context
import com.thoughtcrimes.securesms.util.AsyncLoader
import com.thoughtcrimes.securesms.util.ContactUtilities

class BlockedContactLoader(context: Context, private val usersToExclude: Set<String>) :
    AsyncLoader<List<String>>(context) {

    /*Hales63*/
    override fun loadInBackground(): List<String> {
        val contacts = ContactUtilities.getAllContacts(context)
        return contacts.filter {
            !it.isGroupRecipient && !usersToExclude.contains(it.address.toString()) && it.isBlocked
        }.map {
            it.address.toString()
        }
    }
}
// endregion
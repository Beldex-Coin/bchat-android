package io.beldex.bchat.contacts

import android.content.Context
import io.beldex.bchat.util.ContactUtilities
import io.beldex.bchat.util.AsyncLoader

class SelectContactsLoader(context: Context, private val usersToExclude: Set<String>) : io.beldex.bchat.util.AsyncLoader<List<String>>(context) {

    /*Hales63*/
    override fun loadInBackground(): List<String> {
        val contacts = ContactUtilities.getAllContacts(context)
        return contacts.filter {
            !it.isGroupRecipient && !usersToExclude.contains(it.address.toString()) && it.hasApprovedMe() && !it.isBlocked && it.isApproved
        }.map {
            it.address.toString()
        }
    }
}
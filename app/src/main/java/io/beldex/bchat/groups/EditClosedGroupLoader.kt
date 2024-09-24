package io.beldex.bchat.groups

import android.content.Context
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.AsyncLoader

class EditClosedGroupLoader(context: Context, val groupID: String) : AsyncLoader<EditClosedGroupActivity.GroupMembers>(context) {

    override fun loadInBackground(): EditClosedGroupActivity.GroupMembers {
        val groupDatabase = DatabaseComponent.get(context).groupDatabase()
        val members = groupDatabase.getGroupMembers(groupID, true)
        val zombieMembers = groupDatabase.getGroupZombieMembers(groupID)
        return EditClosedGroupActivity.GroupMembers(
                members.map {
                    it.address.toString()
                },
                zombieMembers.map {
                    it.address.toString()
                }
        )
    }
}
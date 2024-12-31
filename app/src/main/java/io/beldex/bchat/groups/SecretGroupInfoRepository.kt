package io.beldex.bchat.groups

import io.beldex.bchat.database.GroupDatabase

class SecretGroupInfoRepository (private val groupDatabase: GroupDatabase) {
    fun getGroupMembers(groupID: String): GroupMembers {
        val members = groupDatabase.getGroupMembers(groupID, true)
        val zombieMembers = groupDatabase.getGroupZombieMembers(groupID)
        return GroupMembers(
            members.map { it.address.toString() },
            zombieMembers.map { it.address.toString() }
        )
    }
}
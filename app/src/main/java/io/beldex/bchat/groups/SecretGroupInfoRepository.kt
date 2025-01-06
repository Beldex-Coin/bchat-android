package io.beldex.bchat.groups

import androidx.compose.runtime.mutableStateOf
import io.beldex.bchat.database.GroupDatabase

class SecretGroupInfoRepository (private val groupDatabase: GroupDatabase) {
    fun getGroupMembers(groupID: String): GroupMembers {
        val members = groupDatabase.getGroupMembers(groupID, true)
        val zombieMembers = groupDatabase.getGroupZombieMembers(groupID)
        val groupInfo = mutableStateOf(groupDatabase.getGroup(groupID).get())
        val groupAdmin = groupInfo.value.admins.toList().toString().trim('[',']')
        val allMembers = (members.map { it.address.toString() } + zombieMembers.map { it.address.toString() }).toSet().toList()
        val filteredMembers = allMembers.sortedWith(compareByDescending { it == groupAdmin })
        return GroupMembers(filteredMembers)
    }
}
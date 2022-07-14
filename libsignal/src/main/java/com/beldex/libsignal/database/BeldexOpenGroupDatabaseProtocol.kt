package com.beldex.libsignal.database

interface BeldexOpenGroupDatabaseProtocol {

    fun updateTitle(groupID: String, newValue: String)
    fun updateProfilePicture(groupID: String, newValue: ByteArray)
}

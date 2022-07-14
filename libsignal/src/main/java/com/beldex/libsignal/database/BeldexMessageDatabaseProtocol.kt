package com.beldex.libsignal.database

interface BeldexMessageDatabaseProtocol {

    fun setServerID(messageID: Long, serverID: Long, isSms: Boolean)
}

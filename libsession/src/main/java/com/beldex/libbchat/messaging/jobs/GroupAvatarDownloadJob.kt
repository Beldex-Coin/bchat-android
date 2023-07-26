package com.beldex.libbchat.messaging.jobs

import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.messaging.utilities.Data
import com.beldex.libbchat.mnode.MnodeAPI

class GroupAvatarDownloadJob(val room: String, val server: String) : Job {

    override var delegate: JobDelegate? = null
    override var id: String? = null
    override var failureCount: Int = 0
    override val maxFailureCount: Int = 10

    override fun execute() {
        val storage = MessagingModuleConfiguration.shared.storage
        try {
            val info = OpenGroupAPIV2.getInfo(room, server).get()
            val bytes = OpenGroupAPIV2.downloadOpenGroupProfilePicture(info.id, server).get()
            val groupId = GroupUtil.getEncodedOpenGroupID("$server.$room".toByteArray())
            storage.updateProfilePicture(groupId, bytes)
            storage.updateTimestampUpdated(groupId, MnodeAPI.nowWithOffset)
            delegate?.handleJobSucceeded(this)
        } catch (e: Exception) {
            delegate?.handleJobFailed(this, e)
        }
    }

    override fun serialize(): Data {
        return Data.Builder()
            .putString(ROOM, room)
            .putString(SERVER, server)
            .build()
    }

    override fun getFactoryKey(): String = KEY

    companion object {
        const val KEY = "GroupAvatarDownloadJob"

        private const val ROOM = "room"
        private const val SERVER = "server"
    }

    class Factory : Job.Factory<GroupAvatarDownloadJob> {

        override fun create(data: Data): GroupAvatarDownloadJob {
            return GroupAvatarDownloadJob(
                data.getString(ROOM),
                data.getString(SERVER)
            )
        }
    }
}
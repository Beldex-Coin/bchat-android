package com.beldex.libbchat.messaging.jobs

import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.utilities.Data
import com.beldex.libbchat.utilities.TextSecurePreferences

class TrimThreadJob(val threadId: Long, val openGroupId: String?) : Job {
    override var delegate: JobDelegate? = null
    override var id: String? = null
    override var failureCount: Int = 0

    override val maxFailureCount: Int = 1

    companion object {
        const val KEY: String = "TrimThreadJob"
        const val THREAD_ID = "thread_id"
        const val OPEN_GROUP_ID = "open_group"
    }

    override fun execute() {
        val context = MessagingModuleConfiguration.shared.context
        val trimmingEnabled = TextSecurePreferences.isThreadLengthTrimmingEnabled(context)
        val threadLengthLimit = TextSecurePreferences.getThreadTrimLength(context)
        if (trimmingEnabled) {
            MessagingModuleConfiguration.shared.storage.trimThread(threadId, threadLengthLimit)
        }
        delegate?.handleJobSucceeded(this)
    }

    override fun serialize(): Data {
        val builder = Data.Builder()
            .putLong(THREAD_ID, threadId)
        if (!openGroupId.isNullOrEmpty()) {
            builder.putString(OPEN_GROUP_ID, openGroupId)
        }
        return builder.build()
    }

    override fun getFactoryKey(): String = "TrimThreadJob"

    class Factory : Job.Factory<TrimThreadJob> {

        override fun create(data: Data): TrimThreadJob {
            return TrimThreadJob(data.getLong(THREAD_ID), data.getStringOrDefault(OPEN_GROUP_ID, null))
        }
    }

}
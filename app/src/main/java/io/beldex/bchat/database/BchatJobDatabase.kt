package io.beldex.bchat.database

import android.content.ContentValues
import android.content.Context
import com.beldex.libbchat.messaging.jobs.*
import com.beldex.libbchat.messaging.utilities.Data
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper
import io.beldex.bchat.jobmanager.impl.JsonDataSerializer
import android.database.Cursor

class BchatJobDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper) {

    companion object {
        const val bchatJobTable = "bchat_job_database"
        const val jobID = "job_id"
        const val jobType = "job_type"
        const val failureCount = "failure_count"
        const val serializedData = "serialized_data"
        @JvmStatic val createBchatJobTableCommand
            = "CREATE TABLE $bchatJobTable ($jobID INTEGER PRIMARY KEY, $jobType STRING, $failureCount INTEGER DEFAULT 0, $serializedData TEXT);"
    }

    fun persistJob(job: Job) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(4)
        contentValues.put(jobID, job.id!!)
        contentValues.put(jobType, job.getFactoryKey())
        contentValues.put(failureCount, job.failureCount)
        contentValues.put(serializedData, BchatJobHelper.dataSerializer.serialize(job.serialize()))
        database.insertOrUpdate(bchatJobTable, contentValues, "$jobID = ?", arrayOf( job.id!! ))
    }

    fun markJobAsSucceeded(jobID: String) {
        databaseHelper.writableDatabase.delete(bchatJobTable, "${Companion.jobID} = ?", arrayOf( jobID ))
    }

    fun markJobAsFailedPermanently(jobID: String) {
        databaseHelper.writableDatabase.delete(bchatJobTable, "${Companion.jobID} = ?", arrayOf( jobID ))
    }

    fun getAllPendingJobs(type: String): Map<String, Job?> {
        val database = databaseHelper.readableDatabase
        return database.getAll(bchatJobTable, "$jobType = ?", arrayOf( type )) { cursor ->
            val jobID = cursor.getString(jobID)
            try {
                jobID to jobFromCursor(cursor)
            } catch (e: Exception) {
                Log.e("Beldex", "Error deserializing job of type: $type.", e)
                jobID to null
            }
        }.toMap()
    }

    fun getAttachmentUploadJob(attachmentID: Long): AttachmentUploadJob? {
        val database = databaseHelper.readableDatabase
        val result = mutableListOf<AttachmentUploadJob>()
        database.getAll(bchatJobTable, "$jobType = ?", arrayOf( AttachmentUploadJob.KEY )) { cursor ->
            val job = jobFromCursor(cursor) as AttachmentUploadJob?
            if (job != null) { result.add(job) }
        }
        return result.firstOrNull { job -> job.attachmentID == attachmentID }
    }

    fun getMessageSendJob(messageSendJobID: String): MessageSendJob? {
        val database = databaseHelper.readableDatabase
        return database.get(bchatJobTable, "$jobID = ? AND $jobType = ?", arrayOf( messageSendJobID, MessageSendJob.KEY )) { cursor ->
            jobFromCursor(cursor) as MessageSendJob?
        }
    }

    fun getMessageReceiveJob(messageReceiveJobID: String): MessageReceiveJob? {
        val database = databaseHelper.readableDatabase
        return database.get(bchatJobTable, "$jobID = ? AND $jobType = ?", arrayOf( messageReceiveJobID, MessageReceiveJob.KEY )) { cursor ->
            jobFromCursor(cursor) as MessageReceiveJob?
        }
    }

    fun getGroupAvatarDownloadJob(server: String, room: String): GroupAvatarDownloadJob? {
        val database = databaseHelper.readableDatabase
        return database.getAll(bchatJobTable, "$jobType = ?", arrayOf(GroupAvatarDownloadJob.KEY)) {
            jobFromCursor(it) as GroupAvatarDownloadJob?
        }.filterNotNull().find { it.server == server && it.room == room }
    }

    fun cancelPendingMessageSendJobs(threadID: Long) {
        val database = databaseHelper.writableDatabase
        val attachmentUploadJobKeys = mutableListOf<String>()
        database.getAll(bchatJobTable, "$jobType = ?", arrayOf( AttachmentUploadJob.KEY )) { cursor ->
            val job = jobFromCursor(cursor) as AttachmentUploadJob?
            if (job != null && job.threadID == threadID.toString()) { attachmentUploadJobKeys.add(job.id!!) }
        }
        val messageSendJobKeys = mutableListOf<String>()
        database.getAll(bchatJobTable, "$jobType = ?", arrayOf( MessageSendJob.KEY )) { cursor ->
            val job = jobFromCursor(cursor) as MessageSendJob?
            if (job != null && job.message.threadID == threadID) { messageSendJobKeys.add(job.id!!) }
        }
        if (attachmentUploadJobKeys.isNotEmpty()) {
            val attachmentUploadJobKeysAsString = attachmentUploadJobKeys.joinToString(", ")
            database.delete(bchatJobTable, "$jobType = ? AND $jobID IN (?)",
                arrayOf( AttachmentUploadJob.KEY, attachmentUploadJobKeysAsString ))
        }
        if (messageSendJobKeys.isNotEmpty()) {
            val messageSendJobKeysAsString = messageSendJobKeys.joinToString(", ")
            database.delete(bchatJobTable, "$jobType = ? AND $jobID IN (?)",
                arrayOf( MessageSendJob.KEY, messageSendJobKeysAsString ))
        }
    }

    fun isJobCanceled(job: Job): Boolean {
        val database = databaseHelper.readableDatabase
        var cursor: android.database.Cursor? = null
        try {
            cursor = database.rawQuery("SELECT * FROM $bchatJobTable WHERE $jobID = ?", arrayOf( job.id!! ))
            return cursor == null || !cursor.moveToFirst()
        } catch (e: Exception) {
            // Do nothing
        }  finally {
            cursor?.close()
        }
        return false
    }

    private fun jobFromCursor(cursor: Cursor): Job? {
        val type = cursor.getString(jobType)
        val data = BchatJobHelper.dataSerializer.deserialize(cursor.getString(serializedData))
        val job = BchatJobHelper.bchatJobInstantiator.instantiate(type, data) ?: return null
        job.id = cursor.getString(jobID)
        job.failureCount = cursor.getInt(failureCount)
        return job
    }

    fun hasBackgroundGroupAddJob(groupJoinUrl: String): Boolean {
        val database = databaseHelper.readableDatabase
        return database.getAll(bchatJobTable, "$jobType = ?", arrayOf(BackgroundGroupAddJob.KEY)) { cursor ->
            jobFromCursor(cursor) as? BackgroundGroupAddJob
        }.filterNotNull().any { it.joinUrl == groupJoinUrl }
    }
}

object BchatJobHelper {
    val dataSerializer: Data.Serializer = JsonDataSerializer()
    val bchatJobInstantiator: BchatJobInstantiator = BchatJobInstantiator(BchatJobManagerFactories.getBchatJobFactories())
}
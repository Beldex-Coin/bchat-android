package io.beldex.bchat.database

import android.net.Uri
import java.util.*

/**
 * Represents a record for a backup file in the [io.beldex.bchat.database.BeldexBackupFilesDatabase].
 */
data class BackupFileRecord(val id: Long, val uri: Uri, val fileSize: Long, val timestamp: Date) {

    constructor(uri: Uri, fileSize: Long, timestamp: Date) : this(-1, uri, fileSize, timestamp)
}
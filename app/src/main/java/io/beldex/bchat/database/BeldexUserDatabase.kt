package io.beldex.bchat.database

import android.content.Context
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper

class BeldexUserDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper) {

    companion object {
        // Shared
        private const val displayName = "display_name"
        // Display name cache
        private const val displayNameTable = "beldex_user_display_name_database"
        private const val publicKey = "hex_encoded_public_key"
        @JvmStatic val createDisplayNameTableCommand = "CREATE TABLE $displayNameTable ($publicKey TEXT PRIMARY KEY, $displayName TEXT);"
        // Server display name cache
        private const val serverDisplayNameTable = "beldex_user_server_display_name_database"
        private const val serverID = "server_id"
        @JvmStatic val createServerDisplayNameTableCommand = "CREATE TABLE $serverDisplayNameTable ($publicKey TEXT, $serverID TEXT, $displayName TEXT, PRIMARY KEY ($publicKey, $serverID));"
    }

    fun getDisplayName(publicKey: String): String? {
        if (publicKey == TextSecurePreferences.getLocalNumber(context)) {
            return TextSecurePreferences.getProfileName(context)
        } else {
            val database = databaseHelper.readableDatabase
            val result = database.get(displayNameTable, "${Companion.publicKey} = ?", arrayOf( publicKey )) { cursor ->
                cursor.getString(cursor.getColumnIndexOrThrow(displayName))
            } ?: return null
            val suffix = " (...${publicKey.substring(publicKey.count() - 8)})"
            return if (result.endsWith(suffix)) {
                result.substring(0..(result.count() - suffix.count()))
            } else {
                result
            }
        }
    }
}
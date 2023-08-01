package com.thoughtcrimes.securesms.dependencies

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.beldex.libbchat.database.MessageDataProvider
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.database.*
import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper


@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseComponent {

    companion object {
        @JvmStatic
        fun get(context: Context) = ApplicationContext.getInstance(context).databaseComponent
    }

    fun openHelper(): SQLCipherOpenHelper

    fun smsDatabase(): SmsDatabase
    fun mmsDatabase(): MmsDatabase
    fun attachmentDatabase(): AttachmentDatabase
    fun mediaDatabase(): MediaDatabase
    fun threadDatabase(): ThreadDatabase
    fun mmsSmsDatabase(): MmsSmsDatabase
    fun draftDatabase(): DraftDatabase
    fun pushDatabase(): PushDatabase
    fun groupDatabase(): GroupDatabase
    fun recipientDatabase(): RecipientDatabase
    fun groupReceiptDatabase(): GroupReceiptDatabase
    fun searchDatabase(): SearchDatabase
    fun jobDatabase(): JobDatabase
    fun beldexAPIDatabase(): BeldexAPIDatabase
    fun beldexMessageDatabase(): BeldexMessageDatabase
    fun beldexThreadDatabase(): BeldexThreadDatabase
    fun beldexUserDatabase(): BeldexUserDatabase
    fun beldexBackupFilesDatabase(): BeldexBackupFilesDatabase
    fun bchatJobDatabase(): BchatJobDatabase
    fun bchatContactDatabase(): BchatContactDatabase
    fun storage(): Storage
    fun attachmentProvider(): MessageDataProvider
    fun bchatRecipientAddressDatabase():BchatRecipientAddressDatabase
}
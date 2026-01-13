package io.beldex.bchat.dependencies

import android.content.Context
import android.util.Log
import com.beldex.libbchat.database.MessageDataProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.beldex.bchat.attachments.DatabaseAttachmentProvider
import io.beldex.bchat.crypto.AttachmentSecret
import io.beldex.bchat.crypto.AttachmentSecretProvider
import io.beldex.bchat.crypto.DatabaseSecretProvider
import io.beldex.bchat.database.AttachmentDatabase
import io.beldex.bchat.database.BchatContactDatabase
import io.beldex.bchat.database.BchatJobDatabase
import io.beldex.bchat.database.BchatRecipientAddressDatabase
import io.beldex.bchat.database.BeldexAPIDatabase
import io.beldex.bchat.database.BeldexBackupFilesDatabase
import io.beldex.bchat.database.BeldexMessageDatabase
import io.beldex.bchat.database.BeldexThreadDatabase
import io.beldex.bchat.database.BeldexUserDatabase
import io.beldex.bchat.database.DraftDatabase
import io.beldex.bchat.database.EmojiSearchDatabase
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.database.GroupReceiptDatabase
import io.beldex.bchat.database.JobDatabase
import io.beldex.bchat.database.MediaDatabase
import io.beldex.bchat.database.MmsDatabase
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.database.PushDatabase
import io.beldex.bchat.database.ReactionDatabase
import io.beldex.bchat.database.RecipientDatabase
import io.beldex.bchat.database.SearchDatabase
import io.beldex.bchat.database.SmsDatabase
import io.beldex.bchat.database.Storage
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @JvmStatic
    fun init(context: Context) {
        Log.d("DatabaseModule","${context.filesDir}")
        try {
            System.loadLibrary("sqlcipher")
        }catch (e:UnsatisfiedLinkError) {
           Log.d("DatabaseModule","${e.message.toString()}")
        }
    }

    @Provides
    @Singleton
    fun provideAttachmentSecret(@ApplicationContext context: Context) = AttachmentSecretProvider.getInstance(context).orCreateAttachmentSecret

    @Provides
    @Singleton
    fun provideOpenHelper(@ApplicationContext context: Context): SQLCipherOpenHelper {
        val dbSecret = DatabaseSecretProvider(
            context
        ).orCreateDatabaseSecret
        SQLCipherOpenHelper.migrateSqlCipher3To4IfNeeded(context, dbSecret)
        return SQLCipherOpenHelper(
            context,
            dbSecret
        )
    }

    @Provides
    @Singleton
    fun provideSmsDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        SmsDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideMmsDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        MmsDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideAttachmentDatabase(@ApplicationContext context: Context,
                                  openHelper: SQLCipherOpenHelper,
                                  attachmentSecret: AttachmentSecret
    ) = AttachmentDatabase(
        context,
        openHelper,
        attachmentSecret
    )
    @Provides
    @Singleton
    fun provideMediaDatbase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        MediaDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideThread(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        ThreadDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideMmsSms(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        MmsSmsDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideDraftDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        DraftDatabase(context, openHelper)

    @Provides
    @Singleton
    fun providePushDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        PushDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideGroupDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        GroupDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideRecipientDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        RecipientDatabase(
            context,
            openHelper
        )

    @Provides
    @Singleton
    fun provideGroupReceiptDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        GroupReceiptDatabase(
            context,
            openHelper
        )

    @Provides
    @Singleton
    fun searchDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        SearchDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideJobDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) =
        JobDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideBeldexApiDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BeldexAPIDatabase(context,openHelper)

    @Provides
    @Singleton
    fun provideBeldexMessageDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BeldexMessageDatabase(context,openHelper)

    @Provides
    @Singleton
    fun provideBeldexThreadDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BeldexThreadDatabase(context,openHelper)

    @Provides
    @Singleton
    fun provideBeldexUserDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BeldexUserDatabase(context,openHelper)

    @Provides
    @Singleton
    fun provideBeldexBackupFilesDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BeldexBackupFilesDatabase(context,openHelper)

    @Provides
    @Singleton
    fun provideBchatJobDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BchatJobDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideBchatContactDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BchatContactDatabase(context,openHelper)

    @Provides
    @Singleton
    fun provideReactionDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = ReactionDatabase(context, openHelper)
    @Provides
    @Singleton
    fun provideEmojiSearchDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = EmojiSearchDatabase(context, openHelper)

    @Provides
    @Singleton
    fun provideStorage(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = Storage(context,openHelper)

    @Provides
    @Singleton
    fun provideAttachmentProvider(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper): MessageDataProvider = DatabaseAttachmentProvider(context, openHelper)

    @Provides
    @Singleton
    fun provideBchatRecipientAddressDatabase(@ApplicationContext context: Context, openHelper: SQLCipherOpenHelper) = BchatRecipientAddressDatabase(context,openHelper)

}
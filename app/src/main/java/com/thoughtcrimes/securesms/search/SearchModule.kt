package com.thoughtcrimes.securesms.search

import android.content.Context
import com.beldex.libbchat.utilities.concurrent.SignalExecutors
import com.thoughtcrimes.securesms.contacts.ContactAccessor
import com.thoughtcrimes.securesms.database.BchatContactDatabase
import com.thoughtcrimes.securesms.database.GroupDatabase
import com.thoughtcrimes.securesms.database.SearchDatabase
import com.thoughtcrimes.securesms.database.ThreadDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object SearchModule {

    @Provides
    @ViewModelScoped
    fun provideSearchRepository(@ApplicationContext context: Context,
                                searchDatabase: SearchDatabase,
                                threadDatabase: ThreadDatabase,
                                groupDatabase: GroupDatabase,
                                contactDatabase: BchatContactDatabase
    ) =
        SearchRepository(
            context,
            searchDatabase,
            threadDatabase,
            groupDatabase,
            contactDatabase,
            ContactAccessor.getInstance(),
            SignalExecutors.SERIAL
        )


}
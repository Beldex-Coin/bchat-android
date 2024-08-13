package io.beldex.bchat.search

import android.content.Context
import com.beldex.libbchat.utilities.concurrent.SignalExecutors
import io.beldex.bchat.contacts.ContactAccessor
import io.beldex.bchat.database.BchatContactDatabase
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.database.SearchDatabase
import io.beldex.bchat.database.ThreadDatabase
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
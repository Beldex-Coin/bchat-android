package io.beldex.bchat.dependencies

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.beldex.libbchat.utilities.AppTextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.repository.ConversationRepository
import io.beldex.bchat.repository.DefaultConversationRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindTextSecurePreferences(preferences: AppTextSecurePreferences): TextSecurePreferences

    @Binds
    abstract fun bindConversationRepository(repository: DefaultConversationRepository): ConversationRepository

}
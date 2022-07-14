package com.thoughtcrimes.securesms.dependencies

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.beldex.libbchat.utilities.AppTextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.repository.ConversationRepository
import com.thoughtcrimes.securesms.repository.DefaultConversationRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindTextSecurePreferences(preferences: AppTextSecurePreferences): TextSecurePreferences

    @Binds
    abstract fun bindConversationRepository(repository: DefaultConversationRepository): ConversationRepository

}
package com.thoughtcrimes.securesms.dependencies

import android.content.Context
import com.thoughtcrimes.securesms.database.Storage
import com.thoughtcrimes.securesms.webrtc.CallManager
import com.thoughtcrimes.securesms.webrtc.audio.AudioManagerCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CallModule {

    @Provides
    @Singleton
    fun provideAudioManagerCompat(@ApplicationContext context: Context) = AudioManagerCompat.create(context)

    @Provides
    @Singleton
    fun provideCallManager(@ApplicationContext context: Context, audioManagerCompat: AudioManagerCompat, storage: Storage) =
        CallManager(context, audioManagerCompat, storage)

}
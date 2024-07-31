package io.beldex.bchat.dependencies

import android.content.Context
import io.beldex.bchat.database.Storage
import io.beldex.bchat.webrtc.CallManager
import io.beldex.bchat.webrtc.audio.AudioManagerCompat
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
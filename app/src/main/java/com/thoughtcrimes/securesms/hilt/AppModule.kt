package com.thoughtcrimes.securesms.hilt

import android.app.Application
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Singleton
    @Provides
    fun providesSharedPreference(application: Application): SharedPreferenceUtil {
        return SharedPreferenceUtil(application.applicationContext)
    }

    @Singleton
    @Provides
    fun providesWalletManager(): WalletManager {
        return WalletManager.getInstance()
    }

}
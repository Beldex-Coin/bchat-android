package io.beldex.bchat.hilt

import android.app.Application
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.model.WalletManager
import io.beldex.bchat.util.FirebaseRemoteConfigUtil
import io.beldex.bchat.util.SharedPreferenceUtil
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

    @Singleton
    @Provides
    fun providesFirebaseRemoteConfig(): FirebaseRemoteConfigUtil {
        val remoteConfigUtil = FirebaseRemoteConfigUtil()
        remoteConfigUtil.init()
        return remoteConfigUtil
    }

    @Singleton
    @Provides
    fun providesDatabaseComponent(application: Application): DatabaseComponent {
        return ApplicationContext.getInstance(application.applicationContext).databaseComponent
    }

    @Singleton
    @Provides
    fun providesGlide(application: Application): RequestManager {
        return Glide.with(application.baseContext)
    }

}
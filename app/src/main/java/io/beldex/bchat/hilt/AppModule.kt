package io.beldex.bchat.hilt

import android.app.Application
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
    fun providesFirebaseRemoteConfig(): FirebaseRemoteConfigUtil {
        val remoteConfigUtil = FirebaseRemoteConfigUtil()
        remoteConfigUtil.init()
        return remoteConfigUtil
    }

}
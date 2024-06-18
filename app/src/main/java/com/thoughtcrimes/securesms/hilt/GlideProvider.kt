package com.thoughtcrimes.securesms.hilt

import com.bumptech.glide.RequestManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlideProvider {
    fun provideGlide(): RequestManager
}
package io.beldex.bchat.util

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getString(@StringRes stringResId: Int) : String {
        return context.getString(stringResId)
    }
    fun  <T> getString(@StringRes stringResId: Int, input:T) : String {
        return context.getString(stringResId,input)
    }

    fun getStringArray(@ArrayRes stringResId: Int) : Array<String> {
        return context.resources.getStringArray(stringResId)
    }

}
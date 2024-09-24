package io.beldex.bchat.glide

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import com.beldex.libbchat.avatars.PlaceholderAvatarPhoto
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import io.beldex.bchat.util.AvatarPlaceholderGenerator

class PlaceholderAvatarFetcher(private val context: Context, private val photo: PlaceholderAvatarPhoto): DataFetcher<BitmapDrawable> {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in BitmapDrawable>) {
        try {
            val avatar = AvatarPlaceholderGenerator.generate(context, 128, photo.hashString, photo.displayName)
            callback.onDataReady(avatar)
        } catch (e: Exception) {
            Log.e("Beldex", "Error in fetching avatar")
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {}

    override fun cancel() {}

    override fun getDataClass(): Class<BitmapDrawable> {
        return BitmapDrawable::class.java
    }

    override fun getDataSource(): DataSource = DataSource.LOCAL
}
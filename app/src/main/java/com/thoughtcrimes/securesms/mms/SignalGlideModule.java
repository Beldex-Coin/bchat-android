package com.thoughtcrimes.securesms.mms;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.beldex.libbchat.avatars.PlaceholderAvatarPhoto;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.UnitModelLoader;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.StreamGifDecoder;
import com.bumptech.glide.module.AppGlideModule;

import com.beldex.libbchat.avatars.ContactPhoto;
import com.thoughtcrimes.securesms.crypto.AttachmentSecret;
import com.thoughtcrimes.securesms.crypto.AttachmentSecretProvider;
import com.thoughtcrimes.securesms.giph.model.ChunkedImageUrl;
import com.thoughtcrimes.securesms.glide.ChunkedImageUrlLoader;
import com.thoughtcrimes.securesms.glide.ContactPhotoLoader;
import com.thoughtcrimes.securesms.glide.OkHttpUrlLoader;
import com.thoughtcrimes.securesms.glide.PlaceholderAvatarLoader;
import com.thoughtcrimes.securesms.glide.cache.EncryptedBitmapCacheDecoder;
import com.thoughtcrimes.securesms.glide.cache.EncryptedBitmapResourceEncoder;
import com.thoughtcrimes.securesms.glide.cache.EncryptedCacheEncoder;
import com.thoughtcrimes.securesms.glide.cache.EncryptedGifCacheDecoder;
import com.thoughtcrimes.securesms.glide.cache.EncryptedGifDrawableResourceEncoder;
import com.thoughtcrimes.securesms.mms.AttachmentStreamUriLoader.AttachmentModel;

import java.io.File;
import java.io.InputStream;

@GlideModule
public class SignalGlideModule extends AppGlideModule {

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setLogLevel(Log.ERROR);
//    builder.setDiskCache(new NoopDiskCacheFactory());
  }

  @Override
  public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    AttachmentSecret attachmentSecret = AttachmentSecretProvider.getInstance(context).getOrCreateAttachmentSecret();
    byte[]           secret           = attachmentSecret.getModernKey();

    registry.prepend(File.class, File.class, UnitModelLoader.Factory.getInstance());
    registry.prepend(InputStream.class, new EncryptedCacheEncoder(secret, glide.getArrayPool()));
    registry.prepend(File.class, Bitmap.class, new EncryptedBitmapCacheDecoder(secret, new StreamBitmapDecoder(new Downsampler(registry.getImageHeaderParsers(), context.getResources().getDisplayMetrics(), glide.getBitmapPool(), glide.getArrayPool()), glide.getArrayPool())));
    registry.prepend(File.class, GifDrawable.class, new EncryptedGifCacheDecoder(secret, new StreamGifDecoder(registry.getImageHeaderParsers(), new ByteBufferGifDecoder(context, registry.getImageHeaderParsers(), glide.getBitmapPool(), glide.getArrayPool()), glide.getArrayPool())));

    registry.prepend(Bitmap.class, new EncryptedBitmapResourceEncoder(secret));
    registry.prepend(GifDrawable.class, new EncryptedGifDrawableResourceEncoder(secret));

    registry.append(ContactPhoto.class, InputStream.class, new ContactPhotoLoader.Factory(context));
    registry.append(DecryptableStreamUriLoader.DecryptableUri.class, InputStream.class, new DecryptableStreamUriLoader.Factory(context));
    registry.append(AttachmentModel.class, InputStream.class, new AttachmentStreamUriLoader.Factory());
    registry.append(ChunkedImageUrl.class, InputStream.class, new ChunkedImageUrlLoader.Factory());
    registry.append(PlaceholderAvatarPhoto.class, BitmapDrawable.class, new PlaceholderAvatarLoader.Factory(context));
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }

  public static class NoopDiskCacheFactory implements DiskCache.Factory {
    @Override
    public DiskCache build() {
      return new DiskCacheAdapter();
    }
  }
}

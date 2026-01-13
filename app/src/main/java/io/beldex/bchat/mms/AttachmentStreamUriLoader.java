package io.beldex.bchat.mms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import io.beldex.bchat.mms.AttachmentStreamUriLoader.AttachmentModel;
import com.beldex.libsignal.utilities.guava.Optional;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;

public class AttachmentStreamUriLoader implements ModelLoader<AttachmentModel, InputStream> {

  @Override
  public @Nullable LoadData<InputStream> buildLoadData(@NonNull AttachmentModel attachmentModel, int width, int height, @NonNull Options options) {
    return new LoadData<>(attachmentModel, new AttachmentStreamLocalUriFetcher(attachmentModel.attachment, attachmentModel.plaintextLength, attachmentModel.key, attachmentModel.digest));
  }

  @Override
  public boolean handles(@NonNull AttachmentModel attachmentModel) {
    return true;
  }

  static class Factory implements ModelLoaderFactory<AttachmentModel, InputStream> {

    @Override
    public @NonNull ModelLoader<AttachmentModel, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new AttachmentStreamUriLoader();
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  public static class AttachmentModel implements Key {
    public final @NonNull File             attachment;
    public final @NonNull byte[]           key;
    public final @NonNull Optional<byte[]> digest;
    public final long             plaintextLength;

    public AttachmentModel(@NonNull File attachment, @NonNull byte[] key,
                           long plaintextLength, @NonNull Optional<byte[]> digest)
    {
      this.attachment      = attachment;
      this.key             = key;
      this.digest          = digest;
      this.plaintextLength = plaintextLength;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
      messageDigest.update(attachment.toString().getBytes());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AttachmentModel that = (AttachmentModel)o;

      return attachment.equals(that.attachment);

    }

    @Override
    public int hashCode() {
      return attachment.hashCode();
    }
  }
}


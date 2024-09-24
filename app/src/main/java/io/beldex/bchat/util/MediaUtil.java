package io.beldex.bchat.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import io.beldex.bchat.mms.AudioSlide;
import io.beldex.bchat.mms.DecryptableStreamUriLoader;
import io.beldex.bchat.mms.DocumentSlide;
import io.beldex.bchat.mms.GifSlide;
import io.beldex.bchat.mms.ImageSlide;
import io.beldex.bchat.mms.MmsSlide;
import io.beldex.bchat.mms.PartAuthority;
import io.beldex.bchat.mms.Slide;
import io.beldex.bchat.mms.TextSlide;
import io.beldex.bchat.mms.VideoSlide;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.MediaTypes;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.mms.GlideApp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class MediaUtil {

  private static final String TAG = MediaUtil.class.getSimpleName();

  public static Slide getSlideForAttachment(Context context, Attachment attachment) {
    Slide slide = null;
    if (isGif(attachment.getContentType())) {
      slide = new GifSlide(context, attachment);
    } else if (isImageType(attachment.getContentType())) {
      slide = new ImageSlide(context, attachment);
    } else if (isVideoType(attachment.getContentType())) {
      slide = new VideoSlide(context, attachment);
    } else if (isAudioType(attachment.getContentType())) {
      slide = new AudioSlide(context, attachment);
    } else if (isMms(attachment.getContentType())) {
      slide = new MmsSlide(context, attachment);
    } else if (isLongTextType(attachment.getContentType())) {
      slide = new TextSlide(context, attachment);
    } else if (attachment.getContentType() != null) {
      slide = new DocumentSlide(context, attachment);
    }

    return slide;
  }

  public static @Nullable String getMimeType(Context context, Uri uri) {
    if (uri == null) return null;

    if (PartAuthority.isLocalUri(uri)) {
      return PartAuthority.getAttachmentContentType(context, uri);
    }

    String type = context.getContentResolver().getType(uri);
    if (type == null) {
      final String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
      type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }

    return getCorrectedMimeType(type);
  }

  public static @Nullable String getCorrectedMimeType(@Nullable String mimeType) {
    if (mimeType == null) return null;

    switch(mimeType) {
    case "image/jpg":
      return MimeTypeMap.getSingleton().hasMimeType(MediaTypes.IMAGE_JPEG)
             ? MediaTypes.IMAGE_JPEG
             : mimeType;
    default:
      return mimeType;
    }
  }

  public static long getMediaSize(Context context, Uri uri) throws IOException {
    InputStream in = PartAuthority.getAttachmentStream(context, uri);
    if (in == null) throw new IOException("Couldn't obtain input stream.");

    long   size   = 0;
    byte[] buffer = new byte[4096];
    int    read;

    while ((read = in.read(buffer)) != -1) {
      size += read;
    }
    in.close();

    return size;
  }

  @WorkerThread
  public static Pair<Integer, Integer> getDimensions(@NonNull Context context, @Nullable String contentType, @Nullable Uri uri) {
    if (uri == null || !MediaUtil.isImageType(contentType)) {
      return new Pair<>(0, 0);
    }

    Pair<Integer, Integer> dimens = null;

    if (MediaUtil.isGif(contentType)) {
      try {
        GifDrawable drawable = GlideApp.with(context)
                .asGif()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .load(new DecryptableStreamUriLoader.DecryptableUri(uri))
                .submit()
                .get();
        dimens = new Pair<>(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
      } catch (InterruptedException e) {
        Log.w(TAG, "Was unable to complete work for GIF dimensions.", e);
      } catch (ExecutionException e) {
        Log.w(TAG, "Glide experienced an exception while trying to get GIF dimensions.", e);
      }
    } else {
      InputStream attachmentStream = null;
      try {
        if (MediaUtil.isJpegType(contentType)) {
          attachmentStream = PartAuthority.getAttachmentStream(context, uri);
          dimens = BitmapUtil.getExifDimensions(attachmentStream);
          attachmentStream.close();
          attachmentStream = null;
        }
        if (dimens == null) {
          attachmentStream = PartAuthority.getAttachmentStream(context, uri);
          dimens = BitmapUtil.getDimensions(attachmentStream);
        }
      } catch (FileNotFoundException e) {
        Log.w(TAG, "Failed to find file when retrieving media dimensions.", e);
      } catch (IOException e) {
        Log.w(TAG, "Experienced a read error when retrieving media dimensions.", e);
      } catch (BitmapDecodingException e) {
        Log.w(TAG, "Bitmap decoding error when retrieving dimensions.", e);
      } finally {
        if (attachmentStream != null) {
          try {
            attachmentStream.close();
          } catch (IOException e) {
            Log.w(TAG, "Failed to close stream after retrieving dimensions.", e);
          }
        }
      }
    }
    if (dimens == null) {
      dimens = new Pair<>(0, 0);
    }
    Log.d(TAG, "Dimensions for [" + uri + "] are " + dimens.first + " x " + dimens.second);
    return dimens;
  }

  public static boolean isMms(String contentType) {
    return !TextUtils.isEmpty(contentType) && contentType.trim().equals("application/mms");
  }

  public static boolean isGif(Attachment attachment) {
    return isGif(attachment.getContentType());
  }

  public static boolean isJpeg(Attachment attachment) {
    return isJpegType(attachment.getContentType());
  }

  public static boolean isImage(Attachment attachment) {
    return isImageType(attachment.getContentType());
  }

  public static boolean isAudio(Attachment attachment) {
    return isAudioType(attachment.getContentType());
  }

  public static boolean isVideo(Attachment attachment) {
    return isVideoType(attachment.getContentType());
  }

  public static boolean isVcard(String contentType) {
    return !TextUtils.isEmpty(contentType) && contentType.trim().equals(MediaTypes.VCARD);
  }

  public static boolean isGif(String contentType) {
    return !TextUtils.isEmpty(contentType) && contentType.trim().equals("image/gif");
  }

  public static boolean isJpegType(String contentType) {
    return !TextUtils.isEmpty(contentType) && contentType.trim().equals(MediaTypes.IMAGE_JPEG);
  }

  public static boolean isFile(Attachment attachment) {
    return !isGif(attachment) && !isImage(attachment) && !isAudio(attachment) && !isVideo(attachment);
  }

  public static boolean isImageType(String contentType) {
    return (null != contentType)
            && contentType.startsWith("image/")
            && !contentType.contains("svg");  // Do not treat SVGs as regular images.
  }

  public static boolean isAudioType(String contentType) {
    return (null != contentType) && contentType.startsWith("audio/");
  }

  public static boolean isVideoType(String contentType) {
    return (null != contentType) && contentType.startsWith("video/");
  }

  public static boolean isLongTextType(String contentType) {
    return (null != contentType) && contentType.equals(MediaTypes.LONG_TEXT);
  }

  public static boolean hasVideoThumbnail(Uri uri) {
    Log.i(TAG, "Checking: " + uri);

    if (uri == null || !ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
      return false;
    }

    if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
      return uri.getLastPathSegment().contains("video");
    } else if (uri.toString().startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())) {
      return true;
    }

    return false;
  }

  public static @Nullable Bitmap getVideoThumbnail(Context context, Uri uri) {
    if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
      long videoId = Long.parseLong(uri.getLastPathSegment().split(":")[1]);

      return MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                                                      videoId,
                                                      MediaStore.Images.Thumbnails.MINI_KIND,
                                                      null);
    } else if (uri.toString().startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())) {
      long videoId = Long.parseLong(uri.getLastPathSegment());

      return MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                                                      videoId,
                                                      MediaStore.Images.Thumbnails.MINI_KIND,
                                                      null);
    }

    return null;
  }

  public static @Nullable String getDiscreteMimeType(@NonNull String mimeType) {
    final String[] sections = mimeType.split("/", 2);
    return sections.length > 1 ? sections[0] : null;
  }

  public static class ThumbnailData {
    Bitmap bitmap;
    float aspectRatio;

    public ThumbnailData(Bitmap bitmap) {
      this.bitmap      = bitmap;
      this.aspectRatio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
    }

    public Bitmap getBitmap() {
      return bitmap;
    }

    public float getAspectRatio() {
      return aspectRatio;
    }

    public InputStream toDataStream() {
      return BitmapUtil.toCompressedJpeg(bitmap);
    }
  }
}

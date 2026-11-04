package io.beldex.bchat.mms;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.MediaTypes;

public class GifSlide extends ImageSlide {

  public GifSlide(Context context, Attachment attachment) {
    super(context, attachment);
  }


  public GifSlide(Context context, Uri uri, long size, int width, int height) {
    this(context, uri, size, width, height, null);
  }

  public GifSlide(Context context, Uri uri, long size, int width, int height, @Nullable String caption) {
    this(context, uri, size, width, height, caption, null);
  }

  /** @param fileName set to {@link #STICKER_FILE_NAME} for a Giphy sticker so recipients can draw it without a bubble. */
  public GifSlide(Context context, Uri uri, long size, int width, int height, @Nullable String caption, @Nullable String fileName) {
    super(context, constructAttachmentFromUri(context, uri, MediaTypes.IMAGE_GIF, size, width, height, true, fileName, caption, false, false));
  }

  public static final String STICKER_FILE_NAME = "bchat-sticker.gif";

  @Override
  @Nullable
  public Uri getThumbnailUri() {
    return getUri();
  }
}

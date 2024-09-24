package io.beldex.bchat.mms;


import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libsignal.utilities.ExternalStorageUtil;

public class DocumentSlide extends Slide {

  public DocumentSlide(@NonNull Context context, @NonNull Attachment attachment) {
    super(context, attachment);
  }

  public DocumentSlide(@NonNull Context context, @NonNull Uri uri,
                       @NonNull String contentType,  long size,
                       @Nullable String fileName)
  {
    super(context, constructAttachmentFromUri(context, uri, contentType, size, 0, 0, true, ExternalStorageUtil.getCleanFileName(fileName), null, false, false));
  }

  @Override
  public boolean hasDocument() {
    return true;
  }

}

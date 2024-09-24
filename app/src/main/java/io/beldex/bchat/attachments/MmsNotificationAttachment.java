package io.beldex.bchat.attachments;


import android.net.Uri;
import androidx.annotation.Nullable;

import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentTransferProgress;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import io.beldex.bchat.database.MmsDatabase;

public class MmsNotificationAttachment extends Attachment {

  public MmsNotificationAttachment(int status, long size) {
    super("application/mms", getTransferStateFromStatus(status), size, null, null, null, null, null, null, false, 0, 0, false, null, "");
  }

  @Nullable
  @Override
  public Uri getDataUri() {
    return null;
  }

  @Nullable
  @Override
  public Uri getThumbnailUri() {
    return null;
  }

  private static int getTransferStateFromStatus(int status) {
    if (status == MmsDatabase.Status.DOWNLOAD_INITIALIZED ||
        status == MmsDatabase.Status.DOWNLOAD_NO_CONNECTIVITY)
    {
      return AttachmentTransferProgress.TRANSFER_PROGRESS_PENDING;
    } else if (status == MmsDatabase.Status.DOWNLOAD_CONNECTING) {
      return AttachmentTransferProgress.TRANSFER_PROGRESS_STARTED;
    } else {
      return AttachmentTransferProgress.TRANSFER_PROGRESS_FAILED;
    }
  }
}

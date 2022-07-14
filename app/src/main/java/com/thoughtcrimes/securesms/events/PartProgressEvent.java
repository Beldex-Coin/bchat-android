package com.thoughtcrimes.securesms.events;


import androidx.annotation.NonNull;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;

public class PartProgressEvent {

  public final Attachment attachment;
  public final long       total;
  public final long       progress;

  public PartProgressEvent(@NonNull Attachment attachment, long total, long progress) {
    this.attachment = attachment;
    this.total      = total;
    this.progress   = progress;
  }
}

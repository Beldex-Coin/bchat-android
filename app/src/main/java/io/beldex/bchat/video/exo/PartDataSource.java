package io.beldex.bchat.video.exo;


import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import io.beldex.bchat.mms.PartUriParser;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import io.beldex.bchat.database.AttachmentDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PartDataSource implements DataSource {

  private final @NonNull  Context          context;
  private final @Nullable TransferListener listener;

  private Uri         uri;
  private InputStream inputSteam;

  PartDataSource(@NonNull Context context, @Nullable TransferListener listener) {
    this.context  = context.getApplicationContext();
    this.listener = listener;
  }

  @Override
  public void addTransferListener(TransferListener transferListener) {
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    this.uri = dataSpec.uri;

    AttachmentDatabase attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase();
    PartUriParser partUri            = new PartUriParser(uri);
    Attachment         attachment         = attachmentDatabase.getAttachment(partUri.getPartId());

    if (attachment == null) throw new IOException("Attachment not found");

    this.inputSteam = attachmentDatabase.getAttachmentStream(partUri.getPartId(), dataSpec.position);

    if (listener != null) {
      listener.onTransferStart(this, dataSpec, false);
    }

    if (attachment.getSize() - dataSpec.position <= 0) throw new EOFException("No more data");

    return attachment.getSize() - dataSpec.position;
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) throws IOException {
    int read = inputSteam.read(buffer, offset, readLength);

    if (read > 0 && listener != null) {
      listener.onBytesTransferred(this, null, false, read);
    }

    return read;
  }

  @Override
  public Uri getUri() {
    return uri;
  }

  @Override
  public Map<String, List<String>> getResponseHeaders() {
    return Collections.emptyMap();
  }

  @Override
  public void close() throws IOException {
    inputSteam.close();
  }
}

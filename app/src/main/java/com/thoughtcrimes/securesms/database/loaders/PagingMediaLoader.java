package com.thoughtcrimes.securesms.database.loaders;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.thoughtcrimes.securesms.database.AttachmentDatabase;
import com.thoughtcrimes.securesms.mms.PartAuthority;
import com.thoughtcrimes.securesms.util.AsyncLoader;
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentId;
import com.beldex.libbchat.utilities.recipients.Recipient;

import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;

public class PagingMediaLoader extends AsyncLoader<Pair<Cursor, Integer>> {

  @SuppressWarnings("unused")
  private static final String TAG = PagingMediaLoader.class.getSimpleName();

  private final Recipient recipient;
  private final Uri       uri;
  private final boolean   leftIsRecent;

  public PagingMediaLoader(@NonNull Context context, @NonNull Recipient recipient, @NonNull Uri uri, boolean leftIsRecent) {
    super(context);
    this.recipient    = recipient;
    this.uri          = uri;
    this.leftIsRecent = leftIsRecent;
  }

  @Nullable
  @Override
  public Pair<Cursor, Integer> loadInBackground() {
    long   threadId = DatabaseComponent.get(getContext()).threadDatabase().getOrCreateThreadIdFor(recipient);
    Cursor cursor   = DatabaseComponent.get(getContext()).mediaDatabase().getGalleryMediaForThread(threadId);

    while (cursor != null && cursor.moveToNext()) {
      AttachmentId attachmentId  = new AttachmentId(cursor.getLong(cursor.getColumnIndexOrThrow(AttachmentDatabase.ROW_ID)), cursor.getLong(cursor.getColumnIndexOrThrow(AttachmentDatabase.UNIQUE_ID)));
      Uri          attachmentUri = PartAuthority.getAttachmentDataUri(attachmentId);

      if (attachmentUri.equals(uri)) {
        return new Pair<>(cursor, leftIsRecent ? cursor.getPosition() : cursor.getCount() - 1 - cursor.getPosition());
      }
    }
    if (cursor != null) {
      cursor.close();
    }

    return null;
  }
}

package com.thoughtcrimes.securesms.database.loaders;


import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.util.AbstractCursorLoader;

import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;

public class ThreadMediaLoader extends AbstractCursorLoader {

  private final Address address;
  private final boolean gallery;

  public ThreadMediaLoader(@NonNull Context context, @NonNull Address address, boolean gallery) {
    super(context);
    this.address = address;
    this.gallery = gallery;
  }

  @Override
  public Cursor getCursor() {
    long threadId = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(Recipient.from(getContext(), address, true));

    if (gallery) return DatabaseComponent.get(context).mediaDatabase().getGalleryMediaForThread(threadId);
    else         return DatabaseComponent.get(context).mediaDatabase().getDocumentMediaForThread(threadId);
  }

  public Address getAddress() {
    return address;
  }

}

package com.thoughtcrimes.securesms.database.model;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thoughtcrimes.securesms.mms.SlideDeck;

import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel;
import com.beldex.libbchat.utilities.Address;
import com.thoughtcrimes.securesms.mms.SlideDeck;

public class Quote {

  private final long      id;
  private final Address   author;
  private final String    text;
  private final boolean   missing;
  private final SlideDeck attachment;

  public Quote(long id, @NonNull Address author, @Nullable String text, boolean missing, @NonNull SlideDeck attachment) {
    this.id         = id;
    this.author     = author;
    this.text       = text;
    this.missing    = missing;
    this.attachment = attachment;
  }

  public long getId() {
    return id;
  }

  public @NonNull Address getAuthor() {
    return author;
  }

  public @Nullable String getText() {
    return text;
  }

  public boolean isOriginalMissing() {
    return missing;
  }

  public @NonNull SlideDeck getAttachment() {
    return attachment;
  }

  public QuoteModel getQuoteModel() {
    return new QuoteModel(id, author, text, missing, attachment.asAttachments());
  }
}

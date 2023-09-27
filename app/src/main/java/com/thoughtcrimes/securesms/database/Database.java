/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.thoughtcrimes.securesms.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.beldex.libbchat.messaging.utilities.WindowDebouncer;
import com.thoughtcrimes.securesms.ApplicationContext;
import com.beldex.libbchat.utilities.Debouncer;
import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper;

import java.util.Set;

public abstract class Database {

  protected static final String ID_WHERE = "_id = ?";
  protected static final String ID_IN = "_id IN (?)";

  protected       SQLCipherOpenHelper databaseHelper;
  protected final Context             context;
  private   final WindowDebouncer conversationListNotificationDebouncer;
  private   final Runnable            conversationListUpdater;

  @SuppressLint("WrongConstant")
  public Database(Context context, SQLCipherOpenHelper databaseHelper) {
    this.context = context;
    this.conversationListUpdater = () -> {
      context.getContentResolver().notifyChange(DatabaseContentProviders.ConversationList.CONTENT_URI, null);
    };
    this.databaseHelper = databaseHelper;
    this.conversationListNotificationDebouncer = ApplicationContext.getInstance(context).getConversationListDebouncer();
  }

  protected void notifyConversationListeners(Set<Long> threadIds) {
    for (long threadId : threadIds)
      notifyConversationListeners(threadId);
  }

  protected void notifyConversationListeners(long threadId) {
    ConversationNotificationDebouncer.Companion.get(context).notify(threadId);
  }

  protected void notifyConversationListListeners() {
    conversationListNotificationDebouncer.publish(conversationListUpdater);
  }

  protected void notifyStickerListeners() {
    context.getContentResolver().notifyChange(DatabaseContentProviders.Sticker.CONTENT_URI, null);
  }

  protected void notifyStickerPackListeners() {
    context.getContentResolver().notifyChange(DatabaseContentProviders.StickerPack.CONTENT_URI, null);
  }

  protected void setNotifyConverationListeners(Cursor cursor, long threadId) {
    cursor.setNotificationUri(context.getContentResolver(), DatabaseContentProviders.Conversation.getUriForThread(threadId));
  }

  protected void setNotifyConverationListListeners(Cursor cursor) {
    cursor.setNotificationUri(context.getContentResolver(), DatabaseContentProviders.ConversationList.CONTENT_URI);
  }

  protected void registerAttachmentListeners(@NonNull ContentObserver observer) {
    context.getContentResolver().registerContentObserver(DatabaseContentProviders.Attachment.CONTENT_URI,
                                                         true,
                                                         observer);
  }

  protected void notifyAttachmentListeners() {
    context.getContentResolver().notifyChange(DatabaseContentProviders.Attachment.CONTENT_URI, null);
  }

  public void reset(SQLCipherOpenHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

  protected void notifyRecipientListeners() {
    context.getContentResolver().notifyChange(DatabaseContentProviders.Recipient.CONTENT_URI, null);
    notifyConversationListListeners();
  }

}

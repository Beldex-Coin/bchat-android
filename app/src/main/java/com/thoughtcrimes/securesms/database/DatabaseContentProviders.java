package com.thoughtcrimes.securesms.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Starting in API 26, a {@link ContentProvider} needs to be defined for each authority you wish to
 * observe changes on. These classes essentially do nothing except exist so Android doesn't complain.
 */
public class DatabaseContentProviders {

  public static class ConversationList extends NoopContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://io.beldex.securesms.database.conversationlist");
  }

  public static class Conversation extends NoopContentProvider {
    private static final String CONTENT_URI_STRING = "content://io.beldex.securesms.database.conversation/";

    public static Uri getUriForThread(long threadId) {
      return Uri.parse(CONTENT_URI_STRING + threadId);
    }
  }

  public static class Attachment extends NoopContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://io.beldex.securesms.database.attachment");
  }

  public static class Sticker extends NoopContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://io.beldex.securesms.database.sticker");
  }

  public static class StickerPack extends NoopContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://io.beldex.securesms.database.stickerpack");
  }

  private static abstract class NoopContentProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
      return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
      return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
      return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
      return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
      return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
      return 0;
    }
  }
}

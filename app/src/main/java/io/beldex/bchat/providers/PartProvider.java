/*
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
package io.beldex.bchat.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

import io.beldex.bchat.BuildConfig;
import io.beldex.bchat.mms.PartUriParser;
import io.beldex.bchat.service.KeyCachingService;
import io.beldex.bchat.util.MemoryFileUtil;
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentId;
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.dependencies.DatabaseComponent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PartProvider extends ContentProvider {

  private static final String TAG = PartProvider.class.getSimpleName();

  private static final String CONTENT_URI_STRING = "content://"+ BuildConfig.providerId +".provider.securesms/part";
  private static final Uri    CONTENT_URI        = Uri.parse(CONTENT_URI_STRING);
  private static final int    SINGLE_ROW         = 1;

  private static final UriMatcher uriMatcher;

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(BuildConfig.providerId + ".provider.securesms", "part/*/#", SINGLE_ROW);
  }

  @Override
  public boolean onCreate() {
    Log.i(TAG, "onCreate()");
    return true;
  }

  public static Uri getContentUri(AttachmentId attachmentId) {
    Uri uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(attachmentId.getUniqueId()));
    return ContentUris.withAppendedId(uri, attachmentId.getRowId());
  }

  @Override
  public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
    Log.i(TAG, "openFile() called!");

    if (KeyCachingService.isLocked(getContext())) {
      Log.w(TAG, "masterSecret was null, abandoning.");
      return null;
    }

    switch (uriMatcher.match(uri)) {
    case SINGLE_ROW:
      Log.i(TAG, "Parting out a single row...");
      try {
        final PartUriParser partUri = new PartUriParser(uri);
        return getParcelStreamForAttachment(partUri.getPartId());
      } catch (IOException ioe) {
        Log.w(TAG, ioe);
        throw new FileNotFoundException("Error opening file");
      }
    }

    throw new FileNotFoundException("Request for bad part.");
  }

  @Override
  public int delete(@NonNull Uri arg0, String arg1, String[] arg2) {
    Log.i(TAG, "delete() called");
    return 0;
  }

  @Override
  public String getType(@NonNull Uri uri) {
    Log.i(TAG, "getType() called: " + uri);

    switch (uriMatcher.match(uri)) {
      case SINGLE_ROW:
        PartUriParser      partUriParser = new PartUriParser(uri);
        DatabaseAttachment attachment    = DatabaseComponent.get(getContext()).attachmentDatabase()
                                                          .getAttachment(partUriParser.getPartId());

        if (attachment != null) {
          return attachment.getContentType();
        }
    }

    return null;
  }

  @Override
  public Uri insert(@NonNull Uri arg0, ContentValues arg1) {
    Log.i(TAG, "insert() called");
    return null;
  }

  @Override
  public Cursor query(@NonNull Uri url, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    Log.i(TAG, "query() called: " + url);

    if (projection == null || projection.length <= 0) return null;

    switch (uriMatcher.match(url)) {
      case SINGLE_ROW:
        PartUriParser      partUri      = new PartUriParser(url);
        DatabaseAttachment attachment   = DatabaseComponent.get(getContext()).attachmentDatabase().getAttachment(partUri.getPartId());

        if (attachment == null) return null;

        MatrixCursor       matrixCursor = new MatrixCursor(projection, 1);
        Object[]           resultRow    = new Object[projection.length];

        for (int i=0;i<projection.length;i++) {
          if (OpenableColumns.DISPLAY_NAME.equals(projection[i])) {
            resultRow[i] = attachment.getFileName();
          }
        }

        matrixCursor.addRow(resultRow);
        return matrixCursor;
    }

    return null;
  }

  @Override
  public int update(@NonNull Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
    Log.i(TAG, "update() called");
    return 0;
  }

  private ParcelFileDescriptor getParcelStreamForAttachment(AttachmentId attachmentId) throws IOException {
    long       plaintextLength = Util.getStreamLength(DatabaseComponent.get(getContext()).attachmentDatabase().getAttachmentStream(attachmentId, 0));
    MemoryFile memoryFile      = new MemoryFile(attachmentId.toString(), Util.toIntExact(plaintextLength));

    InputStream  in  = DatabaseComponent.get(getContext()).attachmentDatabase().getAttachmentStream(attachmentId, 0);
    OutputStream out = memoryFile.getOutputStream();

    Util.copy(in, out);
    Util.close(out);
    Util.close(in);

    return MemoryFileUtil.getParcelFileDescriptor(memoryFile);
  }
}

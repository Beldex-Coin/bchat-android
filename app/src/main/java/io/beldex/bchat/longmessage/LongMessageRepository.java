package io.beldex.bchat.longmessage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.beldex.bchat.database.model.MessageRecord;
import io.beldex.bchat.database.model.MmsMessageRecord;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.mms.PartAuthority;
import io.beldex.bchat.mms.TextSlide;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.database.MmsDatabase;
import io.beldex.bchat.database.SmsDatabase;
import io.beldex.bchat.database.model.MessageRecord;
import io.beldex.bchat.database.model.MmsMessageRecord;
import io.beldex.bchat.mms.PartAuthority;
import io.beldex.bchat.mms.TextSlide;

import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.concurrent.SignalExecutors;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.guava.Optional;

import io.beldex.bchat.dependencies.DatabaseComponent;

import java.io.IOException;
import java.io.InputStream;

class LongMessageRepository {

  private final static String TAG = LongMessageRepository.class.getSimpleName();

  private final MmsDatabase mmsDatabase;
  private final SmsDatabase smsDatabase;

  LongMessageRepository(@NonNull Context context) {
    this.mmsDatabase = DatabaseComponent.get(context).mmsDatabase();
    this.smsDatabase = DatabaseComponent.get(context).smsDatabase();
  }

  void getMessage(@NonNull Context context, long messageId, boolean isMms, @NonNull Callback<Optional<LongMessage>> callback) {
    SignalExecutors.BOUNDED.execute(() -> {
      if (isMms) {
        callback.onComplete(getMmsLongMessage(context, mmsDatabase, messageId));
      } else {
        callback.onComplete(getSmsLongMessage(smsDatabase, messageId));
      }
    });
  }

  @WorkerThread
  private Optional<LongMessage> getMmsLongMessage(@NonNull Context context, @NonNull MmsDatabase mmsDatabase, long messageId) {
    Optional<MmsMessageRecord> record = getMmsMessage(mmsDatabase, messageId);

    if (record.isPresent()) {
      TextSlide textSlide = record.get().getSlideDeck().getTextSlide();

      if (textSlide != null && textSlide.getUri() != null) {
        return Optional.of(new LongMessage(record.get(), readFullBody(context, textSlide.getUri())));
      } else {
        return Optional.of(new LongMessage(record.get(), ""));
      }
    } else {
      return Optional.absent();
    }
  }

  @WorkerThread
  private Optional<LongMessage> getSmsLongMessage(@NonNull SmsDatabase smsDatabase, long messageId) {
    Optional<MessageRecord> record = getSmsMessage(smsDatabase, messageId);

    if (record.isPresent()) {
      return Optional.of(new LongMessage(record.get(), ""));
    } else {
      return Optional.absent();
    }
  }


  @WorkerThread
  private Optional<MmsMessageRecord> getMmsMessage(@NonNull MmsDatabase mmsDatabase, long messageId) {
    try (Cursor cursor = mmsDatabase.getMessage(messageId)) {
      return Optional.fromNullable((MmsMessageRecord) mmsDatabase.readerFor(cursor).getNext());
    }
  }

  @WorkerThread
  private Optional<MessageRecord> getSmsMessage(@NonNull SmsDatabase smsDatabase, long messageId) {
    try (Cursor cursor = smsDatabase.getMessageCursor(messageId)) {
      return Optional.fromNullable(smsDatabase.readerFor(cursor).getNext());
    }
  }

  private String readFullBody(@NonNull Context context, @NonNull Uri uri) {
    try (InputStream stream = PartAuthority.getAttachmentStream(context, uri)) {
      return Util.readFullyAsString(stream);
    } catch (IOException e) {
      Log.w(TAG, "Failed to read full text body.", e);
      return "";
    }
  }

  interface Callback<T> {
    void onComplete(T result);
  }
}

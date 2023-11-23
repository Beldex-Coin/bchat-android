package com.thoughtcrimes.securesms.service;


import android.content.ComponentName;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.ShareActivity;
import com.thoughtcrimes.securesms.util.BitmapUtil;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.utilities.Log;
import com.thoughtcrimes.securesms.database.ThreadDatabase;
import com.thoughtcrimes.securesms.database.model.ThreadRecord;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.mms.GlideApp;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class DirectShareService extends ChooserTargetService {

  private static final String TAG = DirectShareService.class.getSimpleName();

  @Override
  public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
                                                 IntentFilter matchedFilter)
  {
    List<ChooserTarget> results        = new LinkedList<>();
    ComponentName       componentName  = new ComponentName(this, ShareActivity.class);
    ThreadDatabase      threadDatabase = DatabaseComponent.get(this).threadDatabase();
      try (Cursor cursor = threadDatabase.getDirectShareList()) {
          ThreadDatabase.Reader reader = threadDatabase.readerFor(cursor);
          ThreadRecord record;

          while ((record = reader.getNext()) != null && results.size() < 10) {
              Recipient recipient = Recipient.from(this, record.getRecipient().getAddress(), false);
              String name = recipient.toShortString();

              Bitmap avatar;

              if (recipient.getContactPhoto() != null) {
                  try {
                      avatar = GlideApp.with(this)
                              .asBitmap()
                              .load(recipient.getContactPhoto())
                              .circleCrop()
                              .submit(getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                      getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width))
                              .get();
                  } catch (InterruptedException | ExecutionException e) {
                      Log.w(TAG, e);
                      avatar = getFallbackDrawable(recipient);
                  }
              } else {
                  avatar = getFallbackDrawable(recipient);
              }

              Parcel parcel = Parcel.obtain();
              parcel.writeParcelable(recipient.getAddress(), 0);

              Bundle bundle = new Bundle();
              bundle.putLong(ShareActivity.EXTRA_THREAD_ID, record.getThreadId());
              bundle.putByteArray(ShareActivity.EXTRA_ADDRESS_MARSHALLED, parcel.marshall());
              bundle.putInt(ShareActivity.EXTRA_DISTRIBUTION_TYPE, record.getDistributionType());
              bundle.setClassLoader(getClassLoader());

              results.add(new ChooserTarget(name, Icon.createWithBitmap(avatar), 1.0f, componentName, bundle));
              parcel.recycle();
          }
          return results;
      }
  }

  private Bitmap getFallbackDrawable(@NonNull Recipient recipient) {
    return BitmapUtil.createFromDrawable(recipient.getFallbackContactPhotoDrawable(this, false),
                                         getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                         getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height));
  }
}

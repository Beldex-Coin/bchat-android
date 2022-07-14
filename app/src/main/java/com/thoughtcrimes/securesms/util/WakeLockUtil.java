package com.thoughtcrimes.securesms.util;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.annotation.NonNull;

import com.beldex.libbchat.utilities.ServiceUtil;
import com.beldex.libsignal.utilities.Log;

public class WakeLockUtil {

  private static final String TAG = WakeLockUtil.class.getSimpleName();

  /**
   * @param tag will be prefixed with "signal:" if it does not already start with it.
   */
  public static WakeLock acquire(@NonNull Context context, int lockType, long timeout, @NonNull String tag) {
    tag = prefixTag(tag);
    try {
      Log.d("Beldex","JobRunner acquire 2");
      PowerManager powerManager = ServiceUtil.getPowerManager(context);
      WakeLock     wakeLock     = powerManager.newWakeLock(lockType, tag);

      wakeLock.acquire(timeout);
      Log.d(TAG, "Acquired wakelock with tag: " + tag);

      return wakeLock;
    } catch (Exception e) {
      Log.w(TAG, "Failed to acquire wakelock with tag: " + tag, e);
      return null;
    }
  }

  /**
   * @param tag will be prefixed with "signal:" if it does not already start with it.
   */
  public static void release(@NonNull WakeLock wakeLock, @NonNull String tag) {
    tag = prefixTag(tag);
    try {
      Log.d("Beldex","JobRunner acquire 3");
      if (wakeLock.isHeld()) {
        wakeLock.release();
        Log.d(TAG, "Released wakelock with tag: " + tag);
      } else {
        Log.d(TAG, "Wakelock wasn't held at time of release: " + tag);
      }
    } catch (Exception e) {
      Log.w(TAG, "Failed to release wakelock with tag: " + tag, e);
    }
  }

  private static String prefixTag(@NonNull String tag) {
    return tag.startsWith("signal:") ? tag : "signal:" + tag;
  }
}

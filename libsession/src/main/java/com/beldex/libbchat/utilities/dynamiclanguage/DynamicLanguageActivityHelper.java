package com.beldex.libbchat.utilities.dynamiclanguage;

import android.app.Activity;

import androidx.annotation.MainThread;
import androidx.core.os.ConfigurationCompat;

import com.beldex.libsignal.utilities.Log;

import java.util.Locale;

public final class DynamicLanguageActivityHelper {

  private static final String TAG = DynamicLanguageActivityHelper.class.getSimpleName();

  private static String reentryProtection;

  /**
   * If the activity isn't in the specified language, it will restart the activity.
   */
  @MainThread
  public static void recreateIfNotInCorrectLanguage(Activity activity, String language) {
    Locale currentActivityLocale = ConfigurationCompat.getLocales(activity.getResources().getConfiguration()).get(0);
    Locale selectedLocale        = LocaleParser.findBestMatchingLocaleForLanguage(language);

    if (currentActivityLocale.equals(selectedLocale)) {
      reentryProtection = "";
      return;
    }

    String reentryKey = activity.getClass().getName() + ":" + selectedLocale;
    if (!reentryKey.equals(reentryProtection)) {
      reentryProtection = reentryKey;
      Log.d(TAG, String.format("Activity Locale %s, Selected locale %s, restarting", currentActivityLocale, selectedLocale));
      activity.recreate();
    } else {
      Log.d(TAG, String.format("Skipping recreate as looks like looping, Activity Locale %s, Selected locale %s", currentActivityLocale, selectedLocale));
    }
  }
}

package com.beldex.libbchat.utilities;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beldex.libsignal.utilities.Base64;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;

import java.io.IOException;

public class ProfileKeyUtil {

  public static synchronized @NonNull byte[] getProfileKey(@NonNull Context context) {
    try {
      String encodedProfileKey = TextSecurePreferences.getProfileKey(context);

      if (encodedProfileKey == null) {
        encodedProfileKey = Util.getSecret(32);
        TextSecurePreferences.setProfileKey(context, encodedProfileKey);
      }

      return Base64.decode(encodedProfileKey);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static synchronized @NonNull byte[] getProfileKeyFromEncodedString(String encodedProfileKey) {
    try {
      return Base64.decode(encodedProfileKey);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static synchronized @NonNull String generateEncodedProfileKey(@NonNull Context context) {
    return Util.getSecret(32);
  }

  public static synchronized void setEncodedProfileKey(@NonNull Context context, @Nullable String key) {
    TextSecurePreferences.setProfileKey(context, key);
  }
}

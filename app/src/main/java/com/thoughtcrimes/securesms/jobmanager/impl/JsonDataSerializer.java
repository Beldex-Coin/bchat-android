package com.thoughtcrimes.securesms.jobmanager.impl;

import androidx.annotation.NonNull;

import com.beldex.libbchat.messaging.utilities.Data;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.JsonUtil;

import java.io.IOException;

public class JsonDataSerializer implements Data.Serializer {

  private static final String TAG = Log.tag(JsonDataSerializer.class);

  @Override
  public @NonNull String serialize(@NonNull Data data) {
    try {
      return JsonUtil.toJsonThrows(data);
    } catch (IOException e) {
      Log.e(TAG, "Failed to serialize to JSON.", e);
      throw new AssertionError(e);
    }
  }

  @Override
  public @NonNull Data deserialize(@NonNull String serialized) {
    try {
      return JsonUtil.fromJson(serialized, Data.class);
    } catch (IOException e) {
      Log.e(TAG, "Failed to deserialize JSON.", e);
      throw new AssertionError(e);
    }
  }
}

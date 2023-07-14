package com.thoughtcrimes.securesms.util.adapter.mapping;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface MappingModel<T> {
  boolean areItemsTheSame(@NonNull T newItem);
  boolean areContentsTheSame(@NonNull T newItem);

  default @Nullable Object getChangePayload(@NonNull T newItem) {
    return null;
  }
}

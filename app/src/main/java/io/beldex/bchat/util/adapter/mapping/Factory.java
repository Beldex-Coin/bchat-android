package io.beldex.bchat.util.adapter.mapping;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import io.beldex.bchat.util.adapter.mapping.MappingModel;
import io.beldex.bchat.util.adapter.mapping.MappingViewHolder;

public interface Factory<T extends MappingModel<T>> {
  @NonNull
  MappingViewHolder<T> createViewHolder(@NonNull ViewGroup parent);
}

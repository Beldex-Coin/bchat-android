package com.thoughtcrimes.securesms.jobmanager.impl;

import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.jobmanager.ExecutorFactory;
import com.thoughtcrimes.securesms.jobmanager.ExecutorFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultExecutorFactory implements ExecutorFactory {
  @Override
  public @NonNull ExecutorService newSingleThreadExecutor(@NonNull String name) {
    return Executors.newSingleThreadExecutor(r -> new Thread(r, name));
  }
}

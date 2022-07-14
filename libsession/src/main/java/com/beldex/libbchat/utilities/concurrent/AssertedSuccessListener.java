package com.beldex.libbchat.utilities.concurrent;

import com.beldex.libsignal.utilities.ListenableFuture;

import java.util.concurrent.ExecutionException;

public abstract class AssertedSuccessListener<T> implements ListenableFuture.Listener<T> {

  @Override
  public void onFailure(ExecutionException e) {
    throw new AssertionError(e);
  }
}

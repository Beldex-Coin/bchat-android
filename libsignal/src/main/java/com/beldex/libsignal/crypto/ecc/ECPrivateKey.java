/**
 * Copyright (C) 2013-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package com.beldex.libsignal.crypto.ecc;

public interface ECPrivateKey {
  public byte[] serialize();
  //public String getType();
  public int getType();
}

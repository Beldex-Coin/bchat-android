/**
 * Copyright (C) 2013-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package com.beldex.libsignal.crypto.ecc;

public interface ECPublicKey extends Comparable<ECPublicKey> {

  public static final int KEY_SIZE = 33;

  public byte[] serialize();

  //public String getType();

  public int getType();
}

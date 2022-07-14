/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package com.beldex.libsignal.crypto;

import com.beldex.libsignal.crypto.ecc.ECPrivateKey;

/**
 * Holder for public and private identity key pair.
 *
 * @author Moxie Marlinspike
 */
public class IdentityKeyPair {

  private final IdentityKey  publicKey;
  private final ECPrivateKey privateKey;

  public IdentityKeyPair(IdentityKey publicKey, ECPrivateKey privateKey) {
    this.publicKey  = publicKey;
    this.privateKey = privateKey;
  }

  public IdentityKey getPublicKey() {
    return publicKey;
  }

  public ECPrivateKey getPrivateKey() {
    return privateKey;
  }
}

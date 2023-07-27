package com.beldex.libsignal.streams;

import static com.beldex.libsignal.crypto.CipherUtil.CIPHER_LOCK;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ProfileCipherOutputStream extends DigestingOutputStream {

  private final Cipher cipher;

  public ProfileCipherOutputStream(OutputStream out, byte[] key) throws IOException {
    super(out);
    try {
      this.cipher = Cipher.getInstance("AES/GCM/NoPadding");

      byte[] nonce  = generateNonce();
      this.cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));

      super.write(nonce, 0, nonce.length);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    } catch (NoSuchPaddingException e) {
      throw new AssertionError(e);
    } catch (InvalidAlgorithmParameterException e) {
      throw new AssertionError(e);
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void write(byte[] buffer) throws IOException {
    write(buffer, 0, buffer.length);
  }

  @Override
  public void write(byte[] buffer, int offset, int length) throws IOException {
    byte[] output = cipher.update(buffer, offset, length);
    super.write(output);
  }

  @Override
  public void write(int b) throws IOException {
    byte[] input = new byte[1];
    input[0] = (byte)b;

    byte[] output;
    synchronized (CIPHER_LOCK) {
      output = cipher.update(input);
    }
    super.write(output);
  }

  @Override
  public void flush() throws IOException {
    try {
      byte[] output;
      synchronized (CIPHER_LOCK) {
        output = cipher.doFinal();
      }

      super.write(output);
      super.flush();
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      throw new AssertionError(e);
    }
  }

  private byte[] generateNonce() {
    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);
    return nonce;
  }

  public static long getCiphertextLength(long plaintextLength) {
    return 12 + 16 + plaintextLength;
  }
}

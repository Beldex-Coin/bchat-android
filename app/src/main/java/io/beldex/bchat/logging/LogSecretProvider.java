package io.beldex.bchat.logging;

import android.content.Context;
import androidx.annotation.NonNull;

import io.beldex.bchat.crypto.KeyStoreHelper;
import com.beldex.libsignal.utilities.Base64;
import com.beldex.libbchat.utilities.TextSecurePreferences;

import java.io.IOException;
import java.security.SecureRandom;

class LogSecretProvider {

  static byte[] getOrCreateAttachmentSecret(@NonNull Context context) {
    String unencryptedSecret = TextSecurePreferences.getLogUnencryptedSecret(context);
    String encryptedSecret   = TextSecurePreferences.getLogEncryptedSecret(context);

    if      (unencryptedSecret != null) return parseUnencryptedSecret(unencryptedSecret);
    else if (encryptedSecret != null)   return parseEncryptedSecret(encryptedSecret);
    else                                return createAndStoreSecret(context);
  }

  private static byte[] parseUnencryptedSecret(String secret) {
    try {
      return Base64.decode(secret);
    } catch (IOException e) {
      throw new AssertionError("Failed to decode the unecrypted secret.");
    }
  }

  private static byte[] parseEncryptedSecret(String secret) {
    KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.SealedData.fromString(secret);
    return KeyStoreHelper.unseal(encryptedSecret);
  }

  private static byte[] createAndStoreSecret(@NonNull Context context) {
    SecureRandom random = new SecureRandom();
    byte[]       secret = new byte[32];
    random.nextBytes(secret);

    KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(secret);
    TextSecurePreferences.setLogEncryptedSecret(context, encryptedSecret.serialize());

    return secret;
  }
}

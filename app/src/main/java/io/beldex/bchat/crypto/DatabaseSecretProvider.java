package io.beldex.bchat.crypto;


import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;

import com.beldex.libbchat.utilities.TextSecurePreferences;

import java.io.IOException;
import java.security.SecureRandom;

public class DatabaseSecretProvider {

  @SuppressWarnings("unused")
  private static final String TAG = DatabaseSecretProvider.class.getSimpleName();

  private final Context context;

  public DatabaseSecretProvider(@NonNull Context context) {
    this.context = context.getApplicationContext();
  }

  public DatabaseSecret getOrCreateDatabaseSecret() {
    String unencryptedSecret = TextSecurePreferences.getDatabaseUnencryptedSecret(context);
    String encryptedSecret   = TextSecurePreferences.getDatabaseEncryptedSecret(context);

    if      (unencryptedSecret != null) return getUnencryptedDatabaseSecret(context, unencryptedSecret);
    else if (encryptedSecret != null)   return getEncryptedDatabaseSecret(encryptedSecret);
    else                                return createAndStoreDatabaseSecret(context);
  }

  private @NonNull DatabaseSecret getUnencryptedDatabaseSecret(@NonNull Context context, @NonNull String unencryptedSecret)
  {
    try {
      DatabaseSecret databaseSecret = new DatabaseSecret(unencryptedSecret);

      if (Build.VERSION.SDK_INT < 23) {
        return databaseSecret;
      } else {
        KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(databaseSecret.asBytes());

        TextSecurePreferences.setDatabaseEncryptedSecret(context, encryptedSecret.serialize());
        TextSecurePreferences.setDatabaseUnencryptedSecret(context, null);

        return databaseSecret;
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private @NonNull DatabaseSecret getEncryptedDatabaseSecret(@NonNull String serializedEncryptedSecret) {
    if (Build.VERSION.SDK_INT < 23) {
      throw new AssertionError("OS downgrade not supported. KeyStore sealed data exists on platform < M!");
    } else {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.SealedData.fromString(serializedEncryptedSecret);
      return new DatabaseSecret(KeyStoreHelper.unseal(encryptedSecret));
    }
  }

  private @NonNull DatabaseSecret createAndStoreDatabaseSecret(@NonNull Context context) {
    SecureRandom random = new SecureRandom();
    byte[]       secret = new byte[32];
    random.nextBytes(secret);

    DatabaseSecret databaseSecret = new DatabaseSecret(secret);

    if (Build.VERSION.SDK_INT >= 23) {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(databaseSecret.asBytes());
      TextSecurePreferences.setDatabaseEncryptedSecret(context, encryptedSecret.serialize());
    } else {
      TextSecurePreferences.setDatabaseUnencryptedSecret(context, databaseSecret.asString());
    }

    return databaseSecret;
  }
}

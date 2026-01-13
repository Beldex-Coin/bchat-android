package io.beldex.bchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import io.beldex.bchat.data.BarcodeData;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.home.HomeActivity;
import io.beldex.bchat.onboarding.SplashScreenActivity;
import io.beldex.bchat.service.KeyCachingService;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import io.beldex.bchat.webrtc.PowerButtonReceiver;

//TODO AC: Rename to ScreenLockActionBarActivity.
public abstract class PassphraseRequiredActionBarActivity extends BaseActionBarActivity {
  private static final String TAG = PassphraseRequiredActionBarActivity.class.getSimpleName();

  private static final int STATE_NORMAL                   = 0;
  private static final int STATE_PROMPT_PASSPHRASE        = 1;  //TODO AC: Rename to STATE_SCREEN_LOCKED
  private static final int STATE_UPGRADE_DATABASE         = 2;  //TODO AC: Rename to STATE_MIGRATE_DATA
  private static final int STATE_WELCOME_SCREEN           = 3;

  private BroadcastReceiver          clearKeyReceiver;
  private PowerButtonReceiver powerButtonReceiver = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate(" + savedInstanceState + ")");
    onPreCreate();

    final boolean locked = KeyCachingService.isLocked(this) &&
            TextSecurePreferences.isScreenLockEnabled(this) &&
            TextSecurePreferences.getLocalNumber(this) != null;
    routeApplicationState(locked);

    super.onCreate(savedInstanceState);

    if (!isFinishing()) {
      Log.i(TAG, "isFinishing if");
      initializeClearKeyReceiver();
      onCreate(savedInstanceState, true);
    }
  }

  protected void onPreCreate() {}
  protected void onCreate(Bundle savedInstanceState, boolean ready) {}

  @Override
  protected void onPause() {
    Log.i(TAG, "onPause()");
    super.onPause();
    boolean shouldRegister =
            TextSecurePreferences.getCallisActive(ApplicationContext.getInstance(this)) &&
                    TextSecurePreferences.getMuteVideo(ApplicationContext.getInstance(this));

    if (shouldRegister) {
      if (powerButtonReceiver == null) {
        powerButtonReceiver = new PowerButtonReceiver();
        registerReceiver(
                powerButtonReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF)
        );
      }
    } else {
      if (powerButtonReceiver != null) {
        unregisterReceiver(powerButtonReceiver);
        powerButtonReceiver = null;
      }
    }
  }
  @Override
  protected void onResume() {
    Log.i(TAG, "onResume()");
    super.onResume();

    boolean shouldRegister =
            TextSecurePreferences.getCallisActive(ApplicationContext.getInstance(this)) &&
                    TextSecurePreferences.getMuteVideo(ApplicationContext.getInstance(this));

    if (shouldRegister) {
      if (powerButtonReceiver == null) {
        powerButtonReceiver = new PowerButtonReceiver();
        registerReceiver(
                powerButtonReceiver,
                new IntentFilter(Intent.ACTION_USER_PRESENT)
        );
      }
    } else {
      if (powerButtonReceiver != null) {
        unregisterReceiver(powerButtonReceiver);
        powerButtonReceiver = null;
      }
    }
  }

  @Override
  protected void onDestroy() {
    Log.i(TAG, "onDestroy()");
    super.onDestroy();
    if (TextSecurePreferences.getCallisActive(ApplicationContext.getInstance(this))) {
      if (powerButtonReceiver != null) {
        this.unregisterReceiver(powerButtonReceiver);
        powerButtonReceiver = null;
      }
    }
    removeClearKeyReceiver(this);
  }

  public void onMasterSecretCleared() {
    Log.i(TAG, "onMasterSecretCleared(), if");

    if (ApplicationContext.getInstance(this).isAppVisible()) {
      routeApplicationState(true);
    } else {
      Log.i(TAG, "onMasterSecretCleared(), else");
      finish();
    }
  }

  private void routeApplicationState(boolean locked) {
    Intent intent = getIntentForState(getApplicationState(locked));
    if (intent != null) {
      startActivity(intent);
      Log.i(TAG, "routeApplicationState if  ");
      finish();
    }
  }

  private Intent getIntentForState(int state) {
    Log.i(TAG, "routeApplicationState(), state: " + state);

    switch (state) {
    case STATE_PROMPT_PASSPHRASE:        return getPromptPassphraseIntent();
    case STATE_UPGRADE_DATABASE:         return getUpgradeDatabaseIntent();
    case STATE_WELCOME_SCREEN:           return getWelcomeIntent();
    default:                             return null;
    }
  }

  private int getApplicationState(boolean locked) {
    if (locked) {
      return STATE_PROMPT_PASSPHRASE;
    } else if (DatabaseUpgradeActivity.isUpdate(this)) {
      return STATE_UPGRADE_DATABASE;
    } else if (!TextSecurePreferences.hasSeenWelcomeScreen(this)) {
      return STATE_WELCOME_SCREEN;
    } else {
      return STATE_NORMAL;
    }
  }

  private Intent getPromptPassphraseIntent() {
    return getRoutedNextIntent(getIntent(),true);
  }

  private Intent getUpgradeDatabaseIntent() {
    return getRoutedIntent(getConversationListIntent());
  }

  private Intent getWelcomeIntent() {
    return getRoutedNextIntent(getConversationListIntent(),false);
  }

  private Intent getConversationListIntent() {
    return new Intent(this, HomeActivity.class);
  }

  private Intent getRoutedIntent(@Nullable Intent nextIntent) {
    final Intent intent = new Intent(this, DatabaseUpgradeActivity.class);
    if (nextIntent != null)   intent.putExtra("next_intent", nextIntent);
    return intent;
  }

  private Intent getRoutedNextIntent(@Nullable Intent nextIntent, Boolean next) {
    final Intent intent = new Intent(this, SplashScreenActivity.class);
    intent.putExtra("nextPage",next);
    if (nextIntent != null)   intent.putExtra("next_intent", nextIntent);
    return intent;
  }

  private void initializeClearKeyReceiver() {
    Log.i(TAG, "initializeClearKeyReceiver()");
    this.clearKeyReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive() for clear key event");
        onMasterSecretCleared();
      }
    };

    IntentFilter filter = new IntentFilter(KeyCachingService.CLEAR_KEY_EVENT);
    ContextCompat.registerReceiver(
            this,
            clearKeyReceiver, filter,
            KeyCachingService.KEY_PERMISSION,
            null,
            ContextCompat.RECEIVER_NOT_EXPORTED
    );
  }

  private void removeClearKeyReceiver(Context context) {
    if (clearKeyReceiver != null) {
      context.unregisterReceiver(clearKeyReceiver);
      clearKeyReceiver = null;
    }
  }

  public void onUriScanned(BarcodeData barcodeData) {

  }
}

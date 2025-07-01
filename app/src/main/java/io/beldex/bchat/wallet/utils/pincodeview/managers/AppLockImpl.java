package io.beldex.bchat.wallet.utils.pincodeview.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import io.beldex.bchat.wallet.utils.keyboardview.interfaces.LifeCycleInterface;
import io.beldex.bchat.wallet.utils.pincodeview.PinActivity;
import io.beldex.bchat.wallet.utils.pincodeview.encryption.Encryptor;

import java.security.SecureRandom;
import java.util.Arrays;

public class AppLockImpl<T extends AppLockActivity> extends AppLock implements LifeCycleInterface {

    public static final String TAG = "AppLockImpl";

    /**
     * The {@link android.content.SharedPreferences} key used to store the password
     */
    private static final String PASSWORD_PREFERENCE_KEY = "PASSCODE";
    /**
     * The {@link android.content.SharedPreferences} key used to store the last active time
     */
    private static final String LAST_ACTIVE_MILLIS_PREFERENCE_KEY = "LAST_ACTIVE_MILLIS";
    /**
     * The {@link android.content.SharedPreferences} key used to store the timeout
     */
    private static final String TIMEOUT_MILLIS_PREFERENCE_KEY = "TIMEOUT_MILLIS_PREFERENCE_KEY";
    /**
     * The {@link android.content.SharedPreferences} key used to store the logo resource id
     */
    private static final String LOGO_ID_PREFERENCE_KEY = "LOGO_ID_PREFERENCE_KEY";
    /**
     * The {@link android.content.SharedPreferences} key used to store the forgot option
     */
    private static final String SHOW_FORGOT_PREFERENCE_KEY = "SHOW_FORGOT_PREFERENCE_KEY";
    /**
     * The {@link SharedPreferences} key used to store whether the user has backed out of the {@link AppLockActivity}
     */
    private static final String PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY = "PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY";
    /**
     * The {@link android.content.SharedPreferences} key used to store the dynamically generated password salt
     */
    private static final String PASSWORD_SALT_PREFERENCE_KEY = "PASSWORD_SALT_PREFERENCE_KEY";

    /**
     * The default password salt
     */
    private static final String DEFAULT_PASSWORD_SALT = "7xn7@c$";
    /**
     * The key algorithm used to generating the dynamic salt
     */
    private static final String KEY_ALGORITHM = "PBEWithMD5AndDES";
    /**
     * The key length of the salt
     */
    private static final int KEY_LENGTH = 256;
    /**
     * The number of iterations used to generate a dynamic salt
     */
    private static final int KEY_ITERATIONS = 20;

    /**
     * The {@link android.content.SharedPreferences} used to store the password, the last active time etc...
     */
    private SharedPreferences mSharedPreferences;

    /**
     * The activity class that extends {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     */
    private Class<T> mActivityClass;

    /**
     * Static instance of {@link AppLockImpl}
     */
    private static AppLockImpl mInstance;

    /**
     * Static method that allows to get back the current static Instance of {@link AppLockImpl}
     *
     * @param context       The current context of the {@link Activity}
     * @param activityClass The activity extending {@link AppLockActivity}
     * @return The instance.
     */
    public static AppLockImpl getInstance(Context context, Class<? extends AppLockActivity> activityClass) {
        synchronized (LockManager.class) {
            if (mInstance == null) {
                mInstance = new AppLockImpl<>(context, activityClass);
            }
        }
        return mInstance;
    }

    private AppLockImpl(Context context, Class<T> activityClass) {
        super();
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mActivityClass = activityClass;
    }

    @Override
    public void setTimeout(long timeout) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(TIMEOUT_MILLIS_PREFERENCE_KEY, timeout);
        editor.apply();
    }

    private void setSalt(String salt) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PASSWORD_SALT_PREFERENCE_KEY, salt);
        editor.apply();
    }

    private String getSalt() {
        String salt = mSharedPreferences.getString(PASSWORD_SALT_PREFERENCE_KEY, null);
        if (salt == null) {
            salt = generateSalt();
            setSalt(salt);
        }
        return salt;
    }

    private String generateSalt() {
        byte[] salt = new byte[KEY_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(System.currentTimeMillis());
            sr.nextBytes(salt);
            return Arrays.toString(salt);
        } catch (Exception e) {
            salt = DEFAULT_PASSWORD_SALT.getBytes();
        }
        return Base64.encodeToString(salt, Base64.DEFAULT);
    }

    @Override
    public long getTimeout() {
        return mSharedPreferences.getLong(TIMEOUT_MILLIS_PREFERENCE_KEY, DEFAULT_TIMEOUT);
    }

    @Override
    public void setLogoId(int logoId) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(LOGO_ID_PREFERENCE_KEY, logoId);
        editor.apply();
    }

    @Override
    public int getLogoId() {
        return mSharedPreferences.getInt(LOGO_ID_PREFERENCE_KEY, LOGO_ID_NONE);
    }

    @Override
    public void setShouldShowForgot(boolean showForgot) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SHOW_FORGOT_PREFERENCE_KEY, showForgot);
        editor.apply();
    }

    @Override
    public boolean pinChallengeCancelled() {
        return mSharedPreferences.getBoolean(PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY, false);
    }

    @Override
    public void setPinChallengeCancelled(boolean backedOut) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PIN_CHALLENGE_CANCELLED_PREFERENCE_KEY, backedOut);
        editor.apply();
    }

    @Override
    public boolean shouldShowForgot() {
        return mSharedPreferences.getBoolean(SHOW_FORGOT_PREFERENCE_KEY, true);
    }

    @Override
    public void enable() {
        PinActivity.setListener(this);
        //PinCompatActivity.setListener(this);
        //PinFragmentActivity.setListener(this);
    }

    @Override
    public void disable() {
        PinActivity.clearListeners();
        //PinCompatActivity.clearListeners();
        //PinFragmentActivity.clearListeners();
    }

    @Override
    public void disableAndRemoveConfiguration() {
        PinActivity.clearListeners();
        //PinCompatActivity.clearListeners();
        //PinFragmentActivity.clearListeners();
        mSharedPreferences.edit().remove(PASSWORD_PREFERENCE_KEY)
                .remove(LAST_ACTIVE_MILLIS_PREFERENCE_KEY)
                .remove(TIMEOUT_MILLIS_PREFERENCE_KEY)
                .remove(LOGO_ID_PREFERENCE_KEY)
                .remove(SHOW_FORGOT_PREFERENCE_KEY)
                .apply();
    }

    @Override
    public long getLastActiveMillis() {
        return mSharedPreferences.getLong(LAST_ACTIVE_MILLIS_PREFERENCE_KEY, 0);
    }

    @Override
    public void setLastActiveMillis() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(LAST_ACTIVE_MILLIS_PREFERENCE_KEY, System.currentTimeMillis());
        editor.apply();
    }

    @Override
    public boolean checkPasscode(String passcode) {
        String salt = getSalt();
        passcode = salt + passcode + salt;
        passcode = Encryptor.getSHA1(passcode);
        String storedPasscode = "";

        if (mSharedPreferences.contains(PASSWORD_PREFERENCE_KEY)) {
            storedPasscode = mSharedPreferences.getString(PASSWORD_PREFERENCE_KEY, "");
        }

        if (passcode.equalsIgnoreCase(storedPasscode)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean setPasscode(String passcode) {
        String salt = getSalt();
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        if (passcode == null) {
            editor.remove(PASSWORD_PREFERENCE_KEY);
            editor.apply();
            this.disable();
        } else {
            passcode = salt + passcode + salt;
            passcode = Encryptor.getSHA1(passcode);
            editor.putString(PASSWORD_PREFERENCE_KEY, passcode);
            editor.apply();
            this.enable();
        }

        return true;
    }

    @Override
    public boolean isPasscodeSet() {
        if (mSharedPreferences.contains(PASSWORD_PREFERENCE_KEY)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isIgnoredActivity(Activity activity) {
        String clazzName = activity.getClass().getName();

        // ignored activities
        if (mIgnoredActivities.contains(clazzName)) {
            Log.d(TAG, "ignore activity " + clazzName);
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldLockSceen(Activity activity) {
        Log.d(TAG, "Lollipin shouldLockSceen() called");

        // previously backed out of pin screen
        if (pinChallengeCancelled()) {
            return true;
        }

        // already unlock
        if (activity instanceof AppLockActivity) {
            AppLockActivity ala = (AppLockActivity) activity;
            if (ala.getType() == AppLock.UNLOCK_PIN) {
                Log.d(TAG, "already unlock activity");
                return false;
            }
        }

        // no pass code set
        if (!isPasscodeSet()) {
            Log.d(TAG, "lock passcode not set.");
            return false;
        }

        // no enough timeout
        long lastActiveMillis = getLastActiveMillis();
        long passedTime = System.currentTimeMillis() - lastActiveMillis;
        long timeout = getTimeout();
        if (lastActiveMillis > 0 && passedTime <= timeout) {
            Log.d(TAG, "no enough timeout " + passedTime + " for "
                    + timeout);
            return false;
        }

        return true;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (isIgnoredActivity(activity)) {
            return;
        }

        String clazzName = activity.getClass().getName();
        Log.d(TAG, "onActivityPaused " + clazzName);

        setLastActiveMillis();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isIgnoredActivity(activity)) {
            return;
        }

        String clazzName = activity.getClass().getName();
        Log.d(TAG, "onActivityResumed " + clazzName);

        //SteveJosephh21
        if(!TextSecurePreferences.getChangePin(activity.getApplicationContext())) {
            TextSecurePreferences.setChangePin(activity.getApplicationContext(),false);
            if (shouldLockSceen(activity)) {
                Log.d(TAG, "mActivityClass.getClass() " + mActivityClass);
                Intent intent = new Intent(activity.getApplicationContext(),
                        mActivityClass);
                intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.getApplication().startActivity(intent);
            }
        }else{
            TextSecurePreferences.setChangePin(activity.getApplicationContext(),false);
        }

        setLastActiveMillis();
    }
}

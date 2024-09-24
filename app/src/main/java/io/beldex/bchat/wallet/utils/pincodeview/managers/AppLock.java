package io.beldex.bchat.wallet.utils.pincodeview.managers;

import android.app.Activity;

import java.util.HashSet;

public abstract class AppLock {
    /**
     * ENABLE_PINLOCK type, uses at firt to define the password
     */
    public static final int ENABLE_PINLOCK = 0;
    /**
     * DISABLE_PINLOCK type, uses to disable the system by asking the current password
     */
    public static final int DISABLE_PINLOCK = 1;
    /**
     * CHANGE_PIN type, uses to change the current password
     */
    public static final int CHANGE_PIN = 2;
    /**
     * CONFIRM_PIN type, used to confirm the new password
     */
    public static final int CONFIRM_PIN = 3;
    /**
     * UNLOCK_PIN type, uses to ask the password to the user, in order to unlock the app
     */
    public static final int UNLOCK_PIN = 4;

    /**
     * LOGO_ID_NONE used to denote when a user has not set a logoId using {@link #setLogoId(int)}
     */
    public static final int LOGO_ID_NONE = -1;

    /**
     * EXTRA_TYPE, uses to pass to the {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     * to determine in which type it musts be started.
     */
    public static final String EXTRA_TYPE = "type";

    /**
     * DEFAULT_TIMEOUT, define the default timeout returned by {@link #getTimeout()}.
     * If you want to modify it, you can call {@link #setTimeout(long)}. Will be stored using
     * {@link android.content.SharedPreferences}
     */
    public static final long DEFAULT_TIMEOUT = 1000 * 10; // 10sec

    /**
     * A {@link java.util.HashSet} of {@link java.lang.String} which are the classes we don't want to
     * take into account for the {@link io.beldex.bchat.wallet.utils.pincodeview.PinActivity}. These activities
     * will not log the last opened time, will not launch the
     * {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity} etc...
     */
    protected HashSet<String> mIgnoredActivities;

    public AppLock() {
        mIgnoredActivities = new HashSet<String>();
    }

    /**
     * Add an ignored activity to the {@link java.util.HashSet}
     */
    public void addIgnoredActivity(Class<?> clazz) {
        String clazzName = clazz.getName();
        this.mIgnoredActivities.add(clazzName);
    }

    /**
     * Remove an ignored activity to the {@link java.util.HashSet}
     */
    public void removeIgnoredActivity(Class<?> clazz) {
        String clazzName = clazz.getName();
        this.mIgnoredActivities.remove(clazzName);
    }

    /**
     * Get the timeout used in {@link #shouldLockSceen(android.app.Activity)}
     */
    public abstract long getTimeout();

    /**
     * Set the timeout used in {@link #shouldLockSceen(android.app.Activity)}
     */
    public abstract void setTimeout(long timeout);

    /**
     * Get logo resource id used by {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     */
    public abstract int getLogoId();

    /**
     * Set logo resource id used by {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     */
    public abstract void setLogoId(int logoId);

    /**
     * Get the forgot option used by {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     */
    public abstract boolean shouldShowForgot();

    /**
     * Set the forgot option used by {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     */
    public abstract void setShouldShowForgot(boolean showForgot);

    /**
     * Get whether the user backed out of the {@link AppLockActivity} previously
     */
    public abstract boolean pinChallengeCancelled();

    /**
     * Set whether the user backed out of the {@link AppLockActivity}
     */
    public abstract void setPinChallengeCancelled(boolean cancelled);

    /**
     * Enable the {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLock} by setting
     * {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockImpl} as the
     * {@link io.beldex.bchat.wallet.utils.keyboardview.interfaces.LifeCycleInterface}
     */
    public abstract void enable();

    /**
     * Disable the {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLock} by removing any
     * {@link io.beldex.bchat.wallet.utils.keyboardview.interfaces.LifeCycleInterface}
     */
    public abstract void disable();

    /**
     * Disable the {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLock} by removing any
     * {@link io.beldex.bchat.wallet.utils.keyboardview.interfaces.LifeCycleInterface} and also delete
     * all the previous saved configurations into {@link android.content.SharedPreferences}
     */
    public abstract void disableAndRemoveConfiguration();

    /**
     * Get the last active time of the app used by {@link #shouldLockSceen(android.app.Activity)}
     */
    public abstract long getLastActiveMillis();

    /**
     * Set the last active time of the app used by {@link #shouldLockSceen(android.app.Activity)}.
     * Set in {@link io.beldex.bchat.wallet.utils.keyboardview.interfaces.LifeCycleInterface#onActivityPaused(android.app.Activity)}
     * and {@link io.beldex.bchat.wallet.utils.keyboardview.interfaces.LifeCycleInterface#onActivityResumed(android.app.Activity)}
     */
    public abstract void setLastActiveMillis();

    /**
     * Set the passcode (store his SHA1 into {@link android.content.SharedPreferences}) using the
     * {@link com.github.orangegangsters.lollipin.lib.encryption.Encryptor} class.
     */
    public abstract boolean setPasscode(String passcode);

    /**
     * Check the passcode by comparing his SHA1 into {@link android.content.SharedPreferences} using the
     * {@link com.github.orangegangsters.lollipin.lib.encryption.Encryptor} class.
     */
    public abstract boolean checkPasscode(String passcode);

    /**
     * Check the {@link android.content.SharedPreferences} to see if a password already exists
     */
    public abstract boolean isPasscodeSet();

    /**
     * Check if an activity must be ignored and then don't call the
     * {@link io.beldex.bchat.wallet.utils.keyboardview.interfaces.LifeCycleInterface}
     */
    public abstract boolean isIgnoredActivity(Activity activity);

    /**
     * Evaluates if:
     * - we are already into the {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     * - the passcode is not set
     * - the timeout didn't reached
     * If any of this is true, then we don't need to start the
     * {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity} (it returns false)
     * Otherwise returns true
     */
    public abstract boolean shouldLockSceen(Activity activity);
}
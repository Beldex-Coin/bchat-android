package io.beldex.bchat.wallet.utils.pincodeview.managers;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import io.beldex.bchat.wallet.utils.keyboardview.KeyboardView;
import io.beldex.bchat.wallet.utils.keyboardview.enums.KeyboardButtonEnum;
import io.beldex.bchat.wallet.utils.keyboardview.interfaces.KeyboardButtonClickedListener;
import io.beldex.bchat.wallet.utils.pincodeview.PinActivity;
import io.beldex.bchat.wallet.utils.pincodeview.PinCodeRoundView;

import java.util.Arrays;
import java.util.List;

import io.beldex.bchat.R;

/**
 * The activity that appears when the password needs to be set or has to be asked.
 * Call this activity in normal or singleTop mode (not singleTask or singleInstance, it does not work
 * with {@link android.app.Activity#startActivityForResult(android.content.Intent, int)}).
 */
public abstract class AppLockActivityDuplicate extends PinActivity implements KeyboardButtonClickedListener, View.OnClickListener, FingerprintUiHelper.Callback {

    public static final String TAG = AppLockActivity.class.getSimpleName();
    public static final String ACTION_CANCEL = TAG + ".actionCancelled";
    private static final int DEFAULT_PIN_LENGTH = 4;

    protected TextView mStepTextView;
    protected TextView mForgotTextView;
    protected PinCodeRoundView mPinCodeRoundView;
    protected KeyboardView mKeyboardView;
    protected ImageView mFingerprintImageView;
    protected TextView mFingerprintTextView;

    protected LockManager mLockManager;


    protected FingerprintManager mFingerprintManager;
    protected FingerprintUiHelper mFingerprintUiHelper;

    protected int mType = AppLock.UNLOCK_PIN;
    protected int mAttempts = 1;
    protected String mPinCode;
    //Steve Josephh
    protected String oldPinCode;

    protected String mOldPinCode;

    protected boolean changePin = false;
    protected boolean sendAuthentication = false;

    /**
     * First creation
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getContentView());
        initializeToolbar();
        initLayout(getIntent());
        if (TextSecurePreferences.isScreenSecurityEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void initializeToolbar() {
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
//    actionBar.setHomeButtonEnabled(false);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        actionBar.setTitle("Wallet Password");*/
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowHomeEnabled(false);
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(false);
        actionbar.setHomeButtonEnabled(false);

        actionbar.setCustomView(R.layout.bchat_logo_action_bar_content);
        actionbar.setDisplayShowCustomEnabled(true);

        Toolbar rootView  = (Toolbar) actionbar.getCustomView().getParent();
        rootView.setPadding(0,0,0,0);
        rootView.setContentInsetsAbsolute(0,0);

        View backButton = actionbar.getCustomView().findViewById(R.id.back_button);
        TextView titleName = actionbar.getCustomView().findViewById(R.id.title_name);
        titleName.setText(getString(R.string.activity_wallet_password_page_title));
        backButton.setOnClickListener(view -> {
            onBackPressed();
            /*  onSupportNavigateUp();*/
        });
    }
   /* @Override
    public boolean onSupportNavigateUp() {
        if (super.onSupportNavigateUp()) return true;

        onBackPressed();
        return true;
    }*/

    /**
     * If called in singleTop mode
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        initLayout(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* initializeToolbar();*/
        //Init layout for Fingerprint
        //initLayoutForFingerprint();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFingerprintUiHelper != null) {
            mFingerprintUiHelper.stopListening();
        }
    }

    /**
     * Init completely the layout, depending of the extra {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLock#EXTRA_TYPE}
     */
    private void initLayout(Intent intent) {
        //Animate if greater than 2.3.3
        overridePendingTransition(R.anim.nothing, R.anim.nothing);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            mType = extras.getInt(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
            changePin = extras.getBoolean("change_pin",false);
            sendAuthentication = extras.getBoolean("send_authentication",false);
        }

        mLockManager = LockManager.getInstance();
        mPinCode = "";
        mOldPinCode = "";
        oldPinCode="";

        enableAppLockerIfDoesNotExist();
        mLockManager.getAppLock().setPinChallengeCancelled(false);

        mStepTextView = (TextView) this.findViewById(R.id.pin_code_step_textview);
        mPinCodeRoundView = (PinCodeRoundView) this.findViewById(R.id.pin_code_round_view);
        mPinCodeRoundView.setPinLength(this.getPinLength());
        mForgotTextView = (TextView) this.findViewById(R.id.pin_code_forgot_textview);
        mForgotTextView.setOnClickListener(this);
        mKeyboardView = (KeyboardView) this.findViewById(R.id.pin_code_keyboard_view);
        mKeyboardView.setKeyboardButtonClickedListener(this);

       /* int logoId = mLockManager.getAppLock().getLogoId();
        ImageView logoImage = ((ImageView) findViewById(R.id.pin_code_logo_imageview));
        if (logoId != AppLock.LOGO_ID_NONE) {
            logoImage.setVisibility(View.VISIBLE);
            logoImage.setImageResource(logoId);
        }*/
        /*mForgotTextView.setText(getForgotText());
        mForgotTextView.setVisibility(mLockManager.getAppLock().shouldShowForgot() ? View.VISIBLE : View.GONE);*/

        Log.d("AppLockActivity 1",String.valueOf(mType));
        if(changePin) {
            Log.d("AppLockActivity 2",String.valueOf(mType));
            mStepTextView.setText(getStepText(AppLock.CHANGE_PIN));
        }else{
            Log.d("AppLockActivity 3",String.valueOf(mType));
            setStepText();
        }
    }

    /**
     * Init {@link FingerprintManager} of the {@link android.os.Build.VERSION#SDK_INT} is > to Marshmallow
     * and {@link FingerprintManager#isHardwareDetected()}.
     */
    private void initLayoutForFingerprint() {
        mFingerprintImageView = (ImageView) this.findViewById(R.id.pin_code_fingerprint_imageview);
        mFingerprintTextView = (TextView) this.findViewById(R.id.pin_code_fingerprint_textview);
        if (mType == AppLock.UNLOCK_PIN) {
            mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
            mFingerprintUiHelper = new FingerprintUiHelper.FingerprintUiHelperBuilder(mFingerprintManager).build(mFingerprintImageView, mFingerprintTextView, this);
            try {
                if (mFingerprintManager.isHardwareDetected() && mFingerprintUiHelper.isFingerprintAuthAvailable()) {
                    //SteveJosephh21
                    /*mFingerprintImageView.setVisibility(View.VISIBLE);
                    mFingerprintTextView.setVisibility(View.VISIBLE);*/
                    mFingerprintUiHelper.startListening();
                } else {
                    mFingerprintImageView.setVisibility(View.GONE);
                    mFingerprintTextView.setVisibility(View.GONE);
                }
            } catch (SecurityException e) {
                Log.e(TAG, e.toString());
                mFingerprintImageView.setVisibility(View.GONE);
                mFingerprintTextView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Re enable {@link AppLock} if it has been collected to avoid
     * {@link NullPointerException}.
     */
    @SuppressWarnings("unchecked")
    private void enableAppLockerIfDoesNotExist() {
        try {
            if (mLockManager.getAppLock() == null) {
                mLockManager.enableAppLock(this, getCustomAppLockActivityClass());
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Init the {@link #mStepTextView} based on {@link #mType}
     */
    private void setStepText() {
        mStepTextView.setText(getStepText(mType));
    }

    /**
     * Gets the {@link String} to be used in the {@link #mStepTextView} based on {@link #mType}
     *
     * @param reason The {@link #mType} to return a {@link String} for
     * @return The {@link String} for the {@link AppLockActivity}
     */
    public String getStepText(int reason) {
        String msg = null;
        switch (reason) {
            case AppLock.DISABLE_PINLOCK:
                msg = getString(R.string.pin_code_step_disable, this.getPinLength());
                break;
            case AppLock.ENABLE_PINLOCK:
                msg = getString(R.string.pin_code_step_create);
                break;
            case AppLock.CHANGE_PIN:
                msg = getString(R.string.pin_code_step_change, this.getPinLength());
                break;
            case AppLock.UNLOCK_PIN:
                msg = getString(R.string.pin_code_step_unlock);
                break;
            case AppLock.CONFIRM_PIN:
                msg = getString(R.string.pin_code_step_enable_confirm);
                break;
        }
        return msg;
    }

    public String getForgotText() {
        return getString(R.string.pin_code_forgot_text);
    }

    /**
     * Overrides to allow a slide_down animation when finishing
     */
    @Override
    public void finish() {
        super.finish();
        if (mLockManager != null) {
            AppLock appLock = mLockManager.getAppLock();
            if (appLock != null) {
                appLock.setLastActiveMillis();
            }
        }
        //Animate if greater than 2.3.3
        overridePendingTransition(R.anim.nothing, R.anim.slide_down);
    }

    /**
     * Add the button clicked to {@link #mPinCode} each time.
     * Refreshes also the {@link io.beldex.bchat.wallet.utils.pincodeview.PinCodeRoundView}
     */
    @Override
    public void onKeyboardClick(KeyboardButtonEnum keyboardButtonEnum) {
        if (mPinCode.length() < this.getPinLength()) {
            int value = keyboardButtonEnum.getButtonValue();

            if (value == KeyboardButtonEnum.BUTTON_CLEAR.getButtonValue()) {
                if (!mPinCode.isEmpty()) {
                    setPinCode(mPinCode.substring(0, mPinCode.length() - 1));
                } else {
                    setPinCode("");
                }
            } else {
                setPinCode(mPinCode + value);
            }
        }
    }

    /**
     * Called at the end of the animation of the {@link io.beldex.bchat.wallet.utils.keyboardview.RippleView}
     * Calls {@link #onPinCodeInputed} when {@link #mPinCode}
     */
    @Override
    public void onRippleAnimationEnd() {
        if (mPinCode.length() == this.getPinLength()) {
            onPinCodeInputed();
        }
    }

    /**
     * Switch over the {@link #mType} to determine if the password is ok, if we should pass to the next step etc...
     */
    protected void onPinCodeInputed() {
        switch (mType) {
            case AppLock.DISABLE_PINLOCK:
                if (mLockManager.getAppLock().checkPasscode(mPinCode)) {
                    setResult(RESULT_OK);
                    mLockManager.getAppLock().setPasscode(null);
                    onPinCodeSuccess(1, this);
                    finish();
                } else {
                    onPinCodeError();
                }
                break;
            case AppLock.ENABLE_PINLOCK:
                if(changePin){
                    if(oldPinCode.equals(mPinCode)){
                        Toast.makeText(this, getString(R.string.change_pin_confirmation_message), Toast.LENGTH_SHORT).show();
                        onPinCodeError();
                    }else{
                        mOldPinCode = mPinCode;
                        setPinCode("");
                        mType = AppLock.CONFIRM_PIN;
                        setStepText();
                    }
                }else{
                    mOldPinCode = mPinCode;
                    setPinCode("");
                    mType = AppLock.CONFIRM_PIN;
                    setStepText();
                }
                break;
            case AppLock.CONFIRM_PIN:
                if (mPinCode.equals(mOldPinCode)) {
                    setResult(RESULT_OK);
                    TextSecurePreferences.setWalletEntryPassword(this,mPinCode);
                    mLockManager.getAppLock().setPasscode(mPinCode);
                    if(changePin) {
                        onPinCodeSuccess(7,this);
                    }else{
                        onPinCodeSuccess(3, this);
                    }
                } else {
                    mOldPinCode = "";
                    setPinCode("");
                    mType = AppLock.ENABLE_PINLOCK;
                    setStepText();
                    onPinCodeError();
                }
                break;
            case AppLock.CHANGE_PIN:
                if (mLockManager.getAppLock().checkPasscode(mPinCode)) {
                    oldPinCode=mPinCode;
                    mType = AppLock.ENABLE_PINLOCK;
                    setStepText();
                    setPinCode("");
                    onPinCodeSuccess(2, this);
                } else {
                    onPinCodeError();
                }
                break;
            case AppLock.UNLOCK_PIN:
                if (mLockManager.getAppLock().checkPasscode(mPinCode)) {
                    setResult(RESULT_OK);
                    if(sendAuthentication){
                        onPinCodeSuccess(6, this);
                    }else {
                        onPinCodeSuccess(4, this);
                    }
                    finish();
                } else {
                    onPinCodeError();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Override {@link #onBackPressed()} to prevent user for finishing the activity
     */
    @Override
    public void onBackPressed() {
        if (getBackableTypes().contains(mType)) {
            if (AppLock.UNLOCK_PIN == getType()) {
                mLockManager.getAppLock().setPinChallengeCancelled(true);
                LocalBroadcastManager
                        .getInstance(this)
                        .sendBroadcast(new Intent().setAction(ACTION_CANCEL));
            }
        }
        super.onBackPressed();
    }

    @Override
    public void onAuthenticated() {
        Log.e(TAG, "Fingerprint READ!!!");
        setResult(RESULT_OK);
        onPinCodeSuccess(5, this);
        finish();
    }

    @Override
    public void onError() {
        Log.e(TAG, "Fingerprint READ ERROR!!!");
    }

    /**
     * Gets the list of {@link AppLock} types that are acceptable to be backed out of using
     * the device's back button
     *
     * @return an {@link List<Integer>} of {@link AppLock} types which are backable
     */
    public List<Integer> getBackableTypes() {
        return Arrays.asList(AppLock.CHANGE_PIN, AppLock.DISABLE_PINLOCK,AppLock.ENABLE_PINLOCK,AppLock.UNLOCK_PIN);
    }

    /**
     * Displays the information dialog when the user clicks the
     * {@link #mForgotTextView}
     */
    public abstract void showForgotDialog();

    /**
     * Run a shake animation when the password is not valid.
     */
    protected void onPinCodeError() {
        onPinFailure(mAttempts++);
        Thread thread = new Thread() {
            public void run() {
                Log.d("AppLock","step onPinCodeError()");
                mPinCodeRoundView.refresh(mPinCode.length());
                mPinCode = "";
                Animation animation = AnimationUtils.loadAnimation(
                        AppLockActivityDuplicate.this, R.anim.shake);
                mKeyboardView.startAnimation(animation);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mPinCodeRoundView.refresh(mPinCode.length());
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        };
        runOnUiThread(thread);
    }

    protected void onPinCodeSuccess(int pinLockStatus, AppLockActivityDuplicate appLockActivity) {
        //ENABLE_PINLOCK = 0 DISABLE_PINLOCK = 1 CHANGE_PIN = 2 CONFIRM_PIN = 3, UNLOCK_PIN = 4, FINGERPRINT_UNLOCK = 5 SEND_AUTHENTICATION = 6 CHANGE_PIN pop up confirmation = 7
        onPinSuccess(mAttempts,pinLockStatus,appLockActivity);
        mAttempts = 1;
    }

    /**
     * Set the pincode and refreshes the {@link io.beldex.bchat.wallet.utils.pincodeview.PinCodeRoundView}
     */
    public void setPinCode(String pinCode) {
        mPinCode = pinCode;
       /* if (mPinCode.length() == this.getPinLength()) {
            Log.d("AppLock","step 8 -> "+mPinCode.length()+" getPinLength() -> "+this.getPinLength());
            onPinCodeInputed();
        }*/
        Log.d("AppLock","step setPincode()");
        mPinCodeRoundView.refresh(mPinCode.length());
    }


    /**
     * Returns the type of this {@link io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity}
     */
    public int getType() {
        return mType;
    }

    /**
     * When we click on the {@link #mForgotTextView} handle the pop-up
     * dialog
     *
     * @param view {@link #mForgotTextView}
     */
    @Override
    public void onClick(View view) {
        showForgotDialog();
    }

    /**
     * When the user has failed a pin challenge
     *
     * @param attempts the number of attempts the user has used
     */
    public abstract void onPinFailure(int attempts);

    /**
     * When the user has succeeded at a pin challenge
     *
     * @param attempts the number of attempts the user had used
     * @param appLockActivity
     */
    public abstract void onPinSuccess(int attempts, int pinLockStatus, AppLockActivityDuplicate appLockActivity);

    /**
     * Gets the resource id to the {@link View} to be set with {@link #setContentView(int)}.
     * The custom layout must include the following:
     * - {@link TextView} with an id of pin_code_step_textview
     * - {@link TextView} with an id of pin_code_forgot_textview
     * - {@link PinCodeRoundView} with an id of pin_code_round_view
     * - {@link KeyboardView} with an id of pin_code_keyboard_view
     *
     * @return the resource id to the {@link View}
     */
    public int getContentView() {
        return R.layout.activity_pin_code;
    }

    /**
     * Gets the number of digits in the pin code.  Subclasses can override this to change the
     * length of the pin.
     *
     * @return the number of digits in the PIN
     */
    public int getPinLength() {
        return DEFAULT_PIN_LENGTH;
    }

    /**
     * Get the current class extending {@link AppLockActivity} to re-enable {@link AppLock}
     * in case it has been collected
     *
     * @return the current class extending {@link AppLockActivity}
     */
    public Class<? extends AppLockActivityDuplicate> getCustomAppLockActivityClass() {
        return this.getClass();
    }
}

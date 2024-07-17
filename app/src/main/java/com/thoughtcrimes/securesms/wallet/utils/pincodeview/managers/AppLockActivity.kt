package com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers

import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.isScreenSecurityEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setWalletEntryPassword
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.ui.ScreenContainer
import com.thoughtcrimes.securesms.onboarding.ui.EXTRA_PIN_CODE_ACTION
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeAction
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeEvents
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeScreen
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeState
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeSteps
import com.thoughtcrimes.securesms.onboarding.ui.PinCodeViewModel
import com.thoughtcrimes.securesms.wallet.utils.keyboardview.KeyboardView
import com.thoughtcrimes.securesms.wallet.utils.keyboardview.enums.KeyboardButtonEnum
import com.thoughtcrimes.securesms.wallet.utils.keyboardview.interfaces.KeyboardButtonClickedListener
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.PinActivity
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.PinCodeRoundView
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.FingerprintUiHelper.FingerprintUiHelperBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Arrays

/**
 * The activity that appears when the password needs to be set or has to be asked.
 * Call this activity in normal or singleTop mode (not singleTask or singleInstance, it does not work
 * with [android.app.Activity.startActivityForResult]).
 */
@AndroidEntryPoint
abstract class AppLockActivity : PinActivity(), KeyboardButtonClickedListener, View.OnClickListener,
    FingerprintUiHelper.Callback {
    protected var mStepTextView: TextView? = null
    protected var mForgotTextView: TextView? = null
    protected var mPinCodeRoundView: PinCodeRoundView? = null
    protected var mKeyboardView: KeyboardView? = null
    protected var mFingerprintImageView: ImageView? = null
    protected var mFingerprintTextView: TextView? = null

    protected var mLockManager: LockManager<*>? = null


    protected var mFingerprintManager: FingerprintManager? = null
    protected var mFingerprintUiHelper: FingerprintUiHelper? = null

    /**
     * Returns the type of this [com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLockActivity]
     */
    var type: Int = AppLock.UNLOCK_PIN
        protected set
    protected var mAttempts: Int = 1
    protected var mPinCode: String? = null

    //Steve Josephh
    protected var oldPinCode: String? = null

    protected var mOldPinCode: String? = null

    protected var changePin: Boolean = false
    protected var sendAuthentication: Boolean = false
    private var pinCodeAction = PinCodeAction.VerifyWalletPin .action

    /**
     * First creation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setContentView(contentView)
//        initializeToolbar()
        initLayout(intent)
        if (isScreenSecurityEnabled(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            BChatTheme {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        val context = LocalContext.current
                        val viewModel: PinCodeViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsState()
                        val handleError: (String) -> Unit = { error ->
                            showErrorMessage(error)
                            viewModel.onEvent(PinCodeEvents.ResetPinCode)
                        }
                        LaunchedEffect(key1 = true) {
                            launch {
                                viewModel.errorMessage.collectLatest { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        ScreenContainer(
                            title = when (state.step) {
                                PinCodeSteps.EnterPin -> {
                                    stringResource(R.string.create_pin)
                                }
                                PinCodeSteps.OldPin,
                                PinCodeSteps.VerifyPin -> {
                                    stringResource(R.string.verify_pin)
                                }
                                PinCodeSteps.ReEnterPin -> {
                                    stringResource(R.string.create_pin)
                                }
                            },
                            onBackClick = {
                                onBackPressed()
                            },
                            modifier = Modifier.padding(it)
                        ) {
                            PinCodeScreen(
                                state = state,
                                onEvent = { event ->
                                    when (event) {
                                        PinCodeEvents.Submit -> {
                                            handlePinCodeSetUp(
                                                state = state,
                                                handlePinCodeInput = {
                                                    viewModel.handleWalletPinActions()
                                                },
                                                onPinCodeError = { error ->
                                                    handleError(error ?: "")
                                                }
                                            )
                                        }
                                        is PinCodeEvents.PinCodeChanged -> {
                                            viewModel.onEvent(event)
                                            when (pinCodeAction) {
                                                PinCodeAction.VerifyWalletPin.action -> {
                                                    if (event.pinCode.length == 4) {
                                                        if (mLockManager!!.appLock.checkPasscode(event.pinCode)) {
                                                            setResult(RESULT_OK)
                                                            if (sendAuthentication) {
                                                                onPinCodeSuccess(6, this@AppLockActivity)
                                                            } else {
                                                                onPinCodeSuccess(4, this@AppLockActivity)
                                                            }
                                                            finish()
                                                        } else {
                                                            handleError(context.resources.getString(R.string.incorrect_pin))
                                                        }
                                                    }
                                                }
                                                else -> Unit
                                            }
                                        }
                                        else -> viewModel.onEvent(event)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showErrorMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun handlePinCodeSetUp(
        state: PinCodeState,
        handlePinCodeInput: () -> Unit,
        onPinCodeError: (String?) -> Unit
    ) {
        when (pinCodeAction) {
            PinCodeAction.CreateWalletPin.action -> {
                when (state.step) {
                    PinCodeSteps.EnterPin -> {
                        handlePinCodeInput()
                    }
                    PinCodeSteps.ReEnterPin -> {
                        if (state.newPin == state.reEnteredPin) {
                            setResult(RESULT_OK)
                            setWalletEntryPassword(this, state.newPin)
                            mLockManager!!.appLock.setPasscode(state.newPin)
                            onPinCodeSuccess(3, this)
                        } else {
                            onPinCodeError(getString(R.string.pin_mismatch))
                        }
                    }
                    else -> Unit
                }
            }
            PinCodeAction.ChangeWalletPin.action -> {
                when (state.step) {
                    PinCodeSteps.EnterPin -> {
                        handlePinCodeInput()
                    }
                    PinCodeSteps.OldPin -> {
                        if (mLockManager!!.appLock.checkPasscode(state.pin)) {
                            handlePinCodeInput()
                            onPinCodeSuccess(2, this)
                        } else {
                            onPinCodeError(getString(R.string.incorrect_pin))
                        }
                    }
                    PinCodeSteps.ReEnterPin -> {
                        if (state.newPin == state.reEnteredPin) {
                            setResult(RESULT_OK)
                            setWalletEntryPassword(this, state.newPin)
                            mLockManager!!.appLock.setPasscode(state.newPin)
                            onPinCodeSuccess(7, this)
                        } else {
                            onPinCodeError(getString(R.string.pin_mismatch))
                        }
                    }
                    else -> Unit
                }
            }
            else -> {
                Unit
            }
        }
    }

    private fun initializeToolbar() {
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
//    actionBar.setHomeButtonEnabled(false);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
        actionBar.setTitle("Wallet Password");*/
        val actionbar = supportActionBar
        actionbar!!.setDisplayShowHomeEnabled(false)
        actionbar.setDisplayShowTitleEnabled(false)
        actionbar.setDisplayHomeAsUpEnabled(false)
        actionbar.setHomeButtonEnabled(false)

        actionbar.setCustomView(R.layout.bchat_logo_action_bar_content)
        actionbar.setDisplayShowCustomEnabled(true)

        val rootView = actionbar.customView.parent as Toolbar
        rootView.setPadding(0, 0, 0, 0)
        rootView.setContentInsetsAbsolute(0, 0)

        val backButton = actionbar.customView.findViewById<View>(R.id.back_button)
        val titleName = actionbar.customView.findViewById<TextView>(R.id.title_name)
        titleName.text = getString(R.string.activity_wallet_password_page_title)
        backButton.setOnClickListener { view: View? ->
            onBackPressed()
        }
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
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        initLayout(intent)
    }

    override fun onResume() {
        super.onResume()
        /* initializeToolbar();*/
        //Init layout for Fingerprint
        //initLayoutForFingerprint();
    }

    override fun onPause() {
        super.onPause()
        if (mFingerprintUiHelper != null) {
            mFingerprintUiHelper!!.stopListening()
        }
    }

    /**
     * Init completely the layout, depending of the extra [com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLock.EXTRA_TYPE]
     */
    private fun initLayout(intent: Intent) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            //Animate if greater than 2.3.3
            overridePendingTransition(R.anim.nothing, R.anim.nothing)
        }

        val extras = intent.extras
        if (extras != null) {
//            type = extras.getInt(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN)
//            changePin = extras.getBoolean("change_pin", false)
            pinCodeAction = extras.getInt(EXTRA_PIN_CODE_ACTION) ?: PinCodeAction.VerifyWalletPin.action
            sendAuthentication = extras.getBoolean("send_authentication", false)
        }

        mLockManager = LockManager.getInstance()
        mPinCode = ""
        mOldPinCode = ""
        oldPinCode = ""

        enableAppLockerIfDoesNotExist()
        mLockManager?.getAppLock()?.setPinChallengeCancelled(false)

//        mStepTextView = findViewById<View>(R.id.pin_code_step_textview) as TextView
//        mPinCodeRoundView = findViewById<View>(R.id.pin_code_round_view) as PinCodeRoundView
//        mPinCodeRoundView!!.setPinLength(pinLength)
//        mForgotTextView = findViewById<View>(R.id.pin_code_forgot_textview) as TextView
//        mForgotTextView!!.setOnClickListener(this)
//        mKeyboardView = findViewById<View>(R.id.pin_code_keyboard_view) as KeyboardView
//        mKeyboardView!!.setKeyboardButtonClickedListener(this)
//
//        /* int logoId = mLockManager.getAppLock().getLogoId();
//        ImageView logoImage = ((ImageView) findViewById(R.id.pin_code_logo_imageview));
//        if (logoId != AppLock.LOGO_ID_NONE) {
//            logoImage.setVisibility(View.VISIBLE);
//            logoImage.setImageResource(logoId);
//        }*/
//        /*mForgotTextView.setText(getForgotText());
//        mForgotTextView.setVisibility(mLockManager.getAppLock().shouldShowForgot() ? View.VISIBLE : View.GONE);*/
//        Log.d("AppLockActivity 1", type.toString())
//        if (changePin) {
//            Log.d("AppLockActivity 2", type.toString())
//            mStepTextView!!.text = getStepText(AppLock.CHANGE_PIN)
//        } else {
//            Log.d("AppLockActivity 3", type.toString())
//            setStepText()
//        }
    }

    /**
     * Init [FingerprintManager] of the [android.os.Build.VERSION.SDK_INT] is > to Marshmallow
     * and [FingerprintManager.isHardwareDetected].
     */
    private fun initLayoutForFingerprint() {
        mFingerprintImageView = findViewById<View>(R.id.pin_code_fingerprint_imageview) as ImageView
        mFingerprintTextView = findViewById<View>(R.id.pin_code_fingerprint_textview) as TextView
        if (type == AppLock.UNLOCK_PIN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFingerprintManager = getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
            mFingerprintUiHelper = FingerprintUiHelperBuilder(mFingerprintManager).build(
                mFingerprintImageView,
                mFingerprintTextView,
                this
            )
            try {
                if (mFingerprintManager!!.isHardwareDetected && mFingerprintUiHelper?.isFingerprintAuthAvailable() == true) {
                    //SteveJosephh21
                    /*mFingerprintImageView.setVisibility(View.VISIBLE);
                    mFingerprintTextView.setVisibility(View.VISIBLE);*/
                    mFingerprintUiHelper?.startListening()
                } else {
                    mFingerprintImageView!!.visibility = View.GONE
                    mFingerprintTextView!!.visibility = View.GONE
                }
            } catch (e: SecurityException) {
                Log.e(TAG, e.toString())
                mFingerprintImageView!!.visibility = View.GONE
                mFingerprintTextView!!.visibility = View.GONE
            }
        } else {
            mFingerprintImageView!!.visibility = View.GONE
            mFingerprintTextView!!.visibility = View.GONE
        }
    }

    /**
     * Re enable [AppLock] if it has been collected to avoid
     * [NullPointerException].
     */
    private fun enableAppLockerIfDoesNotExist() {
        try {
            if (mLockManager?.appLock == null) {
                mLockManager?.enableAppLock(this, customAppLockActivityClass)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Init the [.mStepTextView] based on [.mType]
     */
    private fun setStepText() {
        mStepTextView!!.text = getStepText(type)
    }

    /**
     * Gets the [String] to be used in the [.mStepTextView] based on [.mType]
     *
     * @param reason The [.mType] to return a [String] for
     * @return The [String] for the [AppLockActivity]
     */
    fun getStepText(reason: Int): String? {
        var msg: String? = null
        when (reason) {
            AppLock.DISABLE_PINLOCK -> msg = getString(
                R.string.pin_code_step_disable,
                getPinLength()
            )

            AppLock.ENABLE_PINLOCK -> msg = getString(R.string.pin_code_step_create)
            AppLock.CHANGE_PIN -> msg = getString(
                R.string.pin_code_step_change,
                getPinLength()
            )

            AppLock.UNLOCK_PIN -> msg = getString(R.string.pin_code_step_unlock)
            AppLock.CONFIRM_PIN -> msg = getString(R.string.pin_code_step_enable_confirm)
        }
        return msg
    }

    val forgotText: String
        get() = getString(R.string.pin_code_forgot_text)

    /**
     * Overrides to allow a slide_down animation when finishing
     */
    override fun finish() {
        super.finish()
        if (mLockManager != null) {
            val appLock = mLockManager!!.appLock
            appLock?.setLastActiveMillis()
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            //Animate if greater than 2.3.3
            overridePendingTransition(R.anim.nothing, R.anim.slide_down)
        }
    }

    /**
     * Add the button clicked to [.mPinCode] each time.
     * Refreshes also the [com.thoughtcrimes.securesms.wallet.utils.pincodeview.PinCodeRoundView]
     */
    override fun onKeyboardClick(keyboardButtonEnum: KeyboardButtonEnum) {
        if (mPinCode!!.length < getPinLength()) {
            val value = keyboardButtonEnum.buttonValue

            if (value == KeyboardButtonEnum.BUTTON_CLEAR.buttonValue) {
                if (!mPinCode!!.isEmpty()) {
                    setPinCode(mPinCode!!.substring(0, mPinCode!!.length - 1))
                } else {
                    setPinCode("")
                }
            } else {
                setPinCode(mPinCode + value)
            }
        }
    }

    /**
     * Called at the end of the animation of the [com.thoughtcrimes.securesms.wallet.utils.keyboardview.RippleView]
     * Calls [.onPinCodeInputed] when [.mPinCode]
     */
    override fun onRippleAnimationEnd() {
        if (mPinCode!!.length == getPinLength()) {
            onPinCodeInputed()
        }
    }

    /**
     * Switch over the [.mType] to determine if the password is ok, if we should pass to the next step etc...
     */
    protected fun onPinCodeInputed() {
        when (type) {
            AppLock.DISABLE_PINLOCK -> if (mLockManager!!.appLock.checkPasscode(mPinCode)) {
                setResult(RESULT_OK)
                mLockManager!!.appLock.setPasscode(null)
                onPinCodeSuccess(1, this)
                finish()
            } else {
                onPinCodeError()
            }

            AppLock.ENABLE_PINLOCK -> if (changePin) {
                if (oldPinCode == mPinCode) {
                    Toast.makeText(
                        this,
                        getString(R.string.change_pin_confirmation_message),
                        Toast.LENGTH_SHORT
                    ).show()
                    onPinCodeError()
                } else {
                    mOldPinCode = mPinCode
                    setPinCode("")
                    type = AppLock.CONFIRM_PIN
                    setStepText()
                }
            } else {
                mOldPinCode = mPinCode
                setPinCode("")
                type = AppLock.CONFIRM_PIN
                setStepText()
            }

            AppLock.CONFIRM_PIN -> if (mPinCode == mOldPinCode) {
                setResult(RESULT_OK)
                setWalletEntryPassword(this, mPinCode)
                mLockManager!!.appLock.setPasscode(mPinCode)
                if (changePin) {
                    onPinCodeSuccess(7, this)
                } else {
                    onPinCodeSuccess(3, this)
                }
            } else {
                mOldPinCode = ""
                setPinCode("")
                type = AppLock.ENABLE_PINLOCK
                setStepText()
                onPinCodeError()
            }

            AppLock.CHANGE_PIN -> if (mLockManager!!.appLock.checkPasscode(mPinCode)) {
                oldPinCode = mPinCode
                type = AppLock.ENABLE_PINLOCK
                setStepText()
                setPinCode("")
                onPinCodeSuccess(2, this)
            } else {
                onPinCodeError()
            }

            AppLock.UNLOCK_PIN -> if (mLockManager!!.appLock.checkPasscode(mPinCode)) {
                setResult(RESULT_OK)
                if (sendAuthentication) {
                    onPinCodeSuccess(6, this)
                } else {
                    onPinCodeSuccess(4, this)
                }
                finish()
            } else {
                onPinCodeError()
            }

            else -> {}
        }
    }

    /**
     * Override [.onBackPressed] to prevent user for finishing the activity
     */
    override fun onBackPressed() {
        if (backableTypes.contains(type)) {
            if (AppLock.UNLOCK_PIN == type) {
                mLockManager!!.appLock.setPinChallengeCancelled(true)
                LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(Intent().setAction(ACTION_CANCEL))
            }
        }
        super.onBackPressed()
    }

    override fun onAuthenticated() {
        Log.e(TAG, "Fingerprint READ!!!")
        setResult(RESULT_OK)
        onPinCodeSuccess(5, this)
        finish()
    }

    override fun onError() {
        Log.e(TAG, "Fingerprint READ ERROR!!!")
    }

    val backableTypes: List<Int>
        /**
         * Gets the list of [AppLock] types that are acceptable to be backed out of using
         * the device's back button
         *
         * @return an [<] of [AppLock] types which are backable
         */
        get() = Arrays.asList(
            AppLock.CHANGE_PIN,
            AppLock.DISABLE_PINLOCK,
            AppLock.ENABLE_PINLOCK,
            AppLock.UNLOCK_PIN
        )

    /**
     * Displays the information dialog when the user clicks the
     * [.mForgotTextView]
     */
    abstract fun showForgotDialog()

    /**
     * Run a shake animation when the password is not valid.
     */
    protected fun onPinCodeError() {
        onPinFailure(mAttempts++)
        val thread: Thread = object : Thread() {
            override fun run() {
                Log.d("AppLock", "step onPinCodeError()")
                mPinCodeRoundView!!.refresh(mPinCode!!.length)
                mPinCode = ""
                val animation = AnimationUtils.loadAnimation(
                    this@AppLockActivity, R.anim.shake
                )
                mKeyboardView!!.startAnimation(animation)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        mPinCodeRoundView!!.refresh(mPinCode!!.length)
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                    }
                })
            }
        }
        runOnUiThread(thread)
    }

    protected fun onPinCodeSuccess(pinLockStatus: Int, appLockActivity: AppLockActivity?) {
        //ENABLE_PINLOCK = 0 DISABLE_PINLOCK = 1 CHANGE_PIN = 2 CONFIRM_PIN = 3, UNLOCK_PIN = 4, FINGERPRINT_UNLOCK = 5 SEND_AUTHENTICATION = 6 CHANGE_PIN pop up confirmation = 7
        onPinSuccess(mAttempts, pinLockStatus, appLockActivity)
        mAttempts = 1
    }

    /**
     * Set the pincode and refreshes the [com.thoughtcrimes.securesms.wallet.utils.pincodeview.PinCodeRoundView]
     */
    fun setPinCode(pinCode: String?) {
        mPinCode = pinCode
        /* if (mPinCode.length() == this.getPinLength()) {
            Log.d("AppLock","step 8 -> "+mPinCode.length()+" getPinLength() -> "+this.getPinLength());
            onPinCodeInputed();
        }*/
        Log.d("AppLock", "step setPincode()")
        mPinCodeRoundView!!.refresh(mPinCode!!.length)
    }


    /**
     * When we click on the [.mForgotTextView] handle the pop-up
     * dialog
     *
     * @param view [.mForgotTextView]
     */
    override fun onClick(view: View) {
        showForgotDialog()
    }

    /**
     * When the user has failed a pin challenge
     *
     * @param attempts the number of attempts the user has used
     */
    abstract fun onPinFailure(attempts: Int)

    /**
     * When the user has succeeded at a pin challenge
     *
     * @param attempts the number of attempts the user had used
     * @param appLockActivity
     */
    abstract fun onPinSuccess(attempts: Int, pinLockStatus: Int, appLockActivity: AppLockActivity?)

    val contentView: Int
        /**
         * Gets the resource id to the [View] to be set with [.setContentView].
         * The custom layout must include the following:
         * - [TextView] with an id of pin_code_step_textview
         * - [TextView] with an id of pin_code_forgot_textview
         * - [PinCodeRoundView] with an id of pin_code_round_view
         * - [KeyboardView] with an id of pin_code_keyboard_view
         *
         * @return the resource id to the [View]
         */
        get() = R.layout.activity_pin_code

    val customAppLockActivityClass: Class<out AppLockActivity>
        /**
         * Get the current class extending [AppLockActivity] to re-enable [AppLock]
         * in case it has been collected
         *
         * @return the current class extending [AppLockActivity]
         */
        get() = this::class.java

    open fun getPinLength(): Int {
        return DEFAULT_PIN_LENGTH
    }

    companion object {
        @JvmField
        val TAG: String = AppLockActivity::class.java.simpleName

        @JvmField
        val ACTION_CANCEL: String = TAG + ".actionCancelled"

        const val DEFAULT_PIN_LENGTH: Int = 4

        const val EXTRA_ACTION = "action"
    }
}

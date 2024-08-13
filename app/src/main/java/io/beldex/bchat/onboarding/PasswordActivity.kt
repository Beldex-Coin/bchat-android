package io.beldex.bchat.onboarding

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import io.beldex.bchat.databinding.ActivityPasswordBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.isScreenLockEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setScreenLockEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setScreenLockTimeout

import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.keyboard.CustomKeyboardView
import io.beldex.bchat.service.KeyCachingService.KeySetBinder
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.R


class PasswordActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityPasswordBinding
    private var keyCachingService: KeyCachingService? = null
    private var failure = false
    private val authenticated = false
    private val TAG = PasswordActivity::class.java.simpleName
    private lateinit var keyboard: CustomKeyboardView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Password")

        //  Start and bind to the KeyCachingService instance.
        val bindIntent = Intent(this, KeyCachingService::class.java)
        startService(bindIntent)
        bindService(bindIntent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                keyCachingService = (service as KeySetBinder).service
            }

            override fun onServiceDisconnected(name: ComponentName) {
                keyCachingService!!.setMasterSecret(Any())
                keyCachingService = null
            }
        }, BIND_AUTO_CREATE)

        with(binding) {
            userPinEditTxt.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validatePassword(s.toString(),false)
                }
            })

            binding.userPinEditTxt.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            val ic: InputConnection = binding.userPinEditTxt.onCreateInputConnection(EditorInfo())
            binding.keyboard1?.setInputConnection(ic)

            binding.userPinEditTxt.setOnTouchListener { _: View, event: MotionEvent ->
                binding.userPinEditTxt.onTouchEvent(event) // call native handler

                true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // API 21
                binding.userPinEditTxt.showSoftInputOnFocus = false
            } else { // API 11-20
                binding.userPinEditTxt.setTextIsSelectable(true)
            }

            binding.keyboard1.buttonEnter!!.setOnClickListener() {
                validatePassword(userPinEditTxt.text.toString(),true)
            }
        }
    }

    private fun validatePassword(pin: String, validation: Boolean) {
        val userPassword = TextSecurePreferences.getMyPassword(this@PasswordActivity)
        when {
            pin.isEmpty() -> {
                binding.userPinEditTxtLayout.isErrorEnabled=false
                if(validation) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_enter_your_four_digit_pin),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            pin.length<4 -> {
                if(validation) {
                    binding.userPinEditTxtLayout.isErrorEnabled=true
                    binding.userPinEditTxtLayout.error = getString(R.string.invalid_password)
                }else{
                    binding.userPinEditTxtLayout.isErrorEnabled=false
                }
            }
            userPassword != pin -> {
                binding.userPinEditTxtLayout.isErrorEnabled=true
                binding.userPinEditTxtLayout.error = getString(R.string.invalid_password)
            }
            userPassword == pin -> {
                validateSuccess()
            }
            else -> {}
        }
        if(pin.isEmpty()){
            binding.userPinEditTxtLayout.isErrorEnabled=false
            if(validation) {
                Toast.makeText(
                    this,
                    getString(R.string.please_enter_your_four_digit_pin),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        else if(pin.length<4){
            if(validation) {
                binding.userPinEditTxtLayout.isErrorEnabled=true
                binding.userPinEditTxtLayout.error = getString(R.string.invalid_password)
            }else{
                binding.userPinEditTxtLayout.isErrorEnabled=false
            }
        }else if (userPassword != pin) {
            binding.userPinEditTxtLayout.isErrorEnabled=true
            binding.userPinEditTxtLayout.error = getString(R.string.invalid_password)
        }else if (userPassword == pin) {
            validateSuccess()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isScreenLockEnabled(this) && !authenticated && !failure) {
            resumeScreenLock()
        }
        failure = false
    }

    private fun handleAuthenticated() {

        //TODO Replace with a proper call.
        if (keyCachingService != null) {
            keyCachingService!!.setMasterSecret(Any())
        }

        // Finish and proceed with the next intent.
        val nextIntent = intent.getParcelableExtra<Intent>("next_intent")
        nextIntent?.let { startActivity(it) }
        finish()
    }

    private fun resumeScreenLock() {
        val keyguardManager = (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
        if (!keyguardManager.isKeyguardSecure) {
            setScreenLockEnabled(applicationContext, false)
            Log.d("Problem","Yes")
            setScreenLockTimeout(applicationContext, 0)
            handleAuthenticated()
            return
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultcode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultcode, data)
        if (requestCode != 1) return
        if (resultcode == RESULT_OK) {
            handleAuthenticated()
        } else {
            Log.w("PasswordActivity", "Authentication failed")
            failure = true
        }
    }

    private fun validateSuccess() {
        // TextSecurePreferences.setScreenLockEnabled(this,false)
        TextSecurePreferences.setHasSeenWelcomeScreen(this, true)
        resumeScreenLock()
        handleAuthenticated()
        val intent = Intent(this, HomeActivity::class.java)
        push(intent)
    }

    override fun onBackPressed() {
            super.onBackPressed()
        //New Line
        finish()
    }
}
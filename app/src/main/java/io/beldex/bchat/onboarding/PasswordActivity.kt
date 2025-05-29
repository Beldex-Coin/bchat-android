package io.beldex.bchat.onboarding

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.setContent
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
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.isScreenLockEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setScreenLockEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setScreenLockTimeout
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ui.ScreenContainer
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.keyboard.CustomKeyboardView
import io.beldex.bchat.onboarding.ui.PinCodeScreen
import io.beldex.bchat.onboarding.ui.PinCodeViewModel
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.service.KeyCachingService.KeySetBinder
import io.beldex.bchat.util.push
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityPasswordBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PasswordActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityPasswordBinding
    private var keyCachingService: KeyCachingService? = null
    private var failure = false
    private val authenticated = false
    private val TAG = PasswordActivity::class.java.simpleName
    private lateinit var keyboard: CustomKeyboardView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityPasswordBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        setUpActionBarBchatLogo("Password")
        setContent {
            BChatTheme {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        val context = LocalContext.current
                        val viewModel: PinCodeViewModel = hiltViewModel()
                        val state by viewModel.state.collectAsState()
                        LaunchedEffect(key1 = true) {
                            launch {
                                viewModel.errorMessage.collectLatest { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            launch {
                                viewModel.successEvent.collectLatest { success ->
                                    if (success) {
                                        validateSuccess()
                                    }else{
                                        validateSuccess()
                                    }
                                }
                            }
                        }
                        ScreenContainer(
                            title = stringResource(R.string.verify_password),
                            onBackClick = {
                                onBackPressed()
                            },
                            modifier = Modifier.padding(it)
                        ) {
                            PinCodeScreen(
                                state = state,
                                onEvent = viewModel::onEvent
                            )
                        }
                    }
                }
            }
        }

        //  Start and bind to the KeyCachingService instance.
        val bindIntent = Intent(this, KeyCachingService::class.java)
        try {
            this.startService(bindIntent)
        } catch (e: Exception) {
            Log.d("Beldex", "Unable to start KeyCachingService intent: ", e)
        }
        bindService(bindIntent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                keyCachingService = (service as KeySetBinder).service
            }

            override fun onServiceDisconnected(name: ComponentName) {
                keyCachingService!!.setMasterSecret(Any())
                keyCachingService = null
            }
        }, BIND_AUTO_CREATE)

//        with(binding) {
//            userPinEditTxt.addTextChangedListener(object : TextWatcher {
//                override fun afterTextChanged(s: Editable?) {
//                }
//
//                override fun beforeTextChanged(
//                    s: CharSequence?,
//                    start: Int,
//                    count: Int,
//                    after: Int
//                ) {
//                }
//
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                    validatePassword(s.toString(),false)
//                }
//            })
//
//            binding.userPinEditTxt.requestFocus()
//            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
//            val ic: InputConnection = binding.userPinEditTxt.onCreateInputConnection(EditorInfo())
//            binding.keyboard1?.setInputConnection(ic)
//
//            binding.userPinEditTxt.setOnTouchListener { _: View, event: MotionEvent ->
//                binding.userPinEditTxt.onTouchEvent(event) // call native handler
//
//                true
//            }
//
//            binding.userPinEditTxt.showSoftInputOnFocus = false
//
//            binding.keyboard1.buttonEnter!!.setOnClickListener() {
//                validatePassword(userPinEditTxt.text.toString(),true)
//            }
//        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    @Deprecated("Deprecated in Java")
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
            super.onBackPressed()
        //New Line
        finish()
    }
}
package io.beldex.bchat.onboarding

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import io.beldex.bchat.databinding.ActivityCreatePasswordBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.keyboard.CustomKeyboardView
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import javax.inject.Inject
import android.view.inputmethod.InputConnection

import android.R
import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.view.MotionEvent
import android.view.View.OnTouchListener
import androidx.core.widget.addTextChangedListener
import android.text.Editable

import android.text.TextWatcher
import org.w3c.dom.Text


class CreatePasswordActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityCreatePasswordBinding

    @Inject
    lateinit var textSecurePreferences: TextSecurePreferences
    private var callPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Create Password", true)
        binding = ActivityCreatePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        callPage = intent.extras!!.getInt("callPage")
        with(binding)
        {
            keyboard1?.buttonEnter!!.setOnClickListener() {
                val enteredPIN = enterPinEditTxt.text.toString()
                val reEnterPIN = reEnterPinEditTxt.text.toString()
                TextSecurePreferences.setMyPassword(this@CreatePasswordActivity, enteredPIN)
                //-Log.d("Beldex", "my password --> Create $enteredPIN")

                if (enteredPIN.isEmpty()) {
                    enterPinEditTxtLayout!!.isErrorEnabled = false
                    Toast.makeText(
                        this@CreatePasswordActivity,
                        "Must set your 4 digit PIN.",
                        Toast.LENGTH_LONG
                    ).show()
                    //enterPinEditTxtLayout!!.error = "Must set your 4 digit PIN."
                } else if (enteredPIN.length < 4) {
                    enterPinEditTxtLayout!!.isErrorEnabled = true
                    enterPinEditTxtLayout.error = "Please enter 4 digit PIN."
                } else if (reEnterPIN.isEmpty()) {
                    enterPinEditTxtLayout!!.isErrorEnabled = false
                    reEnterPinEditTxtLayout!!.isErrorEnabled = false
                    Toast.makeText(
                        this@CreatePasswordActivity,
                        "Must set your 4 digit PIN.",
                        Toast.LENGTH_LONG
                    ).show()
                    //reEnterPinEditTxtLayout!!.error = "Must set your 4 digit PIN."
                } else if (reEnterPIN.length < 4) {
                    enterPinEditTxtLayout!!.isErrorEnabled = false
                    reEnterPinEditTxtLayout!!.isErrorEnabled = true
                    reEnterPinEditTxtLayout.error = "Please enter 4 digit PIN."
                } else if (enteredPIN != reEnterPIN) {
                    reEnterPinEditTxtLayout!!.isErrorEnabled = true
                    reEnterPinEditTxtLayout!!.error = "Password is not matched"
                } else if (enteredPIN == reEnterPIN) {
                    callPage(callPage)
                }

            }

        }
        //Important
        /*  binding.customKeyboardView?.registerEditText(
              CustomKeyboardView.KeyboardType.NUMBER,
              binding.enterPinEditTxt
          )
          binding.customKeyboardView?.registerEditText(
              CustomKeyboardView.KeyboardType.NUMBER,
              binding.reEnterPinEditTxt
          )
          if(binding.enterPinEditTxt.requestFocus()) {
          }*/

        /*val keyboard: MyKeyboard = findViewById<View>(R.id.keyboard) as MyKeyboard
        editText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        editText.setTextIsSelectable(true)*/
        binding.enterPinEditTxt.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val ic: InputConnection = binding.enterPinEditTxt.onCreateInputConnection(EditorInfo())
        binding.keyboard?.setInputConnection(ic)
        val ic1: InputConnection = binding.reEnterPinEditTxt.onCreateInputConnection(EditorInfo())
        binding.keyboard1!!.setInputConnection(ic1)
        binding.enterPinEditTxt.setOnTouchListener { _: View, event: MotionEvent ->
            binding.keyboard1!!.visibility = View.GONE
            binding.keyboard!!.visibility = View.VISIBLE
            binding.enterPinEditTxt.onTouchEvent(event) // call native handler

            true
        }
        binding.reEnterPinEditTxt.setOnTouchListener { _: View, event: MotionEvent ->
            binding.keyboard!!.visibility = View.GONE
            binding.keyboard1!!.visibility = View.VISIBLE
            binding.reEnterPinEditTxt.onTouchEvent(event) // call native handler

            true
        }

        binding.keyboard?.buttonEnter?.setOnClickListener {
            binding.reEnterPinEditTxt.requestFocus()
            binding.keyboard!!.visibility = View.GONE
            binding.keyboard1!!.visibility = View.VISIBLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // API 21
            binding.enterPinEditTxt.showSoftInputOnFocus = false
            binding.reEnterPinEditTxt.showSoftInputOnFocus = false
        } else { // API 11-20
            binding.enterPinEditTxt.setTextIsSelectable(true)
            binding.reEnterPinEditTxt.setTextIsSelectable(true)
        }

        //New Line
        binding.enterPinEditTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (s.length == 4 ){
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.reEnterPinEditTxt.requestFocus()
                        binding.keyboard!!.visibility = View.GONE
                        binding.keyboard1!!.visibility = View.VISIBLE
                    },10)
                }
            }
        })
    }

    private fun callPage(callPage: Int) {
        if (callPage == 1) {
            callRecoveryPhrasePage()
        } else if (callPage == 2) {
            callHomePage()
        }
    }


    private fun callRecoveryPhrasePage() {
        //New Line AirDrop
        TextSecurePreferences.setAirdropAnimationStatus(this,true)

        TextSecurePreferences.setScreenLockEnabled(this, true)
        TextSecurePreferences.setScreenLockTimeout(this, 950400)
        //New Line
        TextSecurePreferences.setHasSeenWelcomeScreen(this, true)
        val intent1 = Intent(this, KeyCachingService::class.java)
        intent1.action = KeyCachingService.LOCK_TOGGLED_EVENT
        this.startService(intent1)
        val intent = Intent(this, RecoveryPhraseActivity::class.java)
        push(intent)
//        finish()
    }

    private fun callHomePage() {
        TextSecurePreferences.setCopiedSeed(this,true)
        //New Line AirDrop
        TextSecurePreferences.setAirdropAnimationStatus(this,true)

        TextSecurePreferences.setScreenLockEnabled(this, true)
        /*Hales63*/

        TextSecurePreferences.setScreenLockTimeout(this, 950400)
        TextSecurePreferences.setHasSeenWelcomeScreen(this, true)
        val intent1 = Intent(this, KeyCachingService::class.java)
        intent1.action = KeyCachingService.LOCK_TOGGLED_EVENT
        this.startService(intent1)
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        push(intent)
    }

    //Important
    /*override fun onBackPressed() {
        if (binding.customKeyboardView!!.isExpanded) {
            binding.customKeyboardView!!.translateLayout()
        } else {
            super.onBackPressed()
        }
    }*/
    override fun onBackPressed() {
            super.onBackPressed()
    }
}
package com.thoughtcrimes.securesms.onboarding

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityCheckPasswordBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.keyboard.CustomKeyboardView
import com.thoughtcrimes.securesms.keys.ShowKeysActivity
import com.thoughtcrimes.securesms.seed.ShowSeedActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class CheckPasswordActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityCheckPasswordBinding
    private lateinit var keyboard: CustomKeyboardView
    private var page: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Password")

        page = intent.extras!!.getInt("page")


        with(binding) {
            if (page == 1) {
                checkPasswordDescriptionTextView.text =
                    resources.getString(R.string.check_password_description_content_seed)
            } else {
                checkPasswordDescriptionTextView.text =
                    resources.getString(R.string.check_password_description_content_keys)
            }
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

            //Important
            /*continueButton.setOnClickListener() {
                validatePassword(userPinEditTxt.text.toString())
            }
            binding.customKeyboardView.registerEditText(
                CustomKeyboardView.KeyboardType.NUMBER,
                binding.userPinEditTxt
            )
            if (binding.userPinEditTxt.requestFocus()) {
                //keyboard.isExpanded=true
                //window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }*/
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

    private fun validatePassword(pin: String,validation:Boolean) {
        val userPassword = TextSecurePreferences.getMyPassword(this@CheckPasswordActivity)
        if (pin.isEmpty()) {
            binding.userPinEditTxtLayout.isErrorEnabled = false
            if(validation) {
                Toast.makeText(
                    this,
                    "Please enter your 4 digit PIN.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (pin.length < 4) {
            if(validation) {
                binding.userPinEditTxtLayout.isErrorEnabled=true
                binding.userPinEditTxtLayout.error = "Invalid Password."
            }else{
                binding.userPinEditTxtLayout.isErrorEnabled=false
            }
        } else if (userPassword != pin) {
            binding.userPinEditTxtLayout.isErrorEnabled = true
            binding.userPinEditTxtLayout.error = "Invalid Password."
        } else if (userPassword == pin) {
            validateSuccess(page)
        }
    }

    private fun validateSuccess(page: Int) {
        if (page == 1) {
            val intent = Intent(this, ShowSeedActivity::class.java)
            push(intent)
            finish()
        } else {
            val intent = Intent(this, ShowKeysActivity::class.java)
            push(intent)
            finish()
        }

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
package com.thoughtcrimes.securesms.applock

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import io.beldex.bchat.databinding.ActivityChangePasswordBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.keyboard.CustomKeyboardView
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class ChangePasswordActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Change Password")
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val oldPassword = TextSecurePreferences.getMyPassword(this)
        with(binding)
        {
            binding.keyboard1?.buttonEnter?.setOnClickListener()
            {
                val oldEnteredPassword = oldPasswordEditTxt.text.toString()
                val newEnteredPassword = newPasswordEditTxt.text.toString()

                when {
                    oldPassword != oldEnteredPassword -> {
                        oldPasswordEditTxtLayout.isErrorEnabled = true
                        oldPasswordEditTxtLayout.error = "Invalid old password"
                        newPasswordEditTxtLayout.isErrorEnabled = false
                    }
                    oldEnteredPassword.isEmpty() -> {
                        oldPasswordEditTxtLayout.isErrorEnabled = false
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Must set your Old PIN.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    oldEnteredPassword == newEnteredPassword -> {
                        if(oldPassword == oldEnteredPassword){
                            oldPasswordEditTxtLayout.isErrorEnabled = false
                        }
                        newPasswordEditTxtLayout.isErrorEnabled = true
                        newPasswordEditTxtLayout.error = "Both are Same"
                    }
                    newEnteredPassword.isEmpty() -> {
                        oldPasswordEditTxtLayout.isErrorEnabled = false
                        newPasswordEditTxtLayout.isErrorEnabled = false
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Must set your New PIN.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    newEnteredPassword.length < 4 -> {
                        oldPasswordEditTxtLayout.isErrorEnabled = false
                        newPasswordEditTxtLayout.isErrorEnabled = true
                        newPasswordEditTxtLayout.error = "Please enter 4 digit PIN."
                    }
                    oldEnteredPassword != newEnteredPassword -> {
                        TextSecurePreferences.setMyPassword(
                            this@ChangePasswordActivity,
                            newPasswordEditTxt.text.toString()
                        )
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Password Changed Successfully",
                            Toast.LENGTH_LONG
                        ).show()
                        oldPasswordEditTxt.text.clear()
                        newPasswordEditTxt.text.clear()
                        finish()
                    }
                }
            }
        }
        binding.oldPasswordEditTxt.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val ic: InputConnection = binding.oldPasswordEditTxt.onCreateInputConnection(EditorInfo())
        binding.keyboard?.setInputConnection(ic)
        val ic1: InputConnection = binding.newPasswordEditTxt.onCreateInputConnection(EditorInfo())
        binding.keyboard1!!.setInputConnection(ic1)
        binding.oldPasswordEditTxt.setOnTouchListener { _: View, event: MotionEvent ->
            binding.keyboard1!!.visibility = View.GONE
            binding.keyboard!!.visibility = View.VISIBLE
            binding.oldPasswordEditTxt.onTouchEvent(event) // call native handler

            true
        }
        binding.newPasswordEditTxt.setOnTouchListener { _: View, event: MotionEvent ->
            binding.keyboard!!.visibility = View.GONE
            binding.keyboard1!!.visibility = View.VISIBLE
            binding.newPasswordEditTxt.onTouchEvent(event) // call native handler

            true
        }

        binding.keyboard?.buttonEnter?.setOnClickListener {
            binding.newPasswordEditTxt.requestFocus()
            binding.keyboard!!.visibility = View.GONE
            binding.keyboard1!!.visibility = View.VISIBLE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // API 21
            binding.oldPasswordEditTxt.showSoftInputOnFocus = false
            binding.newPasswordEditTxt.showSoftInputOnFocus = false
        } else { // API 11-20
            binding.oldPasswordEditTxt.setTextIsSelectable(true)
            binding.newPasswordEditTxt.setTextIsSelectable(true)
        }

        //New Line
        binding.oldPasswordEditTxt.addTextChangedListener(object : TextWatcher {
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
                        binding.newPasswordEditTxt.requestFocus()
                        binding.keyboard!!.visibility = View.GONE
                        binding.keyboard1!!.visibility = View.VISIBLE
                    },10)
                }
            }
        })
    }
    override fun onBackPressed() {
        super.onBackPressed()
    }
}

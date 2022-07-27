package com.thoughtcrimes.securesms.onboarding

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.beldex.bchat.databinding.ActivitySplashScreenHomeActivtityBinding

class SplashScreenHomeActivtity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenHomeActivtityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySplashScreenHomeActivtityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, PasswordActivity::class.java))
            finish()
        }, 2000)
    }

}
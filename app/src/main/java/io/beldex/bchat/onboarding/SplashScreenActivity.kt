package io.beldex.bchat.onboarding

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.beldex.bchat.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private var nextPage: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("SplashScreenActivity-->","OK")
        nextPage = intent.extras?.getBoolean("nextPage")!!
        Handler(Looper.getMainLooper()).postDelayed({
            if (nextPage) {
                startActivity(Intent(this, PasswordActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, LandingActivity::class.java))
                finish()
            }
        }, 1000)
    }
}
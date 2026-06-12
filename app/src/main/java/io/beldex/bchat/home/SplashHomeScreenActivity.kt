package io.beldex.bchat.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.beldex.bchat.BaseAppCompatActivity
import io.beldex.bchat.databinding.ActivitySplashScreenBinding

class SplashHomeScreenActivity : BaseAppCompatActivity() {
        private lateinit var binding: ActivitySplashScreenBinding
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding= ActivitySplashScreenBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }, 5000)
        }
    }

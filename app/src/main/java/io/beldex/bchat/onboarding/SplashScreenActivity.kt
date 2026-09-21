package io.beldex.bchat.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import io.beldex.bchat.BaseAppCompatActivity
import io.beldex.bchat.onboarding.ui.PinCodeAction
import io.beldex.bchat.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : BaseAppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private var nextPage: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        nextPage = intent.getBooleanExtra("nextPage", false)
        if (nextPage) {
            startActivity(Intent(this, PasswordActivity::class.java).apply {
                putExtra("action", PinCodeAction.VerifyPinCode.action)
            })
            finish()
        } else {
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }
    }
}
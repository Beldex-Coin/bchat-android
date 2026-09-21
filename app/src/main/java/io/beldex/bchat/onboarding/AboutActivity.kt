package io.beldex.bchat.onboarding

import android.content.Context
import android.os.Bundle
import android.util.Log
import io.beldex.bchat.databinding.ActivityAboutBinding
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.util.setUpActionBarBchatLogo

class AboutActivity : BaseActionBarActivity() {
    private lateinit var binding:ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo(getString(R.string.activity_settings_app_lock_button_title),false)
        with(binding){
            aboutPageContent.text = this@AboutActivity.getString(R.string.about_content)
        }
    }
}
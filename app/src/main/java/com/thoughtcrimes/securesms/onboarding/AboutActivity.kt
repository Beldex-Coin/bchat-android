package com.thoughtcrimes.securesms.onboarding

import android.content.Context
import android.os.Bundle
import android.util.Log
import io.beldex.bchat.databinding.ActivityAboutBinding
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class AboutActivity : BaseActionBarActivity() {
    private lateinit var binding:ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("About",false)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding){
            aboutPageContent.text = loadFileContents(this@AboutActivity)
        }

    }

    public fun loadFileContents(context: Context,): String {
        val inputStream = context.assets.open("about.txt")
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        return String(buffer)
    }
}
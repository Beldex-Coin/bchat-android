package com.thoughtcrimes.securesms.wallet.rescan


import android.os.Bundle
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityReScanBinding

class ReScanActivity : PassphraseRequiredActionBarActivity() {
    private lateinit var binding: ActivityReScanBinding

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityReScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_rescan_page_title)


    }
}

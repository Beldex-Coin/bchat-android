package com.thoughtcrimes.securesms.wallet.receive


import android.os.Bundle
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityReceiveBinding


class ReceiveActivity : PassphraseRequiredActionBarActivity() {

    private lateinit var binding:ActivityReceiveBinding

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityReceiveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_receive_page_title)


    }


}
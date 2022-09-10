package com.thoughtcrimes.securesms.wallet.node


import android.os.Bundle
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityAddNodeBinding


class AddNodeActivity : PassphraseRequiredActionBarActivity () {
    private lateinit var binding: ActivityAddNodeBinding

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityAddNodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_new_node_page_title)


    }
}
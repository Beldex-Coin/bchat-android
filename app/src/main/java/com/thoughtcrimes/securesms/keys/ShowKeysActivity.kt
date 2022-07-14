package com.thoughtcrimes.securesms.keys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityShowKeysBinding
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo

class ShowKeysActivity : BaseActionBarActivity() {
    private lateinit var binding:ActivityShowKeysBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowKeysBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Keys")
        with(binding){
            showKeysViewKeyPublicText.text = IdentityKeyUtil.retrieve(this@ShowKeysActivity, IdentityKeyUtil.IDENTITY_W_PUBLIC_KEY_PREF) ?:""
            showKeysViewKeyPrivateText.text = IdentityKeyUtil.retrieve(this@ShowKeysActivity, IdentityKeyUtil.IDENTITY_W_PUBLIC_TWO_KEY_PREF) ?:""
            showKeysSpendKeyPublicText.text = IdentityKeyUtil.retrieve(this@ShowKeysActivity,
                IdentityKeyUtil.IDENTITY_W_PUBLIC_THREE_KEY_PREF)?:""
            showKeysSpendKeyPrivateText.text = IdentityKeyUtil.retrieve(this@ShowKeysActivity,
                IdentityKeyUtil.IDENTITY_W_PUBLIC_FOUR_KEY_PREF)?:""
            showKeysViewKeyPublicCopyButton.setOnClickListener {
                copyKeys(binding.showKeysViewKeyPublicText.text.toString(),"View Key (public)")
            }
            showKeysViewKeyPrivateCopyButton.setOnClickListener {
                copyKeys(binding.showKeysViewKeyPrivateText.text.toString(),"View Key (private)")
            }
            showKeysSpendKeyPublicCopyButton.setOnClickListener {
                copyKeys(binding.showKeysSpendKeyPublicText.text.toString(),"Spend Key (public)")
            }
            showKeysSpendKeyPrivateCopyButton.setOnClickListener {
                copyKeys(binding.showKeysSpendKeyPrivateText.text.toString(),"Spend Key (private)")
            }
        }
    }
    private fun copyKeys(keys: String, keyTitle: String) {
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(keyTitle, keys)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}
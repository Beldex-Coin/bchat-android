package io.beldex.bchat.wallet.startwallet

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setBooleanPreference
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.wallet.WalletSetupLoadingBar
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityStartWalletInfoBinding

class StartWalletInfo : PassphraseRequiredActionBarActivity() {

    private lateinit var binding: ActivityStartWalletInfoBinding
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityStartWalletInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.wallets)

        val contentFirst = resources.getString(R.string.wallet_setup_content_first)
        val contentSecond = resources.getString(R.string.wallet_setup_content_second)
        val contentFourth = resources.getString(R.string.wallet_setup_content_fourth)
        val spannableFirst = SpannableStringBuilder(contentFirst)
        val spannableSecond = SpannableStringBuilder(contentSecond)
        val spannableFourth = SpannableStringBuilder(contentFourth)
        val color = ResourcesCompat.getColor(this.resources, R.color.node_status, this.theme)
        spannableFirst.setSpan(ForegroundColorSpan(color), 7, 28, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableSecond.setSpan(ForegroundColorSpan(color), 5, 20, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableFourth.setSpan(ForegroundColorSpan(color), 73, 101, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val face = Typeface.createFromAsset(this.assets, "fonts/open_sans_bold.ttf")
        binding.contentFirst.text = spannableFirst
        binding.contentSecond.text = spannableSecond
        binding.contentFourth.text = spannableFourth
        binding.contentFirst.typeface = face
        binding.contentSecond.typeface = face
        binding.contentFourth.typeface = face


        binding.enableWallet.setTextColor(ContextCompat.getColor(this, R.color.disable_button_text_color))
        binding.enableWallet.background = ContextCompat.getDrawable(this, R.drawable.prominent_filled_button_medium_background_disable)
        binding.enableWallet.isEnabled = binding.checkEnableWallet.isChecked

        binding.checkEnableWallet.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.enableWallet.isEnabled = true
                binding.enableWallet.background = ContextCompat.getDrawable(this, R.drawable.prominent_filled_button_medium_background)
                binding.enableWallet.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                binding.enableWallet.isEnabled = false
                binding.enableWallet.setTextColor(ContextCompat.getColor(this, R.color.disable_button_text_color))
                binding.enableWallet.background = ContextCompat.getDrawable(this, R.drawable.prominent_filled_button_medium_background_disable)
            }
        }
        binding.enableWallet.setOnClickListener {
            setBooleanPreference(this, TextSecurePreferences.Companion.IS_WALLET_ACTIVE, true)
            showProgress()
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                hideProgress()
                restartHome()
            }, 2000)
        }
    }

    private fun restartHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showProgress() {
        WalletSetupLoadingBar().show(
                this.supportFragmentManager,
                "wallet_setup_progressbar_tag")
    }

    private fun hideProgress() {
        val fragment = this.supportFragmentManager.findFragmentByTag("wallet_setup_progressbar_tag") as WalletSetupLoadingBar
        if (fragment != null) {
            val dialogFragment = DialogFragment()
            try {
                dialogFragment.dismiss()
            } catch (ex: IllegalStateException) {
                Log.e("Beldex", "IllegalStateException $ex")
            }
        }
    }
}
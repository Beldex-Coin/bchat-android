package com.thoughtcrimes.securesms.wallet

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityWalletSetupLoadingBarBinding

class WalletSetupLoadingBar : BaseDialog() {
    private lateinit var binding: ActivityWalletSetupLoadingBarBinding
    override fun setContentView(builder: AlertDialog.Builder) {
        binding = ActivityWalletSetupLoadingBarBinding.inflate(LayoutInflater.from(requireContext()))
        binding.walletSetUpProgressText.text = fromHtml(getString(R.string.wallet_setup_loading_content))
        builder.setView(binding.root)
        isCancelable = false
    }

    private fun fromHtml(source: String?): Spanned? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(source)
        }
    }
}
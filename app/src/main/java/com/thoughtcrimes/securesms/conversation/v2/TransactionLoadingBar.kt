package com.thoughtcrimes.securesms.conversation.v2

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.ActivityTransactionLoadingBarBinding

class TransactionLoadingBar : BaseDialog() {
    private lateinit var binding: ActivityTransactionLoadingBarBinding

    override fun setContentView(builder: AlertDialog.Builder) {
        binding =
            ActivityTransactionLoadingBarBinding.inflate(LayoutInflater.from(requireContext()))
        binding.transactionProgressText.text = fromHtml("Please <b>don\'t close this window,</b> attend calls, or navigate to another app until the transaction gets initiated")
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
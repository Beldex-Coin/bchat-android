package com.thoughtcrimes.securesms.conversation.v2

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.ActivityTransactionLoadingBarBinding

class TransactionLoadingBar : BaseDialog() {
    private lateinit var binding: ActivityTransactionLoadingBarBinding

    override fun setContentView(builder: AlertDialog.Builder) {
        binding =
            ActivityTransactionLoadingBarBinding.inflate(LayoutInflater.from(requireContext()))

        builder.setView(binding.root)
        isCancelable = false
    }
}
package io.beldex.bchat.util

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.home.HomeFragment
import io.beldex.bchat.databinding.SaveYourSeedDialogBoxBinding

class SaveYourSeedDialogBox(
    private val showSeed: () -> Unit,
) : BaseDialog() {
    private lateinit var binding: SaveYourSeedDialogBoxBinding


    override fun setContentView(builder: AlertDialog.Builder) {
        binding = SaveYourSeedDialogBoxBinding.inflate(LayoutInflater.from(requireContext()))

        with(binding) {
            okButton.setOnClickListener {
                TextSecurePreferences.setCopiedSeed(requireContext(),true)
                showSeed()
                dismiss()
            }
        }

        builder.setView(binding.root)
        isCancelable = false
    }
}
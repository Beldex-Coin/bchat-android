package io.beldex.bchat.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.beldex.bchat.R
import io.beldex.bchat.databinding.DialogSeedBinding
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.conversation.v2.utilities.BaseDialog

class SeedDialog : BaseDialog() {

    private val seed by lazy {
        var hexEncodedSeed = IdentityKeyUtil.retrieve(requireContext(), IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed = IdentityKeyUtil.getIdentityKeyPair(requireContext()).hexEncodedPrivateKey // Legacy account
        }
        val loadFileContents: (String) -> String = { fileName ->
            MnemonicUtilities.loadFileContents(requireContext(), fileName)
        }
        MnemonicCodec(loadFileContents).encode(hexEncodedSeed!!, MnemonicCodec.Language.Configuration.english)
    }

    override fun setContentView(builder: AlertDialog.Builder) {
        val binding = DialogSeedBinding.inflate(LayoutInflater.from(requireContext()))
        binding.seedTextView.text = seed
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.copyButton.setOnClickListener { copySeed() }
        builder.setView(binding.root)
    }

    private fun copySeed() {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Seed", seed)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        dismiss()
    }
}
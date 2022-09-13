package com.thoughtcrimes.securesms.wallet.receive

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.util.FileProviderUtil
import com.thoughtcrimes.securesms.util.QRCodeUtilities
import com.thoughtcrimes.securesms.util.toPx
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityReceiveBinding
import java.io.File
import java.io.FileOutputStream


class ReceiveActivity : PassphraseRequiredActionBarActivity() {

    private lateinit var binding: ActivityReceiveBinding
    private var walletAddress: String? = null
    private var amount: String? = null
    private var qrCodedata: String? = null

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityReceiveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_receive_page_title)

        binding.walletAddressReceive.text =
            IdentityKeyUtil.retrieve(this, IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF)

        walletAddress = binding.walletAddressReceive.text.toString()
        amount = binding.amountEditTextReceive.text.toString()
        qrCodedata = "$walletAddress $amount"
        val size = toPx(280, resources)
        val qrCode = QRCodeUtilities.encode(
            qrCodedata!!,
            size,
            false,
            false
        )
        binding.qrCodeReceive.setImageBitmap(qrCode)
        binding.amountEditTextReceive.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                reGenerateQr()
            }
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {

            }
            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {

            }
        })
        binding.shareButton.setOnClickListener {
            shareQrCode()
        }
    }

    private fun shareQrCode() {
        val directory = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = "MyWalletQr.png"
        val file = File(directory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        val size = toPx(280, resources)
        val qrCode = qrCodedata?.let { QRCodeUtilities.encode(it, size, false, false) }
        qrCode?.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, FileProviderUtil.getUriFor(this, file))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        startActivity(
            Intent.createChooser(
                intent,
                resources.getString(R.string.fragment_view_my_qr_code_share_title)
            )
        )
    }

    private fun reGenerateQr() {
        val size = toPx(280, resources)
        qrCodedata = walletAddress + binding.amountEditTextReceive.text.toString()
        val qrCode = qrCodedata?.let {
            QRCodeUtilities.encode(
                qrCodedata!!,
                size,
                false,
                false
            )
        }
        binding.qrCodeReceive.setImageBitmap(qrCode)
    }
}
// endregion
package com.thoughtcrimes.securesms.preferences

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityAppLockBinding.inflate
import io.beldex.bchat.databinding.ActivityShowQrcodeWithScanQrcodeBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.util.*
import java.io.File
import java.io.FileOutputStream

class ShowQRCodeWithScanQRCodeActivity :  PassphraseRequiredActionBarActivity(){
    private lateinit var binding: ActivityShowQrcodeWithScanQrcodeBinding

    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityShowQrcodeWithScanQrcodeBinding.inflate(layoutInflater)
        // Set content view
        setContentView(binding.root)
        // Set title
        supportActionBar!!.title = resources.getString(R.string.activity_qr_code_title)

        val size = toPx(280, resources)
        val qrCode = QRCodeUtilities.encode(hexEncodedPublicKey, size, false, false)
        binding.qrCodeImageView.setImageBitmap(qrCode)
//        val explanation = SpannableStringBuilder("This is your unique public QR code. Other users can scan this to start a conversation with you.")
//        explanation.setSpan(StyleSpan(Typeface.BOLD), 8, 34, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        //binding.explanationTextView.text = resources.getString(R.string.fragment_view_my_qr_code_explanation)
        binding.shareButton.setOnClickListener { shareQRCode() }
        binding.scanButton.setOnClickListener {
            val intent = Intent(this,ScanQRCodeActivity::class.java)
            push(intent)
            finish()
        }
    }

    private fun shareQRCode() {
        val directory = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = "$hexEncodedPublicKey.png"
        val file = File(directory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        val size = toPx(280, resources)
        val qrCode = QRCodeUtilities.encode(hexEncodedPublicKey, size, false, false)
        qrCode.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, FileProviderUtil.getUriFor(this, file))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        startActivity(Intent.createChooser(intent, resources.getString(R.string.fragment_view_my_qr_code_share_title)))
    }
}
package com.thoughtcrimes.securesms.preferences

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityAppLockBinding.inflate
import io.beldex.bchat.databinding.ActivityShowQrcodeWithScanQrcodeBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
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
            /*val intent = Intent(this,ScanQRCodeActivity::class.java)
            push(intent)
            finish()*/
            val intent = Intent(this,ScanQRCodeActivity::class.java)
            callScanQRCodeActivityResultLauncher.launch(intent)
        }
    }

    private var callScanQRCodeActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS, result.data!!.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.URI,result.data!!.getParcelableExtra(ConversationFragmentV2.URI))
            val returnIntent = Intent()
            returnIntent.putExtra(ConversationFragmentV2.TYPE,result.data!!.getStringArrayExtra(ConversationFragmentV2.TYPE))
            //returnIntent.setDataAndType(intent.data, intent.type)
            returnIntent.putExtras(extras)
            setResult(RESULT_OK, returnIntent)
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
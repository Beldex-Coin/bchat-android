package com.thoughtcrimes.securesms.wallet.receive

import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.jakewharton.rxbinding3.view.visibility
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.data.Crypto
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.FileProviderUtil
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.QRCodeUtilities
import com.thoughtcrimes.securesms.util.toPx
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityReceiveBinding
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ClassCastException
import java.util.HashMap


class ReceiveActivity : PassphraseRequiredActionBarActivity() {

    private lateinit var binding: ActivityReceiveBinding
    private var walletAddress: String? = null
    private var amount: String? = null
    private var qrCodedata: String? = null
    private var bcData: BarcodeData? = null
    private var qrValid = false
    private var logo: Bitmap? = null
    private val isLoaded = false


    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityReceiveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_receive_page_title)


        binding.walletAddressReceive.text =
            IdentityKeyUtil.retrieve(this, IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF)



        walletAddress = binding.walletAddressReceive.text.toString()
        amount = binding.amountEditTextReceive.text.toString()
        generateQr()

        binding.amountEditTextReceive.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (binding.amountEditTextReceive.text.isNotEmpty()) {
                    reGenerateQr()
                } else {
                    generateQr()
                }
            }
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {}
        })




        binding.shareButton.setOnClickListener {
           // saveQrCode()
            shareQrCode()
        }
    }

    private fun shareQrCode() {
        val directory = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = "MyWalletQr.png"
        val file = File(directory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        val qrCode = (binding.qrCodeReceive.drawable as BitmapDrawable).bitmap
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


    fun generate(text: String?, width: Int, height: Int): Bitmap? {
        if (width <= 0 || height <= 0) return null
        val hints: MutableMap<EncodeHintType, Any?> = HashMap()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        try {
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            val pixels = IntArray(width * height)
            for (i in 0 until height) {
                for (j in 0 until width) {
                    if (bitMatrix[j, i]) {
                        pixels[i * width + j] = 0x00000000
                    } else {
                        pixels[i * height + j] = -0x1
                    }
                }
            }
            var bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565)
            bitmap = addLogo(bitmap)
            return bitmap
        } catch (ex: WriterException) {
            Timber.e(ex)
        }
        return null
    }

    private fun generateQr() {
        Log.d("Beldex","generateQR fun called")
        Timber.d("A-> GENQR")
        val address: String = binding.walletAddressReceive.text.toString()
        val notes: String = ""
        val xmrAmount: String = binding.amountEditTextReceive.text.toString()
        Log.d("beldex", "generateQR value of  $xmrAmount, $notes, $address")
        //        if ((xmrAmount == null) || !Wallet.isAddressValid(address)) {
//            clearQR();
//            Timber.d("CLEARQR");
//            return;
//        }
        bcData = BarcodeData(Crypto.XMR, address, notes, xmrAmount)

        val size: Int = Math.max(200, 200)
        val qr = generate(bcData!!.uriString, size, size)
        Log.d("Beldex","generateQR value of qr $qr")
        if (qr != null) {
            setQR(qr)
            Timber.d("A-> SETQR")
            //by hales
            /* etDummy.requestFocus()*/
            Helper.hideKeyboard(this)
        }
    }

    fun setQR(qr: Bitmap?) {
        binding.qrCodeReceive.setImageBitmap(qr)
        qrValid = true
        //by hales
        /* setShareIntent()*/
        binding.qrCodeReceive.visibility(View.GONE)
    }

    private fun addLogo(qrBitmap: Bitmap): Bitmap? {
        // addume logo & qrcode are both square
        val logo: Bitmap? = getBchatLogo()
        val qrSize = qrBitmap.width
        val logoSize = logo!!.width
        val logoBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(logoBitmap)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
        canvas.save()
        val sx = 0.2f * qrSize / logoSize
        canvas.scale(sx, sx, qrSize / 2f, qrSize / 2f)
        //canvas.drawBitmap(logo, (qrSize - logoSize) / 2f, (qrSize - logoSize) / 2f, null);
        canvas.restore()
        return logoBitmap
    }

    private fun getBchatLogo(): Bitmap? {
        if (logo == null) {
            logo = Helper.getBitmap(this, R.drawable.ic_launcher_foreground)
        }
        return logo
    }

    private fun reGenerateQr() {
        Timber.d("B->GENQR")
        val address: String = binding.walletAddressReceive.text.toString()
        val notes: String = ""
        val xmrAmount: String = binding.amountEditTextReceive.text.toString()
        Timber.d("%s/%s/%s", xmrAmount, notes, address)
        if (!Wallet.isAddressValid(address)) {

            clearQR()
            Timber.d("B-> CLEARQR")
            return
        }
        bcData = BarcodeData(Crypto.XMR, address, notes, xmrAmount)
        val size: Int = Math.max(binding.qrCodeReceive.width, binding.qrCodeReceive.height)
        val qr = generate(bcData!!.uriString, size, size)
        Timber.d("QR COde -> %s", qr)
        if (qr != null) {
            setQR(qr)
            Timber.d("B-> SETQR")
        }
    }


    fun clearQR() {
        if (qrValid) {
            binding.qrCodeReceive.setImageBitmap(null)
            qrValid = false
            //by hales
           /* setShareIntent()*/
            if (isLoaded)  binding.qrCodeReceive.setVisibility(View.VISIBLE)
        }
    }


    override fun onPause() {
        Timber.d("onPause()")
        Helper.hideKeyboard(this)
        super.onPause()
    }
}
// endregion
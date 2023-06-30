package com.thoughtcrimes.securesms.wallet.receive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.transition.MaterialContainerTransform
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.jakewharton.rxbinding3.view.visibility
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.data.Crypto
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.FileProviderUtil
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.utils.ThemeHelper
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityReceiveBinding
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.lang.ClassCastException
import java.lang.Exception
import java.util.HashMap


class ReceiveFragment : Fragment(), OnBackPressedListener {
    private lateinit var binding: ActivityReceiveBinding
    private var bcData: BarcodeData? = null
    private var qrValid = false
    private var logo: Bitmap? = null
    private val isLoaded = false
    var listenerCallback: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    interface Listener {
        fun walletOnBackPressed() //-
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listenerCallback = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityReceiveBinding.inflate(layoutInflater,container,false)
        (activity as HomeActivity).setSupportActionBar(binding.toolbar)
        binding.walletAddressReceive.text = IdentityKeyUtil.retrieve(requireActivity(),IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF)
        generateQr()

        binding.amountEditTextReceive.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (binding.amountEditTextReceive.text.isNotEmpty()) {
                    if(validateBELDEXAmount(s.toString())) {
                        hideErrorMessage()
                        reGenerateQr()
                    }else{
                        binding.beldexAmountConstraintLayout.setBackgroundResource(R.drawable.error_view_background)
                        binding.beldexAmountErrorMessage.visibility =View.VISIBLE
                        binding.beldexAmountErrorMessage.text=getString(R.string.beldex_amount_valid_error_message)
                    }
                } else {
                    hideErrorMessage()
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
            shareQrCode()
        }

        binding.addressCopy.setOnClickListener {
            copyYourBeldexAddress()
        }

        binding.exitButton.setOnClickListener {
            listenerCallback?.walletOnBackPressed()
        }

        return binding.root
    }

    private fun hideErrorMessage(){
        binding.amountEditTextReceive.setBackgroundResource(R.drawable.bchat_id_text_view_background)
        binding.beldexAmountErrorMessage.visibility = View.GONE
        binding.beldexAmountErrorMessage.text = ""
    }

    private fun copyYourBeldexAddress() {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Beldex Address", binding.walletAddressReceive.text.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireActivity(), R.string.beldex_address_clipboard, Toast.LENGTH_SHORT).show()
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
        val address: String = binding.walletAddressReceive.text.toString()
        val notes: String = ""
        val bdxAmount: String = binding.amountEditTextReceive.text.toString()
        bcData = BarcodeData(Crypto.BDX, address, notes, bdxAmount)

        val size: Int = Math.max(200, 200)
        val qr = generate(bcData!!.uriString, size, size)
        if (qr != null) {
            setQR(qr)
            Helper.hideKeyboard(activity)
        }
    }

    private fun setQR(qr: Bitmap?) {
        binding.qrCodeReceive.setImageBitmap(qr)
        qrValid = true
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
        canvas.restore()
        return logoBitmap
    }

    private fun getBchatLogo(): Bitmap? {
        if (logo == null) {
            logo = Helper.getBitmap(context, R.drawable.ic_launcher_foreground)
        }
        return logo
    }

    private fun reGenerateQr() {
        val address: String = binding.walletAddressReceive.text.toString()
        val notes: String = ""
        val bdxAmount: String = binding.amountEditTextReceive.text.toString()
        if (!Wallet.isAddressValid(address)) {
            clearQR()
            return
        }
        bcData = BarcodeData(Crypto.BDX, address, notes, bdxAmount)
        val size: Int = Math.max(binding.qrCodeReceive.width, binding.qrCodeReceive.height)
        val qr = generate(bcData!!.uriString, size, size)
        if (qr != null) {
            setQR(qr)
        }
    }

    private fun clearQR() {
        if (qrValid) {
            binding.qrCodeReceive.setImageBitmap(null)
            qrValid = false
            if (isLoaded) binding.qrCodeReceive.visibility = View.VISIBLE
        }
    }

    private fun shareQrCode() {
        val directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = "MyWalletQr.png"
        val file = File(directory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        val qrCode = (binding.qrCodeReceive.drawable as BitmapDrawable).bitmap
        qrCode?.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, FileProviderUtil.getUriFor(requireContext(), file))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        startActivity(
            Intent.createChooser(
                intent,
                resources.getString(R.string.fragment_view_my_qr_code_share_title)
            )
        )
    }

    private fun validateBELDEXAmount(amount:String):Boolean {
        val maxValue = 150000000.00000
        val value = amount.replace(',', '.')
        val regExp ="^(([0-9]{0,9})?|[.][0-9]{0,5})?|([0-9]{0,9}+([.][0-9]{0,5}))\$"
        var isValid = false

        isValid = if (value.matches(Regex(regExp))) {
            if (value == ".") {
                false
            } else {
                try {
                    val dValue = value.toDouble()
                    (dValue <= maxValue && dValue > 0)
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            false
        }
        return isValid
    }

    override fun onPause() {
        Helper.hideKeyboard(activity)
        super.onPause()
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}
//endregion
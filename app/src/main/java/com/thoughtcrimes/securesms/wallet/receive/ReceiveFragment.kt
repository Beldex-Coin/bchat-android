package com.thoughtcrimes.securesms.wallet.receive

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.content.FileProvider
import com.google.android.material.transition.MaterialContainerTransform
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.jakewharton.rxbinding3.view.visibility
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.data.Crypto
import com.thoughtcrimes.securesms.model.Wallet
import com.thoughtcrimes.securesms.util.FileProviderUtil
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.utils.ThemeHelper
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityReceiveBinding
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.HashMap

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ReceiveFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ReceiveFragment : Fragment(), OnBackPressedListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: ActivityReceiveBinding
    private var bcData: BarcodeData? = null
    private var qrValid = false
    private var logo: Bitmap? = null
    private val isLoaded = false
    var listenerCallback: ReceiveFragment.Listener? = null
    private val shareActionProvider: ShareActionProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        val transform = MaterialContainerTransform()
        transform.drawingViewId = R.id.fragment_container
        transform.duration = resources.getInteger(R.integer.tx_item_transition_duration).toLong()
        transform.setAllContainerColors(
            ThemeHelper.getThemedColor(
                context,
                android.R.attr.colorBackground
            )
        )
        sharedElementEnterTransition = transform
    }
    interface Listener {
        fun setToolbarButton(type: Int)
        fun setTitle(title: String?)
        fun setSubtitle(subtitle: String?)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityReceiveBinding.inflate(layoutInflater,container,false)
        // Inflate the layout for this fragment

        //val  view: View = inflater.inflate(R.layout.fragment_receive, container, false)
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
            shareQrCode()
        }

        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ReceiveFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ReceiveFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
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
            Helper.hideKeyboard(activity)
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
            logo = Helper.getBitmap(context, R.drawable.ic_launcher_foreground)
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


    private fun getShareIntent(): Intent? {
        val imagePath = File(requireActivity().cacheDir, "images")
        val png = File(imagePath, "QR.png")
        val contentUri = FileProvider.getUriForFile(
            requireActivity(),
            BuildConfig.APPLICATION_ID.toString() + ".fileprovider",
            png
        )
        if (contentUri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, requireActivity().contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.putExtra(Intent.EXTRA_TEXT, bcData!!.uriString)
            return shareIntent
        }
        return null
    }
    private fun saveQrCode() {
        check(qrValid) { "trying to save null qr code!" }
        val cachePath = File(requireActivity().cacheDir, "images")
        if (!cachePath.exists()) check(cachePath.mkdirs()) { "cannot create images folder" }
        val png = File(cachePath, "QR.png")
        try {
            val stream = FileOutputStream(png)
            val qrBitmap = (binding.qrCodeReceive.drawable as BitmapDrawable).bitmap
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
        } catch (ex: IOException) {
            Timber.e(ex)
            // make sure we don't share an old qr code
            check(png.delete()) { "cannot delete old qr code" }
            // if we manage to delete it, the URI points to nothing and the user gets a toast with the error
        }
    }


    override fun onPause() {
        Timber.d("onPause()")
        Helper.hideKeyboard(activity)
        super.onPause()
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}
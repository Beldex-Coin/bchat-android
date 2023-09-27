package com.thoughtcrimes.securesms.wallet.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.send.SendFragment
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentWalletScannerBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.io.FileNotFoundException
import java.io.InputStream


class WalletScannerFragment(
) : Fragment(), ZXingScannerView.ResultHandler,OnUriScannedListener, OnBackPressedListener
{
    private var onScannedListener: OnScannedListener? = null
    var activityCallback: Listener? = null

    fun newInstance(listener: Listener): WalletScannerFragment {
        val instance: WalletScannerFragment = WalletScannerFragment()
        instance.setSendListener(listener)
        return instance
    }
    private fun setSendListener(listener: Listener) {
        this.activityCallback = listener
    }

    interface Listener {
        fun onSendRequest(view: View?)
        fun setBarcodeData(data: BarcodeData?)
        fun walletOnBackPressed() //-
    }

    interface OnScannedListener {
        fun onScanned(qrCode: String?): Boolean
        fun setOnBarcodeScannedListener(onUriScannedListener: OnUriScannedListener?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);

    }

    lateinit var binding:FragmentWalletScannerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWalletScannerBinding.inflate(inflater,container,false)
        (activity as HomeActivity).setSupportActionBar(binding.toolbar)
        setHasOptionsMenu(true)
        binding.exitButton.setOnClickListener {
            activityCallback?.walletOnBackPressed()
        }
        binding.uploadFromGalleryLayout.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            resultLauncher.launch(pickIntent)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.mScannerView.setResultHandler(this)
        binding.mScannerView.startCamera()
    }

    override fun handleResult(rawResult: Result) {
        if (rawResult.barcodeFormat == BarcodeFormat.QR_CODE) {
            if (onScannedListener!!.onScanned(rawResult.text)) {
                return
            } else {
                Toast.makeText(
                    activity,
                    getString(R.string.send_qr_address_invalid),
                    Toast.LENGTH_SHORT
                ).show()
                val handler = Handler()
                handler.postDelayed(
                    { binding.mScannerView.resumeCameraPreview(this) },
                    1000
                )
            }
        } else {
            Toast.makeText(activity, getString(R.string.send_qr_invalid), Toast.LENGTH_SHORT).show()
        }

        // Note from dm77:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        val handler = Handler()
        handler.postDelayed(
            { binding.mScannerView.resumeCameraPreview(this) },
            1000
        )
    }

    private fun validateQRCode(result: Result) {
        if (result.barcodeFormat == BarcodeFormat.QR_CODE) {
            if (onScannedListener!!.onScanned(result.text)) {
                return
            } else {
                Toast.makeText(
                        activity,
                        getString(R.string.send_qr_address_invalid),
                        Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(activity, getString(R.string.send_qr_invalid), Toast.LENGTH_SHORT).show()
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data == null || data.data == null) {
                Log.e("TAG", "scan qr The uri is null, probably the user cancelled the image selection process using the back button.")
                return@registerForActivityResult
            }
            val uri = data.data
            try {
                val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(uri!!)
                var bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    Log.e("TAG", "scan qr uri is not a bitmap,$uri")
                    return@registerForActivityResult
                }
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                bitmap.recycle()
                bitmap = null
                val source = RGBLuminanceSource(width, height, pixels)
                val bBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                try {
                    val result = reader.decode(bBitmap)
                    validateQRCode(result)
                } catch (e: NotFoundException) {
                    Toast.makeText(requireActivity(), getString(R.string.invalid_qr_code_image), Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                Log.e("TAG", "scan qr can not open file" + uri.toString(), e)
            }
        }
    }

   /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            101 -> {
                if (data == null || data.data == null) {
                    Log.e("TAG", "scan qr The uri is null, probably the user cancelled the image selection process using the back button.")
                    return
                }
                val uri = data.data
                try {
                    val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(uri!!)
                    var bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap == null) {
                        Log.e("TAG", "scan qr uri is not a bitmap,$uri")
                        return
                    }
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    bitmap.recycle()
                    bitmap = null
                    val source = RGBLuminanceSource(width, height, pixels)
                    val bBitmap = BinaryBitmap(HybridBinarizer(source))
                    val reader = MultiFormatReader()
                    try {
                        val result = reader.decode(bBitmap)
                        validateQRCode(result)
                    } catch (e: NotFoundException) {
                        Toast.makeText(requireActivity(), getString(R.string.invalid_qr_code_image), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: FileNotFoundException) {
                    Log.e("TAG", "scan qr can not open file" + uri.toString(), e)
                }
            }
        }
    }*/

    override fun onStop() {
        super.onStop()
        binding.mScannerView.stopCamera();
    }

    override fun onPause() {
        super.onPause()
        this.binding.cameraView.onPause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnScannedListener) {
            onScannedListener = context
            onScannedListener!!.setOnBarcodeScannedListener(this)
            activityCallback = context as Listener

        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    override fun onUriScanned(barcodeData: BarcodeData?): Boolean {
        processScannedData(barcodeData, sendFragment = SendFragment())
        return true
    }

    private fun processScannedData(barcodeData: BarcodeData?, sendFragment: SendFragment) {
        activityCallback!!.onSendRequest(view)
        activityCallback!!.setBarcodeData(barcodeData)
        sendFragment.processScannedData(barcodeData)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}
//endregion

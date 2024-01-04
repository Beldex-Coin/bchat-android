package com.thoughtcrimes.securesms.util

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.thoughtcrimes.securesms.qr.ScanListener
import com.thoughtcrimes.securesms.qr.ScanningThread
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentScanQrCodeBinding
import java.io.FileNotFoundException
import java.io.InputStream

class ScanQRCodeFragment : Fragment() {
    private lateinit var binding: FragmentScanQrCodeBinding
    private val scanningThread = ScanningThread()
    var scanListener: ScanListener? = null
        set(value) { field = value; scanningThread.setScanListener(scanListener) }
    var message: CharSequence = ""
    var time = (1 * 1000).toLong()

    companion object{
       const val FROM_NEW_CHAT_SCREEN = "from_new_chat_screen"
    }


    override fun onCreateView(layoutInflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View {
        binding = FragmentScanQrCodeBinding.inflate(layoutInflater, viewGroup, false)
        return binding.root
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> binding.overlayView.orientation = LinearLayout.HORIZONTAL
            else -> binding.overlayView.orientation = LinearLayout.VERTICAL
        }

        val fromScreenValue = requireActivity().intent.extras!!.getBoolean("from_new_chat_screen")
        if (fromScreenValue) {
            binding.messageTextView.text = getString(R.string.activity_create_private_chat_scan_qr_code_explanation)
        } else {
            binding.messageTextView.text = getString(R.string.join_social_group_content)
        }
        binding.uploadFromGalleryLayout.setOnClickListener {
            binding.uploadFromGalleryLayout.isEnabled = false
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            resultLauncher.launch(pickIntent)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.uploadFromGalleryLayout.isEnabled = true
            }, time)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.cameraView.onResume()
        binding.cameraView.setPreviewCallback(scanningThread)
        try {
            scanningThread.start()
        } catch (exception: Exception) {
            // Do nothing
        }
        scanningThread.setScanListener(scanListener)
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
                    scanListener!!.onQrDataFound(result.toString())
                } catch (e: NotFoundException) {
                    Toast.makeText(requireActivity(), R.string.invalid_qr_code_image, Toast.LENGTH_SHORT).show()
                } catch (e: NotFoundException) {
                    Toast.makeText(requireActivity(), getString(R.string.invalid_qr_code_image), Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                Log.e("TAG", "scan qr can not open file" + uri.toString(), e)
            }
        }
    }

    override fun onConfigurationChanged(newConfiguration: Configuration) {
        super.onConfigurationChanged(newConfiguration)
        binding.cameraView.onPause()
        when (newConfiguration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> binding.overlayView.orientation = LinearLayout.HORIZONTAL
            else -> binding.overlayView.orientation = LinearLayout.VERTICAL
        }
        binding.cameraView.onResume()
        binding.cameraView.setPreviewCallback(scanningThread)
    }

    override fun onPause() {
        super.onPause()
        this.binding.cameraView.onPause()
        this.scanningThread.stopScanning()
    }
}
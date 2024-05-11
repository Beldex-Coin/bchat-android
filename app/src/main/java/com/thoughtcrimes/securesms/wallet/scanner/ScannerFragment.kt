package com.thoughtcrimes.securesms.wallet.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.walletscanqr.WalletScannerScreen
import io.beldex.bchat.R
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.ClassCastException

class ScannerFragment: Fragment(),OnBackPressedListener {
    private var onScannedListener: OnWalletScannedListener? = null

    interface OnWalletScannedListener {
        fun onWalletScanned(qrCode: String?): Boolean
        fun walletOnBackPressed() //-
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        val context = LocalContext.current
                        val lifecycleOwner = LocalLifecycleOwner.current
                        val cameraProviderFuture = remember {
                            ProcessCameraProvider.getInstance(context)
                        }
                        var hasCamPermission by remember {
                            mutableStateOf(
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            )
                        }
                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission(),
                            onResult = { granted ->
                                hasCamPermission = granted
                            }
                        )
                        LaunchedEffect(key1 = true) {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                        WalletScannerScreen(
                            hasCamPermission,
                            cameraProviderFuture,
                            lifecycleOwner,
                            onQrCodeScanned = { result ->
                                handleResult(result)
                            },
                            intent = {
                                resultLauncher.launch(it)
                            },
                            onBackPress = {
                                onScannedListener?.walletOnBackPressed()
                            })
                    }
                }
            }
        }
    }
    private fun handleResult(rawResult: Result) {
        if (rawResult.barcodeFormat == BarcodeFormat.QR_CODE) {
            if (onScannedListener!!.onWalletScanned(rawResult.text)) {
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

    private fun validateQRCode(result: Result) {
        if (result.barcodeFormat == BarcodeFormat.QR_CODE) {
            if (onScannedListener!!.onWalletScanned(result.text)) {
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnWalletScannedListener) {
            onScannedListener = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}
//endregion


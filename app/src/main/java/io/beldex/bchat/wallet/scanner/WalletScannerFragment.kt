package io.beldex.bchat.wallet.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.data.BarcodeData
import io.beldex.bchat.wallet.OnBackPressedListener
import io.beldex.bchat.wallet.OnUriScannedListener
import io.beldex.bchat.wallet.jetpackcomposeUI.walletscanqr.WalletScannerScreen
import io.beldex.bchat.wallet.send.SendFragment
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentWalletScannerBinding
import java.io.FileNotFoundException
import java.io.InputStream


class WalletScannerFragment(
) : Fragment(), OnUriScannedListener, OnBackPressedListener {
    private var onScannedListener: OnScannedListener? = null
    private var activityCallback: Listener? = null
    var time = (1 * 1000).toLong()
    private var lastAddress: String? = null
    companion object {
        const val FRAGMENT_TAG = "WalletScannerFragment_FRAGMENT_TAG"
    }

    fun newInstance(listener: Listener): WalletScannerFragment {
        val instance = WalletScannerFragment()
        instance.setSendListener(listener)
        return instance
    }

    private fun setSendListener(listener: Listener) {
        this.activityCallback = listener
    }

    interface Listener {
        fun onSendRequest()
        fun setBarcodeData(data: BarcodeData?)
        fun walletOnBackPressed()
    }

    interface OnScannedListener {
        fun onScanned(qrCode: String?): Boolean
        fun setOnBarcodeScannedListener(onUriScannedListener: OnUriScannedListener?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
    }

    lateinit var binding: FragmentWalletScannerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme {
                    WalletScannerScreen(
                        onQrCodeScanned = { result ->
                            handleResult(result)
                        },
                        intent = {
                            resultLauncher.launch(it)
                        },
                        onBackPress = {
                            activityCallback?.walletOnBackPressed()
                        })
                }
            }
        }
    }

    private fun handleResult(rawResult: Result) {
        if(lastAddress == rawResult.text) return
        lastAddress = rawResult.text
        if (rawResult.barcodeFormat == BarcodeFormat.QR_CODE) {
            if (!onScannedListener!!.onScanned(rawResult.text)) {
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

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data == null || data.data == null) {
                    Log.e(
                        FRAGMENT_TAG,
                        "scan qr The uri is null, probably the user cancelled the image selection process using the back button."
                    )
                    return@registerForActivityResult
                }
                val uri = data.data
                try {
                    val inputStream: InputStream? =
                        requireActivity().contentResolver.openInputStream(uri!!)
                    var bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap == null) {
                        Log.e(FRAGMENT_TAG, "scan qr uri is not a bitmap,$uri")
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
                    val reader = QRCodeReader()
                    try {
                        val resultData = reader.decode(bBitmap)
                        handleResult(resultData)
                    } catch (e: NotFoundException) {
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.invalid_qr_code_image),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: FileNotFoundException) {
                    Log.e(FRAGMENT_TAG, "scan qr can not open file" + uri.toString(), e)
                }
            }
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
        activityCallback!!.onSendRequest()
        activityCallback!!.setBarcodeData(barcodeData)
        sendFragment.processScannedData(barcodeData)
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}
//endregion

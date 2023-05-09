package com.thoughtcrimes.securesms.wallet.scanner

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentScannerBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.lang.ClassCastException

class ScannerFragment: Fragment(), ZXingScannerView.ResultHandler,OnBackPressedListener {
    private var onScannedListener: OnWalletScannedListener? = null

    interface OnWalletScannedListener {
        fun onWalletScanned(qrCode: String?): Boolean
        fun walletOnBackPressed() //-
    }

    lateinit var binding:FragmentScannerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScannerBinding.inflate(inflater,container,false)
        (activity as HomeActivity).setSupportActionBar(binding.toolbar)
        binding.exitButton.setOnClickListener {
            onScannedListener?.walletOnBackPressed()
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
            if (onScannedListener!!.onWalletScanned(rawResult.text)) {
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
            1000)
    }

    override fun onStop() {
        super.onStop()
        binding.mScannerView.stopCamera();
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


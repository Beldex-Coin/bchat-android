package com.thoughtcrimes.securesms.wallet.scanner

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.wallet.OnBackPressedListener
import com.thoughtcrimes.securesms.wallet.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.send.SendFragment
import com.thoughtcrimes.securesms.wallet.widget.Toolbar
import io.beldex.bchat.R
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber
import java.lang.ClassCastException


class WalletScannerFragment(
) : Fragment(), ZXingScannerView.ResultHandler,OnUriScannedListener, OnBackPressedListener
{
    private var onScannedListener: OnScannedListener? = null
  /*  private var activityCallback: SendFragment.Listener? = null*/
    private var sendFragment: SendFragment? = null
    private var uri: String? = null
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
        fun setToolbarButton(type: Int)
        fun setTitle(title: String?)
        fun setSubtitle(subtitle: String?)
    }

    interface OnScannedListener {
        fun onScanned(qrCode: String?): Boolean
        fun setOnBarcodeScannedListener(onUriScannedListener: OnUriScannedListener?)
    }

    private var mScannerView: ZXingScannerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        Timber.d("onCreateView")
        mScannerView = ZXingScannerView(activity)
        return mScannerView
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        mScannerView!!.setResultHandler(this)
        mScannerView!!.startCamera()
        activityCallback!!.setTitle(getString(R.string.activity_scan_page_title))
        activityCallback!!.setToolbarButton(Toolbar.BUTTON_BACK)
    }

    override fun handleResult(rawResult: Result) {
        if (rawResult.barcodeFormat == BarcodeFormat.QR_CODE) {
            if (onScannedListener!!.onScanned(rawResult.text)) {
                Log.d("Beldex","value of barcode ${rawResult.text}")
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

        // Note from dm77:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        val handler = Handler()
        handler.postDelayed({ mScannerView!!.resumeCameraPreview(this@WalletScannerFragment) }, 2000)
    }

    override fun onPause() {
        Timber.d("onPause")
        mScannerView!!.stopCamera()
        super.onPause()
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
        Log.d("Beldex","value of barcode 7")
        processScannedData(barcodeData, sendFragment = SendFragment())
        Log.d("Beldex","value of barcode 8")
        return true
    }
    private fun processScannedData(barcodeData: BarcodeData?, sendFragment: SendFragment) {
        Log.d("Beldex","value of barcode 9 $activityCallback")
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

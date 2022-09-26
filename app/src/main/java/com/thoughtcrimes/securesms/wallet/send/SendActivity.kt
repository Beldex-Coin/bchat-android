package com.thoughtcrimes.securesms.wallet.send


import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.wallet.node.NodeFragment
import com.thoughtcrimes.securesms.wallet.scan.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.scan.ScannerFragment
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySendBinding
import timber.log.Timber

class SendActivity : PassphraseRequiredActionBarActivity(),SendFragmentNew.OnScanListener, ScannerFragment.OnScannedListener {
    private lateinit var binding: ActivitySendBinding


    val onUriScannedListener: OnUriScannedListener? = null

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.send)

        val walletFragment: Fragment = SendFragmentNew()
        supportFragmentManager.beginTransaction()
            .add(R.id.sendScreen_Frame, walletFragment, SendFragmentNew::class.java.name).commit()



    }
        override fun onScan() {
            if (Helper.getCameraPermission(this)) {
                startScanFragment()
            } else {
                Timber.i("Waiting for permissions")
            }
        }

    private fun startScanFragment() {
        val extras = Bundle()
        replaceFragment(ScannerFragment(), null, extras)
    }

    private fun replaceFragment(newFragment: Fragment, stackName: String?, extras: Bundle?) {
        if (extras != null) {
            newFragment.arguments = extras
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.sendScreen_Frame, newFragment)
            .addToBackStack(stackName)
            .commit()
    }

        override fun onScanned(qrCode: String?): Boolean {
            // #gurke
            val bcData: BarcodeData = BarcodeData.fromString(qrCode)
            return if (bcData != null) {
                popFragmentStack(null)
                Timber.d("AAA")
                onUriScanned(bcData)
                true
            } else {
                false
            }
        }
    fun popFragmentStack(name: String?) {
        if (name == null) {
            supportFragmentManager.popBackStack()
        } else {
            supportFragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
    override fun onUriScanned(barcodeData: BarcodeData?) {
        super.onUriScanned(barcodeData)
        var processed = false
        if (onUriScannedListener != null) {
            processed = onUriScannedListener.onUriScanned(barcodeData)
        }
        if (!processed || onUriScannedListener == null) {
            Toast.makeText(this, getString(R.string.nfc_tag_read_what), Toast.LENGTH_LONG).show()
        }
    }


    }

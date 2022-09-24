package com.thoughtcrimes.securesms.wallet.send


import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.data.BarcodeData
import com.thoughtcrimes.securesms.data.TxData
import com.thoughtcrimes.securesms.data.UserNotes
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.wallet.scan.OnUriScannedListener
import com.thoughtcrimes.securesms.wallet.scan.ScannerFragment
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySendBinding
import timber.log.Timber

class SendActivity : PassphraseRequiredActionBarActivity(),
    SendFragmentSub.OnScanListener,ScannerFragment.OnScannedListener,
    SendFragmentSub.Listener, SendFragmentMain.Listener{
    private lateinit var binding: ActivitySendBinding


    private var onUriScannedListener: OnUriScannedListener? = null
    private var barcodeData: BarcodeData? = null
    private val toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.send)

        val walletFragment: Fragment =
            SendFragmentMain()
        supportFragmentManager.beginTransaction()
            .add(R.id.sendScreen_Frame, walletFragment, SendFragmentMain::class.java.name).commit()



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
    private fun popFragmentStack(name: String?) {
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
            processed = onUriScannedListener!!.onUriScanned(barcodeData)
        }
        if (!processed || onUriScannedListener == null) {
            Toast.makeText(this, getString(R.string.nfc_tag_read_what), Toast.LENGTH_LONG).show()
        }
    }


    override fun setBarcodeData(data: BarcodeData) {
        barcodeData = data
    }
    override fun getBarcodeData(): BarcodeData {
        return barcodeData!!
    }

    override fun popBarcodeData(): BarcodeData {
        Timber.d("POPPED")
        val data = barcodeData!!
        barcodeData = null
        return data
    }

    override fun getTxData(): TxData {
        return txData
    }

    override fun getPrefs(): SharedPreferences {
        return getPreferences(MODE_PRIVATE)
    }

    override fun getTotalFunds(): Long {
        TODO("Not yet implemented")
    }

    override fun isStreetMode(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onPrepareSend(tag: String?, data: TxData?) {
        TODO("Not yet implemented")
    }

    override fun getWalletName(): String {
        TODO("Not yet implemented")
    }

    override fun onSend(notes: UserNotes?) {
        TODO("Not yet implemented")
    }

    override fun onDisposeRequest() {
        TODO("Not yet implemented")
    }

    override fun onFragmentDone() {
        TODO("Not yet implemented")
    }

    override fun setToolbarButton(type: Int) {
        TODO("Not yet implemented")
    }

    override fun setTitle(title: String?) {
        Timber.d("setTitle:%s.", title)
        toolbar?.title = title
    }

    override fun setSubtitle(subtitle: String?) {
        toolbar?.subtitle = subtitle
    }

    override fun setOnUriScannedListener(onUriScannedListener: OnUriScannedListener?) {
        this.onUriScannedListener = onUriScannedListener
    }

}

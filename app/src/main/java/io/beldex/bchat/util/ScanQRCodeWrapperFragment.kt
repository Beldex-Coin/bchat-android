package io.beldex.bchat.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.beldex.bchat.R
import io.beldex.bchat.qr.ScanListener

class ScanQRCodeWrapperFragment : Fragment(), ScanQRCodePlaceholderFragmentDelegate, ScanListener {

    companion object {
        const val FRAGMENT_TAG = "ScanQRCodeWrapperFragment_FRAGMENT_TAG"
    }

    var delegate: ScanQRCodeWrapperFragmentDelegate? = null
    var message: CharSequence = ""
    var enabled: Boolean = true
    set(value) {
        val shouldUpdate = field != value // update if value changes (view appears or disappears)
        field = value
        if (shouldUpdate) {
            update()
        }
    }
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        enabled = isVisibleToUser
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan_qr_code_wrapper, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        update()
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                update()
            } else {
                Log.d("Beldex","Camera permission was denied by the user.")
            }
        }
    }

    private fun update() {
        if (!this.isAdded) return

        val fragment: Fragment
        if (!enabled) {
            val manager = childFragmentManager
            manager.findFragmentByTag(FRAGMENT_TAG)?.let { existingFragment ->
                // remove existing camera fragment (if switching back to other page)
                manager.beginTransaction().remove(existingFragment).commit()
            }
            return
        }
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val scanQRCodeFragment = ScanQRCodeFragment()
            scanQRCodeFragment.scanListener = this
            scanQRCodeFragment.message = message
            fragment = scanQRCodeFragment
        } else {
            val scanQRCodePlaceholderFragment = ScanQRCodePlaceholderFragment()
            scanQRCodePlaceholderFragment.delegate = this
            fragment = scanQRCodePlaceholderFragment
        }
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment, FRAGMENT_TAG)
        transaction.commit()
    }

    override fun requestCameraAccess() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onQrDataFound(string: String) {
        activity?.runOnUiThread {
            delegate?.handleQRCodeScanned(string)
        }
    }
}

interface ScanQRCodeWrapperFragmentDelegate {

    fun handleQRCodeScanned(string: String)
}
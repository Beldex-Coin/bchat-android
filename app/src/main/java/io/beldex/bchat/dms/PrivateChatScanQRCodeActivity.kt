package io.beldex.bchat.dms

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libsignal.utilities.PublicKeyValidation
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.qr.ScanListener
import io.beldex.bchat.util.ScanQRCodeFragment
import io.beldex.bchat.util.ScanQRCodePlaceholderFragment
import io.beldex.bchat.util.ScanQRCodePlaceholderFragmentDelegate
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityPrivateChatScanQrcodeBinding
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class PrivateChatScanQRCodeActivity : PassphraseRequiredActionBarActivity(),
    ScanQRCodePlaceholderFragmentDelegate, ScanListener {
    companion object {
        const val FRAGMENT_TAG = "PrivateChatScanQRCodeActivity_ACTIVITY_TAG"
    }

    private lateinit var binding: ActivityPrivateChatScanQrcodeBinding
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

    /* override fun setUserVisibleHint(isVisibleToUser: Boolean) {
         super.setUserVisibleHint(isVisibleToUser)
         enabled = isVisibleToUser
     }*/

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityPrivateChatScanQrcodeBinding.inflate(layoutInflater)
        // Set content view
        setContentView(binding.root)
        // Set title
        supportActionBar!!.title = resources.getString(R.string.activity_qr_code_view_scan_qr_code_tab_title)
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

        val fragment: Fragment
        if (!enabled) {
            val manager = supportFragmentManager
            manager.findFragmentByTag(FRAGMENT_TAG)?.let { existingFragment ->
                // remove existing camera fragment (if switching back to other page)
                manager.beginTransaction().remove(existingFragment).commit()
            }
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val scanQRCodeFragment = ScanQRCodeFragment()
            scanQRCodeFragment.scanListener = this
            scanQRCodeFragment.message = message
            intent.putExtra(ScanQRCodeFragment.FROM_NEW_CHAT_SCREEN, true)
            fragment = scanQRCodeFragment
        } else {
            val scanQRCodePlaceholderFragment = ScanQRCodePlaceholderFragment()
            scanQRCodePlaceholderFragment.delegate = this
            fragment = scanQRCodePlaceholderFragment
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(
            R.id.fragmentContainer, fragment,
            FRAGMENT_TAG
        )
        transaction.commit()
    }

    override fun requestCameraAccess() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onQrDataFound(string: String) {
        this.runOnUiThread {
            handleQRCodeScanned(string)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    // region Updating
    private fun showLoader() {
        binding.loader.visibility = View.VISIBLE
        binding.loader.animate().setDuration(150).alpha(1.0f).start()
    }

    private fun hideLoader() {
        binding.loader.animate().setDuration(150).alpha(0.0f).setListener(object : AnimatorListenerAdapter() {
           override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.loader.visibility = View.GONE
            }
        })
    }

    private fun handleQRCodeScanned(hexEncodedPublicKey: String) {
        createPrivateChatIfPossible(hexEncodedPublicKey)
    }

    private fun createPrivateChatIfPossible(bnsNameOrPublicKey: String) {
        if (PublicKeyValidation.isValid(bnsNameOrPublicKey)) {
            createPrivateChat(bnsNameOrPublicKey, bnsNameOrPublicKey)
        } else {
            //Toast.makeText(this, R.string.invalid_bchat_id, Toast.LENGTH_SHORT).show()
            // This could be an BNS name
            showLoader()
            MnodeAPI.getBchatID(bnsNameOrPublicKey).successUi { hexEncodedPublicKey ->
                hideLoader()
                this.createPrivateChat(hexEncodedPublicKey,bnsNameOrPublicKey)
            }.failUi { exception ->
                hideLoader()
                var message = resources.getString(R.string.fragment_enter_public_key_error_message)
                exception.localizedMessage?.let {
                    message = it
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createPrivateChat(hexEncodedPublicKey: String,bnsName: String ) {
        val bundle = Bundle()
        bundle.putParcelable(ConversationFragmentV2.URI,intent.data)
        bundle.putString(ConversationFragmentV2.TYPE,intent.type)
        bundle.putString(ConversationFragmentV2.BNS_NAME,bnsName)
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationFragmentV2.HEX_ENCODED_PUBLIC_KEY, hexEncodedPublicKey)
        //returnIntent.setDataAndType(intent.data, intent.type)
        returnIntent.putExtras(bundle)
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}
package com.thoughtcrimes.securesms.preferences

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tbruyelle.rxpermissions2.RxPermissions
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityScanQrcodeBinding
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.qr.ScanListener
import com.thoughtcrimes.securesms.util.*

class ScanQRCodeActivity : PassphraseRequiredActionBarActivity(), ScanQRCodePlaceholderFragmentDelegate,
    ScanListener {
    companion object {
        const val FRAGMENT_TAG = "ScanQRCodeActivity_ACTIVITY_TAG"
    }

    private lateinit var binding: ActivityScanQrcodeBinding
    var message: CharSequence = ""
    var enabled: Boolean = true
        set(value) {
            val shouldUpdate = field != value // update if value changes (view appears or disappears)
            field = value
            if (shouldUpdate) {
                update()
            }
        }

   /* override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        enabled = isVisibleToUser
    }*/

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityScanQrcodeBinding.inflate(layoutInflater)
        // Set content view
        setContentView(binding.root)
        // Set title
        supportActionBar!!.title = resources.getString(R.string.activity_qr_code_view_scan_qr_code_tab_title)
        update()
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
        transaction.replace(R.id.fragmentContainer, fragment,
            FRAGMENT_TAG
        )
        transaction.commit()
    }

    override fun requestCameraAccess() {
        @SuppressWarnings("unused")
        val unused = RxPermissions(this).request(Manifest.permission.CAMERA).subscribe { isGranted ->
            if (isGranted) {
                update()
            }
        }
    }

    override fun onQrDataFound(string: String) {
        this.runOnUiThread {
            handleQRCodeScanned(string)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        /*val intent = Intent(this,ShowQRCodeWithScanQRCodeActivity::class.java)
        push(intent)*/
        finish()
    }

    // region Interaction
    fun handleQRCodeScanned(hexEncodedPublicKey: String) {
        createPrivateChatIfPossible(hexEncodedPublicKey)
    }

    fun createPrivateChatIfPossible(hexEncodedPublicKey: String) {
        if (!PublicKeyValidation.isValid(hexEncodedPublicKey)) { return Toast.makeText(this, R.string.invalid_bchat_id, Toast.LENGTH_SHORT).show() }
       /* val recipient = Recipient.from(this, Address.fromSerialized(hexEncodedPublicKey), false)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS, recipient.address)
        intent.setDataAndType(getIntent().data, getIntent().type)
        val existingThread = DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        intent.putExtra(ConversationActivityV2.THREAD_ID, existingThread)
        startActivity(intent)
        finish()*/

        val recipient = Recipient.from(this, Address.fromSerialized(hexEncodedPublicKey), false)
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationFragmentV2.ADDRESS, recipient.address)
        returnIntent.putExtra(ConversationFragmentV2.URI,intent.data)
        returnIntent.putExtra(ConversationFragmentV2.TYPE,intent.type)
        //returnIntent.setDataAndType(intent.data, intent.type)
        val existingThread = DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, existingThread)
        setResult(RESULT_OK, returnIntent)
        finish()
    }
    // endregion
}
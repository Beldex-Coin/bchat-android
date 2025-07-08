package io.beldex.bchat.groups

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityJoinPublicChatScanQrcodeBinding
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.PublicKeyValidation
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.dms.PrivateChatScanQRCodeActivity
import io.beldex.bchat.qr.ScanListener
import io.beldex.bchat.util.*

class JoinPublicChatScanQRCodeActivity : PassphraseRequiredActionBarActivity(),
    ScanQRCodePlaceholderFragmentDelegate,
        ScanListener {

    companion object {
        const val FRAGMENT_TAG = "JoinPublicChatScanQRCodeActivity_ACTIVITY_TAG"
    }

    private lateinit var binding: ActivityJoinPublicChatScanQrcodeBinding
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

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityJoinPublicChatScanQrcodeBinding.inflate(layoutInflater)
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
            manager.findFragmentByTag(PrivateChatScanQRCodeActivity.FRAGMENT_TAG)?.let { existingFragment ->
                // remove existing camera fragment (if switching back to other page)
                manager.beginTransaction().remove(existingFragment).commit()
            }
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val scanQRCodeFragment = ScanQRCodeFragment()
            scanQRCodeFragment.scanListener = this
            scanQRCodeFragment.message = message
            intent.putExtra(ScanQRCodeFragment.FROM_NEW_CHAT_SCREEN, false)
            fragment = scanQRCodeFragment
        } else {
            val scanQRCodePlaceholderFragment = ScanQRCodePlaceholderFragment()
            scanQRCodePlaceholderFragment.delegate = this
            fragment = scanQRCodePlaceholderFragment
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(
            R.id.fragmentContainer, fragment,
            PrivateChatScanQRCodeActivity.FRAGMENT_TAG
        )
        transaction.commit()
    }

    override fun requestCameraAccess() {
        @SuppressWarnings("unused")
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                update()
            } else {
                // Handle denial if needed
            }
        }
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

    // region Interaction
    fun handleQRCodeScanned(url: String) {
        joinPublicChatIfPossible(url)
    }

    fun joinPublicChatIfPossible(url: String) {
        // Add "http" if not entered explicitly
        val stringWithExplicitScheme = if (!url.startsWith("http")) "http://$url" else url
        Log.d("Beldex","join group URL  $url")
        val url = stringWithExplicitScheme.toHttpUrlOrNull() ?: return Toast.makeText(this,
            R.string.invalid_url, Toast.LENGTH_SHORT).show()
        Log.d("Beldex","join group full URL  $url")
        val room = url.pathSegments.firstOrNull()
        Log.d("Beldex","join group room  $room")
        val publicKey = url.queryParameter("public_key")
        Log.d("Beldex","join group public key  $publicKey")
        val isV2OpenGroup = !room.isNullOrEmpty()
        if (isV2OpenGroup && (publicKey == null || !PublicKeyValidation.isValid(publicKey, 64,false))) {
            return Toast.makeText(this, R.string.invalid_public_key, Toast.LENGTH_SHORT).show()
        }
        showLoader()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val (threadID, groupID) = if (isV2OpenGroup) {
                    val server = HttpUrl.Builder().scheme(url.scheme).host(url.host).apply {
                        if (url.port != 80 || url.port != 443) { this.port(url.port) } // Non-standard port; add to server
                    }.build()
                    Log.d("Beldex","join group server in joinPublicChatIfPossible fun  $server")
                    Log.d("Beldex","join group url in joinPublicChatIfPossible fun $url")

                    val sanitizedServer = server.toString().removeSuffix("/")
                    Log.d("Beldex","join group sanitizedServer in joinPublicChatIfPossible fun $sanitizedServer")
                    val openGroupID = "$sanitizedServer.${room!!}"
                    Log.d("Beldex","join group openGroupID in joinPublicChatIfPossible fun $openGroupID")
                    OpenGroupManager.add(sanitizedServer, room, publicKey!!, this@JoinPublicChatScanQRCodeActivity)
                    val threadID = GroupManager.getOpenGroupThreadID(openGroupID, this@JoinPublicChatScanQRCodeActivity)
                    Log.d("Beldex","join group threadID in joinPublicChatIfPossible fun $threadID")
                    val groupID = GroupUtil.getEncodedOpenGroupID(openGroupID.toByteArray())
                    Log.d("Beldex","join group groupID in joinPublicChatIfPossible fun $groupID")
                    threadID to groupID
                } else {
                    throw Exception("No longer supported.")
                }
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@JoinPublicChatScanQRCodeActivity)
                withContext(Dispatchers.Main) {
                    val recipient = Recipient.from(this@JoinPublicChatScanQRCodeActivity, Address.fromSerialized(groupID), false)
                    openConversationActivity(threadID, recipient)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("Beldex", "Couldn't join social group.", e)
                withContext(Dispatchers.Main) {
                    hideLoader()
                    Toast.makeText(this@JoinPublicChatScanQRCodeActivity, R.string.activity_join_public_chat_error, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
        }
    }
    // endregion

    // region Convenience
    private fun openConversationActivity(threadId: Long, recipient: Recipient) {
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,threadId)
        returnIntent.putExtra(ConversationFragmentV2.ADDRESS,recipient.address)
        setResult(RESULT_OK, returnIntent)
    }
    // endregion
}
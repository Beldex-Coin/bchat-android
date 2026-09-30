package io.beldex.bchat.onboarding

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.utilities.Hex
import com.beldex.libsignal.utilities.KeyHelper
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.beldex.libsignal.utilities.hexEncodedPublicKey
import com.goterl.lazysodium.utils.KeyPair
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.KeyPairUtilities
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.model.AsyncTaskCoroutine
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.model.WalletManager
import io.beldex.bchat.onboarding.ui.PinCodeAction
import io.beldex.bchat.onboarding.ui.RegisterScreen
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.util.BChatThreadPoolExecutor
import io.beldex.bchat.util.push
import io.beldex.bchat.R
import java.util.Locale
import java.util.concurrent.Executor

class RegisterActivity : BaseComponentActivity() {
    private var seed: ByteArray? = null
    private var ed25519KeyPair: KeyPair? = null
    private var x25519KeyPair: ECKeyPair? = null
        set(value) {
            field = value; updatePublicKeyTextView()
        }

    //New Line
    private var walletPath: String? = null
    private var localPassword: String? = null
    private var displayName: String? = null
    val REQUEST_PATH = "path"
    val REQUEST_PASSWORD = "password"
    val VIEW_TYPE_DETAILS = "details"
    val VIEW_TYPE_ACCEPT = "accept"
    val VIEW_TYPE_WALLET = "wallet"
    val REQUEST_NAME = "displayName"
    var type: String? = null
    val REQUEST_TYPE = "type"
    private var walletName: String? = null

    // Compose UI state (replaces the old ViewBinding field reads/writes below)
    private var isAddressLoading by mutableStateOf(true)
    private var beldexAddressText by mutableStateOf("")
    private var isPublicKeyLoading by mutableStateOf(true)
    private var publicKeyText by mutableStateOf("")
    private var isRegisterEnabled by mutableStateOf(false)
    private var headlineText by mutableStateOf("")

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        TextSecurePreferences.apply {
            setHasViewedSeed(this@RegisterActivity, false)
            setConfigurationMessageSynced(this@RegisterActivity, true)
            setRestorationTime(this@RegisterActivity, 0)
            setLastProfileUpdateTime(this@RegisterActivity, System.currentTimeMillis())
        }

        //New Line
        type = intent.extras?.getString(REQUEST_TYPE)
        walletPath = intent.extras?.getString(REQUEST_PATH)
        localPassword = intent.extras?.getString(REQUEST_PASSWORD)
        displayName =intent.extras?.getString(REQUEST_NAME)
        val displayedName : String=displayName?.substring(0, 1)?.uppercase(Locale.ROOT) + displayName?.substring(1)?.lowercase(Locale.ROOT)
        headlineText = resources.getString(R.string.hey_user_welcome_to_bchat).format(displayedName)

        setContent {
            BChatTheme {
                RegisterScreen(
                    headline = headlineText,
                    isAddressLoading = isAddressLoading,
                    beldexAddress = beldexAddressText,
                    isPublicKeyLoading = isPublicKeyLoading,
                    publicKey = publicKeyText,
                    registerEnabled = isRegisterEnabled,
                    onCopyPublicKey = { copyPublicKey() },
                    onCopyAddress = { copyBeldexAddress() },
                    onRegisterClick = { register() },
                    onBackClick = { finish() }
                )
            }
        }

        showDetails()


        //Important
        //updateKeyPair()
    }
    // endregion

    //Dummy
    private val seed1 by lazy {
        var hexEncodedSeed = IdentityKeyUtil.retrieve(this, IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed = IdentityKeyUtil.getIdentityKeyPair(this).hexEncodedPrivateKey // Legacy account
        }
        val loadFileContents: (String) -> String = { fileName ->
            MnemonicUtilities.loadFileContents(this, fileName)
        }
        MnemonicCodec(loadFileContents).encode(
            hexEncodedSeed!!,
            MnemonicCodec.Language.Configuration.english
        )
    }

    fun showDetails() {
        AsyncShow(this, walletPath)
            .execute<Executor>(BChatThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR)
    }

    private class AsyncShow(val registerActivity: RegisterActivity, val walletPathVal: String?) :
        AsyncTaskCoroutine<Executor?, Boolean>() {
        var name: String? = null
        var address: String? = null
        var height: Long = 0
        var seed: String? = null
        var viewKey: String? = null
        var spendKey: String? = null
        var isWatchOnly = false
        var walletStatus: Wallet.Status? = null
        var dialogOpened = false
        override fun onPreExecute() {
            super.onPreExecute()
            //showProgress()
            //registerActivity.showProgressDialog(R.string.please_wait, 250)
            registerActivity.isAddressLoading = true
            registerActivity.isRegisterEnabled = false
            /*if (walletPathVal != null
                && (WalletManager.getInstance()
                    .queryWalletDevice("$walletPathVal.keys", registerActivity.localPassword)
                        === Wallet.Device.Device_Ledger)
            //&& progressCallback != null
            ) {
                Log.d("onPreExecute--> ","ok")
                //progressCallback.showLedgerProgressDialog(LedgerProgressDialog.TYPE_RESTORE)
                dialogOpened = true
            }*/
        }

        override fun doInBackground(vararg params: Executor?): Boolean {
            //Important refer this line what purpose of this below code
            /*if (walletPathVal?.length != 1) {
                Log.d("doInBackground wallet path length", walletPathVal?.length.toString())
                return false
            }
            val walletPath = walletPathVal[0]!!*/


            /*val wallet: Wallet
              val closeWallet: Boolean
              if (registerActivity.type == registerActivity.VIEW_TYPE_WALLET) {
                    wallet = registerActivity.walletCallback.getWallet()
                    closeWallet = false
              } else {
                    wallet = WalletManager.getInstance().openWallet(walletPathVal, registerActivity.localPassword)
                    closeWallet = true
              }*/

            val wallet = WalletManager.getInstance()
                .openWallet(walletPathVal, registerActivity.localPassword)
            val closeWallet = true
            name = wallet.name
            walletStatus = wallet.status
            if (!walletStatus!!.isOk) {
                Log.e("", walletStatus!!.errorString)
                if (closeWallet) wallet.close()
                return false
            }
            address = wallet.address
            height = wallet.restoreHeight
            seed = wallet.seed
            viewKey = wallet.secretViewKey
/* viewKey = when (wallet.getDeviceType()) {
     Device_Ledger -> Ledger.Key()
     Device_Software -> wallet.getSecretViewKey()
     else -> throw IllegalStateException("Hardware backing not supported. At all!")
 }*/
            spendKey = wallet.secretSpendKey
/*spendKey = if (isWatchOnly) getActivity().getString(R.string.label_watchonly) else wallet.getSecretSpendKey()*/
            isWatchOnly = wallet.isWatchOnly

            IdentityKeyUtil.save(
                registerActivity,
                IdentityKeyUtil.IDENTITY_W_PUBLIC_KEY_PREF,
                wallet.publicViewKey
            )
            IdentityKeyUtil.save(
                registerActivity,
                IdentityKeyUtil.IDENTITY_W_PUBLIC_TWO_KEY_PREF,
                wallet.secretViewKey
            )
            IdentityKeyUtil.save(
                registerActivity,
                IdentityKeyUtil.IDENTITY_W_PUBLIC_THREE_KEY_PREF,
                wallet.publicSpendKey
            )
            IdentityKeyUtil.save(
                registerActivity,
                IdentityKeyUtil.IDENTITY_W_PUBLIC_FOUR_KEY_PREF,
                wallet.secretSpendKey
            )
            IdentityKeyUtil.save(
                registerActivity,
                IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF,
                wallet.address
            )
            TextSecurePreferences.setSenderAddress(registerActivity,wallet.address)
            if (closeWallet) wallet.close()
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            //if (dialogOpened) registerActivity.dismissProgressDialog()
            //if (!isAdded()) return  // never mind
            //registerActivity.dismissProgressDialog()

            registerActivity.walletName = name
            Log.d("onPostExecute--> ", result.toString())
            if (result == true) {
                if (registerActivity.type == registerActivity.VIEW_TYPE_ACCEPT) {
                    /*bAccept.setVisibility(View.VISIBLE)
                    bAccept.setEnabled(true)*/
                }

                /*tvWalletAddress.setText(address)
                tvWalletHeight.setText(NumberFormat.getInstance().format(height))*/
                if (!seed!!.isEmpty()) {
                    /*bCopyAddressMnemonicSeed.setEnabled(true)
                    llMnemonic.setVisibility(View.VISIBLE)
                    tvWalletMnemonic.setText(seed)*/
                    val loadFileContents: (String) -> String = { fileName ->
                        MnemonicUtilities.loadFileContents(registerActivity, fileName)
                    }
                    val hexEncodedSeed = MnemonicCodec(loadFileContents).decode(seed!!)
                    val seedByteArray = Hex.fromStringCondensed(hexEncodedSeed)
                    registerActivity.updateKeyPair(seedByteArray, address)
                }
                /*var showAdvanced = false
                if (isKeyValid(viewKey)) {
                    llViewKey.setVisibility(View.VISIBLE)
                    tvWalletViewKey.setText(viewKey)
                    showAdvanced = true
                }
                if (isKeyValid(spendKey)) {
                    llSpendKey.setVisibility(View.VISIBLE)
                    tvWalletSpendKey.setText(spendKey)
                    showAdvanced = true
                }*/

                /* if (showAdvanced) llAdvancedInfo.setVisibility(View.VISIBLE)
                 bCopyAddress.setEnabled(true)
                 activityCallback.setTitle(name + " " + getString(R.string.details_title), "")
                 activityCallback.setToolbarButton(
                     if (VIEW_TYPE_ACCEPT == type) Toolbar.BUTTON_NONE else Toolbar.BUTTON_BACK
                 )*/
            } else {
                /*tvWalletAddress.setText(walletStatus.toString())
                tvWalletHeight.setText(walletStatus.toString())
                tvWalletMnemonic.setText(walletStatus.toString())
                tvWalletViewKey.setText(walletStatus.toString())
                tvWalletSpendKey.setText(walletStatus.toString())*/
            }
            //hideProgress()
        }
    }

    // region Updating
    /*private fun updateKeyPair() {
        val keyPairGenerationResult = KeyPairUtilities.generate()
        seed = keyPairGenerationResult.seed
        ed25519KeyPair = keyPairGenerationResult.ed25519KeyPair
        x25519KeyPair = keyPairGenerationResult.x25519KeyPair
    }*/

    //New Line
    private fun updateKeyPair(seedByteArray: ByteArray, address: String?) {
        beldexAddressText = address.orEmpty()
        val keyPairGenerationResult = KeyPairUtilities.generate(seedByteArray)
        seed = keyPairGenerationResult.seed
        ed25519KeyPair = keyPairGenerationResult.ed25519KeyPair
        x25519KeyPair = keyPairGenerationResult.x25519KeyPair
        KeyPairUtilities.store(
            this,
            seed!!,
            keyPairGenerationResult.ed25519KeyPair,
            x25519KeyPair!!
        )
        isAddressLoading = false
        isRegisterEnabled = true
    }

    //Main
    /*private fun updatePublicKeyTextView() {
        val hexEncodedPublicKey = x25519KeyPair!!.hexEncodedPublicKey
        val characterCount = hexEncodedPublicKey.count()
        var count = 0
        val limit = 32
        fun animate() {
            val numberOfIndexesToShuffle = 32 - count
            val indexesToShuffle =
                (0 until characterCount).shuffled().subList(0, numberOfIndexesToShuffle)
            var mangledHexEncodedPublicKey = hexEncodedPublicKey
            for (index in indexesToShuffle) {
                try {
                    mangledHexEncodedPublicKey = mangledHexEncodedPublicKey.substring(
                        0,
                        index
                    ) + "0123456789abcdef__".random() + mangledHexEncodedPublicKey.substring(
                        index + 1,
                        mangledHexEncodedPublicKey.count()
                    )
                } catch (exception: Exception) {
                    // Do nothing
                }
            }
            count += 1
            if (count < limit) {
                binding.publicKeyTextView.text = mangledHexEncodedPublicKey
                Handler().postDelayed({
                    animate()
                }, 32)
            } else {
                binding.publicKeyTextView.text = hexEncodedPublicKey
            }
        }
        animate()
    }*/

    private fun updatePublicKeyTextView() {
        Handler().postDelayed({
            isPublicKeyLoading = false
            publicKeyText = x25519KeyPair!!.hexEncodedPublicKey
        }, 32)
    }
// endregion

    private val pinCodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            //New Line AirDrop
            TextSecurePreferences.setAirdropAnimationStatus(this,true)

            TextSecurePreferences.setScreenLockEnabled(this, true)
            TextSecurePreferences.setScreenLockTimeout(this, 950400)
            //New Line
            TextSecurePreferences.setHasSeenWelcomeScreen(this, true)
            val intent1 = Intent(this, KeyCachingService::class.java)
            intent1.action = KeyCachingService.LOCK_TOGGLED_EVENT
            this.startService(intent1)
            val intent = Intent(this, RecoveryPhraseActivity::class.java)
            push(intent)
            finish()
        }
    }

    // region Interaction
    private fun register() {
        //Important
        //KeyPairUtilities.store(this, seed!!, ed25519KeyPair!!, x25519KeyPair!!)
        if (seed == null || x25519KeyPair == null || x25519KeyPair?.hexEncodedPublicKey == null) {
            return
        }

        val userHexEncodedPublicKey = x25519KeyPair!!.hexEncodedPublicKey
        val registrationID = KeyHelper.generateRegistrationId(false)
        TextSecurePreferences.setLocalRegistrationId(this, registrationID)
        TextSecurePreferences.setLocalNumber(this, userHexEncodedPublicKey)
        TextSecurePreferences.setRestorationTime(this, 0)
        TextSecurePreferences.setHasViewedSeed(this, false)
        val intent = Intent(Intent.ACTION_VIEW, "onboarding://manage_pin?finish=true&action=${PinCodeAction.CreatePinCode.action}".toUri())
        pinCodeLauncher.launch(intent)
    }

    private fun copyPublicKey() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BChat ID", x25519KeyPair!!.hexEncodedPublicKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun copyBeldexAddress() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Beldex Address", beldexAddressText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

// endregion
}
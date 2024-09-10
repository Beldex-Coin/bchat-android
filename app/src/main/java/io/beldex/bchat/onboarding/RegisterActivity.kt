package io.beldex.bchat.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.beldex.bchat.crypto.MnemonicUtilities
import com.goterl.lazysodium.utils.KeyPair
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityRegisterBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.utilities.*
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.KeyPairUtilities
import io.beldex.bchat.model.AsyncTaskCoroutine
import io.beldex.bchat.util.BChatThreadPoolExecutor
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import java.util.concurrent.Executor

class RegisterActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityRegisterBinding
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

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Register", true)

        TextSecurePreferences.apply {
            setHasViewedSeed(this@RegisterActivity, false)
            setConfigurationMessageSynced(this@RegisterActivity, true)
            setRestorationTime(this@RegisterActivity, 0)
            setLastProfileUpdateTime(this@RegisterActivity, System.currentTimeMillis())
        }
        binding.registerButton.setOnClickListener { register() }
        binding.copyButton.setOnClickListener { copyPublicKey() }
        val termsExplanation =
                SpannableStringBuilder("By using this service, you agree to our Terms of Service and Privacy Policy")
        termsExplanation.setSpan(
                StyleSpan(Typeface.BOLD),
                40,
                56,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        termsExplanation.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                openURL("https://www.beldex.io/")
            }
        }, 40, 56, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        termsExplanation.setSpan(
                StyleSpan(Typeface.BOLD),
                61,
                75,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        termsExplanation.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                openURL("https://www.beldex.io/")
            }
        }, 61, 75, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.termsTextView.movementMethod = LinkMovementMethod.getInstance()
        binding.termsTextView.text = termsExplanation

        //New Line
        type = intent.extras?.getString(REQUEST_TYPE)
        walletPath = intent.extras?.getString(REQUEST_PATH)
        localPassword = intent.extras?.getString(REQUEST_PASSWORD)
        displayName =intent.extras?.getString(REQUEST_NAME)
        binding.titleContentTextView?.text = "Hey $displayName, welcome to BChat!"

        //showDetails()


        //Important
        updateKeyPair()
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

/*    fun showDetails() {
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
            registerActivity.binding.registerButton.isEnabled=false
            registerActivity.binding.registerButton.setTextColor(ContextCompat.getColor(registerActivity, R.color.disable_button_text_color))
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                registerActivity.binding.registerButton.setBackgroundDrawable(ContextCompat.getDrawable(registerActivity, R.drawable.prominent_filled_button_medium_background_disable) );
            } else {
                registerActivity.binding.registerButton.background =
                        ContextCompat.getDrawable(registerActivity, R.drawable.prominent_filled_button_medium_background_disable);
            }
        }

        override fun doInBackground(vararg params: Executor?): Boolean {

            name = wallet.name
            walletStatus = wallet.status
            if (!walletStatus!!.isOk) {
                Log.e("", walletStatus!!.errorString)
                if (closeWallet) wallet.close()
                return false
            }
            address = wallet.address
            height = wallet.restoreHeight
            seed = KeyPairUtilities.se
            viewKey = wallet.secretViewKey
            *//* viewKey = when (wallet.getDeviceType()) {
                 Device_Ledger -> Ledger.Key()
                 Device_Software -> wallet.getSecretViewKey()
                 else -> throw IllegalStateException("Hardware backing not supported. At all!")
             }*//*
            spendKey = wallet.secretSpendKey
            *//*spendKey = if (isWatchOnly) getActivity().getString(R.string.label_watchonly) else wallet.getSecretSpendKey()*//*
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
            registerActivity.binding.registerButton.isEnabled=true
            registerActivity.binding.registerButton.setTextColor(ContextCompat.getColor(registerActivity, R.color.white))
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                registerActivity.binding.registerButton.setBackgroundDrawable(ContextCompat.getDrawable(registerActivity, R.drawable.prominent_filled_button_medium_background) );
            } else {
                registerActivity.binding.registerButton.background =
                        ContextCompat.getDrawable(registerActivity, R.drawable.prominent_filled_button_medium_background);
            }
            registerActivity.walletName = name
            Log.d("onPostExecute--> ", result.toString())
            if (result == true) {
                if (registerActivity.type == registerActivity.VIEW_TYPE_ACCEPT) {
                    *//*bAccept.setVisibility(View.VISIBLE)
                    bAccept.setEnabled(true)*//*
                }

                *//*tvWalletAddress.setText(address)
                tvWalletHeight.setText(NumberFormat.getInstance().format(height))*//*
                if (!seed!!.isEmpty()) {
                    *//*bCopyAddressMnemonicSeed.setEnabled(true)
                    llMnemonic.setVisibility(View.VISIBLE)
                    tvWalletMnemonic.setText(seed)*//*
                    val loadFileContents: (String) -> String = { fileName ->
                        MnemonicUtilities.loadFileContents(registerActivity, fileName)
                    }
                    val hexEncodedSeed = MnemonicCodec(loadFileContents).decode(seed!!)
                    val seedByteArray = Hex.fromStringCondensed(hexEncodedSeed)
                    registerActivity.updateKeyPair(seedByteArray)
                }
                *//*var showAdvanced = false
                if (isKeyValid(viewKey)) {
                    llViewKey.setVisibility(View.VISIBLE)
                    tvWalletViewKey.setText(viewKey)
                    showAdvanced = true
                }
                if (isKeyValid(spendKey)) {
                    llSpendKey.setVisibility(View.VISIBLE)
                    tvWalletSpendKey.setText(spendKey)
                    showAdvanced = true
                }*//*

                *//* if (showAdvanced) llAdvancedInfo.setVisibility(View.VISIBLE)
                 bCopyAddress.setEnabled(true)
                 activityCallback.setTitle(name + " " + getString(R.string.details_title), "")
                 activityCallback.setToolbarButton(
                     if (VIEW_TYPE_ACCEPT == type) Toolbar.BUTTON_NONE else Toolbar.BUTTON_BACK
                 )*//*
            } else {
                *//*tvWalletAddress.setText(walletStatus.toString())
                tvWalletHeight.setText(walletStatus.toString())
                tvWalletMnemonic.setText(walletStatus.toString())
                tvWalletViewKey.setText(walletStatus.toString())
                tvWalletSpendKey.setText(walletStatus.toString())*//*
            }
            //hideProgress()
        }
    }*/

    // region Updating
    private fun updateKeyPair() {
        println("update key pair value called 1")
        val keyPairGenerationResult = KeyPairUtilities.generate()
        seed = keyPairGenerationResult.seed
        println("update key pair value called 2 $seed")
        ed25519KeyPair = keyPairGenerationResult.ed25519KeyPair
        x25519KeyPair = keyPairGenerationResult.x25519KeyPair
        println("update key pair value called 3 $ed25519KeyPair and $x25519KeyPair")
    }

    //New Line
/*    private fun updateKeyPair(seedByteArray: ByteArray) {
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
    }*/

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
            binding.publicKeyAnimation!!.visibility=View.GONE
            binding.publicKeyTextView.visibility=View.VISIBLE
            binding.publicKeyTextView.text = x25519KeyPair!!.hexEncodedPublicKey
        }, 32)
    }
// endregion

    // region Interaction
    private fun register() {
        //Important
        //KeyPairUtilities.store(this, seed!!, ed25519KeyPair!!, x25519KeyPair!!)

        val userHexEncodedPublicKey = x25519KeyPair!!.hexEncodedPublicKey
        val registrationID = KeyHelper.generateRegistrationId(false)
        TextSecurePreferences.setLocalRegistrationId(this, registrationID)
        TextSecurePreferences.setLocalNumber(this, userHexEncodedPublicKey)
        TextSecurePreferences.setRestorationTime(this, 0)
        TextSecurePreferences.setHasViewedSeed(this, false)
        val intent = Intent(this, CreatePasswordActivity::class.java)
        intent.putExtra("callPage", 1)
        push(intent)
    }

    private fun copyPublicKey() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BChat ID", x25519KeyPair!!.hexEncodedPublicKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun openURL(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
        }
    }
// endregion
}
package io.beldex.bchat.preferences

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.beldex.libbchat.avatars.AvatarHelper
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySettingsBinding
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.successUi
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfileKeyUtil
import com.beldex.libbchat.utilities.ProfilePictureUtilities
import com.beldex.libbchat.utilities.SSKEnvironment.ProfileManagerProtocol
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.truncateIdForDisplay
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.applock.AppLockDetailsActivity
import io.beldex.bchat.avatar.AvatarSelection
import io.beldex.bchat.changelog.ChangeLogActivity
import io.beldex.bchat.components.ProfilePictureView
import io.beldex.bchat.contacts.blocked.BlockedContactsActivity
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.home.PathActivity
import com.bumptech.glide.Glide;
import io.beldex.bchat.util.*
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Date
import com.bumptech.glide.RequestManager
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.profiles.ProfileMediaConstraints
import io.beldex.bchat.showCustomDialog
import io.beldex.bchat.wallet.CheckOnline
import kotlinx.coroutines.Dispatchers
import java.util.regex.Pattern
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import com.beldex.libsignal.utilities.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : PassphraseRequiredActionBarActivity(), Animation.AnimationListener {
    private lateinit var binding: ActivitySettingsBinding
    private var displayNameEditActionMode: ActionMode? = null
        set(value) {
            field = value; handleDisplayNameEditActionModeChanged()
        }
    private lateinit var glide: RequestManager
    private var tempFile: File? = null

    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    private val TAG = "SettingsActivity"
    private val onAvatarCropped = registerForActivityResult(CropImageContract()) { result ->
        when {
            result.isSuccessful -> {
                Log.i(TAG, result.getUriFilePath(this).toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val profilePictureToBeUploaded =
                            BitmapUtil.createScaledBytes(
                                this@SettingsActivity,
                                result.getUriFilePath(this@SettingsActivity).toString(),
                                ProfileMediaConstraints()
                            ).bitmap
                        launch(Dispatchers.Main) {
                            updateProfile(true,profilePictureToBeUploaded)
                        }
                    } catch (e: BitmapDecodingException) {
                        Log.e(TAG, e)
                    }
                }
            }
            result is CropImage.CancelledResult -> {
                Log.i(TAG, "Cropping image was cancelled by the user")
            }
            else -> {
                Log.e(TAG, "Cropping image failed")
            }
        }
    }
    private val onPickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val outputFile = Uri.fromFile(File(cacheDir, "cropped"))
        val inputFile: Uri? = result.data?.data ?: tempFile?.let(Uri::fromFile)
        cropImage(inputFile, outputFile)
    }
    private val avatarSelection = AvatarSelection(this, onAvatarCropped, onPickImage)

    companion object {
        const val updatedProfileResultCode = 1234
    }

    //New Line
    private lateinit var animation1: Animation
    private lateinit var animation2: Animation
    private var isFrontOfCardShowing = true
    private val namePattern = Pattern.compile("[A-Za-z0-9\\s]+")
    private var shareButtonLastClickTime: Long = 0

    private fun getDisplayName(): String =
        TextSecurePreferences.getProfileName(this) ?: truncateIdForDisplay(hexEncodedPublicKey)

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("My Account")
        val displayName = getDisplayName()
        glide = Glide.with(this)

        val size = toPx(280, resources)
        val qrCode = QRCodeUtilities.encode(hexEncodedPublicKey, size, false, false)
        binding.qrCodeImageView.setImageBitmap(qrCode)
        binding.qrCodeShareButton.setOnClickListener {
            if (SystemClock.elapsedRealtime() - shareButtonLastClickTime >= 1000) {
                shareButtonLastClickTime = SystemClock.elapsedRealtime()
                shareQRCode()
            }
        }

        // apply animation from to_middle
        animation1 = AnimationUtils.loadAnimation(this, R.anim.to_middle)
        animation1.setAnimationListener(this)

        // apply animation from to_middle
        animation2 = AnimationUtils.loadAnimation(this, R.anim.from_middle)
        animation2.setAnimationListener(this)

        with(binding) {
            setupProfilePictureView(profilePictureView.root)
            //New Line
            profilePictureView.root.setOnClickListener { showEditProfilePictureUI() }

            profilePictureViewButton.setOnClickListener { showEditProfilePictureUI() }
            ctnGroupNameSection.setOnClickListener {
                startActionMode(
                    DisplayNameEditActionModeCallback()
                )
            }
            btnGroupNameDisplay.text = displayName
            publicKeyTextView.text = hexEncodedPublicKey
            publicKeyCardView.setOnClickListener { copyPublicKey() }
            shareButton.setOnClickListener { sharePublicKey() }
            pathButton.setOnClickListener { showPath() }
            pathContainer.disableClipping()
            privacyButton.setOnClickListener { showPrivacySettings() }
            notificationsButton.setOnClickListener { showNotificationSettings() }
            chatsButton.setOnClickListener { showChatSettings() }
            faqButton.setOnClickListener { showFAQ() }
            surveyButton.setOnClickListener { showSurvey() }
            seedButton.setOnClickListener { showSeed() }
            clearAllDataButton.setOnClickListener { clearAllData() }
            debugLogButton.setOnClickListener { shareLogs() }
            blockedcontactbutton.setOnClickListener{ blockedContacts()}

            //New Line
            changeLogButton.setOnClickListener { showChangeLog() }
            //New Line
            appLockButton.setOnClickListener { showAppLockDetailsPage() }
            val isLightMode = UiModeUtilities.isDayUiMode(this@SettingsActivity)
            //versionTextView.text = String.format(getString(R.string.version_s), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            beldexAddressTextView.text = IdentityKeyUtil.retrieve(
                this@SettingsActivity,
                IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF
            )
            profileButton.setOnClickListener {
                it.isEnabled = false

                // stop animation
                profileCardView.clearAnimation()
                profileCardView.animation = animation1

                // start the animation
                profileCardView.startAnimation(animation1)
            }
            qrCodeButton.setOnClickListener {
                it.isEnabled = false

                // stop animation
                profileCardView.clearAnimation()
                profileCardView.animation = animation1

                // start the animation
                profileCardView.startAnimation(animation1)
            }

            beldexAddressCardView.setOnClickListener { copyBeldexAddress() }
            beldexAddressShareButton.setOnClickListener { shareBeldexAddress() }

            loader.setOnClickListener {  }
        }
    }

    private fun setupProfilePictureView(view: ProfilePictureView) {
        view.glide = glide
        view.apply {
            publicKey = hexEncodedPublicKey
            displayName = getDisplayName()
            isLarge = true
            update(displayName)
        }
    }

    override fun onAnimationEnd(animation: Animation) {
        if (animation === animation1) {
            // check whether the front of the card is showing
            if (isFrontOfCardShowing) {
                // set image from card_front to card_back
                binding.backViewLinearLayout.visibility = View.VISIBLE
                binding.frontViewLinearLayout.visibility = View.GONE
            } else {
                // set image from card_back to card_front
                binding.backViewLinearLayout.visibility = View.GONE
                binding.frontViewLinearLayout.visibility = View.VISIBLE
            }
            // stop the animation of the ImageView
            binding.profileCardView.clearAnimation()
            binding.profileCardView.animation = animation2
            // allow fine-grained control
            // over the start time and invalidation
            binding.profileCardView.startAnimation(animation2)
        } else {
            isFrontOfCardShowing = !isFrontOfCardShowing
            binding.profileButton.isEnabled = true
            binding.qrCodeButton.isEnabled = true
        }
    }

    override fun onAnimationRepeat(animation: Animation?) {
        // TODO Auto-generated method stub
    }

    override fun onAnimationStart(animation: Animation?) {
        // TODO Auto-generated method stub
    }

    private fun shareQRCode() {
        val directory = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = "$hexEncodedPublicKey.png"
        val file = File(directory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        val size = toPx(280, resources)
        val qrCode = QRCodeUtilities.encode(hexEncodedPublicKey, size, false, false)
        qrCode.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, FileProviderUtil.getUriFor(this, file))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        startActivity(
            Intent.createChooser(
                intent,
                resources.getString(R.string.fragment_view_my_qr_code_share_title)
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_general, menu)
        // Update UI mode menu icon
        /*val uiMode = UiModeUtilities.getUserSelectedUiMode(this)
        menu.findItem(R.id.action_change_theme).icon!!.level = uiMode.ordinal*/
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_qr_code -> {
                showQRCode()
                true
            }
            /*  R.id.action_change_theme -> {
                  ChangeUiModeDialog().show(supportFragmentManager, ChangeUiModeDialog.TAG)
                  true
              }*/
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
    // endregion

    // region Updating
    private fun handleDisplayNameEditActionModeChanged() {
        val isEditingDisplayName = this.displayNameEditActionMode !== null

        binding.btnGroupNameDisplay.visibility =
            if (isEditingDisplayName) View.INVISIBLE else View.VISIBLE
        binding.displayNameTitleLinearLayout.visibility = if(isEditingDisplayName) View.INVISIBLE else View.VISIBLE
        binding.displayNameEditText.visibility =
            if (isEditingDisplayName) View.VISIBLE else View.INVISIBLE

        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (isEditingDisplayName) {
            binding.displayNameEditText.setText(binding.btnGroupNameDisplay.text)
            binding.displayNameEditText.selectAll()
            binding.displayNameEditText.requestFocus()
            inputMethodManager.showSoftInput(binding.displayNameEditText, 0)
        } else {
            inputMethodManager.hideSoftInputFromWindow(binding.displayNameEditText.windowToken, 0)
        }
    }

    private fun updateProfile(isUpdatingProfilePicture: Boolean, profilePicture: ByteArray? = null,
                              displayName: String? = null) {
        binding.loader.isVisible = true
        val promises = mutableListOf<Promise<*, Exception>>()
        if (displayName != null) {
            TextSecurePreferences.setProfileName(this, displayName)
        }
        val encodedProfileKey = ProfileKeyUtil.generateEncodedProfileKey(this)
        if (isUpdatingProfilePicture) {
            if (profilePicture != null) {
                promises.add(ProfilePictureUtilities.upload(profilePicture, encodedProfileKey, this))
            } else {
                TextSecurePreferences.setLastProfilePictureUpload(this, System.currentTimeMillis())
                TextSecurePreferences.setProfilePictureURL(this, null)
            }
        }
        val compoundPromise = all(promises)
        compoundPromise.successUi { // Do this on the UI thread so that it happens before the alwaysUi clause below
            if (isUpdatingProfilePicture) {
                AvatarHelper.setAvatar(
                    this,
                    Address.fromSerialized(TextSecurePreferences.getLocalNumber(this)!!),
                    profilePicture
                )
                TextSecurePreferences.setProfileAvatarId(this,profilePicture?.let { SecureRandom().nextInt() } ?: 0)
                TextSecurePreferences.setLastProfilePictureUpload(this, Date().time)
                ProfileKeyUtil.setEncodedProfileKey(this, encodedProfileKey)
            }
            if (profilePicture != null || displayName != null) {
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@SettingsActivity)
            }
        }
        compoundPromise.alwaysUi {
            if (displayName != null) {
                binding.btnGroupNameDisplay.text = displayName
            }
            if (isUpdatingProfilePicture) {
                binding.profilePictureView.root.recycle() // Clear the cached image before updating
                binding.profilePictureView.root.update(displayName)
            }
            binding.loader.isVisible = false
        }
    }

    private fun blockedContacts() {
        val intent = Intent(this, BlockedContactsActivity::class.java)
        show(intent)
    }

    // endregion

    // region Interaction

    /**
     * @return true if the update was successful.
     */
    private fun saveDisplayName(): Boolean {
        val displayName = binding.displayNameEditText.text.toString().trim()
        if (displayName.isEmpty()) {
            Toast.makeText(
                this,
                R.string.activity_settings_display_name_missing_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (displayName.toByteArray().size > ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            Toast.makeText(
                this,
                R.string.activity_settings_display_name_too_long_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (!displayName.matches(namePattern.toRegex())) {
            Toast.makeText(
                    this,
                    R.string.display_name_validation,
                    Toast.LENGTH_SHORT
            ).show()
            return false
        }
        updateProfile(false, displayName = displayName)
        return true
    }

    private fun showQRCode() {
        val intent = Intent(this, ShowQRCodeWithScanQRCodeActivity::class.java)
        showQRCodeWithScanQRCodeActivityResultLauncher.launch(intent)
    }

    private var showQRCodeWithScanQRCodeActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS, result.data!!.getParcelableExtra(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID,-1))
            extras.putParcelable(ConversationFragmentV2.URI,result.data!!.getParcelableExtra(ConversationFragmentV2.URI))
            val returnIntent = Intent()
            returnIntent.putExtra(ConversationFragmentV2.TYPE,result.data!!.getStringArrayExtra(ConversationFragmentV2.TYPE))
            //returnIntent.setDataAndType(intent.data,intent.type)
            returnIntent.putExtras(extras)
            setResult(RESULT_OK, returnIntent)
            finish() //-
        }
    }

    private fun showEditProfilePictureUI() {
        showCustomDialog {
            title(getString(R.string.activity_settings_profile_picture))
            icon(R.drawable.ic_close)
            view(R.layout.dialog_change_avatar)
            removeButton(R.string.activity_settings_remove) { removeAvatar() }
            button(R.string.activity_settings_upload) { startAvatarSelection() }
        }.apply {
            findViewById<ProfilePictureView>(R.id.profile_picture_view)?.let(::setupProfilePictureView)
        }
    }

    private fun removeAvatar() {
        updateProfile(true)
    }

    private fun startAvatarSelection() {
        // Ask for an optional camera permission.
        if (CheckOnline.isOnline(this)) {
            Permissions.with(this)
                    .request(Manifest.permission.CAMERA)
                    .onAnyResult {
                        tempFile = avatarSelection.startAvatarSelection(false, true)
                    }
                    .execute()
        } else {
            Toast.makeText(
                    this,
                    getString(R.string.please_check_your_internet_connection),
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cropImage(inputFile: Uri?, outputFile: Uri?){
        avatarSelection.circularCropImage(
            inputFile = inputFile,
            outputFile = outputFile,
        )
    }

    private fun copyPublicKey() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat ID", hexEncodedPublicKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.bchat_id_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun copyBeldexAddress() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat ID", binding.beldexAddressTextView.text.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.beldex_address_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun sharePublicKey() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, hexEncodedPublicKey)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.share))
        startActivity(chooser)
    }

    private fun shareBeldexAddress() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, binding.beldexAddressTextView.text.toString())
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.share))
        startActivity(chooser)
    }

    private fun showPrivacySettings() {
        val intent = Intent(this, PrivacySettingsActivity::class.java)
        push(intent)
    }

    private fun showNotificationSettings() {
        val intent = Intent(this, NotificationSettingsActivity::class.java)
        push(intent)
    }

    private fun showChatSettings() {
        val intent = Intent(this, ChatSettingsActivity::class.java)
        push(intent)
    }

    private fun showFAQ() {
        try {
            val url = "https://bchat.beldex.io/faq"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open URL", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPath() {
        val intent = Intent(this, PathActivity::class.java)
        show(intent)
    }

    private fun showChangeLog() {
        val intent = Intent(this, ChangeLogActivity::class.java)
        show(intent)
    }

    private fun showAppLockDetailsPage() {
        val intent = Intent(this, AppLockDetailsActivity::class.java)
        show(intent)
    }

    private fun showSurvey() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@beldex.io"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "")
        startActivity(intent)
    }

    private fun helpTranslate() {
        try {
            val url = "https://www.beldex.io/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open URL", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSeed() {
        SeedDialog().show(supportFragmentManager, "Recovery Seed Dialog")
    }

    private fun clearAllData() {
        ClearAllDataDialog().show(supportFragmentManager, "Clear All Data Dialog")
    }

    private fun shareLogs() {
        Permissions.with(this)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .maxSdkVersion(Build.VERSION_CODES.P)
            .withPermanentDenialDialog(getString(R.string.MediaPreviewActivity_signal_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
            .onAnyDenied {
                Toast.makeText(
                    this,
                    R.string.MediaPreviewActivity_unable_to_write_to_external_storage_without_permission,
                    Toast.LENGTH_LONG
                ).show()
            }
            .onAllGranted {
                ShareLogsDialog().show(supportFragmentManager, "Share Logs Dialog")
            }
            .execute()
    }

    // endregion

    private inner class DisplayNameEditActionModeCallback : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = getString(R.string.activity_settings_display_name_edit_text_hint)
            mode.menuInflater.inflate(R.menu.menu_apply, menu)
            this@SettingsActivity.displayNameEditActionMode = mode
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            this@SettingsActivity.displayNameEditActionMode = null
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.applyButton -> {
                    if (this@SettingsActivity.saveDisplayName()) {
                        mode.finish()
                    }
                    return true
                }
            }
            return false;
        }
    }
}
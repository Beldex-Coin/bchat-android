package com.thoughtcrimes.securesms.my_account.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.beldex.libbchat.avatars.AvatarHelper
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfileKeyUtil
import com.beldex.libbchat.utilities.ProfilePictureUtilities
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.truncateIdForDisplay
import com.thoughtcrimes.securesms.avatar.AvatarSelection
import com.thoughtcrimes.securesms.components.ProfilePictureView
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.ComposeDialogContainer
import com.thoughtcrimes.securesms.compose_utils.DialogType
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.mms.GlideApp
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.permissions.Permissions
import com.thoughtcrimes.securesms.profiles.ProfileMediaConstraints
import com.thoughtcrimes.securesms.util.BitmapDecodingException
import com.thoughtcrimes.securesms.util.BitmapUtil
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities
import com.thoughtcrimes.securesms.util.FileProviderUtil
import com.thoughtcrimes.securesms.util.QRCodeUtilities
import com.thoughtcrimes.securesms.util.toPx
import com.thoughtcrimes.securesms.wallet.CheckOnline
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityMyProfileBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.successUi
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Date
import java.util.regex.Pattern

@AndroidEntryPoint
class MyProfileActivity: AppCompatActivity() {

    private lateinit var glide: GlideRequests
    private var tempFile: File? = null
    private lateinit var binding: ActivityMyProfileBinding
    private val namePattern = Pattern.compile("[A-Za-z0-9]+")
    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    private fun getDisplayName(): String =
        TextSecurePreferences.getProfileName(this) ?: truncateIdForDisplay(hexEncodedPublicKey)
    val viewModel: MyAccountViewModel by viewModels()
    private var shareButtonLastClickTime: Long = 0

    var profileEditable:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(LayoutInflater.from(this))
        glide = GlideApp.with(this)
        profileEditable = intent.getBooleanExtra("profile_editable",false)
        setContentView(binding.root)
        setupProfilePictureView(binding.profilePictureView.root)
        binding.back.setOnClickListener {
            finish()
        }
        binding.share.setOnClickListener {
            if (SystemClock.elapsedRealtime() - shareButtonLastClickTime >= 1000) {
                shareButtonLastClickTime = SystemClock.elapsedRealtime()
                shareQRCode()
            }
        }
        binding.cameraView.isVisible = profileEditable
        binding.cameraView.apply {
            setOnClickListener {
                showEditProfilePictureUI()
            }
            setContent {
                BChatTheme {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.appColors.backgroundColor
                            )
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = "",
                            tint = MaterialTheme.appColors.editTextColor,
                            modifier = Modifier
                                .size(20.dp)
                        )
                    }
                }
            }
        }
        binding.composeView.setContent {
            BChatTheme {
                val state by viewModel.uiState.collectAsState()
                AccountHeader(
                    uiState =  state,
                    saveDisplayName = {
                        saveDisplayName(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 48.dp
                        ),
                    profileEditable
                )
            }
        }
    }

    private fun setupProfilePictureView(view: ProfilePictureView) {
        view.glide = glide
        view.apply {
            publicKey = hexEncodedPublicKey
            displayName = getDisplayName()
            isLarge = true
            update()
        }
    }

    private fun shareQRCode() {
        val directory = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = "$hexEncodedPublicKey.png"
        val file = File(directory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        val size = toPx(280, resources)
        val qrCode = QRCodeUtilities.encode(hexEncodedPublicKey, size, isInverted = false, hasTransparentBackground = false)
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

    private fun showEditProfilePictureUI() {
        val dialog = ComposeDialogContainer(
            dialogType = DialogType.UploadProfile,
            onConfirm = {
                startAvatarSelection()
            },
            onCancel = {
                removeAvatar()
            }
        )
        dialog.arguments = bundleOf(ComposeDialogContainer.EXTRA_ARGUMENT_1 to hexEncodedPublicKey, ComposeDialogContainer.EXTRA_ARGUMENT_2 to getDisplayName())
        dialog.show(supportFragmentManager, ComposeDialogContainer.TAG)
    }

    private fun startAvatarSelection() {
        if (CheckOnline.isOnline(this)) {
            Permissions.with(this)
                .request(Manifest.permission.CAMERA)
                .onAnyResult {
                    tempFile = AvatarSelection.startAvatarSelection(this, false, true)
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

    private fun removeAvatar() {
        updateProfile(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AvatarSelection.REQUEST_CODE_AVATAR -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }
                val outputFile = Uri.fromFile(File(cacheDir, "cropped"))
                var inputFile: Uri? = data?.data
                if (inputFile == null && tempFile != null) {
                    inputFile = Uri.fromFile(tempFile)
                }
                AvatarSelection.circularCropImage(
                    this,
                    inputFile,
                    outputFile,
                    R.string.CropImageActivity_profile_avatar
                )
            }
            AvatarSelection.REQUEST_CODE_CROP_IMAGE -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }
                AsyncTask.execute {
                    try {
                        val profilePictureToBeUploaded = BitmapUtil.createScaledBytes(
                            this,
                            AvatarSelection.getResultUri(data),
                            ProfileMediaConstraints()
                        ).bitmap
                        Handler(Looper.getMainLooper()).post {
                            updateProfile(true,profilePictureToBeUploaded)
                        }
                    } catch (e: BitmapDecodingException) {
                        e.printStackTrace()
                    }
                }
            }
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

    private fun updateProfile(isUpdatingProfilePicture: Boolean, profilePicture: ByteArray? = null,
                              displayName: String? = null) {
        binding.loader.isVisible = true
        lifecycleScope.launch {
            delay(3000)
            binding.loader.isVisible = false
        }
        val promises = mutableListOf<Promise<*, Exception>>()
        if (displayName != null) {
            TextSecurePreferences.setProfileName(this, displayName)
            viewModel.refreshProfileName()
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
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this)
            }
        }
        compoundPromise.alwaysUi {
            if (isUpdatingProfilePicture) {
                binding.profilePictureView.root.recycle() // Clear the cached image before updating
                binding.profilePictureView.root.update()
            }
            binding.loader.isVisible = false
        }
    }

    private fun saveDisplayName(displayName: String): Boolean {
        if (displayName.isEmpty()) {
            Toast.makeText(
                this,
                R.string.activity_settings_display_name_missing_error,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (displayName.toByteArray().size > SSKEnvironment.ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
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
}
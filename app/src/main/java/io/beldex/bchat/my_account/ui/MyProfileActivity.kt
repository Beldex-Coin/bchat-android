package io.beldex.bchat.my_account.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
import com.beldex.libsignal.utilities.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.avatar.AvatarSelection
import io.beldex.bchat.components.ProfilePictureView
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ComposeDialogContainer
import io.beldex.bchat.compose_utils.DialogType
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.databinding.ActivityMyProfileBinding
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.profiles.ProfileMediaConstraints
import io.beldex.bchat.util.BitmapDecodingException
import io.beldex.bchat.util.BitmapUtil
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.util.FileProviderUtil
import io.beldex.bchat.util.QRCodeUtilities
import io.beldex.bchat.util.toPx
import io.beldex.bchat.wallet.CheckOnline
import kotlinx.coroutines.Dispatchers
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

    private lateinit var glide: RequestManager
    private var tempFile: File? = null
    private lateinit var binding: ActivityMyProfileBinding
    private val namePattern = Pattern.compile("[A-Za-z0-9\\s]+")
    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    private fun getDisplayName(): String =
        TextSecurePreferences.getProfileName(this) ?: truncateIdForDisplay(hexEncodedPublicKey)
    val viewModel: MyAccountViewModel by viewModels()
    private var shareButtonLastClickTime: Long = 0

    private var profileEditable:Boolean = false
    private fun isBnsHolder():String? = TextSecurePreferences.getIsBNSHolder(this)

    private val TAG = "MyProfileActivity"
    private val onAvatarCropped = registerForActivityResult(CropImageContract()) { result ->
        when {
            result.isSuccessful -> {
                Log.i(TAG, result.getUriFilePath(this).toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val profilePictureToBeUploaded =
                            BitmapUtil.createScaledBytes(
                                this@MyProfileActivity,
                                result.getUriFilePath(this@MyProfileActivity).toString(),
                                ProfileMediaConstraints()
                            ).bitmap
                        launch(Dispatchers.Main) {
                            TextSecurePreferences.setIsLocalProfile(this@MyProfileActivity,false)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(LayoutInflater.from(this))
        glide = Glide.with(this)
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
        binding.cameraView.isVisible = profileEditable && isBnsHolder().isNullOrEmpty()
        binding.isBnsHolderCameraView.isVisible = profileEditable && !isBnsHolder().isNullOrEmpty()
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
                                color = MaterialTheme.appColors.profileCameraIconBackground
                            )
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_camera_edit),
                            contentDescription = "",
                            tint = MaterialTheme.appColors.editTextColor,
                            modifier = Modifier
                                .size(20.dp)
                        )
                    }
                }
            }
        }
        binding.isBnsHolderCameraView.apply {
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
                                color = MaterialTheme.appColors.profileCameraIconBackgroundWithBnsTag
                            )
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_camera_edit),
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
                        .fillMaxWidth(),
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
            update(displayName)
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
                    tempFile = avatarSelection.startAvatarSelection(
                        includeClear=false,
                        attemptToIncludeCamera=true
                    )
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

    private fun removeAvatar() {
        val latestName = TextSecurePreferences.getProfileName(this)
        /*val profile =TextSecurePreferences.getLocalNumber(this)?.let {
            val sizeInPX =
                    resources.getDimensionPixelSize(R.dimen.small_profile_picture_size)
            AvatarPlaceholderGenerator.generate(
                    this,
                    sizeInPX,
                    it,
                    latestName
            ).bitmap

        }
        val profilePictureToBeUploaded = BitmapUtil.toByteArray(profile)*/
        TextSecurePreferences.setIsLocalProfile(this,true)
        updateProfile(true, null,displayName = latestName)
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
                binding.profilePictureView.root.update(displayName)
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
       /*val profile =TextSecurePreferences.getLocalNumber(this)?.let {
           val sizeInPX =
                   resources.getDimensionPixelSize(R.dimen.small_profile_picture_size)
           AvatarPlaceholderGenerator.generate(
               this,
               sizeInPX,
                   it,
               displayName
       ).bitmap

       }
        val profilePictureToBeUploaded = BitmapUtil.toByteArray(profile)*/
        val checkGalleryProfile = TextSecurePreferences.getIsLocalProfile(this)
        updateProfile(checkGalleryProfile,null, displayName = displayName)
        return true
    }
}
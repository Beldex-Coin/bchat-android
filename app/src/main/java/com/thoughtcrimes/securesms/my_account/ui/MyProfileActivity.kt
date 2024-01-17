package com.thoughtcrimes.securesms.my_account.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.beldex.libbchat.avatars.AvatarHelper
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.ProfileKeyUtil
import com.beldex.libbchat.utilities.ProfilePictureUtilities
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.ExternalStorageUtil
import com.thoughtcrimes.securesms.avatar.AvatarSelection
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.permissions.Permissions
import com.thoughtcrimes.securesms.profiles.ProfileMediaConstraints
import com.thoughtcrimes.securesms.util.BitmapDecodingException
import com.thoughtcrimes.securesms.util.BitmapUtil
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.successUi
import java.io.File
import java.security.SecureRandom
import java.util.Date

@AndroidEntryPoint
class MyProfileActivity: AppCompatActivity() {

    private var tempFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme(
                darkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                Surface {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        val navController = rememberNavController()
                        val viewModel: MyAccountViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = MaterialTheme.appColors.editTextColor,
                                    modifier = Modifier
                                        .clickable {
                                            finish()
                                        }
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = stringResource(R.string.my_account),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.appColors.editTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.appColors.backgroundColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                MyAccountScreen(
                                    uiState = uiState,
                                    startAvatarSelection = {
                                        startAvatarSelection()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startAvatarSelection() {
        tempFile = File.createTempFile("avatar-capture", ".jpg",
            ExternalStorageUtil.getImageDir(this)
        )
        val intent = AvatarSelection.createAvatarSelectionIntent(this, tempFile, false)
        startActivityForResult(intent, AvatarSelection.REQUEST_CODE_AVATAR)
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
//        binding.loader.isVisible = true
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
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this)
            }
        }
        compoundPromise.alwaysUi {
//            if (displayName != null) {
//                binding.btnGroupNameDisplay.text = displayName
//            }
//            if (isUpdatingProfilePicture) {
//                binding.profilePictureView.root.recycle() // Clear the cached image before updating
//                binding.profilePictureView.root.update()
//            }
//            binding.loader.isVisible = false
        }
    }
}
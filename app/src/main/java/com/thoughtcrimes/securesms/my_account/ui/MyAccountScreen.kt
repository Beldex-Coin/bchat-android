package com.thoughtcrimes.securesms.my_account.ui

import android.Manifest
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureComponent
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureMode
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.checkAndRequestPermissions
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.my_account.ui.dialogs.PermissionSettingDialog
import com.thoughtcrimes.securesms.my_account.ui.dialogs.ProfilePicturePopup
import com.thoughtcrimes.securesms.util.QRCodeUtilities
import com.thoughtcrimes.securesms.util.copyToClipBoard
import com.thoughtcrimes.securesms.util.isValidString
import com.thoughtcrimes.securesms.util.toPx
import com.thoughtcrimes.securesms.wallet.CheckOnline
import io.beldex.bchat.R
import java.io.File

@Composable
fun MyAccountScreen(
    uiState: MyAccountViewModel.UIState = MyAccountViewModel.UIState(),
    startAvatarSelection: () -> Unit
) {
    val context = LocalContext.current
    val profileSize = ProfilePictureMode.LargePicture.size
    val requiredPermission = arrayOf(Manifest.permission.CAMERA)
    var capturedFile: File? = null
    var showPictureDialog by remember {
        mutableStateOf(false)
    }
    var showPermissionDialog by remember {
        mutableStateOf(false)
    }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.all { it.value }) {

        } else {
            showPermissionDialog = true
        }
    }
    val avatarLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        println(">>>>>data:${it.data}---${it.resultCode}")
//        if (it.resultCode == Activity.RESULT_OK) {
//            val outputFile = Uri.fromFile(File(cacheDir, "cropped"))
//            var inputFile: Uri? = it.data?.data
//            if (inputFile == null && capturedFile != null) {
//                inputFile = Uri.fromFile(capturedFile)
//            }
//            AvatarSelection.circularCropImage(
//                context as Activity,
//                inputFile,
//                outputFile,
//                R.string.CropImageActivity_profile_avatar
//            )
//        }
    }
//    fun startAvatarSelection() {
//        capturedFile = File.createTempFile("avatar-capture", ".jpg", getImageDir(context))
//        val intent = AvatarSelection.createAvatarSelectionIntent(context, capturedFile, false)
//        avatarLauncher.launch(intent)
//    }
    fun checkForPermission() {
        // Ask for an optional camera permission.
        if (CheckOnline.isOnline(context)) {
            checkAndRequestPermissions(
                context = context,
                permissions = requiredPermission,
                launcher = launcher,
                onGranted = {
                    startAvatarSelection()
                }
            )
        } else {
            Toast.makeText(
                context,
                context.resources.getString(R.string.please_check_your_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (showPictureDialog) {
        ProfilePicturePopup(
            publicKey = uiState.publicKey,
            displayName = uiState.profileName ?: "",
            onDismissRequest = {
                showPictureDialog = false
            },
            removePicture = {
                showPictureDialog = false
            },
            uploadPicture = {
                showPictureDialog = false
                checkForPermission()
            }
        )
    }
    if (showPermissionDialog) {
        PermissionSettingDialog(
            message = "BChat needs library access to continue. You can enable access in the Settings page",
            onDismissRequest = {},
            gotoSettings = {}
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = profileSize / 2
                )
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = profileSize / 2
                    )
            ) {
                AccountHeader(
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = profileSize / 2
                        )
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clickable {
                            showPictureDialog = true
                        }
                ) {
                    ProfilePictureComponent(
                        publicKey = uiState.publicKey,
                        displayName = uiState.profileName ?: "",
                        additionalPublicKey = uiState.additionalPublicKey,
                        additionalDisplayName = uiState.additionalDisplayName,
                        containerSize = profileSize,
                        pictureMode = ProfilePictureMode.LargePicture
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.appColors.backgroundColor
                            )
                            .align(Alignment.BottomEnd)
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

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth(0.8f)
        ) {
            Icon(
                Icons.Outlined.Share,
                contentDescription = ""
            )

            Text(
                text = stringResource(id = R.string.share),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                ),
                modifier = Modifier
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun AccountHeader(
    uiState: MyAccountViewModel.UIState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val beldexAddress by remember {
        mutableStateOf(
            IdentityKeyUtil.retrieve(
            context,
            IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF
        ))
    }
    val copyToClipBoard: (String, String) -> Unit = { label, content ->
        context.copyToClipBoard(label, content)
    }
    var editingName by remember {
        mutableStateOf(false)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!editingName) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                Text(
                    text = uiState.profileName ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.titleTextColor
                    ),
                    modifier = Modifier
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_name),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.iconColor,
                    modifier = Modifier
                        .clickable {
                            editingName = true
                        }
                )
            }
        } else {
            var profileName by remember {
                mutableStateOf(uiState.profileName ?: "")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                BasicTextField(
                    value = profileName,
                    onValueChange = { text: String  ->
                        profileName = text
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight(700)
                    ),
                    decorationBox = { innerTextField ->
                        Column(
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            if (profileName.isEmpty()) {
                                Text(
                                    text = "Display Name",
                                    style = MaterialTheme.typography.bodyMedium.copy(

                                    )
                                )
                            } else {
                                innerTextField()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.appColors.primaryButtonColor
                        )
                        .clickable {
                            editingName = false
                        }
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.chatid),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.appColors.primaryButtonColor
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        KeyContainer(
            key = uiState.publicKey,
            onCopy = {
                copyToClipBoard("Chat Id", uiState.publicKey)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.beldex_address),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.appColors.beldexAddressColor
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        KeyContainer(
            key = beldexAddress,
            onCopy = {
                copyToClipBoard("Chat Id", beldexAddress)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            if (uiState.publicKey.isValidString()) {
                val resources = LocalContext.current.resources
                val size = toPx(280, resources)
                val bitMap by remember {
                    mutableStateOf(QRCodeUtilities.encode(uiState.publicKey, size, isInverted = false, hasTransparentBackground = false))
                }
                Image(
                    bitmap = bitMap.asImageBitmap(),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .padding(
                            16.dp
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                )
            }
        }
    }
}

@Composable
fun KeyContainer(
    key: String?,
    onCopy: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.cardBackground
            ),
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = key ?: "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(
                        16.dp
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.appColors.primaryButtonColor
                )
                .clickable {
                    onCopy()
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "",
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AccountHeaderPreview() {
    BChatTheme {
        MyAccountScreen(
            uiState = MyAccountViewModel.UIState(
                profileName = "Testing UI"
            ),
            startAvatarSelection = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun AccountHeaderPreviewLight() {
    BChatTheme {
        MyAccountScreen(
            uiState = MyAccountViewModel.UIState(
                profileName = "Testing UI"
            ),
            startAvatarSelection = {}
        )
    }
}
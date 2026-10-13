package io.beldex.bchat.my_account.ui.dialogs

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.compose_utils.PrimaryButton
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image

@Composable
fun ProfilePicturePopup(
    publicKey: String,
    displayName: String,
    onDismissRequest: () -> Unit,
    closePopUP: () -> Unit,
    removePicture: () -> Unit,
    uploadPicture: () -> Unit
) {
    val context = LocalContext.current
    var profilePictureStatus by remember {
        mutableStateOf(true)
    }
    val recipient = Recipient.from(context, Address.fromSerialized(publicKey), false)
    val signalProfilePicture = recipient.contactPhoto
    val avatar = (signalProfilePicture as? ProfileContactPhoto)?.avatarObject
    val avatar1 = (signalProfilePicture as? ProfileContactPhoto)?.hashCode()
    Log.d("Profile_Picture-> ","$avatar1,$avatar")
    if (signalProfilePicture != null && avatar != "0" && avatar != "") {
        profilePictureStatus = true
        Log.d("Profile_Picture-> ","Profile is there")
    }else{
        profilePictureStatus = false
        Log.d("Profile_Picture-> ","Profile is not there")
    }
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.activity_settings_profile_picture),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.titleTextColor,
                        fontFamily = OpenSans
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.close),
                    tint = MaterialTheme.appColors.iconTint,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable {
                            closePopUP()
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box {
                    ProfilePictureComponent(
                        publicKey = publicKey,
                        displayName = displayName,
                        containerSize = ProfilePictureMode.LargePicture.size,
                        pictureMode = ProfilePictureMode.LargePicture
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_camera_badge),
                        contentDescription = stringResource(id = R.string.activity_settings_profile_picture),
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.BottomEnd)
                            .clickable { uploadPicture() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                PrimaryButton(
                    onClick = removePicture,
                    enabled = profilePictureStatus,
                    shape = notchedCornerShape(12.dp),
                    containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground,
                    contentColor = MaterialTheme.appColors.onboardingSecondaryButtonText,
                    disabledContainerColor = MaterialTheme.appColors.optionalTextFieldBackground,
                    disabledContentColor = MaterialTheme.appColors.disabledButtonContent,
                    border = BorderStroke(1.dp, if (profilePictureStatus) MaterialTheme.appColors.onboardingSecondaryButtonBorder else MaterialTheme.appColors.optionalTextFieldBackground),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1.13f).height(52.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.activity_settings_remove),
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }

                PrimaryButton(
                    onClick = uploadPicture,
                    shape = notchedCornerShape(12.dp),
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color(0xFF0B0B0B),
                    border = BorderStroke(1.4.dp, Color(0xFF0A370A)),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.save),
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ProfilePicturePopupPreview() {
    ProfilePicturePopup(
        publicKey = "",
        displayName = "Demo Account",
        onDismissRequest = {},
        closePopUP = {},
        removePicture = {},
        uploadPicture = {}
    )
}
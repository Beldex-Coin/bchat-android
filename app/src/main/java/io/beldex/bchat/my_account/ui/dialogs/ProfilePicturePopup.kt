package io.beldex.bchat.my_account.ui.dialogs

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

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
                        color = MaterialTheme.appColors.primaryButtonColor
                    ),
                    textAlign = TextAlign.Center,
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
                ProfilePictureComponent(
                    publicKey = publicKey,
                    displayName = displayName,
                    containerSize = ProfilePictureMode.LargePicture.size,
                    pictureMode = ProfilePictureMode.LargePicture
                )
               /* Box {
                    ProfilePictureComponent(
                        publicKey = publicKey,
                        displayName = displayName,
                        containerSize = ProfilePictureMode.LargePicture.size,
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
                }*/
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = removePicture,
                    enabled = profilePictureStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.secondaryButtonColor,
                        disabledContainerColor = MaterialTheme.appColors.optionalTextfieldBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.activity_settings_remove),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if(profilePictureStatus) MaterialTheme.appColors.secondaryContentColor else MaterialTheme.appColors.disabledButtonContent
                        )
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                Button(
                    onClick = uploadPicture,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.primaryButtonColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.activity_settings_upload),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White
                        )
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
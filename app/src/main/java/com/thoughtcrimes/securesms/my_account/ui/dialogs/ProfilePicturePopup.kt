package com.thoughtcrimes.securesms.my_account.ui.dialogs

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureComponent
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureMode
import com.thoughtcrimes.securesms.compose_utils.appColors
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
                Box {
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = removePicture,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.secondaryButtonColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.activity_settings_remove),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

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
                        style = MaterialTheme.typography.bodyMedium.copy(
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
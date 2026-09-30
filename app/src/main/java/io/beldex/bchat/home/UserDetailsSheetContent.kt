package io.beldex.bchat.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape

/**
 * Revamp_2026 contact-details bottom sheet content (Figma `profile_Detail` 7546:2219 / view state,
 * `profile_Detail_1` 7546:2372 / name-edit state), hosted by [UserDetailsBottomSheet] via a
 * `ComposeView`. The avatar stays circular (`ProfilePictureComponent`, shared across every other
 * restyled screen) rather than Figma's one-off notched-corner avatar treatment, for visual
 * consistency with the rest of the app.
 */
@Composable
fun UserDetailsSheetContent(
    publicKey: String,
    name: String,
    isEditingName: Boolean,
    nicknameInput: String,
    onNicknameInputChange: (String) -> Unit,
    onEditNameClick: () -> Unit,
    onCancelEditClick: () -> Unit,
    onSaveNicknameClick: () -> Unit,
    showBchatIdAndMessageButton: Boolean,
    onCopyBchatId: () -> Unit,
    onMessageClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.userDetailsSheetBackground)
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        ProfilePictureComponent(
            publicKey = publicKey,
            displayName = name,
            containerSize = ProfilePictureMode.LargePicture.size,
            pictureMode = ProfilePictureMode.LargePicture
        )

        if (isEditingName) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                TextField(
                    value = nicknameInput,
                    onValueChange = { if (it.length <= 26) onNicknameInputChange(it) },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.fragment_user_details_bottom_sheet_edit_text_hint),
                            fontFamily = OpenSans,
                            fontSize = 18.sp
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.appColors.userDetailsNameColor,
                        fontFamily = OpenSans,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSaveNicknameClick() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.appColors.userDetailsNameColor,
                        unfocusedIndicatorColor = MaterialTheme.appColors.userDetailsNameColor,
                        cursorColor = MaterialTheme.appColors.userDetailsNameColor
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_close_circle),
                    contentDescription = stringResource(R.string.cancel),
                    tint = MaterialTheme.appColors.userDetailsCancelIconColor,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.appColors.userDetailsCancelBackground)
                        .padding(4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancelEditClick
                        )
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_done_circle),
                    contentDescription = stringResource(R.string.save),
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.appColors.userDetailsConfirmBackground)
                        .padding(4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSaveNicknameClick
                        )
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = name,
                    color = MaterialTheme.appColors.userDetailsNameColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_edit_group_name),
                    contentDescription = stringResource(R.string.edit_title),
                    tint = MaterialTheme.appColors.userDetailsEditIconColor,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onEditNameClick
                        )
                )
            }
        }

        if (showBchatIdAndMessageButton) {
            Text(
                text = publicKey,
                color = MaterialTheme.appColors.userDetailsBchatIdText,
                fontFamily = RobotoMono,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.appColors.userDetailsBchatIdBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCopyBchatId
                    )
                    .padding(16.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(notchedCornerShape(14.5.dp))
                    .background(MaterialTheme.appColors.onboardingPrimaryButtonBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMessageClick
                    )
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.ConversationActivity_message),
                    color = MaterialTheme.appColors.onboardingPrimaryButtonText,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

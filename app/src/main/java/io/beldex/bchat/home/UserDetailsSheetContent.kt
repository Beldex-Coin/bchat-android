package io.beldex.bchat.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import io.beldex.bchat.compose_utils.PrimaryButton
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
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A1A1A), androidx.compose.ui.graphics.Color(0xFF0B0B0B)),
                    radius = 900f
                )
            )
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
                        color = MaterialTheme.appColors.editTextColor,
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
                        focusedIndicatorColor = MaterialTheme.appColors.editTextColor,
                        unfocusedIndicatorColor = MaterialTheme.appColors.editTextColor,
                        cursorColor = MaterialTheme.appColors.editTextColor
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_close_circle),
                    contentDescription = stringResource(R.string.cancel),
                    tint = MaterialTheme.appColors.userDetailsCancelIconColor,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(26.dp)
                        .background(androidx.compose.ui.graphics.Color(0xFF222222), notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchSmall))
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
                    tint = MaterialTheme.appColors.primaryButtonIconColor,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(26.dp)
                        .background(MaterialTheme.appColors.userDetailsConfirmBackground, notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchSmall))
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
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_edit_group_name),
                    contentDescription = stringResource(R.string.edit_title),
                    tint = MaterialTheme.appColors.userDetailsConfirmBackground,
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
                    .background(androidx.compose.ui.graphics.Color(0xFF111111))
                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFF333333))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCopyBchatId
                    )
                    .padding(16.dp)
            )

            PrimaryButton(
                onClick = onMessageClick,
                shape = notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchLarge),
                containerColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                contentColor = androidx.compose.ui.graphics.Color(0xFF0B0B0B),
                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF0A370A)),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
            ) {
                Text(
                    text = stringResource(R.string.write_message),
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

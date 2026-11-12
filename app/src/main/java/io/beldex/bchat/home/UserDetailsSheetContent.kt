package io.beldex.bchat.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.CursorGapTransformation
import io.beldex.bchat.compose_utils.NotchCorner
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.compose_utils.singleCornerCutShape

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
            .border(0.5.dp, MaterialTheme.appColors.optionBorder, RectangleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(MaterialTheme.appColors.dialogPanelGradientStart, MaterialTheme.appColors.dialogPanelGradientEnd),
                    radius = 900f
                )
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        ProfilePictureComponent(
            publicKey = publicKey,
            displayName = name,
            containerSize = ProfilePictureMode.LargePicture.size,
            pictureMode = ProfilePictureMode.LargePicture
        )

        if (isEditingName) {
            // Figma profile_Detail_1: underlined name centred in the row, cancel / save tiles on the right.
            var fieldValue by remember {
                mutableStateOf(TextFieldValue(nicknameInput, TextRange(nicknameInput.length)))
            }
            if (fieldValue.text != nicknameInput) {
                fieldValue = TextFieldValue(nicknameInput, TextRange(nicknameInput.length))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                BasicTextField(
                    value = fieldValue,
                    onValueChange = {
                        if (it.text.length <= 26) {
                            fieldValue = it
                            onNicknameInputChange(it.text)
                        }
                    },
                    textStyle = TextStyle(
                        color = MaterialTheme.appColors.editTextColor,
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    ),
                    singleLine = true,
                    visualTransformation = CursorGapTransformation,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSaveNicknameClick() }),
                    cursorBrush = SolidColor(MaterialTheme.appColors.editTextColor),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (nicknameInput.isEmpty()) {
                                Text(
                                    text = name,
                                    fontFamily = OpenSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.appColors.editTextColor.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .widthIn(min = 1.dp, max = 190.dp)
                        .padding(vertical = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(23.dp)
                            .background(
                                MaterialTheme.appColors.nicknameCancelBackground,
                                singleCornerCutShape(BChatDimens.NotchSmall, NotchCorner.TopRight)
                            )
                            .border(
                                0.36.dp,
                                MaterialTheme.appColors.nicknameCancelBorder,
                                singleCornerCutShape(BChatDimens.NotchSmall, NotchCorner.TopRight)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCancelEditClick
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.appColors.cancelIconTint,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(22.dp)
                            .background(MaterialTheme.appColors.userDetailsConfirmBackground, RectangleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSaveNicknameClick
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_done),
                            contentDescription = stringResource(R.string.save),
                            tint = MaterialTheme.appColors.dialogPositiveButtonText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
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
                    painter = painterResource(id = R.drawable.ic_user_detail_edit_pencil),
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
                    .background(MaterialTheme.appColors.selectedItemBackground)
                    .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCopyBchatId
                    )
                    .padding(16.dp)
            )

            PrimaryButton(
                onClick = onMessageClick,
                shape = notchedCornerShape(BChatDimens.NotchLarge),
                containerColor = MaterialTheme.appColors.dialogPositiveButtonBackground,
                contentColor = MaterialTheme.appColors.dialogPositiveButtonText,
                border = BorderStroke(1.dp, MaterialTheme.appColors.dialogPositiveButtonBorder),
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

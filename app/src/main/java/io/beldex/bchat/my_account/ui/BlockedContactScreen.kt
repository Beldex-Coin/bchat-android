package io.beldex.bchat.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogCancelButton
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.DialogPrimaryButton
import io.beldex.bchat.compose_utils.NotchCorner
import io.beldex.bchat.compose_utils.NotchedAvatarShape
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.compose_utils.singleCornerCutShape

@Composable
fun BlockedContactScreen(
    blockedList: List<Recipient>,
    selectedList: List<Recipient>,
    multiSelectActivated: Boolean,
    unBlockSingleContact: (Recipient) -> Unit,
    unBlockMultipleContacts: () -> Unit,
    addRemoveContactToList: (Recipient, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmationDialog by remember {
        mutableStateOf(false)
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (showConfirmationDialog) {
        DialogContainer(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            onDismissRequest = {
                showConfirmationDialog = false
            },
            wrapContentWidth = isLandscape,
        ) {
            val context = LocalContext.current
            val title = if (selectedList.size == 1) {
                stringResource(id = R.string.Unblock_dialog__title_single).format(selectedList.first().name)
            } else {
                stringResource(id = R.string.Unblock_dialog__title_multiple)
            }
            val message = if (selectedList.size == 1) {
                stringResource(id = R.string.Unblock_dialog__message).format(selectedList.first().name)
            } else {
                val stringBuilder = StringBuilder()
                val iterator = selectedList.iterator()
                var numberAdded = 0
                while (iterator.hasNext() && numberAdded < 3) {
                    val nextRecipient = iterator.next()
                    if (numberAdded > 0) stringBuilder.append(", ")

                    stringBuilder.append(nextRecipient.name)
                    numberAdded++
                }
                val overflow = selectedList.size - numberAdded
                if (overflow > 0) {
                    stringBuilder.append(" ")
                    val string = context.resources.getQuantityString(R.plurals.Unblock_dialog__message_multiple_overflow, overflow)
                    stringBuilder.append(string.format(overflow))
                }
                stringResource(id = R.string.Unblock_dialog__message, stringBuilder.toString())
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DialogCancelButton(
                        text = stringResource(id = R.string.cancel),
                        onClick = { showConfirmationDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    DialogPrimaryButton(
                        text = stringResource(id = R.string.continue_2),
                        onClick = {
                            unBlockMultipleContacts()
                            showConfirmationDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                count = blockedList.size,
                key = {
                    "${blockedList[it].address.contactIdentifier()}_$it"
                }
            ) { index ->
                val contact = blockedList[index]
                BlockedContactItem(
                    contact = contact,
                    multiSelectActivated = multiSelectActivated,
                    isSelected = selectedList.contains(contact),
                    unblockContact = {
                        unBlockSingleContact(contact)
                    },
                    selectContact = {
                        addRemoveContactToList(contact, it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        if (multiSelectActivated && selectedList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = {
                    showConfirmationDialog = true
                },
                shape = notchedCornerShape(BChatDimens.NotchLarge),
                containerColor = MaterialTheme.appColors.editTextColor,
                contentColor = MaterialTheme.appColors.backgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.unblock_selected),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.backgroundColor,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight(600),
                        fontSize = 16.sp
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun BlockedContactItem(
    contact: Recipient,
    multiSelectActivated: Boolean,
    isSelected: Boolean,
    unblockContact: () -> Unit,
    selectContact: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = if (multiSelectActivated && isSelected) {
        notchedCornerShape(BChatDimens.NotchLarge)
    } else {
        RectangleShape
    }
    Card(
        shape = rowShape,
        border = if (multiSelectActivated) androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00BC33).copy(alpha = 0.5f) else Color(0xFF444444)
        ) else null,
        colors = CardDefaults.cardColors(
            containerColor = if(isSelected) Color(0xFF111111) else Color.Transparent
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {

            Box(
                    modifier=Modifier
                            .height(36.dp)
                            .width(36.dp)
                            .clip(NotchedAvatarShape),
                    contentAlignment=Alignment.Center,
            ) {
                ProfilePictureComponent(
                        publicKey=contact.address.toString(),
                        displayName=contact.name.toString(),
                        containerSize=36.dp,
                        pictureMode=ProfilePictureMode.SmallPicture)
            }
            Text(
                text = contact.name ?: "",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OpenSans,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 14.dp
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (multiSelectActivated) {
                CircleCheck(
                    checked = isSelected,
                    onClick = { selectContact(!isSelected) }
                )
            } else {
            val unblockBg = if (isSystemInDarkTheme()) MaterialTheme.appColors.editTextColor else Color(0xFFF8F8F8)
            val unblockText = if (isSystemInDarkTheme()) MaterialTheme.appColors.backgroundColor else MaterialTheme.appColors.editTextColor
            Text(
                text = stringResource(R.string.ConversationActivity_unblock),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight(600),
                    fontSize = 12.sp
                ),
                color = unblockText,
            modifier = Modifier
                .background(
                    color = unblockBg,
                    shape = singleCornerCutShape(BChatDimens.NotchSmall, NotchCorner.TopRight)
                )
                .clickable {
                    unblockContact()
                }
                .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            }
        }
    }
}

@Preview
@Composable
fun BlockedContactScreenPreview() {
    BChatTheme {
        BlockedContactScreen(
            blockedList = emptyList(),
            selectedList = emptyList(),
            multiSelectActivated = false,
            unBlockSingleContact = {},
            unBlockMultipleContacts = {},
            addRemoveContactToList = {_,_ ->},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BlockedContactScreenPreviewDark() {
    BChatTheme {
        BlockedContactScreen(
            blockedList = emptyList(),
            selectedList = emptyList(),
            multiSelectActivated = false,
            unBlockSingleContact = {},
            unBlockMultipleContacts = {},
            addRemoveContactToList = {_,_ ->},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

/** Figma tick: a green rounded square that fills with a dark check when selected. */
@Composable
private fun CircleCheck(checked: Boolean, onClick: () -> Unit) {
    val green = Color(0xFF00BC33)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 1.35.dp.toPx()
            // Figma "Iconex/Light/Check": an 18dp rounded square outlined in green.
            val corner = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            if (checked) {
                drawRoundRect(
                    color = green,
                    cornerRadius = corner
                )
                val path = Path().apply {
                    moveTo(size.width * 0.27f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.68f)
                    lineTo(size.width * 0.74f, size.height * 0.34f)
                }
                drawPath(path, Color(0xFF0B0B0B), style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round
                )
                )
            } else {
                drawRoundRect(
                    color = green,
                    cornerRadius = corner,
                    style = Stroke(width = stroke)
                )
            }
        }
    }
}

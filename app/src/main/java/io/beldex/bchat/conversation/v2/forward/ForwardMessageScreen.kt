package io.beldex.bchat.conversation.v2.forward

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.NotchedAvatarShape
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono

/**
 * Revamp_2026 "Forward" screen (Figma Select_contact 7546:9857 dark / 7762:12730 white):
 * header, "SEARCH CONTACTS" field, Secret group / Social group / Contacts sections of selectable
 * rows with a green checkbox, and a corner-cut "Forward to selected" button.
 */
@Composable
fun ForwardMessageScreen(
    state: ForwardState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggle: (ForwardTarget) -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textPrimary = colorResource(R.color.disappear_icon_color)      // #EBEBEB / #222222
    val sectionColor = colorResource(R.color.contact_card_action_text)  // #ACACAC / #555555
    val hintColor = colorResource(R.color.chat_input_hint)              // #737373 / #8D8D8D

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        // Header (Figma 7546:9877): back + "Forward", #0B0B0B / #F0F0F0 bar with a bottom border.
        val headerBorder = colorResource(R.color.chat_header_border)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(colorResource(R.color.chat_header_background))
                .drawBehind {
                    drawRect(headerBorder, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
                }
                .padding(horizontal = 12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.back),
                tint = textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onBack)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.media_preview__forward_title),
                fontFamily = OpenSans,
                fontSize = 16.sp,
                color = textPrimary
            )
        }

        // Search (Figma 7546:9938): 60dp, #111111 / #FFFFFF at 50% with top + bottom hairlines.
        val searchBorder = colorResource(R.color.contact_card_inner_bar)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(colorResource(R.color.reply_draft_card_background).copy(alpha = 0.5f))
                .drawBehind {
                    val stroke = 0.75.dp.toPx()
                    drawRect(searchBorder, Offset.Zero, Size(size.width, stroke))
                    drawRect(searchBorder, Offset(0f, size.height - stroke), Size(size.width, stroke))
                }
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home_search),
                contentDescription = null,
                tint = hintColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(13.dp))
            Box(Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_contact).uppercase(),
                        fontFamily = OpenSans,
                        fontSize = 14.sp,
                        color = hintColor,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = OpenSans, fontSize = 14.sp, color = textPrimary),
                    cursorBrush = SolidColor(colorResource(R.color.home_row_timestamp_unread)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> CircularProgressIndicator(
                    color = colorResource(R.color.home_row_timestamp_unread),
                    modifier = Modifier.align(Alignment.Center)
                )
                state.noteToSelf.isEmpty() && state.secretGroups.isEmpty() && state.socialGroups.isEmpty() && state.contacts.isEmpty() -> Text(
                    text = stringResource(R.string.forward_no_chats),
                    fontFamily = OpenSans,
                    fontSize = 14.sp,
                    color = hintColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> {
                    val secretTitle = stringResource(R.string.secret_group_kind)
                    val socialTitle = stringResource(R.string.social_group_kind)
                    val contactsTitle = stringResource(R.string.fragment_contact_selection_contacts_title)
                    LazyColumn(
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Note to Self: always first, no section title.
                        items(state.noteToSelf, key = { "note_to_self" }) { target ->
                            ForwardRow(target, target.threadId in state.selected, onToggle)
                        }
                        section(secretTitle, state.secretGroups, state.selected, sectionColor, onToggle)
                        section(socialTitle, state.socialGroups, state.selected, sectionColor, onToggle)
                        section(contactsTitle, state.contacts, state.selected, sectionColor, onToggle)
                    }
                }
            }
        }

        ForwardButton(
            enabled = state.canForward,
            sending = state.sending,
            onClick = onForward,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 15.dp)
        )
    }
}

private fun LazyListScope.section(
    title: String,
    targets: List<ForwardTarget>,
    selected: Set<Long>,
    titleColor: Color,
    onToggle: (ForwardTarget) -> Unit
) {
    if (targets.isEmpty()) return
    item(key = "header_$title") {
        Text(
            text = title,
            fontFamily = RobotoMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = titleColor,
            modifier = Modifier.padding(top = 8.dp, bottom = 0.dp)
        )
    }
    items(targets, key = { it.threadId }) { target ->
        ForwardRow(target, target.threadId in selected, onToggle)
    }
}

/** Figma row (7546:9867): 52dp, 1dp #333333 / #DDDDDD border, filled #1A1A1A / #FFFFFF when selected. */
@Composable
private fun ForwardRow(target: ForwardTarget, isSelected: Boolean, onToggle: (ForwardTarget) -> Unit) {
    val rowFill = colorResource(R.color.reply_draft_card_background)
    val avatarBorder = colorResource(R.color.chat_header_background)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (isSelected) rowFill else Color.Transparent)
            .border(BorderStroke(1.dp, colorResource(R.color.recording_bar_border) /* #333333 / #D0D0D0 */))
            .clickable { onToggle(target) }
            .padding(horizontal = 12.dp)
    ) {
        if (target.kind == ForwardTarget.Kind.NOTE_TO_SELF) {
            // Same Note to Self icon as the New Chat screen, on a notched tile.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(NotchedAvatarShape)
                    .background(colorResource(R.color.media_overview_tab_selected), NotchedAvatarShape)
                    .border(1.dp, avatarBorder, NotchedAvatarShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_newchat_note),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else Box(
            // No clip: the BNS verified badge overhangs the avatar's bottom-right corner.
            modifier = Modifier.size(36.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val publicKey = target.recipient.address.serialize()
            val verified = remember(publicKey) { io.beldex.bchat.compose_utils.isBnsVerified(context, publicKey) }
            // Order: picture → outline → BNS badge, so the outline sits behind the verified tick.
            ProfilePictureComponent(
                publicKey = publicKey,
                displayName = target.name,
                containerSize = 36.dp,
                pictureMode = ProfilePictureMode.SmallPicture,
                showBnsBadge = false
            )
            Box(Modifier.matchParentSize().border(1.dp, avatarBorder, NotchedAvatarShape))
            if (verified) {
                io.beldex.bchat.compose_utils.BnsVerifiedBadge(
                    containerSize = 36.dp,
                    small = true,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = target.name,
            fontFamily = OpenSans,
            fontSize = 16.sp,
            color = colorResource(R.color.disappear_icon_color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            painter = painterResource(if (isSelected) R.drawable.ic_forward_checkbox_on else R.drawable.ic_forward_checkbox_off),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Figma "Forward to selected" (7546:9934): 58dp, top-right + bottom-left cut corners. */
@Composable
private fun ForwardButton(enabled: Boolean, sending: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = remember { ForwardButtonShape }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(if (enabled || sending) 1f else 0.4f)
            .clip(shape)
            .background(colorResource(R.color.read_button_background), shape)
            .border(1.dp, colorResource(R.color.chat_header_background), shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        if (sending) {
            CircularProgressIndicator(
                color = colorResource(R.color.chat_header_background),
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.forward_to_selected),
                fontFamily = RobotoMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = colorResource(R.color.chat_header_background)
            )
        }
    }
}

private object ForwardButtonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val trX = with(density) { 18.6.dp.toPx() }
        val trY = with(density) { 15.dp.toPx() }
        val blX = with(density) { 16.dp.toPx() }
        val blY = with(density) { 12.9.dp.toPx() }
        return Outline.Generic(Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width - trX, 0f)
            lineTo(size.width, trY)
            lineTo(size.width, size.height)
            lineTo(blX, size.height)
            lineTo(0f, size.height - blY)
            close()
        })
    }
}

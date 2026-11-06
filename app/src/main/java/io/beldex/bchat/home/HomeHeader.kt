package io.beldex.bchat.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.NotchedAvatarShape
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors

/**
 * Revamp_2026 Home header (Figma `home page` 7546:2060): "Chats" title with the profile avatar
 * (opens the settings drawer — no Figma redesign exists for the drawer itself, see Phase 2 plan
 * notes) and the connectivity warning banner ("You are not connected to the Hop...").
 */
@Composable
fun HomeHeader(
    publicKey: String,
    displayName: String,
    showConnectivityWarning: Boolean,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onProfileClick
                    )
            ) {
                if (hasProfilePhoto(publicKey)) {
                    Box(
                        modifier = Modifier
                            .size(29.dp)
                            .border(0.5.dp, selfAvatarStrokeBrush(), NotchedAvatarShape)
                    ) {
                        ProfilePictureComponent(
                            publicKey = publicKey,
                            displayName = displayName,
                            containerSize = 29.dp,
                            pictureMode = ProfilePictureMode.SmallPicture
                        )
                    }
                } else {
                    SelfAvatarPlaceholder(displayName = displayName)
                    if (!com.beldex.libbchat.utilities.TextSecurePreferences.getIsBNSHolder(LocalContext.current).isNullOrEmpty()) {
                        io.beldex.bchat.compose_utils.BnsVerifiedBadge(
                            containerSize = 29.dp,
                            small = true,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.activity_chat_settings_title),
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )

            // Self-contained onion-routing path status dot (own broadcast receivers/coloring
            // logic) — reused as-is rather than re-derived, matching Figma's small status dot
            // next to the title.
            AndroidView(
                factory = { ctx -> PathStatusView(ctx) },
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(10.dp)
            )
        }

        AnimatedVisibility(visible = showConnectivityWarning) {
            // Figma: red "danger" triangle left of the message, text directly on the page.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_warning_no_internet),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.hop_connection_error_message),
                    color = MaterialTheme.appColors.homeBannerText,
                    fontFamily = RobotoMono,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}


/** True when the user has an uploaded profile photo (same check ProfilePicture() uses). */
@Composable
private fun hasProfilePhoto(publicKey: String): Boolean {
    val context = LocalContext.current
    return remember(publicKey) {
        runCatching {
            val photo = Recipient.from(context, Address.fromSerialized(publicKey), false).contactPhoto
            val avatar = (photo as? ProfileContactPhoto)?.avatarObject
            photo != null && avatar != "0" && avatar != ""
        }.getOrDefault(false)
    }
}

@Composable
private fun isWhiteTheme(): Boolean = MaterialTheme.appColors.backgroundColor.luminance() > 0.5f

@Composable
private fun selfAvatarStrokeBrush(): Brush =
    if (isWhiteTheme()) SolidColor(Color(0xFF078720))
    else Brush.linearGradient(listOf(Color(0xFF00D239), Color(0xFF003A00)))

/**
 * Figma own-profile placeholder:
 *  - dark : fill 50% gradient #031203 → #002400 (38%) → #000E00, stroke #00D239 → #003A00, text #00BC33
 *  - white: fill 50% gradient #FFFFFF → #078720 (38%) → #FFFFFF, stroke #078720, text #078720
 */
@Composable
private fun SelfAvatarPlaceholder(displayName: String) {
    val white = isWhiteTheme()
    val fill = if (white) {
        Brush.horizontalGradient(
            0f to Color(0xFFFFFFFF), 0.38f to Color(0xFF078720), 1f to Color(0xFFFFFFFF)
        )
    } else {
        Brush.horizontalGradient(
            0f to Color(0xFF031203), 0.38f to Color(0xFF002400), 1f to Color(0xFF000E00)
        )
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(29.dp)
            .clip(NotchedAvatarShape)
            .background(if (white) Color.White else Color.Black)
            .background(fill, NotchedAvatarShape, alpha = 0.5f)
            .border(1.dp, selfAvatarStrokeBrush(), NotchedAvatarShape)
    ) {
        Text(
            text = selfInitials(displayName),
            color = if (white) Color(0xFF078720) else Color(0xFF00BC33),
            fontFamily = OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

/** Same rules as AvatarPlaceholderGenerator.extractLabel (kept local so it doesn't depend on that file). */
private fun selfInitials(name: String): String {
    val t = name.trim()
    if (t.isEmpty()) return "0"
    return if (t.length > 2 && t.startsWith("bd")) {
        t[2].toString()
    } else {
        val words = t.split(Regex("\\W")).filter { it.isNotEmpty() }
        if (words.size < 2) t.take(2) else words.take(2).joinToString("") { it.first().toString() }
    }.uppercase()
}

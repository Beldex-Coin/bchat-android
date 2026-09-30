package io.beldex.bchat.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
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
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            ProfilePictureComponent(
                publicKey = publicKey,
                displayName = displayName,
                containerSize = ProfilePictureMode.SmallPicture.size,
                pictureMode = ProfilePictureMode.SmallPicture,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onProfileClick
                    )
            )

            Text(
                text = stringResource(R.string.activity_chat_settings_title),
                color = MaterialTheme.appColors.homeTitleColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 12.dp)
            )

            // Self-contained onion-routing path status dot (own broadcast receivers/coloring
            // logic) — reused as-is rather than re-derived, matching Figma's small status dot
            // next to the title.
            AndroidView(
                factory = { ctx -> PathStatusView(ctx) },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(12.dp)
            )
        }

        AnimatedVisibility(visible = showConnectivityWarning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.appColors.homeSearchBarBackground)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.appColors.homeBannerIconBackground)
                )
                Text(
                    text = stringResource(R.string.hop_connection_error_message),
                    color = MaterialTheme.appColors.homeBannerText,
                    fontFamily = RobotoMono,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

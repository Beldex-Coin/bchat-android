package com.thoughtcrimes.securesms.compose_utils

import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.util.DebugLogger
import com.beldex.libbchat.avatars.ContactColors
import com.beldex.libbchat.avatars.PlaceholderAvatarPhoto
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.avatars.ResourceContactPhoto
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.R

enum class ProfilePictureMode(val size: Dp) {
    GroupPicture(40.dp),
    SmallPicture(48.dp),
    LargePicture(96.dp)
}

@Composable
fun ProfilePictureComponent(
    publicKey: String,
    displayName: String,
    containerSize: Dp,
    additionalPublicKey: String? = null,
    additionalDisplayName: String? = null,
    pictureMode: ProfilePictureMode = ProfilePictureMode.SmallPicture
) {
    Box(
        modifier = Modifier
    ) {
        when (pictureMode) {
            ProfilePictureMode.GroupPicture -> {
                Box(
                    modifier = Modifier
                        .size(containerSize)
                ) {
                    ProfilePicture(
                        containerSize = containerSize,
                        publicKey = publicKey,
                        displayName = displayName
                    )
                    additionalPublicKey?.let { adnPublicKey ->
                        additionalDisplayName?.let { addDisplayName ->
                            ProfilePicture(
                                containerSize = containerSize,
                                publicKey = adnPublicKey,
                                displayName = addDisplayName
                            )
                        }
                    }
                }
            }
            ProfilePictureMode.SmallPicture,
            ProfilePictureMode.LargePicture -> {
                ProfilePicture(
                    containerSize = containerSize,
                    publicKey = publicKey,
                    displayName = displayName
                )
            }
        }
    }
}

@Composable
fun ProfilePicture(
    containerSize: Dp,
    publicKey: String,
    displayName: String,
) {
    val context = LocalContext.current
    val recipient = Recipient.from(context, Address.fromSerialized(publicKey), false)
//    if (profilePicturesCache.containsKey(publicKey) && profilePicturesCache[publicKey] == recipient.profileAvatar) return
    val signalProfilePicture = recipient.contactPhoto
    val avatar = (signalProfilePicture as? ProfileContactPhoto)?.avatarObject

    val unknownRecipientDrawable = ResourceContactPhoto(R.drawable.ic_profile_default)
        .asDrawable(context, ContactColors.UNKNOWN_COLOR.toConversationColor(context), false)
    val unknownDrawable = ResourceContactPhoto(R.drawable.ic_notification_)
        .asDrawable(context, ContactColors.UNKNOWN_COLOR.toConversationColor(context), false)

    if (signalProfilePicture != null && avatar != "0" && avatar != "") {
        val imageLoader = LocalContext.current.imageLoader.newBuilder()
            .logger(DebugLogger())
            .build()
        val imageRequest = ImageRequest.Builder(context)
            .data(signalProfilePicture.getUri(context))
            .placeholder(unknownRecipientDrawable)
            .error(unknownDrawable)
            .build()
        AsyncImage(
            model = imageRequest,
            contentDescription = displayName,
            imageLoader = imageLoader,
            modifier = Modifier
                .size(containerSize)
                .clip(CircleShape)
        )
    } else if (recipient.isOpenGroupRecipient && recipient.groupAvatarId == null)  {
        val sizePx = with(LocalDensity.current) {
            containerSize.toPx()
        }.toInt()
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                    setImageDrawable(unknownDrawable)
                }
            }
        )
    } else {
        val placeHolderKey = if (displayName.isNotEmpty() || displayName.isNotBlank())
            displayName
        else
            "${publicKey.take(4)}...${publicKey.takeLast(4)}"
        val placeholder = PlaceholderAvatarPhoto(publicKey, placeHolderKey)

        val imageRequest = ImageRequest.Builder(context)
            .data(placeholder)
            .placeholder(unknownRecipientDrawable)
            .build()
        AsyncImage(
            model = imageRequest,
            contentDescription = displayName,
            modifier = Modifier
                .size(containerSize)
                .clip(CircleShape)
        )
    }
}

@Preview
@Composable
fun ProfilePictureComponentPreview() {
    ProfilePictureComponent(
        publicKey = "",
        displayName = "",
        containerSize = 48.dp,
        pictureMode = ProfilePictureMode.GroupPicture
    )
}
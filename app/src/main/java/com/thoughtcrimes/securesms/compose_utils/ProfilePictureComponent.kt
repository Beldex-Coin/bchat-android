package com.thoughtcrimes.securesms.compose_utils

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.beldex.libbchat.avatars.ContactColors
import com.beldex.libbchat.avatars.PlaceholderAvatarPhoto
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.avatars.ResourceContactPhoto
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.thoughtcrimes.securesms.hilt.GlideProvider
import com.thoughtcrimes.securesms.util.AvatarPlaceholderGenerator
import dagger.hilt.android.EntryPointAccessors.fromApplication
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
    modifier: Modifier = Modifier,
    additionalPublicKey: String? = null,
    additionalDisplayName: String? = null,
    pictureMode: ProfilePictureMode = ProfilePictureMode.SmallPicture,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (pictureMode) {
            ProfilePictureMode.GroupPicture -> {
                Box(
                    modifier = Modifier
                        .size(containerSize + 8.dp)
                ) {
                    ProfilePicture(
                        containerSize = containerSize,
                        publicKey = publicKey,
                        displayName = displayName,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                    )
                    additionalPublicKey?.let { adnPublicKey ->
                        additionalDisplayName?.let { addDisplayName ->
                            ProfilePicture(
                                containerSize = containerSize,
                                publicKey = adnPublicKey,
                                displayName = addDisplayName,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glide = remember {
        fromApplication(
            context,
            GlideProvider::class.java
        ).provideGlide()
    }
    val recipient = Recipient.from(context, Address.fromSerialized(publicKey), false)
//    if (profilePicturesCache.containsKey(publicKey) && profilePicturesCache[publicKey] == recipient.profileAvatar) return
    val signalProfilePicture = recipient.contactPhoto
    val avatar = (signalProfilePicture as? ProfileContactPhoto)?.avatarObject

    val unknownRecipientDrawable = ResourceContactPhoto(R.drawable.ic_profile_default)
        .asDrawable(context, ContactColors.UNKNOWN_COLOR.toConversationColor(context), false)
    val unknownDrawable = ResourceContactPhoto(R.drawable.ic_notification_)
        .asDrawable(context, ContactColors.UNKNOWN_COLOR.toConversationColor(context), false)

     fun setupDefaultProfileView(): Drawable {
        return AvatarPlaceholderGenerator.generate(context, 128, publicKey, displayName)
    }

    if (signalProfilePicture != null && avatar != "0" && avatar != "") {
//        val updatedUrl = rememberUpdatedState(signalProfilePicture.getUri(context))
//        val imageLoader = LocalContext.current.imageLoader.newBuilder()
//            .logger(DebugLogger())
//            .build()
//        val imageRequest = remember() {
//            ImageRequest.Builder(context)
//                .data(updatedUrl.value)
//                .placeholder(unknownRecipientDrawable)
//                .diskCachePolicy(CachePolicy.DISABLED)
//                .error(unknownDrawable)
//                .build()
//        }
//        AsyncImage(
//            model = imageRequest,
//            contentDescription = displayName,
//            imageLoader = imageLoader,
//            modifier = modifier
//                .size(containerSize)
//                .clip(CircleShape)
//        )
        val sizePx = with(LocalDensity.current) {
            containerSize.toPx()
        }.toInt()
        AndroidView(
            factory = { ctx ->
                val imageView = ImageView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                    contentDescription = displayName
                }
                glide.load(signalProfilePicture)
                    .placeholder(unknownRecipientDrawable)
                    .error(setupDefaultProfileView())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(CenterInside(),
                        CircleCrop())
                    .into(imageView)
                imageView
            },
            modifier = modifier
        )
    } else if (recipient.isOpenGroupRecipient && recipient.groupAvatarId == null) {
        val sizePx = with(LocalDensity.current) {
            containerSize.toPx()
        }.toInt()
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                    setImageDrawable(unknownDrawable)
                }
            },
            modifier = modifier
        )
    } else {
        var image by remember { mutableStateOf<Drawable?>(null) }
        var status by remember {
            mutableStateOf(true)
        }

        val placeholder = PlaceholderAvatarPhoto(
            publicKey, if (displayName.isNotEmpty() || displayName.isNotBlank())
                displayName
            else
                "${publicKey.take(4)}...${publicKey.takeLast(4)}"
        )
        if (status) {
            glide
                .load(placeholder)
                .placeholder(unknownRecipientDrawable)
                .diskCacheStrategy(DiskCacheStrategy.NONE).transform(
                    CenterInside(),
                    GranularRoundedCorners(20f, 20f, 20f, 20f)
                )
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        image = resource
                        status = false
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        image = placeholder
                    }
                })
        }
        image?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(containerSize)
                    .clip(CircleShape)
            )
        }
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
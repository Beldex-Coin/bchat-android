package io.beldex.bchat.compose_utils

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.beldex.libbchat.avatars.ContactColors
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.avatars.ResourceContactPhoto
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.components.NotchedAvatarImageView
import io.beldex.bchat.hilt.GlideProvider
import io.beldex.bchat.util.AvatarPlaceholderGenerator
import dagger.hilt.android.EntryPointAccessors.fromApplication
import io.beldex.bchat.R
import android.content.Context
import androidx.compose.ui.res.stringResource
import com.beldex.libbchat.utilities.TextSecurePreferences


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
    isRefresh: Boolean = false,
    isGroupInfo: Boolean = false,
    /** False when the caller draws [BnsVerifiedBadge] itself (e.g. above its own avatar outline). */
    showBnsBadge: Boolean = true
) {
    val context = LocalContext.current
    fun getUserIsBNSHolderStatus(publicKey: String): Boolean? {
        // Your own account has no contact row: use the BNS name saved in preferences instead.
        if (publicKey == TextSecurePreferences.getLocalNumber(context)) {
            return !TextSecurePreferences.getIsBNSHolder(context).isNullOrEmpty()
        }
        val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
        return contact?.isBnsHolder
    }

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
                        Image(
                            painter=painterResource(id=R.drawable.ic_secret_group_profile),
                            contentDescription=null,
                            // Figma group badge: 16dp, flush with the tile's bottom-right corner.
                            modifier=modifier
                                .size(if(isGroupInfo) 26.dp else 16.dp)
                                .align(Alignment.BottomEnd)
                        )
                    /*additionalPublicKey?.let { adnPublicKey ->
                        additionalDisplayName?.let { addDisplayName ->
                            ProfilePicture(
                                containerSize = containerSize,
                                publicKey = adnPublicKey,
                                displayName = addDisplayName,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                            )
                        }
                    }*/
                }
            }
            ProfilePictureMode.SmallPicture -> {
                ProfilePicture(
                    containerSize = containerSize,
                    publicKey = publicKey,
                    displayName = displayName,
                    isBnsTag = showBnsBadge && (getUserIsBNSHolderStatus(publicKey)?:false),
                    pictureType = 1,
                )
            }
            ProfilePictureMode.LargePicture -> {
                var pictureType = 0
                if(isRefresh){
                    pictureType = -1
                }
                ProfilePicture(
                    containerSize = containerSize,
                    publicKey = publicKey,
                    displayName = displayName,
                    isBnsTag = showBnsBadge && (getUserIsBNSHolderStatus(publicKey)?:false),
                    pictureType = pictureType
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
    modifier: Modifier = Modifier,
    isBnsTag: Boolean = false,
    pictureType: Int = 0,
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
//                .clip(NotchedAvatarShape)
//        )
        val sizePx = with(LocalDensity.current) {
            containerSize.toPx()
        }.toInt()
        if(isBnsTag) {
            Box() {
                AndroidView(
                    factory = { ctx ->
                        val imageView = NotchedAvatarImageView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                            contentDescription = displayName
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        glide.load(signalProfilePicture)
                            .placeholder(unknownRecipientDrawable)
                            .error(setupDefaultProfileView())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .transform(CenterCrop())
                            .into(imageView)
                        imageView
                    },
                    modifier = modifier
                )
                BnsVerifiedBadge(containerSize = containerSize, small = pictureType == 1, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }else{
            AndroidView(
                factory = { ctx ->
                    val imageView = NotchedAvatarImageView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                        contentDescription = displayName
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    glide.load(signalProfilePicture)
                        .placeholder(unknownRecipientDrawable)
                        .error(setupDefaultProfileView())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .transform(CenterCrop())
                        .into(imageView)
                    imageView
                },
                modifier = modifier
            )
        }
    } else if (recipient.isOpenGroupRecipient && recipient.groupAvatarId == null) {
        val sizePx = with(LocalDensity.current) {
            containerSize.toPx()
        }.toInt()
        AndroidView(
            factory = { ctx ->
                NotchedAvatarImageView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                    scaleType = ImageView.ScaleType.CENTER_CROP
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
        val placeholder = setupDefaultProfileView()
        if (status) {
            glide
                .load(placeholder)
                .placeholder(unknownRecipientDrawable)
                .diskCacheStrategy(DiskCacheStrategy.NONE).transform(
                    CenterInside()
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
            if(isBnsTag) {
                Box() {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = modifier
                            .size(containerSize)
                            .clip(NotchedAvatarShape)
                    )
                    BnsVerifiedBadge(containerSize = containerSize, small = pictureType == 1, modifier = Modifier.align(Alignment.BottomEnd))
                }
            }else {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .size(containerSize)
                        .clip(NotchedAvatarShape)
                )
            }
        }
    }
}

/** True when [publicKey] is BNS-verified (your own account: the BNS name saved in preferences). */
fun isBnsVerified(context: Context, publicKey: String): Boolean {
    if (publicKey == TextSecurePreferences.getLocalNumber(context)) {
        return !TextSecurePreferences.getIsBNSHolder(context).isNullOrEmpty()
    }
    return DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)?.isBnsHolder == true
}

/**
 * Figma BNS verified badge ("Approval 4", 7546:23392 / 7546:22322): ~43% of a list avatar (18dp on
 * 42dp) or ~36% of a large one (30dp on 84dp), pushed a quarter of its size past the avatar's
 * bottom-right corner. Parents must not clip, or the overhang is cut off.
 */
@Composable
fun BnsVerifiedBadge(containerSize: Dp, small: Boolean, modifier: Modifier = Modifier) {
    val size = if (small) (containerSize * 0.43f).coerceIn(12.dp, 20.dp) else (containerSize * 0.36f).coerceAtLeast(20.dp)
    Image(
        painter = painterResource(id = R.drawable.ic_bns_verified_tag),
        contentDescription = stringResource(R.string.bns_verified_tag),
        modifier = modifier
            // Figma: list badge 4dp past an 18dp badge (~22%), account badge 9dp past a 30dp one (~30%).
            .offset(x = size * (if (small) 0.22f else 0.3f), y = size * (if (small) 0.22f else 0.3f))
            .size(size)
    )
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

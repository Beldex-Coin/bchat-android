package io.beldex.bchat.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.DimenRes
import com.beldex.libbchat.avatars.ContactColors
import com.beldex.libbchat.avatars.PlaceholderAvatarPhoto
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.avatars.ResourceContactPhoto
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.AvatarPlaceholderGenerator.generate
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewProfilePictureBinding
import io.beldex.bchat.mms.GlideRequests

class ProfilePictureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {
    private val binding: ViewProfilePictureBinding by lazy { ViewProfilePictureBinding.bind(this) }
    lateinit var glide: GlideRequests
    var publicKey: String? = null
    var displayName: String? = null
    var additionalPublicKey: String? = null
    var additionalDisplayName: String? = null
    var isLarge = false
    private var isEditGroup = false

    private val profilePicturesCache = mutableMapOf<String, String?>()
    private val profilePicturesCacheWithBnsTag = mutableMapOf<String, String?>()
    private val resourcePadding by lazy {
        context.resources.getDimensionPixelSize(R.dimen.normal_padding).toFloat()
    }
    private val unknownRecipientDrawable by lazy {
        ResourceContactPhoto(R.drawable.ic_profile_default)
            .asDrawable(context, ContactColors.UNKNOWN_COLOR.toConversationColor(context), false, resourcePadding)
    }
    private val unknownOpenGroupDrawable by lazy {
        ResourceContactPhoto(R.drawable.ic_notification_)
            .asDrawable(context, ContactColors.UNKNOWN_COLOR.toConversationColor(context), false, resourcePadding)
    }

    // region Updating
    
    fun update(recipient: Recipient,groupImage: Boolean = false, fromEditGroup : Boolean= false) {
        fun getUserDisplayName(publicKey: String): String {
            val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
        }

        fun isOpenGroupWithProfilePicture(recipient: Recipient): Boolean {
            return recipient.isOpenGroupRecipient && recipient.groupAvatarId != null
        }

        if(isOpenGroupWithProfilePicture(recipient)){
            val publicKey = recipient.address.toString()
            this.publicKey = publicKey
            displayName = getUserDisplayName(publicKey)
            additionalPublicKey = null
        }
        else if (recipient.isGroupRecipient && !isOpenGroupWithProfilePicture(recipient)) {
            val members = DatabaseComponent.get(context).groupDatabase()
                    .getGroupMemberAddresses(recipient.address.toGroupString(), true)
                    .sorted()
                    .take(2)
                    .toMutableList()
            val pk = members.getOrNull(0)?.serialize() ?: ""
            publicKey = pk
            displayName = recipient.name ?: ""
            val apk = members.getOrNull(1)?.serialize() ?: ""
            additionalPublicKey = apk
            additionalDisplayName = getUserDisplayName(apk)
            isEditGroup = fromEditGroup
        } else {
            val publicKey = recipient.address.toString()
            this.publicKey = publicKey
            displayName = getUserDisplayName(publicKey)
            additionalPublicKey = null
        }
        update(displayName,groupImage)
    }

    private fun getUserIsBNSHolderStatus(publicKey: String): Boolean? {
        val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
        return contact?.isBnsHolder
    }

    fun update(displayName: String? = publicKey,groupImage:Boolean = false) {
        val publicKey = publicKey ?: return
        val additionalPublicKey = additionalPublicKey
        val isBnsHolder = getUserIsBNSHolderStatus(publicKey)?:false
        if(isEditGroup){
            setProfilePictureIfNeeded(binding.editGroupdoubleModeImageView1, publicKey, displayName, R.dimen.small_profile_picture_size)
            binding.editGroupdoubleModeImageView2.visibility = View.VISIBLE
            binding.editGroupdoubleModeImageViewContainer.visibility = View.VISIBLE
        }else{
            glide.clear(binding.editGroupdoubleModeImageView1)
            binding.editGroupdoubleModeImageView2.visibility = View.INVISIBLE
            binding.editGroupdoubleModeImageViewContainer.visibility = View.INVISIBLE
        }
        if (additionalPublicKey != null) {
            Log.d("beldex","if 1")
            setProfilePictureIfNeeded(binding.doubleModeImageView1, publicKey, displayName, R.dimen.small_profile_picture_size)
            //setProfilePictureIfNeeded(binding.doubleModeImageView2, additionalPublicKey, additionalDisplayName, R.dimen.small_profile_picture_size)
            binding.doubleModeImageView2.visibility = View.VISIBLE
            binding.doubleModeImageViewContainer.visibility = View.VISIBLE
        } else {
            Log.d("beldex","else 1")
            glide.clear(binding.doubleModeImageView1)
            //glide.clear(binding.doubleModeImageView2)
            binding.doubleModeImageView2.visibility = View.INVISIBLE
            binding.doubleModeImageViewContainer.visibility = View.INVISIBLE
        }
        if (additionalPublicKey == null && !isLarge) {
            Log.d("beldex","if 2")
            if(isBnsHolder){
                setProfilePictureIfNeeded(binding.singleModeWithTagImageView, publicKey, displayName, R.dimen.medium_profile_picture_size,true)
                if(groupImage){
                    binding.singleModeBnsVerifiedTagGroupImageView.visibility = View.VISIBLE
                    binding.singleModeBnsVerifiedTagImageView.visibility = View.INVISIBLE
                }else{
                    binding.singleModeBnsVerifiedTagImageView.visibility = View.VISIBLE
                    binding.singleModeBnsVerifiedTagGroupImageView.visibility = View.INVISIBLE
                }
                binding.singleModeWithTagContainer.visibility = View.VISIBLE
                glide.clear(binding.singleModeImageView)
                binding.singleModeImageView.visibility = View.INVISIBLE
            }else{
                setProfilePictureIfNeeded(binding.singleModeImageView, publicKey, displayName, R.dimen.medium_profile_picture_size)
                binding.singleModeImageView.visibility = View.VISIBLE
                glide.clear(binding.singleModeWithTagImageView)
                binding.singleModeBnsVerifiedTagGroupImageView.visibility = View.INVISIBLE
                binding.singleModeBnsVerifiedTagImageView.visibility = View.INVISIBLE
                binding.singleModeWithTagContainer.visibility = View.INVISIBLE
            }
        } else {
            Log.d("beldex","else 2")
            glide.clear(binding.singleModeImageView)
            binding.singleModeImageView.visibility = View.INVISIBLE
            glide.clear(binding.singleModeWithTagImageView)
            binding.singleModeBnsVerifiedTagImageView.visibility = View.INVISIBLE
            binding.singleModeWithTagContainer.visibility = View.INVISIBLE
        }
        if (additionalPublicKey == null && isLarge) {
            Log.d("beldex","if 3")
            if(isBnsHolder) {
                setProfilePictureIfNeeded(binding.largeSingleModeWithTagImageView, publicKey, displayName, R.dimen.large_profile_picture_size)
                binding.largeSingleModeBnsVerifiedTagImageView.visibility = View.VISIBLE
                binding.largeSingleModeWithTagContainer.visibility = View.VISIBLE
                glide.clear(binding.largeSingleModeImageView)
                binding.largeSingleModeImageView.visibility = View.INVISIBLE
            }else{
                setProfilePictureIfNeeded(binding.largeSingleModeImageView, publicKey, displayName, R.dimen.large_profile_picture_size)
                binding.largeSingleModeImageView.visibility = View.VISIBLE
                glide.clear(binding.largeSingleModeWithTagImageView)
                binding.largeSingleModeBnsVerifiedTagImageView.visibility = View.INVISIBLE
                binding.largeSingleModeWithTagContainer.visibility = View.INVISIBLE
            }
        } else {
            Log.d("beldex","else 3")
            glide.clear(binding.largeSingleModeImageView)
            binding.largeSingleModeImageView.visibility = View.INVISIBLE
            glide.clear(binding.largeSingleModeWithTagImageView)
            binding.largeSingleModeBnsVerifiedTagImageView.visibility = View.INVISIBLE
            binding.largeSingleModeWithTagContainer.visibility = View.INVISIBLE
        }
    }
    private fun setupDefaultProfileView(): Drawable {
        return generate(context,128, publicKey!!, displayName)
    }

    private fun setProfilePictureIfNeeded(imageView: ImageView, publicKey: String, displayName: String?, @DimenRes sizeResId: Int, isBnsTag:Boolean = false) {
        if (publicKey.isNotEmpty()) {
            val recipient = Recipient.from(context, Address.fromSerialized(publicKey), false)
            if(isBnsTag) {
                if (profilePicturesCacheWithBnsTag.containsKey(publicKey) && profilePicturesCacheWithBnsTag[publicKey] == recipient.profileAvatar) {
                    return
                }
            }else {
                if (profilePicturesCache.containsKey(publicKey) && profilePicturesCache[publicKey] == recipient.profileAvatar) {
                    return
                }
            }
            val signalProfilePicture = recipient.contactPhoto
            val avatar = (signalProfilePicture as? ProfileContactPhoto)?.avatarObject

            if (signalProfilePicture != null && avatar != "0" && avatar != "") {
                glide.clear(imageView)
                //New Line
               /* glide.load(signalProfilePicture).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).transform(
                    CenterInside(),
                    GranularRoundedCorners(20f, 20f, 20f, 20f)).into(imageView)*/
                glide.load(signalProfilePicture)
                    .placeholder(unknownRecipientDrawable)
                    .error(setupDefaultProfileView())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(CenterInside(),
                        GranularRoundedCorners(20f, 20f, 20f, 20f))
                    .into(imageView)
            } else if (recipient.isOpenGroupRecipient && recipient.groupAvatarId == null) {
                glide.clear(imageView)
                imageView.setImageDrawable(unknownOpenGroupDrawable)

            } else {
                val placeholder = PlaceholderAvatarPhoto(publicKey, displayName ?: "${publicKey.take(4)}...${publicKey.takeLast(4)}")
                glide.clear(imageView)
                //New Line
                /*glide.load(AvatarPlaceholderGenerator.generate(context, sizeInPX, publicKey, displayName)).diskCacheStrategy(DiskCacheStrategy.ALL).transform(
                    CenterInside(),
                    GranularRoundedCorners(20f, 20f, 20f, 20f)
                ).into(imageView)*/
                glide.load(placeholder)
                    .placeholder(unknownRecipientDrawable)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).transform(
                        CenterInside(),
                        GranularRoundedCorners(20f, 20f, 20f, 20f)
                    ).into(imageView)
            }
            if(isBnsTag){
                profilePicturesCacheWithBnsTag[publicKey] = recipient.profileAvatar
            }else {
                profilePicturesCache[publicKey] = recipient.profileAvatar
            }
        } else {
            imageView.setImageDrawable(null)
        }
    }

    fun recycle() {
        profilePicturesCache.clear()
        profilePicturesCacheWithBnsTag.clear()
    }
    // endregion
}

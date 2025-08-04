package io.beldex.bchat.conversation.v2.contact_sharing

import android.os.Parcelable
import com.beldex.libbchat.utilities.Address
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactModel(
    val threadId: String,
    val address: Address,
    val name: String
): Parcelable
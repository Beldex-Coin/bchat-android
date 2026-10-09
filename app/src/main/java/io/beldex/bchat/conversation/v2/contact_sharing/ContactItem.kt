package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.BorderStroke
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.noRippleCallback
import io.beldex.bchat.database.model.ThreadRecord

@Composable
fun ContactItem(
    contact: ThreadRecord?,
    isSelected: Boolean,
    contactChanged: (ThreadRecord?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isSharing: Boolean = true
) {
    val shape = if (isSelected) notchedCornerShape(12.dp) else RectangleShape
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                if (isSelected) MaterialTheme.appColors.onboardingInputBackground else Color.Transparent,
                shape
            )
            .border(
                BorderStroke(
                    if (isSelected) 0.5.dp else 1.dp,
                    if (isSelected) MaterialTheme.appColors.userDetailsConfirmBackground
                    else MaterialTheme.appColors.pinBoxInactiveBorder
                ),
                shape
            )
            .noRippleCallback {
                contactChanged(contact, !isSelected)
            }
            .padding(horizontal = 12.dp)
    ) {
        if (contact != null) {
            ProfilePictureComponent(
                publicKey = contact.recipient.address.toString(),
                displayName = contact.recipient.name.toString(),
                containerSize = 32.dp,
                pictureMode = ProfilePictureMode.SmallPicture
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                (contact?.recipient?.name ?: contact?.recipient?.address.toString()).capitalizeFirstLetter(),
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                contact?.recipient?.address?.toString()?.let { formatAddresses(it) } ?: "",
                color = MaterialTheme.appColors.onboardingCaptionColor,
                fontFamily = RobotoMono,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSharing) {
            Spacer(modifier = Modifier.width(10.dp))
            Image(
                painter = painterResource(id = if (isSelected) R.drawable.ic_checkedbox else R.drawable.ic_checkbox),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/*created just for use with contact sharing where checked functionality needs to be disabled.*/
@Composable
private fun OutlinedCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.appColors.primaryButtonColor
) {
    Box(
        modifier=modifier
            .border(
                shape=RoundedCornerShape(2.dp),
                border=BorderStroke(
                    2.dp, if (checked) {
                        MaterialTheme.appColors.primaryButtonColor
                    } else {
                        MaterialTheme.appColors.secondaryContentColor
                    }
                )
            )
            .size(18.dp)
            .background(Color.Transparent)
            .padding(2.dp),
        contentAlignment=Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription="Checked",
                tint=color,
            )
        }
    }
}
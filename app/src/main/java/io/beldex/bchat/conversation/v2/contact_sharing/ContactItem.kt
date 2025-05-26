package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Message
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
import io.beldex.bchat.compose_utils.BChatCheckBox
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
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
    OutlinedCard(
        shape = RoundedCornerShape(50),
        modifier = modifier
    ){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    8.dp
                )
                .noRippleCallback {
                    contactChanged(contact, !isSelected)
                }
        ) {
            if (contact == null) {
                Box(
                    modifier = Modifier
                        .weight(0.1f)
                        .size(36.dp)
                        .background(
                            color = Color.Green,
                            shape = RoundedCornerShape(100)
                        )
                )
            }
            if (contact != null) {
                ProfilePictureComponent(
                    publicKey = contact.recipient.address.toString(),
                    displayName = contact.recipient.name.toString(),
                    containerSize = 36.dp,
                    pictureMode = ProfilePictureMode.SmallPicture
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(if (isSharing) 0.8f else 0.7f)
            ) {
                Text(
                    contact?.recipient?.name ?: "No Name",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    contact?.recipient?.address?.toString() ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (isSharing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.1f)
                ) {
                    BChatCheckBox(
                        checked = isSelected,
                        onCheckedChange = {
                            contactChanged(contact, !isSelected)
                        }
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.2f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Message,
                            contentDescription = "",
                            modifier = Modifier
                                .size(24.dp)
                        )

                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
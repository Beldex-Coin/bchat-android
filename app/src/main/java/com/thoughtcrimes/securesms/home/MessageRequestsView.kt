package com.thoughtcrimes.securesms.home

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.ExpandCircleDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.messaging.contacts.Contact
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureComponent
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureMode
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.ui.BubbledText
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import io.beldex.bchat.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageRequestsView(
    requests: List<ThreadRecord>,
    modifier: Modifier = Modifier
) {
    var showRequests by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.activity_message_requests_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight(700)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            BubbledText(
                text = requests.size.toString(),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.appColors.requestCountColor
                ),
                boxBackground = MaterialTheme.appColors.requestCountBackground
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                Icons.Outlined.ExpandCircleDown,
                contentDescription = "",
                tint = MaterialTheme.appColors.iconTint,
                modifier = Modifier
                    .clickable {
                        showRequests = !showRequests
                    }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = showRequests,
            label = "MessageRequests"
        ) {
            if (it) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(requests) { record ->
                        RequestItem(request = record)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    request: ThreadRecord
) {
    val address = request.recipient.address.toString()
    val displayName = getDisplayName(LocalContext.current, address)
    Box(
        modifier = Modifier
    ) {
        ProfilePictureComponent(
            publicKey = request.recipient.address.toString(),
            displayName = displayName,
            containerSize = ProfilePictureMode.SmallPicture.size,
            pictureMode = ProfilePictureMode.SmallPicture,
            modifier = Modifier
                .padding(
                    end = 8.dp,
                    top = 8.dp
                )
        )
        Icon(
            Icons.Filled.Cancel,
            contentDescription = "",
            tint = MaterialTheme.appColors.iconTint,
            modifier = Modifier
                .align(Alignment.TopEnd)
        )
    }
}

private fun getDisplayName(context: Context, publicKey: String): String {
    val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
}

@Preview
@Composable
fun MessageRequestPreview() {
    BChatTheme {
        MessageRequestsView(
            requests = emptyList(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MessageRequestPreviewDark() {
    BChatTheme {
        MessageRequestsView(
            requests = emptyList(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
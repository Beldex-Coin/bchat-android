package com.thoughtcrimes.securesms.home

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ExpandCircleDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.messaging.contacts.Contact
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureComponent
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureMode
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.noRippleCallback
import com.thoughtcrimes.securesms.compose_utils.ui.BubbledText
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import io.beldex.bchat.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageRequestsView(
    requests: List<ThreadRecord>,
    openSearch: () -> Unit,
    ignoreRequest: (ThreadRecord) -> Unit,
    openChat: (ThreadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.appColors.searchBackground
                )
                .noRippleCallback {
                    openSearch()
                }
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "",
                tint = MaterialTheme.appColors.iconTint
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(id = R.string.search_people_and_groups),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.inputHintColor
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (requests.isNotEmpty()) {
            var showRequests by remember {
                mutableStateOf(false)
            }
            val rotationDegree by animateFloatAsState(
                targetValue = if (showRequests) 180f else 0f,
                label = "rotation",
                animationSpec = tween(
                    durationMillis = 500,
                )
            )

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
                        fontSize = 10.sp,
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
                        .rotate(
                            degrees = rotationDegree
                        )
                )
            }

            if (showRequests) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedContent(
                targetState = showRequests,
                label = "MessageRequests"
            ) {
                if (it) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        items(requests) { record ->
                            RequestItem(
                                request = record,
                                onClick = openChat,
                                onCancel = ignoreRequest
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RequestItem(
    request: ThreadRecord,
    onCancel: (ThreadRecord) -> Unit,
    onClick: (ThreadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val address = request.recipient.address.toString()
    val displayName = getDisplayName(LocalContext.current, address)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
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
                    .clickable {
                        onClick(request)
                    }
            )

            Card(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clickable {
                        onCancel(request)
                    }
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "",
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = request.recipient.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .width(48.dp)
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
            openSearch = {},
            ignoreRequest = {},
            openChat = {},
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
            openSearch = {},
            ignoreRequest = {},
            openChat = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
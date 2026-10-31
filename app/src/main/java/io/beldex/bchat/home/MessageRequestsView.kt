package io.beldex.bchat.home

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.messaging.contacts.Contact
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.noRippleCallback
import io.beldex.bchat.conversation.v2.contact_sharing.capitalizeFirstLetter
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.dependencies.DatabaseComponent

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageRequestsView(
    requests: List<ThreadRecord>,
    openSearch: () -> Unit,
    ignoreRequest: (ThreadRecord) -> Unit,
    openChat: (ThreadRecord) -> Unit,
    modifier: Modifier = Modifier,
    openMessageRequests: () -> Unit = {}
) {
    // Revamp_2026 (Figma 7546:1780): flat full-width search band, then a green Message Requests
    // banner that opens the requests screen. ignoreRequest/openChat stay for the requests screen.
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.appColors.dialogBackground)
                .drawBehind {
                    val stroke = 0.75.dp.toPx()
                    drawLine(homeBandBorder, Offset(0f, 0f), Offset(size.width, 0f), stroke)
                    drawLine(homeBandBorder, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                }
                .noRippleCallback { openSearch() }
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_home_search),
                contentDescription = null,
                tint = MaterialTheme.appColors.onboardingInputHint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.search_name_bns_bchat_id),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                fontFamily = RobotoMono,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = MaterialTheme.appColors.onboardingInputHint
            )
        }

        if (requests.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(53.dp)
                    .background(color = MaterialTheme.appColors.messageRequestBackground.copy(alpha = 0.5f))
                    .drawBehind {
                        val strokeWidth = 0.40.dp.toPx()

                        drawLine(
                            color = requestBannerAccent,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = strokeWidth
                        )
                    }
                    .clickable { openMessageRequests() }
                    .padding(horizontal = 20.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_message_request_banner),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.lockTimerColor,
                    modifier = Modifier.size(width = 20.dp, height = 18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = if (requests.size == 1) R.string.activity_message_request_title else R.string.activity_message_requests_title),
                    fontFamily = OpenSans,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.appColors.editTextColor,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 21.dp, minHeight = 18.dp)
                        .background(requestBannerAccent)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = requests.size.toString(),
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.appColors.dialogPositiveButtonText,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right_green),
                    contentDescription = null,
                    tint = requestBannerAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private val homeBandBorder = Color(0xFF444444)
private val requestBannerAccent = Color(0xFF00BC33)
private val requestBannerFill = Color(0x80061306)

@Composable
fun RequestItem(
    request: ThreadRecord,
    onCancel: (ThreadRecord) -> Unit,
    onClick: (ThreadRecord) -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false
) {
    val context = LocalContext.current

    if (horizontal) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
            ) {
                key(request.recipient.address.toString()) {
                    ProfilePictureComponent(
                        publicKey=request.recipient.address.toString(),
                        displayName=getDisplayName(context, request.recipient.address.toString()),
                        containerSize=ProfilePictureMode.SmallPicture.size,
                        pictureMode=ProfilePictureMode.SmallPicture,
                        modifier=Modifier
                            .padding(
                                end=8.dp,
                                top=8.dp
                            )
                            .clickable {
                                onClick(request)
                            }
                    )

                    Card(
                        elevation=CardDefaults.cardElevation(
                            defaultElevation=4.dp
                        ),
                        colors=CardDefaults.cardColors(
                            containerColor=MaterialTheme.appColors.requestCountBackground
                        ),
                        shape=CircleShape,
                        modifier=Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clickable {
                                onCancel(request)
                            }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription="",
                            modifier=Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = request.recipient.name?.capitalizeFirstLetter() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = RobotoMono,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
            ) {
                key(request.recipient.address.toString()) {
                    ProfilePictureComponent(
                        publicKey=request.recipient.address.toString(),
                        displayName=getDisplayName(context, request.recipient.address.toString()),
                        containerSize=ProfilePictureMode.SmallPicture.size,
                        pictureMode=ProfilePictureMode.SmallPicture,
                        modifier=Modifier
                            .padding(
                                end=8.dp,
                                top=8.dp
                            )
                            .clickable {
                                onClick(request)
                            }
                    )

                    Card(
                        elevation=CardDefaults.cardElevation(
                            defaultElevation=4.dp
                        ),
                        colors=CardDefaults.cardColors(
                            containerColor=MaterialTheme.appColors.requestCountBackground
                        ),
                        shape=CircleShape,
                        modifier=Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clickable {
                                onCancel(request)
                            }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription="",
                            modifier=Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = request.recipient.name?.capitalizeFirstLetter() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .width(48.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getDisplayName(context: Context, publicKey: String): String {
    val contact = DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
    return contact?.displayName(Contact.ContactContext.REGULAR, context) ?: publicKey
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
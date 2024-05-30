package com.thoughtcrimes.securesms.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.noRippleCallback
import com.thoughtcrimes.securesms.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.R

@Composable
fun NewChatButtons(
    isExpanded: Boolean,
    changeExpandedStatus: (Boolean) -> Unit,
    createPrivateChat: () -> Unit,
    createSecretGroup: () -> Unit,
    joinPublicGroup: () -> Unit
) {
    val modifier = if (isExpanded) {
        Modifier
            .fillMaxSize()
            .noRippleCallback {
                changeExpandedStatus(false)
            }
    } else {
        Modifier
            .fillMaxSize()
    }
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideIn(initialOffset = {IntOffset.Zero}),
            exit = slideOut(targetOffset = {IntOffset.Zero})
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.appColors.beldexAddressBackground,
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(IntrinsicSize.Max).padding(end = 2.dp)
            ) {
                ChatOptionButton(
                    title = stringResource(id = R.string.activity_create_private_chat_title),
                    icon = painterResource(id = R.drawable.ic_new_chat),
                    modifier = Modifier
                        .noRippleCallback {
                            changeExpandedStatus(false)
                            createPrivateChat()
                        }
                )
                ChatOptionButton(
                    title = stringResource(id = R.string.home_screen_secret_groups_title),
                    icon = painterResource(id = R.drawable.ic_secret_group),
                    modifier = Modifier
                        .noRippleCallback {
                            changeExpandedStatus(false)
                            createSecretGroup()
                        }
                )
                ChatOptionButton(
                    title = stringResource(id = R.string.home_screen_social_groups_title),
                    icon = painterResource(id = R.drawable.ic_social_group),
                    modifier = Modifier
                        .noRippleCallback {
                            changeExpandedStatus(false)
                            joinPublicGroup()
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
        ) {
            FloatingActionButton(
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                onClick = {
                    changeExpandedStatus(!isExpanded)
                },
                containerColor = MaterialTheme.appColors.primaryButtonColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                modifier = Modifier
                    .padding(
                        bottom = 8.dp,
                        end = 8.dp
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bchat_plus),
                    contentDescription = "",
                    tint = Color.White
                )
            }
            Icon(
                if (isExpanded) Icons.Filled.Cancel else Icons.Filled.AddCircle,
                contentDescription = "",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.appColors.primaryButtonColor)
            )
        }
    }
}

@Composable
private fun ChatOptionButton(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            )
    ){
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.appColors.primaryButtonColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Image(
            painter = icon,
            contentDescription = ""
        )
    }
}

@Preview
@Composable
fun NewChatButtonsPreview() {
    BChatPreviewContainer {
        NewChatButtons(
            isExpanded = true,
            changeExpandedStatus = {},
            createPrivateChat = {},
            createSecretGroup = {},
            joinPublicGroup = {}
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun NewChatButtonsPreviewDark() {
    BChatPreviewContainer {
        NewChatButtons(
            isExpanded = true,
            changeExpandedStatus = {},
            createPrivateChat = {},
            createSecretGroup = {},
            joinPublicGroup = {}
        )
    }
}
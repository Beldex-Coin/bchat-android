package com.thoughtcrimes.securesms.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.noRippleCallback
import io.beldex.bchat.R

@Composable
fun NewChatButtons(
    isExpanded: Boolean,
    changeExpandedStatus: (Boolean) -> Unit,
    createPrivateChat: () -> Unit,
    createSecretGroup: () -> Unit,
    joinPublicGroup: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End
    ) {
        AnimatedVisibility(visible = isExpanded) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.appColors.contactCardBackground
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(IntrinsicSize.Max)
            ) {
                ChatOptionButton(
                    title = "New Chat",
                    icon = painterResource(id = R.drawable.ic_bchat_plus),
                    modifier = Modifier
                        .noRippleCallback {
                            createPrivateChat()
                        }
                )
                ChatOptionButton(
                    title = "Secret Group",
                    icon = painterResource(id = R.drawable.ic_bchat_plus),
                    modifier = Modifier
                        .noRippleCallback {
                            createSecretGroup()
                        }
                )
                ChatOptionButton(
                    title = "Social Group",
                    icon = painterResource(id = R.drawable.ic_bchat_plus),
                    modifier = Modifier
                        .noRippleCallback {
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
                onClick = {
                    if (!isExpanded)
                        changeExpandedStatus(true)
                },
                containerColor = MaterialTheme.appColors.primaryButtonColor,
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
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
                .padding(
                    8.dp
                )
        ) {
            Icon(
                painter = icon,
                contentDescription = ""
            )
        }
    }
}

@Preview
@Composable
fun NewChatButtonsPreview() {
    BChatTheme {
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
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun NewChatButtonsPreviewDark() {
    BChatTheme {
        NewChatButtons(
            isExpanded = false,
            changeExpandedStatus = {},
            createPrivateChat = {},
            createSecretGroup = {},
            joinPublicGroup = {}
        )
    }
}
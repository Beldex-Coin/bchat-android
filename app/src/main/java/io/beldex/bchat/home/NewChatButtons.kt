package io.beldex.bchat.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.R

@Composable
fun NewChatButtons(
    openNewConversationChat: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                    openNewConversationChat()
                },
                containerColor = MaterialTheme.appColors.floatingActionButtonBackground,
                modifier = Modifier
                    .size(48.dp).padding(
                        bottom = 8.dp,
                        end = 8.dp
                    ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun NewChatButtonsPreview() {
    BChatPreviewContainer {
        NewChatButtons(
            openNewConversationChat = {}
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
            openNewConversationChat = {}
        )
    }
}
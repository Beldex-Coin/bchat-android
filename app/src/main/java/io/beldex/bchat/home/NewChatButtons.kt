package io.beldex.bchat.home

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import io.beldex.bchat.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.R

@Composable
fun NewChatButtons(
    openNewConversationChat: () -> Unit
) {
    val fabDescription = stringResource(R.string.activity_create_private_chat_title)
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 10.dp)
                    .size(48.dp)
                    .clickable(onClick = openNewConversationChat)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.appColors.homeFabBackground, notchedCornerShape(6.dp))
                ) {
                    // Figma "FAB New Chat": two 10 x 1.5 bars, #0A0A0A.
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(10.dp)
                            .semantics { contentDescription = fabDescription }
                    ) {
                        val bar = 1.5.dp.toPx()
                        val barColor = androidx.compose.ui.graphics.Color(0xFF0A0A0A)
                        drawRect(barColor, topLeft = androidx.compose.ui.geometry.Offset(0f, (size.height - bar) / 2), size = androidx.compose.ui.geometry.Size(size.width, bar))
                        drawRect(barColor, topLeft = androidx.compose.ui.geometry.Offset((size.width - bar) / 2, 0f), size = androidx.compose.ui.geometry.Size(bar, size.height))
                    }
                }
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
package io.beldex.bchat.home

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities


@Composable
fun ArchiveChatView(
    archiveChatViewModel : ArchiveChatViewModel,
    threadDatabase : ThreadDatabase,
    onRequestClick : () -> Unit,
    context: Context
) {

    var count by remember {
        mutableIntStateOf(0)
    }

    val lifecycleOwner=LocalLifecycleOwner.current
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
    archiveChatViewModel.archiveChatsCount.observe(lifecycleOwner) {
        count=threadDatabase.archivedConversationList.count
    }

    Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier
        .background(
            color=MaterialTheme.appColors.archiveChatCardBackground, shape=RoundedCornerShape(40.dp)
        )
        .padding(8.dp)
        .clickable {
            onRequestClick()
        }) {
        Box(
            contentAlignment=Alignment.Center, modifier=Modifier
                .size(42.dp)
                .background(
                    color=MaterialTheme.appColors.archiveChatIconBackground, shape=CircleShape
                )
        ) {
            Image(
                painterResource(id= if(isDarkTheme) R.drawable.ic_archive else R.drawable.ic_archive_icon_white),
                contentDescription="Archived Chats",
                modifier=Modifier.size(26.dp)

            )
        }
        Spacer(modifier=Modifier.width(8.dp))
        Text(
            text=stringResource(id=R.string.archive_chat),
            style=MaterialTheme.typography.titleMedium.copy(
                fontSize=14.sp, fontWeight=FontWeight(600), color=MaterialTheme.appColors.textColor
            ),
        )
        Spacer(modifier=Modifier.weight(1f))
        Box(
            contentAlignment=Alignment.Center, modifier=Modifier
                .size(28.dp)
                .background(
                    color=MaterialTheme.appColors.archiveChatCountBackground, shape=CircleShape
                )
        ) {
            Text(
                text=count.toString(),
                color=MaterialTheme.appColors.textColor,
                fontWeight=FontWeight.Bold,
                fontSize=14.sp
            )
        }
    }
}
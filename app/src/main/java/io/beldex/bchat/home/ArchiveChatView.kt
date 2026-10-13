package io.beldex.bchat.home

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities


@Composable
fun ArchiveChatView(
    count: Int,
    onRequestClick : () -> Unit,
    context: Context
) {
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.homeBackground.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
            .clickable { onRequestClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Image(
            painterResource(id = R.drawable.ic_archive_chats),
            contentDescription = stringResource(R.string.archive_chat),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.appColors.onboardingInputText),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = stringResource(id = R.string.archive_chat),
            color = MaterialTheme.appColors.onboardingInputText,
            fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(24.dp)
                .defaultMinSize(minWidth = 24.dp)
                .background(
                    color = MaterialTheme.appColors.userDetailsConfirmBackground,
                    shape = io.beldex.bchat.compose_utils.notchedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = androidx.compose.ui.graphics.Color(0xFF0B0B0B),
                fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

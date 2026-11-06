package io.beldex.bchat.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.GroupUtil
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.compose_utils.appColors

/**
 * Figma "You Joined BChat" / "You Created My secret group": a notched card shown in a brand-new
 * group chat with the group picture, a green group name, the group kind with its member count and
 * the time it happened.
 */
@Composable
fun GroupJoinedCard(
    verb: String,
    groupName: String,
    kind: String,
    memberCount: String,
    whenText: String,
    publicKey: String,
    modifier: Modifier = Modifier
) {
    val shape = notchedCornerShape(BChatDimens.NotchCard)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.messageRequestItemBackground, shape)
            .border(1.dp, Color(0xFF078720).copy(alpha = 0.7f), shape)
            .padding(vertical = 28.dp, horizontal = 16.dp)
    ) {
        val isSecretGroup = GroupUtil.isClosedGroup(publicKey)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.appColors.cardTileBackground, notchedCornerShape(BChatDimens.NotchMedium))
                .border(1.dp, colorResource(R.color.contact_card_inner_bar), notchedCornerShape(BChatDimens.NotchMedium))
        ) {
            if (isSecretGroup) {
                Image(
                    painter = painterResource(R.drawable.ic_group_created_secret),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
            } else {
                ProfilePictureComponent(
                    publicKey = publicKey,
                    displayName = groupName,
                    containerSize = 40.dp,
                    pictureMode = ProfilePictureMode.SmallPicture
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = buildAnnotatedString {
                append("$verb ")
                withStyle(SpanStyle(color = MaterialTheme.appColors.userDetailsBchatIdText)) { append(groupName) }
            },
            color = MaterialTheme.appColors.editTextColor,
            fontFamily = OpenSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$kind · $memberCount",
            color = Color(0xFF8D8D8D),
            fontFamily = RobotoMono,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = whenText,
            color = Color(0xFF737373),
            fontFamily = RobotoMono,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

package io.beldex.bchat.my_account.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.DialogPanel
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.notchedCornerShape
import androidx.compose.ui.graphics.Color
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.R

@Composable
fun CopyContentDialog(
    title: String,
    data: String,
    onCopy: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val accentColor = MaterialTheme.appColors.userDetailsConfirmBackground
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.5f else 0.9f)
                .widthIn(max = if (isLandscape) 320.dp else 400.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GreenSideBar(
                modifier = Modifier.fillMaxHeight(0.6F),
                color = accentColor
            )
            DialogPanel(
                modifier = Modifier
                    .weight(1F)
                    .heightIn(max = 560.dp),
                shape = notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchCard)
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 13.dp, bottom = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1F)
            ) {
                Text(
                    text = title,
                    style = BChatTypography.titleMedium.copy(
                        color = MaterialTheme.appColors.userDetailsConfirmBackground,
                        fontFamily = OpenSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight(700),
                    ),
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = data,
                    style = BChatTypography.titleSmall.copy(
                        color = MaterialTheme.appColors.editTextColor,
                        fontFamily = RobotoMono,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight(400),
                    ),
                )

            }
            Spacer(modifier = Modifier.width(5.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "",
                tint = MaterialTheme.appColors.userDetailsConfirmBackground,
                modifier = Modifier
                    .size(16.dp)
                    .align(alignment = Alignment.CenterVertically)
                    .clickable {
                        onCopy()
                    }
            )
            }
            }
            GreenSideBar(
                modifier = Modifier.fillMaxHeight(0.6F),
                color = accentColor
            )
        }
    }
}
@Composable
private fun GreenSideBar(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight(0.7F)
            .background(color)
    )
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
    showBackground = true
)
@Composable
fun CopyContentDialogPreview() {
    BChatTheme {
        CopyContentDialog(
            title = "",
            data = "",
            onCopy = {},
            onDismissRequest = {}
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
    showBackground = true
)
@Composable
fun CopyContentDialogPreviewDark() {
    BChatTheme {
        CopyContentDialog(
            title = "",
            data = "",
            onCopy = {},
            onDismissRequest = {}
        )
    }
}
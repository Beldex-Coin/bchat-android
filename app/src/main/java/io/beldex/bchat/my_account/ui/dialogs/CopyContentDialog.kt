package io.beldex.bchat.my_account.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun CopyContentDialog(
    title: String,
    data: String,
    onCopy: () -> Unit,
    onDismissRequest: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = {
               onDismissRequest()
        },
        containerColor = MaterialTheme.appColors.bnsDialogBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ){
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1F)
            ) {
                Text(
                    text = title,
                    style = BChatTypography.titleMedium.copy(
                        color = MaterialTheme.appColors.primaryButtonColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight(700),
                    ),
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = data,
                    style = BChatTypography.titleSmall.copy(
                        color = MaterialTheme.appColors.editTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight(400),
                    ),
                )

            }
            Spacer(modifier = Modifier.width(5.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "",
                tint = MaterialTheme.appColors.primaryButtonColor,
                modifier = Modifier
                    .size(16.dp)
                    .align(alignment = Alignment.CenterVertically)
                    .clickable {
                        onCopy()
                    }
            )
        }
    }

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
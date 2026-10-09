package io.beldex.bchat.compose_utils

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DialogContainer(
    dismissOnBackPress : Boolean = false,
    dismissOnClickOutside : Boolean = false,
    onDismissRequest: () -> Unit,
    containerColor: Color = MaterialTheme.appColors.dialogBackground,
    wrapContentWidth: Boolean = false,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val maxCardHeight = minOf(560.dp, (maxHeight - 32.dp).coerceAtLeast(120.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .then(
                        if (wrapContentWidth)
                            Modifier.wrapContentWidth().padding(horizontal = 16.dp)
                        else
                            Modifier.fillMaxWidth(if (isLandscape) 0.5f else 0.9f)
                    )
                    .heightIn(max = maxCardHeight)
                    .widthIn(max = if (isLandscape) 320.dp else 400.dp)
            ) {
                content()
            }
        }
    }
}
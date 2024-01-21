package com.thoughtcrimes.securesms.my_account.ui.dialogs

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.my_account.ui.ScreenTimeoutOptions
import io.beldex.bchat.R

@Composable
fun LockOptionsDialog(
    onDismiss: () -> Unit
) {
    DialogContainer(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_inactivity_timeout),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                factory = { context ->
                    val options = ScreenTimeoutOptions.values().map { it.displayValue }.toTypedArray()
                    NumberPickerView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300)
                        displayedValues = options
                        minValue = 0
                        maxValue = options.size - 1
                        wrapSelectorWheel = true
                        setDividerColor(ContextCompat.getColor(context, R.color.text))
                        setSelectedTextColor(ContextCompat.getColor(context, R.color.text))
                        setNormalTextColor(ContextCompat.getColor(context, R.color.scan_qr_code_text_color))
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun LockOptionsDialogPreview() {
    LockOptionsDialog(
        onDismiss = {}
    )
}
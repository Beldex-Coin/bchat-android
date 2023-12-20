package com.thoughtcrimes.securesms.my_account.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.carbswang.android.numberpickerview.library.NumberPickerView

@Composable
fun LockOptionsDialog(

) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
        ) {
            Text(
                text = "Screen Inactivity Timeout",
                style = MaterialTheme.typography.titleMedium
            )

            AndroidView(
                factory = { context ->
                    NumberPickerView(context)
                }
            )
        }
    }
}

@Preview
@Composable
fun LockOptionsDialogPreview() {
    LockOptionsDialog()
}
package com.thoughtcrimes.securesms.compose_utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun MultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    hintText: String = "",
    maxLines: Int = 4
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        textStyle = textStyle,
        decorationBox = { innerTextField ->
            Box(
                modifier = modifier
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hintText,
                        color = MaterialTheme.appColors.editTextHint
                    )
                }
                innerTextField()
            }
        }
    )
}
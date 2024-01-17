package com.thoughtcrimes.securesms.my_account.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
@Composable
fun ContentScreen(
    content: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .verticalScroll(
                state = scrollState
            )
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
package io.beldex.bchat.my_account.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.appColors

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
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.appColors.editTextColor,
                fontWeight = FontWeight(400),
                fontSize = 14.sp
            )
        )
    }
}
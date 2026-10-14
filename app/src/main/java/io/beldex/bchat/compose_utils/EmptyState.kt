package io.beldex.bchat.compose_utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Figma empty state ("No contacts Yet!"): two ghosted bordered rows over a grey mono caption. */
@Composable
fun GhostEmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        val ghost = notchedCornerShape(BChatDimens.NotchSmall)
        Box(modifier = Modifier.size(width = 200.dp, height = 62.dp)) {
            Box(
                modifier = Modifier
                    .offset(x = 77.dp, y = 0.dp)
                    .size(width = 123.dp, height = 33.dp)
                    .background(Color(0xFF111111), ghost)
                    .border(1.dp, Color(0xFF444444), ghost)
            )
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 26.dp)
                    .size(width = 123.dp, height = 33.dp)
                    .background(Color(0xFF111111), ghost)
                    .border(1.dp, Color(0xFF444444), ghost)
            )
        }
        Text(
            text = text,
            color = Color(0xFF555555),
            fontFamily = RobotoMono,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

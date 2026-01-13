package io.beldex.bchat.compose_utils.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BubbledText(
    text: String,
    modifier: Modifier = Modifier,
    boxBackground: Color = Color.White,
    defaultMinSize: Dp = 16.dp,
    padding: Dp = 2.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Box(
        modifier = modifier
            .background(
                color = boxBackground,
                shape = CircleShape
            )
            .padding(padding)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val maxDim = maxOf(placeable.width, placeable.height)
                layout(maxDim, maxDim) {
                    val x = (maxDim - placeable.width) / 2
                    val y = (maxDim - placeable.height) / 2
                    placeable.placeRelative(x, y)
                }
            }
    ) {
        Text(
            text = text,
            style = textStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minWidth = defaultMinSize, minHeight = defaultMinSize)
        )
    }
}

@Preview("Bubbled Text", showBackground = true)
@Composable
private fun BubbledTextPreview() {
    BubbledText(text = "1")
}
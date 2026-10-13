package io.beldex.bchat.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.OpenSans
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.my_account.domain.PathNodeModel
import io.beldex.bchat.R

@Composable
fun HopsScreen(
    nodes: List<PathNodeModel>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text(
            text = stringResource(id = R.string.activity_path_explanation),
            color = MaterialTheme.appColors.onboardingBodyColor,
            fontFamily = RobotoMono,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Figma "Hops" path card: #111111 @ 60% fill, #333333 hairline, green start/end markers.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99111111))
                .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.MediaPreviewActivity_you),
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            PathMarker(filled = true, color = MaterialTheme.appColors.userDetailsConfirmBackground.copy(alpha = 0.75f), size = 16.dp)
            PathLine()

            nodes.forEachIndexed { index, node ->
                PathMarker(
                    filled = index != nodes.lastIndex || nodes.size == 1,
                    color = MaterialTheme.appColors.editTextColor,
                    size = 12.dp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = node.title,
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = RobotoMono,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
                Text(
                    text = node.subTitle,
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = RobotoMono,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                PathLine()
            }

            PathMarker(filled = true, color = MaterialTheme.appColors.userDetailsConfirmBackground.copy(alpha = 0.75f), size = 16.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.activity_path_destination_row_title),
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

/** Marker centred in a 16dp column so every marker and dashed connector share one x. */
@Composable
private fun PathMarker(filled: Boolean, color: Color, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (filled) Modifier.background(color)
                    else Modifier.border(1.dp, color, CircleShape)
                )
        )
    }
}

@Composable
private fun PathLine() {
    val color = MaterialTheme.appColors.primaryButtonColor
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(30.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }
    )
}

@Preview
@Composable
fun HopsScreenPreview() {
    BChatTheme {
        HopsScreen(
            nodes = listOf()
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HopsScreenPreviewDark() {
    BChatTheme {
        HopsScreen(
            nodes = listOf()
        )
    }
}
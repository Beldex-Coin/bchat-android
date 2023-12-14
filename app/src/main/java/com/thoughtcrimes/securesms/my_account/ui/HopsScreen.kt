package com.thoughtcrimes.securesms.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.my_account.domain.PathNodeModel
import io.beldex.bchat.R

@Composable
fun HopsScreen(
    nodes: List<PathNodeModel>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.activity_path_explanation),
            style = MaterialTheme.typography.labelMedium
        )

        Column(
            modifier = Modifier
                .padding(
                    vertical = 16.dp,
                    horizontal = 16.dp
                )
        ) {
            Text(
                text = stringResource(id = R.string.activity_path_device_row_title),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            GetFilledCircle()

            nodes.forEach {
                GetInternalNode(
                    title = it.title,
                    subTitle = it.subTitle
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            GetLine()

            GetFilledCircle()

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = R.string.activity_path_destination_row_title),
                style = MaterialTheme.typography.bodySmall
            )

        }
    }
}

@Composable
fun GetInternalNode(
    title: String,
    subTitle: String
) {
    val color = MaterialTheme.appColors.primaryButtonColor
    val density = LocalDensity.current
    val lineHeight = with(density) {
        64.dp.toPx()
    }
    val lineXOffset = with(density) {
        8.dp.toPx()
    }
    val circleCenterOffset = with(density) {
        2.dp.toPx()
    }
    val startingPoint by remember {
        mutableStateOf(Offset(lineXOffset, 0f))
    }
    Column {
        Box(
            modifier = Modifier
                .height(64.dp)
                .drawBehind {
                    drawLine(
                        color = color,
                        start = startingPoint,
                        end = Offset(
                            x = startingPoint.x,
                            y = startingPoint.y + lineHeight
                        ),
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(
                                10f,
                                10f
                            ), 0f
                        )
                    )
                }
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .drawBehind {
                    drawCircle(
                        color = color,
                        style = Stroke(
                            width = 6f
                        ),
                        center = Offset(
                            x = center.x + circleCenterOffset,
                            y = center.y
                        )
                    )
                }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = subTitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun GetFilledCircle() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                color = MaterialTheme.appColors.primaryButtonColor
            )
    )
}

@Composable
private fun GetLine() {
    val color = MaterialTheme.appColors.primaryButtonColor
    val density = LocalDensity.current
    val lineHeight = with(density) {
        64.dp.toPx()
    }
    val lineXOffset = with(density) {
        8.dp.toPx()
    }
    val startingPoint by remember {
        mutableStateOf(Offset(x = lineXOffset, y = 0f))
    }
    Box(
        modifier = Modifier
            .height(64.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = startingPoint,
                    end = Offset(
                        x = startingPoint.x,
                        y = startingPoint.y + lineHeight
                    ),
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(
                            10f,
                            10f
                        ), 0f
                    )
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
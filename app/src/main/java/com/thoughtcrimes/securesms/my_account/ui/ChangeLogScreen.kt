package com.thoughtcrimes.securesms.my_account.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandCircleDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.my_account.domain.ChangeLogModel

@Composable
fun ChangeLogScreen(
    changeLogs: List<ChangeLogModel>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        itemsIndexed(
            items = changeLogs,
            key = { _, item ->
                item.version
            }
        ) { _, versionLog ->
            LogItem(
                versionLog = versionLog,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LogItem(
    versionLog: ChangeLogModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember {
        mutableStateOf(false)
    }
    val iconRotation by remember(isExpanded) {
        mutableStateOf(if (isExpanded) 180f else 0f)
    }
    val borderStroke = if (isSystemInDarkTheme()) {
        1.dp
    } else {
        0.dp
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.appColors.changeLogBackground
        ),
        border = BorderStroke(
            width = borderStroke,
            color = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isExpanded = !isExpanded
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.appColors.primaryButtonColor,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = versionLog.version,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                )
                Icon(
                    Icons.Outlined.ExpandCircleDown,
                    contentDescription = "",
                    tint = MaterialTheme.appColors.iconTint,
                    modifier = Modifier
                        .rotate(iconRotation)
                )
            }

            AnimatedContent(
                targetState = isExpanded,
                label = versionLog.version
            ) {
                if (it) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        versionLog.logs.forEach { log ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp,
                                        top = 4.dp,
                                        bottom = 4.dp
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            color = MaterialTheme.appColors.changeLogColor,
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(
                                            horizontal = 8.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ChangeLogScreenPreview() {
    BChatTheme {
        ChangeLogScreen(
            changeLogs = listOf(
                ChangeLogModel(
                    version = "1.0.0",
                    logs = listOf(
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release"
                    )
                ),
                ChangeLogModel(
                    version = "1.0.0",
                    logs = listOf(
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release"
                    )
                )
            )
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChangeLogScreenPreviewDArk() {
    BChatTheme {
        ChangeLogScreen(
            changeLogs = listOf(
                ChangeLogModel(
                    version = "1.0.0",
                    logs = listOf(
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release"
                    )
                ),
                ChangeLogModel(
                    version = "1.0.0",
                    logs = listOf(
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release",
                        "Initial Release"
                    )
                )
            )
        )
    }
}
package io.beldex.bchat.my_account.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import io.beldex.bchat.my_account.domain.ChangeLogModel
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

@Composable
fun ChangeLogScreen(
    changeLogs: List<ChangeLogModel>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 16.dp)
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
    Card(
        shape = androidx.compose.ui.graphics.RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.appColors.dividerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp)
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
                Spacer(modifier = Modifier.width(11.dp))

                Text(
                    text = versionLog.version,
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .weight(1f)
                )
                Icon(
                    Icons.Outlined.ExpandCircleDown,
                    contentDescription = "",
                    tint = MaterialTheme.appColors.onboardingInputText,
                    modifier = Modifier
                        .size(22.dp)
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
                                        start = 2.dp,
                                        top = 7.dp,
                                        bottom = 2.dp
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
                                        color = MaterialTheme.appColors.onboardingInputText,
                                        fontFamily = RobotoMono,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 16.sp
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
                    version = "1.0.1",
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
                    version = "1.0.1",
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
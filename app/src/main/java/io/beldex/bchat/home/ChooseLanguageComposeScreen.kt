package io.beldex.bchat.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.my_account.ui.MyAccountViewModel


@Composable
fun ChooseLanguage(
    viewModel: MyAccountViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val selectedCode by viewModel.selectedLanguageCode.collectAsState()

    val onLanguageSelected: (String) -> Unit = { code ->
        if (code != selectedCode) {
            viewModel.selectLanguage(code)
        }
        onBack()
    }

    val languages = listOf(
        Language("Arabic", "ar"),
        Language("Chinese", "zh"),
        Language("English", "en"),
        Language("German", "de"),
        Language("Japanese", "ja"),
        Language("Korean", "ko"),
        Language("Portuguese", "pt"),
        Language("Russian", "ru"),
        Language("Spanish", "es"),
        Language("Turkish", "tr"),
        Language("Vietnamese", "vi"),
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        LazyColumn(
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp)

        ) {
            items(languages, key = { it.code }) { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (language.code == selectedCode) 1.dp else 0.dp,
                            color = if (language.code == selectedCode)
                                MaterialTheme.appColors.primaryButtonColor
                            else
                                MaterialTheme.appColors.editTextBackground,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .background(
                            color = if (selectedCode == language.code) MaterialTheme.appColors.contactCardBackground else MaterialTheme.appColors.editTextBackground
                        )
                        .clickable {
                            onLanguageSelected(language.code)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(
                        selected = language.code == selectedCode,
                        onClick = {
                            onLanguageSelected(language.code)
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF4CAF50),
                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        ),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = language.name,
                            color = MaterialTheme.appColors.textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = language.code,
                            color = MaterialTheme.appColors.transactionSubTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        LazyColumnScrollbar(
            listState = scrollState,
            thumbColor = MaterialTheme.appColors.scrollBarColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 8.dp, top = 16.dp, end = 2.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun LazyColumnScrollbar(
    listState: LazyListState,
    thumbColor: Color,
    trackColor: Color = Color.Transparent,
    modifier: Modifier = Modifier

) {
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(8.dp)
    ) {
        val layoutInfo = listState.layoutInfo

        if (layoutInfo.totalItemsCount == 0) return@Canvas

        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(size.width)
        )

        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return@Canvas

        val firstItem = visibleItems.first()

        val estimatedItemHeight = firstItem.size.toFloat()

        val totalContentHeight =
            layoutInfo.totalItemsCount * estimatedItemHeight

        val viewportHeight = size.height

        val thumbHeight =
            (viewportHeight * viewportHeight / totalContentHeight)
                .coerceIn(40f, viewportHeight)

        val scrollY =
            (listState.firstVisibleItemIndex * estimatedItemHeight) +
                    listState.firstVisibleItemScrollOffset

        val maxScroll =
            (totalContentHeight - viewportHeight)
                .coerceAtLeast(1f)

        val thumbOffset =
            ((viewportHeight - thumbHeight) * (scrollY / maxScroll))
                .coerceIn(0f, viewportHeight - thumbHeight)

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, thumbOffset),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(size.width)
        )
    }
}

data class Language(val name: String, val code: String)

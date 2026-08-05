package io.beldex.bchat.home

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.my_account.ui.MyAccountViewModel
import io.beldex.bchat.util.AppLanguageEvent


@Composable
fun ChooseLanguage(
    viewModel: MyAccountViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val selectedCode by viewModel.selectedLanguageCode.collectAsState()
    val deviceLanguageCode = AppLanguageEvent.deviceLanguageState.collectAsState().value
    val context = LocalContext.current

    val onLanguageSelected: (String) -> Unit = { code ->
        if (code != selectedCode) {
            viewModel.selectLanguage(code, context)
        }
        onBack()
    }

    val languages = listOf(
        Language("Arabic", "العربية", "ar"),
        Language("Chinese, Simplified", "简体中文", "zh"),
        Language("English", "English", "en"),
        Language("German", "Deutsch", "de"),
        Language("Japanese", "日本語", "ja"),
        Language("Korean", "한국어", "ko"),
        Language("Portuguese (Brazil)", "Português (Brasil)", "pt"),
        Language("Russian", "Русский", "ru"),
        Language("Spanish", "Español", "es"),
        Language("Turkish", "Türkçe", "tr"),
        Language("Vietnamese", "Tiếng Việt", "vi")
    )

    fun updateSelectedLanguage(context: Context, language: Language) {
        TextSecurePreferences.setAppSelectedLanguage(context, language.code)
        onLanguageSelected(language.code)
    }

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
                            updateSelectedLanguage(context, language)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(
                        selected = language.code == selectedCode,
                        onClick = {
                            updateSelectedLanguage(context, language)
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.appColors.primaryButtonColor,
                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        ),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = language.nativeName,
                            color = MaterialTheme.appColors.textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (language.code.contains(deviceLanguageCode)) "(device's language)" else language.englishName,
                            color = MaterialTheme.appColors.transactionSubTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

data class Language(
    val englishName: String,
    val nativeName: String,
    val code: String
)

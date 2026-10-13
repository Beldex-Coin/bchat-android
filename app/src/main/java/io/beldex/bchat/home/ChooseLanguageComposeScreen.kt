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
import io.beldex.bchat.compose_utils.PrimaryButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatRadioButton
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.my_account.ui.MyAccountViewModel
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.service.WebRtcCallService
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
    // Revamp_2026: a row only highlights; "Apply" commits the language.
    var pendingCode by remember(selectedCode) { mutableStateOf(selectedCode) }
    var query by remember { mutableStateOf("") }

    val languages = listOf(
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
        val keyCachingIntent = Intent(context, KeyCachingService::class.java)
        keyCachingIntent.action = KeyCachingService.LANGUAGE_CHANGED_ACTION
        context.startService(keyCachingIntent)
        val webRtcIntent = Intent(context, WebRtcCallService::class.java)
        webRtcIntent.action = WebRtcCallService.ACTION_LANGUAGE_CHANGED
        context.startService(webRtcIntent)
        onLanguageSelected(language.code)
    }

    val visibleLanguages = languages.filter {
        query.isBlank() || it.nativeName.contains(query.trim(), ignoreCase = true) ||
            it.englishName.contains(query.trim(), ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LanguageSearchField(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )
    Box(
        modifier =
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp)

        ) {
            items(visibleLanguages, key = { it.code }) { language ->
                val isSelected = language.code == pendingCode
                val rowShape = if (isSelected) io.beldex.bchat.compose_utils.notchedCornerShape(14.dp) else androidx.compose.ui.graphics.RectangleShape
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(
                            color = if (isSelected) Color(0xFF0B1210) else MaterialTheme.appColors.homeRowBackground.copy(alpha = 0.9f),
                            shape = rowShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.pinBoxInactiveBorder,
                            shape = rowShape,
                        )
                        .clickable {
                            pendingCode = language.code
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = language.nativeName,
                            color = MaterialTheme.appColors.onboardingInputText,
                            fontFamily = OpenSans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (language.code.contains(deviceLanguageCode)) stringResource(R.string.choose_language_device_language) else language.englishName,
                            color = if (isSelected) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.onboardingCaptionColor,
                            fontFamily = RobotoMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BChatRadioButton(
                        selected = isSelected,
                        onClick = { pendingCode = language.code }
                    )
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
        PrimaryButton(
            onClick = {
                val chosen = languages.firstOrNull { it.code == pendingCode }
                if (chosen != null && chosen.code != selectedCode) {
                    updateSelectedLanguage(context, chosen)
                } else {
                    onBack()
                }
            },
            shape = io.beldex.bchat.compose_utils.notchedCornerShape(14.dp),
            containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
            contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(58.dp)
        ) {
            Text(
                text = stringResource(R.string.apply),
                fontFamily = RobotoMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun LanguageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.appColors.onboardingInputBackground)
            .border(1.dp, MaterialTheme.appColors.dividerColor)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        androidx.compose.material3.Icon(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_home_search),
            contentDescription = null,
            tint = MaterialTheme.appColors.onboardingCaptionColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_language).uppercase(),
                    color = MaterialTheme.appColors.onboardingInputHint,
                    fontFamily = RobotoMono,
                    fontSize = 12.sp
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = RobotoMono,
                    fontSize = 14.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.appColors.textSelectionColor),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_close),
                contentDescription = null,
                tint = MaterialTheme.appColors.onboardingInputText,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") }
            )
        }
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

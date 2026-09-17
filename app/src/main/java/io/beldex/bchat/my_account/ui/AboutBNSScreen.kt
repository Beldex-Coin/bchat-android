package io.beldex.bchat.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.R

@Composable
fun AboutBNSScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.2.sp,
        )){
            append(stringResource(id = R.string.about_bns_pricing_label))
        }
        withStyle(style = SpanStyle(fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.2.sp,)) {
            append(" ")
            append(stringResource(id = R.string.about_bns_pricing_text))
            append(" ")
        }
        withStyle(style = SpanStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.2.sp,
        )){
            append(stringResource(id = R.string.about_bns_pricing_values))
        }
        withStyle(style = SpanStyle(fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.2.sp,)) {
            append(" ")
            append(stringResource(id = R.string.about_bns_pricing_suffix))
        }
    }
    Column(
        modifier = modifier
            .verticalScroll(
                state = scrollState
            )
    ) {
        Text(
            text = stringResource(id = R.string.about_bns_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )
        Text(
            text = stringResource(id = R.string.about_bns_description_1),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )
        Text(
            text = stringResource(id = R.string.about_bns_key_benefits),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )
        Text(
            text = stringResource(id = R.string.about_bns_description_4),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.padding(start = 10.dp, bottom = 15.dp)
        )
        Text(
            text = stringResource(id = R.string.about_bns_description_5),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.padding(start = 10.dp, bottom = 15.dp)
        )
        Text(
            text = stringResource(id = R.string.about_bns_description_6),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.padding(start = 10.dp, bottom = 15.dp)
        )
        Text(
            text = annotatedString,
            modifier = Modifier.padding(bottom = 15.dp)
        )
        Text(
            text = stringResource(id = R.string.about_bns_description_7),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )
        Text(
            text = stringResource(id = R.string.about_bns_description_8),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
    showBackground = true
)
@Composable
fun AboutBNSScreenPreviewDark() {
    BChatTheme {
        AboutBNSScreen(
            modifier = Modifier
        )
    }
}
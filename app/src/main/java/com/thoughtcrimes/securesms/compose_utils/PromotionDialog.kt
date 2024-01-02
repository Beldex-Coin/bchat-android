package com.thoughtcrimes.securesms.compose_utils

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.beldex.bchat.R

@Composable
fun PromotionDialog(
    bannerUrl: String,
    redirectToLandingPage: () -> Unit,
    isDarkTheme: Boolean,
    dismiss: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(bannerUrl)
                .crossfade(true)
                .build(),
            contentDescription = "",
            modifier = Modifier
                .padding(
                    end = 8.dp,
                    top = 8.dp
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    redirectToLandingPage()
                }
        )
//        Image(
//            painter = painterResource(id = R.drawable.banner_dark),
//            contentDescription = "",
//            modifier = Modifier
//                .padding(
//                    end = 12.dp,
//                    top = 12.dp
//                )
//                .clip(RoundedCornerShape(16.dp))
//                .clickable {
//                    redirectToLandingPage(redirectionUrl)
//                }
//        )
        Image(
            painter =
            if (isDarkTheme)
                painterResource(id = R.drawable.cancel_dialog_dark)
            else
                painterResource(id = R.drawable.cancel_dialog_light),
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable {
                    dismiss()
                }
        )
    }
}

@Preview
@Composable
fun PromotionDialogPreview() {
    PromotionDialog(
        bannerUrl = "",
        redirectToLandingPage = {},
        isDarkTheme = false,
        dismiss = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PromotionDialogPreviewDark() {
    PromotionDialog(
        bannerUrl = "",
        redirectToLandingPage = {},
        isDarkTheme = true,
        dismiss = {}
    )
}
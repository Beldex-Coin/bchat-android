package io.beldex.bchat.compose_utils

import androidx.compose.ui.text.style.TextDirection

private val ARABIC_PATTERN =
    Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

fun CharSequence.inputTextDirection(): TextDirection = when {
    isEmpty() -> TextDirection.Unspecified
    ARABIC_PATTERN.containsMatchIn(this) -> TextDirection.Rtl
    else -> TextDirection.Ltr
}

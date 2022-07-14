package com.thoughtcrimes.securesms.util

import android.view.ViewGroup

fun ViewGroup.disableClipping() {
    clipToPadding = false
    clipChildren = false
    clipToOutline = false
}
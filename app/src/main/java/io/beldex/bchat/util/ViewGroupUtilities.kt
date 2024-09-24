package io.beldex.bchat.util

import android.view.ViewGroup

fun ViewGroup.disableClipping() {
    clipToPadding = false
    clipChildren = false
    clipToOutline = false
}
package com.thoughtcrimes.securesms.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.beldex.libbchat.utilities.recipients.Recipient
import dagger.hilt.android.internal.managers.ViewComponentManager
import io.beldex.bchat.R

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
  SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
  else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
  SDK_INT >= 33 -> getParcelable(key, T::class.java)
  else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

fun String?.isValidString(): Boolean {
  val input = this
  return !input.isNullOrBlank() && input.isNotEmpty()
}

fun Context?.getFragmentManager(): FragmentManager? {
  val context = this
  context ?: return null
  return when (context) {
    is AppCompatActivity -> context.supportFragmentManager
    is ContextThemeWrapper -> context.baseContext.getFragmentManager()
    is ViewComponentManager.FragmentContextWrapper -> context.baseContext.getFragmentManager()
    else -> null
  }
}

fun Context.copyToClipBoard(label: String, content: String) {
  val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText(label, content)
  clipBoard.setPrimaryClip(clip)
  Toast.makeText(this, "Copied to clip board",  Toast.LENGTH_SHORT).show()
}

package io.beldex.bchat.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import dagger.hilt.android.internal.managers.ViewComponentManager
import io.beldex.bchat.database.model.MessageRecord
import org.json.JSONObject
import java.io.Serializable

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
  SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
  else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
  SDK_INT >= 33 -> getParcelable(key, T::class.java)
  else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
  else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Serializable> Intent.serializable(key: String): T? = when {
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(key, T::class.java)
  else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
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
  Toast.makeText(this, "Copied to clipboard",  Toast.LENGTH_SHORT).show()
}

fun Context.toPx(dp: Int): Float = TypedValue.applyDimension(
  TypedValue.COMPLEX_UNIT_DIP,
  dp.toFloat(),
  resources.displayMetrics
)

fun getScreenWidth(): Int {
  return Resources.getSystem().displayMetrics.widthPixels
}

fun isSameDayMessage(current: MessageRecord, previous: MessageRecord?): Boolean {
  previous ?: return false
  return DateUtils.isSameDay(current.timestamp, previous.timestamp)
}

fun isSharedContact(body: String): Boolean {
  try {
    val mainObject = JSONObject(body)
    val uniObject = mainObject.getJSONObject("kind")
    val type = uniObject.getString("@type")
    return type.equals("SharedContact")
  } catch (e: Exception) {
    return false
  }
}

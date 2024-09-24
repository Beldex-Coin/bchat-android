package io.beldex.bchat.crypto

import android.content.Context
import android.util.Log

object MnemonicUtilities {

  public fun loadFileContents(context: Context, fileName: String): String {
      val inputStream = context.assets.open("mnemonic/$fileName.txt")
      val size = inputStream.available()
      val buffer = ByteArray(size)
      inputStream.read(buffer)
      inputStream.close()
      return String(buffer)
  }
}
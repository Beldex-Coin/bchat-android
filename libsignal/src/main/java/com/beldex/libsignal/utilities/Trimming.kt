package com.beldex.libsignal.utilities

fun String.removingbdPrefixIfNeeded(): String {
  return if (length == 66) removePrefix("bd") else this
}

fun ByteArray.removingbdPrefixIfNeeded(): ByteArray {
    val string = Hex.toStringCondensed(this).removingbdPrefixIfNeeded()
    return Hex.fromStringCondensed(string)
}

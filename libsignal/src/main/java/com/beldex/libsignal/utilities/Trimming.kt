package com.beldex.libsignal.utilities

fun String.removing05PrefixIfNeeded(): String {
  return if (length == 66) removePrefix("bd") else this
}

fun ByteArray.removing05PrefixIfNeeded(): ByteArray {
    val string = Hex.toStringCondensed(this).removing05PrefixIfNeeded()
    return Hex.fromStringCondensed(string)
}

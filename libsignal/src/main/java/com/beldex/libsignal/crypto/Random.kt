package com.beldex.libsignal.crypto

import com.beldex.libsignal.utilities.Util.SECURE_RANDOM
import java.security.SecureRandom

/**
 * Uses `SecureRandom` to pick an element from this collection.
 */
fun <T> Collection<T>.getRandomElementOrNull(): T? {
    if (isEmpty()) return null
    val index = SecureRandom().nextInt(size) // SecureRandom() should be cryptographically secure
    return elementAtOrNull(index)
}

/**
 * Uses `SecureRandom` to pick an element from this collection.
 */
fun <T> Collection<T>.getRandomElement(): T {
    return getRandomElementOrNull()!!
}

fun <T> Collection<T>.shuffledRandom(): List<T> = shuffled(SECURE_RANDOM)

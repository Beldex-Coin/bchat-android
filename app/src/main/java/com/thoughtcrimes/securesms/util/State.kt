package com.thoughtcrimes.securesms.util

sealed class State<out T> {
    object Loading : State<Nothing>()
    data class Success<T>(val value: T): State<T>()
    data class Error(val error: Exception): State<Nothing>()
}

package com.beldex.libsignal.exceptions

class NonRetryableException(message: String? = null, cause: Throwable? = null): RuntimeException(message, cause)
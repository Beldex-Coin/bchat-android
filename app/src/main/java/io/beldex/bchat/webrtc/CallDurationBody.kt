package io.beldex.bchat.webrtc

object CallDurationBody {

    /** Unit-separator control character plus a tag, so it can't clash with normal text. */
    private const val SEPARATOR = "\u001Fcall_duration:"

    /** Returns [body] with the duration appended, replacing any duration already there. */
    @JvmStatic
    fun append(body: String?, durationMillis: Long): String =
        baseBody(body) + SEPARATOR + durationMillis.coerceAtLeast(0)

    /** Connected time stored in [body], or 0 when none was saved (call never connected). */
    @JvmStatic
    fun durationOf(body: String?): Long {
        if (body.isNullOrEmpty()) return 0
        val index = body.lastIndexOf(SEPARATOR)
        if (index < 0) return 0
        return body.substring(index + SEPARATOR.length).trim().toLongOrNull()?.coerceAtLeast(0) ?: 0
    }

    /** [body] without the appended duration. */
    @JvmStatic
    fun baseBody(body: String?): String {
        if (body.isNullOrEmpty()) return ""
        val index = body.lastIndexOf(SEPARATOR)
        return if (index < 0) body else body.substring(0, index)
    }
}

package io.beldex.bchat.util

import java.util.Locale
import kotlin.math.abs

object NumberUtil {

    @JvmStatic
    fun getFormattedNumber(count: Long): String {
        val isNegative = count < 0
        val absoluteCount = abs(count)
        if (absoluteCount < 1000) return count.toString()
        val thousands = absoluteCount / 1000
        val hundreds = (absoluteCount - thousands * 1000) / 100
        val negativePrefix = if (isNegative) "-" else ""
        return if (hundreds == 0L) {
            String.format(Locale.ROOT, "$negativePrefix%dk", thousands)
        } else {
            String.format(Locale.ROOT, "$negativePrefix%d.%dk", thousands, hundreds)
        }
    }

}
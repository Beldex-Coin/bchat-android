package io.beldex.bchat.textformatter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.graphics.toColorInt
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

class AppTextFormatter(private val text: String, val context: Context) {
    private val codeBlockPattern = Regex(
        "(?s)```(.*?)```"
    )
    private val pattern = Regex(
        "(?s)" +
                "(?<!\\*)\\*(?!\\*)(\\S(?:.*?\\S)?)\\*(?!\\*)|" +
                "(?<!_)_(\\S(?:.*?\\S)?)_(?!_)|" +
                "(?<!~)~(\\S(?:.*?\\S)?)~(?!~)|" +
                "(?<!`)`(\\S(?:.*?\\S)?)`(?!`)"
    )

    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
    @SuppressLint("UseKtx")
    private fun getSymbolColor(isDark: Boolean): Int {
        return if (isDark) {
            "#66FFFFFF".toColorInt()
        } else {
            "#66000000".toColorInt()
        }
    }
    private val backgroundColorSpan = "#797984".toColorInt()

    private fun normalizeInput(text: String): String {
        return text.replace(
            Regex("(?<!\\*)\\*\\*(?!\\*)(\\S(?:.*?\\S)?)\\*\\*(?!\\*)")
        ) { match ->
            val start = match.range.first
            val end = match.range.last
            if ((start > 0 && text[start - 1] == '*') ||
                (end + 1 < text.length && text[end + 1] == '*')
            ) {
                match.value
            } else {
                "*${match.groupValues[1]}*"
            }
        }
    }


    private fun isValidPattern(content: String): Boolean {
        return when {
            content.startsWith("*") && content.endsWith("*") ->
                content.count { it == '*' } == 2

            content.startsWith("_") && content.endsWith("_") ->
                content.count { it == '_' } == 2

            content.startsWith("~") && content.endsWith("~") ->
                content.count { it == '~' } == 2

            content.startsWith("`") && content.endsWith("`") ->
                content.count { it == '`' } == 2

            else -> false
        }
    }

    fun appendFormatted(out: SpannableStringBuilder) {

        var last = 0
        val cleanText = normalizeInput(text)

        if (codeBlockPattern.containsMatchIn(cleanText)) {

            for (match in codeBlockPattern.findAll(cleanText)) {
                if (match.range.first > last) {
                    out.append(cleanText.substring(last, match.range.first))
                }

                val content = match.value
                // -------------------------------------------------
                // CODE BLOCK (``` ``` )
                // -------------------------------------------------
                if (content.startsWith("```")) {
                    val innerText = content.substring(3, content.length - 3)
                    val monoUnicode = TextFormatter.toUnicodeMonospace(innerText)

                    val openStart = out.length
                    out.append("```")
                    out.setSpan(
                        ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                        openStart,
                        openStart + 3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    val monoStart = out.length
                    out.append(monoUnicode)
                    val monoEnd = out.length

                    val closeStart = out.length
                    out.append("```")
                    out.setSpan(
                        ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                        closeStart,
                        closeStart + 3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    out.setSpan(
                        TypefaceSpan("monospace"),
                        monoStart,
                        monoEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    out.setSpan(
                        BackgroundColorSpan(Color.TRANSPARENT),
                        monoStart,
                        monoEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                last = match.range.last + 1
            }
        } else {
            for (match in pattern.findAll(cleanText)) {

                if (match.range.first > last) {
                    out.append(cleanText.substring(last, match.range.first))
                }

                val content = match.value

                when {
                    // -------------------------------------------------
                    // 1: BOLD (*text*)
                    // -------------------------------------------------
                    content.startsWith('*') -> {

                        if (!isValidPattern(content)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = content.substring(1, content.length - 1)

                        if (innerText.startsWith(" ") || innerText.endsWith(" ")) {
                            out.append(content)
                        } else {
                            val startSymbol = out.length
                            out.append("*")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                startSymbol,
                                startSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val start = out.length
                            out.append(TextFormatter.toUnicodeBold(innerText))
                            val end = out.length

                            out.setSpan(
                                StyleSpan(Typeface.BOLD),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val endSymbol = out.length
                            out.append("*")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                endSymbol,
                                endSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    // -------------------------------------------------
                    // 2: ITALIC (_text_)
                    // -------------------------------------------------
                    content.startsWith("_") -> {

                        if (!isValidPattern(content)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = content.substring(1, content.length - 1)

                        if (innerText.startsWith(" ") || innerText.endsWith(" ")) {
                            out.append(content)
                        } else {
                            val startSymbol = out.length
                            out.append("_")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                startSymbol,
                                startSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val start = out.length
                            out.append(TextFormatter.toUnicodeItalic(innerText))
                            val end = out.length

                            out.setSpan(
                                StyleSpan(Typeface.ITALIC),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val endSymbol = out.length
                            out.append("_")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                endSymbol,
                                endSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    // -------------------------------------------------
                    // 3: STRIKETHROUGH (~text~)
                    // -------------------------------------------------
                    content.startsWith("~") -> {

                        if (!isValidPattern(content)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = content.substring(1, content.length - 1)

                        if (innerText.startsWith(" ") || innerText.endsWith(" ")) {
                            out.append(content)
                        } else {
                            val startSymbol = out.length
                            out.append("~")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                startSymbol,
                                startSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val start = out.length
                            out.append(TextFormatter.toUnicodeStrikethrough(innerText))
                            val end = out.length

                            out.setSpan(
                                StrikethroughSpan(),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val endSymbol = out.length
                            out.append("~")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                endSymbol,
                                endSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    // -------------------------------------------------
                    // 4: INLINE CODE (`code`)
                    // -------------------------------------------------
                    content.startsWith("`") -> {

                        if (!isValidPattern(content)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = content.substring(1, content.length - 1)

                        if (innerText.contains("\n")) {
                            out.append(content)
                        } else {
                            val startSymbol = out.length
                            out.append("`")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                startSymbol,
                                startSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val start = out.length
                            out.append(TextFormatter.toUnicodeInlineCode(innerText))
                            val end = out.length

                            out.setSpan(
                                TypefaceSpan("monospace"),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            out.setSpan(
                                BackgroundColorSpan(backgroundColorSpan),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            val endSymbol = out.length
                            out.append("`")
                            out.setSpan(
                                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                                endSymbol,
                                endSymbol + 1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                }
                last = match.range.last + 1
            }
        }

        if (last < cleanText.length) {
            out.append(cleanText.substring(last))
        }
    }
}
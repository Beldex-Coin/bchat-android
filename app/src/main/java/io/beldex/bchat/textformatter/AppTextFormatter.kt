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
    private val codeBlockPattern = Regex("(?s)```(.*?)```")
    // Disallow newlines inside inline markers and keep backticks single
    private val pattern = Regex(
        "\\*([^\\r\\n]+?)\\*|" +
                "_([^\\r\\n]+?)_|" +
                "~([^\\r\\n]+?)~|" +
                "`([^\\r\\n`]+?)`"
    )

    private val nestedPattern = Regex(
        "`([^\\r\\n`]+?)`|" +
                "_([^\\r\\n]+?)_|" +
                "~([^\\r\\n]+?)~|" +
                "\\*([^\\r\\n]+?)\\*"
    )

    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT

    @SuppressLint("UseKtx")
    private fun getSymbolColor(isDark: Boolean): Int =
        if (isDark) "#66FFFFFF".toColorInt() else "#66000000".toColorInt()

    private val backgroundColorSpan = "#797984".toColorInt()

    private fun hasWordBoundaries(full: String, range: IntRange): Boolean {
        val prev = full.getOrNull(range.first - 1)
        val next = full.getOrNull(range.last + 1)
        return (prev == null || !prev.isLetterOrDigit()) &&
                (next == null || !next.isLetterOrDigit())
    }

    private fun applyStyleSkippingEmojiSpan(
        out: SpannableStringBuilder,
        base: Int,
        text: String,
        spanFactory: () -> Any
    ) {
        var runStart = -1
        for (i in text.indices) {
            val ch = text[i]
            if (!ch.isSurrogate()) {
                if (runStart == -1) runStart = i
            } else {
                if (runStart != -1) {
                    out.setSpan(spanFactory(), base + runStart, base + i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    runStart = -1
                }
            }
        }
        if (runStart != -1) {
            out.setSpan(spanFactory(), base + runStart, base + text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applySpanSkippingEmoji(
        out: SpannableStringBuilder,
        base: Int,
        text: String,
        spanFactory: () -> Any
    ) {
        var runStart = -1
        for (i in text.indices) {
            val ch = text[i]
            if (!ch.isSurrogate()) {
                if (runStart == -1) runStart = i
            } else {
                if (runStart != -1) {
                    out.setSpan(spanFactory(), base + runStart, base + i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    runStart = -1
                }
            }
        }
        if (runStart != -1) {
            out.setSpan(spanFactory(), base + runStart, base + text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyNestedInlineSpans(
        out: SpannableStringBuilder,
        baseOffset: Int,
        innerText: String,
        outerMarker: Char
    ) {
        for (match in nestedPattern.findAll(innerText)) {
            val nestedContent = match.value
            val nestedMarker = nestedContent[0]

            // Don't re-process the same marker type as the outer span
            if (nestedMarker == outerMarker) continue

            val nestedInner = nestedContent.substring(1, nestedContent.length - 1)
            if (nestedInner.isBlank()) continue
            if (nestedInner.first().isWhitespace() || nestedInner.last().isWhitespace()) continue

            // Absolute positions inside `out`
            val absMatchStart = baseOffset + match.range.first
            val absMatchEnd   = baseOffset + match.range.last + 1   // exclusive
            val absInnerStart = absMatchStart + 1
            val absInnerEnd   = absMatchEnd - 1

            if (absMatchEnd > out.length) continue

            // Dim the nested marker symbols
            out.setSpan(
                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                absMatchStart, absMatchStart + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            out.setSpan(
                ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                absMatchEnd - 1, absMatchEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply the nested style to the inner content
            when (nestedMarker) {
                '`' -> {
                    out.setSpan(TypefaceSpan("monospace"),        absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(BackgroundColorSpan(backgroundColorSpan), absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                '_' -> out.setSpan(StyleSpan(Typeface.ITALIC),      absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                '*' -> out.setSpan(StyleSpan(Typeface.BOLD),         absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                '~' -> out.setSpan(StrikethroughSpan(),              absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun appendFormatted(out: SpannableStringBuilder) {
        var last = 0
        val cleanText = text

        if (codeBlockPattern.containsMatchIn(cleanText)) {
            for (match in codeBlockPattern.findAll(cleanText)) {
                if (match.range.first > last) out.append(cleanText.substring(last, match.range.first))

                val content = match.value
                if (content.startsWith("```")) {
                    val innerText = content.substring(3, content.length - 3)

                    val openStart = out.length
                    out.append("```")
                    out.setSpan(
                        ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                        openStart, openStart + 3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    val monoStart = out.length
                    out.append(innerText)
                    val monoEnd = out.length

                    applySpanSkippingEmoji(out, monoStart, innerText) { TypefaceSpan("monospace") }
                    applySpanSkippingEmoji(out, monoStart, innerText) { BackgroundColorSpan(Color.TRANSPARENT) }
                    val closeStart = out.length
                    out.append("```")
                    out.setSpan(
                        ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                        closeStart, closeStart + 3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    out.setSpan(TypefaceSpan("monospace"), monoStart, monoEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(BackgroundColorSpan(Color.TRANSPARENT), monoStart, monoEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                last = match.range.last + 1
            }
        } else {
            for (match in pattern.findAll(cleanText)) {
                if (match.range.first > last) out.append(cleanText.substring(last, match.range.first))

                val prev = cleanText.getOrNull(match.range.first - 1)
                val next = cleanText.getOrNull(match.range.last + 1)

                val content = match.value
                when {
                    // 1: BOLD (*text*)
                    content.startsWith('*') -> {
                        val markerChar = '*'
                        val innerText = content.substring(1, content.length - 1)
                        if (!hasWordBoundaries(cleanText, match.range) ||
                            innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r') ||
                            innerText.first() == markerChar || innerText.last() == markerChar ||
                            prev == markerChar || next == markerChar
                        ) {
                            out.append(content); last = match.range.last + 1; continue
                        }

                        val startSymbol = out.length
                        out.append("*")
                        out.setSpan(
                            ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                            startSymbol,
                            startSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)
                        applyStyleSkippingEmojiSpan(out, start, innerText) { StyleSpan(Typeface.BOLD) }
                        applyNestedInlineSpans(out, start, innerText, markerChar)
                        val endSymbol = out.length
                        out.append("*")
                        out.setSpan(
                            ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                            endSymbol,
                            endSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // 2: ITALIC (_text_)
                    content.startsWith("_") -> {
                        val markerChar = '_'
                        val innerText = content.substring(1, content.length - 1)
                        if (!hasWordBoundaries(cleanText, match.range) ||
                            innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r') ||
                            innerText.first() == markerChar || innerText.last() == markerChar ||
                            prev == markerChar || next == markerChar
                        ) {
                            out.append(content); last = match.range.last + 1; continue
                        }

                        val startSymbol = out.length
                        out.append("_")
                        out.setSpan(
                            ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                            startSymbol,
                            startSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)
                        applyStyleSkippingEmojiSpan(out, start, innerText) { StyleSpan(Typeface.ITALIC) }
                        applyNestedInlineSpans(out, start, innerText, markerChar)
                        val endSymbol = out.length
                        out.append("_")
                        out.setSpan(
                            ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                            endSymbol,
                            endSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // 3: STRIKETHROUGH (~text~)
                    content.startsWith("~") -> {
                        val markerChar = '~'
                        val innerText = content.substring(1, content.length - 1)
                        if (!hasWordBoundaries(cleanText, match.range) ||
                            innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r') ||
                            innerText.first() == markerChar || innerText.last() == markerChar ||
                            prev == markerChar || next == markerChar
                        ) {
                            out.append(content); last = match.range.last + 1; continue
                        }

                        val startSymbol = out.length
                        out.append("~")
                        out.setSpan(
                            ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                            startSymbol,
                            startSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)
                        applyStyleSkippingEmojiSpan(out, start, innerText) { StrikethroughSpan() }
                        applyNestedInlineSpans(out, start, innerText, markerChar)
                        val endSymbol = out.length
                        out.append("~")
                        out.setSpan(
                            ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                            endSymbol,
                            endSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // 4: INLINE CODE (`code`)
                    content.startsWith("`") -> {
                        val markerChar = '`'
                        val innerText = content.substring(1, content.length - 1)
                        if (!hasWordBoundaries(cleanText, match.range) ||
                            innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r') ||
                            innerText.first() == markerChar || innerText.last() == markerChar ||
                            prev == markerChar || next == markerChar
                        ) {
                            out.append(content); last = match.range.last + 1; continue
                        }

                        val startSymbol = out.length
                        out.append("`")
                        out.setSpan(
                            ForegroundColorSpan(getSymbolColor(isDarkTheme)),
                            startSymbol,
                            startSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)
                        applySpanSkippingEmoji(out, start, innerText) { TypefaceSpan("monospace") }
                        applySpanSkippingEmoji(out, start, innerText) {
                            BackgroundColorSpan(
                                backgroundColorSpan
                            )
                        }
                        val end=out.length
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
                last = match.range.last + 1
            }
        }

        if (last < cleanText.length) out.append(cleanText.substring(last))
    }
}

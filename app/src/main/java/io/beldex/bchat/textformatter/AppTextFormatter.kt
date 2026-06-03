package io.beldex.bchat.textformatter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import io.beldex.bchat.R


class AppTextFormatter(private val text: String, val context: Context, imageInputBar: Boolean) {
    private val monospacePattern = Regex("(?s)```(.*?)```")
    // Disallow newlines inside inline markers and keep backticks single
    private val pattern = Regex(
        "(\\*\\*\\*([^*\\r\\n]+?)\\*\\*\\*)|" +   // ***text***
                "(\\*\\*([^*\\r\\n]+?)\\*\\*)|" +         // **text**
                "(?<![A-Za-z0-9])\\*([^\\r\\n]+?)\\*(?![A-Za-z0-9])|" +  // *text*
                "(?<![A-Za-z0-9])_([^\\r\\n]+?)_(?![A-Za-z0-9])|" +      // _text_
                "(?<![A-Za-z0-9])~([^\\r\\n]+?)~(?![A-Za-z0-9])|" +      // ~text~
                "(?<![A-Za-z0-9])`([^\\r\\n`]+?)`(?![A-Za-z0-9])"        // `text`
    )

    private val nestedPattern = Regex(
        "`([^\\r\\n`]+?)`|" +
                "_([^\\r\\n]+?)_|" +
                "~([^\\r\\n]+?)~|" +
                "\\*([^\\r\\n]+?)\\*"
    )

    private val foregroundColorSpan = if(imageInputBar) ContextCompat.getColor(context, R.color.foreground_image_input_bar_symbol) else ContextCompat.getColor(context, R.color.foreground_symbol)
    private val backgroundColorSpan = ContextCompat.getColor(context, R.color.background_color_span)

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
                ForegroundColorSpan(foregroundColorSpan),
                absMatchStart, absMatchStart + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            out.setSpan(
                ForegroundColorSpan(foregroundColorSpan),
                absMatchEnd - 1, absMatchEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply the nested style to the inner content
            when (nestedMarker) {
                '`' -> {
                    out.setSpan(MonospaceSpan(),        absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(BackgroundColorSpan(backgroundColorSpan), absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                '_' -> out.setSpan(StyleSpan(Typeface.ITALIC),      absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                '*' -> out.setSpan(StyleSpan(Typeface.BOLD),         absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                '~' -> out.setSpan(StrikethroughSpan(),              absInnerStart, absInnerEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun isValidOpening(text: String, index: Int): Boolean {
        // block letter before (okay*text)
        if (index > 0 && text[index - 1].isLetterOrDigit()) return false

        // block space after (* text)
        if (index + 1 < text.length && text[index + 1].isWhitespace()) return false

        return true
    }

    private fun isValidClosing(text: String, index: Int): Boolean {
        // block space before (*text *)
        if (index > 0 && text[index - 1].isWhitespace()) return false

        return true
    }

    private fun findClosingChar(
        text: String,
        start: Int,
        marker: Char,
        endLimit: Int
    ): Int {
        var i = start
        var lastValid = -1

        while (i < endLimit) {
            if (text[i] == marker) {
                val inner = text.substring(start, i)

                if (inner.isNotBlank() &&
                    !inner.first().isWhitespace() &&
                    !inner.last().isWhitespace()
                ) {
                    lastValid = i
                }
            }

            if (text[i] == '\n' || text[i] == '\r') break
            i++
        }

        return lastValid
    }

    fun appendFormatted(out: SpannableStringBuilder) {
        var last = 0
        val cleanText = text

        if (monospacePattern.containsMatchIn(cleanText)) {
            for (match in monospacePattern.findAll(cleanText)) {
                if (match.range.first > last) out.append(cleanText.substring(last, match.range.first))

                val content = match.value
                if (content.startsWith("```")) {
                    val innerText = content.substring(3, content.length - 3)

                    val openStart = out.length
                    out.append("```")
                    out.setSpan(
                        ForegroundColorSpan(foregroundColorSpan),
                        openStart, openStart + 3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    val monoStart = out.length
                    out.append(innerText)
                    val monoEnd = out.length

                    applySpanSkippingEmoji(out, monoStart, innerText) { MonospaceSpan() }
                    applySpanSkippingEmoji(out, monoStart, innerText) { BackgroundColorSpan(Color.TRANSPARENT) }
                    val closeStart = out.length
                    out.append("```")
                    out.setSpan(
                        ForegroundColorSpan(foregroundColorSpan),
                        closeStart, closeStart + 3,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    out.setSpan(MonospaceSpan(), monoStart, monoEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(BackgroundColorSpan(Color.TRANSPARENT), monoStart, monoEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                last = match.range.last + 1
            }
        } else {
            for (match in pattern.findAll(cleanText)) {
                if (match.range.first > last) out.append(cleanText.substring(last, match.range.first))

                val content = match.value
                when {
                    content.startsWith("***") -> {
                        // Plain text
                        out.append(content)
                        last = match.range.last + 1
                    }
                    content.startsWith("**") -> {
                        val absoluteStart = match.range.first
                        val startIndex = absoluteStart + 2

                        // block "** text"
                        if (startIndex < cleanText.length && cleanText[startIndex].isWhitespace()) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val betterEnd = cleanText.indexOf("**", startIndex)

                        if (betterEnd == -1 ||
                            (betterEnd > 0 && cleanText[betterEnd - 1].isWhitespace())
                        ) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = cleanText.substring(startIndex, betterEnd)

                        if (innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace()
                        ) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val startSymbol = out.length
                        out.append("**")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
                            startSymbol,
                            startSymbol + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)

                        applyStyleSkippingEmojiSpan(out, start, innerText) {
                            StyleSpan(Typeface.BOLD)
                        }

                        applyNestedInlineSpans(out, start, innerText, '*')

                        val endSymbol = out.length
                        out.append("**")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
                            endSymbol,
                            endSymbol + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        last = betterEnd + 2
                    }
                    // 1: BOLD (*text*)
                    content.startsWith("*") -> {
                        val markerChar = '*'
                        val absoluteStart = match.range.first
                        val startIndex = absoluteStart + 1

                        if (!isValidOpening(cleanText, absoluteStart)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val betterEnd = findClosingChar(cleanText, startIndex, '*', match.range.last+1)

                        if (betterEnd == -1 || !isValidClosing(cleanText, betterEnd)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val nextCharIndex = betterEnd + 1
                        val nextStar = cleanText.indexOf('*', nextCharIndex)

                        // block only if:
                        // 1. next char is letter/digit
                        // 2. AND no more '*' ahead (i.e. not chaining)
                        if (
                            nextCharIndex < cleanText.length &&
                            cleanText[nextCharIndex].isLetterOrDigit() &&
                            nextStar == -1
                        ) {
                            // e.g. "*test*ok" → INVALID
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = cleanText.substring(startIndex, betterEnd)
                        if (innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r')
                        ) {
                            out.append(cleanText.substring(absoluteStart, betterEnd + 1))
                            last = betterEnd + 1
                            continue
                        }

                        val startSymbol = out.length
                        out.append("*")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
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
                            ForegroundColorSpan(foregroundColorSpan),
                            endSymbol,
                            endSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        last = betterEnd + 1
                    }

                    content.startsWith("___") -> {
                        // Plain text
                        out.append(content)
                        last = match.range.last + 1
                    }

                    content.startsWith("__") -> {
                        val absoluteStart = match.range.first
                        val startIndex = absoluteStart + 2

                        // block "__ text"
                        if (startIndex < cleanText.length && cleanText[startIndex].isWhitespace()) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val betterEnd = cleanText.indexOf("__", startIndex)

                        if (betterEnd == -1 ||
                            (betterEnd > 0 && cleanText[betterEnd - 1].isWhitespace())
                        ) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = cleanText.substring(startIndex, betterEnd)

                        if (innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace()
                        ) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val startSymbol = out.length
                        out.append("__")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
                            startSymbol,
                            startSymbol + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)

                        applyStyleSkippingEmojiSpan(out, start, innerText) {
                            StyleSpan(Typeface.ITALIC)
                        }

                        applyNestedInlineSpans(out, start, innerText, '_')

                        val endSymbol = out.length
                        out.append("__")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
                            endSymbol,
                            endSymbol + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        last = betterEnd + 2
                    }

                    // 2: ITALIC (_text_)
                    content.startsWith("_") -> {
                        val markerChar = '_'
                        val absoluteStart = match.range.first
                        val startIndex = absoluteStart + 1

                        if (!isValidOpening(cleanText, absoluteStart)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val betterEnd = findClosingChar(cleanText, startIndex, '_', match.range.last+1)

                        if (betterEnd == -1 || !isValidClosing(cleanText, betterEnd)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val nextCharIndex = betterEnd + 1
                        val nextStar = cleanText.indexOf('_', nextCharIndex)

                        // block only if:
                        // 1. next char is letter/digit
                        // 2. AND no more '_' ahead (i.e. not chaining)
                        if (
                            nextCharIndex < cleanText.length &&
                            cleanText[nextCharIndex].isLetterOrDigit() &&
                            nextStar == -1
                        ) {
                            // e.g. "_test_ok" → INVALID
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = cleanText.substring(startIndex, betterEnd)
                        if (innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r')
                        ) {
                            out.append(cleanText.substring(absoluteStart, betterEnd + 1))
                            last = betterEnd + 1
                            continue
                        }

                        val startSymbol = out.length
                        out.append("_")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
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
                            ForegroundColorSpan(foregroundColorSpan),
                            endSymbol,
                            endSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        last = betterEnd + 1
                    }

                    content.startsWith("~~~") -> {
                        // Plain text
                        out.append(content)
                        last = match.range.last + 1
                    }

                    content.startsWith("~~") -> {
                        val absoluteStart = match.range.first
                        val startIndex = absoluteStart + 2

                        // block "~~ text"
                        if (startIndex < cleanText.length && cleanText[startIndex].isWhitespace()) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val betterEnd = cleanText.indexOf("~~", startIndex)

                        if (betterEnd == -1 ||
                            (betterEnd > 0 && cleanText[betterEnd - 1].isWhitespace())
                        ) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = cleanText.substring(startIndex, betterEnd)

                        if (innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace()
                        ) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val startSymbol = out.length
                        out.append("~~")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
                            startSymbol,
                            startSymbol + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)

                        applyStyleSkippingEmojiSpan(out, start, innerText) {
                            StrikethroughSpan()
                        }

                        applyNestedInlineSpans(out, start, innerText, '~')

                        val endSymbol = out.length
                        out.append("~~")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
                            endSymbol,
                            endSymbol + 2,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        last = betterEnd + 2
                    }

                    // 3: STRIKETHROUGH (~text~)
                    content.startsWith("~") -> {
                        val markerChar = '~'
                        val absoluteStart = match.range.first
                        val startIndex = absoluteStart + 1

                        if (!isValidOpening(cleanText, absoluteStart)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val betterEnd = findClosingChar(cleanText, startIndex, '~', match.range.last+1)

                        if (betterEnd == -1 || !isValidClosing(cleanText, betterEnd)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val nextCharIndex = betterEnd + 1
                        val nextStar = cleanText.indexOf('~', nextCharIndex)

                        // block only if:
                        // 1. next char is letter/digit
                        // 2. AND no more '~' ahead (i.e. not chaining)
                        if (
                            nextCharIndex < cleanText.length &&
                            cleanText[nextCharIndex].isLetterOrDigit() &&
                            nextStar == -1
                        ) {
                            // e.g. "~test~ok" → INVALID
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = cleanText.substring(startIndex, betterEnd)
                        if (innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r')
                        ) {
                            out.append(cleanText.substring(absoluteStart, betterEnd + 1))
                            last = betterEnd + 1
                            continue
                        }

                        val startSymbol = out.length
                        out.append("~")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
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
                            ForegroundColorSpan(foregroundColorSpan),
                            endSymbol,
                            endSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        last = betterEnd + 1
                    }

                    // 4: INLINE CODE (`code`)
                    content.startsWith("`") -> {
                        val absoluteStart = match.range.first
                        val startIndex = absoluteStart + 1

                        if (!isValidOpening(cleanText, absoluteStart)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val betterEnd = findClosingChar(cleanText, startIndex, '`', match.range.last+1)

                        if (betterEnd == -1 || !isValidClosing(cleanText, betterEnd)) {
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val nextCharIndex = betterEnd + 1
                        val nextStar = cleanText.indexOf('`', nextCharIndex)

                        // block only if:
                        // 1. next char is letter/digit
                        // 2. AND no more '`' ahead (i.e. not chaining)
                        if (
                            nextCharIndex < cleanText.length &&
                            cleanText[nextCharIndex].isLetterOrDigit() &&
                            nextStar == -1
                        ) {
                            // e.g. "`test`ok" → INVALID
                            out.append(content)
                            last = match.range.last + 1
                            continue
                        }

                        val innerText = cleanText.substring(startIndex, betterEnd)
                        if (innerText.isBlank() ||
                            innerText.first().isWhitespace() ||
                            innerText.last().isWhitespace() ||
                            innerText.contains('\n') || innerText.contains('\r')
                        ) {
                            out.append(cleanText.substring(absoluteStart, betterEnd + 1))
                            last = betterEnd + 1
                            continue
                        }

                        val startSymbol = out.length
                        out.append("`")
                        out.setSpan(
                            ForegroundColorSpan(foregroundColorSpan),
                            startSymbol,
                            startSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val start = out.length
                        out.append(innerText)
                        applySpanSkippingEmoji(out, start, innerText) { MonospaceSpan() }
                        applySpanSkippingEmoji(out, start, innerText) {
                            BackgroundColorSpan(
                                backgroundColorSpan
                            )
                        }
                        val end=out.length
                        out.setSpan(
                            MonospaceSpan(),
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
                            ForegroundColorSpan(foregroundColorSpan),
                            endSymbol,
                            endSymbol + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        last = betterEnd + 1
                    }
                }
            }
        }

        if (last < cleanText.length) out.append(cleanText.substring(last))
    }
}

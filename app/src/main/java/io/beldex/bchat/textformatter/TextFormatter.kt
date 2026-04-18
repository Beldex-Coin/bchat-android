package io.beldex.bchat.textformatter

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.graphics.toColorInt
import com.beldex.libsignal.utilities.Log


object TextFormatter {

    @JvmStatic
    fun formatAppText(input: CharSequence, context: Context): SpannableStringBuilder {
        val rawText = input.toString()
        val out = SpannableStringBuilder()
        val parser = AppTextFormatter(rawText, context)
        parser.appendFormatted(out)
        return out.ifEmpty { SpannableStringBuilder(rawText) }
    }

    @JvmStatic
    fun formatForSentMessage(rawText: CharSequence): SpannableStringBuilder {
        val sanitized = sanitizeLoneListMarkers(rawText.toString())
        val builder = SpannableStringBuilder(sanitized)

        var index = 0

        while (index < builder.length) {

            val lineEnd = builder.indexOf('\n', index).let {
                if (it == -1) builder.length else it
            }

            val line = builder.substring(index, lineEnd)

            val isNumberList = Regex("""^\d+\.\s""").containsMatchIn(line)
            val isBulletList = Regex("""^([\u2022\-\*])\s""").containsMatchIn(line)

            val sub = SpannableStringBuilder(line)

            // ---- CODE BLOCK ----
            applyRegexSpan(
                sub,
                Regex("(?s)(?<!`)```(?!`)(.+?)```(?!`)"),
                marker = '`',
                allowNewlines = true,
                enforceBoundaries = false
            ) {
                toUnicodeMonospace(it.groupValues[1])
            }

            // ---- INLINE CODE ----
            applyRegexSpan(sub, Regex("`([^\\r\\n`]+?)`"), marker = '`') {
                toUnicodeInlineCode(it.groupValues[1])
            }

            if (isNumberList || isBulletList) {

                // LIST → REGEX ENGINE
                applyRegexSpan(sub, Regex("""\*(?!\s)(.+?)(?<!\s)\*"""), marker = '*') {
                    toUnicodeBold(it.groupValues[1])
                }

                applyRegexSpan(sub, Regex("""_(?!\s)(.+?)(?<!\s)_"""), marker = '_') {
                   toUnicodeItalic(it.groupValues[1])
                }

                applyRegexSpan(sub, Regex("""~(?!\s)(.+?)(?<!\s)~"""), marker = '~') {
                    toUnicodeStrikethrough(it.groupValues[1])
                }

            } else {
                // NON-LIST → OLD ENGINE
                applyMarkerBold(sub)
                applyMarkerItalic(sub)
                applyMarkerStrikethrough(sub)
            }

            // SAFE REPLACE
            builder.replace(index, lineEnd, sub)

            // MOVE INDEX CORRECTLY
            index += sub.length

            // skip newline if exists
            if (index < builder.length && builder[index] == '\n') {
                index++
            }
        }

        toUnicodeBlockQuote(builder)

        return builder
    }

    fun SpannableStringBuilder.toAnnotatedString(): AnnotatedString {
        return buildAnnotatedString {
            append(this@toAnnotatedString.toString())

            getSpans(0, length, StyleSpan::class.java).forEach { span ->
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end
                    )
                }
            }

            getSpans(0, length, TypefaceSpan::class.java).forEach { span ->
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                if (span.family == "monospace") {
                    addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
                }
            }

            getSpans(0, length, BackgroundColorSpan::class.java).forEach { span ->
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                addStyle(SpanStyle(background = Color(span.backgroundColor)), start, end)
            }

            getSpans(0, length, StrikethroughSpan::class.java).forEach { span ->
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
            }

            getSpans(0, length, CustomQuoteSpan::class.java).forEach { span ->
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                addStyle(SpanStyle(color = Color.Gray, background = Color(0x1A888888)), start, end)
            }
        }
    }


    private fun sanitizeLoneListMarkers(raw: String): String {
        return raw.lines().joinToString("\n") { line ->

            val numberMatch = Regex("""^\s*(\d{1,3})\.\s*(.*)$""").find(line)

            if (numberMatch != null) {
                val number = numberMatch.groupValues[1].toIntOrNull()
                val content = numberMatch.groupValues[2]

                return@joinToString if (number != null && number in 1..99) {
                    "$number. $content"
                } else {
                    content // strip invalid numbers like 100+
                }
            }

            when {
                Regex("""^\s*-\s*$""").matches(line) -> "-"

                Regex("""^\s*\u2022\s*$""").matches(line) -> "*"

                else -> line
            }
        }
    }

    private fun applyStyleSkippingEmoji(
        text: String?,
        spanFactory: () -> Any
    ): CharSequence {
        if (text.isNullOrEmpty()) return ""
        val sb = SpannableStringBuilder(text)
        var start = -1
        for (i in text.indices) {
            val ch = text[i]
            if (!ch.isSurrogate()) {
                if (start == -1) start = i
            } else {
                if (start != -1) {
                    sb.setSpan(spanFactory(), start, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    start = -1
                }
            }
        }
        if (start != -1) {
            sb.setSpan(spanFactory(), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    private fun applySpanSkippingEmoji(builder: SpannableStringBuilder, spanFactory: () -> Any) {
        var runStart = -1
        for (i in builder.indices) {
            val ch = builder[i]
            if (!ch.isSurrogate()) {
                if (runStart == -1) runStart = i
            } else {
                if (runStart != -1) {
                    builder.setSpan(spanFactory(), runStart, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    runStart = -1
                }
            }
        }
        if (runStart != -1) {
            builder.setSpan(spanFactory(), runStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyRegexSpan(
        builder: SpannableStringBuilder,
        regex: Regex,
        marker: Char,
        allowNewlines: Boolean = false,
        enforceBoundaries: Boolean = true,
        replacement: (MatchResult) -> CharSequence
    ) {
        val matches = regex.findAll(builder).toList().asReversed()
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last
            val prev = builder.getOrNull(start - 1)
            val next = builder.getOrNull(end + 1)

            if (prev == marker || next == marker) continue

            val innerText = match.groupValues.getOrNull(1) ?: ""
            if (innerText.isBlank()) continue
            if (!allowNewlines && (innerText.contains('\n') || innerText.contains('\r'))) continue
            if (innerText.first().isWhitespace() || innerText.last().isWhitespace()) continue
            if (innerText.startsWith(marker) || innerText.endsWith(marker)) continue // blocks **hello**, __hi__, ~~ok~~, etc.
            if (enforceBoundaries && (prev?.isLetterOrDigit() == true || next?.isLetterOrDigit() == true)) continue

            val replacementText = replacement(match)
            if (replacementText.isEmpty()) continue
            builder.replace(start, end + 1, replacementText)
        }
    }

    private fun applyMarkerBold(builder: SpannableStringBuilder) {
        val text = builder.toString()
        if (text.isEmpty()) return

        try {
            val startCount = text.takeWhile { it == '*' }.length
            val endCount = text.reversed().takeWhile { it == '*' }.length

            if (startCount != endCount || startCount !in 1..2) return

            val inner = text.substring(startCount, text.length - endCount)

            if (inner.isBlank() ||
                inner.first().isWhitespace() ||
                inner.last().isWhitespace()
            ) return

            // ===== **hello** =====
            if (startCount == 2) {
                builder.replace(0, 2, "*")
                builder.replace(builder.length - 2, builder.length, "*")

                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // ===== *hello* =====
            else {
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    1,
                    text.length - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                builder.delete(builder.length - 1, builder.length)
                builder.delete(0, 1)
            }

        } catch (ex: Exception) {
            Log.e("TextFormatter", "Bold span failed", ex)
        }
    }

    private fun applyMarkerItalic(builder: SpannableStringBuilder) {
        val text = builder.toString()
        if (text.isEmpty()) return

        try {
            val startCount = text.takeWhile { it == '_' }.length
            val endCount = text.reversed().takeWhile { it == '_' }.length

            if (startCount != endCount || startCount !in 1..2) return

            val innerStart = startCount
            val innerEnd = text.length - endCount
            val inner = text.substring(innerStart, innerEnd)

            if (inner.isBlank() ||
                inner.first().isWhitespace() ||
                inner.last().isWhitespace()
            ) return

            // ===== __hello__ → _hello_ (italic) =====
            if (startCount == 2) {
                builder.replace(0, 2, "_")
                builder.replace(builder.length - 2, builder.length, "_")
            }

            // apply italic
            builder.setSpan(
                StyleSpan(Typeface.ITALIC),
                innerStart,
                builder.length - endCount,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // HANDLE NESTED BOLD (*hello*)
            if (inner.startsWith("*") && inner.endsWith("*") && inner.length > 2) {
                // apply bold
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    innerStart + 1,
                    builder.length - endCount - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // REMOVE inner *
                builder.delete(builder.length - endCount - 1, builder.length - endCount)
                builder.delete(innerStart, innerStart + 1)
            }

            // remove markers for single `_`
            if (startCount == 1) {
                builder.delete(builder.length - 1, builder.length)
                builder.delete(0, 1)
            }

        } catch (ex: Exception) {
            Log.e("TextFormatter", "Italic span failed", ex)
        }
    }

    private fun applyMarkerStrikethrough(builder: SpannableStringBuilder) {
        val text = builder.toString()
        if (text.isEmpty()) return

        try {
            val startCount = text.takeWhile { it == '~' }.length
            val endCount = text.reversed().takeWhile { it == '~' }.length

            if (startCount != endCount || startCount !in 1..2) return

            val innerStart = startCount
            val innerEnd = text.length - endCount
            val inner = text.substring(innerStart, innerEnd)

            if (inner.isBlank() ||
                inner.first().isWhitespace() ||
                inner.last().isWhitespace()
            ) return

            // ===== ~~hello~~ → ~hello~ =====
            if (startCount == 2) {
                builder.replace(0, 2, "~")
                builder.replace(builder.length - 2, builder.length, "~")
            }

            // apply strikethrough
            builder.setSpan(
                StrikethroughSpan(),
                innerStart,
                builder.length - endCount,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // HANDLE NESTED ITALIC (_hello_)
            if (inner.startsWith("_") && inner.endsWith("_") && inner.length > 2) {

                // apply italic
                builder.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    innerStart + 1,
                    builder.length - endCount - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // REMOVE inner _
                builder.delete(builder.length - endCount - 1, builder.length - endCount)
                builder.delete(innerStart, innerStart + 1)
            }

            // HANDLE NESTED BOLD (*hello*)
            if (inner.startsWith("*") && inner.endsWith("*") && inner.length > 2) {

                // apply bold
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    innerStart + 1,
                    builder.length - endCount - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // REMOVE inner *
                builder.delete(builder.length - endCount - 1, builder.length - endCount)
                builder.delete(innerStart, innerStart + 1)
            }

            // remove outer ~ if single
            if (startCount == 1) {
                builder.delete(builder.length - 1, builder.length)
                builder.delete(0, 1)
            }

        } catch (ex: Exception) {
            Log.e("TextFormatter", "Strikethrough span failed", ex)
        }
    }

    @JvmStatic
    fun toUnicodeBold(text: String?): CharSequence =
        applyStyleSkippingEmoji(text) { StyleSpan(Typeface.BOLD) }

    @JvmStatic
    fun toUnicodeItalic(text: String?): CharSequence =
        applyStyleSkippingEmoji(text) { StyleSpan(Typeface.ITALIC) }

    @JvmStatic
    fun toUnicodeMonospace(text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableStringBuilder(text).apply {
            applySpanSkippingEmoji(this) { TypefaceSpan("monospace") }
        }

    @JvmStatic
    fun toUnicodeInlineCode(text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableStringBuilder(text).apply {
            applySpanSkippingEmoji(this) { TypefaceSpan("monospace") }
            applySpanSkippingEmoji(this) { BackgroundColorSpan("#797984".toColorInt()) }
        }

    @JvmStatic
    fun toUnicodeStrikethrough(text: String?): CharSequence =
        applyStyleSkippingEmoji(text) { StrikethroughSpan() }


    fun toUnicodeBlockQuote(builder: Editable) {
        val spans = builder.getSpans(0, builder.length, CustomQuoteSpan::class.java)
        spans.forEach { builder.removeSpan(it) }
        Regex("(?m)^>\\s").findAll(builder).forEach { match ->
            builder.setSpan(
                CustomQuoteSpan(),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
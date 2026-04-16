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

        applyRegexSpan(builder, Regex("(?s)(?<!`)```(?!`)(.+?)```(?!`)"), marker = '`', allowNewlines = true, enforceBoundaries = false) {
            toUnicodeMonospace(it.groupValues[1])
        }
        applyRegexSpan(builder, Regex("`([^\\r\\n`]+?)`"), marker = '`') { match ->
            toUnicodeInlineCode(match.groupValues[1])
        }
        applyMarkerSpan(builder, Regex("\\*([^\\r\\n]+?)\\*"), '*') { start, end ->
            applyStyleSkippingEmojiInRange(builder, start, end) {
                StyleSpan(Typeface.BOLD)
            }
        }
        applyMarkerSpan(builder, Regex("_([^\\r\\n]+?)_"), '_') { start, end ->
            applyStyleSkippingEmojiInRange(builder, start, end) {
                StyleSpan(Typeface.ITALIC)
            }
        }
        applyMarkerSpan(builder, Regex("~([^\\r\\n]+?)~"), '~') { start, end ->
            applyStyleSkippingEmojiInRange(builder, start, end) {
                StrikethroughSpan()
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

    private fun applyStyleSkippingEmojiInRange(
        builder: SpannableStringBuilder,
        start: Int,
        end: Int,
        spanFactory: () -> Any
    ) {
        var runStart = -1

        for (i in start until end) {
            val ch = builder[i]

            if (!ch.isSurrogate()) {
                if (runStart == -1) runStart = i
            } else {
                if (runStart != -1) {
                    builder.setSpan(
                        spanFactory(),
                        runStart,
                        i,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    runStart = -1
                }
            }
        }

        if (runStart != -1) {
            builder.setSpan(
                spanFactory(),
                runStart,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
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
    private fun applyMarkerSpan(
        builder: SpannableStringBuilder,
        regex: Regex,
        marker: Char,
        spanApplier: (Int, Int) -> Unit
    ) {
        val matches = regex.findAll(builder).toList().asReversed()

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last

            val prev = builder.getOrNull(start - 1)
            val next = builder.getOrNull(end + 1)

            if (prev == marker || next == marker) continue

            val innerStart = start + 1
            val innerEnd = end

            val innerText = builder.substring(innerStart, innerEnd)

            if (innerText.isBlank()) continue
            if (innerText.first().isWhitespace() || innerText.last().isWhitespace()) continue
            if (innerText.contains('\n') || innerText.contains('\r')) continue

            builder.delete(end, end + 1)
            builder.delete(start, start + 1)

            spanApplier(start, end - 1)
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
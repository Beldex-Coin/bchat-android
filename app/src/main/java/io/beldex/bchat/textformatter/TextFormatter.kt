package io.beldex.bchat.textformatter

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.graphics.toColorInt


object TextFormatter {

    @JvmStatic
    fun formatAppText(input: CharSequence, context: Context): SpannableStringBuilder {
        val rawText = input.toString()
        val out = SpannableStringBuilder()
        val parser = AppTextFormatter(rawText, context)
        parser.appendFormatted(out)
        return if (out.isEmpty()) SpannableStringBuilder(rawText) else out
    }

    @JvmStatic
    fun formatForSentMessage(rawText: CharSequence): SpannableStringBuilder {
        val builder = SpannableStringBuilder(rawText)

        applyRegexSpan(builder, Regex("(?s)(?<!`)```(?!`)(.+?)```(?!`)"), marker = '`', allowNewlines = true, enforceBoundaries = false) {
            toUnicodeMonospace(it.groupValues[1])
        }
        applyRegexSpan(builder, Regex("`([^\\r\\n`]+?)`"), marker = '`') { match ->
            toUnicodeInlineCode(match.groupValues[1])
        }
        applyRegexSpan(builder, Regex("\\*([^\\r\\n]+?)\\*"), marker = '*') { match ->
            toUnicodeBold(match.groupValues[1])
        }
        applyRegexSpan(builder, Regex("_([^\\r\\n]+?)_"), marker = '_') { match ->
            toUnicodeItalic(match.groupValues[1])
        }
        applyRegexSpan(builder, Regex("~([^\\r\\n]+?)~"), marker = '~') { match ->
            toUnicodeStrikethrough(match.groupValues[1])
        }

        toUnicodeBlockQuote(builder)
        return builder
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

    @JvmStatic
    fun toUnicodeBold(text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    @JvmStatic
    fun toUnicodeItalic(text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    @JvmStatic
    fun toUnicodeMonospace(text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableString(text).apply {
            setSpan(TypefaceSpan("monospace"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    @JvmStatic
    fun toUnicodeInlineCode(text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableString(text).apply {
            setSpan(TypefaceSpan("monospace"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(BackgroundColorSpan("#797984".toColorInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    @JvmStatic
    fun toUnicodeStrikethrough(text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableString(text).apply {
            setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }


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
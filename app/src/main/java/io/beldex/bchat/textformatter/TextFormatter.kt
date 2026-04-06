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

        applyRegexSpan(builder, Regex("(?s)(?<!`)```(?!`)(.+?)```(?!`)")) { match ->
            toUnicodeMonospace(match.groupValues[1])
        }
        applyRegexSpan(builder, Regex("(?<!`)`(?!`)(.+?)`(?!`)")) { match ->
            toUnicodeInlineCode(match.groupValues[1])
        }
        applyRegexSpan(builder, Regex("(?<!\\*)\\*(?!\\*)(.+?)\\*(?!\\*)")) { match ->
            toUnicodeBold(match.groupValues[1])
        }
        applyRegexSpan(builder, Regex("(?<!_)_(?!_)(.+?)_(?!_)")) { match ->
            toUnicodeItalic(match.groupValues[1])
        }
        applyRegexSpan(builder, Regex("(?<!~)~(?!~)(.+?)~(?!~)")) { match ->
            toUnicodeStrikethrough(match.groupValues[1])
        }

        toUnicodeBlockQuote(builder)
        return builder
    }

    private fun applyRegexSpan(
        builder: SpannableStringBuilder,
        regex: Regex,
        replacement: (MatchResult) -> CharSequence
    ) {
        val matches = regex.findAll(builder).toList().asReversed()
        for (match in matches) {
            val innerText = match.groupValues.getOrNull(1) ?: ""
            if (innerText.isBlank()) continue  // don't format empty/whitespace-only content
            val replacementText = replacement(match)
            if (replacementText.isEmpty()) continue
            builder.replace(match.range.first, match.range.last + 1, replacementText)
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
        Regex("> ").findAll(builder).forEach { match ->
            builder.setSpan(
                CustomQuoteSpan(),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
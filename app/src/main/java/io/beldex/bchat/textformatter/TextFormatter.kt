package io.beldex.bchat.textformatter

import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import androidx.core.graphics.toColorInt


object TextFormatter {

    @JvmStatic
    fun formatAppText(input: CharSequence): SpannableStringBuilder {
        val rawText = input.toString()

        val out = SpannableStringBuilder()
        val parser = AppTextFormatter(rawText)
        parser.appendFormatted(out)

        if (out.isEmpty()) {
            return SpannableStringBuilder(rawText)
        }

        return out
    }
    @JvmStatic
    fun formatForSentMessage(rawText: CharSequence): SpannableStringBuilder {
        val builder = SpannableStringBuilder(rawText)

        applyRegexSpan(builder, Regex("(?s)```(.*?)```")) { match ->
            toUnicodeMonospace(match.groupValues[1])
        }

        // Inline code first (important so it doesn’t break other spans)
        applyRegexSpan(
            builder,
            Regex("`(.*?)`")
        ) { match ->
            toUnicodeInlineCode(match.groupValues[1])
        }

        applyRegexSpan(builder, Regex("\\*(.*?)\\*")) { match ->
            toUnicodeBold(match.groupValues[1])
        }

        applyRegexSpan(builder, Regex("_(.*?)_")) { match ->
            toUnicodeItalic(match.groupValues[1])
        }

        applyRegexSpan(builder, Regex("~(.*?)~")) { match ->
            toUnicodeStrikethrough(match.groupValues[1])
        }

        applyRegexSpan(builder, Regex("~(.*?)~")) { match ->
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

            // 🚫 Skip ONLY if empty (this fixes ** issue)
            if (innerText.isEmpty()) {
                continue
            }

            val replacementText = replacement(match)

            // Extra safety
            if (replacementText.isEmpty()) {
                continue
            }

            builder.replace(match.range.first, match.range.last + 1, replacementText)
        }
    }

    @JvmStatic
    fun toUnicodeBold(text: String?): CharSequence {
        if (text.isNullOrEmpty()) return ""

        // Normal characters
        val normal = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        // Bold equivalents (Mathematical Alphanumeric Symbols)
        val bold = arrayOf(
            // Uppercase A–Z
            "𝗔","𝗕","𝗖","𝗗","𝗘","𝗙","𝗚","𝗛","𝗜","𝗝","𝗞","𝗟","𝗠","𝗡","𝗢","𝗣","𝗤","𝗥","𝗦","𝗧","𝗨","𝗩","𝗪","𝗫","𝗬","𝗭",
            // Lowercase a–z
            "𝗮","𝗯","𝗰","𝗱","𝗲","𝗳","𝗴","𝗵","𝗶","𝗷","𝗸","𝗹","𝗺","𝗻","𝗼","𝗽","𝗾","𝗿","𝘀","𝘁","𝘂","𝘃","𝘄","𝘅","𝘆","𝘇",
            // Digits 0–9
            "𝟬","𝟭","𝟮","𝟯","𝟰","𝟱","𝟲","𝟳","𝟴","𝟵"
        )

        // Map normal → bold
        val map = normal.mapIndexed { i, c -> c to bold[i] }.toMap()

        // Convert each character, replace if available
        val sb = StringBuilder()
        for (c in text) {
            sb.append(map[c] ?: c) // leave symbols like !@# unchanged
        }

        return sb.toString()
    }

    @JvmStatic
    fun toUnicodeItalic(text: String?): CharSequence {
        if (text.isNullOrEmpty()) return ""

        // Normal characters
        val normal = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        // Italic Unicode equivalents
        val italic = arrayOf(
            // A–Z
            "𝘈","𝘉","𝘊","𝘋","𝘌","𝘍","𝘎","𝘏","𝘐","𝘑","𝘒","𝘓","𝘔","𝘕","𝘖","𝘗","𝘘","𝘙","𝘚","𝘛","𝘜","𝘝","𝘞","𝘟","𝘠","𝘡",
            // a–z
            "𝘢","𝘣","𝘤","𝘥","𝘦","𝘧","𝘨","𝘩","𝘪","𝘫","𝘬","𝘭","𝘮","𝘯","𝘰","𝘱","𝘲","𝘳","𝘴","𝘵","𝘶","𝘷","𝘸","𝘹","𝘺","𝘻",
            // 0–9 (italic math digits)
            "𝟢","𝟣","𝟤","𝟥","𝟦","𝟧","𝟨","𝟩","𝟪","𝟫"
        )

        // Build map
        val map = normal.mapIndexed { i, c -> c to italic[i] }.toMap()

        val sb = StringBuilder()
        for (c in text) {
            sb.append(map[c] ?: c) // fallback for symbols and punctuation
        }

        return sb.toString()
    }

    @JvmStatic
    fun toUnicodeMonospace(text: String?): CharSequence {
        if (text.isNullOrEmpty()) return ""

        val normal = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val mono = arrayOf(
            // A–Z
            "𝙰","𝙱","𝙲","𝙳","𝙴","𝙵","𝙶","𝙷","𝙸","𝙹","𝙺","𝙻","𝙼","𝙽","𝙾","𝙿","𝚀","𝚁","𝚂","𝚃","𝚄","𝚅","𝚆","𝚇","𝚈","𝚉",
            // a–z
            "𝚊","𝚋","𝚌","𝚍","𝚎","𝚏","𝚐","𝚑","𝚒","𝚓","𝚔","𝚕","𝚖","𝚗","𝚘","𝚙","𝚚","𝚛","𝚜","𝚝","𝚞","𝚟","𝚠","𝚡","𝚢","𝚣",
            // 0–9
            "𝟶","𝟷","𝟸","𝟹","𝟺","𝟻","𝟼","𝟽","𝟾","𝟿"
        )

        val map = normal.mapIndexed { i, c -> c to mono[i] }.toMap()
        val sb = StringBuilder()
        for (c in text) {
            sb.append(map[c] ?: c) // keep symbols and punctuation unchanged
        }

        return sb.toString()
    }

    @JvmStatic
    fun toUnicodeInlineCode(text: String?): CharSequence {
        if (text.isNullOrEmpty()) return ""

        val normal = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val mono = arrayOf(
            // A–Z
            "𝙰","𝙱","𝙲","𝙳","𝙴","𝙵","𝙶","𝙷","𝙸","𝙹","𝙺","𝙻","𝙼","𝙽","𝙾","𝙿","𝚀","𝚁","𝚂","𝚃","𝚄","𝚅","𝚆","𝚇","𝚈","𝚉",
            // a–z
            "𝚊","𝚋","𝚌","𝚍","𝚎","𝚏","𝚐","𝚑","𝚒","𝚓","𝚔","𝚕","𝚖","𝚗","𝚘","𝚙","𝚚","𝚛","𝚜","𝚝","𝚞","𝚟","𝚠","𝚡","𝚢","𝚣",
            // 0–9
            "𝟶","𝟷","𝟸","𝟹","𝟺","𝟻","𝟼","𝟽","𝟾","𝟿"
        )

        val map = normal.mapIndexed { i, c -> c to mono[i] }.toMap()
        val sb = StringBuilder()
        for (c in text) {
            sb.append(map[c] ?: c) // keep symbols and punctuation unchanged
        }

        val result = SpannableString(sb.toString())
        result.setSpan(
            BackgroundColorSpan("#797984".toColorInt()),
            0,
            result.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return result
    }

    @JvmStatic
    fun toUnicodeStrikethrough(text: String?): CharSequence {
        if (text.isNullOrEmpty()) return ""
        return buildString {
            for (c in text) {
                append(c)
                append('\u0336') // Unicode combining long stroke overlay
            }
        }
    }

    fun toUnicodeBlockQuote(builder: Editable) {
        // Remove previous spans
        val spans = builder.getSpans(0, builder.length, CustomQuoteSpan::class.java)
        spans.forEach { builder.removeSpan(it) }
        // Find occurrences of "> "
        val regex = Regex("> ")
        regex.findAll(builder).forEach { match ->
            builder.setSpan(
                CustomQuoteSpan(),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
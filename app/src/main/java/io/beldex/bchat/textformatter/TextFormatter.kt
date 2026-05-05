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
import androidx.core.content.ContextCompat
import io.beldex.bchat.R


object TextFormatter {

    @JvmStatic
    fun formatAppText(input: CharSequence, context: Context, imageInputBar: Boolean = false): SpannableStringBuilder {
        val rawText = input.toString()
        val out = SpannableStringBuilder()
        val parser = AppTextFormatter(rawText, context, imageInputBar)
        parser.appendFormatted(out)
        return out.ifEmpty { SpannableStringBuilder(rawText) }
    }

    @JvmStatic
    fun formatForSentMessage(context: Context, rawText: CharSequence): SpannableStringBuilder {
        val sanitized = sanitizeLoneListMarkers(rawText.toString())
        val builder = SpannableStringBuilder(sanitized)

        var index = 0

        while (index < builder.length) {

            val lineEnd = builder.indexOf('\n', index).let {
                if (it == -1) builder.length else it
            }

            val line = builder.substring(index, lineEnd)
            val isBulletList = Regex("""^\s*[\u2022\-\*]\s""").containsMatchIn(line)

            var sub = SpannableStringBuilder(line)

            if (isBulletList) {
                sub = convertBulletMarkers(sub)
            }

            // ---- CODE BLOCK ----
            applyRegexSpan(
                sub,
                Regex("(?s)(?<!`)```(?!`)(.+?)```(?!`)"),
                marker = '`',
                allowNewlines = true
            ) {
                toUnicodeMonospace(it.groupValues[1])
            }

            // ---- INLINE CODE ----
            applyRegexSpan(sub, Regex("`([^\\r\\n`]+?)`"), marker = '`') {
                toUnicodeInlineCode(context, it.groupValues[1])
            }

            // ---- BOLD, ITALIC and STRIKETHROUGH ----
            applyInlineFormatting(context, sub)

            // SAFE REPLACE
            builder.replace(index, lineEnd, sub)

            // MOVE INDEX CORRECTLY
            index += sub.length

            // skip newline if exists
            if (index < builder.length && builder[index] == '\n') {
                index++
            }
        }

        toUnicodeBlockQuote(context, builder, removeMarker = true)

        return builder
    }

    private fun applyInlineFormatting(context: Context, builder: SpannableStringBuilder) {
        if (handleFullLineFormat(
                builder,
                '*',
                "**",
                validateInner = {
                    it.isNotBlank() &&
                            !it.first().isWhitespace() &&
                            !it.last().isWhitespace()
                },
                applySpan = { start, end ->
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )) return
        if (handleFullLineFormat(
                builder,
                '_',
                "__",
                validateInner = {
                    it.isNotBlank() &&
                            !it.first().isWhitespace() &&
                            !it.last().isWhitespace()
                },
                applySpan = { start, end ->
                    builder.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )) return
        if (handleFullLineFormat(
                builder,
                '~',
                "~~",
                validateInner = {
                    it.isNotBlank() &&
                            !it.first().isWhitespace() &&
                            !it.last().isWhitespace()
                },
                applySpan = { start, end ->
                    builder.setSpan(
                        StrikethroughSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )) return
        if (handleFullLineFormat(
                builder,
                '`',
                "``",
                validateInner = {
                    it.isNotBlank()
                },
                applySpan = { start, end ->
                    builder.setSpan(
                        TypefaceSpan("monospace"),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        BackgroundColorSpan(
                            ContextCompat.getColor(context, R.color.background_color_span)
                        ),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )) return

        var i = 0

        while (i < builder.length) {

            val length = builder.length

            // =========================
            // ESCAPE BLOCKS (*** ___ ~~~)
            // =========================
            if (i + 2 < length) {
                val triple = builder.substring(i, i + 3)
                if (triple == "***" || triple == "___" || triple == "~~~") {
                    val end = builder.indexOf(triple, i + 3)
                    if (end != -1) {
                        i = end + 3
                        continue
                    }
                }
            }

            // =====================================================
            // BOLD (*text* OR **text**)
            // =====================================================
            if (builder[i] == '*') {

                val isDouble = i + 1 < length && builder[i + 1] == '*'

                if (isDouble) {
                    val start = i + 2
                    val end = builder.indexOf("**", start)

                    if (end != -1) {
                        val inner = builder.substring(start, end)

                        if (inner.isNotBlank() &&
                            !inner.first().isWhitespace() &&
                            !inner.last().isWhitespace()
                        ) {
                            // Apply bold
                            builder.setSpan(
                                StyleSpan(Typeface.BOLD),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            applyNestedFormatting(context, builder, start, end)

                            // Convert **text** -> *text*
                            if (end + 2 <= builder.length) {
                                builder.replace(end, end + 2, "*")
                            }
                            if (i + 2 <= builder.length) {
                                builder.replace(i, i + 2, "*")
                            }

                            // Move index forward instead of restarting
                            i = end
                            continue
                        }
                    }
                } else {
                    val start = i + 1
                    val end = builder.indexOf("*", start)

                    if (end != -1) {
                        val inner = builder.substring(start, end)

                        if (inner.isNotBlank() &&
                            !inner.first().isWhitespace() &&
                            !inner.last().isWhitespace() &&
                            isValidOpening(builder.toString(), i) &&
                            isValidClosing(builder, end)
                        ) {
                            builder.setSpan(
                                StyleSpan(Typeface.BOLD),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            applyNestedFormatting(context, builder, start, end)

                            // recompute safety
                            val safeEnd = builder.indexOf("*", i + 1)

                            if (safeEnd != -1) {
                                builder.delete(safeEnd, safeEnd + 1)
                            }
                            if (i < builder.length) {
                                builder.delete(i, i + 1)
                            }

                            i = (end - 1).coerceAtLeast(0)
                            continue
                        }
                    }
                }
            }

            // =====================================================
            // ITALIC (_text_ OR __text__)
            // =====================================================
            if (builder[i] == '_') {

                val isDouble = i + 1 < length && builder[i + 1] == '_'

                if (isDouble) {
                    val start = i + 2
                    val end = builder.indexOf("__", start)

                    if (end != -1) {
                        val inner = builder.substring(start, end)

                        if (inner.isNotBlank() &&
                            !inner.first().isWhitespace() &&
                            !inner.last().isWhitespace()
                        ) {
                            // Apply italic
                            builder.setSpan(
                                StyleSpan(Typeface.ITALIC),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            applyNestedFormatting(context, builder, start, end)

                            // Convert __text__ -> _text_
                            if (end + 2 <= builder.length) {
                                builder.replace(end, end + 2, "_")
                            }
                            if (i + 2 <= builder.length) {
                                builder.replace(i, i + 2, "_")
                            }

                            // Move index forward instead of restarting
                            i = end
                            continue
                        }
                    }
                } else {
                    val start = i + 1
                    val end = builder.indexOf("_", start)

                    if (end != -1) {
                        val inner = builder.substring(start, end)

                        if (inner.isNotBlank() &&
                            !inner.first().isWhitespace() &&
                            !inner.last().isWhitespace() &&
                            isValidOpening(builder.toString(), i) &&
                            isValidClosing(builder, end)
                        ) {
                            builder.setSpan(
                                StyleSpan(Typeface.ITALIC),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            applyNestedFormatting(context, builder, start, end)

                            // recompute safety
                            val safeEnd = builder.indexOf("_", i + 1)

                            if (safeEnd != -1) {
                                builder.delete(safeEnd, safeEnd + 1)
                            }
                            if (i < builder.length) {
                                builder.delete(i, i + 1)
                            }

                            i = (end - 1).coerceAtLeast(0)
                            continue
                        }
                    }
                }
            }

            // =====================================================
            // STRIKETHROUGH (~text~)
            // =====================================================
            if (builder[i] == '~') {

                val isDouble = i + 1 < length && builder[i + 1] == '~'

                if (isDouble) {
                    val start = i + 2
                    val end = builder.indexOf("~~", start)

                    if (end != -1) {
                        val inner = builder.substring(start, end)

                        if (inner.isNotBlank() &&
                            !inner.first().isWhitespace() &&
                            !inner.last().isWhitespace()
                        ) {
                            // Apply strikethrough
                            builder.setSpan(
                                StrikethroughSpan(),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            applyNestedFormatting(context, builder, start, end)

                            // Convert ~~text~~ -> ~text~
                            if (end + 2 <= builder.length) {
                                builder.replace(end, end + 2, "~")
                            }
                            if (i + 2 <= builder.length) {
                                builder.replace(i, i + 2, "~")
                            }

                            // Move index forward instead of restarting
                            i = end
                            continue
                        }
                    }
                } else {
                    val start = i + 1
                    val end = builder.indexOf("~", start)

                    if (end != -1) {
                        val inner = builder.substring(start, end)

                        if (inner.isNotBlank() &&
                            !inner.first().isWhitespace() &&
                            !inner.last().isWhitespace() &&
                            isValidOpening(builder.toString(), i) &&
                            isValidClosing(builder, end)
                        ) {
                            builder.setSpan(
                                StrikethroughSpan(),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            applyNestedFormatting(context, builder, start, end)

                            // recompute safety
                            val safeEnd = builder.indexOf("~", i + 1)

                            if (safeEnd != -1) {
                                builder.delete(safeEnd, safeEnd + 1)
                            }
                            if (i < builder.length) {
                                builder.delete(i, i + 1)
                            }

                            i = (end - 1).coerceAtLeast(0)
                            continue
                        }
                    }
                }
            }

            i++
        }
    }

    private fun convertBulletMarkers(text: SpannableStringBuilder): SpannableStringBuilder {
        val bulletChar = '\u2022'

        var markerPos = 0
        while (markerPos < text.length && text[markerPos] == ' ') {
            markerPos++
        }

        if (markerPos < text.length && markerPos < text.length - 1) {
            val currentChar = text[markerPos]
            val nextChar = text[markerPos + 1]

            if ((currentChar == '-' || currentChar == '*') && nextChar == ' ') {
                text.replace(markerPos, markerPos + 1, bulletChar.toString())
            }
        }

        return text
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

            val replacementText = replacement(match)
            if (replacementText.isEmpty()) continue
            builder.replace(start, end + 1, replacementText)
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
            applySpanSkippingEmoji(this) { MonospaceSpan() }
        }

    @JvmStatic
    fun toUnicodeInlineCode(context: Context, text: String?): CharSequence =
        if (text.isNullOrEmpty()) "" else SpannableStringBuilder(text).apply {
            applySpanSkippingEmoji(this) { MonospaceSpan() }
            applySpanSkippingEmoji(this) { BackgroundColorSpan(ContextCompat.getColor(context, R.color.background_color_span)) }
        }

    @JvmStatic
    fun toUnicodeStrikethrough(text: String?): CharSequence =
        applyStyleSkippingEmoji(text) { StrikethroughSpan() }

    fun toUnicodeBlockQuote(
        context: Context,
        builder: Editable,
        removeMarker: Boolean = false
    ) {

        // Remove old spans
        builder.getSpans(0, builder.length, CustomQuoteSpan::class.java)
            .forEach { builder.removeSpan(it) }

        builder.getSpans(0, builder.length, QuoteIndentSpan::class.java)
            .forEach { builder.removeSpan(it) }

        builder.getSpans(0, builder.length, QuoteMarkerHideSpan::class.java)
            .forEach { builder.removeSpan(it) }

        var i = 0

        while (i < builder.length) {

            val lineStart = i
            var lineEnd = builder.indexOf('\n', i)
            if (lineEnd == -1) lineEnd = builder.length

            if (lineStart >= lineEnd) {
                i = lineEnd + 1
                continue
            }

            if (builder[lineStart] == '>') {

                val valid =
                    lineStart + 2 < lineEnd &&
                            builder[lineStart + 1] == ' ' &&
                            builder[lineStart + 2] != ' '

                if (valid) {

                    var contentStart = lineStart

                    if (removeMarker) {
                        // Remove "> "
                        builder.delete(lineStart, lineStart + 2)
                        lineEnd -= 2
                        contentStart = lineStart

                    } else {
                        // Keep text, hide "> "
                        contentStart = lineStart + 2

                        builder.setSpan(
                            QuoteMarkerHideSpan(),
                            lineStart,
                            contentStart,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // Quote bar
                    builder.setSpan(
                        CustomQuoteSpan(context),
                        lineStart,
                        lineEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // Alignment spacing
                    builder.setSpan(
                        QuoteIndentSpan(20),
                        lineStart,
                        lineEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // Text color
                    builder.setSpan(
                        android.text.style.ForegroundColorSpan(
                            ContextCompat.getColor(context, R.color.quote_gray)
                        ),
                        contentStart,
                        lineEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // Move index safely after mutation
                    i = lineEnd + 1
                    continue
                }
            }

            i = lineEnd + 1
        }
    }

    private fun isValidOpening(text: String, index: Int): Boolean {
        if (index <= 0) return true

        val prev = text[index - 1]

        // normal case: space before *
        if (prev == ' ') return true

        // allow @username*text*
        if (prev.isLetterOrDigit()) {
            var i = index - 1

            // walk backwards to find start of word
            while (i >= 0 && text[i].isLetterOrDigit()) {
                i--
            }

            // if word starts with '@', allow it
            if (i >= 0 && text[i] == '@') {
                return true
            }

            return false
        }

        // allow @ directly
        if (prev == '@') return true

        return true
    }

    private fun isValidClosing(text: CharSequence, index: Int): Boolean {
        val prev = text.getOrNull(index - 1)
        val next = text.getOrNull(index + 1)

        // space before closing (*text *)
        if (prev != null && prev.isWhitespace()) return false

        // letter immediately after (bold*text)
        if (next != null && next.isLetterOrDigit()) return false

        return true
    }

    private fun applyNestedFormatting(
        context: Context,
        builder: SpannableStringBuilder,
        start: Int,
        endInput: Int
    ) {
        var i = start
        var end = endInput

        while (i < end && i < builder.length) {

            // ========= BOLD (*text*) =========
            if (builder[i] == '*' && (i + 1 >= end || builder[i + 1] != '*')) {
                val s = i + 1
                val e = builder.indexOf("*", s)

                if (e != -1 && e < end) {
                    val inner = builder.substring(s, e)

                    if (inner.isNotBlank() &&
                        !inner.first().isWhitespace() &&
                        !inner.last().isWhitespace()
                    ) {
                        builder.setSpan(
                            StyleSpan(Typeface.BOLD),
                            s,
                            e,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        if (e < builder.length) builder.delete(e, e + 1)
                        if (i < builder.length) builder.delete(i, i + 1)

                        end -= 2

                        i = (e - 2).coerceAtLeast(start)
                        continue
                    }
                }
            }

            // ========= ITALIC (_text_) =========
            if (builder[i] == '_' && (i + 1 >= end || builder[i + 1] != '*')) {
                val s = i + 1
                val e = builder.indexOf("_", s)

                if (e != -1 && e < end) {
                    val inner = builder.substring(s, e)

                    if (inner.isNotBlank() &&
                        !inner.first().isWhitespace() &&
                        !inner.last().isWhitespace()
                    ) {
                        builder.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            s,
                            e,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        if (e < builder.length) builder.delete(e, e + 1)
                        if (i < builder.length) builder.delete(i, i + 1)

                        end -= 2

                        i = (e - 2).coerceAtLeast(start)
                        continue
                    }
                }
            }

            // ========= STRIKE (~text~) =========
            if (builder[i] == '~' && (i + 1 >= end || builder[i + 1] != '*')) {
                val s = i + 1
                val e = builder.indexOf("~", s)

                if (e != -1 && e < end) {
                    val inner = builder.substring(s, e)

                    if (inner.isNotBlank() &&
                        !inner.first().isWhitespace() &&
                        !inner.last().isWhitespace()
                    ) {
                        builder.setSpan(
                            StrikethroughSpan(),
                            s,
                            e,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        if (e < builder.length) builder.delete(e, e + 1)
                        if (i < builder.length) builder.delete(i, i + 1)

                        end -= 2

                        i = (e - 2).coerceAtLeast(start)
                        continue
                    }
                }
            }

            // ========= INLINE CODE (`text`) =========
            if (builder[i] == '`' && (i + 1 >= end || builder[i + 1] != '*')) {
                val s = i + 1
                val e = builder.indexOf("`", s)

                if (e != -1 && e < end) {
                    val inner = builder.substring(s, e)

                    if (inner.isNotBlank()) {
                        builder.setSpan(
                            TypefaceSpan("monospace"),
                            s,
                            e,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        builder.setSpan(
                            BackgroundColorSpan(
                                ContextCompat.getColor(context, R.color.background_color_span)
                            ),
                            s,
                            e,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        if (e < builder.length) builder.delete(e, e + 1)
                        if (i < builder.length) builder.delete(i, i + 1)

                        end -= 2

                        i = (e - 2).coerceAtLeast(start)
                        continue
                    }
                }
            }

            i++
        }
    }

    fun applyInlineBlockQuote(
        context: Context,
        builder: Editable,
        attachmentRegex: Regex
    ) {

        var index = 0

        while (index < builder.length) {

            val lineStart = index
            val lineEnd = builder.indexOf('\n', index).let {
                if (it == -1) builder.length else it
            }

            val line = builder.substring(lineStart, lineEnd)

            val attachmentMatch = attachmentRegex.find(line)
            if (attachmentMatch == null) {
                index = lineEnd + 1
                continue
            }

            val attachmentEnd = lineStart + attachmentMatch.range.last + 1

            val quoteIndex = builder.indexOf("> ", attachmentEnd)
            if (quoteIndex == -1 || quoteIndex >= lineEnd) {
                index = lineEnd + 1
                continue
            }

            val quoteStart = quoteIndex

            builder.replace(quoteStart, quoteStart + 2, "| ")

            val pipeStart = quoteStart
            val pipeEnd = quoteStart + 1

            builder.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(context, R.color.quote_gray)
                ),
                pipeStart,
                pipeEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                pipeStart,
                pipeEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            builder.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(context, R.color.quote_gray)
                ),
                pipeStart,
                lineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            index = lineEnd + 1
        }
    }

    private fun handleFullLineFormat(
        builder: SpannableStringBuilder,
        delimiter: Char,
        invalidToken: String,
        validateInner: (String) -> Boolean,
        applySpan: (Int, Int) -> Unit
    ): Boolean {

        val text = builder.toString()
        val length = builder.length
        if (length <= 2) return false

        val innerStart = 1
        val innerEnd = length - 1

        if (
            builder.first() == delimiter &&
            builder.last() == delimiter &&

            !text.startsWith(invalidToken) &&
            !text.endsWith(invalidToken) &&
            !text.contains(invalidToken) &&

            builder.indexOf(delimiter, 1) != builder.lastIndexOf(delimiter)
        ) {

            val inner = builder.substring(innerStart, innerEnd)

            if (validateInner(inner)) {

                applySpan(innerStart, innerEnd)

                builder.delete(innerEnd, innerEnd + 1)
                builder.delete(0, 1)

                return true
            }
        }

        return false
    }
}

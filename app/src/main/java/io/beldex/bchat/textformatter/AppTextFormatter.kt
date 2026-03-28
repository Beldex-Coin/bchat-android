package io.beldex.bchat.textformatter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.graphics.toColorInt

class AppTextFormatter(private val text: String) {
    private val codeBlockPattern = Regex(
        "(?s)" +
                "(```.+?```)$"               // code block
    )
    private val pattern = Regex(
        "(?s)" +
                "\\*([^*]+)\\*|" +           // 1: bold
                "_([^_]+)_|" +               // 2: italic
                "~([^~]+)~|" +               // 3: strike
                "(`[^`]+`)$"                 // 4: inline code
    )

    @SuppressLint("UseKtx")
    private val foregroundColorSpan = "#66FFFFFF".toColorInt()
    private val backgroundColorSpan = "#797984".toColorInt()

    fun appendFormatted(out: SpannableStringBuilder) {

        var last = 0
        if(text.startsWith("```")){
            for (match in codeBlockPattern.findAll(text)) {
                if (match.range.first > last) {
                    out.append(text.substring(last, match.range.first))
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
                        ForegroundColorSpan(foregroundColorSpan),
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
                        ForegroundColorSpan(foregroundColorSpan),
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
            for (match in pattern.findAll(text)) {
                if (match.range.first > last) {
                    out.append(text.substring(last, match.range.first))
                }

                val content = match.value

                when {
                    // -------------------------------------------------
                    // 1: BOLD (*text*)
                    // -------------------------------------------------
                    content.startsWith('*') -> {
                        val innerText = content.substring(1, content.length - 1)
                        val boldText = TextFormatter.toUnicodeBold(innerText)

                        val startPos = out.length

                        out.append("*")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), startPos, startPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        out.append(boldText)
                        val endPos = out.length

                        out.append("*")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), endPos, endPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        out.setSpan(StyleSpan(Typeface.BOLD), startPos + 1, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    // -------------------------------------------------
                    // 2: ITALIC (_text_)
                    // -------------------------------------------------
                    content.startsWith("_") -> {
                        val innerText = content.substring(1, content.length - 1)
                        val italicUnicode = TextFormatter.toUnicodeItalic(innerText)

                        val openStart = out.length
                        out.append("_")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), openStart, openStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        val italicStart = out.length
                        out.append(italicUnicode)
                        val italicEnd = out.length

                        val closeStart = out.length
                        out.append("_")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), closeStart, closeStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        out.setSpan(StyleSpan(Typeface.ITALIC), italicStart, italicEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    // -------------------------------------------------
                    // 3: STRIKETHROUGH (~text~)
                    // -------------------------------------------------
                    content.startsWith("~") -> {
                        val innerText = content.substring(1, content.length - 1)
                        val strikeUnicode = TextFormatter.toUnicodeStrikethrough(innerText)

                        val openStart = out.length
                        out.append("~")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), openStart, openStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        val strikeStart = out.length
                        out.append(strikeUnicode)
                        val strikeEnd = out.length

                        val closeStart = out.length
                        out.append("~")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), closeStart, closeStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        out.setSpan(StrikethroughSpan(), strikeStart, strikeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    // -------------------------------------------------
                    // 4: INLINE CODE (`code`)
                    // -------------------------------------------------
                    content.startsWith("`") -> {
                        val innerText = content.substring(1, content.length - 1)
                        val monoUnicode = TextFormatter.toUnicodeInlineCode(innerText)

                        val openStart = out.length
                        out.append("`")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), openStart, openStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        val monoStart = out.length
                        out.append(monoUnicode)
                        val monoEnd = out.length

                        val closeStart = out.length
                        out.append("`")
                        out.setSpan(ForegroundColorSpan(foregroundColorSpan), closeStart, closeStart + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        out.setSpan(TypefaceSpan("monospace"), monoStart, monoEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        out.setSpan(BackgroundColorSpan(backgroundColorSpan), monoStart, monoEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                last = match.range.last + 1
            }
        }
        if (last < text.length) {
            out.append(text.substring(last))
        }
    }
}
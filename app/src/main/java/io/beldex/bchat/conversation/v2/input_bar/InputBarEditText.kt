package io.beldex.bchat.conversation.v2.input_bar

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import io.beldex.bchat.textformatter.TextFormatter
import io.beldex.bchat.textformatter.TextFormatter.toUnicodeBlockQuote
import kotlin.math.max
import kotlin.math.min

class InputBarEditText : AppCompatEditText {
    var delegate: InputBarEditTextDelegate? = null
    var showMediaControls: Boolean = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var isFormatting = false
    private val bulletChar = '\u2022'
    private var lastBulletTriggerChar: Char = '*'

    private val reformatRunnable = Runnable {
        val editable = text ?: return@Runnable
        if (BaseInputConnection.getComposingSpanStart(editable) != -1) return@Runnable

        val raw = editable.toString()
        val formatted = TextFormatter.formatAppText(raw, context)
        val formattedHasSpans = formatted.getSpans(0, formatted.length, Any::class.java).isNotEmpty()
        val existingHasSpans = editable.getSpans(0, editable.length, Any::class.java).isNotEmpty()

        if (formatted.toString() != raw || formattedHasSpans || existingHasSpans) {
            isFormatting = true
            try {
                val cursorSnapshot = selectionStart.coerceIn(0, raw.length)
                val formattedBeforeCursor = TextFormatter.formatAppText(raw.take(cursorSnapshot), context)
                val newCursor = min(formattedBeforeCursor.length, formatted.length)
                editable.replace(0, editable.length, formatted)
                setSelection(newCursor.coerceIn(0, editable.length))
            } catch (_: Exception) {
                setSelection(editable.length)
            } finally {
                isFormatting = false
            }
        }

        // Quotes
        toUnicodeBlockQuote(text ?: return@Runnable)

        // Delegates
        post {
            delegate?.inputBarEditTextHeightChanged(height)
            delegate?.inputBarEditTextContentChanged(text.toString())
        }
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (isFormatting) return

        if (lengthAfter > 1 && (text.contains("* ") || text.contains("- "))) {
            val editable = this.text ?: return
            val content = editable.toString()
            isFormatting = true
            try {
                var pos = 0
                while (pos < content.length - 1) {
                    val isMarker = content[pos] == '*' || content[pos] == '-'
                    val isSingleSpace = content[pos + 1] == ' '
                    val isDoubleSpace = pos + 2 < content.length && content[pos + 2] == ' '

                    if (isMarker && isSingleSpace && !isDoubleSpace) {
                        val lineStart = content.lastIndexOf('\n', pos)
                            .let { if (it == -1) 0 else it + 1 }

                        val beforeMarker = content.substring(lineStart, pos)

                        if (beforeMarker.isEmpty()) {
                            lastBulletTriggerChar = content[pos]
                            editable.replace(pos, pos + 1, bulletChar.toString())
                            pos++
                        }
                    }
                    pos++
                }
            } finally {
                isFormatting = false
            }
            setSelection(selectionStart.coerceIn(0, editable.length))
            removeCallbacks(reformatRunnable)
            post(reformatRunnable)
            return
        }

        if (lengthAfter > 1) {
            removeCallbacks(reformatRunnable)
            post {
                applyFormattingNow()
                delegate?.inputBarEditTextContentChanged(this.text?.toString().orEmpty())
            }
            return
        }

        val editable = this.text ?: return
        val cursorPos = selectionStart
        val rawText = editable.toString()
        if (lengthAfter == 1 && text.endsWith(" ")) {
            if (cursorPos >= 3) {
                val threeChars = rawText.substring(cursorPos - 3, cursorPos)
                if (threeChars == "$bulletChar  ") {
                    isFormatting = true
                    editable.replace(cursorPos - 3, cursorPos, "$lastBulletTriggerChar  ")
                    setSelection(cursorPos)
                    isFormatting = false
                    return
                }
            }

            if (cursorPos >= 2) {
                val lineStart = rawText.lastIndexOf('\n', max(0, cursorPos - 2))
                    .let { if (it == -1) 0 else it + 1 }
                val twoChars = rawText.substring(cursorPos - 2, cursorPos)
                val beforeMarker = rawText.substring(lineStart, cursorPos - 2)

                val isNextCharSpace = cursorPos < rawText.length && rawText[cursorPos] == ' '
                if ((twoChars == "* " || twoChars == "- ") && beforeMarker.isEmpty() && !isNextCharSpace) {
                    if (BaseInputConnection.getComposingSpanStart(editable) != -1) return

                    lastBulletTriggerChar = twoChars[0]
                    isFormatting = true
                    editable.replace(cursorPos - 2, cursorPos, "$bulletChar ")
                    setSelection(cursorPos)
                    isFormatting = false
                    return
                }
            }
        }

        if (lengthBefore == 1 && lengthAfter == 0 && cursorPos > 0 && editable.getOrNull(cursorPos - 1) == bulletChar) {
            isFormatting = true
            editable.replace(cursorPos - 1, cursorPos, lastBulletTriggerChar.toString())
            setSelection(cursorPos)
            isFormatting = false
            return
        }
        if (lengthAfter > lengthBefore &&
            text.subSequence(start, start + lengthAfter).any { it == '\n' }) {
            isFormatting = true
            try {
                val beforeText = rawText.substring(0, max(0, cursorPos - 1))
                handleAutoList(beforeText)
            } finally {
                isFormatting = false
            }
            return
        }

        if (text.contains('@')) {
            removeCallbacks(reformatRunnable)
            postDelayed(reformatRunnable, 200)
            return
        }

        if (BaseInputConnection.getComposingSpanStart(editable) != -1) {
            removeCallbacks(reformatRunnable)
            post(reformatRunnable)
            return
        }

        removeCallbacks(reformatRunnable)
        post(reformatRunnable)
    }

    fun applyFormattingNow() {
        removeCallbacks(reformatRunnable)
        reformatRunnable.run()
    }

    private fun handleAutoList(beforeText: String) {
        val editable = text ?: return
        var cursor = selectionStart
        if (cursor == 0) return

        val lineStart = beforeText.lastIndexOf('\n') + 1
        var currentLine = beforeText.substring(lineStart)

        if (currentLine.startsWith(" ") || currentLine.startsWith("\t")) {
            return
        }

        currentLine = currentLine.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")

        val numberMatch = Regex("""^(\d+)\.\s(?! )""").find(currentLine)
        val bulletMatch = Regex("""^([\-\u2022\*])\s(?! )""").find(currentLine)

        if (numberMatch != null) {

            val number = numberMatch.groupValues[1].toIntOrNull() ?: return

            if (number !in 1..99) {
                val lineBegin = editable.toString()
                    .lastIndexOf('\n', cursor - 1)
                    .let { if (it == -1) 0 else it + 1 }

                editable.delete(lineBegin, cursor)
                setSelection(lineBegin)
                return
            }

            val contentAfterMarker = currentLine.substring(numberMatch.value.length).trim()

            if (contentAfterMarker.isEmpty()) {

                if (cursor > 0 && editable[cursor - 1] == '\n') {
                    editable.delete(cursor - 1, cursor)
                    cursor--
                }

                val lineBegin = editable.toString()
                    .lastIndexOf('\n', cursor - 1)
                    .let { if (it == -1) 0 else it + 1 }

                editable.delete(lineBegin, cursor)
                setSelection(lineBegin)
                return
            }

            val insertText = "${number + 1}. "
            val nextNumber = number + 1

            if (nextNumber > 99) return

            if (cursor > 0 && editable[cursor - 1] == '\n') {
                editable.delete(cursor - 1, cursor)
                cursor -= 1
            }

            editable.insert(cursor, insertText)
            setSelection(cursor + insertText.length)
        } else if (bulletMatch != null) {
            val contentAfterMarker = currentLine.substring(bulletMatch.value.length).trim()
            if (contentAfterMarker.isEmpty()) {
                if (cursor > 0 && editable[cursor - 1] == '\n') {
                    editable.delete(cursor - 1, cursor)
                    cursor--
                }
                val lineBegin = editable.toString().lastIndexOf('\n', cursor - 1)
                    .let { if (it == -1) 0 else it + 1 }
                editable.delete(lineBegin, cursor)
                setSelection(lineBegin)
                return
            }
            val bulletCharVal = bulletMatch.groupValues[1]
            val insertText = "$bulletCharVal "

            editable.insert(cursor, insertText)
            setSelection(cursor + insertText.length)
        } else {
            if (cursor > 0 && editable[cursor - 1] == '\n') return
            editable.insert(cursor, "\n")
            setSelection(cursor + 1)
        }
    }

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(editorInfo) ?: return null
        EditorInfoCompat.setContentMimeTypes(
            editorInfo,
            if (showMediaControls) arrayOf("image/png", "image/gif", "image/jpg") else null
        )
        val callback =
            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
                val lacksPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                if (lacksPermission) {
                    try { inputContentInfo.requestPermission() }
                    catch (e: Exception) { return@OnCommitContentListener false }
                }
                delegate?.commitInputContent(inputContentInfo.contentUri)
                true
            }
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }
}

interface InputBarEditTextDelegate {

    fun inputBarEditTextContentChanged(text: CharSequence)
    fun inputBarEditTextHeightChanged(newValue: Int)
    fun commitInputContent(contentUri: Uri)
}
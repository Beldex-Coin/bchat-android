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

    // Runs formatting after IME commits text, also clears spans when markers are removed
    private val reformatRunnable = Runnable {
        val editable = text ?: return@Runnable
        // Avoid touching text while IME is composing to prevent duplication
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

                // In-place replace keeps the InputConnection stable (no keyboard reset)
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
        delegate?.inputBarEditTextContentChanged(text.toString())
        post { delegate?.inputBarEditTextHeightChanged(height) }
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (isFormatting) return

        val editable = this.text ?: return
        val cursorPos = selectionStart
        val rawText = editable.toString()

        // revert bullet back to '*' when the trailing space is deleted
        if (lengthBefore == 1 && lengthAfter == 0 && cursorPos > 0 && editable.getOrNull(cursorPos - 1) == bulletChar) {
            isFormatting = true
            editable.replace(cursorPos - 1, cursorPos, "*")
            setSelection(cursorPos)
            isFormatting = false
            return
        }

        if (lengthAfter == 1 && text.endsWith(" ")) {
            if (cursorPos >= 2) {
                val twoChars = rawText.substring(cursorPos - 2, cursorPos)
                if (twoChars == "* " || twoChars == "- ") {
                    isFormatting = true
                    editable.delete(cursorPos - 2, cursorPos)
                    editable.insert(cursorPos - 2, "$bulletChar ")
                    setSelection(cursorPos)
                    isFormatting = false
                    return
                }
            }
        }

        if (lengthAfter > lengthBefore &&
            text.subSequence(start, start + lengthAfter).any { it == '\n' }) {
            isFormatting = true
            try {
                // cursorPos is after the inserted "\n"; use text before it
                val beforeText = rawText.substring(0, max(0, cursorPos - 1))
                handleAutoList(beforeText)
            } finally {
                isFormatting = false
            }
            return
        }

        // If IME is composing, still schedule deferred reformat (prevents eating markers)
        if (BaseInputConnection.getComposingSpanStart(editable) != -1) {
            removeCallbacks(reformatRunnable)
            post(reformatRunnable)
            return
        }

        // Schedule reformat for normal cases
        removeCallbacks(reformatRunnable)
        post(reformatRunnable)
    }

    // -------------------------
    // Auto-numbered and bullet list handling
    // -------------------------
    private fun handleAutoList(beforeText: String) {
        val editable = text ?: return
        var cursor = selectionStart
        if (cursor == 0) return

        val lineStart = beforeText.lastIndexOf('\n') + 1
        var currentLine = beforeText.substring(lineStart)
        currentLine = currentLine.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")

        val numberMatch = Regex("""^\s*(\d+)\.\s+""").find(currentLine)
        val bulletMatch = Regex("""^\s*([\-\u2022])\s+""").find(currentLine)

        if (numberMatch != null) {
            val number = numberMatch.groupValues[1].toInt()
            val nextNumber = number + 1

            if (cursor > 0 && editable[cursor - 1] == '\n') {
                editable.delete(cursor - 1, cursor)
                cursor -= 1
            }

            val insertText = "\n$nextNumber. "
            editable.insert(cursor, insertText)
            setSelection(cursor + insertText.length)

        } else if (bulletMatch != null) {
            if (cursor > 0 && editable[cursor - 1] == '\n') {
                editable.delete(cursor - 1, cursor)
                cursor -= 1
            }

            val bulletChar = bulletMatch.groupValues[1]
            val insertText = "\n$bulletChar "
            editable.insert(cursor, insertText)
            setSelection(cursor + insertText.length)

        } else {
            if (cursor > 0 && editable[cursor - 1] == '\n') return
            editable.insert(cursor, "\n")
            setSelection(cursor + 1)
        }
    }

    /*Hales63*/
    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(editorInfo) ?: return null
        EditorInfoCompat.setContentMimeTypes(
            editorInfo,
            if (showMediaControls) arrayOf("image/png", "image/gif", "image/jpg") else null
        )

        val callback =
            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                val lacksPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                if (lacksPermission) {
                    try {
                        inputContentInfo.requestPermission()
                    } catch (e: Exception) {
                        return@OnCommitContentListener false
                    }
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
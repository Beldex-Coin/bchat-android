package io.beldex.bchat.conversation.v2.input_bar

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
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
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var isFormatting = false
    private var isBlocQuote = false

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)

        if (isFormatting) return

        val editable = this.text ?: return
        val cursorPos = selectionStart
        val rawText = editable.toString()

        // --- Convert "* " or "- " into bullet "• " ---
        if (lengthAfter == 1 && text.endsWith(" ")) {
            if (cursorPos >= 2) {
                val twoChars = rawText.substring(cursorPos - 2, cursorPos)
                if (twoChars == "* " || twoChars == "- ") {
                    isFormatting = true
                    // Remove typed symbols
                    editable.delete(cursorPos - 2, cursorPos)
                    // Insert bullet
                    editable.insert(cursorPos - 2, "• ")
                    // Move cursor after bullet
                    setSelection(cursorPos - 2 + 2)
                    isFormatting = false
                    return
                }
            }
        }

        // --- Auto-list logic on Enter ---
        if (lengthAfter == 1 && text.endsWith("\n")) {
            isFormatting = true
            val beforeText = rawText.substring(0, max(0, cursorPos - 1))
            handleAutoList(beforeText)
            isFormatting = false
            return
        }

        // --- Apply formatting (bold/italic/spans) ---
        val formatted = TextFormatter.formatAppText(rawText)
        if (formatted.toString() != rawText) {
            isFormatting = true
            val oldCursor = selectionStart
            val beforeCursorText = if (oldCursor in 1..rawText.length) rawText.substring(0, oldCursor) else rawText
            val formattedBeforeCursor = TextFormatter.formatAppText(beforeCursorText)
            val newCursor = formattedBeforeCursor.length
            setText(formatted)
            try { setSelection(min(newCursor, formatted.length)) } catch (e: Exception) { setSelection(formatted.length) }

            isFormatting = false
        }

        // -------------------------------------------------
        // 5: QUOTES (> text)
        // -------------------------------------------------
        toUnicodeBlockQuote(editable)

        // --- Notify delegate about text changes ---
        delegate?.inputBarEditTextContentChanged(editable.toString())
        post {
            delegate?.inputBarEditTextHeightChanged(height)
        }
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
        val bulletMatch = Regex("""^\s*([-•])\s+""").find(currentLine)

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
        EditorInfoCompat.setContentMimeTypes(editorInfo,
            if (showMediaControls) arrayOf("image/png", "image/gif", "image/jpg") else null
        )

        val callback =
            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                val lacksPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                // read and display inputContentInfo asynchronously
                if (lacksPermission) {
                    try {
                        inputContentInfo.requestPermission()
                    } catch (e: Exception) {
                        return@OnCommitContentListener false // return false if failed
                    }
                }

                inputContentInfo.contentUri

                // read and display inputContentInfo asynchronously.
                delegate?.commitInputContent(inputContentInfo.contentUri)

                true  // return true if succeeded
            }
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }

}

interface InputBarEditTextDelegate {

    fun inputBarEditTextContentChanged(text: CharSequence)
    fun inputBarEditTextHeightChanged(newValue: Int)
    fun commitInputContent(contentUri: Uri)
}
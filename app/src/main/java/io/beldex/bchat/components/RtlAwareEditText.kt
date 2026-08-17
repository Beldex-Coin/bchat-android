package io.beldex.bchat.components

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.WeakHashMap

open class RtlAwareEditText : AppCompatEditText {

    constructor(context: Context) : super(context) {
        initRtlAware()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initRtlAware()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initRtlAware()
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) applyDirection(this, text)
    }

    private fun initRtlAware() {
        if (isInEditMode) return

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyDirection(this@RtlAwareEditText, s)
            }
        })

        applyDirection(this, text)
    }

    companion object {
        private val ARABIC_PATTERN =
            Regex("""[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]""")

        private val DIRECTION_WATCHERS = WeakHashMap<EditText, TextWatcher>()

        @JvmStatic
        fun applyDirectionHandling(editText: EditText?): TextWatcher? {
            if (editText == null) return null

            DIRECTION_WATCHERS[editText]?.let { existing ->
                applyDirection(editText, editText.text)
                return existing
            }

            val viewRef = WeakReference(editText)
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewRef.get()?.let { view -> applyDirection(view, s) }
                }
            }
            editText.addTextChangedListener(watcher)
            DIRECTION_WATCHERS[editText] = watcher

            applyDirection(editText, editText.text)
            return watcher
        }

        private fun applyDirection(editText: EditText, text: CharSequence?) {
            val direction = getDirection(editText, text)
            val alignment = if (direction == View.TEXT_DIRECTION_INHERIT) {
                View.TEXT_ALIGNMENT_INHERIT
            } else {
                View.TEXT_ALIGNMENT_TEXT_START
            }

            var changed = false
            if (editText.textDirection != direction) {
                editText.textDirection = direction
                changed = true
            }
            if (editText.textAlignment != alignment) {
                editText.textAlignment = alignment
                changed = true
            }

            val scrollbarPos = when (direction) {
                View.TEXT_DIRECTION_RTL -> View.SCROLLBAR_POSITION_LEFT
                else -> View.SCROLLBAR_POSITION_RIGHT
            }
            if (editText.verticalScrollbarPosition != scrollbarPos) {
                editText.verticalScrollbarPosition = scrollbarPos
                changed = true
            }

            if (changed) refreshCursorPosition(editText)
        }

        private fun refreshCursorPosition(editText: EditText) {
            val capturedStart = editText.selectionStart
            val capturedEnd = editText.selectionEnd
            editText.post {
                if (!editText.hasFocus()) return@post
                val current: Editable = editText.text
                // Never touch the selection while the IME is still composing text.
                if (BaseInputConnection.getComposingSpanStart(current) != -1) return@post

                val length = current.length
                val liveStart = editText.selectionStart
                val liveEnd = editText.selectionEnd

                // If something else moved the cursor in the meantime (e.g. text
                // formatting in InputBarEditText), respect that and do nothing.
                if (liveStart != capturedStart || liveEnd != capturedEnd) return@post
                if (liveStart < 0 || liveEnd < 0) return@post

                val start = minOf(liveStart, length)
                val end = minOf(liveEnd, length)
                if (start < 0 || end < 0) return@post
                editText.setSelection(start, end)
            }
        }

        private fun getDirection(editText: EditText, text: CharSequence?): Int {
            if (!text.isNullOrEmpty()) {
                return if (ARABIC_PATTERN.containsMatchIn(text)) {
                    View.TEXT_DIRECTION_RTL
                } else {
                    View.TEXT_DIRECTION_LTR
                }
            }
            return when (isArabicKeyboard(editText)) {
                null -> View.TEXT_DIRECTION_INHERIT
                true -> View.TEXT_DIRECTION_RTL
                else -> View.TEXT_DIRECTION_LTR
            }
        }

        private fun isArabicKeyboard(editText: EditText): Boolean? {
            var locale: String? = null
            try {
                val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                if (imm != null) {
                    val subtype: InputMethodSubtype? = imm.currentInputMethodSubtype
                    if (subtype != null) {
                        locale = subtype.languageTag
                        if (locale.isNullOrEmpty()) locale = subtype.locale
                    }
                }
            } catch (e: RuntimeException) {
                // getCurrentInputMethodSubtype() is unreliable on modern Android;
                // fall through to the locale fallback below.
            }
            if (locale.isNullOrEmpty()) {
                // Fall back to the device locale so empty fields in an Arabic-locale
                // app still default to RTL even when the IME subtype cannot be queried.
                locale = Locale.getDefault().toLanguageTag()
            }
            if (locale.isNullOrEmpty()) return null
            return locale.lowercase(Locale.ROOT).startsWith("ar")
        }
    }
}

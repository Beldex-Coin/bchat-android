package io.beldex.bchat.compose_utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Keeps a small gap between the last typed character and the caret by appending a thin space
 * after the text. The caret maps to the position after that space; the stored value is untouched.
 */
object CursorGapTransformation : VisualTransformation {
    private const val GAP = " "

    override fun filter(text: AnnotatedString): TransformedText {
        val length = text.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = if (offset >= length) length + 1 else offset
            override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(length)
        }
        return TransformedText(AnnotatedString(text.text + GAP, text.spanStyles, text.paragraphStyles), mapping)
    }
}

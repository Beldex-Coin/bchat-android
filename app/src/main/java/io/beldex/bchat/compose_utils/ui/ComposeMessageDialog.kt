package io.beldex.bchat.compose_utils.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogCancelButton
import io.beldex.bchat.compose_utils.DialogConfirmButton
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape

@Composable
fun ComposeMessageDialogContent(
    title: String,
    message: CharSequence,
    positiveText: String,
    negativeText: String?,
    destructive: Boolean,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    iconRes: Int = 0
) {
    val annotated = androidx.compose.runtime.remember(message) { message.toBoldAwareAnnotatedString() }
    if (iconRes != 0 && negativeText != null) {
        io.beldex.bchat.compose_utils.IconConfirmDialog(
            icon = androidx.compose.ui.res.painterResource(id = iconRes),
            title = title,
            message = annotated,
            cancelText = negativeText,
            confirmText = positiveText,
            onCancel = onNegative,
            onConfirm = onPositive
        )
        return
    }
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onNegative
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 18.dp)) {
            Text(
                text = title,
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = annotated,
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (negativeText != null) {
                    DialogCancelButton(
                        text = negativeText,
                        onClick = onNegative,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (destructive) {
                    DialogConfirmButton(
                        text = positiveText,
                        onClick = onPositive,
                        destructive = true,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    PrimaryButton(
                        onClick = onPositive,
                        shape = notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium),
                        containerColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                        contentColor = androidx.compose.ui.graphics.Color(0xFF0B0B0B),
                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF0A370A)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(text = positiveText, fontFamily = OpenSans, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/** Keeps bold runs from HTML-formatted strings (e.g. the call-permission description). */
private fun CharSequence.toBoldAwareAnnotatedString(): androidx.compose.ui.text.AnnotatedString {
    val spanned = this as? android.text.Spanned ?: return androidx.compose.ui.text.AnnotatedString(toString())
    return androidx.compose.ui.text.buildAnnotatedString {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, android.text.style.StyleSpan::class.java).forEach { span ->
            if (span.style == android.graphics.Typeface.BOLD || span.style == android.graphics.Typeface.BOLD_ITALIC) {
                addStyle(
                    androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                    spanned.getSpanStart(span),
                    spanned.getSpanEnd(span)
                )
            }
        }
    }
}

class ComposeMessageDialogFragment : DialogFragment() {

    private var title: String = ""
    private var message: CharSequence = ""
    private var iconRes: Int = 0
    private var positiveText: String = ""
    private var negativeText: String? = null
    private var destructive: Boolean = false
    private var onPositive: (() -> Unit)? = null
    private var onNegative: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        arguments?.let {
            title = it.getString(ARG_TITLE).orEmpty()
            message = it.getCharSequence(ARG_MESSAGE) ?: ""
            iconRes = it.getInt(ARG_ICON, 0)
            positiveText = it.getString(ARG_POSITIVE).orEmpty()
            negativeText = it.getString(ARG_NEGATIVE)
            destructive = it.getBoolean(ARG_DESTRUCTIVE)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme {
                    ComposeMessageDialogContent(
                        title = title,
                        message = message,
                        positiveText = positiveText,
                        negativeText = negativeText,
                        destructive = destructive,
                        iconRes = iconRes,
                        onPositive = {
                            dismiss()
                            onPositive?.invoke()
                        },
                        onNegative = {
                            dismiss()
                            onNegative?.invoke()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE = "positive"
        private const val ARG_NEGATIVE = "negative"
        private const val ARG_DESTRUCTIVE = "destructive"
        private const val ARG_ICON = "icon"
        const val TAG = "ComposeMessageDialog"

        @JvmStatic
        @JvmOverloads
        fun show(
            fragmentManager: FragmentManager,
            title: String,
            message: CharSequence,
            positiveText: String,
            negativeText: String? = null,
            destructive: Boolean = false,
            onPositive: (() -> Unit)? = null,
            onNegative: (() -> Unit)? = null,
            iconRes: Int = 0
        ) {
            ComposeMessageDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putCharSequence(ARG_MESSAGE, message)
                    putInt(ARG_ICON, iconRes)
                    putString(ARG_POSITIVE, positiveText)
                    putString(ARG_NEGATIVE, negativeText)
                    putBoolean(ARG_DESTRUCTIVE, destructive)
                }
                this.onPositive = onPositive
                this.onNegative = onNegative
            }.show(fragmentManager, TAG)
        }
    }
}

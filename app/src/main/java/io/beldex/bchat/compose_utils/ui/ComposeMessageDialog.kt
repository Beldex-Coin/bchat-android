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
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape

@Composable
fun ComposeMessageDialogContent(
    title: String,
    message: String,
    positiveText: String,
    negativeText: String?,
    destructive: Boolean,
    onPositive: () -> Unit,
    onNegative: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onNegative
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.secondaryContentColor,
                    fontFamily = RobotoMono,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (negativeText != null) {
                    PrimaryButton(
                        onClick = onNegative,
                        shape = notchedCornerShape(12.dp),
                        containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground,
                        contentColor = MaterialTheme.appColors.onboardingSecondaryButtonText,
                        border = BorderStroke(1.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = negativeText, fontFamily = OpenSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
                PrimaryButton(
                    onClick = onPositive,
                    shape = notchedCornerShape(12.dp),
                    containerColor = if (destructive) MaterialTheme.appColors.negativeRedButtonBorder else MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                    contentColor = if (destructive) MaterialTheme.appColors.onboardingSecondaryButtonText else MaterialTheme.appColors.onboardingPrimaryButtonText,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = positiveText, fontFamily = OpenSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

class ComposeMessageDialogFragment : DialogFragment() {

    private var title: String = ""
    private var message: String = ""
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
            message = it.getString(ARG_MESSAGE).orEmpty()
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
            onNegative: (() -> Unit)? = null
        ) {
            ComposeMessageDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message.toString())
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

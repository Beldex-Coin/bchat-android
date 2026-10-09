package io.beldex.bchat.onboarding.ui

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors

/**
 * Live "Set your Display Name" onboarding step (Figma `display_name` 7546:1015/1036), hosted by
 * [io.beldex.bchat.onboarding.DisplayNameActivity]. Not to be confused with the unrelated,
 * unreachable `DisplayNameScreen` in this package (part of a dead parallel Compose nav flow).
 */
@Composable
fun DisplayNameStepScreen(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    title: String = stringResource(R.string.display_name),
    headline: String = stringResource(R.string.display_name_screen_title_content),
    errorMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.onboardingBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            OnboardingTopBar(
                title = title,
                onBackClick = onBackClick
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 22.dp)) {
                Text(
                    text = headline,
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )

                val focusRequester = remember { FocusRequester() }

                // A classic EditText (not a Compose TextField) is used deliberately here: Compose's
                // KeyboardOptions has no public API for EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING,
                // which the previous XML implementation set on this exact field to stop keyboard apps
                // (e.g. Gboard) learning/storing the typed display name. That privacy behavior must
                // not regress just because the screen moved to Compose.
                val hintColor = MaterialTheme.appColors.onboardingInputHint.toArgb()
                val textColor = MaterialTheme.appColors.onboardingInputText.toArgb()
                val hintText = stringResource(R.string.enter_name)
                val latestOnDisplayNameChange = rememberUpdatedState(onDisplayNameChange)
                val latestOnContinueClick = rememberUpdatedState(onContinueClick)

                AndroidView(
                    factory = { context ->
                        EditText(context).apply {
                            background = null
                            setSingleLine(true)
                            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                            typeface = ResourcesCompat.getFont(context, R.font.roboto_mono)
                            textSize = 14f
                            setHintTextColor(hintColor)
                            setTextColor(textColor)
                            setPadding(0, 0, 0, 0)
                            setOnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    latestOnContinueClick.value()
                                    true
                                } else false
                            }
                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                override fun afterTextChanged(s: Editable?) {
                                    latestOnDisplayNameChange.value(s?.toString().orEmpty())
                                }
                            })
                        }
                    },
                    update = { editText ->
                        editText.hint = android.text.SpannableString(hintText.uppercase()).apply {
                            setSpan(android.text.style.AbsoluteSizeSpan(12, true), 0, length, android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                        }
                        if (editText.text?.toString() != displayName) {
                            editText.setText(displayName)
                            editText.setSelection(displayName.length)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(MaterialTheme.appColors.onboardingInputBackground)
                        .border(
                            1.dp,
                            if (errorMessage != null) MaterialTheme.appColors.negativeRedButtonBorder
                            else MaterialTheme.appColors.pinBoxInactiveBorder
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .focusRequester(focusRequester)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.appColors.negativeRedButtonBorder,
                        fontFamily = RobotoMono,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.activity_display_name_hint),
                        color = MaterialTheme.appColors.onboardingCaptionColor,
                        fontFamily = RobotoMono,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            OnboardingPrimaryButton(
                text = stringResource(R.string.continue_2),
                enabled = displayName.isNotBlank(),
                onClick = onContinueClick,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun DisplayNameStepScreenPreview() {
    BChatTheme {
        DisplayNameStepScreen(
            displayName = "",
            onDisplayNameChange = {},
            onContinueClick = {},
            onBackClick = {}
        )
    }
}

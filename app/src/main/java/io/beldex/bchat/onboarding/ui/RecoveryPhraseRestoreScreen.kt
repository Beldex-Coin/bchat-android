package io.beldex.bchat.onboarding.ui

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors

/**
 * Live seed-restore onboarding step (Figma `Restore Seed` 7546:4119/4150), hosted by
 * [io.beldex.bchat.onboarding.RecoveryPhraseRestoreActivity].
 */
@Composable
fun RecoveryPhraseRestoreScreen(
    mnemonic: String,
    onMnemonicChange: (String) -> Unit,
    wordCount: Int,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit
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
                title = stringResource(R.string.restore_seed),
                onBackClick = onBackClick
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 22.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.appColors.onboardingInputBackground)
                        .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        val hintColor = MaterialTheme.appColors.onboardingInputHint.toArgb()
                        val textColor = MaterialTheme.appColors.onboardingInputText.toArgb()
                        val hintText = stringResource(R.string.mnemonic_edit_text_hint)
                        val latestOnMnemonicChange = rememberUpdatedState(onMnemonicChange)

                        AndroidView(
                            factory = { context ->
                                EditText(context).apply {
                                    background = null
                                    // Same privacy rationale as the display-name field: Compose's
                                    // KeyboardOptions has no public API for
                                    // IME_FLAG_NO_PERSONALIZED_LEARNING, which the original XML
                                    // screen set on this field.
                                    imeOptions = EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                                    typeface = ResourcesCompat.getFont(context, R.font.roboto_mono)
                                    textSize = 14f
                                    minLines = 3
                                    setHintTextColor(hintColor)
                                    setTextColor(textColor)
                                    setPadding(0, 0, 0, 0)
                                    filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
                                        val result = dest.substring(0, dstart) + source.subSequence(start, end) + dest.substring(dend)
                                        if (result.split(Regex("\\s+")).count { it.isNotEmpty() } > 25) "" else null
                                    })
                                    addTextChangedListener(object : TextWatcher {
                                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                        override fun afterTextChanged(s: Editable?) {
                                            latestOnMnemonicChange.value(s?.toString().orEmpty())
                                        }
                                    })
                                }
                            },
                            update = { editText ->
                                editText.hint = hintText
                                if (editText.text?.toString() != mnemonic) {
                                    editText.setText(mnemonic)
                                    editText.setSelection(editText.text?.length ?: 0)
                                    // A paste longer than 25 words is rejected by the filter; keep state in sync.
                                    val shown = editText.text?.toString().orEmpty()
                                    if (shown != mnemonic) latestOnMnemonicChange.value(shown)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "$wordCount/25",
                            color = MaterialTheme.appColors.onboardingBodyColor,
                            fontFamily = RobotoMono,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_paste),
                        contentDescription = stringResource(R.string.paste_seed),
                        tint = MaterialTheme.appColors.onboardingHeadlineColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(18.dp)
                            .size(16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPasteClick
                            )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClearClick
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clear_seed),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.onboardingHeadlineColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(R.string.clear),
                        color = MaterialTheme.appColors.onboardingHeadlineColor,
                        fontFamily = RobotoMono,
                        fontSize = 14.sp
                    )
                }
            }

            AnimatedVisibility(visible = mnemonic.isBlank()) {
                Text(
                    text = stringResource(R.string.paste_the_seed_to_continue),
                    color = MaterialTheme.appColors.onboardingCaptionColor,
                    fontFamily = RobotoMono,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                )
            }

            OnboardingPrimaryButton(
                text = stringResource(R.string.continue_2),
                enabled = mnemonic.isNotBlank(),
                onClick = onContinueClick,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun RecoveryPhraseRestoreScreenPreview() {
    BChatTheme {
        RecoveryPhraseRestoreScreen(
            mnemonic = "",
            onMnemonicChange = {},
            wordCount = 0,
            onPasteClick = {},
            onClearClick = {},
            onContinueClick = {},
            onBackClick = {}
        )
    }
}

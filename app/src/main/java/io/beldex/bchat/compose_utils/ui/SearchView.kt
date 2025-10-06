package io.beldex.bchat.compose_utils.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.beldex.bchat.compose_utils.appColors

@Composable
fun SearchView(
    hint: String,
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    Card(
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width=1.dp,
                color=MaterialTheme.appColors.textFiledBorderColor,
                shape=RoundedCornerShape(50.dp)
            )
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onQueryChanged,
            placeholder = {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.appColors.inputHintColor
                    )
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            colors = TextFieldDefaults.colors(
                disabledTextColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.appColors.searchBackground,
                unfocusedContainerColor = MaterialTheme.appColors.searchBackground,
                cursorColor = MaterialTheme.appColors.primaryButtonColor,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.appColors.primaryButtonColor,
                    backgroundColor = MaterialTheme.appColors.primaryButtonColor
                )
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    }
}
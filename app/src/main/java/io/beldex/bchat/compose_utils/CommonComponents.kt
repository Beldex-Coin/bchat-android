package io.beldex.bchat.compose_utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.beldex.bchat.util.CustomCheckBox

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(25),
    containerColor: Color = MaterialTheme.appColors.primaryButtonColor,
    contentColor: Color = Color.White,
    disabledContainerColor: Color = MaterialTheme.colorScheme.primary,
    disabledContentColor: Color = MaterialTheme.appColors.disabledPrimaryButtonContentColor,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun BChatOutlinedTextField(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    textColor: Color = MaterialTheme.appColors.textFieldTextColor,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    singleLine: Boolean = true,
    focusedBorderColor: Color = MaterialTheme.appColors.textFieldFocusedColor,
    focusedLabelColor: Color = MaterialTheme.appColors.textFieldFocusedColor,
    unFocusedBorderColor: Color = MaterialTheme.appColors.textFieldUnfocusedColor,
    unFocusedLabelColor: Color = MaterialTheme.appColors.textFieldUnfocusedColor,
    focusedContainerColor: Color = MaterialTheme.appColors.disabledButtonContainerColor,
    unFocusedContainerColor: Color = MaterialTheme.appColors.disabledButtonContainerColor,
    cursorColor: Color = MaterialTheme.appColors.textFieldCursorColor,
    selectionColors : Color = MaterialTheme.appColors.textSelectionColor,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    placeHolder: String = "",
    maxLen: Int = -1,
    textAlign: TextAlign = TextAlign.Start,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            if (maxLen > -1) {
                if (it.length <= maxLen)
                    onValueChange(it)
            } else {
                onValueChange(it)
            }
        },
        placeholder = {
            Text(
                text = placeHolder,
                style = TextStyle(
                    fontFamily = OpenSans,
                    fontStyle = fontStyle,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = MaterialTheme.appColors.editTextPlaceholder
                )
            )
        },
        label = label?.let {
            {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = OpenSans,
                        fontStyle = fontStyle,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = MaterialTheme.appColors.editTextPlaceholder
                    )
                )
            }
        },
        singleLine = singleLine,
        textStyle = TextStyle(
            fontFamily = OpenSans,
            fontStyle = fontStyle,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = textAlign,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unFocusedBorderColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unFocusedLabelColor,
            cursorColor = cursorColor,
            selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
            unfocusedContainerColor = unFocusedContainerColor,
            focusedContainerColor = focusedContainerColor,
        ),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization
        ),
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        readOnly = readOnly,
        shape = shape,
        modifier = Modifier
            .then(modifier)
    )
}

@Composable
fun BChatRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(
            selectedColor = MaterialTheme.appColors.primaryButtonColor,
        ),
        modifier = modifier
    )
}

@Composable
fun BChatCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    CustomCheckBox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        checkBoxSize = 18.dp,
        modifier = modifier
    )
}

@Composable
fun ImageView(
    data: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(data)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
package io.beldex.bchat.compose_utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.getValue
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalTextStyle
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
        interactionSource = interactionSource
    ) {
        // BChatTypography bakes a fixed text color into every style, which would override the
        // button's content color; clear it so labels follow the enabled/disabled content color.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(color = Color.Unspecified)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
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

// Revamp_2026 radio (Figma Clear all data dialog, 7546:5043): 17dp ring, 11dp inner dot when selected.
@Composable
fun BChatRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = if (selected) Color(0xFF078720) else MaterialTheme.appColors.onboardingInputText
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .selectable(selected = selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(17.dp)) {
            drawCircle(color = ringColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
            if (selected) drawCircle(color = ringColor, radius = 5.5.dp.toPx())
        }
    }
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
            .crossfade(600)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

// Revamp_2026 long-press / more-options menu row (Figma 7546:7547).
@Composable
fun ContextMenuItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val color = if (destructive) MaterialTheme.appColors.deleteOptionColor else MaterialTheme.appColors.onboardingInputText
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = color,
            fontFamily = RobotoMono,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// Revamp_2026 toggle (Figma Toggle 7546:13500): 40x22 rectangle, gradient track, 14dp square thumb.
@Composable
fun BChatSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.appColors.backgroundColor.luminance() < 0.5f
    val trackStart = if (isDark) Color(0xFF1A1A1A) else Color(0xFFCFCFCF)
    val trackEnd = if (isDark) Color(0xFF333333) else Color(0xFFE6E6E6)
    val thumbOff = if (isDark) Color(0xFF666666) else Color(0xFF8D8D8D)
    val thumbColor = if (checked) Color(0xFF00BC33) else thumbOff
    val thumbX by androidx.compose.animation.core.animateDpAsState(if (checked) 22.dp else 4.dp, label = "switchThumb")
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(trackStart, trackEnd)))
            .then(
                if (onCheckedChange != null)
                    Modifier.toggleable(value = checked, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onCheckedChange)
                else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX, y = 4.dp)
                .size(14.dp)
                .background(thumbColor)
        )
    }
}

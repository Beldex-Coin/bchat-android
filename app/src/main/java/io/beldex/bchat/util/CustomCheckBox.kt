package io.beldex.bchat.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.beldex.bchat.compose_utils.appColors

@Composable
fun CustomCheckBox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    checkBoxSize: Dp = 18.dp,
    checkBoxBorderWidth: Dp = 1.dp,
    checkBoxBorderColorSelected: Color = MaterialTheme.appColors.userDetailsConfirmBackground,
    checkBoxBorderColorUnSelected: Color = MaterialTheme.appColors.userDetailsConfirmBackground,
    checkBoxCheckedIconColor: Color = Color(0xFF0B0B0B),
) {
    // Revamp_2026 checkbox: a green square outline; filled green with a dark tick when checked.
    // Fully controlled by `checked` (the previous copy kept its own state and could desync in lists).
    Box(
        modifier = modifier
            .size(checkBoxSize)
            .background(if (checked) checkBoxBorderColorSelected else Color.Transparent)
            .border(
                border = BorderStroke(
                    checkBoxBorderWidth,
                    if (checked) checkBoxBorderColorSelected else checkBoxBorderColorUnSelected
                )
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                tint = checkBoxCheckedIconColor,
                contentDescription = "Custom CheckBox",
                modifier = Modifier.size(checkBoxSize - 6.dp)
            )
        }
    }
}

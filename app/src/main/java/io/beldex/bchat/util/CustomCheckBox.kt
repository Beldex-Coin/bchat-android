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
    checkBoxBorderWidth: Dp = 2.dp,
    checkBoxBorderColorSelected: Color = MaterialTheme.appColors.primaryButtonColor,
    checkBoxBorderColorUnSelected: Color = MaterialTheme.appColors.secondaryContentColor,
    checkBoxCheckedIconColor: Color = MaterialTheme.appColors.primaryButtonColor,
) {
// state is used to hold the checkbox click or not by default is false
    val checkBoxState = remember(checked) { mutableStateOf(checked) }
// Ui for checkbox
    Box(
        modifier = modifier
            .border(
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(checkBoxBorderWidth,
                if (checkBoxState.value) checkBoxBorderColorSelected
                else checkBoxBorderColorUnSelected)
            )
            .size(checkBoxSize)
            .background(Color.Transparent)
            .padding(2.dp)
            .clickable {
                onCheckedChange(!checkBoxState.value)
                checkBoxState.value = !checkBoxState.value
            },
        contentAlignment = Alignment.Center
    ) {
        if (checkBoxState.value)
            Icon(
                Icons.Default.Check,
                tint = checkBoxCheckedIconColor,
                contentDescription = "Custom CheckBox")
    }
}
package io.beldex.bchat.conversation.v2.dialogs

import androidx.compose.ui.text.AnnotatedString
import io.beldex.bchat.compose_utils.PopupBody
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun LeaveGroupDialog(title : String,
                     message : String,
                     positiveButtonTitle : String,
                     onLeave : () -> Unit,
                     onCancel : () -> Unit,
                     modifier : Modifier=Modifier,
                     negativeButtonTitle : String=stringResource(id=R.string.cancel)
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onCancel
    ) {
        PopupBody(
            title = title,
            message = AnnotatedString(message),
            cancelText = negativeButtonTitle,
            onCancel = onCancel,
            confirmText = positiveButtonTitle,
            onConfirm = onLeave,
            destructive = true,
            modifier = modifier
        )
    }
}

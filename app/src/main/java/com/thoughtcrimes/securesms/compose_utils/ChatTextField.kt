package com.thoughtcrimes.securesms.compose_utils

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.beldex.bchat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTextField(
    modifier: Modifier = Modifier
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    var message by remember {
        mutableStateOf("")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
        ) {
            BasicTextField(
                value = message,
                onValueChange = {
                    message = it
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.editTextColor
                ),
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = message,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Write here...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Light
                                )
                            )
                        },
                        visualTransformation = VisualTransformation.None,
                        leadingIcon = {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "",
                                modifier = Modifier
                                    .rotate(45f)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_in_chat_bdx),
                                    contentDescription = "",
                                    modifier = Modifier
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "",
                                    modifier = Modifier
                                )
                            }
                        },
                        interactionSource = interactionSource,
                        container = {
                            Box {

                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp
                    )
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.appColors.primaryButtonColor
                )
        ) {
            Icon(
                Icons.Outlined.Send,
                contentDescription = "",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(-45f)
            )
        }
    }
}

@Preview
@Composable
fun ChatTextFieldPreview() {
    BChatTheme {
        ChatTextField(
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChatTextFieldPreviewDark() {
    BChatTheme {
        ChatTextField(
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}
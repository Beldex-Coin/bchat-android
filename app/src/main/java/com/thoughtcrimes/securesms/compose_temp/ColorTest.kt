package com.thoughtcrimes.securesms.compose_temp

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.Primary
import com.thoughtcrimes.securesms.compose_utils.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorTest() {
    BChatTheme {
        Surface() {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.primary,
                floatingActionButton = {
                    FloatingActionButton(onClick = {}) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = ""
                        )
                    }
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(it)
                        .padding(16.dp)
                ) {
                    Text(text = "Hello BChat")
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.primaryButtonColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = "Create Account")
                    }

                    OutlinedButton(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.secondaryButtonColor,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Create Account 2",
                            style = BChatTypography.bodyMedium.copy(
                                color = MaterialTheme.appColors.secondaryContentColor
                            )
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Card",
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }

                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = {
                                Text(text = "Enter BChat Id")
                        },
                        placeholder = {
                            Text(text = "Enter BChat Id")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(text = "Chip 1")
                            }
                        )
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = {
                                Text(text = "Chip 1")
                            }
                        )
                    }

                }
            }
        }
    }
}

@Preview(
    name = "Light Theme",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun ColorTestPreview() {
    ColorTest()
}

@Preview(
    name = "Dark Theme",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Composable
fun ColorTestPreviewDark() {
    ColorTest()
}
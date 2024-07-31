package io.beldex.bchat.onboarding.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.beldex.bchat.compose_utils.BChatOutlinedTextField
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun RestoreFromSeedScreen(
    navigateToPinCode: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        var restoreFromHeight by remember {
            mutableStateOf(false)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = stringResource(id = R.string.display_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            BChatOutlinedTextField(
                value = "",
                onValueChange = {},
                placeHolder = stringResource(id = R.string.enter_name),
                modifier = Modifier
                    .fillMaxWidth()
            )

            if (restoreFromHeight) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.restore_from_height_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                BChatOutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeHolder = stringResource(id = R.string.restore_from_block_height_hint),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.restore_from_date_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                BChatOutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeHolder = stringResource(id = R.string.restore_from_date_hint),
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = ""
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = {
                    restoreFromHeight = !restoreFromHeight
                },
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.appColors.tertiaryButtonColor,
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                Icon(
                    painter = if (!restoreFromHeight)
                        painterResource(id = R.drawable.ic_blockheight)
                    else
                        painterResource(id = R.drawable.ic_calendar),
                    contentDescription = ""
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (restoreFromHeight)
                        stringResource(id = R.string.restore_from_date)
                    else
                        stringResource(id = R.string.restore_from_height),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(
                            vertical = 4.dp
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = ""
                )
            }
        }

        PrimaryButton(
            onClick = navigateToPinCode,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.restore),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White
                ),
                modifier = Modifier
                    .padding(4.dp)
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun RestoreFromSeedScreenPreview() {
    BChatTheme() {
        Scaffold {
            RestoreFromSeedScreen(
                navigateToPinCode = {}
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RestoreFromSeedScreenPreviewLight() {
    BChatTheme() {
        Scaffold {
            RestoreFromSeedScreen(
                navigateToPinCode = {}
            )
        }
    }
}
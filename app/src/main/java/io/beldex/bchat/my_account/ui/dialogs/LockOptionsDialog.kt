package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatRadioButton
import io.beldex.bchat.compose_utils.DialogCancelButton
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.lazyListScrollbar
import io.beldex.bchat.compose_utils.notchedCornerShape

@Composable
fun LockOptionsDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onValueChanged: (String, Int) -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            // Only reserve a gutter when the list really scrolls, so the scrollbar never sits on the cards.
            val scrollable = listState.canScrollForward || listState.canScrollBackward
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .lazyListScrollbar(listState)
                    .padding(end = if (scrollable) 12.dp else 0.dp)
            ) {
                itemsIndexed(options) { index, option ->
                    val selected = option == currentValue
                    val accent = MaterialTheme.appColors.userDetailsConfirmBackground
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                if (selected) MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground
                                else MaterialTheme.appColors.backgroundColor,
                                notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium)
                            )
                            .border(
                                1.dp,
                                if (selected) accent else MaterialTheme.appColors.dividerColor,
                                notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium)
                            )
                            .clickable { onValueChanged(option, index) }
                            .padding(horizontal = 24.dp)
                    ) {
                        BChatRadioButton(selected = selected, onClick = { onValueChanged(option, index) })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option,
                            color = if (selected) accent else MaterialTheme.appColors.onboardingBodyColor,
                            fontFamily = RobotoMono,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            DialogCancelButton(
                text = stringResource(id = R.string.cancel),
                onClick = onDismiss,
                fontFamily = RobotoMono,
                textColor = MaterialTheme.appColors.editTextColor,
                modifier = Modifier.width(162.dp)
            )
        }
    }
}

/**
 * Figma "Mute Notification" popup (7546:8884): title on the left with a close tile, the durations as
 * centred notched rows (selected = bold green text, green hairline), and an Ok button that applies the
 * picked duration. The close tile dismisses without changing anything.
 */
@Composable
fun MuteOptionsDialog(
    title: String,
    options: List<String>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedIndex by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableIntStateOf(currentIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)))
    }
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                io.beldex.bchat.compose_utils.CloseTile(onClick = onDismiss)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val shape = notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium)
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                options.forEachIndexed { index, option ->
                    val selected = index == selectedIndex
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                if (selected) MaterialTheme.appColors.selectedItemBackground
                                else MaterialTheme.appColors.messageRequestItemBackground,
                                shape
                            )
                            .border(
                                if (selected) 0.5.dp else 1.dp,
                                if (selected) MaterialTheme.appColors.homeFabBackground else MaterialTheme.appColors.optionBorder,
                                shape
                            )
                            .clickable { selectedIndex = index }
                    ) {
                        Text(
                            text = option,
                            color = if (selected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.onboardingBodyColor,
                            fontFamily = RobotoMono,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            io.beldex.bchat.compose_utils.DialogPrimaryButton(
                text = stringResource(id = R.string.ok),
                onClick = { onConfirm(selectedIndex) },
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

@Preview
@Composable
fun LockOptionsDialogPreview() {
    LockOptionsDialog(
        title = "",
        options = listOf(),
        currentValue = "",
        onDismiss = {},
        onValueChanged = {_, _ -> }
    )
}

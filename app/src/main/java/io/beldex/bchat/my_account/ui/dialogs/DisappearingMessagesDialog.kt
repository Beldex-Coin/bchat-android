package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.DialogPrimaryButton
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.lazyListScrollbar
import io.beldex.bchat.compose_utils.notchedCornerShape

/**
 * Disappearing Messages popup (Figma 7546:8691 / 7546:7975): title with a close tile, notched
 * option cards (selected one in green), a one-line explanation and a light "Ok" button that
 * confirms the staged choice.
 */
@Composable
fun DisappearingMessagesDialog(
    options: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(initialIndex.coerceIn(0, options.lastIndex)) }
    val accent = MaterialTheme.appColors.userDetailsConfirmBackground

    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.disappearing_messages),
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.appColors.userDetailsCancelBackground)
                        .clickable(onClick = onDismiss)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.cancel),
                        tint = MaterialTheme.appColors.editTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val listState = rememberLazyListState()
            val scrollable = listState.canScrollForward || listState.canScrollBackward
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .lazyListScrollbar(listState)
                    .padding(end = if (scrollable) 12.dp else 0.dp)
            ) {
                itemsIndexed(options) { index, option ->
                    val isSelected = index == selected
                    val shape = notchedCornerShape(BChatDimens.NotchMedium)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(MaterialTheme.appColors.backgroundColor, shape)
                            .border(
                                1.dp,
                                if (isSelected) accent else MaterialTheme.appColors.dividerColor,
                                shape
                            )
                            .clickable { selected = index }
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) accent else MaterialTheme.appColors.onboardingBodyColor,
                            fontFamily = RobotoMono,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (selected == 0) {
                    stringResource(id = R.string.ExpirationDialog_your_messages_will_not_expire) + "."
                } else {
                    stringResource(
                        id = R.string.ExpirationDialog_your_messages_will_disappear_s_after_they_have_been_seen,
                        options[selected]
                    )
                },
                color = MaterialTheme.appColors.onboardingBodyColor,
                fontFamily = RobotoMono,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            DialogPrimaryButton(
                text = stringResource(id = R.string.ok),
                onClick = { onConfirm(selected) },
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

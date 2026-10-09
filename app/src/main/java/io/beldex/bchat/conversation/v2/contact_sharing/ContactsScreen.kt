package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import androidx.compose.foundation.border
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.onboarding.ui.OnboardingTopBar
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.noRippleCallback
import io.beldex.bchat.compose_utils.ui.SearchView
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

@Composable
fun ContactsScreen(
    searchQuery: String,
    contacts: List<ThreadRecord>,
    selectedContacts: List<ThreadRecord>,
    onQueryChanged: (String) -> Unit,
    onSend: (List<ThreadRecord>) -> Unit,
    contactChanged: (ThreadRecord, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT
    Scaffold(
        topBar = {
            OnboardingTopBar(
                title = stringResource(R.string.share_contacts),
                onBackClick = onBack
            )
        },
        bottomBar = {
            if (contacts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.appColors.createButtonBackground)
                        .padding(horizontal = 21.dp, vertical = 15.dp)
                        .imePadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    PrimaryButton(
                        onClick = { onSend(selectedContacts) },
                        enabled = selectedContacts.isNotEmpty(),
                        shape = notchedCornerShape(14.5.dp),
                        containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                        contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
                        disabledContainerColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground,
                        disabledContentColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.conversation_activity__send),
                            fontFamily = RobotoMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.appColors.backgroundColor,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 13.dp)
                .fillMaxSize()
                .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
                .padding(13.dp)
        ) {
            SearchView(
                hint = stringResource(id = R.string.search_contact),
                searchQuery = searchQuery,
                onQueryChanged = onQueryChanged,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "clear",
                            tint = MaterialTheme.appColors.iconTint,
                            modifier = Modifier.clickable {
                                onQueryChanged("")
                            }
                        )
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "search",
                            tint = MaterialTheme.appColors.iconTint
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isDarkTheme) R.drawable.ic_no_contact_found
                                else R.drawable.ic_no_contact_found_white
                            ),
                            contentDescription = stringResource(id = R.string.no_contact_found),
                        )
                        Text(
                            text = stringResource(id = R.string.no_contact_found),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.appColors.editTextPlaceholder
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f, fill = true)
                ) {
                    items(
                        contacts.size,
                        key = { index -> contacts[index].recipient.address }
                    ) { index ->
                        val contact = contacts[index]
                        ContactItem(
                            isSharing = true,
                            contact = contact,
                            isSelected = selectedContacts.any { c ->
                                c.threadId == contact.threadId
                            },
                            contactChanged = { c, isSelected ->
                                c?.let {
                                    contactChanged(c, isSelected)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ContactsScreenPreview() {
    BChatTheme {
        ContactsScreen(
            searchQuery = "",
            contacts = emptyList(),
            selectedContacts = emptyList(),
            onQueryChanged = {},
            contactChanged = {_, _ -> },
            onBack = {},
            onSend = {}
        )
    }
}
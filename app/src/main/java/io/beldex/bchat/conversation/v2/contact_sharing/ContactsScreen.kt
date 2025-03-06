package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.SearchView

@Composable
fun ContactsScreen(
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Row {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = ""
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    stringResource(R.string.send_contact),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SearchView(
                hint = stringResource(id = R.string.search_contact),
                searchQuery = searchQuery,
                onQueryChanged = onQueryChanged,
                trailingIcon = {
                    if(searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "clear",
                            tint = MaterialTheme.appColors.iconTint,
                            modifier = Modifier.clickable(
                                onClick = {
                                    onQueryChanged("")
                                }
                            )
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

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
            ) {
                item {
                    ContactItem(
                        isSelected = true,
                        isSharing = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false,
                        isSharing = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = true
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
                item {
                    ContactItem(
                        isSelected = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = {

                },
                containerColor = MaterialTheme.appColors.primaryButtonColor,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.send),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight(400),
                        fontSize = 16.sp
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
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
            onQueryChanged = {}
        )
    }
}
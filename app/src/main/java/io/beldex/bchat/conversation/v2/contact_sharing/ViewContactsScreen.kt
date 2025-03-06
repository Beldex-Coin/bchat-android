package io.beldex.bchat.conversation.v2.contact_sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme

@Composable
fun ViewContactScreen(
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
                    stringResource(R.string.view_contacts),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(it)
        ) {
            item {
                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }
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
    }
}

@Preview
@Composable
private fun ViewContactsScreenPreview() {
    BChatTheme {
        ViewContactScreen()
    }
}
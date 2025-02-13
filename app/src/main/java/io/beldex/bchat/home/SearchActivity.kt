package io.beldex.bchat.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.ScreenContainer
import io.beldex.bchat.conversation_v2.getUserDisplayName
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.home.search.GlobalSearchViewModel
import io.beldex.bchat.home.search.getSearchName
import io.beldex.bchat.search.SearchActivityResults
import io.beldex.bchat.search.SearchResults
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class SearchActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SEARCH_DATA = "io.beldex.bchat.EXTRA_SEARCH_DATA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val focusManager = LocalFocusManager.current
            BChatTheme(
                darkTheme = isDarkTheme
            ) {
                Surface {
                    Scaffold {
                        ScreenContainer(
                            title = stringResource(id = R.string.SearchToolbar_search),
                            onBackClick = {
                                focusManager.clearFocus()
                                finish() },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                .padding(it)
                        ) {
                            val viewModel: GlobalSearchViewModel = hiltViewModel()
                            val query by viewModel.queryText.collectAsState()
                            val results by viewModel.searchResults.collectAsState()
                            SearchView(
                                searchQuery = query.toString(),
                                results = results,
                                onQueryChanged = viewModel::postQuery,
                                onClick = { result ->
                                    setResult(result = result)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setResult(result: SearchResults) {
        val data = Intent()
        var searchData: SearchActivityResults? = null
        when (result) {
            is SearchResults.Contact -> {
                searchData = SearchActivityResults.Contact(Address.fromSerialized(result.contact.bchatID))
            }
            is SearchResults.GroupConversation -> {
                searchData = SearchActivityResults.GroupConversation(result.groupRecord.encodedId)
            }
            is SearchResults.Header -> {

            }
            is SearchResults.Message -> {
                searchData = SearchActivityResults.Message(
                    threadId = result.messageResult.threadId,
                    timeStamp = result.messageResult.sentTimestampMs,
                    author = result.messageResult.messageRecipient.address
                )
            }
            is SearchResults.SavedMessages -> {
                searchData = SearchActivityResults.SavedMessage(Address.fromSerialized(result.currentUserPublicKey))
            }
        }
        data.putExtra(EXTRA_SEARCH_DATA, searchData)
        setResult(Activity.RESULT_OK, data)
        finish()
    }
}

@Composable
private fun SearchView(
    searchQuery: String,
    results: List<SearchResults>,
    onQueryChanged: (String) -> Unit,
    onClick: (SearchResults) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean
) {
    val lifeCycle = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val onSearchResultClick: (SearchResults) -> Unit = { result ->
        focusManager.clearFocus()
        onClick(result)
    }
    LaunchedEffect(key1 = Unit) {
        lifeCycle.lifecycleScope.launch {
            lifeCycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(200)
                focusRequester.requestFocus()
            }
        }
    }
    Column(
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.search_people_and_groups),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.inputHintColor
                        )
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                colors = TextFieldDefaults.colors(
                    disabledTextColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.appColors.searchBackground,
                    unfocusedContainerColor = MaterialTheme.appColors.searchBackground,
                    cursorColor = MaterialTheme.appColors.primaryButtonColor,
                    selectionColors = TextSelectionColors(
                        handleColor = MaterialTheme.appColors.primaryButtonColor,
                        backgroundColor = MaterialTheme.appColors.primaryButtonColor
                    )
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "search",
                        tint = MaterialTheme.appColors.iconTint
                    )
                },
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
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = results,
                key = {
                    when (it) {
                        is SearchResults.Contact -> {
                            it.contact.bchatID
                        }
                        is SearchResults.GroupConversation -> {
                            it.groupRecord.encodedId
                        }
                        is SearchResults.Header -> {
                            it.title
                        }
                        is SearchResults.Message -> {
                            it.messageResult.sentTimestampMs
                        }
                        is SearchResults.SavedMessages -> {
                            it.currentUserPublicKey
                        }
                    }
                }
            ) {
                when (it) {
                    is SearchResults.Contact -> {
                        ContactView(
                            model = it,
                            onClick = onSearchResultClick
                        )
                    }
                    is SearchResults.GroupConversation -> {
                        GroupConversationView(
                            model = it,
                            onClick = onSearchResultClick
                        )
                    }
                    is SearchResults.Header -> {
                        Text(
                            text = stringResource(id = it.title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    is SearchResults.Message -> {
                        MessageView(
                            model = it,
                            onClick = onSearchResultClick
                        )
                    }
                    is SearchResults.SavedMessages -> {
                        Card(
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.appColors.searchBackground
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                                    .clickable {
                                        onSearchResultClick(it)
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = if(isDarkTheme) R.drawable.ic_note_to_self else R.drawable.ic_note_to_self_light),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(ProfilePictureMode.GroupPicture.size)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = stringResource(id = R.string.note_to_self),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight(600)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactView(
    model: SearchResults.Contact,
    onClick: (SearchResults) -> Unit
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(model)
            }
    ) {
        val recipient = Recipient.from(context, Address.fromSerialized(model.contact.bchatID), false)

        ProfilePictureComponent(
            publicKey = recipient.address.toString(),
            displayName = getUserDisplayName(recipient.address.toString(), context),
            containerSize = ProfilePictureMode.SmallPicture.size,
            pictureMode = ProfilePictureMode.SmallPicture
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = model.contact.getSearchName(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessageView(
    model: SearchResults.Message,
    onClick: (SearchResults) -> Unit
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(model)
            }
    ) {
        val recipient = model.messageResult.conversationRecipient
        val messageDate = DateUtils.getDisplayFormattedTimeSpanString(context, Locale.getDefault(), model.messageResult.sentTimestampMs)
        val textSpannable = SpannableStringBuilder()
        if (model.messageResult.conversationRecipient != model.messageResult.messageRecipient) {
            // group chat, bind
            val text = "${model.messageResult.messageRecipient.getSearchName()}: "
            textSpannable.append(text)
        }
        textSpannable.append(
            model.messageResult.bodySnippet
        )
        val address = recipient.address
        if (recipient.isGroupRecipient) {
            val groupRecipients = remember {
                DatabaseComponent.get(context).groupDatabase()
                    .getGroupMemberAddresses(recipient.address.toGroupString(), true)
                    .sorted()
                    .take(2)
                    .toMutableList()
            }
            GetGroupProfilePicture(
                context = context,
                isOpenGroup = recipient.isOpenGroupRecipient,
                address = address,
                groupRecipients = groupRecipients,
                title = recipient.name,
                recipient = recipient.address.toString()
            )
        } else {
            ProfilePictureComponent(
                publicKey = address.toString(),
                displayName = getUserDisplayName(address.toString(), context),
                containerSize = ProfilePictureMode.SmallPicture.size,
                pictureMode = ProfilePictureMode.SmallPicture
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = model.messageResult.conversationRecipient.toShortString(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = messageDate,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = textSpannable.toString(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GroupConversationView(
    model: SearchResults.GroupConversation,
    onClick: (SearchResults) -> Unit
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(model)
            }
    ) {
        val nameString = model.groupRecord.title
        val groupRecipients = model.groupRecord.members.map { Recipient.from(context, it, false) }
        val membersString = groupRecipients.joinToString {
            val address = it.address.serialize()
            it.name ?: "${address.take(4)}...${address.takeLast(4)}"
        }
        GetGroupProfilePicture(
            context = context,
            isOpenGroup = model.groupRecord.isOpenGroup,
            address = Address.fromSerialized(model.groupRecord.encodedId),
            groupRecipients = groupRecipients.map { it.address },
            title = model.groupRecord.title,
            recipient = model.groupRecord.encodedId
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = nameString,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (model.groupRecord.isClosedGroup) {
                Text(
                    text = membersString,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GetGroupProfilePicture(
    context: Context,
    isOpenGroup: Boolean,
    address: Address,
    modifier: Modifier = Modifier,
    groupRecipients: List<Address> = emptyList(),
    title : String? = "",
    recipient: String? = ""
) {
    if (isOpenGroup) {
        val pictureMode = ProfilePictureMode.SmallPicture
        val threadRecipient = Recipient.from(context, address, false)
        val pk = threadRecipient.address.toString()
        val displayName = getUserDisplayName(pk, context)
        ProfilePictureComponent(
            publicKey = pk,
            displayName = displayName,
            containerSize = pictureMode.size,
            pictureMode = pictureMode
        )
    } else {
        val pictureMode = if (groupRecipients.size >= 2)
            ProfilePictureMode.GroupPicture
        else
            ProfilePictureMode.SmallPicture
       /* val pk = groupRecipients.getOrNull(0)?.serialize() ?: ""*/
        val additionalPk = groupRecipients.getOrNull(1)?.serialize() ?: ""
        val additionalDisplay =
            getUserDisplayName(additionalPk, context)

        ProfilePictureComponent(
            publicKey = recipient ?: "",
            displayName = title ?: "",
            additionalPublicKey = additionalPk,
            additionalDisplayName = additionalDisplay,
            containerSize = pictureMode.size,
            pictureMode = pictureMode
        )
    }
}

@Preview
@Composable
private fun SearchViewPreview() {
    BChatTheme {
        Surface {
            SearchView(
                searchQuery = "",
                results = emptyList(),
                onQueryChanged = {},
                onClick = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                isDarkTheme = false
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchViewPreviewDark() {
    BChatTheme {
        Surface {
            SearchView(
                searchQuery = "",
                results = emptyList(),
                onQueryChanged = {},
                onClick = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                isDarkTheme = true
            )
        }
    }
}
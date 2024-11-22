package io.beldex.bchat.conversation.v2

import io.beldex.bchat.conversation.v2.ConversationViewModel
import com.goterl.lazysodium.utils.KeyPair
import com.beldex.libbchat.utilities.recipients.Recipient
import kotlinx.coroutines.flow.first
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anySet
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import io.beldex.bchat.BaseViewModelTest
import io.beldex.bchat.database.Storage
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.repository.ConversationRepository
import io.beldex.bchat.repository.ResultOf
import org.mockito.Mockito.`when` as whenever

class ConversationViewModelTest: BaseViewModelTest() {

    private val repository = mock(ConversationRepository::class.java)
    private val storage = mock(Storage::class.java)

    private val threadId = 123L
    private val edKeyPair = mock(KeyPair::class.java)
    private lateinit var recipient: Recipient

    private val viewModel: ConversationViewModel by lazy {
        ConversationViewModel(threadId, edKeyPair, repository, storage)
    }

    @Before
    fun setUp() {
        recipient = mock(Recipient::class.java)
        whenever(repository.isBeldexHostedOpenGroup(anyLong())).thenReturn(true)
        whenever(repository.getRecipientForThreadId(anyLong())).thenReturn(recipient)
    }

    @Test
    fun `should emit group type on init`() = runBlockingTest {
        assertTrue(viewModel.uiState.first().isBeldexHostedOpenGroup)
    }

    @Test
    fun `should save draft message`() {
        val draft = "Hi there"

        viewModel.saveDraft(draft)

        verify(repository).saveDraft(threadId, draft)
    }

    @Test
    fun `should retrieve draft message`() {
        val draft = "Hi there"
        whenever(repository.getDraft(anyLong())).thenReturn(draft)

        val result = viewModel.getDraft()

        verify(repository).getDraft(threadId)
        assertThat(result, equalTo(draft))
    }

    @Test
    fun `should invite contacts`() {
        val contacts = listOf<Recipient>()

        viewModel.inviteContacts(contacts)

        verify(repository).inviteContacts(threadId, contacts)
    }

    @Test
    fun `should unblock contact recipient`() {
        whenever(recipient.isContactRecipient).thenReturn(true)

        viewModel.unblock()

        verify(repository).setBlocked(recipient,false)
    }

    @Test
    fun `should delete locally`() {
        val message = mock(MessageRecord::class.java)

        viewModel.deleteLocally(message)

        verify(repository).deleteLocally(recipient, message)
    }

    @Test
    fun `should emit error message on failure to delete a message for everyone`() = runBlockingTest {
        val message = mock(MessageRecord::class.java)
        val error = Throwable()
        whenever(repository.deleteForEveryone(anyLong(), any(), any()))
            .thenReturn(ResultOf.Failure(error))

        viewModel.deleteForEveryone(message)

        assertThat(viewModel.uiState.first().uiMessages.first().message, endsWith("$error"))
    }

    @Test
    fun `should emit error message on failure to delete messages without unsend request`() =
        runBlockingTest {
            val message = mock(MessageRecord::class.java)
            val error = Throwable()
            whenever(repository.deleteMessageWithoutUnsendRequest(anyLong(), anySet()))
                .thenReturn(ResultOf.Failure(error))

            viewModel.deleteMessagesWithoutUnsendRequest(setOf(message))

            assertThat(viewModel.uiState.first().uiMessages.first().message, endsWith("$error"))
        }

    @Test
    fun `should emit error message on ban user failure`() = runBlockingTest {
        val error = Throwable()
        whenever(repository.banUser(anyLong(), any())).thenReturn(ResultOf.Failure(error))

        viewModel.banUser(recipient)

        assertThat(viewModel.uiState.first().uiMessages.first().message, endsWith("$error"))
    }

    @Test
    fun `should emit a message on ban user success`() = runBlockingTest {
        whenever(repository.banUser(anyLong(), any())).thenReturn(ResultOf.Success(Unit))

        viewModel.banUser(recipient)

        assertThat(
            viewModel.uiState.first().uiMessages.first().message,
            equalTo("Successfully banned user")
        )
    }

    @Test
    fun `should emit error message on ban user and delete all failure`() = runBlockingTest {
        val error = Throwable()
        whenever(repository.banAndDeleteAll(anyLong(), any())).thenReturn(ResultOf.Failure(error))

        viewModel.banAndDeleteAll(recipient)

        assertThat(viewModel.uiState.first().uiMessages.first().message, endsWith("$error"))
    }

    @Test
    fun `should emit a message on ban user and delete all success`() = runBlockingTest {
        whenever(repository.banAndDeleteAll(anyLong(), any())).thenReturn(ResultOf.Success(Unit))

        viewModel.banAndDeleteAll(recipient)

        assertThat(
            viewModel.uiState.first().uiMessages.first().message,
            equalTo("Successfully banned user and deleted all their messages")
        )
    }

    @Test
    fun `should accept message request`() = runBlockingTest {
        viewModel.acceptMessageRequest()

        verify(repository).acceptMessageRequest(threadId, recipient)
    }

    @Test
    fun `should decline message request`() {
        viewModel.declineMessageRequest()

        verify(repository).declineMessageRequest(threadId)
    }

    @Test
    fun `should remove shown message`() = runBlockingTest {
        // Given that a message is generated
        whenever(repository.banUser(anyLong(), any())).thenReturn(ResultOf.Success(Unit))
        viewModel.banUser(recipient)
        assertThat(viewModel.uiState.value.uiMessages.size, equalTo(1))
        // When the message is shown
        viewModel.messageShown(viewModel.uiState.first().uiMessages.first().id)
        // Then it should be removed
        assertThat(viewModel.uiState.value.uiMessages.size, equalTo(0))
    }

}
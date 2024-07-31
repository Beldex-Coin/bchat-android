package io.beldex.bchat.messagerequests

import io.beldex.bchat.database.ThreadDatabase
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.repository.ConversationRepository
import io.beldex.bchat.util.BaseViewModelTest
import javax.inject.Inject

class MessageRequestsViewModelTest : BaseViewModelTest() {

    private val repository = mock(ConversationRepository::class.java)
    @Inject
    lateinit var threadDb: ThreadDatabase

    private val viewModel: MessageRequestsViewModel by lazy {
        MessageRequestsViewModel(threadDb,repository)
    }

    @Test
    fun `should delete message request`() = runBlockingTest {
        val thread = mock(ThreadRecord::class.java)

        viewModel.deleteMessageRequest(thread)

        verify(repository).deleteMessageRequest(thread)
    }

    @Test
    fun `should clear all message requests`() = runBlockingTest {
        viewModel.clearAllMessageRequests()

        verify(repository).clearAllMessageRequests()
    }

}
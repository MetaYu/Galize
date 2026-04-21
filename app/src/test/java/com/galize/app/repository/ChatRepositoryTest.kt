package com.galize.app.repository

import app.cash.turbine.test
import com.galize.app.model.db.AppDatabase
import com.galize.app.model.db.ChatLogDao
import com.galize.app.model.db.ChatLogEntity
import com.galize.app.model.db.ConversationDao
import com.galize.app.model.db.ConversationEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ChatRepository using JUnit5 + MockK + Turbine
 */
class ChatRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var conversationDao: ConversationDao
    private lateinit var chatLogDao: ChatLogDao
    private lateinit var repository: ChatRepository

    @BeforeEach
    fun setUp() {
        // Mock dependencies
        database = mockk()
        conversationDao = mockk()
        chatLogDao = mockk()
        
        // Configure mocks
        every { database.conversationDao() } returns conversationDao
        every { database.chatLogDao() } returns chatLogDao
        
        // Create repository with mocked dependencies
        repository = ChatRepository(database)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("getAllConversations Tests")
    inner class GetAllConversationsTests {

        @Test
        fun `should return flow of conversations from dao`() = runBlocking {
            // Given
            val expectedConversations = listOf(
                ConversationEntity(id = 1, appType = "WECHAT"),
                ConversationEntity(id = 2, appType = "QQ")
            )
            every { conversationDao.getAllConversations() } returns flowOf(expectedConversations)

            // When
            repository.getAllConversations().test {
                // Then
                val result = awaitItem()
                assertEquals(expectedConversations, result)
                awaitComplete()
            }

            verify(exactly = 1) { conversationDao.getAllConversations() }
        }

        @Test
        fun `should return empty list when no conversations exist`() = runBlocking {
            // Given
            every { conversationDao.getAllConversations() } returns flowOf(emptyList())

            // When
            repository.getAllConversations().test {
                // Then
                val result = awaitItem()
                assertTrue(result.isEmpty())
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("createConversation Tests")
    inner class CreateConversationTests {

        @Test
        fun `should create conversation and return id`() = runBlocking {
            // Given
            val appType = "WECHAT"
            val expectedId = 42L
            val capturedConversation = slot<ConversationEntity>()
            coEvery { conversationDao.insert(capture(capturedConversation)) } returns expectedId

            // When
            val result = repository.createConversation(appType)

            // Then
            assertEquals(expectedId, result)
            assertEquals(appType, capturedConversation.captured.appType)
            coVerify(exactly = 1) { conversationDao.insert(any()) }
        }

        @Test
        fun `should create conversation with correct app type`() = runBlocking {
            // Given
            coEvery { conversationDao.insert(any()) } returns 1L

            // When
            repository.createConversation("QQ")
            repository.createConversation("DATING_APP")

            // Then
            coVerifyOrder {
                conversationDao.insert(match { it.appType == "QQ" })
                conversationDao.insert(match { it.appType == "DATING_APP" })
            }
        }
    }

    @Nested
    @DisplayName("updateConversation Tests")
    inner class UpdateConversationTests {

        @Test
        fun `should update conversation in dao`() = runBlocking {
            // Given
            val conversation = ConversationEntity(id = 1, appType = "WECHAT", totalAffinity = 75)
            coEvery { conversationDao.update(conversation) } just Runs

            // When
            repository.updateConversation(conversation)

            // Then
            coVerify(exactly = 1) { conversationDao.update(conversation) }
        }
    }

    @Nested
    @DisplayName("getChatLogs Tests")
    inner class GetChatLogsTests {

        @Test
        fun `should return flow of chat logs for conversation`() = runBlocking {
            // Given
            val conversationId = 1L
            val expectedLogs = listOf(
                ChatLogEntity(id = 1, conversationId = conversationId, text = "Hello", isFromMe = true),
                ChatLogEntity(id = 2, conversationId = conversationId, text = "Hi", isFromMe = false)
            )
            every { chatLogDao.getLogsForConversation(conversationId) } returns flowOf(expectedLogs)

            // When
            repository.getChatLogs(conversationId).test {
                // Then
                val result = awaitItem()
                assertEquals(expectedLogs, result)
                assertEquals(2, result.size)
                awaitComplete()
            }

            verify(exactly = 1) { chatLogDao.getLogsForConversation(conversationId) }
        }

        @Test
        fun `should return empty list when no chat logs exist`() = runBlocking {
            // Given
            every { chatLogDao.getLogsForConversation(any()) } returns flowOf(emptyList())

            // When
            repository.getChatLogs(999L).test {
                // Then
                val result = awaitItem()
                assertTrue(result.isEmpty())
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("saveChatLog Tests")
    inner class SaveChatLogTests {

        @Test
        fun `should save single chat log`() = runBlocking {
            // Given
            val log = ChatLogEntity(
                conversationId = 1L,
                text = "Test message",
                isFromMe = true,
                chosenReply = "Reply"
            )
            coEvery { chatLogDao.insert(log) } just Runs

            // When
            repository.saveChatLog(log)

            // Then
            coVerify(exactly = 1) { chatLogDao.insert(log) }
        }
    }

    @Nested
    @DisplayName("saveChatLogs Tests")
    inner class SaveChatLogsTests {

        @Test
        fun `should save multiple chat logs`() = runBlocking {
            // Given
            val logs = listOf(
                ChatLogEntity(conversationId = 1L, text = "Message 1", isFromMe = true),
                ChatLogEntity(conversationId = 1L, text = "Message 2", isFromMe = false),
                ChatLogEntity(conversationId = 1L, text = "Message 3", isFromMe = true)
            )
            coEvery { chatLogDao.insertAll(logs) } just Runs

            // When
            repository.saveChatLogs(logs)

            // Then
            coVerify(exactly = 1) { chatLogDao.insertAll(logs) }
        }

        @Test
        fun `should handle empty list of chat logs`() = runBlocking {
            // Given
            val emptyLogs = emptyList<ChatLogEntity>()
            coEvery { chatLogDao.insertAll(emptyLogs) } just Runs

            // When
            repository.saveChatLogs(emptyLogs)

            // Then
            coVerify(exactly = 1) { chatLogDao.insertAll(emptyLogs) }
        }
    }
}

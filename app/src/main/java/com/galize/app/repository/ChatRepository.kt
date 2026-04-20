package com.galize.app.repository

import com.galize.app.model.db.AppDatabase
import com.galize.app.model.db.ChatLogEntity
import com.galize.app.model.db.ConversationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val database: AppDatabase
) {
    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return database.conversationDao().getAllConversations()
    }

    suspend fun createConversation(appType: String): Long {
        return database.conversationDao().insert(
            ConversationEntity(appType = appType)
        )
    }

    suspend fun updateConversation(conversation: ConversationEntity) {
        database.conversationDao().update(conversation)
    }

    fun getChatLogs(conversationId: Long): Flow<List<ChatLogEntity>> {
        return database.chatLogDao().getLogsForConversation(conversationId)
    }

    suspend fun saveChatLog(log: ChatLogEntity) {
        database.chatLogDao().insert(log)
    }

    suspend fun saveChatLogs(logs: List<ChatLogEntity>) {
        database.chatLogDao().insertAll(logs)
    }
}

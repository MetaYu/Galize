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
    private val logger = com.galize.app.utils.GalizeLogger("ChatRepository")

    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return database.conversationDao().getAllConversations()
    }

    fun getConversationsByContact(contactName: String, packageName: String): Flow<List<ConversationEntity>> {
        return database.conversationDao().getConversationsByContact(contactName, packageName)
    }

    /**
     * 查找已有对话或创建新对话。
     * 用 contactName + packageName 作为唯一标识，相同联系人复用已有对话。
     */
    suspend fun findOrCreateConversation(
        contactName: String,
        packageName: String,
        appType: String
    ): Long {
        val existing = database.conversationDao().findConversation(contactName, packageName)
        if (existing != null) {
            logger.I("Found existing conversation id=${existing.id} for contact='$contactName'")
            return existing.id
        }
        val newId = database.conversationDao().insert(
            ConversationEntity(
                appType = appType,
                packageName = packageName,
                contactName = contactName
            )
        )
        logger.I("Created new conversation id=$newId for contact='$contactName'")
        return newId
    }

    suspend fun createConversation(
        appType: String,
        packageName: String = "",
        contactName: String = "",
        screenshotPath: String = ""
    ): Long {
        return database.conversationDao().insert(
            ConversationEntity(
                appType = appType,
                packageName = packageName,
                contactName = contactName,
                screenshotPath = screenshotPath
            )
        )
    }

    suspend fun updateConversation(conversation: ConversationEntity) {
        database.conversationDao().update(conversation)
    }

    fun getChatLogs(conversationId: Long): Flow<List<ChatLogEntity>> {
        return database.chatLogDao().getLogsForConversation(conversationId)
    }

    /**
     * 获取最近 N 条消息，用于去重对比。
     */
    suspend fun getRecentMessages(conversationId: Long, limit: Int = 30): List<ChatLogEntity> {
        return database.chatLogDao().getRecentLogs(conversationId, limit)
    }

    /**
     * 获取对话总消息数。
     */
    suspend fun getMessageCount(conversationId: Long): Int {
        return database.chatLogDao().getLogCount(conversationId)
    }

    /**
     * 追加新消息到已有对话，自动去重。
     * 通过比较消息文本+发言人来判断是否重复。
     *
     * @return 实际追加的新消息数量
     */
    suspend fun appendNewMessages(
        conversationId: Long,
        newLogs: List<ChatLogEntity>
    ): Int {
        if (newLogs.isEmpty()) return 0

        // 获取最近的已存消息用于去重
        val existingLogs = getRecentMessages(conversationId, 50)
        val existingTexts = existingLogs.map { "${it.senderName}:${it.text}" }.toSet()

        val uniqueNewLogs = newLogs.filter { log ->
            "${log.senderName}:${log.text}" !in existingTexts
        }

        if (uniqueNewLogs.isNotEmpty()) {
            database.chatLogDao().insertAll(uniqueNewLogs)
            logger.I("Appended ${uniqueNewLogs.size} new messages (filtered ${newLogs.size - uniqueNewLogs.size} duplicates)")
        } else {
            logger.D("All ${newLogs.size} messages already exist, skipped")
        }

        return uniqueNewLogs.size
    }

    suspend fun saveChatLog(log: ChatLogEntity) {
        database.chatLogDao().insert(log)
    }

    suspend fun saveChatLogs(logs: List<ChatLogEntity>) {
        database.chatLogDao().insertAll(logs)
    }
}

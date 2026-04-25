package com.galize.app.model.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations", indices = [Index(value = ["contactName", "packageName"], unique = false)])
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val appType: String = "GENERIC",
    val packageName: String = "",  // 应用包名，用于判断平台
    val contactName: String = "",  // 联系人名称/备注
    val screenshotPath: String = "",  // 截图保存路径（可选）
    val totalAffinity: Int = 50,
    val messageCount: Int = 0,
    val summary: String = ""
)

@Entity(tableName = "chat_logs")
data class ChatLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val text: String,
    val isFromMe: Boolean,
    val senderName: String = "",
    val displayTime: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val chosenReply: String? = null,
    val choiceType: String? = null
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY startedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE contactName = :contactName AND packageName = :packageName ORDER BY startedAt DESC")
    fun getConversationsByContact(contactName: String, packageName: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE contactName = :contactName AND packageName = :packageName ORDER BY startedAt DESC LIMIT 1")
    suspend fun findConversation(contactName: String, packageName: String): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)
}

@Dao
interface ChatLogDao {
    @Query("SELECT * FROM chat_logs WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getLogsForConversation(conversationId: Long): Flow<List<ChatLogEntity>>

    @Query("SELECT * FROM chat_logs WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(conversationId: Long, limit: Int): List<ChatLogEntity>

    @Query("SELECT COUNT(*) FROM chat_logs WHERE conversationId = :conversationId")
    suspend fun getLogCount(conversationId: Long): Int

    @Insert
    suspend fun insert(log: ChatLogEntity)

    @Insert
    suspend fun insertAll(logs: List<ChatLogEntity>)
}

@Database(
    entities = [ConversationEntity::class, ChatLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun chatLogDao(): ChatLogDao
}

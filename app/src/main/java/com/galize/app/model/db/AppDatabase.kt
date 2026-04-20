package com.galize.app.model.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val appType: String = "GENERIC",
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
    val timestamp: Long = System.currentTimeMillis(),
    val chosenReply: String? = null,
    val choiceType: String? = null
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY startedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)
}

@Dao
interface ChatLogDao {
    @Query("SELECT * FROM chat_logs WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getLogsForConversation(conversationId: Long): Flow<List<ChatLogEntity>>

    @Insert
    suspend fun insert(log: ChatLogEntity)

    @Insert
    suspend fun insertAll(logs: List<ChatLogEntity>)
}

@Database(
    entities = [ConversationEntity::class, ChatLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun chatLogDao(): ChatLogDao
}

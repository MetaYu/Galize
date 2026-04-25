package com.galize.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galize.app.model.db.ChatLogEntity
import com.galize.app.model.db.ConversationEntity
import com.galize.app.repository.ChatRepository
import com.galize.app.utils.GalizeLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val logger = GalizeLogger("HistoryDetailViewModel")
    
    private val conversationId: Long = try {
        savedStateHandle.get<String>("conversationId")?.toLong() ?: 0L
    } catch (e: Exception) {
        logger.E("Failed to parse conversationId: ${e.message}", e)
        0L
    }
    
    private val _conversation = MutableStateFlow<ConversationEntity?>(null)
    val conversation: StateFlow<ConversationEntity?> = _conversation
    
    private val _chatLogs = MutableStateFlow<List<ChatLogEntity>>(emptyList())
    val chatLogs: StateFlow<List<ChatLogEntity>> = _chatLogs
    
    init {
        if (conversationId > 0) {
            loadConversation(conversationId)
        } else {
            logger.E("Invalid conversation ID")
        }
    }
    
    private fun loadConversation(id: Long) {
        viewModelScope.launch {
            chatRepository.getAllConversations()
                .map { conversations ->
                    conversations.find { it.id == id }
                }
                .collect { conv ->
                    _conversation.value = conv
                }
        }
        
        viewModelScope.launch {
            chatRepository.getChatLogs(id)
                .collect { logs ->
                    _chatLogs.value = logs
                    logger.D("Loaded ${logs.size} chat logs for conversation $id")
                }
        }
    }
}

package com.galize.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.galize.app.repository.ChatRepository
import com.galize.app.utils.GalizeLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val logger = GalizeLogger("HistoryViewModel")

    val conversations: Flow<List<com.galize.app.model.db.ConversationEntity>> = 
        chatRepository.getAllConversations().also { flow ->
            logger.D("Loading conversation history")
        }
}

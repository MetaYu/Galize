package com.galize.app.model

/**
 * Represents a parsed chat message from OCR.
 */
data class ChatMessage(
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a conversation context sent to AI.
 */
data class ConversationContext(
    val messages: List<ChatMessage>,
    val persona: String = "default",     // User's chosen persona
    val appType: AppType = AppType.GENERIC
)

enum class AppType {
    WECHAT,
    QQ,
    DATING_APP,
    GENERIC
}

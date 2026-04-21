package com.galize.app.model

/**
 * Represents a parsed chat message from OCR.
 */
data class ChatMessage(
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val positionX: Int = 0,  // X position on screen for debugging
    val positionY: Int = 0   // Y position on screen for sorting
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

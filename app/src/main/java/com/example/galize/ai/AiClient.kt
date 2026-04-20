package com.example.galize.ai

import com.example.galize.model.ChoiceResult
import com.example.galize.model.ConversationContext

/**
 * Unified AI client interface for generating reply choices.
 */
interface AiClient {
    /**
     * Generate three reply choices based on conversation context.
     * @param context The parsed conversation context
     * @return ChoiceResult with three options + subtext + affinity delta
     */
    suspend fun generateChoices(context: ConversationContext): Result<ChoiceResult>

    /**
     * Check if this client is available (e.g., network for cloud, model loaded for local)
     */
    suspend fun isAvailable(): Boolean
}

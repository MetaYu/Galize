package com.galize.app.ai

import com.galize.app.model.Choice
import com.galize.app.model.ChoiceResult
import com.galize.app.model.ChoiceType
import com.galize.app.model.ConversationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local AI client for offline fallback.
 * Uses simple heuristics when no local model is available.
 * In production, this would integrate Google AI Edge / MediaPipe LLM.
 */
@Singleton
class LocalAiClient @Inject constructor() : AiClient {

    override suspend fun isAvailable(): Boolean = true

    override suspend fun generateChoices(context: ConversationContext): Result<ChoiceResult> {
        val lastMessage = context.messages.lastOrNull { !it.isFromMe }?.text ?: ""

        val result = ChoiceResult(
            pureHeart = Choice(
                text = generatePureHeartFallback(lastMessage),
                description = "Safe, friendly response",
                type = ChoiceType.PURE_HEART
            ),
            chaos = Choice(
                text = generateChaosFallback(lastMessage),
                description = "Chaotic response",
                type = ChoiceType.CHAOS
            ),
            philosopher = Choice(
                text = generatePhilosopherFallback(lastMessage),
                description = "Unexpected response",
                type = ChoiceType.PHILOSOPHER
            ),
            subtext = "[Offline mode - subtext analysis unavailable]",
            affinityDelta = 0
        )

        return Result.success(result)
    }

    private fun generatePureHeartFallback(lastMessage: String): String {
        val responses = listOf(
            "That's really interesting! Tell me more~",
            "I feel the same way!",
            "Haha, you always know how to make me smile",
            "That makes sense, I understand what you mean"
        )
        return responses.random()
    }

    private fun generateChaosFallback(lastMessage: String): String {
        val responses = listOf(
            "...okay, anyway, did you see that cat video?",
            "Bold of you to assume I was listening",
            "I'm going to pretend I didn't read that",
            "ERROR 404: Appropriate response not found"
        )
        return responses.random()
    }

    private fun generatePhilosopherFallback(lastMessage: String): String {
        val responses = listOf(
            "Have you ever thought about what language dolphins think in?",
            "In a parallel universe, this conversation is a bestselling novel",
            "If our chat had a soundtrack, what genre would it be?",
            "That reminds me of Schrödinger's text message - both sent and unsent"
        )
        return responses.random()
    }
}

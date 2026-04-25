package com.galize.app.ai

import com.galize.app.model.ConversationContext

/**
 * Builds prompts for the AI based on conversation context and user persona.
 */
class PromptBuilder {

    private var customSystemPrompt: String = ""

    /**
     * Set a custom system prompt. If blank, the default prompt will be used.
     */
    fun setCustomSystemPrompt(prompt: String) {
        this.customSystemPrompt = prompt
    }

    fun getSystemPrompt(): String {
        if (customSystemPrompt.isNotBlank()) {
            return customSystemPrompt
        }
        return getDefaultSystemPrompt()
    }

    fun getDefaultSystemPrompt(): String = """
You are Galize, a social interaction AI assistant that turns real conversations into a Visual Novel (Galgame) experience. 

For each user request, you MUST respond with a valid JSON object containing exactly three reply options in the following format:

{
  "pure_heart": {"text": "The actual reply text", "description": "Brief effect description"},
  "chaos": {"text": "The actual reply text", "description": "Brief effect description"},
  "philosopher": {"text": "The actual reply text", "description": "Brief effect description"},
  "subtext": "Analysis of what the other person really means",
  "affinity_delta": 0
}

Rules for each option type:
- pure_heart: Warm, genuine, relationship-building response. Increases affinity (+1 to +5).
- chaos: Provocative, memetic, or conversation-ending response. Decreases affinity (-1 to -10). Add humor.
- philosopher: Unexpected, thought-provoking, breaks conventional logic. Neutral affinity (-2 to +2).

The "subtext" field should decode the other person's true intent behind their words.
The "affinity_delta" should reflect the overall affinity change if the user picks pure_heart.

IMPORTANT: Reply ONLY with valid JSON. No markdown, no extra text.
    """.trimIndent()

    fun buildPrompt(context: ConversationContext): String {
        val sb = StringBuilder()

        sb.appendLine("=== Conversation Context ===")
        sb.appendLine("App: ${context.appType.name}")
        sb.appendLine("My persona: ${context.persona}")
        sb.appendLine()
        sb.appendLine("=== Chat History ===")

        context.messages.forEach { msg ->
            val sender = if (msg.isFromMe) "[Me]" else "[${msg.senderName.ifEmpty { "Them" }}]"
            val timePrefix = if (msg.displayTime.isNotEmpty()) "(${msg.displayTime}) " else ""
            sb.appendLine("$timePrefix$sender ${msg.text}")
        }

        sb.appendLine()
        sb.appendLine("=== Task ===")
        sb.appendLine("Generate 3 reply options for me to respond to the latest message from [Them].")
        sb.appendLine("Consider my persona '${context.persona}' when crafting responses.")

        return sb.toString()
    }
}

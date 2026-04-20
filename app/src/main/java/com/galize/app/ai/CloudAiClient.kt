package com.galize.app.ai

import com.galize.app.model.Choice
import com.galize.app.model.ChoiceResult
import com.galize.app.model.ChoiceType
import com.galize.app.model.ConversationContext
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudAiClient @Inject constructor(
    private val promptBuilder: PromptBuilder
) : AiClient {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var apiKey: String = ""
    private var baseUrl: String = "https://api.openai.com/v1"

    fun configure(apiKey: String, baseUrl: String) {
        this.apiKey = apiKey
        this.baseUrl = baseUrl.trimEnd('/')
    }

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }

    override suspend fun generateChoices(context: ConversationContext): Result<ChoiceResult> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = promptBuilder.buildPrompt(context)
                val response = callApi(prompt)
                val parsed = parseResponse(response)
                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun callApi(prompt: String): String {
        val requestBody = ChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                Message(role = "system", content = promptBuilder.getSystemPrompt()),
                Message(role = "user", content = prompt)
            ),
            temperature = 0.8
        )

        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $body")
        }

        val chatResponse = gson.fromJson(body, ChatResponse::class.java)
        return chatResponse.choices.firstOrNull()?.message?.content
            ?: throw Exception("No content in response")
    }

    private fun parseResponse(response: String): ChoiceResult {
        return try {
            val parsed = gson.fromJson(response, AiResponseFormat::class.java)
            ChoiceResult(
                pureHeart = Choice(
                    text = parsed.pureHeart.text,
                    description = parsed.pureHeart.description,
                    type = ChoiceType.PURE_HEART
                ),
                chaos = Choice(
                    text = parsed.chaos.text,
                    description = parsed.chaos.description,
                    type = ChoiceType.CHAOS
                ),
                philosopher = Choice(
                    text = parsed.philosopher.text,
                    description = parsed.philosopher.description,
                    type = ChoiceType.PHILOSOPHER
                ),
                subtext = parsed.subtext,
                affinityDelta = parsed.affinityDelta
            )
        } catch (e: Exception) {
            ChoiceResult(
                pureHeart = Choice(text = response.take(100), description = "", type = ChoiceType.PURE_HEART),
                chaos = Choice(text = "...", description = "Parse error", type = ChoiceType.CHAOS),
                philosopher = Choice(text = "...", description = "Parse error", type = ChoiceType.PHILOSOPHER),
                subtext = "",
                affinityDelta = 0
            )
        }
    }
}

// Request/Response models for OpenAI-compatible API
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.8
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: Message
)

// Expected AI response format
data class AiResponseFormat(
    @SerializedName("pure_heart") val pureHeart: ChoiceOption,
    @SerializedName("chaos") val chaos: ChoiceOption,
    @SerializedName("philosopher") val philosopher: ChoiceOption,
    @SerializedName("subtext") val subtext: String = "",
    @SerializedName("affinity_delta") val affinityDelta: Int = 0
)

data class ChoiceOption(
    val text: String,
    val description: String = ""
)

package com.galize.app.ai

import com.galize.app.model.Choice
import com.galize.app.model.ChoiceResult
import com.galize.app.model.ChoiceType
import com.galize.app.model.ConversationContext
import com.galize.app.utils.GalizeLogger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud AI client that communicates with OpenAI-compatible APIs.
 * Supports any service that provides a compatible chat completions endpoint.
 * 
 * Configuration:
 * - apiKey: Your API key (required)
 * - baseUrl: API endpoint URL (default: https://api.openai.com/v1)
 * 
 * Supported models:
 * - gpt-4o-mini (default, fast and cost-effective)
 * - gpt-4o (higher quality, more expensive)
 * - Any other OpenAI-compatible model
 */
@Singleton
class CloudAiClient @Inject constructor(
    private val promptBuilder: PromptBuilder
) : AiClient {
    private val logger = GalizeLogger("CloudAiClient")
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var apiKey: String = ""
    private var baseUrl: String = "https://api.openai.com/v1"
    private var modelName: String = "gpt-4o-mini"

    /**
     * Configures the AI client with API credentials.
     * 
     * @param apiKey Your API key (required for making requests)
     * @param baseUrl API endpoint URL (default: https://api.openai.com/v1)
     * @param model Model name to use (default: gpt-4o-mini)
     */
    fun configure(apiKey: String, baseUrl: String = "https://api.openai.com/v1", model: String = "gpt-4o-mini") {
        this.apiKey = apiKey
        this.baseUrl = baseUrl.trimEnd('/')
        this.modelName = model
        logger.I("CloudAiClient configured with baseUrl=$baseUrl, model=$model")
    }

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }

    override suspend fun generateChoices(context: ConversationContext): Result<ChoiceResult> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isAvailable()) {
                    return@withContext Result.failure(Exception("API key not configured"))
                }

                logger.D("Generating choices for ${context.messages.size} messages")
                val prompt = promptBuilder.buildPrompt(context)
                
                logger.D("Calling API with model=$modelName")
                val response = try {
                    callApi(prompt)
                } catch (e: SocketTimeoutException) {
                    logger.W("API call timed out, retrying once...")
                    try {
                        callApi(prompt)
                    } catch (e2: SocketTimeoutException) {
                        throw Exception("AI \u54cd\u5e94\u8d85\u65f6\uff0c\u8bf7\u68c0\u67e5\u7f51\u7edc\u540e\u91cd\u8bd5")
                    }
                }
                
                logger.D("Parsing API response")
                val parsed = parseResponse(response)
                
                logger.I("Successfully generated choices: affinity_delta=${parsed.affinityDelta}")
                Result.success(parsed)
            } catch (e: Exception) {
                logger.E("Failed to generate choices: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private fun callApi(prompt: String): String {
        val requestBody = ChatRequest(
            model = modelName,
            messages = listOf(
                Message(role = "system", content = promptBuilder.getSystemPrompt()),
                Message(role = "user", content = prompt)
            ),
            temperature = 0.8,
            maxTokens = 1000
        )

        val json = gson.toJson(requestBody)
        logger.D("Request: $json")

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")

        logger.D("Response (${response.code}): $body")

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $body")
        }

        val chatResponse = gson.fromJson(body, ChatResponse::class.java)
        return chatResponse.choices.firstOrNull()?.message?.content
            ?: throw Exception("No content in response")
    }

    private fun parseResponse(response: String): ChoiceResult {
        return try {
            // Strip markdown code fences if present (e.g. ```json ... ```)
            val cleaned = response.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            logger.D("Cleaned AI response: $cleaned")
            
            val parsed = gson.fromJson(cleaned, AiResponseFormat::class.java)
            
            if (parsed.pureHeart == null || parsed.chaos == null || parsed.philosopher == null) {
                throw Exception("Missing required fields in AI response")
            }
            
            ChoiceResult(
                pureHeart = Choice(
                    text = parsed.pureHeart!!.text,
                    description = parsed.pureHeart!!.description,
                    type = ChoiceType.PURE_HEART
                ),
                chaos = Choice(
                    text = parsed.chaos!!.text,
                    description = parsed.chaos!!.description,
                    type = ChoiceType.CHAOS
                ),
                philosopher = Choice(
                    text = parsed.philosopher!!.text,
                    description = parsed.philosopher!!.description,
                    type = ChoiceType.PHILOSOPHER
                ),
                subtext = parsed.subtext,
                affinityDelta = parsed.affinityDelta
            )
        } catch (e: Exception) {
            logger.E("Failed to parse AI response: ${e.message}\nRaw response: $response", e)
            ChoiceResult(
                pureHeart = Choice(text = response.take(100), description = "Parse error", type = ChoiceType.PURE_HEART),
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
    val temperature: Double = 0.8,
    @SerializedName("max_tokens") val maxTokens: Int = 1000
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
    @SerializedName("pure_heart") val pureHeart: ChoiceOption? = null,
    @SerializedName("chaos") val chaos: ChoiceOption? = null,
    @SerializedName("philosopher") val philosopher: ChoiceOption? = null,
    @SerializedName("subtext") val subtext: String = "",
    @SerializedName("affinity_delta") val affinityDelta: Int = 0
)

data class ChoiceOption(
    val text: String,
    val description: String = ""
)

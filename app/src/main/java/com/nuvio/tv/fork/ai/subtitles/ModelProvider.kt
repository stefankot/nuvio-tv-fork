package com.nuvio.tv.fork.ai.subtitles

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.nuvio.tv.fork.logged

/** What a provider can do. Audio is false until an audio adapter exists (no pretend support). */
data class ProviderCapabilities(
    val text: Boolean = true,
    val audio: Boolean = false,
    val jsonSchema: Boolean = true,
    val contextTokens: Int,
    val maxOutputTokens: Int,
    val reportsUsage: Boolean = true,
)

/**
 * One text request. [context] is the large stable prefix (full original + helpers) and comes first so the
 * provider's implicit prompt cache can reuse it between blocks; [task] is the per-block instruction.
 */
data class ModelRequest(
    val system: String,
    val context: String,
    val task: String,
    val schema: JsonObject,
    val maxOutputTokens: Int,
)

data class ModelUsage(val inputTokens: Long, val outputTokens: Long, val thoughtTokens: Long, val cachedTokens: Long)

data class ModelResult(val text: String, val complete: Boolean, val usage: ModelUsage?)

/** [maybeSent] = the provider may have processed (and billed) the request; false only when nothing left the device. */
class ProviderException(
    message: String,
    val httpCode: Int? = null,
    val retryAfterMs: Long? = null,
    val transient: Boolean = false,
    val maybeSent: Boolean = true,
) : IOException(message)

interface TextModelProvider {
    val providerId: String
    val model: String
    val capabilities: ProviderCapabilities
    suspend fun complete(request: ModelRequest): ModelResult
}

/**
 * Google Gemini through the Interactions API (`generateContent` is documented as legacy since 2026).
 * `store=false`: requests and answers are not kept for later retrieval. The key travels only in a header,
 * never in the URL or logs.
 */
class GeminiProvider(
    private val apiKey: String,
    override val model: String,
    private val client: OkHttpClient = defaultClient,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/interactions",
    private val thinkingLevel: String = "medium",
) : TextModelProvider {
    override val providerId = "gemini"
    override val capabilities = ProviderCapabilities(contextTokens = 1_000_000, maxOutputTokens = 32_000)

    init { require(apiKey.isNotBlank()) { "Missing API key" } }

    fun buildBody(request: ModelRequest): String = buildJsonObject {
        put("model", model)
        put("system_instruction", request.system)
        put("store", false)
        put("input", buildJsonArray {
            add(buildJsonObject { put("type", "text"); put("text", request.context) })
            add(buildJsonObject { put("type", "text"); put("text", request.task) })
        })
        put("generation_config", buildJsonObject {
            put("max_output_tokens", request.maxOutputTokens)
            put("thinking_level", thinkingLevel)
        })
        put("response_format", buildJsonObject {
            put("type", "text")
            put("mime_type", "application/json")
            put("schema", request.schema)
        })
    }.toString()

    override suspend fun complete(request: ModelRequest): ModelResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val http = Request.Builder().url(baseUrl)
            .header("x-goog-api-key", apiKey)
            .post(buildBody(request).toRequestBody(JSON))
            .build()
        val call = client.newCall(http)
        val job = coroutineContext[kotlinx.coroutines.Job]
        val handle = job?.invokeOnCompletion { if (it != null) call.cancel() }
        try {
            call.execute().use { response ->
                val body = response.body.let { b ->
                    val limit = MAX_RESPONSE_BYTES
                    val source = b.source(); source.request(limit + 1)
                    if (source.buffer.size > limit) throw ProviderException("Response too large", response.code, transient = false)
                    source.buffer.readUtf8()
                }.orEmpty()
                if (!response.isSuccessful) {
                    val retryAfter = response.header("Retry-After")?.trim()?.toLongOrNull()?.times(1000)
                    val transient = response.code == 429 || response.code == 408 || response.code >= 500
                    throw ProviderException("HTTP ${response.code}: ${errorMessage(body)}", response.code, retryAfter, transient,
                        // 4xx other than 408/429 is rejected before generation: not billed.
                        maybeSent = transient)
                }
                parseResponse(body)
            }
        } catch (e: ProviderException) {
            throw e
        } catch (e: IOException) {
            // Connection failures after the request started may still have reached the provider.
            throw ProviderException("Network: ${e.javaClass.simpleName}", transient = true, maybeSent = true)
        } finally {
            handle?.dispose()
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val MAX_RESPONSE_BYTES = 4L * 1024 * 1024
        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS).callTimeout(95, TimeUnit.SECONDS).build()
        }
        private val json = Json { ignoreUnknownKeys = true }

        fun errorMessage(body: String): String = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        }.logged("ForkAi", "ModelProvider.kt:145").getOrNull()?.take(200) ?: "no details"

        /** Text of all `model_output` steps; `incomplete` (e.g. max tokens) is reported, never silently accepted. */
        fun parseResponse(body: String): ModelResult {
            val root = runCatching { json.parseToJsonElement(body).jsonObject }.logged("ForkAi", "ModelProvider.kt:151").getOrElse { throw ProviderException("Malformed provider response", transient = true) }
            val status = root["status"]?.jsonPrimitive?.contentOrNull
            if (status == "failed" || status == "cancelled") throw ProviderException("Provider status $status", transient = status == "failed")
            val text = StringBuilder()
            (root["steps"] as? JsonArray)?.forEach { step ->
                val o = step as? JsonObject ?: return@forEach
                if (o["type"]?.jsonPrimitive?.contentOrNull != "model_output") return@forEach
                (o["content"] as? JsonArray)?.forEach { part ->
                    val p = part as? JsonObject ?: return@forEach
                    if (p["type"]?.jsonPrimitive?.contentOrNull in setOf(null, "text")) p["text"]?.jsonPrimitive?.contentOrNull?.let(text::append)
                }
            }
            val usage = (root["usage"] as? JsonObject)?.let { u ->
                fun n(k: String) = (u[k] as? JsonPrimitive)?.longOrNull ?: 0L
                ModelUsage(n("total_input_tokens"), n("total_output_tokens"), n("total_thought_tokens"), n("total_cached_tokens"))
            }
            return ModelResult(text.toString(), complete = status == null || status == "completed", usage = usage)
        }
    }
}

internal fun JsonElement.str(key: String): String? = (this as? JsonObject)?.get(key)?.let { (it as? JsonPrimitive)?.contentOrNull }
internal fun JsonElement.arr(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray
internal fun JsonArray.objects(): List<JsonObject> = mapNotNull { it as? JsonObject }

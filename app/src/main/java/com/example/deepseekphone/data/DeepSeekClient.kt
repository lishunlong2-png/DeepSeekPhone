package com.example.deepseekphone.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * DeepSeek API 客户端。
 *
 * 对接 DeepSeek 开放平台（OpenAI 兼容接口）：
 *   POST https://api.deepseek.com/chat/completions
 *   Authorization: Bearer <API Key>
 *
 * 使用 SSE（Server-Sent Events）流式接收回复，实现打字机效果。
 * 回调全部发生在 OkHttp 的工作线程，UI 层通过 StateFlow 更新即可（线程安全）。
 */
class DeepSeekClient(
    private val apiKeyProvider: () -> String,
) {

    companion object {
        const val BASE_URL = "https://api.deepseek.com"
        val MODELS = listOf(
            "deepseek-chat" to "DeepSeek-V3（日常对话）",
            "deepseek-reasoner" to "DeepSeek-R1（深度推理）",
        )
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        // 流式场景下读取超时放长，防止长回复被误判为超时
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * 发起一次流式对话。
     *
     * @param messages 完整对话历史（含最新一条用户消息）
     * @param model    模型 id，如 deepseek-chat
     * @param onDelta  收到一段增量内容时回调（逐字累加即可）
     * @param onDone   流正常结束（[DONE] 或连接关闭）时回调
     * @param onError  出错时回调，参数为可读的错误信息
     */
    fun streamChat(
        messages: List<ChatMessage>,
        model: String,
        onDelta: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val body = buildJsonObject {
            put("model", model)
            put("stream", true)
            putJsonArray("messages") {
                messages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    }
                }
            }
        }.toString()

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer ${apiKeyProvider()}")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        EventSources.createFactory(okHttp)
            .newEventSource(request, object : EventSourceListener() {

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    // 流结束标记
                    if (data == "[DONE]") {
                        onDone()
                        return
                    }
                    try {
                        val root = json.parseToJsonElement(data).jsonObject
                        val delta = root["choices"]
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                            ?.get("delta")
                            ?.jsonObject
                            ?.get("content")
                            ?.jsonPrimitive
                            ?.contentOrNull
                        if (!delta.isNullOrEmpty()) {
                            onDelta(delta)
                        }
                    } catch (_: Exception) {
                        // 忽略无法解析的 SSE 片段（keep-alive 等）
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    // 服务端主动关闭（可能未收到 [DONE]），视为正常结束
                    onDone()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    val message = when {
                        response != null -> {
                            val detail = response.body?.string().orEmpty().take(200)
                            "HTTP ${response.code}：$detail"
                        }
                        t != null -> t.message ?: "网络错误"
                        else -> "未知错误"
                    }
                    onError(message)
                }
            })
    }
}

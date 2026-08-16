package com.example.deepseekphone.data

import kotlinx.serialization.Serializable

/**
 * 发给 DeepSeek API 的一条消息。
 * role 取值：user / assistant / system
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

package com.example.deepseekphone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.deepseekphone.data.ChatMessage
import com.example.deepseekphone.data.DeepSeekClient
import com.example.deepseekphone.data.KeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI 层消息：content 为空表示「正在等待回复」的占位 */
data class UiMessage(
    val content: String,
    val isUser: Boolean,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val keyStore = KeyStore(application)
    private val client = DeepSeekClient { keyStore.apiKey }

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _hasKey = MutableStateFlow(keyStore.apiKey.isNotBlank())
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    private val _model = MutableStateFlow("deepseek-chat")
    val model: StateFlow<String> = _model.asStateFlow()

    /** 保存 API Key；空串表示不修改（设置对话框里留空 = 保持原 Key） */
    fun saveKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        keyStore.apiKey = trimmed
        _hasKey.value = true
    }

    fun setModel(modelId: String) {
        _model.value = modelId
    }

    /** 发送一条用户消息，并以流式方式累积助手回复 */
    fun send(text: String) {
        val content = text.trim()
        if (content.isEmpty() || _isSending.value || !_hasKey.value) return

        // 1. 追加用户消息 + 空的助手占位
        _messages.value = _messages.value + UiMessage(content, isUser = true)
        _messages.value = _messages.value + UiMessage("", isUser = false)
        _isSending.value = true

        // 2. 组装发送给 API 的历史（过滤空占位）
        val history = _messages.value.mapNotNull { m ->
            when {
                m.isUser -> ChatMessage("user", m.content)
                m.content.isNotEmpty() -> ChatMessage("assistant", m.content)
                else -> null
            }
        }

        // 3. 发起流式请求。onDelta 在主线程外回调，但 StateFlow.value 线程安全
        var finished = false
        client.streamChat(
            messages = history,
            model = _model.value,
            onDelta = { delta ->
                if (!finished) {
                    val list = _messages.value.toMutableList()
                    val last = list.lastOrNull() ?: return@onDelta
                    list[list.size - 1] = last.copy(content = last.content + delta)
                    _messages.value = list
                }
            },
            onDone = {
                if (!finished) {
                    finished = true
                    _isSending.value = false
                }
            },
            onError = { error ->
                if (!finished) {
                    finished = true
                    val list = _messages.value.toMutableList()
                    if (list.isNotEmpty()) {
                        val last = list.last()
                        list[list.size - 1] = last.copy(
                            content = if (last.content.isEmpty()) {
                                "⚠️ 出错：$error"
                            } else {
                                last.content + "\n\n⚠️ 出错：$error"
                            }
                        )
                    }
                    _messages.value = list
                    _isSending.value = false
                }
            },
        )
    }

    /** 清空会话（发送中不允许） */
    fun clear() {
        if (_isSending.value) return
        _messages.value = emptyList()
    }
}

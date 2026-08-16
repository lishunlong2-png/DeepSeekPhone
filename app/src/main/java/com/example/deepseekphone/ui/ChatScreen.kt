package com.example.deepseekphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.deepseekphone.data.DeepSeekClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val hasKey by viewModel.hasKey.collectAsState()
    val model by viewModel.model.collectAsState()

    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // 新消息或流式增量时自动滚到底部
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeepSeek 手机版") },
                actions = {
                    TextButton(onClick = { showSettings = true }) { Text("设置") }
                    if (messages.isNotEmpty() && !isSending) {
                        TextButton(onClick = { viewModel.clear() }) { Text("清空") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!hasKey) {
                KeySetupCard(
                    newKey = newKey,
                    onKeyChange = { newKey = it },
                    onSave = {
                        viewModel.saveKey(newKey)
                        if (viewModel.hasKey.value) newKey = ""
                    },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(messages) { index, msg ->
                        MessageBubble(
                            msg = msg,
                            showTyping = isSending && index == messages.size - 1,
                        )
                    }
                }

                // 输入栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息…（Enter 换行）") },
                        enabled = !isSending,
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.send(input)
                            input = ""
                        },
                        enabled = input.isNotBlank() && !isSending,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (input.isNotBlank() && !isSending) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentModel = model,
            onModelChange = {
                viewModel.setModel(it)
            },
            onSaveKey = {
                viewModel.saveKey(it)
                newKey = ""
                showSettings = false
            },
            onDismiss = { showSettings = false },
        )
    }
}

/** 首次启动 / 未配置 Key 时的引导界面 */
@Composable
private fun KeySetupCard(
    newKey: String,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("👋 欢迎使用", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "这是一个连接 DeepSeek API 的聊天客户端。\n首次使用，请粘贴你的 API Key：",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = newKey,
            onValueChange = onKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSave, enabled = newKey.isNotBlank()) {
            Text("保存并开始")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Key 只保存在本机，不会上传到任何地方。\n申请地址：platform.deepseek.com",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 设置对话框：切换模型 + 更换 Key */
@Composable
private fun SettingsDialog(
    currentModel: String,
    onModelChange: (String) -> Unit,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column {
                Text("模型", style = MaterialTheme.typography.titleSmall)
                DeepSeekClient.MODELS.forEach { (id, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(
                            selected = currentModel == id,
                            onClick = { onModelChange(id) },
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("更换 API Key（留空则不修改）") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveKey(newKey) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 单条消息气泡；showTyping 时显示「思考中…」指示 */
@Composable
private fun MessageBubble(msg: UiMessage, showTyping: Boolean) {
    val isUser = msg.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (msg.content.isNotEmpty()) {
                    Text(msg.content, style = MaterialTheme.typography.bodyLarge)
                } else if (showTyping) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("思考中…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

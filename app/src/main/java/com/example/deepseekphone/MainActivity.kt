package com.example.deepseekphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.deepseekphone.ui.ChatScreen
import com.example.deepseekphone.ui.ChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatScreen(viewModel)
        }
    }
}

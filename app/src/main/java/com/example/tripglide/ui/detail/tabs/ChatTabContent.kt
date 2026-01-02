package com.example.tripglide.ui.detail.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripglide.data.model.ChatType
import com.example.tripglide.ui.chat.ChatScreen
import com.example.tripglide.ui.chat.ChatViewModel
import com.example.tripglide.ui.chat.ChatViewModelFactory

/**
 * Chat Tab Content
 * 
 * Wrapper around existing ChatScreen for embedded display in tabs.
 * The chat already has Dota rank integration and gamified visuals.
 */
@Composable
fun ChatTabContent(
    circleId: String,
    circleName: String,
    circleImageUrl: String?,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(modifier = modifier.fillMaxSize()) {
        // Reuse existing ChatScreen - it already has all the gamified features
        ChatScreen(
            channelId = circleId,
            chatType = ChatType.GROUP,
            channelName = circleName,
            channelImageUrl = circleImageUrl,
            onBackClick = { /* No-op in embedded mode */ },
            onInfoClick = null,
            onProfileClick = onProfileClick,
            viewModel = viewModel(factory = ChatViewModelFactory(context))
        )
    }
}

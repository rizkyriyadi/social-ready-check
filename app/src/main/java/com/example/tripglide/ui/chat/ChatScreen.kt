package com.example.tripglide.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripglide.data.model.*
import com.example.tripglide.ui.chat.components.*
import com.example.tripglide.ui.chat.components.DotaMenuBottomSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Premium Theme Colors
private val DarkBackground = Color(0xFF0F0F0F)
private val SurfaceColor = Color(0xFF1C1C1E)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val AccentBlue = Color(0xFF007AFF)

/**
 * Universal Chat Screen Component
 * 
 * Can be used for:
 * - Group Chat (Circle messages): Pass `type = ChatType.GROUP`
 * - Direct Messages: Pass `type = ChatType.DIRECT`
 * 
 * Features:
 * - Real-time message updates
 * - Smart message grouping/clustering
 * - Media support with full-screen viewer
 * - Read receipts
 * - Typing indicators
 * - Auto-scroll to new messages
 * - Pull-to-load-more (pagination)
 * - Clickable user profiles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channelId: String,
    chatType: ChatType,
    channelName: String = "Chat",
    channelImageUrl: String? = null,
    onBackClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onProfileClick: ((String) -> Unit)? = null,  // userId -> navigate to profile
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current))
) {
    // Initialize chat on composition
    LaunchedEffect(channelId, chatType) {
        viewModel.initializeChat(channelId, chatType)
    }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Message input state
    var messageText by remember { mutableStateOf("") }

    // Full-screen media viewer state
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedVideoUrl by remember { mutableStateOf<String?>(null) }
    
    // Dota menu state (for GROUP chats only)
    var showDotaMenu by remember { mutableStateOf(false) }

    // Show error toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // Auto-scroll to bottom when new messages arrive (if already near bottom)
    LaunchedEffect(uiState.messages.firstOrNull()?.message?.id) {
        if (listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }

    // Mark visible messages as read
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .map { items -> items.map { it.key as? String }.filterNotNull() }
            .distinctUntilChanged()
            .collect { visibleMessageIds ->
                if (visibleMessageIds.isNotEmpty()) {
                    viewModel.markMessagesAsRead(visibleMessageIds)
                }
            }
    }

    // Load more when scrolling near the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 5 && !uiState.isLoadingMore && uiState.hasMoreMessages
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreMessages()
        }
    }

    // Full-screen image viewer
    selectedImageUrl?.let { url ->
        FullScreenMediaViewer(
            imageUrl = url,
            onDismiss = { selectedImageUrl = null },
            onDownload = { /* TODO: Implement download */ },
            onShare = { /* TODO: Implement share */ }
        )
    }

    // Full-screen video player
    selectedVideoUrl?.let { url ->
        FullScreenVideoPlayer(
            videoUrl = url,
            onDismiss = { selectedVideoUrl = null }
        )
    }
    
    // Dota Menu Bottom Sheet (for GROUP chats)
    if (showDotaMenu && chatType == ChatType.GROUP) {
        DotaMenuBottomSheet(
            onDismiss = { showDotaMenu = false },
            onViewHeroStats = { 
                showDotaMenu = false
                Toast.makeText(context, "Hero Stats coming soon!", Toast.LENGTH_SHORT).show()
            },
            onViewRecentMatches = {
                showDotaMenu = false
                Toast.makeText(context, "Recent Matches coming soon!", Toast.LENGTH_SHORT).show()
            },
            onViewLeaderboard = {
                showDotaMenu = false
                Toast.makeText(context, "Leaderboard coming soon!", Toast.LENGTH_SHORT).show()
            },
            onStartPartyFinder = {
                showDotaMenu = false
                Toast.makeText(context, "Party Finder coming soon!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                channelName = channelName,
                channelImageUrl = channelImageUrl,
                onBackClick = onBackClick,
                onInfoClick = onInfoClick,
                typingUsers = uiState.typingUsers,
                showDotaButton = chatType == ChatType.GROUP,
                onDotaClick = { showDotaMenu = true }
            )
        },
        bottomBar = {
            ChatInputBar(
                value = messageText,
                onValueChange = { messageText = it },
                onSendClick = {
                    viewModel.sendTextMessage(messageText)
                    messageText = ""
                },
                onMediaSelected = { uri, mediaType ->
                    viewModel.sendMediaMessage(uri, mediaType)
                },
                uploadState = uiState.uploadState,
                onTypingChanged = { isTyping ->
                    viewModel.setTyping(isTyping)
                },
                enabled = !uiState.isLoading
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                }
                uiState.messages.isEmpty() -> {
                    // Empty state
                    EmptyChatState()
                }
                else -> {
                    // Get user profiles for Dota ranks
                    val userProfiles by viewModel.userProfiles.collectAsState()
                    
                    // Message list
                    MessageList(
                        messages = uiState.messages,
                        listState = listState,
                        isLoadingMore = uiState.isLoadingMore,
                        userProfiles = userProfiles,
                        onImageClick = { url -> selectedImageUrl = url },
                        onVideoClick = { url -> selectedVideoUrl = url },
                        onProfileClick = onProfileClick
                    )

                    // Scroll to bottom FAB
                    ScrollToBottomFab(
                        listState = listState,
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    channelName: String,
    channelImageUrl: String?,
    onBackClick: () -> Unit,
    onInfoClick: (() -> Unit)?,
    typingUsers: List<ParticipantProfile>,
    showDotaButton: Boolean = false,
    onDotaClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Channel avatar
                if (channelImageUrl != null) {
                    AsyncImage(
                        model = channelImageUrl,
                        contentDescription = channelName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Column {
                    Text(
                        text = channelName,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    // Typing indicator
                    AnimatedVisibility(
                        visible = typingUsers.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Text(
                            text = when {
                                typingUsers.size == 1 -> "${typingUsers[0].displayName} is typing..."
                                typingUsers.size == 2 -> "${typingUsers[0].displayName} and ${typingUsers[1].displayName} are typing..."
                                else -> "Several people are typing..."
                            },
                            color = AccentBlue,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            // Dota 2 button (only for group chats)
            if (showDotaButton && onDotaClick != null) {
                IconButton(onClick = onDotaClick) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6046).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "D2",
                            color = Color(0xFFFF6046),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Info",
                        tint = TextPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground
        )
    )
}

@Composable
private fun MessageList(
    messages: List<DisplayMessage>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    userProfiles: Map<String, com.example.tripglide.data.model.User>,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onProfileClick: ((String) -> Unit)? = null
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true, // Newest at bottom
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = messages,
            key = { it.message.id }
        ) { displayMessage ->
            Column {
                // Date divider (above message since reversed)
                if (displayMessage.showDateDivider) {
                    DateDivider(timestamp = displayMessage.message.createdAt)
                }

                // Message bubble with sender profile (includes Dota rank)
                val senderProfile = userProfiles[displayMessage.message.senderId]
                MessageBubble(
                    displayMessage = displayMessage,
                    senderProfile = senderProfile,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onProfileClick = onProfileClick
                )
            }
        }

        // Loading more indicator
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AccentBlue,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💬",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No messages yet",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Send a message to start the conversation!",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ScrollToBottomFab(
    listState: LazyListState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    AnimatedVisibility(
        visible = showFab,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = SurfaceColor,
            contentColor = TextPrimary,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll to bottom",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

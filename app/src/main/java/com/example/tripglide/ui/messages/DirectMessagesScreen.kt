package com.example.tripglide.ui.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.DMChannel
import com.example.tripglide.data.model.UniversalMessageType
import com.example.tripglide.ui.components.IOSCard
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// Theme colors
private val IOSBackground = Color(0xFFF2F2F7)
private val AccentBlue = Color(0xFF007AFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesScreen(
    onMessageClick: (String) -> Unit, // channelId
    onNewMessageClick: () -> Unit,
    viewModel: DirectMessagesViewModel = viewModel(
        factory = DirectMessagesViewModelFactory(LocalContext.current)
    )
) {
    val dmChannels by viewModel.dmChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        viewModel.loadDMChannels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNewMessageClick) {
                        Icon(Icons.Default.Add, contentDescription = "New Message")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IOSBackground)
            )
        },
        containerColor = IOSBackground
    ) { paddingValues ->
        if (isLoading && dmChannels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (dmChannels.isEmpty()) {
            EmptyMessagesState(onNewMessageClick = onNewMessageClick)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(dmChannels, key = { it.id }) { channel ->
                    DMChannelItem(
                        channel = channel,
                        currentUserId = currentUserId,
                        onClick = { onMessageClick(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DMChannelItem(
    channel: DMChannel,
    currentUserId: String,
    onClick: () -> Unit
) {
    val otherParticipant = channel.getOtherParticipant(currentUserId)
    val unreadCount = channel.getUnreadCount(currentUserId)

    IOSCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
            Box {
                Image(
                    painter = rememberAsyncImagePainter(
                        otherParticipant?.photoUrl?.ifEmpty { "https://i.pravatar.cc/300" }
                            ?: "https://i.pravatar.cc/300"
                    ),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                
                // Online indicator
                if (otherParticipant?.online == true) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and last message
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherParticipant?.displayName ?: "Unknown",
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    channel.lastMessage?.timestamp?.let { timestamp ->
                        Text(
                            text = getRelativeTime(timestamp.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (unreadCount > 0) AccentBlue else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getLastMessagePreview(channel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unreadCount > 0) Color.Black else Color.Gray,
                        fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread badge
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = AccentBlue
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMessagesState(onNewMessageClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "💬",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start a conversation with your friends!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNewMessageClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start New Chat")
            }
        }
    }
}

private fun getLastMessagePreview(channel: DMChannel): String {
    val lastMessage = channel.lastMessage ?: return "No messages yet"
    
    return when (lastMessage.type) {
        UniversalMessageType.IMAGE.name -> "📷 Photo"
        UniversalMessageType.VIDEO.name -> "🎬 Video"
        UniversalMessageType.AUDIO.name -> "🎤 Voice message"
        UniversalMessageType.FILE.name -> "📎 File"
        else -> lastMessage.content.ifEmpty { "..." }
    }
}

private fun getRelativeTime(date: Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time

    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "Now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

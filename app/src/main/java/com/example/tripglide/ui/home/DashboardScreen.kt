package com.example.tripglide.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.DotaLinkedAccount
import com.example.tripglide.data.network.DotaWinLossResponse
import com.example.tripglide.ui.components.TopBar
import java.text.SimpleDateFormat
import java.util.*

// Colors
private val DotaRed = Color(0xFFFF6046)
private val CardBackground = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onChatClick: (String, String) -> Unit = { _, _ -> } // channelId, chatType
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context)
    )

    val userState by viewModel.user.collectAsState()
    val activityFeed by viewModel.activityFeed.collectAsState()
    val recentChats by viewModel.recentChats.collectAsState()
    val isFeedLoading by viewModel.isFeedLoading.collectAsState()
    val dotaWinLoss by viewModel.dotaWinLoss.collectAsState()
    val isDotaLoading by viewModel.isDotaStatsLoading.collectAsState()

    // Get greeting based on time
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                userName = userState?.displayName ?: "Traveler",
                avatarUrl = userState?.photoUrl ?: "",
                onAvatarClick = { /* Could switch tab if wired */ },
                onAddClick = null
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER GREETING
            item {
                Text(
                    text = "$greeting, ${userState?.displayName?.split(" ")?.firstOrNull() ?: "Player"} 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // DOTA STATS CARD (if linked)
            userState?.linkedAccounts?.dota2?.let { dotaAccount ->
                item {
                    DotaStatsCard(
                        account = dotaAccount,
                        winLoss = dotaWinLoss,
                        isLoading = isDotaLoading,
                        onRefresh = { viewModel.refreshDotaStats() }
                    )
                }
            }
            
            // RECENT ACTIVITY SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isFeedLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            // Unread Chats Section (shown at top)
            if (recentChats.isNotEmpty()) {
                item {
                    Text(
                        text = "💬 New Messages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(recentChats, key = { "unread_${it.id}" }) { item ->
                    RecentChatCard(
                        item = item,
                        onClick = {
                            viewModel.markChatAsRead(item.channelId, item.chatType)
                            onChatClick(item.channelId, item.chatType)
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Activity Feed Items
            if (activityFeed.isEmpty() && recentChats.isEmpty() && !isFeedLoading) {
                item {
                    EmptyActivityCard()
                }
            } else {
                items(activityFeed, key = { it.id }) { item ->
                    ActivityFeedCard(item = item)
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DotaStatsCard(
    account: DotaLinkedAccount,
    winLoss: DotaWinLossResponse?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DotaRed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "D2",
                            color = DotaRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "My Dota 2 Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            account.personaName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Rank
                StatColumn(
                    label = "Rank",
                    value = account.medalName,
                    color = getMedalColorByTier(account.medalTier)
                )
                
                // Win Rate
                if (winLoss != null) {
                    StatColumn(
                        label = "Win Rate",
                        value = "${String.format("%.1f", winLoss.winRate)}%",
                        color = if (winLoss.winRate >= 50) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                    
                    // W/L
                    StatColumn(
                        label = "W/L",
                        value = "${winLoss.win}/${winLoss.lose}",
                        color = Color.Black
                    )
                    
                    // Total
                    StatColumn(
                        label = "Matches",
                        value = winLoss.totalGames.toString(),
                        color = Color.Black
                    )
                } else {
                    StatColumn(
                        label = "Win Rate",
                        value = "—",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ActivityFeedCard(item: ActivityFeedItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.actionIcon,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Circle Name Badge
                Text(
                    item.circleName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Activity Message
                Text(
                    item.displayMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Relative Time
                item.timestamp?.let { ts ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        getRelativeTime(ts.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Circle image (optional)
            if (!item.circleImageUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(item.circleImageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun EmptyActivityCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "😴",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Quiet day today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    "No squad updates yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

/**
 * Recent Chat Card for unread messages
 */
@Composable
fun RecentChatCard(
    item: ActivityFeedItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) Color(0xFFE3F2FD) else CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!item.isRead) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                if (item.senderPhotoUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(item.senderPhotoUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("💬", fontSize = 20.sp)
                }
                
                // Unread indicator
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.TopEnd)
                            .background(Color(0xFF007AFF), CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.circleName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (item.chatType == "DIRECT") Color(0xFF007AFF) else Color(0xFF4CAF50)
                    )
                    
                    // Chat type badge
                    Text(
                        if (item.chatType == "DIRECT") "DM" else "Group",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    item.displayMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!item.isRead) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                item.timestamp?.let { ts ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        getRelativeTime(ts.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!item.isRead) Color(0xFF007AFF) else Color.Gray
                    )
                }
            }
            
            // Arrow indicator
            Text(
                "›",
                fontSize = 24.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * Get relative time string (e.g., "2h ago", "Yesterday")
 */
private fun getRelativeTime(date: Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time
    
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 2 -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

/**
 * Get color for Dota medal tier
 */
@Composable
private fun getMedalColorByTier(tier: Int): Color {
    return when (tier) {
        1 -> Color(0xFF8B7355) // Herald
        2 -> Color(0xFF8B8B8B) // Guardian
        3 -> Color(0xFF4CAF50) // Crusader
        4 -> Color(0xFF2196F3) // Archon
        5 -> Color(0xFFFFC107) // Legend
        6 -> Color(0xFF9C27B0) // Ancient
        7 -> Color(0xFFE91E63) // Divine
        8 -> Color(0xFFFFD700) // Immortal
        else -> Color.Gray
    }
}

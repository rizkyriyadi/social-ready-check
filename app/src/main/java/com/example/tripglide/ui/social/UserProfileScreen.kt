package com.example.tripglide.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.data.model.User
import com.example.tripglide.ui.components.IOSButton
import com.example.tripglide.ui.components.IOSCard
import com.example.tripglide.ui.theme.White

enum class RelationshipStatus {
    STRANGER,       // No relationship
    PENDING_SENT,   // I sent request
    PENDING_RECEIVED, // They sent request
    FRIENDS         // Already friends
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    user: User?,
    isLoading: Boolean,
    requestStatus: String?,
    isFriend: Boolean = false,
    onBackClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onRemoveFriendClick: () -> Unit = {},
    onClearStatus: () -> Unit,
    onMessageClick: () -> Unit = {}
) {
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }
    
    // Handle requestStatus changes
    LaunchedEffect(requestStatus) {
        if (requestStatus != null) {
            isActionInProgress = false
            kotlinx.coroutines.delay(2000)
            onClearStatus()
        }
    }
    
    // Confirmation dialog for removing friend
    if (showRemoveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${user?.displayName ?: "this user"} from your friends?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirmDialog = false
                        isActionInProgress = true
                        onRemoveFriendClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFFF2F2F7)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (user == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("User not found", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = user.photoUrl.ifEmpty { "https://i.pravatar.cc/150?u=${user.uid}" },
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        if (user.username.isNotEmpty()) {
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Friend badge
                        if (isFriend) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "👥 Friends",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bio
                if (user.bio.isNotEmpty()) {
                    IOSCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = user.bio,
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Stats
                IOSCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(value = "${user.socialStats.followersCount}", label = "Followers")
                        StatItem(value = "Lv.${user.gamingStats.level}", label = "Level")
                        StatItem(value = user.gamingStats.rank, label = "Rank")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Actions based on relationship
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (isFriend) {
                        // Already friends - show Message and Remove options
                        IOSButton(
                            text = "💬 Send Message",
                            onClick = onMessageClick,
                            containerColor = Color(0xFF007AFF),
                            enabled = true
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = { showRemoveConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF3B30)
                            ),
                            enabled = !isActionInProgress
                        ) {
                            Text(if (isActionInProgress) "Removing..." else "Remove Friend")
                        }
                    } else {
                        // Not friends - show Add Friend option
                        val buttonText = when {
                            isActionInProgress -> "Sending..."
                            requestStatus == "Request Sent!" -> "✓ Request Sent"
                            requestStatus?.contains("Error") == true -> "Try Again"
                            else -> "Add Friend"
                        }
                        
                        IOSButton(
                            text = buttonText,
                            onClick = {
                                if (requestStatus != "Request Sent!") {
                                    isActionInProgress = true
                                    onAddFriendClick()
                                }
                            },
                            containerColor = if (requestStatus == "Request Sent!") Color(0xFF34C759) else Color(0xFF007AFF),
                            enabled = requestStatus != "Request Sent!" && !isActionInProgress
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { /* Coming Soon */ },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = false
                    ) {
                        Text("Block (Coming Soon)")
                    }
                    
                    // Status message
                    if (requestStatus != null && requestStatus != "Request Sent!") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = requestStatus,
                            color = if (requestStatus.contains("Error")) Color.Red else Color(0xFF34C759),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

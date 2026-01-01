package com.example.tripglide.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.Friend
import com.example.tripglide.data.model.FriendRequest
import com.example.tripglide.ui.components.IOSCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: SocialViewModel,
    onFriendClick: (String) -> Unit = {},
    onAddFriendClick: () -> Unit
) {
    val friends by viewModel.friends.collectAsState()
    val requests by viewModel.pendingRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("My Friends", "Requests")

    LaunchedEffect(Unit) {
        viewModel.loadFriends()
        viewModel.loadPendingRequests()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Friends", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onAddFriendClick) {
                            Icon(Icons.Default.Add, contentDescription = "Add Friend")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF2F2F7),
                    contentColor = Color.Black,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF007AFF)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title)
                                    if (index == 1 && requests.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge { Text(requests.size.toString()) }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF2F2F7)
    ) { paddingValues ->
        if (isLoading && friends.isEmpty() && requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (selectedTab == 0) {
                    // FRIENDS TAB
                    if (friends.isEmpty()) {
                        item { EmptyStateMessage("👥", "No friends yet", "Add some friends to get started!") }
                    } else {
                        items(friends) { friend ->
                            FriendItem(friend = friend, onClick = { onFriendClick(friend.uid) })
                        }
                    }
                } else {
                    // REQUESTS TAB
                    if (requests.isEmpty()) {
                        item { EmptyStateMessage("👋", "No pending requests", "") }
                    } else {
                        items(requests, key = { it.id }) { request ->
                            MergedFriendRequestItem(
                                request = request,
                                onAccept = { viewModel.acceptRequest(request) },
                                onReject = { viewModel.rejectRequest(request.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun FriendItem(friend: Friend, onClick: () -> Unit) {
    IOSCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = rememberAsyncImagePainter(friend.photoUrl.ifEmpty { "https://i.pravatar.cc/300" }),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.displayName, fontWeight = FontWeight.SemiBold)
                if (friend.username.isNotEmpty()) {
                    Text("@${friend.username}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Text("›", fontSize = 20.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MergedFriendRequestItem(request: FriendRequest, onAccept: () -> Unit, onReject: () -> Unit) {
    var isAccepting by remember { mutableStateOf(false) }
    var isRejecting by remember { mutableStateOf(false) }
    
    IOSCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = request.senderPhotoUrl.ifEmpty { "https://i.pravatar.cc/150?u=${request.senderId}" },
                    contentDescription = "Avatar",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.senderName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Wants to be your friend", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { isRejecting = true; onReject() },
                    modifier = Modifier.weight(1f).height(44.dp), 
                    enabled = !isAccepting && !isRejecting,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                ) { Text(if (isRejecting) "..." else "Decline") }
                
                Button(
                    onClick = { isAccepting = true; onAccept() },
                    modifier = Modifier.weight(1f).height(44.dp),
                    enabled = !isAccepting && !isRejecting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) { Text(if (isAccepting) "..." else "Accept") }
            }
        }
    }
}

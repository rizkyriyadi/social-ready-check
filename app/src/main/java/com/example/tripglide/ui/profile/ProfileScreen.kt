package com.example.tripglide.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.User
import com.example.tripglide.ui.components.IOSButton
import com.example.tripglide.ui.components.IOSCard
import com.example.tripglide.ui.components.IOSSectionTitle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onAddFriendClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onFriendRequestsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(context)
    )
    val user by viewModel.user.collectAsState()
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateProfileImage(uri)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Open QR Code */ }) {
                        Icon(Icons.Default.Share, contentDescription = "My QR")
                    }
                    IconButton(onClick = { 
                        viewModel.signOut()
                        onLogout() 
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7) // iOS Grouped BG matches container
                )
            )
        },
        containerColor = Color(0xFFF2F2F7) // iOS Grouped Background color
    ) { paddingValues ->
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Profile Header with iOS Card style
                ProfileHeaderIOS(
                    user = user!!, 
                    onEditClick = onEditProfileClick,
                    onPhotoClick = { 
                         photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                
                // Stats Card
                IOSSectionTitle("Statistics")
                GamingStatsIOS(user!!)
                
                // Menu/Tabs Card
                IOSSectionTitle("Social")
                SocialMenuIOS(
                    onAddFriendClick = onAddFriendClick,
                    onFriendsClick = onFriendsClick,
                    onFriendRequestsClick = onFriendRequestsClick
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileHeaderIOS(user: User, onEditClick: () -> Unit, onPhotoClick: () -> Unit) {
    IOSCard(modifier = Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar with Edit Overlay
            Box {
                Image(
                    painter = rememberAsyncImagePainter(user.photoUrl.ifEmpty { "https://i.pravatar.cc/300" }),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                        .clickable { onPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (user.username.isNotEmpty()) user.username else "@username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF007AFF))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (user.bio.isNotEmpty()) {
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Follower Stats inside the main card
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItemIOS(value = user.socialStats.followersCount.toString(), label = "Followers")
            StatItemIOS(value = user.socialStats.followingCount.toString(), label = "Following")
            StatItemIOS(value = user.socialStats.reputationScore.toString(), label = "Reputation")
        }
    }
}

@Composable
fun StatItemIOS(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun GamingStatsIOS(user: User) {
    IOSCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Clean "div over div" look without colored badges
            GameStatCell("Level", user.gamingStats.level.toString())
            Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.LightGray)
            GameStatCell("Rank", user.gamingStats.rank)
            Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.LightGray)
            GameStatCell("Wins", user.gamingStats.matchesWon.toString())
        }
    }
}

@Composable
fun GameStatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
        )
    }
}

@Composable
fun SocialMenuIOS(
    onAddFriendClick: () -> Unit, 
    onFriendsClick: () -> Unit,
    onFriendRequestsClick: () -> Unit = {}
) {
    IOSCard {
        Column {
            MenuRow(title = "My Circle", subtitle = "View your groups")
            Divider(color = Color(0xFFE5E5EA), modifier = Modifier.padding(start = 16.dp))
            MenuRow(title = "Friend Requests", subtitle = "Pending requests", onClick = onFriendRequestsClick)
            Divider(color = Color(0xFFE5E5EA), modifier = Modifier.padding(start = 16.dp))
            MenuRow(title = "Friends", subtitle = "Manage your friends", onClick = onFriendsClick)
            Divider(color = Color(0xFFE5E5EA), modifier = Modifier.padding(start = 16.dp))
            MenuRow(
                title = "Find People", 
                subtitle = "Scan QR or search by username", 
                isLast = true,
                onClick = onAddFriendClick
            )
        }
    }
}

@Composable
fun MenuRow(title: String, subtitle: String, isLast: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
    }
}



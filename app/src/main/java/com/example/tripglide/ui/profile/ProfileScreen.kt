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
import androidx.compose.material.icons.filled.Edit
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.User
import com.example.tripglide.ui.components.IOSCard
import com.example.tripglide.ui.components.IOSSectionTitle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onEditProfileClick: () -> Unit,
    onAddFriendClick: () -> Unit // Kept for QR/Search access if needed, or I can remove.
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
                actions = {
                    IconButton(onClick = onAddFriendClick) {
                        Icon(Icons.Filled.Share, contentDescription = "My QR")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7)
                )
            )
        },
        containerColor = Color(0xFFF2F2F7)
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
                // Reduced top padding (modifier padding(top=...) inside IOSCard handled via ProfileHeaderIOS params?)
                // ProfileHeaderIOS used hardcoded top padding. I'll override it here by redefining or copying logic.
                // I'll inline the header logic or modify ProfileHeaderIOS.
                // I'll inline for control.
                
                // HEADER
                IOSCard(modifier = Modifier.padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(user!!.photoUrl.ifEmpty { "https://i.pravatar.cc/300" }),
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
                                    .clickable { 
                                         photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user!!.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if (user!!.username.isNotEmpty()) user!!.username else "@username", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                        
                        IconButton(onClick = onEditProfileClick) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFF007AFF))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (user!!.bio.isNotEmpty()) {
                        Text(user!!.bio, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItemIOS(value = user!!.socialStats.followersCount.toString(), label = "Followers")
                        StatItemIOS(value = user!!.socialStats.followingCount.toString(), label = "Following")
                        StatItemIOS(value = user!!.socialStats.reputationScore.toString(), label = "Reputation")
                    }
                }
                
                // STATS
                IOSSectionTitle("Statistics")
                GamingStatsIOS(user!!)
                
                // LOGOUT BUTTON at Bottom
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = {
                        viewModel.signOut()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)), // iOS System Red
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatItemIOS(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun GamingStatsIOS(user: User) {
    IOSCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.Gray)
    }
}

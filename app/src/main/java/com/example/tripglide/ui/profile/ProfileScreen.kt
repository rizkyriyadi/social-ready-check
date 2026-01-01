package com.example.tripglide.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.DotaLinkedAccount
import com.example.tripglide.data.model.User
import com.example.tripglide.ui.components.IOSCard
import com.example.tripglide.ui.components.IOSSectionTitle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// Dota 2 brand color
private val DotaRed = Color(0xFFFF6046)

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
                
                // STATS - Hidden for now
                // IOSSectionTitle("Statistics")
                // GamingStatsIOS(user!!)
                
                // GAME ACCOUNTS SECTION
                IOSSectionTitle("Game Accounts")
                GameAccountsSection(
                    user = user!!,
                    viewModel = viewModel
                )
                
                // SETTINGS SECTION
                IOSSectionTitle("Settings")
                SettingsSection()
                
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

// ==================== GAME ACCOUNTS SECTION ====================

@Composable
fun GameAccountsSection(
    user: User,
    viewModel: ProfileViewModel
) {
    val dotaLinkState by viewModel.dotaLinkState.collectAsState()
    var showLinkDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }
    var pendingAccount by remember { mutableStateOf<DotaLinkedAccount?>(null) }
    
    // Handle state changes
    LaunchedEffect(dotaLinkState) {
        when (dotaLinkState) {
            is DotaLinkState.VerificationSuccess -> {
                pendingAccount = (dotaLinkState as DotaLinkState.VerificationSuccess).account
                showLinkDialog = false
                showConfirmDialog = true
            }
            is DotaLinkState.LinkSuccess -> {
                showConfirmDialog = false
                pendingAccount = null
                viewModel.resetDotaLinkState()
            }
            else -> {}
        }
    }
    
    val linkedDota = user.linkedAccounts.dota2
    
    if (linkedDota != null) {
        // Show linked Dota card
        DotaLinkedCard(
            account = linkedDota,
            onUnlink = { showUnlinkDialog = true }
        )
    } else {
        // Show link button
        LinkDotaButton(onClick = { showLinkDialog = true })
    }
    
    // Link Dota Dialog
    if (showLinkDialog) {
        LinkDotaDialog(
            isLoading = dotaLinkState is DotaLinkState.Loading,
            error = (dotaLinkState as? DotaLinkState.Error)?.message,
            onDismiss = {
                showLinkDialog = false
                viewModel.resetDotaLinkState()
            },
            onSubmit = { friendId ->
                viewModel.verifyDotaAccount(friendId)
            }
        )
    }
    
    // Confirmation Dialog
    if (showConfirmDialog && pendingAccount != null) {
        ConfirmDotaLinkDialog(
            account = pendingAccount!!,
            isLoading = dotaLinkState is DotaLinkState.Loading,
            onDismiss = {
                showConfirmDialog = false
                pendingAccount = null
                viewModel.resetDotaLinkState()
            },
            onConfirm = {
                viewModel.confirmDotaLink(pendingAccount!!)
            }
        )
    }
    
    // Unlink Confirmation Dialog
    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Unlink Dota 2?") },
            text = { Text("This will remove your Dota 2 account from your profile.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkDialog = false
                        viewModel.unlinkDotaAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LinkDotaButton(onClick: () -> Unit) {
    IOSCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dota 2 icon placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DotaRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "D2",
                    color = DotaRed,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Dota 2",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Link your Steam account",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DotaRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DotaRed)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Link", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DotaLinkedCard(
    account: DotaLinkedAccount,
    onUnlink: () -> Unit
) {
    IOSCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank/Avatar section
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, getMedalColor(account.medalTier), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (account.avatarUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(account.avatarUrl),
                        contentDescription = "Dota Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        account.personaName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.personaName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Medal badge
                    Box(
                        modifier = Modifier
                            .background(
                                getMedalColor(account.medalTier).copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            account.medalName,
                            style = MaterialTheme.typography.labelSmall,
                            color = getMedalColor(account.medalTier),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    account.mmrEstimate?.let { mmr ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "~${mmr} MMR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Text(
                    "ID: ${account.accountId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            // Unlink button
            IconButton(onClick = onUnlink) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Unlink",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun LinkDotaDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var friendId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { 
            Text("Link Dota 2 Account", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Enter your Dota 2 Friend ID (found in-game or on Dotabuff/OpenDota)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = friendId,
                    onValueChange = { friendId = it.filter { c -> c.isDigit() } },
                    label = { Text("Friend ID") },
                    placeholder = { Text("e.g. 123456789") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    isError = error != null
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...", color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(friendId) },
                enabled = friendId.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = DotaRed)
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDotaLinkDialog(
    account: DotaLinkedAccount,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { 
            Text("Is this you?", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, getMedalColor(account.medalTier), CircleShape)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(account.avatarUrl),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    account.personaName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Medal badge
                Box(
                    modifier = Modifier
                        .background(
                            getMedalColor(account.medalTier).copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        account.medalName,
                        style = MaterialTheme.typography.titleMedium,
                        color = getMedalColor(account.medalTier),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                account.mmrEstimate?.let { mmr ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Estimated MMR: $mmr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = DotaRed)
            ) {
                Text("Yes, Link Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Not Me")
            }
        }
    )
}

/**
 * Get color for medal tier (1-8)
 */
@Composable
fun getMedalColor(tier: Int): Color {
    return when (tier) {
        1 -> Color(0xFF8B7355) // Herald - Brown
        2 -> Color(0xFF8B8B8B) // Guardian - Gray
        3 -> Color(0xFF4CAF50) // Crusader - Green
        4 -> Color(0xFF2196F3) // Archon - Blue
        5 -> Color(0xFFFFC107) // Legend - Gold
        6 -> Color(0xFF9C27B0) // Ancient - Purple
        7 -> Color(0xFFE91E63) // Divine - Pink
        8 -> Color(0xFFFFD700) // Immortal - Bright Gold
        else -> Color.Gray
    }
}

// ==================== SETTINGS SECTION ====================

@Composable
fun SettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { com.example.tripglide.util.UpdateManager(context) }
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<com.example.tripglide.data.model.UpdateCheckResult?>(null) }
    
    val (versionCode, versionName) = updateManager.getCurrentVersionInfo()
    
    IOSCard {
        Column {
            // Check for Updates Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isCheckingUpdate) {
                        isCheckingUpdate = true
                        scope.launch {
                            val result = updateManager.checkForUpdates()
                            updateResult = result
                            isCheckingUpdate = false
                            showUpdateDialog = true
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF007AFF).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔄", style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Check for Updates",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Current version: v$versionName",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF007AFF)
                    )
                } else {
                    Text(
                        "›",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                }
            }
            
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            
            // App Version Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Gray.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ℹ️", style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "App Version",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "v$versionName (Build $versionCode)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
    
    // Update Dialog
    if (showUpdateDialog && updateResult != null) {
        UpdateResultDialog(
            result = updateResult!!,
            onDismiss = { 
                showUpdateDialog = false 
                updateResult = null
            },
            onDownload = { updateInfo ->
                updateManager.downloadUpdate(updateInfo)
                showUpdateDialog = false
                updateResult = null
            }
        )
    }
}

@Composable
fun UpdateResultDialog(
    result: com.example.tripglide.data.model.UpdateCheckResult,
    onDismiss: () -> Unit,
    onDownload: (com.example.tripglide.data.model.AppUpdateInfo) -> Unit
) {
    when (result) {
        is com.example.tripglide.data.model.UpdateCheckResult.NoUpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Text("✅", style = MaterialTheme.typography.displaySmall) },
                title = { Text("You're Up to Date!", fontWeight = FontWeight.Bold) },
                text = { 
                    Text(
                        "You have the latest version of Jack & Mei installed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = Color(0xFF007AFF))
                    }
                }
            )
        }
        
        is com.example.tripglide.data.model.UpdateCheckResult.UpdateAvailable -> {
            val updateInfo = result.updateInfo
            AlertDialog(
                onDismissRequest = { if (!updateInfo.forceUpdate) onDismiss() },
                icon = { Text("🎉", style = MaterialTheme.typography.displaySmall) },
                title = { 
                    Text(
                        "Update Available!",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Version ${updateInfo.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF007AFF)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            "What's New:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                        
                        if (updateInfo.forceUpdate) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "⚠️ This update is required to continue using the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onDownload(updateInfo) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Download & Install")
                    }
                },
                dismissButton = {
                    if (!updateInfo.forceUpdate) {
                        TextButton(onClick = onDismiss) {
                            Text("Later", color = Color.Gray)
                        }
                    }
                }
            )
        }
        
        is com.example.tripglide.data.model.UpdateCheckResult.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Text("❌", style = MaterialTheme.typography.displaySmall) },
                title = { Text("Update Check Failed", fontWeight = FontWeight.Bold) },
                text = { 
                    Text(
                        "Could not check for updates. Please try again later.\n\nError: ${result.message}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = Color(0xFF007AFF))
                    }
                }
            )
        }
        
        else -> { /* Loading state - shouldn't show dialog */ }
    }
}

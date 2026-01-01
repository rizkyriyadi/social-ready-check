package com.example.tripglide.ui.detail

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.data.repository.CircleRepositoryImpl
import com.example.tripglide.data.repository.StorageRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// --- PREMIUM COLORS ---
private val MidnightBlue = Color(0xFF0F172A)
private val DeepBlack = Color(0xFF000000)
private val GlassSurface = Color(0x26FFFFFF) // 15% White
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF94A3B8)
private val AccentBlue = Color(0xFF3B82F6)
private val DangerRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleSettingsScreen(
    circleId: String,
    onBackClick: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onLeaveCircle: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val circleRepo = remember { CircleRepositoryImpl() }
    val storageRepo = remember { StorageRepository() }

    var circleName by remember { mutableStateOf("") }
    var circleImage by remember { mutableStateOf<String?>(null) }
    var inviteCode by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }

    val logs by circleRepo.getCircleActivityLogs(circleId).collectAsState(initial = emptyList())

    LaunchedEffect(circleId) {
        val result = circleRepo.getCircleById(circleId)
        result.getOrNull()?.let {
            circleName = it.name
            circleImage = it.imageUrl
            inviteCode = it.code
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                Log.d("CircleSettings", "Uploading image: $it")
                val uploadResult = storageRepo.uploadCircleImage(circleId, it)
                uploadResult.onSuccess { url ->
                    Log.d("CircleSettings", "Upload success: $url")
                    val updateResult = circleRepo.updateCircleInfo(circleId, null, url)
                    updateResult.onSuccess {
                        circleImage = url
                        Log.d("CircleSettings", "Circle updated with new image")
                    }.onFailure { e ->
                        Log.e("CircleSettings", "Failed to update circle", e)
                    }
                }.onFailure { e ->
                    Log.e("CircleSettings", "Upload failed", e)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightBlue, DeepBlack)
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = GlassSurface)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // A. HEADER (Identity)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        // Blurred Cover
                        AsyncImage(
                            model = circleImage ?: "https://via.placeholder.com/400",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .blur(20.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MidnightBlue)
                                    )
                                )
                        )

                        // Avatar & Name
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = circleImage ?: "https://via.placeholder.com/150",
                                    contentDescription = "Circle Avatar",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .background(MidnightBlue),
                                    contentScale = ContentScale.Crop
                                )
                                // Camera Badge
                                Surface(
                                    onClick = { imageLauncher.launch("image/*") },
                                    shape = CircleShape,
                                    color = AccentBlue,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Change Photo",
                                        modifier = Modifier.padding(8.dp),
                                        tint = TextPrimary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showRenameDialog = true }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = circleName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                // B. SETTINGS SECTION
                item {
                    GlassContainer {
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Members",
                            value = "View & Manage",
                            onClick = onNavigateToMembers
                        )
                        Divider(color = GlassSurface, thickness = 0.5.dp)
                        SettingsItem(
                            icon = Icons.Default.Lock,
                            title = "Invite Code",
                            value = inviteCode,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(inviteCode))
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // C. ACTIVITY LOG SECTION
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            text = "RECENT ACTIVITY",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (logs.isEmpty()) {
                            Text(
                                text = "No recent activity.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                        } else {
                            logs.forEach { log ->
                                ActivityLogItem(log)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }

                // D. DANGER ZONE
                item {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val result = circleRepo.leaveCircle(circleId)
                                if (result.isSuccess) {
                                    onLeaveCircle()
                                } else {
                                    Log.e("CircleSettings", "Error leaving circle", result.exceptionOrNull())
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                    ) {
                        Text("Leave Squad", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(circleName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = MidnightBlue,
            title = { Text("Rename Circle", color = TextPrimary) },
            text = { 
                OutlinedTextField(
                    value = newName, 
                    onValueChange = { newName = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = TextSecondary
                    )
                ) 
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        circleRepo.updateCircleInfo(circleId, newName, null)
                        circleName = newName
                        showRenameDialog = false
                    }
                }) { Text("Save", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun GlassContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(GlassSurface, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
        content = content
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextSecondary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        }
        Text(value, color = AccentBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ActivityLogItem(log: com.example.tripglide.data.model.AuditLog) {
    Row(verticalAlignment = Alignment.Top) {
        // Timeline dot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(AccentBlue, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(30.dp)
                    .background(GlassSurface)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "${log.actorName} ${log.details}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = log.timestamp?.toDate()?.let { 
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(it) 
                } ?: "Just now",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

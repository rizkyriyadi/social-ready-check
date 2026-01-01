package com.example.tripglide.ui.detail

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tripglide.data.repository.CircleRepositoryImpl
import com.example.tripglide.data.repository.StorageRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleSettingsScreen(
    circleId: String,
    onBackClick: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onLeaveCircle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Ideally use Hilt, but for now manual injection
    val circleRepo = remember { CircleRepositoryImpl() }
    val storageRepo = remember { StorageRepository() }

    var circleName by remember { mutableStateOf("") }
    var circleImage by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(circleId) {
        val result = circleRepo.getCircleById(circleId)
        result.getOrNull()?.let {
            circleName = it.name
            circleImage = it.imageUrl
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val uploadResult = storageRepo.uploadCircleImage(circleId, it)
                uploadResult.onSuccess { url ->
                    circleRepo.updateCircleInfo(circleId, null, url)
                    circleImage = url
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Circle Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = circleImage ?: "https://via.placeholder.com/150",
                    contentDescription = "Circle Image",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { imageLauncher.launch("image/*") },
                    contentScale = ContentScale.Crop
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(circleName, style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Menu Items
            OutlinedButton(
                onClick = onNavigateToMembers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Members List")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Danger Zone
            Button(
                onClick = {
                    scope.launch {
                        val result = circleRepo.leaveCircle(circleId)
                        if (result.isSuccess) {
                            onLeaveCircle()
                        } else {
                            // Log error or show snackbar (simplified for now)
                            Log.e("CircleSettings", "Error leaving circle", result.exceptionOrNull())
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LEAVE SQUAD")
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(circleName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Circle") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        circleRepo.updateCircleInfo(circleId, newName, null)
                        circleName = newName
                        showRenameDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

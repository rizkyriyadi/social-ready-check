package com.example.tripglide.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tripglide.ui.components.IOSButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // Initialize state only when user loads, or keep empty if null
    var name by remember(user) { mutableStateOf(user?.displayName ?: "") }
    var bio by remember(user) { mutableStateOf(user?.bio ?: "") }
    var username by remember(user) { mutableStateOf(user?.username?.removePrefix("@") ?: "") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    
    // Check if user is actually loaded
    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             CircularProgressIndicator()
        }
        LaunchedEffect(Unit) {
            viewModel.loadUserProfile()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it.filter { char -> char.isLetterOrDigit() || char == '_' }.take(20)
                    usernameError = null 
                },
                label = { Text("Username") },
                prefix = { Text("@") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = usernameError != null,
                supportingText = {
                    if (usernameError != null) {
                        Text(usernameError!!, color = Color.Red)
                    } else {
                        Text("Unique handle for your profile", color = Color.Gray)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isLoading) {
                 Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                IOSButton(
                    text = "Save Changes",
                    onClick = { 
                        if (username.length < 3) {
                            usernameError = "Too short (min 3 chars)"
                            return@IOSButton
                        }
                        
                        // Optimistic UI or wait for result? 
                        // ViewModel already reloads profile on success
                        viewModel.updateProfile(name, bio, username)
                        // Verify success? Ideally VM exposes a one-time event or we observe user changes
                        // For now we trust the reload
                        Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                        onBackClick()
                    }
                )
            }
        }
    }
}

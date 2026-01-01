package com.example.tripglide.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tripglide.data.repository.CircleRepositoryImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    circleId: String,
    onBackClick: () -> Unit
) {
    val circleRepo = remember { CircleRepositoryImpl() }
    val members = circleRepo.getFullMembers(circleId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Members") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(members.value) { user ->
                ListItem(
                    headlineContent = { Text(user.displayName ?: "Unknown") },
                    supportingContent = { Text(user.username ?: "") },
                    leadingContent = {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                )
            }
        }
    }
}

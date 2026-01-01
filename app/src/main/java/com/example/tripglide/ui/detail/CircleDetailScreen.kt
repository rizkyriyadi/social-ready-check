package com.example.tripglide.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    circleId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    // Mock Data for UI dev
    val members = listOf(
        "https://i.pravatar.cc/150?u=1",
        "https://i.pravatar.cc/150?u=2",
        "https://i.pravatar.cc/150?u=3",
        "https://i.pravatar.cc/150?u=4"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pejuang Mythic", color = White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF101010),
                    titleContentColor = White
                )
            )
        },
        containerColor = Color(0xFF101010)
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cell A: SUMMON BUTTON (Full Span)
            item(span = StaggeredGridItemSpan.FullLine) {
                SummonButton()
            }

            // Cell B: Live Status (Full Span)
            item(span = StaggeredGridItemSpan.FullLine) {
                LiveStatusCard(members)
            }

            // Cell C: Chat Shortcut
            item {
                ChatShortcutCard(onClick = onNavigateToChat)
            }

            // Cell D: Stats
            item {
                StatsCard()
            }
            
            // Extra: Recent Games (Full Span placeholder)
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp)
                ) {
                    Text("Recent Matches History", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun SummonButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFF4B1F), Color(0xFFFF9068))
                )
            )
            .clickable { /* Logic handled in VM later */ }
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Notifications, 
                    contentDescription = null, 
                    tint = White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SUMMON PARTY",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Blast notifications to 4 members",
                color = White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LiveStatusCard(memberUrls: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Live Status",
                color = Color.Gray,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                memberUrls.forEach { url ->
                    Box {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // Online indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853)) // Green
                                .align(Alignment.BottomEnd)
                                .background(Color(0xFF1E1E1E)) // border effect
                        )
                    }
                }
                // Add button mock
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = White, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun ChatShortcutCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = White)
            }
            
            Column {
                Text(
                    text = "Basecamp",
                    color = White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "3 New Messages",
                    color = Color(0xFF64B5F6), // Light Blue
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
             Text(
                text = "Recent Performance",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
            
            Column {
                Text(
                    text = "Win Streak",
                    color = White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "3",
                    color = Color(0xFF00C853),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

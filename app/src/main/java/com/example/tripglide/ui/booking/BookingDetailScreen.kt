package com.example.tripglide.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tripglide.ui.components.BookingTabs
import com.example.tripglide.ui.components.TimelineItem
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

@Composable
fun BookingDetailScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = White,
        bottomBar = {
            // Book Tour Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = { /* TODO: Book logic */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Black)
                ) {
                    Text("Book a tour", color = White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White) // Just transparent/white
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Black)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Iconic Brazil", style = MaterialTheme.typography.titleLarge)
                    Text(text = "Wed, Oct 21 - Sun, Nov 1", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Black)
                }
            }
            
            // Tabs
            BookingTabs(selectedTabIndex = 0, onTabSelected = {})
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "8-Days Brazil Adventure",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Timeline
            TimelineItem(
                time = "", 
                title = "Arrival to Rio de Janeiro",
                imageUrl = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?q=80&w=2670&auto=format&fit=crop"
            )
            
             TimelineItem(
                time = "Morning",
                title = "Arrive in Rio de Janeiro and transfer to your hotel",
                description = null
            )
             TimelineItem(
                time = "Afternoon",
                title = "Free time to relax or explore the nearby area",
                description = null
            )
             TimelineItem(
                time = "Evening",
                title = "Welcome dinner at a traditional Brazilian restaurant",
                description = null,
                isLast = false
             )
            
            // Should be Day 2 ideally, but following screenshot strictly
            // Screenshot shows another card for Day 2 Rio Highlights
             TimelineItem(
                time = "", 
                title = "Rio de Janeiro Highlights",
                imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=2673&auto=format&fit=crop",
                 isLast = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

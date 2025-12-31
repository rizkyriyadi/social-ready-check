package com.example.tripglide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.LightGray
import com.example.tripglide.ui.theme.White

@Composable
fun BookingTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Tour schedule", "Accomodation", "Booking detail")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (selectedTabIndex == index) Black else LightGray)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedTabIndex == index) White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun TimelineItem(
    time: String,
    title: String,
    description: String? = null,
    isLast: Boolean = false,
    imageUrl: String? = null
) {
    val ballSize = 8.dp
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(IntrinsicSize.Min) // Important for measuring line height
    ) {
        // Timeline Line Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(ballSize)
                    .clip(CircleShape)
                    .background(Black)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp)
        ) {
            if (imageUrl != null) {
                // Special case for Image card in timeline (Day 1)
                TimelineImageCard(title, description ?: "", imageUrl)
            } else {
                Text(text = time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Black)
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun TimelineImageCard(
    title: String,
    subtitle: String,
    imageUrl: String
) {
    // Looks like an expandable card in the screenshot for "Day 1"
    // "Day 1 Arrival to Rio de Janeiro" with arrow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightGray)
            .padding(12.dp)
    ) {
         Row(verticalAlignment = Alignment.CenterVertically) {
             AsyncImage(
                 model = imageUrl,
                 contentDescription = null,
                 modifier = Modifier
                     .size(60.dp)
                     .clip(RoundedCornerShape(12.dp)),
                 contentScale = ContentScale.Crop
             )
             Spacer(modifier = Modifier.width(12.dp))
             Column(modifier = Modifier.weight(1f)) {
                 Text(text = "Day 1", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                 Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Black)
             }
         }
    }
}

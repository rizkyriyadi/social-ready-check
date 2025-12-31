package com.example.tripglide.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White
import androidx.compose.foundation.clickable

@Composable
fun DetailHeader(
    imageUrl: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .statusBarsPadding(), // Handle status bar
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(White)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Black)
            }
            
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(White)
            ) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Black)
            }
        }
    }
}

@Composable
fun TripInfoSection(
    title: String,
    location: String,
    rating: Double,
    reviews: Int,
    description: String
) {
    Column(modifier = Modifier.padding(24.dp)) {
        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Flag icon could go here
                     Text(
                        text = "\uD83C\uDDE7\uD83C\uDDF7 $location", // Brasil Flag
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Black, modifier = Modifier.size(16.dp))
                    Text(text = " $rating", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$reviews reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            lineHeight = 24.sp
        )
        
        Text(
            text = "Read more",
             style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Black,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Re-using sp from Type.kt won't work directly if not imported, using extension .sp requires import
// Adding imports manually or fixing usage.

@Composable
fun UpcomingTourCard(
    title: String,
    duration: String,
    price: String,
    rating: Double,
    reviews: Int,
    imageUrl: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(140.dp) // Fixed height for card
            .clickable(onClick = onClick)
    ) {
         Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
        ) {
             Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
             // Heart icon small
             Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Black, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Black,
                     maxLines = 2
                )
                 Spacer(modifier = Modifier.height(8.dp))
                 Text(
                    text = "$duration • from $price",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Black, modifier = Modifier.size(14.dp))
                    Text(text = " $rating", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                     Text(text = " ($reviews)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Black)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go", tint = White, modifier = Modifier.size(16.dp)) // ArrowBack rotated or generic arrow
                    // Using ArrowBack for now as placeholder, likely need ArrowForward
                }
            }
        }
    }
}

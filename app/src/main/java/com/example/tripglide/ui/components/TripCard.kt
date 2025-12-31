package com.example.tripglide.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

@Composable
fun TripCard(
    title: String,
    subtitle: String,
    rating: Double,
    reviews: Int,
    imageUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.LightGray)
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 300f
                    )
                )
        )

        // Heart Icon
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.2f)), // Glassmorphism-ish
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = White)
        }

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Brazil", // Hardcoded subtitle for now like in image
                color = White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = title,
                color = White,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$rating", color = White, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$reviews reviews", color = White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
        
        // See More Button
        IconButton(
           onClick = onClick,
           modifier = Modifier
               .align(Alignment.BottomEnd)
               .padding(24.dp)
               .size(48.dp)
               .clip(CircleShape)
               .background(White)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "See more", tint = Black)
        }
    }
}

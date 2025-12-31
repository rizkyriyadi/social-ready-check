package com.example.tripglide.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.ui.components.DetailHeader
import com.example.tripglide.ui.components.TripInfoSection
import com.example.tripglide.ui.theme.White

@Composable
fun TripDetailScreen(
    onBackClick: () -> Unit,
    onTourClick: () -> Unit
) {
    Scaffold(
        containerColor = White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            DetailHeader(
                imageUrl = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?q=80&w=2670&auto=format&fit=crop",
                onBackClick = onBackClick
            )
            
            TripInfoSection(
                title = "Rio de Janeiro",
                location = "Brazil",
                rating = 5.0,
                reviews = 143,
                description = "Rio de Janeiro, often simply called Rio, is one of Brazil's most iconic cities, renowned for..."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming tours",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = {}) {
                    Text("See all", color = Color.Gray)
                }
            }
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(3) { index ->
                    UpcomingTourHorizontalCard(
                        title = if (index == 0) "Iconic Brazil" else "Beach Paradise",
                        days = "8 days",
                        price = "$659/person",
                        rating = 4.6 + (index.toDouble() * 0.1),
                        reviews = 56 + index,
                        imageUrl = if (index == 0) "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?q=80&w=2670&auto=format&fit=crop" else "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=2673&auto=format&fit=crop",
                        onClick = onTourClick
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun UpcomingTourHorizontalCard(
    title: String,
    days: String,
    price: String,
    rating: Double,
    reviews: Int,
    imageUrl: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
             Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$days • from $price",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
             IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = White)
            }
        }
        
         Spacer(modifier = Modifier.height(4.dp))
         Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Text(text = " $rating", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
             Text(text = " $reviews reviews", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

package com.example.tripglide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

@Composable
fun BottomNavBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(Black)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home (Selected)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                 Icon(Icons.Default.Home, contentDescription = "Home", tint = Black)
            }
            
            // Other items
            IconButton(onClick = {}) {
                Icon(Icons.Default.Info, contentDescription = "Docs", tint = Color.Gray)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites", tint = Color.Gray)
            }
             IconButton(onClick = {}) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Gray)
            }
        }
    }
}

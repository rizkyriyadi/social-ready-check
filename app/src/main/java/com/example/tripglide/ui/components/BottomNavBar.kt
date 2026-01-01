package com.example.tripglide.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

@Composable
fun BottomNavBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
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
            NavBarItem(
                icon = Icons.Default.Home,
                description = "Home",
                isSelected = currentTab == 0,
                onClick = { onTabSelected(0) }
            )
            NavBarItem(
                icon = Icons.Default.Star, // Squads
                description = "Squads",
                isSelected = currentTab == 1,
                onClick = { onTabSelected(1) }
            )
            NavBarItem(
                icon = Icons.Default.Favorite, // Friends
                description = "Friends",
                isSelected = currentTab == 2,
                onClick = { onTabSelected(2) }
            )
            NavBarItem(
                icon = Icons.Default.Person, // Profile
                description = "Profile",
                isSelected = currentTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, tint = Black)
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = description, tint = Color.Gray)
        }
    }
}

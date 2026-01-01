package com.example.tripglide.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.GrayBG
import com.example.tripglide.ui.theme.LightGray
import com.example.tripglide.ui.theme.White

@Composable
fun TopBar(
    userName: String,
    avatarUrl: String,
    onAvatarClick: () -> Unit,
    onAddClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello, $userName",
                style = MaterialTheme.typography.titleLarge,
                color = Black
            )
            Text(
                text = "Welcome to TripGlide",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onAddClick != null) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(Color.Transparent) // Clean look
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Create Squad", 
                        tint = Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Image(
                painter = rememberAsyncImagePainter(
                    model = avatarUrl.ifEmpty { "https://i.pravatar.cc/300" }
                ),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { onAvatarClick() },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Black) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(White),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                disabledContainerColor = White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Black),
            contentAlignment = Alignment.Center
        ) {
             Icon(Icons.Default.Menu, contentDescription = "Filter", tint = White)
        }
    }
}

@Composable
fun CategorySingleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) Black else White)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) White else Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

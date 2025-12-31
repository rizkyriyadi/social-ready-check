package com.example.tripglide.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.tripglide.data.model.Circle
import com.example.tripglide.ui.components.BottomNavBar
import com.example.tripglide.ui.components.CategorySingleChip
import com.example.tripglide.ui.components.SearchBar
import com.example.tripglide.ui.components.TopBar
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

// Mapping for Game Wallpapers
private fun getGameWallpaper(game: String): String {
    return when (game.lowercase()) {
        "dota 2" -> "https://cdn.akamai.steamstatic.com/apps/dota2/images/dota2_social_share_default.jpg"
        "valorant" -> "https://images.contentstack.io/v3/assets/bltb6530b271fddd0b1/blt3f072eb3d2f9bd88/6333621b1b17b65313854a65/VALORANT_Jett_Teaser_Crop_1920x1080.jpg"
        "mobile legends" -> "https://img.redbull.com/images/c_crop,x_517,y_0,h_1080,w_1620/c_fill,w_1500,h_1000/q_auto,f_auto/redbullcom/2021/11/15/dndycu6kyd1dwhc8wzvn/mobile-legends-bang-bang-hero-art"
        else -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2670&auto=format&fit=crop" // Generic gaming
    }
}

@Composable
fun HomeScreen(
    onTripClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context)
    )
    val userState by viewModel.user.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val shouldNavigate by viewModel.shouldNavigateToOnboarding.collectAsState()
    
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            onNavigateToOnboarding()
            viewModel.onOnboardingNavigationHandled()
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar() },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Scrollable Section (Non-list content)
            // We use a LazyColumn to hold everything so it scrolls nicely together
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp), // Clearance for bottom bar
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Item
                item {
                    Column {
                        TopBar(
                            userName = userState?.displayName ?: "Traveler",
                            avatarUrl = userState?.photoUrl ?: "",
                            onAvatarClick = onProfileClick
                        )
                        
                        SearchBar()
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Select your Squad",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Horizontal Categories
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(listOf("All", "Dota 2", "Valorant", "Mobile Legends")) { category ->
                                CategorySingleChip(
                                    text = category,
                                    isSelected = selectedFilter == category,
                                    onClick = { viewModel.setFilter(category) }
                                )
                            }
                        }
                    }
                }

                // Circles List Items
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Black)
                            }
                        }
                    }
                    is HomeUiState.Error -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    is HomeUiState.Success -> {
                        if (state.circles.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No squads found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            items(state.circles) { circle ->
                                CircleCard(
                                    title = circle.name,
                                    subtitle = circle.game,
                                    statusText = "${circle.metadata.memberCount}/5 Online", // Mock status
                                    imageUrl = getGameWallpaper(circle.game),
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    onClick = onTripClick // Redirect to detail
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Replicated TripCard style but adapted for Circles
@Composable
private fun CircleCard(
    title: String,
    subtitle: String,
    statusText: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp) // Keep large height
            .clip(RoundedCornerShape(32.dp))
            .background(Color.LightGray)
            .clickable(onClick = onClick)
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

        // Heart Icon (Keep aesthetics)
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
                text = subtitle,
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
                // Status Text instead of Rating + Reviews
                Text(text = statusText, color = White, style = MaterialTheme.typography.bodySmall)
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

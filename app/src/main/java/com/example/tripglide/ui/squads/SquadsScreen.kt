package com.example.tripglide.ui.squads

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
import androidx.compose.material.icons.filled.Add
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
import com.example.tripglide.ui.components.CategorySingleChip
import com.example.tripglide.ui.components.SearchBar
import com.example.tripglide.ui.components.TopBar
import com.example.tripglide.ui.home.HomeUiState
import com.example.tripglide.ui.home.HomeViewModel
import com.example.tripglide.ui.home.HomeViewModelFactory
import com.example.tripglide.ui.home.CreateCircleUiState
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

// Mapping for Game Wallpapers (Copied from HomeScreen)
private fun getGameWallpaper(game: String): String {
    return when (game.lowercase()) {
        "dota 2" -> "https://cdn.akamai.steamstatic.com/apps/dota2/images/dota2_social_share_default.jpg"
        "valorant" -> "https://images.contentstack.io/v3/assets/bltb6530b271fddd0b1/blt3f072eb3d2f9bd88/6333621b1b17b65313854a65/VALORANT_Jett_Teaser_Crop_1920x1080.jpg"
        "mobile legends" -> "https://img.redbull.com/images/c_crop,x_517,y_0,h_1080,w_1620/c_fill,w_1500,h_1000/q_auto,f_auto/redbullcom/2021/11/15/dndycu6kyd1dwhc8wzvn/mobile-legends-bang-bang-hero-art"
        else -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2670&auto=format&fit=crop" // Generic gaming
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadsScreen(
    onCircleClick: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val creationState by viewModel.creationState.collectAsState()
    
    // Sheet State
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Handle Creation Success
    LaunchedEffect(creationState) {
        if (creationState is CreateCircleUiState.Success) {
            showSheet = false
            viewModel.resetCreationState()
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            CreateCircleSheet(
                onDismiss = { showSheet = false },
                onCreate = { name, game ->
                    viewModel.createCircle(name, game)
                },
                isLoading = creationState is CreateCircleUiState.Loading
            )
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5)
        // No BottomBar here, it's handled by MainScreen
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Item (TopBar + Search + Filter)
                item {
                    Column {
                        // Custom Header for Squads (No Greeting)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Squads",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Black
                            )
                            
                            // Add Button
                            IconButton(
                                onClick = { showSheet = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(White)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create Squad", tint = Black)
                            }
                        }
                        
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
                            // Large Empty State CTA
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No squads found yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { showSheet = true },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Black),
                                            modifier = Modifier.fillMaxWidth().height(56.dp)
                                        ) {
                                            Text("Create Your First Squad", style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(state.circles) { circle ->
                                CircleCard(
                                    title = circle.name,
                                    subtitle = circle.game,
                                    statusText = "${circle.metadata.memberCount}/5 Online",
                                    imageUrl = getGameWallpaper(circle.game),
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    onClick = { onCircleClick(circle.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
            .height(400.dp) 
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

        // Heart Icon
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.2f)),
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

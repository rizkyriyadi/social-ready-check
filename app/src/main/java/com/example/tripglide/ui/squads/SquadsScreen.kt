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
import com.example.tripglide.ui.home.JoinCircleUiState
import androidx.compose.ui.unit.sp
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
import com.example.tripglide.ui.home.HomeUiState
import com.example.tripglide.ui.home.HomeViewModel
import com.example.tripglide.ui.home.HomeViewModelFactory
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

// Mapping for Game Wallpapers
private fun getGameWallpaper(game: String): String {
    return when (game.lowercase()) {
        "dota 2" -> "https://cdn.akamai.steamstatic.com/apps/dota2/images/dota2_social_share_default.jpg"
        "valorant" -> "https://images.contentstack.io/v3/assets/bltb6530b271fddd0b1/blt3f072eb3d2f9bd88/6333621b1b17b65313854a65/VALORANT_Jett_Teaser_Crop_1920x1080.jpg"
        "mobile legends", "mlbb" -> "https://img.redbull.com/images/c_crop,x_517,y_0,h_1080,w_1620/c_fill,w_1500,h_1000/q_auto,f_auto/redbullcom/2021/11/15/dndycu6kyd1dwhc8wzvn/mobile-legends-bang-bang-hero-art"
        "cs2" -> "https://cdn.akamai.steamstatic.com/apps/csgo/images/csgo_react/social/cs2.jpg"
        else -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2670&auto=format&fit=crop" // Generic gaming
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadsScreen(
    onCircleClick: (String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val joinState by viewModel.joinState.collectAsState()

    var showAddOptions by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Join Dialog State
    var joinCode by remember { mutableStateOf("") }

    // Handle Join State
    LaunchedEffect(joinState) {
        if (joinState is JoinCircleUiState.Success) {
            showJoinDialog = false
            joinCode = ""
            viewModel.resetJoinState()
            snackbarHostState.showSnackbar("Successfully joined the squad!")
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Black)
                    }
                }
                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = Color.Red)
                    }
                }
                is HomeUiState.Success -> {
                    if (state.circles.isEmpty()) {
                        EmptyState { showAddOptions = true }
                    } else {
                        SquadListContent(
                            circles = state.circles,
                            selectedFilter = selectedFilter,
                            onFilterSelect = { viewModel.setFilter(it) },
                            onCircleClick = onCircleClick,
                            onNavigateToCreate = { showAddOptions = true }
                        )
                    }
                }
            }
        }

        // Add Options Bottom Sheet
        if (showAddOptions) {
            ModalBottomSheet(
                onDismissRequest = { showAddOptions = false },
                sheetState = sheetState,
                containerColor = White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Add New Squad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Create Option
                    ListItem(
                        headlineContent = { Text("Assemble New Squad", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
                        supportingContent = { Text("Create a fresh squad for your team") },
                        leadingContent = { 
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = null, 
                                tint = White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Black)
                                    .padding(12.dp)
                            ) 
                        },
                        modifier = Modifier
                            .clickable {
                                showAddOptions = false
                                onNavigateToCreate()
                            }
                            .clip(RoundedCornerShape(16.dp))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Join Option
                    ListItem(
                        headlineContent = { Text("Join with Code", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
                        supportingContent = { Text("Enter an invite code to join existing squad") },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Black) // Use generic icon if Key not avail
                            }
                        },
                        modifier = Modifier
                            .clickable {
                                showAddOptions = false
                                showJoinDialog = true
                            }
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        // Join Code Dialog
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = { 
                    Text(
                        "Enter Squad Code", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ask your squad leader for the invite code.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase() },
                            placeholder = { Text("e.g. A7X99") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                letterSpacing = 4.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Black,
                                cursorColor = Black
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        if (joinState is JoinCircleUiState.Error) {
                            Text(
                                text = (joinState as JoinCircleUiState.Error).message,
                                color = Color.Red,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.joinCircleByCode(joinCode) },
                        enabled = joinCode.isNotBlank() && joinState !is JoinCircleUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                         if (joinState is JoinCircleUiState.Loading) {
                             CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp))
                         } else {
                             Text("JOIN NOW")
                         }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun EmptyState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter("https://cdn-icons-png.flaticon.com/512/3063/3063823.png"), // Game controller icon
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            alpha = 0.5f
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No squads found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Black
        )
        Text(
            text = "Assemble your team and dominate/conquer the ladder.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Create Your First Squad", style = MaterialTheme.typography.titleMedium, color = White)
        }
    }
}

@Composable
fun SquadListContent(
    circles: List<Circle>,
    selectedFilter: String,
    onFilterSelect: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header Item (TopBar + Search + Filter)
        item {
            Column {
                // Custom Header for Squads
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
                        onClick = onNavigateToCreate,
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
                    items(listOf("All", "Dota 2", "Valorant", "Mobile Legends", "CS2", "Other")) { category ->
                        CategorySingleChip(
                            text = category,
                            isSelected = selectedFilter == category,
                            onClick = { onFilterSelect(category) }
                        )
                    }
                }
            }
        }

        items(circles) { circle ->
            CircleCard(
                title = circle.name,
                subtitle = circle.game,
                statusText = "${circle.metadata.memberCount}/5 Online",
                imageUrl = getGameWallpaper(circle.game),
                modifier = Modifier.padding(horizontal = 24.dp),
                onClick = { onCircleClick(circle.documentId) }
            )
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

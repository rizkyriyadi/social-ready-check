package com.example.tripglide.ui.squads

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripglide.ui.home.CreateCircleUiState
import com.example.tripglide.ui.home.HomeViewModel
import com.example.tripglide.ui.home.HomeViewModelFactory
import com.example.tripglide.ui.theme.White

// Colors
private val MidnightBlue = Color(0xFF2B2E4A)
private val DeepBlack = Color(0xFF0D0D10)
private val NeonRed = Color(0xFFFF3333)
private val SurfaceGrey = Color(0xFF1F1F23)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateSquadScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current))
) {
    var squadName by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf("Dota 2") }
    var customGameName by remember { mutableStateOf("") }
    var isCustomGame by remember { mutableStateOf(false) }

    val creationState by viewModel.creationState.collectAsState()

    // Background Gradient
    val mainGradient = Brush.verticalGradient(
        colors = listOf(MidnightBlue, DeepBlack)
    )
    
    // Button Gradient
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(NeonRed, Color(0xFFD00020))
    )

    // Handle Success
    LaunchedEffect(creationState) {
        if (creationState is CreateCircleUiState.Success) {
            viewModel.resetCreationState()
            onBackClick() // Pop back to Squads
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // Managed by Box
        topBar = {
            TopAppBar(
                title = { Text("ASSEMBLE SQUAD", color = White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = White, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mainGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Section 1: Squad Name
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("SQUAD IDENTITY")
                    
                    // Custom TextField
                    NeonTextField(
                        value = squadName,
                        onValueChange = { squadName = it },
                        placeholder = "e.g. The Immortals"
                    )
                }

                // Section 2: Game Selection
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("TARGET GAME")
                    
                    val games = listOf("Dota 2", "Valorant", "Mobile Legends", "CS2", "Apex", "Other")
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        games.forEach { game ->
                            val isSelected = (game == "Other" && isCustomGame) || (!isCustomGame && game == selectedGame)
                            GameChip(
                                text = game,
                                isSelected = isSelected,
                                onClick = {
                                    if (game == "Other") {
                                        isCustomGame = true
                                    } else {
                                        isCustomGame = false
                                        selectedGame = game
                                    }
                                }
                            )
                        }
                    }

                    // Animated Custom Game Input
                    AnimatedVisibility(
                        visible = isCustomGame,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            NeonTextField(
                                value = customGameName,
                                onValueChange = { customGameName = it },
                                placeholder = "Enter custom game name..."
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Error Message Box
                if (creationState is CreateCircleUiState.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33FF0000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Red, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = (creationState as CreateCircleUiState.Error).message,
                            color = White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Main Action Button (Gradient)
                val isButtonEnabled = squadName.isNotBlank() && (!isCustomGame || customGameName.isNotBlank())
                val isCreating = creationState is CreateCircleUiState.Loading

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isButtonEnabled) buttonGradient else SolidColor(Color(0xFF333333)))
                        .clickable(enabled = isButtonEnabled && !isCreating) {
                            val finalGame = if (isCustomGame) customGameName else selectedGame
                            viewModel.createCircle(squadName, finalGame)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "SUMMON SQUAD",
                            color = if (isButtonEnabled) White else Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var isFocused by remember { mutableStateOf(false) }
    
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = White, fontSize = 16.sp),
        singleLine = true,
        cursorBrush = SolidColor(NeonRed),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGrey, RoundedCornerShape(12.dp))
                    .border(
                        width = if (isFocused) 1.5.dp else 0.dp,
                        color = if (isFocused) NeonRed else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.Gray, fontSize = 16.sp)
                }
                innerTextField()
            }
        },
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
    )
}

@Composable
fun GameChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Custom Chip
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Brush.linearGradient(listOf(NeonRed, Color(0xFFD00020))) 
                else SolidColor(SurfaceGrey)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) White else Color.LightGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

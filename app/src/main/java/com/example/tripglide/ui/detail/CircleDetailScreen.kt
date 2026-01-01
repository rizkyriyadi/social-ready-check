package com.example.tripglide.ui.detail

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripglide.data.model.User
import com.example.tripglide.data.repository.AuthRepository
import com.example.tripglide.data.repository.CircleRepository
import com.example.tripglide.data.repository.CircleRepositoryImpl
import com.example.tripglide.ui.theme.White
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- PREMIUM COLORS ---
private val DarkMetalBg = Color(0xFF0F0F0F)
private val CardSurface = Color(0xFF1C1C1E) // Apple Dark Gray
private val TextSecondary = Color(0xFF8E8E93)
private val AccentRed = Color(0xFFFF3B30) // Apple System Red
private val DarkRed = Color(0xFF8B0000)
private val ShinyGold = Color(0xFFFFD700)
private val BorderShim = Color(0xFF3A3A3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    circleId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: CircleDetailViewModel = viewModel(factory = CircleDetailViewModelFactory(LocalContext.current))
) {
    LaunchedEffect(circleId) {
        viewModel.loadCircle(circleId)
    }
    val circle by viewModel.circle.collectAsState()
    val members by viewModel.members.collectAsState()

    // Global Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkMetalBg)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            circle?.name ?: "",
                            color = White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkMetalBg,
                        titleContentColor = White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SPACER
                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Cell A: SUMMON BUTTON (Hero)
                item(span = StaggeredGridItemSpan.FullLine) {
                    val summonState by viewModel.summonState.collectAsState()
                    val context = LocalContext.current
                    var showAlreadyActiveDialog by remember { mutableStateOf(false) }
                    var activeSummonId by remember { mutableStateOf<String?>(null) }
                    
                    // Handle summon state changes
                    LaunchedEffect(summonState) {
                        when (val state = summonState) {
                            is SummonState.Success -> {
                                // Launch SummonActivity for initiator
                                val intent = Intent(context, com.example.tripglide.ui.summon.SummonActivity::class.java).apply {
                                    putExtra("circleId", circleId)
                                    putExtra("summonId", state.summonId)
                                    putExtra("initiatorName", "You")
                                    putExtra("isInitiator", true)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                viewModel.resetSummonState()
                            }
                            is SummonState.AlreadyActive -> {
                                activeSummonId = state.existingSummonId
                                showAlreadyActiveDialog = true
                            }
                            is SummonState.Error -> {
                                Toast.makeText(context, "❌ ${state.message}", Toast.LENGTH_LONG).show()
                                viewModel.resetSummonState()
                            }
                            else -> {}
                        }
                    }
                    
                    // Dialog for already active summon
                    if (showAlreadyActiveDialog && activeSummonId != null) {
                        AlertDialog(
                            onDismissRequest = { 
                                showAlreadyActiveDialog = false
                                viewModel.resetSummonState()
                            },
                            title = { Text("Summon Already Active", color = Color.White) },
                            text = { Text("There's an active summon in progress. Would you like to join it or clear it?", color = Color.Gray) },
                            containerColor = Color(0xFF1C1C1E),
                            confirmButton = {
                                TextButton(onClick = {
                                    showAlreadyActiveDialog = false
                                    // Join existing summon
                                    val intent = Intent(context, com.example.tripglide.ui.summon.SummonActivity::class.java).apply {
                                        putExtra("circleId", circleId)
                                        putExtra("summonId", activeSummonId)
                                        putExtra("initiatorName", "Someone")
                                        putExtra("isInitiator", false)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    viewModel.resetSummonState()
                                }) {
                                    Text("Join", color = Color.Green)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showAlreadyActiveDialog = false
                                    viewModel.clearStaleSummon(circleId)
                                    Toast.makeText(context, "Summon cleared!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Clear & Retry", color = Color.Red)
                                }
                            }
                        )
                    }
                    
                    PremiumSummonButton(
                        onClick = { viewModel.startSummon(circleId) },
                        isLoading = summonState is SummonState.Loading
                    )
                }

                // Cell B: Squad Status (Real Data)
                item(span = StaggeredGridItemSpan.FullLine) {
                    SquadStatusCard(members)
                }

                // Cell C: Chat Shortcut
                item {
                    PremiumChatCard(onClick = onNavigateToChat)
                }

                // Cell D: Invite Code
                item {
                    PremiumInviteCard(code = circle?.code ?: "...")
                }

                // Extra: Recent Matches (Placeholder)
                item(span = StaggeredGridItemSpan.FullLine) {
                   PremiumGlassCard(modifier = Modifier.height(120.dp)) {
                       Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                           Text("Recent Activity (Coming Soon)", color = TextSecondary, fontSize = 12.sp)
                       }
                   }
                }
            }
        }
    }
}

// --- VIEWMODEL ---
class CircleDetailViewModel(
    private val repository: CircleRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _circle = MutableStateFlow<com.example.tripglide.data.model.Circle?>(null)
    val circle: StateFlow<com.example.tripglide.data.model.Circle?> = _circle.asStateFlow()

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()
    
    private val _summonState = MutableStateFlow<SummonState>(SummonState.Idle)
    val summonState: StateFlow<SummonState> = _summonState.asStateFlow()

    fun loadCircle(id: String) {
        viewModelScope.launch {
            val result = repository.getCircleById(id)
            val circleData = result.getOrNull()
            _circle.value = circleData
            
            // Fetch real members
            if (circleData != null && circleData.memberIds.isNotEmpty()) {
                val usersResult = authRepository.getUsersByIds(circleData.memberIds)
                _members.value = usersResult.getOrNull() ?: emptyList()
            }
        }
    }

    fun startSummon(circleId: String) {
        Log.d("CircleDetailVM", "🚀 Starting summon for circle: $circleId")
        _summonState.value = SummonState.Loading
        
        viewModelScope.launch {
            try {
                val result = repository.startSummon(circleId)
                result.fold(
                    onSuccess = { summonId ->
                        Log.d("CircleDetailVM", "✅ Summon created successfully: $summonId")
                        _summonState.value = SummonState.Success(summonId)
                    },
                    onFailure = { error ->
                        Log.e("CircleDetailVM", "❌ Summon failed: ${error.message}", error)
                        // Check if it's "already active" error
                        if (error.message?.contains("already active", ignoreCase = true) == true) {
                            // Get the existing summon ID and join it
                            val currentCircle = _circle.value
                            val existingSummonId = currentCircle?.activeSummonId
                            if (existingSummonId != null) {
                                Log.d("CircleDetailVM", "🔄 Joining existing summon: $existingSummonId")
                                _summonState.value = SummonState.AlreadyActive(existingSummonId)
                            } else {
                                _summonState.value = SummonState.Error(error.message ?: "Unknown error")
                            }
                        } else {
                            _summonState.value = SummonState.Error(error.message ?: "Unknown error")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CircleDetailVM", "❌ Summon exception: ${e.message}", e)
                _summonState.value = SummonState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun clearStaleSummon(circleId: String) {
        Log.d("CircleDetailVM", "🧹 Clearing stale summon for circle: $circleId")
        viewModelScope.launch {
            repository.clearActiveSummon(circleId)
            // Reload circle to refresh activeSummonId
            loadCircle(circleId)
            _summonState.value = SummonState.Idle
        }
    }
    
    fun resetSummonState() {
        _summonState.value = SummonState.Idle
    }
}

sealed class SummonState {
    object Idle : SummonState()
    object Loading : SummonState()
    data class Success(val summonId: String) : SummonState()
    data class AlreadyActive(val existingSummonId: String) : SummonState()
    data class Error(val message: String) : SummonState()
}

class CircleDetailViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CircleDetailViewModel::class.java)) {
            val repo = com.example.tripglide.data.repository.CircleRepositoryImpl()
            val authRepo = AuthRepository(context)
            @Suppress("UNCHECKED_CAST")
            return CircleDetailViewModel(repo, authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- PREMIUM COMPONENTS ---

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = CardSurface,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick ?: {}
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun PremiumSummonButton(onClick: () -> Unit, isLoading: Boolean = false) {
    // Shimmer Animation
    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(45.dp)) // Pill Shape
            .background(
                Brush.linearGradient(
                    colors = if (isLoading) listOf(Color(0xFF666666), Color(0xFF888888))
                             else listOf(Color(0xFFCC2B2B), Color(0xFFE84545)) // Deep Red Gradient
                )
            )
            .clickable(enabled = !isLoading) { onClick() }
            // "Shiny Edge" Overlay
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.3f), // The shine
                            Color.White.copy(alpha = 0f)
                        ),
                        start = Offset(translateAnim.value, 0f),
                        end = Offset(translateAnim.value + 100f, 100f) // Diagonal slide
                    )
                )
            }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon or Loading
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isLoading) "SUMMONING..." else "SUMMON PARTY",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Notify all members",
                        color = White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = White.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun SquadStatusCard(members: List<User>) {
    PremiumGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "${members.size} Members",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(members) { user ->
                    AsyncImage(
                        model = user.photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.displayName}" },
                        contentDescription = user.displayName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                }
                item {
                     Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = White, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumInviteCard(code: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current

    PremiumGlassCard(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        onClick = {
            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
             android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ACCESS CODE",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
            
            Text(
                text = code,
                color = White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun PremiumChatCard(onClick: () -> Unit) {
    PremiumGlassCard(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Email, 
                contentDescription = null, 
                tint = Color(0xFF0A84FF), // iOS Blue
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    "Basecamp",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Tap to open chat",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

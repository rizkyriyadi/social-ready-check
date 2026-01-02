package com.example.tripglide.ui.detail

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripglide.data.model.Circle
import com.example.tripglide.data.model.SummonStats
import com.example.tripglide.data.model.User
import com.example.tripglide.data.repository.AuthRepository
import com.example.tripglide.data.repository.CircleRepository
import com.example.tripglide.data.repository.CircleRepositoryImpl
import com.example.tripglide.ui.detail.tabs.BoardTabContent
import com.example.tripglide.ui.detail.tabs.ChatTabContent
import com.example.tripglide.ui.detail.tabs.HQTabContent
import com.example.tripglide.ui.theme.White
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- PREMIUM COLORS ---
private val DarkMetalBg = Color(0xFF0F0F0F)
private val CardSurface = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF8E8E93)
private val AccentRed = Color(0xFFFF3B30)
private val AccentBlue = Color(0xFF007AFF)
private val ShinyGold = Color(0xFFFFD700)

/**
 * CircleDetailScreen - Gamer Basecamp Social Hub
 * 
 * A 3-tab command center for squad management:
 * - HQ: Summon button, stats, member list
 * - CHAT: Real-time group chat with gamified visuals
 * - BOARD: Social feed and memories timeline
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CircleDetailScreen(
    circleId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: CircleDetailViewModel = viewModel(factory = CircleDetailViewModelFactory(LocalContext.current))
) {
    // Load circle data
    LaunchedEffect(circleId) {
        viewModel.loadCircle(circleId)
    }
    
    val circle by viewModel.circle.collectAsState()
    val members by viewModel.members.collectAsState()
    val summonStats by viewModel.summonStats.collectAsState()
    val summonState by viewModel.summonState.collectAsState()
    
    // Tab state
    val tabs = listOf("⚔️ HQ", "💬 CHAT", "📋 BOARD")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = DarkMetalBg,
                    contentColor = White,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = tabPositions[pagerState.currentPage].left)
                                    .width(tabPositions[pagerState.currentPage].width)
                                    .height(3.dp)
                                    .background(AccentRed)
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(color = CardSurface, thickness = 1.dp)
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        val textColor by animateColorAsState(
                            if (selected) White else TextSecondary,
                            label = "tabColor"
                        )
                        
                        Tab(
                            selected = selected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    title,
                                    color = textColor,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
                
                // Horizontal Pager for smooth tab transitions
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> HQTabContent(
                            circleId = circleId,
                            circle = circle,
                            members = members,
                            summonStats = summonStats,
                            summonState = summonState,
                            onSummon = { viewModel.startSummon(circleId) },
                            onClearSummon = { viewModel.clearStaleSummon(circleId) },
                            onMemberClick = { /* Navigate to profile */ }
                        )
                        
                        1 -> ChatTabContent(
                            circleId = circleId,
                            circleName = circle?.name ?: "Chat",
                            circleImageUrl = circle?.imageUrl,
                            onProfileClick = { /* Navigate to profile */ }
                        )
                        
                        2 -> BoardTabContent(
                            circleId = circleId,
                            onProfileClick = { /* Navigate to profile */ }
                        )
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
    private val _circle = MutableStateFlow<Circle?>(null)
    val circle: StateFlow<Circle?> = _circle.asStateFlow()

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()
    
    private val _summonState = MutableStateFlow<SummonState>(SummonState.Idle)
    val summonState: StateFlow<SummonState> = _summonState.asStateFlow()
    
    private val _summonStats = MutableStateFlow<SummonStats?>(null)
    val summonStats: StateFlow<SummonStats?> = _summonStats.asStateFlow()

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
            
            // Load summon stats
            loadSummonStats(id)
        }
    }
    
    private fun loadSummonStats(circleId: String) {
        viewModelScope.launch {
            val result = repository.getSummonStats(circleId)
            result.onSuccess { stats ->
                _summonStats.value = stats
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
                        if (error.message?.contains("already active", ignoreCase = true) == true) {
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
            val repo = CircleRepositoryImpl()
            val authRepo = AuthRepository(context)
            @Suppress("UNCHECKED_CAST")
            return CircleDetailViewModel(repo, authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

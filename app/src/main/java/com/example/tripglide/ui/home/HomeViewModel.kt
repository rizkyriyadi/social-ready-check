package com.example.tripglide.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripglide.data.model.AuditLog
import com.example.tripglide.data.model.Circle
import com.example.tripglide.data.model.DotaLinkedAccount
import com.example.tripglide.data.model.User
import com.example.tripglide.data.network.DotaWinLossResponse
import com.example.tripglide.data.repository.AuthRepository
import com.example.tripglide.data.repository.CircleRepository
import com.example.tripglide.data.repository.CircleRepositoryImpl
import com.example.tripglide.data.repository.DotaRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "HomeViewModel"

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val circles: List<Circle>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * Activity feed item combining audit log with circle info
 */
data class ActivityFeedItem(
    val id: String,
    val circleName: String,
    val circleId: String,
    val actorName: String,
    val actionType: String,
    val details: String,
    val timestamp: Timestamp?,
    val circleImageUrl: String? = null,
    // New fields for chat history
    val isChatMessage: Boolean = false,
    val chatType: String = "GROUP", // "GROUP" or "DIRECT"
    val channelId: String = "",
    val isRead: Boolean = true,
    val senderPhotoUrl: String = ""
) {
    /**
     * Get icon hint for the action type
     */
    val actionIcon: String get() = when {
        isChatMessage -> "💬"
        actionType.contains("JOIN") -> "👋"
        actionType.contains("LEAVE") -> "🚪"
        actionType.contains("NAME") -> "✏️"
        actionType.contains("PHOTO") -> "📷"
        actionType.contains("SUMMON") -> "📢"
        actionType.contains("KICK") -> "🚫"
        actionType.contains("PROMOTE") -> "⭐"
        else -> "📌"
    }
    
    /**
     * Format message for display
     */
    val displayMessage: String get() = when {
        isChatMessage -> details
        actionType.contains("JOIN") -> "$actorName joined"
        actionType.contains("LEAVE") -> "$actorName left"
        actionType.contains("NAME") -> "$actorName renamed the squad"
        actionType.contains("PHOTO") -> "$actorName updated the photo"
        actionType.contains("SUMMON") -> "$actorName created a summon"
        else -> details.ifEmpty { "$actorName made changes" }
    }
}

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val circleRepository: CircleRepository,
    private val dotaRepository: DotaRepository = DotaRepository(),
    private val chatRepository: com.example.tripglide.data.repository.ChatRepository = com.example.tripglide.data.repository.ChatRepositoryImpl()
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()
    
    // Activity Feed (including recent chats)
    private val _activityFeed = MutableStateFlow<List<ActivityFeedItem>>(emptyList())
    val activityFeed: StateFlow<List<ActivityFeedItem>> = _activityFeed.asStateFlow()
    
    // Recent Chats (unread messages)
    private val _recentChats = MutableStateFlow<List<ActivityFeedItem>>(emptyList())
    val recentChats: StateFlow<List<ActivityFeedItem>> = _recentChats.asStateFlow()
    
    private val _isFeedLoading = MutableStateFlow(false)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()
    
    // Dota Stats
    private val _dotaWinLoss = MutableStateFlow<DotaWinLossResponse?>(null)
    val dotaWinLoss: StateFlow<DotaWinLossResponse?> = _dotaWinLoss.asStateFlow()
    
    private val _isDotaStatsLoading = MutableStateFlow(false)
    val isDotaStatsLoading: StateFlow<Boolean> = _isDotaStatsLoading.asStateFlow()

    private val _circlesFlow = circleRepository.getMyCircles()
        .catch { _ -> 
            emit(emptyList())
        }

    val uiState: StateFlow<HomeUiState> = combine(_circlesFlow, _selectedFilter) { circles, filter ->
        try {
            val filtered = if (filter == "All") {
                circles
            } else {
                circles.filter { it.game.equals(filter, ignoreCase = true) }
            }
            HomeUiState.Success(filtered)
        } catch (e: Exception) {
            HomeUiState.Error(if (e.message?.contains("PERMISSION_DENIED") == true) 
                "Check Internet/Access" else e.message ?: "Unknown error")
        }
    }.catch { e ->
        emit(HomeUiState.Error(if (e.message?.contains("PERMISSION_DENIED") == true) 
            "Check Internet/Access" else e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    init {
        loadUserProfile()
        loadActivityFeed()
        loadRecentChats()
    }

    fun setFilter(category: String) {
        _selectedFilter.value = category
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getUserProfile()
            if (result.isSuccess) {
                val user = result.getOrNull()
                _user.value = user
                if (user?.onboardingCompleted == false) {
                    _shouldNavigateToOnboarding.value = true
                }
                
                // Load Dota stats if linked
                user?.linkedAccounts?.dota2?.let { dota ->
                    loadDotaStats(dota)
                }
            }
        }
    }
    
    /**
     * Load activity feed from all user's circles
     * MVP approach: fetch latest 3 logs from each circle, merge and sort
     */
    fun loadActivityFeed() {
        viewModelScope.launch {
            _isFeedLoading.value = true
            
            try {
                // Get user's circles first
                val circles = _circlesFlow.first()
                
                if (circles.isEmpty()) {
                    _activityFeed.value = emptyList()
                    _isFeedLoading.value = false
                    return@launch
                }
                
                Log.d(TAG, "Loading activity from ${circles.size} circles")
                
                // Fetch latest 3 logs from each circle in parallel
                val allLogs = circles.map { circle ->
                    async {
                        try {
                            val circleId = circle.documentId.ifEmpty { circle.id ?: "" }
                            if (circleId.isEmpty()) return@async emptyList<ActivityFeedItem>()
                            
                            val logs = circleRepository.getCircleActivityLogs(circleId).first()
                            logs.take(3).map { log ->
                                ActivityFeedItem(
                                    id = "${circleId}_${log.id}",
                                    circleName = circle.name,
                                    circleId = circleId,
                                    actorName = log.actorName,
                                    actionType = log.actionType,
                                    details = log.details,
                                    timestamp = log.timestamp,
                                    circleImageUrl = circle.imageUrl
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to fetch logs for ${circle.name}", e)
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
                
                // Sort by timestamp descending and take top 20
                val sortedFeed = allLogs
                    .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                    .take(20)
                
                Log.d(TAG, "Activity feed loaded: ${sortedFeed.size} items")
                _activityFeed.value = sortedFeed
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load activity feed", e)
                _activityFeed.value = emptyList()
            } finally {
                _isFeedLoading.value = false
            }
        }
    }
    
    /**
     * Load recent unread chats from both DM and group channels
     */
    fun loadRecentChats() {
        viewModelScope.launch {
            try {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                
                // Load DM channels
                chatRepository.getDMChannels().collect { dmChannels ->
                    val dmChatItems = dmChannels
                        .filter { it.lastMessage != null }
                        .mapNotNull { channel ->
                            val otherUser = channel.getOtherParticipant(currentUserId)
                            val unreadCount = channel.getUnreadCount(currentUserId)
                            val lastMessage = channel.lastMessage ?: return@mapNotNull null
                            
                            ActivityFeedItem(
                                id = "dm_${channel.id}",
                                circleName = otherUser?.displayName ?: "Direct Message",
                                circleId = "",
                                actorName = lastMessage.senderName,
                                actionType = "CHAT_MESSAGE",
                                details = if (lastMessage.senderId == currentUserId) {
                                    "You: ${lastMessage.content}"
                                } else {
                                    lastMessage.content
                                },
                                timestamp = lastMessage.timestamp,
                                circleImageUrl = otherUser?.photoUrl,
                                isChatMessage = true,
                                chatType = "DIRECT",
                                channelId = channel.id,
                                isRead = unreadCount == 0,
                                senderPhotoUrl = otherUser?.photoUrl ?: ""
                            )
                        }
                    
                    // Combine with existing activity feed
                    val currentFeed = _activityFeed.value.filter { !it.isChatMessage }
                    val combinedFeed = (dmChatItems + currentFeed)
                        .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                        .take(30)
                    
                    _recentChats.value = dmChatItems.filter { !it.isRead }
                    
                    Log.d(TAG, "Recent chats loaded: ${dmChatItems.size} DM items")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recent chats", e)
            }
        }
    }
    
    /**
     * Mark a chat as read (remove from unread list)
     */
    fun markChatAsRead(channelId: String, chatType: String) {
        viewModelScope.launch {
            _recentChats.value = _recentChats.value.filter { 
                !(it.channelId == channelId && it.chatType == chatType) 
            }
        }
    }
    
    /**
     * Load Dota win/loss stats for linked account
     */
    private fun loadDotaStats(dota: DotaLinkedAccount) {
        viewModelScope.launch {
            _isDotaStatsLoading.value = true
            
            try {
                dotaRepository.getWinLoss(dota.accountId).collect { result ->
                    if (result.isSuccess) {
                        _dotaWinLoss.value = result.getOrNull()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Dota stats", e)
            } finally {
                _isDotaStatsLoading.value = false
            }
        }
    }
    
    fun refreshDotaStats() {
        _user.value?.linkedAccounts?.dota2?.let { dota ->
            loadDotaStats(dota)
        }
    }

    private val _shouldNavigateToOnboarding = MutableStateFlow(false)
    val shouldNavigateToOnboarding: StateFlow<Boolean> = _shouldNavigateToOnboarding.asStateFlow()

    fun onOnboardingNavigationHandled() {
        _shouldNavigateToOnboarding.value = false
    }

    private val _creationState = MutableStateFlow<CreateCircleUiState>(CreateCircleUiState.Idle)
    val creationState: StateFlow<CreateCircleUiState> = _creationState.asStateFlow()

    fun createCircle(name: String, game: String) {
        viewModelScope.launch {
            _creationState.value = CreateCircleUiState.Loading
            val result = circleRepository.createCircle(name, game, "SEA")
            if (result.isSuccess) {
                _creationState.value = CreateCircleUiState.Success
                // Flow updates automatically
            } else {
                _creationState.value = CreateCircleUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun resetCreationState() {
        _creationState.value = CreateCircleUiState.Idle
    }

    private val _joinState = MutableStateFlow<JoinCircleUiState>(JoinCircleUiState.Idle)
    val joinState: StateFlow<JoinCircleUiState> = _joinState.asStateFlow()

    fun joinCircleByCode(code: String) {
        if (code.isBlank()) return
        
        viewModelScope.launch {
            _joinState.value = JoinCircleUiState.Loading
            val result = circleRepository.joinCircleByCode(code)
            if (result.isSuccess) {
                _joinState.value = JoinCircleUiState.Success
            } else {
                _joinState.value = JoinCircleUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun resetJoinState() {
        _joinState.value = JoinCircleUiState.Idle
    }

    fun signOut() {
        authRepository.signOut()
    }
}

sealed interface JoinCircleUiState {
    data object Idle : JoinCircleUiState
    data object Loading : JoinCircleUiState
    data object Success : JoinCircleUiState
    data class Error(val message: String) : JoinCircleUiState
}

sealed interface CreateCircleUiState {
    data object Idle : CreateCircleUiState
    data object Loading : CreateCircleUiState
    data object Success : CreateCircleUiState
    data class Error(val message: String) : CreateCircleUiState
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val authRepo = AuthRepository(context)
            // Manual DI for CircleRepository
            val circleRepo = CircleRepositoryImpl()
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(authRepo, circleRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

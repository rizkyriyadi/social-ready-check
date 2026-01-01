package com.example.tripglide.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripglide.data.model.*
import com.example.tripglide.data.repository.AuthRepository
import com.example.tripglide.data.repository.ChatRepository
import com.example.tripglide.data.repository.ChatRepositoryImpl
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val TAG = "ChatViewModel"

/**
 * Universal ChatViewModel for both GROUP and DIRECT chats.
 * Handles message loading, sending, read receipts, and media uploads.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Current channel configuration
    private var currentChannelId: String? = null
    private var currentChatType: ChatType = ChatType.GROUP

    // Message collection job
    private var messagesJob: Job? = null
    private var typingJob: Job? = null

    // Current user ID
    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    // Participant profiles for read receipts
    private val _participantProfiles = MutableStateFlow<Map<String, ParticipantProfile>>(emptyMap())
    val participantProfiles: StateFlow<Map<String, ParticipantProfile>> = _participantProfiles.asStateFlow()

    // Channel info (for DM header display)
    private val _dmChannel = MutableStateFlow<DMChannel?>(null)
    val dmChannel: StateFlow<DMChannel?> = _dmChannel.asStateFlow()

    /**
     * Initialize chat for a specific channel
     */
    fun initializeChat(channelId: String, type: ChatType) {
        if (currentChannelId == channelId && currentChatType == type) {
            Log.d(TAG, "Chat already initialized for $channelId")
            return
        }

        Log.d(TAG, "🚀 Initializing chat: channelId=$channelId, type=$type")
        currentChannelId = channelId
        currentChatType = type

        // Cancel previous subscriptions
        messagesJob?.cancel()
        typingJob?.cancel()

        // Reset state
        _uiState.value = ChatUiState(isLoading = true)

        // Start listening to messages
        messagesJob = viewModelScope.launch {
            chatRepository.getMessages(channelId, type)
                .catch { e ->
                    Log.e(TAG, "❌ Error loading messages", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { messages ->
                    Log.d(TAG, "📨 Received ${messages.size} messages")
                    val displayMessages = processMessagesForDisplay(messages)
                    _uiState.update { 
                        it.copy(
                            messages = displayMessages,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }

        // Load channel info for DM
        if (type == ChatType.DIRECT) {
            viewModelScope.launch {
                chatRepository.getDMChannel(channelId).onSuccess { channel ->
                    _dmChannel.value = channel
                    channel?.participantProfiles?.let { profiles ->
                        _participantProfiles.value = profiles
                    }
                }
            }
        }

        // Listen to typing indicators
        typingJob = viewModelScope.launch {
            chatRepository.getTypingUsers(channelId, type)
                .collect { typingUserIds ->
                    val profiles = typingUserIds.mapNotNull { userId ->
                        _participantProfiles.value[userId]
                    }
                    _uiState.update { it.copy(typingUsers = profiles) }
                }
        }
    }

    /**
     * Process raw messages into DisplayMessage with grouping metadata
     */
    private fun processMessagesForDisplay(messages: List<UniversalChatMessage>): List<DisplayMessage> {
        if (messages.isEmpty()) return emptyList()

        val userId = currentUserId ?: return emptyList()
        val result = mutableListOf<DisplayMessage>()
        
        // Messages come in DESC order (newest first), but we display bottom-up
        // So we process as-is but logic is inverted

        messages.forEachIndexed { index, message ->
            val isMe = message.senderId == userId
            
            // Previous message (newer in time, displayed above)
            val prevMessage = messages.getOrNull(index - 1)
            // Next message (older in time, displayed below)
            val nextMessage = messages.getOrNull(index + 1)

            // Check if this is first in a group (show avatar)
            // First in group if: no previous message OR previous sender is different OR time gap > 5min
            val isFirstInGroup = prevMessage == null ||
                prevMessage.senderId != message.senderId ||
                hasTimeGap(prevMessage.createdAt, message.createdAt, 5)

            // Check if this is last in a group (show timestamp)
            val isLastInGroup = nextMessage == null ||
                nextMessage.senderId != message.senderId ||
                hasTimeGap(message.createdAt, nextMessage.createdAt, 5)

            // Check if next message is same sender (for bubble rounding)
            val isNextMessageSameSender = nextMessage != null &&
                nextMessage.senderId == message.senderId &&
                !hasTimeGap(message.createdAt, nextMessage.createdAt, 5)

            // Show date divider if day changed
            val showDateDivider = nextMessage == null ||
                !isSameDay(message.createdAt, nextMessage.createdAt)

            // Get read receipts (exclude self)
            val readByProfiles = message.readBy.keys
                .filter { it != userId && it != message.senderId }
                .mapNotNull { readUserId ->
                    _participantProfiles.value[readUserId]
                }

            result.add(
                DisplayMessage(
                    message = message,
                    isMe = isMe,
                    isFirstInGroup = isFirstInGroup,
                    isLastInGroup = isLastInGroup,
                    isNextMessageSameSender = isNextMessageSameSender,
                    showDateDivider = showDateDivider,
                    readByProfiles = readByProfiles
                )
            )
        }

        return result
    }

    /**
     * Check if there's a time gap between two timestamps
     */
    private fun hasTimeGap(t1: Timestamp?, t2: Timestamp?, minutes: Int): Boolean {
        if (t1 == null || t2 == null) return true
        val diffMs = kotlin.math.abs(t1.toDate().time - t2.toDate().time)
        return diffMs > TimeUnit.MINUTES.toMillis(minutes.toLong())
    }

    /**
     * Check if two timestamps are on the same day
     */
    private fun isSameDay(t1: Timestamp?, t2: Timestamp?): Boolean {
        if (t1 == null || t2 == null) return false
        val cal1 = Calendar.getInstance().apply { time = t1.toDate() }
        val cal2 = Calendar.getInstance().apply { time = t2.toDate() }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // ==================== SEND MESSAGES ====================

    /**
     * Send a text message
     */
    fun sendTextMessage(content: String) {
        val channelId = currentChannelId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendTextMessage(channelId, currentChatType, content.trim())
                .onSuccess {
                    Log.d(TAG, "✅ Message sent")
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Failed to send message", e)
                    _uiState.update { it.copy(error = "Failed to send: ${e.message}") }
                }
        }
    }

    /**
     * Send a media message (image/video)
     */
    fun sendMediaMessage(uri: Uri, mediaType: UniversalMessageType, caption: String? = null) {
        val channelId = currentChannelId ?: return

        viewModelScope.launch {
            // Show upload state with local preview
            _uiState.update {
                it.copy(
                    uploadState = MediaUploadState.Uploading(
                        progress = 0f,
                        localUri = uri.toString(),
                        fileName = uri.lastPathSegment ?: "media"
                    )
                )
            }

            chatRepository.sendMediaMessage(
                channelId = channelId,
                type = currentChatType,
                mediaUri = uri,
                mediaType = mediaType,
                caption = caption,
                onProgress = { progress ->
                    _uiState.update {
                        val currentUpload = it.uploadState as? MediaUploadState.Uploading
                        if (currentUpload != null) {
                            it.copy(uploadState = currentUpload.copy(progress = progress))
                        } else it
                    }
                }
            ).onSuccess { message ->
                Log.d(TAG, "✅ Media message sent")
                _uiState.update { it.copy(uploadState = MediaUploadState.Success(message.mediaUrl ?: "")) }
                // Reset after short delay
                delay(500)
                _uiState.update { it.copy(uploadState = MediaUploadState.Idle) }
            }.onFailure { e ->
                Log.e(TAG, "❌ Failed to send media", e)
                _uiState.update { it.copy(uploadState = MediaUploadState.Error(e.message ?: "Upload failed")) }
            }
        }
    }

    // ==================== READ RECEIPTS ====================

    /**
     * Mark visible messages as read
     */
    fun markMessagesAsRead(messageIds: List<String>) {
        val channelId = currentChannelId ?: return
        val userId = currentUserId ?: return

        // Filter out messages we've already read
        val unreadMessageIds = messageIds.filter { messageId ->
            val message = _uiState.value.messages.find { it.message.id == messageId }?.message
            message != null && !message.isReadBy(userId) && message.senderId != userId
        }

        if (unreadMessageIds.isEmpty()) return

        viewModelScope.launch {
            chatRepository.markMultipleAsRead(channelId, currentChatType, unreadMessageIds)
        }
    }

    /**
     * Mark a single message as read
     */
    fun markAsRead(messageId: String) {
        val channelId = currentChannelId ?: return
        val userId = currentUserId ?: return

        val message = _uiState.value.messages.find { it.message.id == messageId }?.message
        if (message == null || message.isReadBy(userId) || message.senderId == userId) return

        viewModelScope.launch {
            chatRepository.markAsRead(channelId, currentChatType, messageId)
        }
    }

    // ==================== TYPING INDICATORS ====================

    private var typingIndicatorJob: Job? = null

    /**
     * Update typing status (debounced)
     */
    fun setTyping(isTyping: Boolean) {
        val channelId = currentChannelId ?: return

        typingIndicatorJob?.cancel()
        
        if (isTyping) {
            viewModelScope.launch {
                chatRepository.setTypingStatus(channelId, currentChatType, true)
            }
            // Auto-clear after 5 seconds
            typingIndicatorJob = viewModelScope.launch {
                delay(5000)
                chatRepository.setTypingStatus(channelId, currentChatType, false)
            }
        } else {
            viewModelScope.launch {
                chatRepository.setTypingStatus(channelId, currentChatType, false)
            }
        }
    }

    // ==================== LOAD MORE ====================

    /**
     * Load older messages (pagination)
     */
    fun loadMoreMessages() {
        val channelId = currentChannelId ?: return
        val oldestMessage = _uiState.value.messages.lastOrNull()?.message ?: return
        val oldestTimestamp = oldestMessage.createdAt ?: return

        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            chatRepository.loadMoreMessages(channelId, currentChatType, oldestTimestamp)
                .onSuccess { olderMessages ->
                    if (olderMessages.isEmpty()) {
                        _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = false) }
                    } else {
                        // Append older messages
                        val allMessages = _uiState.value.messages.map { it.message } + olderMessages
                        val displayMessages = processMessagesForDisplay(allMessages)
                        _uiState.update {
                            it.copy(
                                messages = displayMessages,
                                isLoadingMore = false
                            )
                        }
                    }
                }
                .onFailure { e ->
                    // On error, disable hasMoreMessages to prevent spam retry loop
                    _uiState.update { 
                        it.copy(
                            isLoadingMore = false, 
                            hasMoreMessages = false,  // Stop retrying on error
                            error = e.message
                        ) 
                    }
                }
        }
    }

    // ==================== DELETE/EDIT ====================

    fun deleteMessage(messageId: String) {
        val channelId = currentChannelId ?: return

        viewModelScope.launch {
            chatRepository.deleteMessage(channelId, currentChatType, messageId)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        val channelId = currentChannelId ?: return

        viewModelScope.launch {
            chatRepository.editMessage(channelId, currentChatType, messageId, newContent)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ==================== CLEANUP ====================

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearUploadState() {
        _uiState.update { it.copy(uploadState = MediaUploadState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
        typingJob?.cancel()
        typingIndicatorJob?.cancel()
        
        // Clear typing status
        currentChannelId?.let { channelId ->
            viewModelScope.launch {
                chatRepository.setTypingStatus(channelId, currentChatType, false)
            }
        }
    }
}

/**
 * ViewModel Factory
 */
class ChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val chatRepo = ChatRepositoryImpl()
            val authRepo = AuthRepository(context)
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepo, authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.tripglide.data.repository

import android.net.Uri
import com.example.tripglide.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Universal Chat Repository Interface
 * 
 * Provides unified API for both GROUP (Circle) and DIRECT (DM) chats.
 * Supports media upload, read receipts, and real-time updates.
 */
interface ChatRepository {
    
    // ==================== MESSAGES ====================
    
    /**
     * Get real-time stream of messages for a channel.
     * 
     * @param channelId The channel ID (circleId for GROUP, dmChannelId for DIRECT)
     * @param type The chat type (GROUP or DIRECT)
     * @param limit Maximum messages to fetch initially (default 50)
     * @return Flow emitting updated list of messages (newest first)
     */
    fun getMessages(
        channelId: String, 
        type: ChatType,
        limit: Int = 50
    ): Flow<List<UniversalChatMessage>>
    
    /**
     * Load older messages for pagination.
     * 
     * @param channelId The channel ID
     * @param type The chat type
     * @param beforeTimestamp Load messages before this timestamp
     * @param limit Number of messages to load
     * @return Result containing list of older messages
     */
    suspend fun loadMoreMessages(
        channelId: String,
        type: ChatType,
        beforeTimestamp: com.google.firebase.Timestamp,
        limit: Int = 30
    ): Result<List<UniversalChatMessage>>
    
    /**
     * Send a text message.
     * 
     * @param channelId The channel ID
     * @param type The chat type
     * @param content The message text content
     * @return Result indicating success or failure
     */
    suspend fun sendTextMessage(
        channelId: String,
        type: ChatType,
        content: String
    ): Result<UniversalChatMessage>
    
    /**
     * Send a media message (image/video).
     * Uploads media to Storage first, then saves message to Firestore.
     * 
     * @param channelId The channel ID
     * @param type The chat type
     * @param mediaUri Local URI of the media file
     * @param mediaType The type (IMAGE or VIDEO)
     * @param caption Optional caption text
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result containing the sent message
     */
    suspend fun sendMediaMessage(
        channelId: String,
        type: ChatType,
        mediaUri: Uri,
        mediaType: UniversalMessageType,
        caption: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<UniversalChatMessage>
    
    /**
     * Send a system message (join/leave notifications, etc.)
     */
    suspend fun sendSystemMessage(
        channelId: String,
        type: ChatType,
        content: String
    ): Result<Unit>
    
    // ==================== READ RECEIPTS ====================
    
    /**
     * Mark a message as read by current user.
     * Updates the message's readBy map.
     * 
     * @param channelId The channel ID
     * @param type The chat type
     * @param messageId The message document ID
     */
    suspend fun markAsRead(
        channelId: String,
        type: ChatType,
        messageId: String
    ): Result<Unit>
    
    /**
     * Mark multiple messages as read (batch operation).
     * More efficient for marking all visible messages.
     */
    suspend fun markMultipleAsRead(
        channelId: String,
        type: ChatType,
        messageIds: List<String>
    ): Result<Unit>
    
    /**
     * Get users who read a specific message.
     */
    suspend fun getReadByUsers(
        channelId: String,
        type: ChatType,
        messageId: String
    ): Result<List<User>>
    
    // ==================== DM CHANNELS ====================
    
    /**
     * Get or create a DM channel with another user.
     * Uses deterministic channel ID: min(uid1, uid2)_max(uid1, uid2)
     * 
     * @param otherUserId The other participant's user ID
     * @return Result containing the DMChannel
     */
    suspend fun getOrCreateDMChannel(otherUserId: String): Result<DMChannel>
    
    /**
     * Get all DM channels for current user.
     * Ordered by lastMessage timestamp descending.
     * 
     * @return Flow emitting updated list of DM channels
     */
    fun getDMChannels(): Flow<List<DMChannel>>
    
    /**
     * Get a specific DM channel by ID.
     */
    suspend fun getDMChannel(channelId: String): Result<DMChannel?>
    
    /**
     * Update DM channel settings (mute, block).
     */
    suspend fun updateDMChannelSettings(
        channelId: String,
        mute: Boolean? = null,
        block: Boolean? = null
    ): Result<Unit>
    
    // ==================== MEDIA ====================
    
    /**
     * Get media messages from a channel (for gallery view).
     * 
     * @param channelId The channel ID
     * @param type The chat type
     * @param mediaTypes Types to filter (IMAGE, VIDEO)
     * @return Flow emitting media messages
     */
    fun getMediaMessages(
        channelId: String,
        type: ChatType,
        mediaTypes: List<UniversalMessageType> = listOf(
            UniversalMessageType.IMAGE, 
            UniversalMessageType.VIDEO
        )
    ): Flow<List<UniversalChatMessage>>
    
    // ==================== TYPING INDICATORS (Future) ====================
    
    /**
     * Set typing status for current user in a channel.
     */
    suspend fun setTypingStatus(
        channelId: String,
        type: ChatType,
        isTyping: Boolean
    )
    
    /**
     * Observe typing users in a channel.
     */
    fun getTypingUsers(
        channelId: String,
        type: ChatType
    ): Flow<List<String>>
    
    // ==================== DELETE/EDIT ====================
    
    /**
     * Soft delete a message (marks as deleted but keeps record).
     */
    suspend fun deleteMessage(
        channelId: String,
        type: ChatType,
        messageId: String
    ): Result<Unit>
    
    /**
     * Edit a message's content.
     */
    suspend fun editMessage(
        channelId: String,
        type: ChatType,
        messageId: String,
        newContent: String
    ): Result<Unit>
}

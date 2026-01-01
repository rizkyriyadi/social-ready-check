package com.example.tripglide.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Universal Chat Models for Group (Circle) and Direct Messages.
 * 
 * This architecture supports:
 * - circles/{circleId}/messages/{messageId} for GROUP chats
 * - chats/{channelId}/messages/{messageId} for DIRECT messages
 */

/**
 * Chat channel type identifier
 */
enum class ChatType {
    GROUP,   // Circle group chat
    DIRECT   // 1-to-1 direct message
}

/**
 * Extended message types for rich chat experience
 */
enum class UniversalMessageType {
    TEXT,    // Regular text message
    IMAGE,   // Image attachment
    VIDEO,   // Video attachment
    AUDIO,   // Voice message (future)
    FILE,    // File attachment (future)
    SYSTEM,  // System notification (e.g., "X joined")
    SUMMON,  // Ready check trigger
    REPLY,   // Reply to another message (future)
    DELETED  // Soft-deleted message placeholder
}

/**
 * Universal chat message model with read receipts support.
 * 
 * Works for both:
 * - circles/{circleId}/messages/{messageId}
 * - chats/{channelId}/messages/{messageId}
 * 
 * Note: We don't use @DocumentId because existing messages in Firestore
 * may have 'id' stored as a field. We set the ID manually after deserialization.
 */
@IgnoreExtraProperties
data class UniversalChatMessage(
    var id: String = "",  // Set manually from document.id
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val content: String = "",
    val type: String = UniversalMessageType.TEXT.name,
    
    // Media attachments
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val mediaDuration: Long? = null,  // For video/audio in seconds
    val mediaSize: Long? = null,      // File size in bytes
    
    // Read receipts: Map<UserId, ReadAtTimestamp>
    // This is scalable for Firestore queries and atomic updates
    val readBy: Map<String, Timestamp> = emptyMap(),
    
    // Reply support (future)
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    
    // Metadata
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val editedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null,
    
    // Extra data (flexible for future features)
    val metadata: Map<String, Any>? = null
) {
    /**
     * Check if message was read by a specific user
     */
    fun isReadBy(userId: String): Boolean = readBy.containsKey(userId)
    
    /**
     * Get list of user IDs who read this message
     */
    fun getReadByUserIds(): List<String> = readBy.keys.toList()
    
    /**
     * Check if message is a media message
     */
    fun isMediaMessage(): Boolean = 
        type == UniversalMessageType.IMAGE.name || 
        type == UniversalMessageType.VIDEO.name ||
        type == UniversalMessageType.AUDIO.name
    
    /**
     * Check if message is system-generated
     */
    fun isSystemMessage(): Boolean = 
        type == UniversalMessageType.SYSTEM.name ||
        type == UniversalMessageType.SUMMON.name
    
    /**
     * Check if message is deleted
     */
    fun isDeleted(): Boolean = 
        type == UniversalMessageType.DELETED.name || deletedAt != null
}

/**
 * Direct Message Channel model.
 * 
 * Firestore path: chats/{channelId}
 * 
 * Channel ID is deterministic: min(uid1, uid2)_max(uid1, uid2)
 * This ensures the same channel for two users regardless of who initiates.
 */
@IgnoreExtraProperties
data class DMChannel(
    @DocumentId
    val id: String = "",
    
    // Participants: exactly 2 user IDs for DM
    val participants: List<String> = emptyList(),
    
    // Participant profiles (denormalized for quick display)
    val participantProfiles: Map<String, ParticipantProfile> = emptyMap(),
    
    // Last message preview
    val lastMessage: LastMessagePreview? = null,
    
    // Unread counts per user: Map<UserId, UnreadCount>
    val unreadCounts: Map<String, Int> = emptyMap(),
    
    // Channel metadata
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    
    // Mute/Block status per user
    val mutedBy: List<String> = emptyList(),
    val blockedBy: List<String> = emptyList()
) {
    companion object {
        /**
         * Generate deterministic channel ID for two users
         */
        fun generateChannelId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }
    
    /**
     * Get the other participant's profile (for DM display)
     */
    fun getOtherParticipant(currentUserId: String): ParticipantProfile? {
        val otherId = participants.find { it != currentUserId }
        return otherId?.let { participantProfiles[it] }
    }
    
    /**
     * Check if current user has muted this channel
     */
    fun isMutedBy(userId: String): Boolean = mutedBy.contains(userId)
    
    /**
     * Get unread count for user
     */
    fun getUnreadCount(userId: String): Int = unreadCounts[userId] ?: 0
}

/**
 * Lightweight participant profile for denormalized display
 */
@IgnoreExtraProperties
data class ParticipantProfile(
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val online: Boolean = false,
    val lastSeen: Timestamp? = null
)

/**
 * Last message preview for channel list display
 */
@IgnoreExtraProperties
data class LastMessagePreview(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",      // Truncated content (max 100 chars)
    val type: String = UniversalMessageType.TEXT.name,
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    /**
     * Get preview text based on message type
     */
    fun getPreviewText(): String {
        return when (type) {
            UniversalMessageType.IMAGE.name -> "📷 Photo"
            UniversalMessageType.VIDEO.name -> "🎬 Video"
            UniversalMessageType.AUDIO.name -> "🎤 Voice message"
            UniversalMessageType.FILE.name -> "📎 File"
            UniversalMessageType.SYSTEM.name -> content
            UniversalMessageType.DELETED.name -> "Message deleted"
            else -> content.take(100)
        }
    }
}

/**
 * Message grouping data for smart bubble clustering
 */
data class MessageGroup(
    val senderId: String,
    val senderName: String,
    val senderPhotoUrl: String,
    val messages: List<UniversalChatMessage>,
    val startTime: Timestamp?,
    val endTime: Timestamp?
)

/**
 * UI state for chat messages with grouping metadata
 */
data class DisplayMessage(
    val message: UniversalChatMessage,
    val isMe: Boolean,
    val isFirstInGroup: Boolean,      // Show avatar/name
    val isLastInGroup: Boolean,       // Show timestamp
    val isNextMessageSameSender: Boolean,  // For bubble corner rounding
    val showDateDivider: Boolean,     // Show date separator
    val readByProfiles: List<ParticipantProfile> = emptyList()  // For read receipts
)

/**
 * Media upload state for UI feedback
 */
sealed class MediaUploadState {
    object Idle : MediaUploadState()
    data class Uploading(
        val progress: Float,      // 0.0 to 1.0
        val localUri: String,     // Local preview URI
        val fileName: String
    ) : MediaUploadState()
    data class Success(val url: String) : MediaUploadState()
    data class Error(val message: String) : MediaUploadState()
}

/**
 * Chat UI state
 */
data class ChatUiState(
    val messages: List<DisplayMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val uploadState: MediaUploadState = MediaUploadState.Idle,
    val typingUsers: List<ParticipantProfile> = emptyList()
)

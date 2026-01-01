package com.example.tripglide.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a Circle (persistent squad/group) in Firestore.
 * 
 * Firestore path: circles/{circleId}
 */
@IgnoreExtraProperties
data class Circle(
    @DocumentId
    val documentId: String = "",
    val id: String? = null, // Handle legacy 'id' field if present
    val name: String = "",
    val game: String = "",
    val region: String = "",
    val imageUrl: String? = null,
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(), // Denormalized for array-contains queries
    val code: String = "", // Unique invite code
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val lastMessage: LastMessage = LastMessage(),
    val metadata: CircleMetadata = CircleMetadata(),
    val settings: CircleSettings = CircleSettings()
)

/**
 * Denormalized preview of the last message for quick UI display.
 */
data class LastMessage(
    val content: String = "",
    val senderName: String = "",
    val senderId: String = "",
    val timestamp: Timestamp? = null,
    val type: String = MessageType.TEXT.name
)

/**
 * Aggregated statistics for a Circle.
 * Stored as a map in the parent document for atomic increments.
 */
data class CircleMetadata(
    val totalSummons: Int = 0,    // Ready check triggers
    val mediaCount: Int = 0,      // Gallery items (IMAGE + VIDEO)
    val memberCount: Int = 0      // Current member count
)

/**
 * Circle configuration toggles.
 */
data class CircleSettings(
    @get:com.google.firebase.firestore.PropertyName("isPublic")
    val isPublic: Boolean = false,
    val allowMediaUpload: Boolean = true,
    val muteNotifications: Boolean = false
)

/**
 * Role hierarchy for Circle members.
 */
enum class MemberRole {
    LEADER,  // Circle creator, full permissions
    ADMIN,   // Elevated permissions (kick members, manage settings)
    MEMBER   // Standard member
}

/**
 * Represents a member within a Circle.
 * 
 * Firestore path: circles/{circleId}/members/{userId}
 */
data class CircleMember(
    @DocumentId
    val documentId: String = "", // Will match userId
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    @ServerTimestamp
    val joinedAt: Timestamp? = null,
    val role: String = MemberRole.MEMBER.name,
    val stats: MemberStats = MemberStats(),
    val lastReadAt: Timestamp? = null // For unread badge calculation
)

/**
 * Per-member statistics within a Circle.
 */
data class MemberStats(
    val messagesCount: Int = 0,
    val mediaUploaded: Int = 0,
    val summonsTriggered: Int = 0
)

/**
 * Types of messages supported in Circle chat.
 */
enum class MessageType {
    TEXT,    // Regular text message
    IMAGE,   // Image attachment
    VIDEO,   // Video attachment
    SYSTEM,  // System notification (e.g., "X joined the circle")
    SUMMON   // Ready check trigger
}

/**
 * Represents a chat message in a Circle.
 * 
 * Firestore path: circles/{circleId}/messages/{messageId}
 */
data class ChatMessage(
    @DocumentId
    val documentId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val content: String = "",
    val type: String = MessageType.TEXT.name,
    val mediaUrl: String? = null,      // Full media URL (Cloud Storage)
    val thumbnailUrl: String? = null,  // Thumbnail for videos
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val metadata: Map<String, Any>? = null // Extra data (e.g., summon responses)
)

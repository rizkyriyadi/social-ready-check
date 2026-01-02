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
    val settings: CircleSettings = CircleSettings(),
    val activeSummonId: String? = null // ID of the currently active summon, if any
)

/**
 * Member info for display in summon UI.
 * Denormalized from user documents for real-time display without extra reads.
 */
@IgnoreExtraProperties
data class MemberInfo(
    val displayName: String = "",
    val photoUrl: String = ""
)

/**
 * Represents a "Summon" (Ready Check) event.
 * Subcollection: circles/{circleId}/summons/{summonId}
 */
@IgnoreExtraProperties
data class Summon(
    @DocumentId
    val id: String = "",
    val initiatorId: String = "",
    val initiatorName: String = "", // Denormalized for UI
    val initiatorPhotoUrl: String? = null, // Denormalized for UI
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null, // Usually createdAt + 30s
    val status: String = SummonStatus.PENDING.name,
    val responses: Map<String, String> = emptyMap(), // Map<UserId, ResponseStatus>
    // New fields for enhanced UI
    val circleName: String = "",           // Circle name for header display
    val circleImageUrl: String? = null,    // Circle photo for header
    val memberInfoMap: Map<String, MemberInfo> = emptyMap() // Map<UserId, MemberInfo> for grid display
)

enum class SummonStatus {
    PENDING,
    SUCCESS, // All accepted
    FAILED   // Someone declined or timeout
}

enum class SummonResponseStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    TIMEOUT
}

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

// ==================== BOARD / TIMELINE MODELS ====================

/**
 * Post in the social feed/timeline (Board Tab)
 * 
 * Firestore path: circles/{circleId}/posts/{postId}
 */
@IgnoreExtraProperties
data class Post(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null, // IMAGE, VIDEO
    val thumbnailUrl: String? = null, // For video thumbnails
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun isLikedBy(userId: String): Boolean = likedBy.contains(userId)
    fun hasMedia(): Boolean = !mediaUrl.isNullOrBlank()
    fun isImage(): Boolean = mediaType == PostMediaType.IMAGE.name
    fun isVideo(): Boolean = mediaType == PostMediaType.VIDEO.name
}

/**
 * Media types for posts
 */
enum class PostMediaType {
    IMAGE,
    VIDEO
}

/**
 * Comment on a post
 * 
 * Firestore path: circles/{circleId}/posts/{postId}/comments/{commentId}
 */
@IgnoreExtraProperties
data class PostComment(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

// ==================== HALL OF REPUTATION MODELS ====================

/**
 * Aggregated summon statistics for "Hall of Reputation" widget
 */
data class SummonStats(
    val warlord: SummonLeaderInfo? = null,  // 👑 Most summons initiated
    val loyal: SummonLeaderInfo? = null,    // 🛡️ Highest accept rate
    val ghost: SummonLeaderInfo? = null     // 👻 Highest timeout/decline rate
)

/**
 * Leader info for a specific reputation stat
 */
data class SummonLeaderInfo(
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val value: Int = 0,         // Count or percentage (0-100)
    val isPercentage: Boolean = false
)
